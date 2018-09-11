package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class ArrayLength implements Instruction {

	public final Temporary result;
	private Temporary array;

	public ArrayLength(Temporary result, Temporary array) {
		this.result = result;
		this.array = array;
	}

	@Override
	public String toString() {
		return String.format("%s = length %s", result, array);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.LENGTH);
		dos.writeShort(array.index());

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> useActual) {
		array = useActual.getOrDefault(array, array);
	}
}
