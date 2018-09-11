package visitors;

import bytecode.*;
import bytecode.type.*;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedVoidType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import exceptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpressionVisitor extends GenericVisitorAdapter<ExpressionResult, Block> {

	/**
	 * Simply allows non-null returning statements which might have block creating
	 * side effects
	 *
	 * @param n
	 * @param arg
	 * @return
	 */
	@Override
	public ExpressionResult visit(BlockStmt n, Block arg) {
		Block currentBlock = arg;
		if (n.getStatements() != null) {
			for (final Statement s : n.getStatements()) {
				if(s instanceof IfStmt || s instanceof WhileStmt || s instanceof ForStmt) {
					currentBlock = s.accept(new BlockVisitor(), currentBlock);
				} else {
					ExpressionResult expressionResult = s.accept(this, currentBlock);
					if(expressionResult == null) {
						throw new UnsupportedOperationException("Cannot calculate statement " + s);
					} else {
						currentBlock = expressionResult.block;
					}
				}
			}
		}
		return new ExpressionResult(null, currentBlock);
	}

	@Override
	public ExpressionResult visit(IfStmt n, Block arg) {
		return new ExpressionResult(null, n.accept(new BlockVisitor(), arg));
	}

	@Override
	public ExpressionResult visit(final AssignExpr expression, Block context) {
		Expression target = expression.getTarget();

		Temporary ptrTemporary = null;
		Temporary idxTemporary = null;
		ObjectType objectType = null;
		String field = null;
		boolean isStaticAssignment = false;

		//-- first check out the LHS
		if(target instanceof NameExpr && !target.asNameExpr().resolve().isField()) {
			// ignore, nothing to do for the LHS
			field = target.asNameExpr().getNameAsString();
		} else if(target instanceof NameExpr && target.asNameExpr().resolve().isField()) {
			// of format x = y; where x is a field of this
			String classNameOfSurroundingClass = JavaParserFacade.get(new ReflectionTypeSolver()).getTypeOfThisIn(target).asReferenceType().getId();
			objectType = context.function.file.typeSystem().findType(classNameOfSurroundingClass);
			field = target.asNameExpr().getNameAsString();

			isStaticAssignment = objectType.isStaticField(field);

			if(!isStaticAssignment) {
				ptrTemporary = context.localManager().readByName("this");
			}

		} else if(target instanceof FieldAccessExpr) {
			// of format x.y = z;
			ExpressionResult ptrResult = target.asFieldAccessExpr().getScope().accept(this, context);
			context = ptrResult.block;
			ptrTemporary = ptrResult.temporary;

			ResolvedType resolvedType = target.asFieldAccessExpr().getScope().calculateResolvedType();
			String className = resolvedType.asReferenceType().getId();

			field = target.asFieldAccessExpr().getNameAsString();

			objectType = context.function.file.typeSystem().findType(className);
		} else if(target instanceof ArrayAccessExpr) {
			ExpressionResult nameResult = target.asArrayAccessExpr().getName().accept(this, context);
			context = nameResult.block;
			ptrTemporary = nameResult.temporary;

			ExpressionResult indexResult = target.asArrayAccessExpr().getIndex().accept(this, context);
			context = indexResult.block;
			idxTemporary = indexResult.temporary;


		} else {
			throw new UnsupportedOperationException("cannot assign to unknown type of LHS");
		}

		if(objectType != null) {
			isStaticAssignment = objectType.isStaticField(field);
		}

		//-- now calculate the RHS
		ExpressionResult rightHandSide;
		ExpressionResult combinedRHS;

		// only for normal assignments we don't need to load the LHS
		if(expression.getOperator() == AssignExpr.Operator.ASSIGN) {
			combinedRHS = rightHandSide = expression.getValue().accept(this, context);
		} else {
			ExpressionResult loadedLHS;
			// load/calculate LHS
			if(target instanceof NameExpr && !target.asNameExpr().resolve().isField()) {
				loadedLHS = target.asNameExpr().accept(this, context);
			} else if(target instanceof ArrayAccessExpr) {
				LoadIndex load = new LoadIndex(context.createTemporary(), idxTemporary, ptrTemporary);
				context.instructions.add(load);
				loadedLHS = new ExpressionResult(load.result(), context);
			} else {
				if(isStaticAssignment) {
					LoadGlobal load = new LoadGlobal(objectType, field, context.createTemporary());
					context.instructions.add(load);
					loadedLHS = new ExpressionResult(load.result(), context);
				} else {
					ObjectLoad load = new ObjectLoad(ptrTemporary, objectType, field, context.createTemporary());
					context.instructions.add(load);
					loadedLHS = new ExpressionResult(load.result(), context);
				}
			}

			context = loadedLHS.block;

			rightHandSide = expression.getValue().accept(this, context);
			context = rightHandSide.block;

			switch(expression.getOperator()) {
				case PLUS:
					combinedRHS = binaryExpr(BinaryExpr.Operator.PLUS, loadedLHS, rightHandSide, context);
					break;
				case MULTIPLY:
					combinedRHS = binaryExpr(BinaryExpr.Operator.MULTIPLY, loadedLHS, rightHandSide, context);
					break;
				default:
					throw new UnsupportedOperationException("combined assign operator not yet implemented");
			}

			context = combinedRHS.block;
		}

		//-- and at last assign
		if(target instanceof NameExpr && !target.asNameExpr().resolve().isField()) {
			// simple variables, that are not member variables

			context.localManager().writeByName(((NameExpr) target).getNameAsString(), combinedRHS.temporary);
			return new ExpressionResult(rightHandSide.temporary, context);
		} else if(target instanceof ArrayAccessExpr) {
			context.instructions.add(new StoreIndex(combinedRHS.temporary, idxTemporary, ptrTemporary));
			return new ExpressionResult(rightHandSide.temporary, context);
		} else {
			if(isStaticAssignment) {
				// variables, that are static fields
				context.instructions.add(new StoreGlobal(objectType.getStaticField(field), combinedRHS.temporary));
			} else {
				// variables, that are members
				context.instructions.add(new ObjectStore(ptrTemporary,
				                                         objectType,
				                                         field,
				                                         combinedRHS.temporary));
			}
			return new ExpressionResult(rightHandSide.temporary, context);
		}
	}

	@Override
	public ExpressionResult visit(BinaryExpr n, Block context) {
		BinaryExpr.Operator operator = n.getOperator();
		return binaryExpr(operator, n.getLeft(), n.getRight(), context);
	}

	private ExpressionResult binaryExpr(BinaryExpr.Operator operator, Expression left, Expression right, Block f) {
		if(BinaryOperation.Operator.isSupported(operator)) {
			ExpressionResult tmpL = left.accept(this, f);
			ExpressionResult tmpR = right.accept(this, tmpL.block);

			return binaryExpr(operator, tmpL, tmpR, f);
		} else {
			Temporary logical = f.createTemporary();

			LazyBinaryOperation e = new LazyBinaryOperation(logical,
					LazyBinaryOperation.Operator.fromASTOperator(operator), left, right, f, this);
			return new ExpressionResult(logical, e.outBlock());
		}
	}

	private ExpressionResult binaryExpr(BinaryExpr.Operator operator, ExpressionResult tmpL, ExpressionResult tmpR, Block context) {
		BinaryOperation e = new BinaryOperation(context.createTemporary(),
		                                        BinaryOperation.Operator.fromASTOperator(operator), tmpL.temporary, tmpR.temporary);
		context.instructions.add(e);
		return new ExpressionResult(e.result, context);
	}

	public ExpressionResult visit(UnaryExpr expr, Block context) {
		ExpressionResult operandResult = expr.getExpression().accept(this, context);
		context = operandResult.block;

		UnaryExpr.Operator operator = expr.getOperator();
		if(UnaryOperation.Operator.isSupported(operator)) {
			UnaryOperation e = new UnaryOperation(context.createTemporary(),
			                                      UnaryOperation.Operator.fromASTOperator(operator), operandResult.temporary);

			context.instructions.add(e);
			return new ExpressionResult(e.result, context);
		} else if(expr.getExpression() instanceof NameExpr) {
			// handle ++ and --

			final Temporary ONE = context.getTemporaryForConstant(PrimitiveType.INT, 1);

			String variableName = ((NameExpr) expr.getExpression()).getName().toString();

			Temporary result = context.createTemporary();
			// at post operations this will be operandResult.temporary as opposed to result in pre operations
			Temporary returnValue;

			switch(operator) {
				case PREFIX_INCREMENT:
					context.instructions.add(new Comment("pre-increment"));
					context.instructions.add(new BinaryOperation(result, BinaryOperation.Operator.add, operandResult.temporary, ONE));
					returnValue = result;
					break;

				case POSTFIX_INCREMENT:
					context.instructions.add(new Comment("post-increment"));
					context.instructions.add(new BinaryOperation(result, BinaryOperation.Operator.add, operandResult.temporary, ONE));
					returnValue = operandResult.temporary;
					break;

				case PREFIX_DECREMENT:
					context.instructions.add(new Comment("pre-decrement"));
					context.instructions.add(new BinaryOperation(result, BinaryOperation.Operator.sub, operandResult.temporary, ONE));
					returnValue = result;
					break;

				case POSTFIX_DECREMENT:
					context.instructions.add(new Comment("post-decrement"));
					context.instructions.add(new BinaryOperation(result, BinaryOperation.Operator.sub, operandResult.temporary, ONE));
					returnValue = operandResult.temporary;
					break;

				default:
					throw new UnsupportedOperationException();
			}

			context.localManager().writeByName(variableName, result);
			return new ExpressionResult(returnValue, context);

		} else {
			throw new UnknownLeftHandSideExpression();
		}
	}

	public ExpressionResult visit(ThisExpr n, Block context) {
		return new ExpressionResult(context.localManager().readByName("this"), context);
	}

	public ExpressionResult visit(NameExpr n, Block f) {
		// todo check if the name is actually a field (of `this`)
		if(n.resolve().isField()) {

			String nameOfSurroundingClass = JavaParserFacade.get(new ReflectionTypeSolver()).getTypeOfThisIn(n).asReferenceType().getId();
			ObjectType objectType = f.function.file.typeSystem().findType(nameOfSurroundingClass);

			Instruction load;
			if(objectType.isStaticField(n.getNameAsString())) {
				load = new LoadGlobal(objectType, n.getNameAsString(), f.createTemporary());
			} else {
				load = new ObjectLoad(f.localManager().readByName("this"),
				                      objectType,
				                      n.getNameAsString(),
				                      f.createTemporary());
			}

			f.instructions.add(load);
			return new ExpressionResult(load.result(), f);
		} else {
			return new ExpressionResult(f.localManager().readByName(n.toString()), f);
		}
	}

	@Override
	public ExpressionResult visit(FieldAccessExpr n, Block context) {
		ResolvedType resolvedType = n.getScope().calculateResolvedType();

		if(resolvedType.isArray() && n.getNameAsString().equals("length")) {
			ExpressionResult scopeResult = n.getScope().accept(this, context);
			context = scopeResult.block;

			Temporary result = context.createTemporary();
			context.instructions.add(new ArrayLength(result, scopeResult.temporary));

			return new ExpressionResult(result, context);
		}

		String className = resolvedType.asReferenceType().getId();

		ObjectType type = context.function.file.typeSystem().findType(className);

		if(type.isStaticField(n.getNameAsString())) {
			// access to static fields
			LoadGlobal load = new LoadGlobal(type, n.getNameAsString(), context.createTemporary());
			context.instructions.add(load);

			return new ExpressionResult(load.result(), context);
		} else {
			ExpressionResult scopeResult = n.getScope().accept(this, context);
			context = scopeResult.block;

			// access to member fields
			ObjectLoad load = new ObjectLoad(scopeResult.temporary,
			                                 type,
			                                 n.getNameAsString(),
			                                 context.createTemporary());
			context.instructions.add(load);
			return new ExpressionResult(load.result(), context);
		}
	}

	@Override
	public ExpressionResult visit(VariableDeclarationExpr n, Block context) {

		for(VariableDeclarator v : n.getVariables()) {
			context = v.accept(this, context).block;
		}

		return new ExpressionResult(null, context);
	}

	public ExpressionResult visit(VariableDeclarator v, Block context) {
		Type variableType = context.function.file.typeSystem().getVariableType(v.getType());
		Variable variable = new Variable(v.getNameAsString(),
		                                 variableType,
		                                 context);

		Expression initializer;
		ExpressionResult init;

		if(v.getInitializer().isPresent()) {
			initializer = v.getInitializer().get();
		} else if(variableType instanceof PrimitiveType) {
			initializer = ((PrimitiveType) variableType).defaultInitializer();
		} else {
			// todo add default initialization; until now it was assumed that defaults
			// are provided by the vm
			throw new UnsupportedOperationException("uninitialized variable");
		}

		init = initializer.accept(this, context);
		context = init.block;

		context.localManager().declare(variable, init.temporary);
		return init;
	}

	@Override
	public ExpressionResult visit(BooleanLiteralExpr n, Block context) {
		return new ExpressionResult(context.getTemporaryForConstant(PrimitiveType.BOOL, n.getValue()), context);
	}

	public ExpressionResult visit(IntegerLiteralExpr n, Block f) {
		return new ExpressionResult(f.getTemporaryForConstant(PrimitiveType.INT, Integer.parseInt(n.getValue())), f);
	}

	@Override
	public ExpressionResult visit(CharLiteralExpr n, Block context) {
		return new ExpressionResult(context.getTemporaryForConstant(PrimitiveType.CHAR, n.getValue().charAt(0)), context);
	}

	@Override
	public ExpressionResult visit(LongLiteralExpr n, Block context) {
		String value = n.getValue();
		if(value.toLowerCase().endsWith("l")) {
			value = value.substring(0, value.length()-1);
		}

		return new ExpressionResult(context.getTemporaryForConstant(PrimitiveType.LONG, Long.parseLong(value)), context);
	}

	@Override
	public ExpressionResult visit(DoubleLiteralExpr n, Block context) {
		if(n.getValue().toLowerCase().endsWith("f")) {
			return new ExpressionResult(context.getTemporaryForConstant(PrimitiveType.FLOAT, Float.parseFloat(n.toString())), context);
		} else {
			return new ExpressionResult(context.getTemporaryForConstant(PrimitiveType.DOUBLE, n.asDouble()), context);
		}
	}

	@Override
	public ExpressionResult visit(NullLiteralExpr n, Block context) {
		return new ExpressionResult(context.getTemporaryForConstant(PrimitiveType.VOID, 0), context);
	}

	@Override
	public ExpressionResult visit(MethodCallExpr n, Block context) {
		// todo special calls

		String functionName = n.getNameAsString();
		List<Type> argumentTypes = new ArrayList<>();
		for(Expression expression : n.getArguments()) {
			argumentTypes.add(context.function.file.typeSystem().getVariableType(expression.calculateResolvedType()));
		}

		Temporary thisArgument = null;

		String classNameOfCallee;
		if(n.getScope().isPresent()) {
			try {
				classNameOfCallee = n.getScope().get().calculateResolvedType().asReferenceType().getQualifiedName();
			} catch(UnsolvedSymbolException e) {
				if(n.getScope().get().isNameExpr() && n.getScope().get().asNameExpr().getNameAsString().equals("VirtualMachine")) {
					classNameOfCallee = JavaParserFacade.get(new ReflectionTypeSolver()).getTypeOfThisIn(n).asReferenceType().getQualifiedName();
				} else {
					throw e;
				}
			}
		} else {
			classNameOfCallee = JavaParserFacade.get(new ReflectionTypeSolver()).getTypeOfThisIn(n).asReferenceType().getQualifiedName();
		}

		ObjectType objectType = context.function.file.typeSystem().findType(classNameOfCallee);
		Function function = objectType.getFunctionFromInheritance(functionName, argumentTypes);

		boolean isSpecial = false;

		if(function == null) {
			if(SpecialCall.Builtin.names().contains(functionName)) {
				isSpecial = true;
			} else {
				throw new UndefinedFunctionException("No function " + functionName + " on type " + objectType);
			}
		}

		if(!isSpecial && n.getScope().isPresent()) {
			// resolve scope, then call on the result
			Expression scopeExpression = n.getScope().get();

			if(function.isStatic && scopeExpression instanceof NameExpr) {
				// ignore. we only need to compute those static scopes that may be unpure
			} else {
				ExpressionResult scopeResult = n.getScope().get().accept(this, context);
				thisArgument = scopeResult.temporary;
				context = scopeResult.block;
			}
		}

		if(!isSpecial && !n.getScope().isPresent() && !function.isStatic) {
			thisArgument = context.localManager().readByName("this");
		}

		//// now to the actual calling

		List<Temporary> args = new ArrayList<>();

		if(!isSpecial && !function.isStatic) {
			args.add(thisArgument);
		}

		for(Expression e : n.getArguments()) {
			ExpressionResult expressionResult = e.accept(this, context);
			context = expressionResult.block;
			args.add(expressionResult.temporary);
		}

		Instruction call;
		com.github.javaparser.resolution.types.ResolvedType type = null;

		try {
			type = JavaParserFacade.get(new ReflectionTypeSolver()).getType(n);
		} catch(Exception e) {
			/* ignore */
		}

		if(isSpecial) {
			call = new SpecialCall(SpecialCall.Builtin.fromString(functionName), args);
		} else if(function.isStatic) {
			if(type instanceof ResolvedVoidType) {
				call = new VoidCall(function, args.toArray(new Temporary[]{}));
			} else {
				call = new Call(context.createTemporary(), function, args.toArray(new Temporary[]{}));
			}

		} else {
			if(type instanceof ResolvedVoidType) {
				call = new VoidMemberCall(objectType, function, args);
			} else {
				call = new MemberCall(context.createTemporary(), objectType, function, args);
			}
		}

		context.instructions.add(call);
		return new ExpressionResult(call.result(), context);
	}

	@Override
	public ExpressionResult visit(ReturnStmt n, Block arg) {
		if(n.getExpression().isPresent()) {
			ExpressionResult expressionResult = n.getExpression().get().accept(this, arg);
			expressionResult.block.instructions.add(new Return(expressionResult.temporary));
			return new ExpressionResult(null, expressionResult.block);
		} else {
			arg.instructions.add(new Return());
			return new ExpressionResult(null, arg);
		}
	}

	@Override
	public ExpressionResult visit(ObjectCreationExpr n, Block context) {

		ArrayList<Temporary> arguments = new ArrayList<>();
		List<Type> argumentTypes = new ArrayList<>();

		for(Expression e : n.getArguments()) {
			ExpressionResult r = e.accept(this, context);
			argumentTypes.add(context.function.file.typeSystem().getVariableType(e.calculateResolvedType()));
			arguments.add(r.temporary);
			context = r.block;
		}

		ObjectType type = context.function.file.typeSystem().findType(n.getTypeAsString());
		Function constructor = type.getConstructor(argumentTypes);

		if(constructor == null) {
			throw new NoSuchConstructorException("Constructor for type " + type.name + " not found");
		}

		Allocate a = new Allocate(context.createTemporary(), type);
		context.instructions.add(a);

		arguments.add(0, a.result());

		VoidCall constructorCall = new VoidCall(constructor, arguments.toArray(new Temporary[0]));

		context.instructions.add(constructorCall);

		return new ExpressionResult(a.result(), context);
	}

	@Override
	public ExpressionResult visit(ArrayCreationExpr n, Block context) {
		if(n.getLevels().size() != 1) {
			throw new RuntimeException("ArrayCreationExpr only implemented for a single level");
		}

		if(n.getLevels().get(0).getDimension().isPresent() == n.getInitializer().isPresent()) {
			throw new RuntimeException("ArrayCreationExpr may contain either (but not both) of dimension and initializer");
		}

		ExpressionResult sizeResult;
		List<Temporary> elements = null;
		if(n.getLevels().get(0).getDimension().isPresent()) {
			// no initializer but size
			sizeResult = n.getLevels().get(0).getDimension().get().accept(this, context);
		} else {
			// initializer
			ArrayInitializerExpr initializer = n.getInitializer().get();
			elements = new ArrayList<>(initializer.getValues().size());

			for(Expression x : initializer.getValues()) {
				ExpressionResult result = x.accept(this, context);

				elements.add(result.temporary);
				context = result.block;
			}

			Temporary sizeTemporary = context.getTemporaryForConstant(PrimitiveType.INT, elements.size());
			sizeResult = new ExpressionResult(sizeTemporary, context);
		}

		context = sizeResult.block;

		ArrayType type = new ArrayType(Type.getVariableType(n.getElementType(), context.function.file.typeSystem()));

		New allocator = new New(context.createTemporary(), type, sizeResult.temporary);

		context.instructions.add(allocator);

		if(elements != null) {
			int index = 0;
			for(Temporary t : elements) {
				Temporary indexTemporary = context.getTemporaryForConstant(PrimitiveType.INT, index++);
				context.instructions.add(new StoreIndex(t, indexTemporary, allocator.result()));
			}
		} else {
			// defaults in arrays will be provided by the VM
		}

		return new ExpressionResult(allocator.result(), context);
	}

	@Override
	public ExpressionResult visit(ArrayInitializerExpr n, Block context) {

		List<Temporary> elements = new ArrayList<>(n.getValues().size());
		for(Expression x : n.getValues()) {
			ExpressionResult result = x.accept(this, context);

			elements.add(result.temporary);
			context = result.block;
		}

		if(elements.size() == 0) {
			throw new RuntimeException("Cannot deduce type from empty initializer");
		}

		Type elementType = Type.getVariableType(n.getValues().get(0).calculateResolvedType(), context.function.file.typeSystem());

		Temporary sizeTemporary = context.createTemporary();
		context.instructions.add(new Const(sizeTemporary, PrimitiveType.INT, elements.size()));

		New allocator = new New(context.createTemporary(), new ArrayType(elementType), sizeTemporary);

		context.instructions.add(allocator);

		int index = 0;
		for(Temporary t : elements) {
			Temporary indexTemporary = context.getTemporaryForConstant(PrimitiveType.INT, index++);
			context.instructions.add(new StoreIndex(t, indexTemporary, allocator.result()));
		}

		return new ExpressionResult(allocator.result(), context);
	}

	@Override
	public ExpressionResult visit(ArrayAccessExpr n, Block context) {
		ExpressionResult nameResult = n.getName().accept(this, context);
		context = nameResult.block;

		ExpressionResult indexResult = n.getIndex().accept(this, context);
		context = indexResult.block;

		Temporary result = context.createTemporary();
		context.instructions.add(new LoadIndex(result, indexResult.temporary, nameResult.temporary));

		return new ExpressionResult(result, context);
	}

	@Override
	public ExpressionResult visit(CastExpr n, Block context) {
		if(n.getExpression() instanceof IntegerLiteralExpr) {

			PrimitiveType type = PrimitiveType.getType(n.getType());
			String value = n.getExpression().asIntegerLiteralExpr().getValue();
			Temporary t;

			if(type == PrimitiveType.BYTE) {
				t = context.getTemporaryForConstant(type, Byte.parseByte(value));
			} else if(type == PrimitiveType.CHAR) {
				t = context.getTemporaryForConstant(type, value.charAt(0));
			} else if(type == PrimitiveType.SHORT) {
				t = context.getTemporaryForConstant(type, Short.parseShort(value));
			} else if(type == PrimitiveType.INT) {
				t = context.getTemporaryForConstant(type, Integer.parseInt(value));
			} else {
				throw new TypeNotSupportedException(type.toString());
			}

			return new ExpressionResult(t, context);
		} else if(n.getExpression() instanceof DoubleLiteralExpr) {
			if(PrimitiveType.getType(n.getType()) == PrimitiveType.FLOAT) {
				return new ExpressionResult(context.getTemporaryForConstant(
						PrimitiveType.FLOAT,
						(float) n.getExpression().asDoubleLiteralExpr().asDouble()),
				                            context
				);
			} else {
				throw new TypeNotSupportedException(PrimitiveType.getType(n.getType()).toString());
			}
		}

		throw new UnsupportedOperationException("Casts are only supported for integer and float literals!");
	}

	@Override
	public ExpressionResult visit(ExplicitConstructorInvocationStmt n, Block context) {

		ArrayList<Temporary> arguments = new ArrayList<>();
		List<Type> argumentTypes = new ArrayList<>();

		for(Expression e : n.getArguments()) {
			ExpressionResult r = e.accept(this, context);
			argumentTypes.add(context.function.file.typeSystem().getVariableType(e.calculateResolvedType()));
			arguments.add(r.temporary);
			context = r.block;
		}

		ObjectType type;
		if(n.isThis()) {
			type = context.function.type();
		} else {
			// is super
			type = context.function.type().parent();
		}

		Function constructor = type.getConstructor(argumentTypes);

		if(constructor == null) {
			throw new NoSuchConstructorException("Constructor for type " + type.name + " not found");
		}

		arguments.add(0, context.localManager().readByName("this"));

		VoidCall constructorCall = new VoidCall(constructor, arguments.toArray(new Temporary[0]));

		context.instructions.add(constructorCall);

		return new ExpressionResult(null, context);
	}
}
