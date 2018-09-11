package bytecode.type;

import bytecode.InvalidCompileOrderException;
import bytecode.type.system.TypeSystem;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import exceptions.TypeNotSupportedException;


public abstract class Type {

	public final String name;

	public Type(String name) {
		this.name = name;
	}

	public static Type getVariableType(com.github.javaparser.ast.type.Type type, TypeSystem typeSystem) {
		if(type.isVoidType()) {
			return PrimitiveType.VOID;
		} else if(type.isPrimitiveType()) {
			return PrimitiveType.getType(type);
		} else if(type.isArrayType()) {
			return new ArrayType(getVariableType(type.getElementType(), typeSystem));
		} else {
			return PrimitiveType.getPointerType(typeSystem.findType(type.asClassOrInterfaceType().getNameAsString()));
		}
	}

	public static Type getVariableType(ResolvedType resolvedType, TypeSystem typeSystem) {
		if(resolvedType.isPrimitive()) {
			return PrimitiveType.getType(resolvedType.asPrimitive());
		} else if(resolvedType.isArray()) {
			return new ArrayType(getVariableType(resolvedType.asArrayType().getComponentType(), typeSystem));
		} else {
			return PrimitiveType.getPointerType(typeSystem.findType(resolvedType.asReferenceType().getQualifiedName()));
		}
	}

	public static Expression getDefaultInitializerExpression(Type type) {
		if(type instanceof PrimitiveType) {
			return PrimitiveType.getDefaultInitializerExpression((PrimitiveType) type);
		} else if(type instanceof ArrayType) {
			return new NullLiteralExpr();
		} else {
			throw new InvalidTypeException();
		}
	}

	public abstract byte getRepresentation() throws InvalidCompileOrderException;

	public abstract boolean isSpecialOf(Type type);
}
