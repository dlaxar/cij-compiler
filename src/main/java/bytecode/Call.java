package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class Call implements Instruction {
	public final Temporary result;
	private final Temporary[] args;
	private Function function = null;

	public Call(Temporary result, Function function, Temporary ...args) {
		super();
		this.result = result;
		this.function = function;
		this.args = args;
	}

	@Override
	public String toString() {
		String argString = Arrays.toString(args).replaceAll("\\[|\\]", "");
		return String.format("%s = call %s(%s) [function index %s]", result, function.name(), argString, function.index());
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		if(function == null) {
			throw new InvalidCompileOrderException("Need to link functions first");
		}

		dos.writeByte(Opcodes.CALL);

		dos.writeShort(function.index());

		// writes args
		dos.writeShort(args.length);
		for(Temporary temp : args) {
			dos.writeShort(temp.index());
		}

		dos.annotate(toString());
	}

	public String calledFunctionName() {
		return function.name();
	}

	@Override
	public Temporary result() {
		return result;
	}

	public void setFunction(Function function) {
		this.function = function;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		for(int i = 0; i < args.length; i++) {
			args[i] = substitute.getOrDefault(args[i], args[i]);
		}
	}
}
