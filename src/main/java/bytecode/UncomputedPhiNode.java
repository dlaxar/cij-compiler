package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.*;

public class UncomputedPhiNode implements Instruction {
	public final String varName;
	Temporary result;
	private final Block context;
	private final Map<Block, Temporary> edges = new HashMap<>();

	public UncomputedPhiNode(String varName, Temporary result, Block context) {
		this.varName = varName;
		this.result = result;
		this.context = context;
	}

	public PhiNode compute() throws InvalidCompileOrderException {
		Map<Block, Temporary> incoming = new HashMap<>();
		for(Block b : context.parents()) {
			if(!b.localManager().getAllAlive().contains(varName)) {
				continue;
			}

			Temporary temporary = b.localManager().temporaryFor(varName);
			incoming.put(b, temporary);
		}

		// phi nodes containing no or only one incoming edge are useless
		// (and will never be used because they are dead)
		if(incoming.size() <= 1) {
			return null;
		}

		return new PhiNode(result, incoming);
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		// todo stub
		throw new InvalidCompileOrderException("Must compute uncomputed phi nodes before streaming");
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		// todo stub
	}

	@Override
	public String toString() {
		return result + " = φ(tbd)";
	}

	@Override
	public int hashCode() {
		return result.hashCode() + context.hashCode();
	}

	public void addEdge(Block incomingEdge, Temporary temporary) {
		if(incomingEdge == null || temporary == null) {
			throw new NullPointerException();
		}
		edges.put(incomingEdge, temporary);
	}
}