package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Goto implements Instruction {

	private Block label;

	public Goto(Block end) {

		label = end;
	}

	@Override
	public String toString() {
		try {
			return "goto block " + label.index();
		} catch(InvalidCompileOrderException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.GOTO);
		dos.writeShort(label.index());
		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
	}

	public Block label() {
		return label;
	}

	public void setLabel(Block label) {
		this.label = label;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {}
}
