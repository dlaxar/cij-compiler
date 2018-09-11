package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class VoidMemberCall implements Instruction {

	private final ObjectType objectType;
	private final Function function;
	private final List<Temporary> args;

	public VoidMemberCall(ObjectType objectType, Function function, List<Temporary> args) {
		this.objectType = objectType;
		this.function = function;
		this.args = args;
	}

	@Override
	public String toString() {
		String argString = Arrays.toString(args.toArray()).replaceAll("\\[|\\]", "");
		return String.format("call void %s.%s(%s)", objectType, function.localIndex(), argString);
	}

	@Override
	public Temporary result() {
		return null;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.VOID_MEMBER_CALL);
		dos.writeByte(function.localIndex());

		dos.writeShort(args.size());
		for(Temporary temp : args) {
			dos.writeShort(temp.index());
		}

		dos.annotate(toString());
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		ListIterator<Temporary> listIterator = args.listIterator();
		while(listIterator.hasNext()) {
			Temporary t = listIterator.next();
			listIterator.set(substitute.getOrDefault(t, t));
		}
	}
}
