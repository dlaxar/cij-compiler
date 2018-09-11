package bytecode.type;

import bytecode.Function;
import bytecode.InvalidCompileOrderException;
import bytecode.Variable;
import bytecode.type.system.TypeSystem;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import exceptions.InvalidFieldException;

import java.util.*;
import java.util.stream.Collectors;

public class ObjectType extends Type {

	public class Field {
		public final String name;
		public final Type type;
		public final Expression initializer;

		public short index = -1;

		public Field(String name, Type type, Expression initializer) {
			this.name = name;
			this.type = type;
			if(initializer != null) {
				this.initializer = initializer;
			} else {
				this.initializer = Type.getDefaultInitializerExpression(type);
			}
		}
	}

	private ObjectType parent;

	public List<Field> staticFields = new ArrayList<>();
	public List<Field> fields = new ArrayList<>();

	public List<Function> staticFunctions = new ArrayList<>();
	public List<Function> virtualMethods = new ArrayList<>();

	private final ClassOrInterfaceDeclaration classDeclaration;
	private byte id = -1;
	private boolean sortedFields = false;
	private boolean sortedFunctions = false;

	public ObjectType(String name, ClassOrInterfaceDeclaration classDeclaration) {
		super(name);
		this.classDeclaration = classDeclaration;

		if(classDeclaration.isInterface()) {
			throw new InvalidTypeException();
		}
	}

	public void setParent(ObjectType type) {
		this.parent = type;
	}

	public boolean hasParent() {
		return parent != null;
	}


	public ObjectType parent() {
		return parent;
	}

	public void setId(byte i) {
		this.id = i;
	}

	/**
	 * Adds a static field
	 *
	 * @param declaration the AST field declaration
	 * @throws InvalidCompileOrderException
	 */
	public void addStaticField(FieldDeclaration declaration, TypeSystem typeSystem) throws InvalidCompileOrderException {
		if(sortedFields) {
			throw new InvalidCompileOrderException("Cannot add fields after they've been sorted");
		}

		for(VariableDeclarator variable : declaration.getVariables()) {
			staticFields.add(new Field(variable.getNameAsString(),
			                           typeSystem.getVariableType(variable.getType()),
			                           variable.getInitializer().orElse(null)));
		}
	}

	/**
	 * Adds a member (i.e. non-static) field
	 *
	 * @param declaration the AST field declaration
	 * @throws InvalidCompileOrderException
	 */
	public void addField(FieldDeclaration declaration, TypeSystem typeSystem) throws InvalidCompileOrderException {
		if(sortedFields) {
			throw new InvalidCompileOrderException("Cannot add fields after they've been sorted");
		}

		for(VariableDeclarator variable : declaration.getVariables()) {
			fields.add(new Field(variable.getNameAsString(),
			                     typeSystem.getVariableType(variable.getType()),
			                     variable.getInitializer().orElse(null)));
		}
	}

	public void addStaticFunction(Function f) throws InvalidCompileOrderException {
		if(sortedFunctions) {
			throw new InvalidCompileOrderException("Cannot add functions after they've been sorted");
		}
		staticFunctions.add(f);
	}

	public void addVirtual(Function f) throws InvalidCompileOrderException {
		if(sortedFunctions) {
			throw new InvalidCompileOrderException("Cannot add function after they've been sorted");
		}
		virtualMethods.add(f);
	}

	@Override
	public String toString() {
		return "type:" + name;
	}

	@Override
	public byte getRepresentation() throws InvalidCompileOrderException {
		if(id == -1) {
			throw new InvalidCompileOrderException("Must number object types first");
		}
		return id;
	}

	public ClassOrInterfaceDeclaration getDeclaration() {
		return classDeclaration;
	}

	public byte getId() {
		return id;
	}

	/**
	 * Reorders fields into their final position
	 *
	 * e.g. packing for best usage of space
	 */
	public void sortFields() {
		if(sortedFields) {
			return;
		}

		if(parent != null) {
			parent.sortFields();
		}

		short index = 0;
		if(hasParent()) {
			try {
				index = (short) parent.getFields().size();
			} catch(InvalidCompileOrderException e) { e.printStackTrace(); /* can't ever happen */}
		}
		for(Field f : fields) {
			f.index = index++;
		}

		sortedFields = true;
	}

	public List<Field> getStaticFields() throws InvalidCompileOrderException {
		if(!sortedFields) {
			throw new InvalidCompileOrderException("Cannot fetch fields unless they've been sorted first");
		}

		return staticFields;
	}

	public List<Field> getFields() throws InvalidCompileOrderException {
		if(!sortedFields) {
			throw new InvalidCompileOrderException("Cannot fetch fields unless they've been sorted first");
		}

		List<Field> all = new ArrayList<>();
		if(hasParent()) {
			all.addAll(parent.getFields());
		}

		all.addAll(fields);

		return all;
	}

	public Map<ObjectType.Field, Expression> getOwnFieldInitializers() throws InvalidCompileOrderException {
		if(!sortedFields) {
			throw new InvalidCompileOrderException("Cannot fetch initializers unless fields have been sorted first");
		}

		return new HashMap<>(fields.stream().collect(Collectors.toMap(
				java.util.function.Function.identity(),
				f -> f.initializer
		)));
	}


