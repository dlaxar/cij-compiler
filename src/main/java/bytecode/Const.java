package bytecode;

import bytecode.type.PrimitiveType;
import bytecode.type.Type;
import com.sun.org.apache.xpath.internal.operations.Bool;
import exceptions.TypeNotSupportedException;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Const<T> implements Instruction {

	private Temporary result;
	public final Type type;
	public final T value;

	public Const(Temporary result, Type type, T value) {
		this.result = result;
		this.type = type;
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("%s = const:%s %s", result, type, value);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.CONST);

		dos.writeByte(type.getRepresentation());

		if(type == PrimitiveType.BOOL) {
			dos.writeByte(((Boolean) value) ? 1 : 0);
		} else if(type == PrimitiveType.BYTE) {
			dos.writeByte((Byte) value);
		} else if(type == PrimitiveType.CHAR) {
			dos.writeShort((Character) value);
		} else if(type == PrimitiveType.SHORT) {
			dos.writeShort((Short) value);
		} else if(type == PrimitiveType.INT) {
			dos.writeInt((Integer) value);
		} else if(type == PrimitiveType.LONG) {
			dos.writeLong((Long) value);
		} else if(type == PrimitiveType.FLOAT) {
			dos.writeFloat((Float) value);
		} else if(type == PrimitiveType.DOUBLE) {
			dos.writeDouble((Double) value);
		} else if(type == PrimitiveType.VOID) {
			/* empty value field */
		} else {
			throw new TypeNotSupportedException(type.toString());
		}
		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Const && ((Const) obj).type == type && ((Const) obj).value == value;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {}
}
