package bytecode.frontier;

import bytecode.Block;
import bytecode.dom.Node;

import java.util.ListIterator;

public class DominanceFrontier {
	public DominanceFrontier(Block root) {
		for(ListIterator<Block> it = root.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			if(b.parents().size() > 1) {
				for(Block p : b.parents()) {
					Node runner = p;
					while(runner != b.IDOM()) {
						runner.DF().add(b);
						runner = runner.IDOM();
					}
				}
			}
		}
	}
}
