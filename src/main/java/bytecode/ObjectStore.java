package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class ObjectStore implements Instruction {

	private Temporary ptr;
	private final ObjectType objectType;
	private final String field;
	private Temporary value;

	public ObjectStore(Temporary ptr, ObjectType objectType, String field, Temporary value) {

		this.ptr = ptr;
		this.objectType = objectType;
		this.field = field;
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("store %s.%s = %s", ptr, field, value);
	}

	@Override
	public Temporary result() {
		return null;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.STORE_OBJ);
		dos.writeShort(ptr.index());
		dos.writeByte(objectType.getRepresentation());
		dos.writeByte(objectType.getFieldIndex(field));
		dos.writeShort(value.index());

		dos.annotate(toString());
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		ptr = substitute.getOrDefault(ptr, ptr);
		value = substitute.getOrDefault(value, value);
	}
}
