package bytecode.dom;

import java.util.*;

public class N2DominanceTree implements DominanceTree {

	public List<Node> blocksInPostorder = new ArrayList<>();

	public N2DominanceTree(Node root) {
		root.putInPostOrder(new HashSet<>(), blocksInPostorder);

		for(Node n : blocksInPostorder) {
			n.setDOM(new HashSet<>(blocksInPostorder));
		}

		root.setDOM(new HashSet<>());
		root.DOM().add(root);

		boolean changed = true;
		while(changed) {
			changed = false;

			// for all nodes n in reverse postorder
			ListIterator<Node> iterator = blocksInPostorder.listIterator(blocksInPostorder.size());
			while(iterator.hasPrevious()) {
				Node node = iterator.previous();

				Set<Node> newSet = node.DOM();
				int originalSize = newSet.size();

				// for each predecessor
				for(Node parent : node.getParents()) {
					newSet.retainAll(parent.DOM());
				}

				newSet.add(node);

				if(originalSize != newSet.size()) {
					node.setDOM(newSet);
					changed = true;
				}
			}
		}

		for(Node n : blocksInPostorder) {
			Set<Node> idom = new HashSet<>(n.DOM());
			idom.remove(n);

			for(Node x : blocksInPostorder) {
				if(x.DOM().containsAll(idom) && idom.contains(x)) {
					n.setIDOM(x);
					break;
				}
			}
		}
	}
}
