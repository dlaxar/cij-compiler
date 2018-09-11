package variables;

import bytecode.*;

import java.util.*;

public class PhiManager {

	private final Block block;
	private SortedMap<String, UncomputedPhiNode> phiNodes = new TreeMap<>();
	private List<PhiNode> computedPhiNodes = new ArrayList<>();

	private boolean hasComputed = false;

	public PhiManager(Block block) {
		this.block = block;
	}

	public void placePhiNode(String variable, Block incomingEdge) {
		if(phiNodes.containsKey(variable)) {
			// ignore (duplicate)
			return;
		}

		Temporary temporary;
		if(block.localManager().isUndefined(variable)) {
			temporary = block.localManager().getUndfinedUsage(variable);
		} else {
			temporary = block.createTemporary();
		}

		phiNodes.put(variable, new UncomputedPhiNode(variable, temporary, block));
		block.localManager().define(variable, temporary);
		phiNodes.get(variable).addEdge(incomingEdge, incomingEdge.localManager().written().get(variable));
	}

	public void computePhiNodes() throws InvalidCompileOrderException {
		for(UncomputedPhiNode phiNode : phiNodes.values()) {
			PhiNode compute = phiNode.compute();
			if(compute != null) {
				computedPhiNodes.add(compute);
			}
		}
		hasComputed = true;
	}

	public int numberPhiNodes(int index) throws InvalidCompileOrderException {
		if(!hasComputed) {
			throw new InvalidCompileOrderException("Cannot number temporaries before phi nodes have been computed");
		}

		for(PhiNode phiNode : computedPhiNodes) {
			phiNode.result().setIndex(index++);
		}

		return index;
	}

	public int numberPhiInstructions(int index) throws InvalidCompileOrderException {
		if(!hasComputed) {
			throw new InvalidCompileOrderException("Cannot number instructions before phi nodes have been computed");
		}
		return index + computedPhiNodes.size();
	}

	public int size() throws InvalidCompileOrderException {
		if(!hasComputed) {
			throw new InvalidCompileOrderException("Cannot count number of phi node before phi nodes have been computed");
		}
		return computedPhiNodes.size();
	}

	public Temporary temporaryForVariable(String variableName) {
		if(!phiNodes.containsKey(variableName)) {
			return null;
		}

		return phiNodes.get(variableName).result();
	}

	public Iterable<PhiNode> computedPhiNodes() throws InvalidCompileOrderException {
		if(!hasComputed) {
			throw new InvalidCompileOrderException("Must compute phi nodes before iterating over them");
		}

		return computedPhiNodes;
	}
}
