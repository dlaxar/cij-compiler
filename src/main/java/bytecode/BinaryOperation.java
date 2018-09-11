package bytecode;

import com.github.javaparser.ast.expr.BinaryExpr;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;


public class BinaryOperation implements Instruction {

	private final Operator operator;
	private Temporary tmpL;
	private Temporary tmpR;
	public Temporary result;

	public BinaryOperation(Temporary result, Operator operator, Temporary tmpL, Temporary tmpR) {
		this.result = result;
		this.operator = operator;
		this.tmpL = tmpL;
		this.tmpR = tmpR;
	}

	public enum Operator {
		add("+", Opcodes.ADD),
		sub("-", Opcodes.SUB),
		mul("*", Opcodes.MUL),
		div("/", Opcodes.DIV),
		mod("mod", Opcodes.MOD),
		gt(">", Opcodes.GT),
		gte(">=", Opcodes.GTE),
		eq("==", Opcodes.EQ),
		neq("!=", Opcodes.NEQ),
		lte("<=", Opcodes.LTE),
		lt("<", Opcodes.LT),
		bitwiseAnd("&", Opcodes.LOGICAL_AND),
		bitwiseOr("|", Opcodes.LOGICAL_OR)
		;

		private final String symbol;
		private final byte opcode;

		public static Operator fromASTOperator(BinaryExpr.Operator operator) {
			switch(operator) {
				case PLUS:
					return add;
				case MINUS:
					return sub;
				case MULTIPLY:
					return mul;
				case DIVIDE:
					return div;
				case REMAINDER:
					return mod;
				case GREATER:
					return gt;
				case GREATER_EQUALS:
					return gte;
				case EQUALS:
					return eq;
				case NOT_EQUALS:
					return neq;
				case LESS_EQUALS:
					return lte;
				case LESS:
					return lt;
				case BINARY_AND:
					return bitwiseAnd;
				case BINARY_OR:
					return bitwiseOr;
				default:
					throw new UnsupportedOperationException();
			}
		}

		Operator(String symbol, byte opcode) {
			this.symbol = symbol;
			this.opcode = opcode;
		}

		public static boolean isSupported(BinaryExpr.Operator operator) {
			try {
				fromASTOperator(operator);
				return true;
			} catch(UnsupportedOperationException e) {
				return false;
			}
		}

		@Override
		public String toString() {
			return symbol;
		}
	}

	@Override
	public String toString() {
		return String.format("%s = %s %s, %s", result, operator.name(), tmpL, tmpR);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(operator.opcode);
		dos.writeShort(tmpL.index());
		dos.writeShort(tmpR.index());
		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		tmpL = substitute.getOrDefault(tmpL, tmpL);
		tmpR = substitute.getOrDefault(tmpR, tmpR);
	}
}
