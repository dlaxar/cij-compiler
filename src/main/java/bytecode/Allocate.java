package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Allocate implements Instruction {
	public final Temporary ptr;
	private final ObjectType objectType;

	public Allocate(Temporary ptr, ObjectType objectType) {

		this.ptr = ptr;
		this.objectType = objectType;
	}

	@Override
	public String toString() {
		return String.format("%s = allocate %s", ptr, objectType);
	}

	@Override
	public Temporary result() {
		return ptr;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.ALLOCATE);
		dos.writeByte(objectType.getRepresentation());
		dos.annotate(toString());
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		// todo implement
//		throw new UnsupportedOperationException();
	}
}
