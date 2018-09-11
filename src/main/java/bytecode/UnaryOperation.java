package bytecode;

import com.github.javaparser.ast.expr.UnaryExpr;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class UnaryOperation implements Instruction {
	public final Temporary result;
	private final Operator operator;
	private Temporary tmp;

	public UnaryOperation(Temporary result, Operator operator, Temporary tmp) {
		super();
		this.result = result;
		this.operator = operator;
		this.tmp = tmp;
	}

	public enum Operator {
		neg("-", Opcodes.NEG),
		not("!", Opcodes.NOT);

		private final String symbol;
		private final byte opcode;

		public static UnaryOperation.Operator fromASTOperator(UnaryExpr.Operator operator) {
			switch(operator) {
				case MINUS:
					return neg;
				case LOGICAL_COMPLEMENT:
					return not;
				default:
					throw new UnsupportedOperationException();
			}
		}

		Operator(String symbol, byte opcode) {
			this.symbol = symbol;
			this.opcode = opcode;
		}

		public static boolean isSupported(UnaryExpr.Operator operator) {
			try {
				fromASTOperator(operator);
				return true;
			} catch(UnsupportedOperationException e) {
				return false;
			}
		}
	}

	@Override
	public String toString() {
		return String.format("%s = %s %s", result, operator.name(), tmp);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(operator.opcode);
		dos.writeShort(tmp.index());

		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		tmp = substitute.getOrDefault(tmp, tmp);
	}
}
