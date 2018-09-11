package bytecode;

import bytecode.type.ArrayType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class New implements Instruction {
	public Temporary reference;
	private final ArrayType type;
	private Temporary size;

	public New(Temporary reference, ArrayType type, Temporary size) {
		this.reference = reference;

		this.type = type;
		this.size = size;
	}

	@Override
	public String toString() {
		return String.format("%s = new %s[%s]", reference, type, size);
	}

	/**
	 * to = new(type, size)
	 * [8:NEW][16:toidx][type][16:sizeidx]
	 *
	 * @param dos stream
	 * @throws IOException on failure
	 */
	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.NEW);

		dos.writeByte(type.baseType.getRepresentation());
		dos.writeShort(size.index());
		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return reference;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		size = substitute.getOrDefault(size, size);
		reference = substitute.getOrDefault(reference, reference);
	}
}
