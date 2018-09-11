package bytecode.type;

import bytecode.InvalidCompileOrderException;

public class ArrayType extends Type {
	public final Type baseType;

	public ArrayType(Type baseType) {
		super(baseType.toString() + "[]");
		this.baseType = baseType;
	}

	@Override
	public byte getRepresentation() throws InvalidCompileOrderException {
		return (byte) ((1 << 7) | baseType.getRepresentation());
	}

	@Override
	public boolean isSpecialOf(Type type) {
		return type instanceof ArrayType && baseType.isSpecialOf(((ArrayType) type).baseType);
	}

	@Override
	public String toString() {
		return baseType + "[]";
	}
}
