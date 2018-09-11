package visitors;

import bytecode.Block;
import bytecode.Temporary;

public class ExpressionResult {
	public final Temporary temporary;
	public final Block block;

	public ExpressionResult(Temporary temporary, Block block) {
		this.temporary = temporary;
		this.block = block;
	}
}
