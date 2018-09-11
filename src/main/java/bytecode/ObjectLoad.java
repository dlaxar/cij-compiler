package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class ObjectLoad implements Instruction {

	private Temporary ptr;
	private final ObjectType objectType;
	private final String field;
	private final Temporary result;

	public ObjectLoad(Temporary ptr, ObjectType objectType, String field, Temporary result) {

		this.ptr = ptr;
		this.objectType = objectType;
		this.field = field;
		this.result = result;
	}

	@Override
	public String toString() {
		return String.format("%s = load %s.%s", result, ptr, field);
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.LOAD_OBJ);
		dos.writeShort(ptr.index());
		dos.writeByte(objectType.getRepresentation());
		dos.writeByte(objectType.getFieldIndex(field));

		dos.annotate(toString());
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		ptr = substitute.getOrDefault(ptr, ptr);
	}
}
