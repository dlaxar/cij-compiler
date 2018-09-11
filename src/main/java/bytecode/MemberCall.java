package bytecode;

import bytecode.type.ObjectType;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class MemberCall implements Instruction {
	private final Temporary result;
	private final ObjectType objectType;
	private final Function function;
	private final List<Temporary> args;

	public MemberCall(Temporary result, ObjectType objectType, Function function, List<Temporary> args) {
		this.result = result;
		this.objectType = objectType;
		this.function = function;
		this.args = args;
	}

	@Override
	public String toString() {
		String argString = Arrays.toString(args.toArray()).replaceAll("\\[|\\]", "");
		return String.format("%s = call %s.%s(%s)", result, objectType, function.localIndex(), argString);
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.MEMBER_CALL);
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
