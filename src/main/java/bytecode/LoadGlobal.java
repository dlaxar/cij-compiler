package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class LoadGlobal implements Instruction {

	private final ObjectType objectType;
	private final String field;
	private final Temporary result;

	public LoadGlobal(ObjectType objectType, String field, Temporary result) {

		this.objectType = objectType;
		this.field = field;
		this.result = result;
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.LOAD_GLOBAL);
		dos.writeShort(objectType.getStaticFieldIndex(field));

		dos.annotate(result + " = load_global " + objectType.getStaticFieldIndex(field));
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {

	}
}
