package bytecode.dom;

import bytecode.Block;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Node {

	Set<Node> DOM();
	void setDOM(Set<Node> dom);

	Node IDOM();
	void setIDOM(Node idom);

	void putInPostOrder(Set<Node> visited, List<Node> list);

	Collection<? extends Node> getParents();

	Set<Node> DF();
}
