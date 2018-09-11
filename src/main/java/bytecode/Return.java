package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Return implements Instruction {

	private Temporary value = null;

	public Return() {
	}

	public Return(Temporary value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if(value == null) {
			return "return void";
		} else {
			return "return " + value.toString();
		}
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		if(value == null) {
			dos.writeByte(Opcodes.RETURN_VOID);
		} else {
			dos.writeByte(Opcodes.RETURN);
			dos.writeShort(value.index());
		}

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		if(value != null) {
			value = substitute.getOrDefault(value, value);
		}
	}
}
