package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Store implements Instruction {

	public Temporary value;
	private final Variable variable;

	public Store(Temporary value, Variable variable) {
		this.value = value;
		this.variable = variable;
	}

	@Override
	public String toString() {
		return String.format("store $%s, %s", variable.name, value);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.STORE);

		dos.writeShort(variable.index());
		dos.writeShort(value.index());

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
	}

	public Temporary value() {
		return value;
	}

	public Variable variable() {
		return variable;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		value = substitute.getOrDefault(value, value);
	}
}
