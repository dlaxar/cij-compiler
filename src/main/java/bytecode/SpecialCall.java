package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.*;

public class SpecialCall implements Instruction {

	public enum Builtin {
		BENCHMARK_START((byte) 0, "benchmark_start"),
		BENCHMARK_END((byte) 1, "benchmark_end"),
		PRINT_FLOAT((byte) 2, "print_float"),
		PRINT_ARRAY_INT((byte) 3, "print_array_int"),
		PRINT_DOUBLE((byte) 4, "print_double"),
		EXIT((byte)5, "exit");

		public final byte opcode;
		private final String name;

		Builtin(byte opcode, String name) {
			this.opcode = opcode;
			this.name = name;
		}

		private static Map<String, Builtin> all() {
			HashMap<String, Builtin> x = new HashMap<>();

			for(Builtin b : Builtin.class.getEnumConstants()) {
				x.put(b.name, b);
			}

			return x;
		}

		public static Builtin fromString(String name) {
			return all().get(name);
		}

		public static Set<String> names() {
			return all().keySet();
		}
	}

	private final Builtin function;
	private final List<Temporary> args;

	public SpecialCall(Builtin function, List<Temporary> args) {
		super();
		this.function = function;
		this.args = args;
	}

	@Override
	public String toString() {
		String argString = Arrays.toString(args.toArray(new Temporary[0])).replaceAll("\\[|\\]", "");
		return String.format("specialcall void %s(%s)", function.name, argString);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.VOID_SPECIAL_CALL);
		dos.writeByte(function.opcode);
		dos.writeShort(args.size());
		for(Temporary t : args) {
			dos.writeShort(t.index());
		}

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
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
