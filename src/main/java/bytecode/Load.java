package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Load implements Instruction {

	public final Temporary result;
	private final Variable variable;

	public Load(Temporary result, Variable variable) {
		this.result = result;
		this.variable = variable;
	}

	@Override
	public String toString() {
		return String.format("%s = load $%s", result, variable.name);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.LOAD);
		dos.writeShort(variable.index());
		dos.annotate(toString());
	}

	public Variable variable() {
		return variable;
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {}
}
