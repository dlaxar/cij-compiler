package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class StoreGlobal implements Instruction {

	private final ObjectType.Field field;
	private final Temporary value;

	public StoreGlobal(ObjectType.Field field, Temporary value) {
		this.field = field;
		this.value = value;
	}

	@Override
	public Temporary result() {
		return null;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.STORE_GLOBAL);
		dos.writeShort(field.index);
		dos.writeShort(value.index());

		dos.annotate("store_global " + field.index + " = " + value);
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {

	}
}
