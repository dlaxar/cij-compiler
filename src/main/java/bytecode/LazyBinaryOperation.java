package bytecode;

import bytecode.type.PrimitiveType;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import stream.AnnotatedDataOutput;
import visitors.ExpressionResult;
import visitors.ExpressionVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LazyBinaryOperation {

	private final Operator operator;
	private final Expression left;
	private final Expression right;
	private Block end;
	public Temporary result;

	public LazyBinaryOperation(Temporary result, Operator operator, Expression left, Expression right, Block arg, ExpressionVisitor bodyVisitor) {

		this.result = result;
		this.operator = operator;
		this.left = left;
		this.right = right;

		ExpressionResult tmpL, tmpR;

		Block context = arg;

		if(this.operator == Operator.and) {

			context.instructions.add(new Comment("Begin of &&"));

			// evaluate the left expression
			tmpL = left.accept(bodyVisitor, context);
			context = tmpL.block;

			UnaryOperation notA = new UnaryOperation(new Temporary(context), UnaryOperation.Operator.not, tmpL.temporary);
			context.instructions.add(notA);

			Block setToFalse = new Block(context);

			// if the LEFT condition is false then (we are lazy) and jump to the setToFalse block
			ConditionalGoto conditionalGotoFalse = new ConditionalGoto(notA.result, context, setToFalse);
			context.instructions.add(conditionalGotoFalse);

			// create a new block (because jumps are the last thing in a block)
			Block evaluateRightBlock = conditionalGotoFalse.elze;

			// if we haven't jumped we now evaluate the right expression
			tmpR = right.accept(bodyVisitor, evaluateRightBlock);
			evaluateRightBlock = tmpR.block;

			// negate the result of the right expression
			UnaryOperation notB = new UnaryOperation(evaluateRightBlock.createTemporary(), UnaryOperation.Operator.not, tmpR.temporary);
			evaluateRightBlock.instructions.add(notB);

			// if the RIGHT condition is false as well then jump to the setToFalse block as well
			ConditionalGoto conditionalGotoFalseFromRIGHT = new ConditionalGoto(notB.result, evaluateRightBlock, setToFalse);
			evaluateRightBlock.instructions.add(conditionalGotoFalseFromRIGHT);
			setToFalse.parents().add(evaluateRightBlock);
			evaluateRightBlock.children().add(setToFalse);

			//// setToTrue block
			Block setToTrueBlock = conditionalGotoFalseFromRIGHT.elze;

			ArrayList<Block> parents = new ArrayList<>();
			parents.add(setToTrueBlock);
			parents.add(setToFalse);
			end = new Block(context.function, parents);

			Temporary tru = setToTrueBlock.getTemporaryForConstant(PrimitiveType.BOOL, true);
			setToTrueBlock.instructions.add(new Goto(end));

			//// setToFalse block
			Temporary fals = setToFalse.getTemporaryForConstant(PrimitiveType.BOOL, false);

			// end block
			HashMap<Block, Temporary> edges = new HashMap<>();
			edges.put(setToTrueBlock, tru);
			edges.put(setToFalse, fals);
			end.instructions.add(new PhiNode(result, edges));
			end.instructions.add(new Comment("End of &&"));

		} else if(this.operator == Operator.or) {

			context.instructions.add(new Comment("Begin of ||"));

			tmpL = left.accept(bodyVisitor, context);
			context = tmpL.block;

			Block setToTrue = new Block(context);

			ConditionalGoto conditionalGoto = new ConditionalGoto(tmpL.temporary, context, setToTrue);
			context.instructions.add(conditionalGoto);

			Block evaluateRightBlock = conditionalGoto.elze;

			tmpR = right.accept(bodyVisitor, evaluateRightBlock);
			evaluateRightBlock = tmpR.block;

			ConditionalGoto conditionalGotoFromRIGHT = new ConditionalGoto(tmpR.temporary, evaluateRightBlock, setToTrue);
			evaluateRightBlock.instructions.add(conditionalGotoFromRIGHT);
			setToTrue.parents().add(evaluateRightBlock);
			evaluateRightBlock.children().add(setToTrue);

			Block setToFalse = conditionalGotoFromRIGHT.elze;

			ArrayList<Block> parents = new ArrayList<>();
			parents.add(setToFalse);
			parents.add(setToTrue);
			end = new Block(context.function, parents);

			Temporary fals = setToFalse.getTemporaryForConstant(PrimitiveType.BOOL, false);
			setToFalse.instructions.add(new Goto(end));

			Temporary tru = setToTrue.getTemporaryForConstant(PrimitiveType.BOOL, true);

			// end block

			HashMap<Block, Temporary> edges = new HashMap<>();
			edges.put(setToTrue, tru);
			edges.put(setToFalse, fals);
			end.instructions.add(new PhiNode(result, edges));
			end.instructions.add(new Comment("End of ||"));
		}
	}

	public Block outBlock() {
		return end;
	}

	public enum Operator {
		and("&&", Opcodes.AND),
		or("||", Opcodes.OR)
		;

		private final String symbol;
		private final byte opcode;

		public static Operator fromASTOperator(BinaryExpr.Operator operator) {
			switch(operator) {
				case AND:
					return and;
				case OR:
					return or;
				default:
					throw new UnsupportedOperationException();
			}
		}

		Operator(String symbol, byte opcode) {
			this.symbol = symbol;
			this.opcode = opcode;
		}

		@Override
		public String toString() {
			return symbol;
		}
	}
}
