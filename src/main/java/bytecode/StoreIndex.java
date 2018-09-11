package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class StoreIndex implements Instruction {

	public Temporary value;
	public Temporary index;
	private Temporary memory;

	public StoreIndex(Temporary value, Temporary index, Temporary memory) {
		this.value = value;
		this.index = index;
		this.memory = memory;
	}

	@Override
	public String toString() {
		return String.format("storeidx %s[%s], %s", memory, index, value);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.STORE_INDEX);

		dos.writeShort(memory.index());
		dos.writeShort(index.index());
		dos.writeShort(value.index());

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		value = substitute.getOrDefault(value, value);
		index = substitute.getOrDefault(index, index);
		memory = substitute.getOrDefault(memory, memory);
	}
}
