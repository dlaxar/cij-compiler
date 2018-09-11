package variables;

import bytecode.*;

import java.util.*;
import java.util.stream.Collectors;

public class LocalManager {

	private final Block block;
	private final Map<Variable, Temporary> declared = new HashMap<>();
	private final Set<String> allAlive = new HashSet<>();
	private final Map<String, Temporary> phid = new HashMap<>();
	private final Map<String, Temporary> written = new HashMap<>();

	private final Map<String, Temporary> undefinedUsages = new HashMap<>();

	public LocalManager(Block block) {

		this.block = block;
	}

	public Temporary declare(Variable variable, Temporary init) {
		declared.put(variable, init);
		allAlive.add(variable.name);
		writeByName(variable.name, init);
		return declared.get(variable);
	}

	public Temporary readByName(String s) {
		// if we've already written the variable once then return the same temporary
		if(written.containsKey(s)) {
			return written.get(s);
		}

		// try locally declared vars first
		for(Map.Entry<Variable, Temporary> entry : declared.entrySet()) {
			if(entry.getKey().name.equals(s)) {
				return entry.getValue();
			}
		}

		// this has already be read before
		if(undefinedUsages.containsKey(s)) {
			return undefinedUsages.get(s);
		}

		// otherwise create a new temporary and record the undefined usage
		// this will later be used to generate phi nodes in place
		Temporary t = new Temporary(block);
		undefinedUsages.put(s, t);

		return t;
	}

	public void ensureWritten(String s, Temporary t) {
		if(!written.containsKey(s)) {
			writeByName(s, t);
		}
	}

	public void define(String s, Temporary t) {
		ensureWritten(s, t);
		undefinedUsages.remove(s);
	}

	public Temporary writeByName(String s, Temporary t) {
		written.put(s, t);
		return t;
	}

	public Temporary temporaryFor(String s) throws InvalidCompileOrderException {
		Set<Block> visited = new HashSet<>();
		ArrayList<Temporary> found = new ArrayList<>();

		ArrayDeque<Block> queue = new ArrayDeque<>();
		queue.add(block);

		while(queue.peek() != null) {
			Block b = queue.pop();

			if(visited.contains(b)) {
				continue;
			}

			visited.add(b);

			if(b.localManager().written.get(s) != null) {
				return b.localManager().written.get(s);
			} else {
				queue.addAll(b.parents());
			}
		}

		throw new InvalidCompileOrderException("Temporary could not be found!");
	}

	public Map<String, Temporary> written() {
		return written;
	}

	public boolean isUndefined(String variableName) {
		return undefinedUsages.keySet().contains(variableName);
	}

	public Temporary getUndfinedUsage(String variableName) {
		return undefinedUsages.get(variableName);
	}

	public boolean hasUndefinedUsages() {
		return undefinedUsages.size() > 0;
	}

	public void resolveUndefinedUsages() {
		Map<Temporary, Temporary> replacement = new HashMap<>();

		// find the corresponding join node for the current node
		Block parent = block;
		while(hasUndefinedUsages() && parent.parents().size() == 1) {
			parent = parent.parents().get(0);

			// now for each undefined usage get the appropriate phi node
			for(Iterator<Map.Entry<String, Temporary>> it = undefinedUsages.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Temporary> use = it.next();

				Temporary actualTemporary = parent.phiManager().temporaryForVariable(use.getKey());

				if(actualTemporary != null) {
					replacement.put(use.getValue(), actualTemporary);

					// check if the variable has been written after the first undefined usage
					// if not, replace the written with the correct temporary
					if(written.get(use.getKey()) == use.getValue()) {
						written.put(use.getKey(), actualTemporary);
					}

					it.remove();
				}
			}
		}

		// Although: not all variables need to come from join nodes and phi nodes. If a variable is simply declared
		// before a separation and used after the joining, there is no corresponding phi node. We need to search
		// for these variables now!
		for(Iterator<Map.Entry<String, Temporary>> it = undefinedUsages.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, Temporary> use = it.next();

			Temporary actualTemporary = deepSearchWritten(use.getKey());
			replacement.put(use.getValue(), actualTemporary);

			it.remove();
		}


		if(hasUndefinedUsages()) {
			throw new RuntimeException("Could not find all usages");
		}

		// finally replace all the undefined usages with the correct temporaries
		for(Compileable i : block.instructions) {
			i.substituteTemporaries(replacement);
		}
	}

	private Temporary deepSearchWritten(String variableName) {
		Set<Block> visited = new HashSet<>();
		Queue<Block> blocks = new ArrayDeque<>();
		blocks.addAll(block.parents());

		while(blocks.peek() != null) {
			Block b = blocks.remove();

			if(visited.contains(b)) {
				continue;
			}

			visited.add(b);

			if(b.localManager().written.containsKey(variableName)) {
				return b.localManager().written.get(variableName);
			}

			blocks.addAll(b.parents());
		}

		throw new RuntimeException("Could not find variable " + variableName + "!");
	}

	public Set<String> getAllAlive() {
		return allAlive;
	}

	public void addAllAlive(Set<String> alive) {
		allAlive.addAll(alive);
	}
}
