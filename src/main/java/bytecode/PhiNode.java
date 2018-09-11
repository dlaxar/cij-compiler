package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PhiNode implements Instruction {
	Temporary result;
	private final Map<Block, Temporary> temporaries;

	public PhiNode(Temporary result, Map<Block, Temporary> edges) {
		this.result = result;
		this.temporaries = edges;
	}

	@Override
	public Temporary result() {
		return result;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(Opcodes.PHI);
		dos.writeShort(temporaries.size());
		for(Map.Entry<Block, Temporary> t : argumentsInOrder()) {
			dos.writeShort(t.getValue().index());
			dos.writeShort(t.getKey().index());
		}

		dos.annotate(toString());
	}

	private Iterable<Map.Entry<Block, Temporary>> argumentsInOrder() {
		SortedMap<Block, Temporary> map = new TreeMap<Block, Temporary>(new Comparator<Block>() {
			@Override
			public int compare(Block o1, Block o2) {
				try {
					return o1.index() - o2.index();
				} catch(InvalidCompileOrderException e) {
					e.printStackTrace();
					return 0;
				}
			}
		});
		map.putAll(temporaries);
		return map.entrySet();
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		// todo stub
	}

	@Override
	public String toString() {
		return result + " = φ(" + StreamSupport.stream(argumentsInOrder().spliterator(), false).map(entry -> {
			try {
				return entry.getValue().toString() + " from block " + entry.getKey().index();
			} catch(InvalidCompileOrderException e) {
				e.printStackTrace();
			}
			return "";
		}).collect(Collectors.joining(", ")) + ")";
	}

	@Override
	public int hashCode() {
		return result.hashCode() + temporaries.hashCode();
	}
}
