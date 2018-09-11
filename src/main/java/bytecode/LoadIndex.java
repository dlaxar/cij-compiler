package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class LoadIndex implements Instruction {

	private final Temporary result;
	public Temporary index;
	private Temporary memory;

	public LoadIndex(Temporary result, Temporary index, Temporary memory) {
		this.result = result;
		this.index = index;
		this.memory = memory;
	}

	@Override
	public String toString() {
		return String.format("%s = loadidx %s[%s]", result, memory, index);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.LOAD_INDEX);

		dos.writeShort(memory.index());
		dos.writeShort(index.index());
		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		index = substitute.getOrDefault(index, index);
		memory = substitute.getOrDefault(memory, memory);
	}
}
