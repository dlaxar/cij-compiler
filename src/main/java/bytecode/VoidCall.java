package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class VoidCall implements Instruction {
	private final Temporary[] args;
	private final Function function;

	public VoidCall(Function function, Temporary ...args) {
		super();
		this.function = function;
		this.args = args;
	}

	@Override
	public String toString() {
		String argString = Arrays.toString(args).replaceAll("\\[|\\]", "");
		return String.format("call void %s(%s) [function index %s]", function.name(), argString, function.index());
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		if(function == null) {
			throw new InvalidCompileOrderException("Need to link functions first");
		}

		dos.writeByte(Opcodes.VOID_CALL);

		dos.writeShort(function.index());

		// writes args
		dos.writeShort(args.length);
		for(Temporary temp : args) {
			dos.writeShort(temp.index());
		}

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		for(int i = 0; i < args.length; i++) {
			args[i] = substitute.getOrDefault(args[i], args[i]);
		}
	}
}
