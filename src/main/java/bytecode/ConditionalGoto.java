package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class ConditionalGoto implements Instruction {
	private final Block context;
	public Block elze;
	private Temporary condition;
	private Block then;

	public ConditionalGoto(Temporary condition, Block context, Block then) {
		this(condition, context, new Block(context), then);
	}

	public ConditionalGoto(Temporary condition, Block context, Block elze, Block then) {
		this.context = context;
		if(condition == null) {
			throw new ArrayIndexOutOfBoundsException();
		}
		this.condition = condition;
		this.then = then;
		this.elze = elze;
	}

	@Override
	public String toString() {
		try {
			return String.format("if %s goto block %s", condition, then.index());
		} catch(InvalidCompileOrderException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.CONDITIONAL_GOTO);
		dos.writeShort(condition.index());
		dos.writeShort(then.index());
		dos.annotate(toString());
	}

	@Override
	public Temporary result() {
		return null;
	}

	public Block label() {
		return then;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		condition = substitute.getOrDefault(condition, condition);
	}

	public void setThen(Block then) {
		this.then = then;
	}

	public void replaceJumpTargets(Map<Block, Block> blocksToRemove) {
		elze = blocksToRemove.getOrDefault(elze, elze);
		then = blocksToRemove.getOrDefault(then, then);
	}
}