	public short getFieldIndex(String name) throws InvalidCompileOrderException {
		// iterate reverse because if a subclass has a field the same name as a field
		// in the superclass we need to return the index of the field in the subclass

		List<Field> fields = getFields();
		ListIterator<Field> fieldListIterator = fields.listIterator(fields.size());

		while(fieldListIterator.hasPrevious()) {
			Field f = fieldListIterator.previous();
			if(name.equals(f.name)) {
				return f.index;
			}
		}

		throw new InvalidFieldException(name, this);
	}

	public boolean isFieldStatic(String name) {
		for(Field f : staticFields) {
			if(f.name.equals(name)) {
				return true;
			}
		}

		return false;
	}

	public short getStaticFieldIndex(String name) throws InvalidCompileOrderException {
		for(Field f : getStaticFields()) {
			if(name.equals(f.name)) {
				return f.index;
			}
		}

		throw new InvalidFieldException(name, this);
	}

	public boolean isStaticField(String name) {
		return tryGetStaticField(name) != null;
	}

	private Field tryGetStaticField(String name) {
		for(Field f : staticFields) {
			if(f.name.equals(name)) {
				return f;
			}
		}

		if(hasParent()) {
			return parent.tryGetStaticField(name);
		}

		return null;
	}

	public Field getStaticField(String name) {
		Field f = tryGetStaticField(name);
		if(f == null) {
			throw new InvalidFieldException(name, this);
		}

		return f;
	}

	private boolean signatureMatches(String name, List<Type> argTypes, boolean isMember, Function f) {
		if(!f.name().equals(name)) {
			return false;
		}

		if(argTypes.size() != f.parameters.size()) {
			return false;
		}

		if(isMember == f.isStatic) {
			return false;
		}

		ListIterator<Type> typeListIterator = argTypes.listIterator();
		ListIterator<Variable> parameterListIterator = f.parameters.listIterator();

		while(typeListIterator.hasNext() && parameterListIterator.hasNext()) {
			Type typeFromArgList = typeListIterator.next();
			Type typeFromSignatureOfF = parameterListIterator.next().type;
			if(!typeFromArgList.isSpecialOf(typeFromSignatureOfF)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Finds a function that matches both name and argument list (careful with regard
	 * to the `this` argument)
	 *
	 * @param name the function name
	 * @param argumentTypes expects only the actual arguments but no `this` argument
	 * @return
	 */
	public Function getFunction(String name, List<Type> argumentTypes) {
		for(Function f : staticFunctions) {
			if(signatureMatches(name, argumentTypes, false, f)) {
				return f;
			}
		}

		List<Type> copyOfArgTypes = new ArrayList<>(argumentTypes);
		copyOfArgTypes.add(0, PrimitiveType.getPointerType(this));
		for(Function f : virtualMethods) {
			if(signatureMatches(name, copyOfArgTypes, true, f)) {
				return f;
			}
		}

		return null;
	}

	public Function getFunctionFromInheritance(String name, List<Type> argumentTypes) {
		Function function = getFunction(name, argumentTypes);
		if(function != null) {
			return function;
		} else if(hasParent()) {
			return parent().getFunctionFromInheritance(name, argumentTypes);
		} else {
			return null;
		}
	}

	/**
	 * `this` argument is included in argumentTypes
	 *
	 * @param name
	 * @param argumentTypes
	 * @return
	 */
	public Function getMemberFunction(String name, List<Type> argumentTypes) {
		for(Function f : virtualMethods) {
			if(signatureMatches(name, argumentTypes, true, f)) {
				return f;
			}
		}

		return null;
	}

	public Function getConstructor(List<Type> argumentTypes) {
		List<Type> copyOfArgTypes = new ArrayList<>(argumentTypes);
		copyOfArgTypes.add(0, PrimitiveType.getPointerType(this));
		for(Function f : staticFunctions) {
			if(signatureMatches(name, copyOfArgTypes, false, f)) {
				return f;
			}
		}

		return null;
	}

	public List<Function> getVirtualFunctions() {
		List<Function> all = new ArrayList<>();

		if(hasParent()) {
			all.addAll(parent.getVirtualFunctions());
		}

		for(Function f : virtualMethods) {
			if(all.size() == f.localIndex()) {
				all.add(f);
			} else {
				all.set(f.localIndex(), f);
			}
		}

		return all;
	}

	public void sortFunctions() {
		if(sortedFunctions) {
			return;
		}


		byte index;
		if(hasParent()) {
			parent.sortFunctions();
			index = (byte) parent.getVirtualFunctions().size();
		} else {
			index = 0;
		}

		for(Function f : virtualMethods) {
			Function overridenFunction = null;
			if(hasParent()) {
				List<Type> types = f.parameters.stream().map(v -> v.type).collect(Collectors.toList());
				overridenFunction = parent.getMemberFunction(f.name(), types);
			}

			if(overridenFunction != null) {
				f.setLocalIndex(overridenFunction.localIndex());
			} else {
				f.setLocalIndex(index++);
			}
		}

		sortedFunctions = true;
	}

	@Override
	public boolean isSpecialOf(Type type) {
		if(this.equals(type)) {
			return true;
		}

		if(hasParent()) {
			return parent.isSpecialOf(type);
		} else {
			return false;
		}
	}
}
