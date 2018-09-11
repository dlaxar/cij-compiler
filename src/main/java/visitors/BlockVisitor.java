package visitors;

import bytecode.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.util.ArrayList;

public class BlockVisitor extends GenericVisitorAdapter<Block,Block> {

	public Block visit(BlockStmt stmt, Block arg) {
		if (stmt.getStatements() != null) {

			Block currentBlock = arg;
			for (final Statement s : stmt.getStatements()) {
				if(s instanceof IfStmt || s instanceof WhileStmt || s instanceof ForStmt) {
					currentBlock = s.accept(this, currentBlock);
				} else {
					s.accept(new ExpressionVisitor(), currentBlock);
				}
			}

			return currentBlock;
		}
		return arg;
	}

	// todo fix block parents
	@Override
	public Block visit(IfStmt n, Block context) {
		ExpressionResult condition = n.getCondition().accept(new ExpressionVisitor(), context);
		context = condition.block;

		Block elze = new Block(context), elzeOut = null, thenOut = null;
		ArrayList<Block> parentsForPostif = new ArrayList<>();

		if(n.getElseStmt().isPresent()) {

			elzeOut = n.getElseStmt().get().accept(new ExpressionVisitor(), elze).block;
		} else {
			elzeOut = elze;
		}

		parentsForPostif.add(elzeOut);

		Block then = new Block(context);
		thenOut = n.getThenStmt().accept(new ExpressionVisitor(), then).block;
		parentsForPostif.add(thenOut);

		Block postif = new Block(context.function, parentsForPostif);
		If i = new If(condition.temporary, context, then, elze, postif);

		elzeOut.instructions.add(new Goto(postif));

		context.instructions.addAll(i.flatten());

		return postif;
	}

	// todo fix block parents - loop body parent of loop body?
	@Override
	public Block visit(WhileStmt n, Block context) {
		// this is the jump target right here
		Block jumpTarget = new Block(context);

		Block loopHeader = jumpTarget;
		loopHeader.instructions.add(new Comment("begin of while-loopHeader"));

		// parse condition
		ExpressionResult conditionResult = n.getCondition().accept(new ExpressionVisitor(), loopHeader);
		loopHeader = conditionResult.block;

		Temporary condition = conditionResult.temporary;

		// negate result
		Temporary notCondition = loopHeader.createTemporary();
		loopHeader.instructions.add(new UnaryOperation(notCondition, UnaryOperation.Operator.not, condition));

		// if condition == false (that is, the negation is true) jump to the loop end (see later)
		ConditionalGoto conditionalGoto = new ConditionalGoto(notCondition, loopHeader, null);
		loopHeader.instructions.add(conditionalGoto);

		loopHeader.instructions.add(new Comment("end of while-loopHeader"));

		// new context is the loop body (new block)
		Block loopBody = conditionalGoto.elze;

		loopBody = n.getBody().accept(new ExpressionVisitor(), loopBody).block;

		// after the body jump to the top
		jumpTarget.createCycle(loopBody);
		loopBody.instructions.add(new Goto(jumpTarget));

		// now this is the post-loop-block. It's only parent is the loopHeader
		Block afterLoopLabel = new Block(loopHeader);
		// and the conditional goto in the loop header will jump here
		conditionalGoto.setThen(afterLoopLabel);
		afterLoopLabel.instructions.add(new Comment("end of while-loop"));

		return afterLoopLabel;
	}

	// todo fix block parents - loop parents?
	@Override
	public Block visit(ForStmt n, Block arg) {
		for(Expression e : n.getInitialization()) {
			e.accept(new ExpressionVisitor(), arg);
		}

		Block loopHeader = new Block(arg);
		loopHeader.instructions.add(new Comment("begin of for-loopHeader"));


		Block context = loopHeader;
		ConditionalGoto conditionalGoto = null;
		if(n.getCompare().isPresent()) {
			ExpressionResult conditionResult = n.getCompare().get().accept(new ExpressionVisitor(), loopHeader);
			Temporary condition = conditionResult.temporary;

			Block conditionResultBlock = conditionResult.block;

			Temporary notCondition = conditionResultBlock.createTemporary();
			conditionResultBlock.instructions.add(new UnaryOperation(notCondition, UnaryOperation.Operator.not, condition));

			conditionalGoto = new ConditionalGoto(notCondition, conditionResultBlock, null);
			loopHeader.instructions.add(conditionalGoto);
			context = conditionalGoto.elze;
		}
		context.instructions.add(new Comment("end of for-loopHeader"));

		Block loopOut = n.getBody().accept(new ExpressionVisitor(), context).block;

		for(Expression e : n.getUpdate()) {
			loopOut = e.accept(new ExpressionVisitor(), loopOut).block;
		}

		loopHeader.createCycle(loopOut);
		loopOut.instructions.add(new Goto(loopHeader));

		Block afterLoopLabel = new Block(loopHeader);
		if(conditionalGoto != null) {
			conditionalGoto.setThen(afterLoopLabel);
		}
		afterLoopLabel.instructions.add(new Comment("end of for-loop"));

		// add blocks to the main context `arg`
//		arg.instructions.addAll(loopHeader.flatten());
//		arg.instructions.add(afterLoop);
		return afterLoopLabel;
	}
}
