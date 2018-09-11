package bytecode;

import bytecode.dom.Node;
import bytecode.type.Type;
import stream.AnnotatedDataOutput;
import variables.LocalManager;
import variables.PhiManager;

import java.io.IOException;
import java.util.*;

public class Block implements Compileable, Node {
	public final Function function;
	public List<Compileable> instructions = new ArrayList<Compileable>();

	private final LocalManager localManager = new LocalManager(this);

	private final List<Block> _parents;
	private List<Block> children = new ArrayList<>();

	private int index = -1;
	private Set<Node> dom;
	private Node idom;
	private Set<Node> df = new HashSet<>();
	private PhiManager phiManager = new PhiManager(this);

	/**
	 * Constructs a block with no parents belonging the to given function
	 *
	 * @param function
	 */
	public Block(Function function) {
		this(function, new LinkedList<>());
	}

	public Block(Block parent) {
		this(parent.function, new LinkedList<>(Collections.singletonList(parent)));
	}

	public Block(Function function, List<Block> _parents) {
		this.function = function;
		this._parents = _parents;
		for(Block b : _parents) {
			b.addChild(this);
			localManager.addAllAlive(b.localManager().getAllAlive());
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Compileable i : instructions) {
			sb.append('\t');
			sb.append(i);
			sb.append('\n');
		}

		return sb.toString();
	}

	public final List<Block> parents() {
		return _parents;
	}

	public Temporary createTemporary() {
		return new Temporary(this);
	}

	/**
	 * Returns a temporary if this constant was already initialized
	 * in this scope. Otherwise it initializes the constant and returns the new
	 * temporary
	 *
	 * @param type
	 * @param value
	 * @param <T>
	 * @return
	 */
	public <T> Temporary getTemporaryForConstant(Type type, T value) {
		// todo optimize only once initialization
		Const<T> c = new Const<>(new Temporary(this), type, value);
		instructions.add(c);
		return c.result();
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		for(Compileable i : instructions) {
			i.writeToStream(dos);
		}
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		for(Compileable c : instructions) {
			c.substituteTemporaries(substitute);
		}
	}

	public ListIterator<Block> blockIterator() {
		Block that = this;
		return new ListIterator<Block>() {

			boolean nextRetrieved = true, prevRetrieved = true;
			boolean thisRetrieved = false;
			ListIterator<Block> child = null;
			ListIterator<Block> prev = null;
			ListIterator<Block> childrenIterator = children.listIterator();

			@Override
			public boolean hasNext() {
				if(!thisRetrieved) {
					return true;
				}

				if(child != null && child.hasNext()) {
					return true;
				}

				if(childrenIterator.hasNext()) {
					if(!nextRetrieved) {
						return true;
					}

					while(childrenIterator.hasNext()) {
						Block kid = childrenIterator.next();
						if(kid.parents().get(kid.parents().size()-1) == that) {
							nextRetrieved = false;
							child = kid.blockIterator();
							return child.hasNext();
						}
					}

					return false;
				} else {
					return false;
				}
			}

			@Override
			public Block next() {
				if(!thisRetrieved) {
					thisRetrieved = true;
					return that;
				}

				if(childrenIterator.hasNext() && !child.hasNext()) {
					while(childrenIterator.hasNext()) {
						Block kid = childrenIterator.next();
						if(kid.parents().get(kid.parents().size()-1) == that) {
							child = kid.blockIterator();
							break;
						}
					}
				}

				nextRetrieved = true;
				return child.next();
			}

			@Override
			public boolean hasPrevious() {
				if(!thisRetrieved) {
					return false;
				}

				if(prev != null && prev.hasPrevious()) {
					return true;
				}

				if(childrenIterator.hasPrevious()) {
					if(!prevRetrieved) {
						return true;
					}

					while(childrenIterator.hasPrevious()) {
						Block kid = childrenIterator.previous();
						if(kid.parents().get(kid.parents().size()-1) == that) {
							prevRetrieved = false;
							prev = kid.blockIterator();
							return prev.hasPrevious();
						}
					}

					return false;
				} else {
					return false;
				}
			}

			@Override
			public Block previous() {
				if(!thisRetrieved) {
					throw new IndexOutOfBoundsException();
				}

				if(childrenIterator.hasPrevious() && !child.hasPrevious()) {
					while(childrenIterator.hasPrevious()) {
						Block kid = childrenIterator.previous();
						if(kid.parents().get(kid.parents().size()-1) == that) {
							child = kid.blockIterator();
							break;
						}
					}
				}

				nextRetrieved = true;
				return child.previous();
			}

			@Override
			public int nextIndex() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int previousIndex() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void remove() {
				if(!thisRetrieved) {
					throw new UnsupportedOperationException("Cannot remove the first element");
				}

				if(!nextRetrieved) {
					throw new UnsupportedOperationException("Cannot remove element before retrieving via call to `next`");
				}

				if(child != null) {
					child.remove();
				}

				throw new UnsupportedOperationException();
			}

			@Override
			public void set(Block block) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void add(Block block) {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterable<Compileable> instructions() {
		Block that = this;
		return () -> that.instructionIterator();
	}

	public ListIterator<Compileable> instructionIterator() {
		Block that = this;
		return new ListIterator<Compileable>() {

			boolean kidRetrieved = true;
			ListIterator<Compileable> instructionIterator = instructions.listIterator();
			ListIterator<Compileable> child = null;
			Iterator<Block> childrenIterator = children.iterator();

			@Override
			public boolean hasNext() {
				if(instructionIterator.hasNext()) {
					return true;
				}

				if(child != null && child.hasNext()) {
					return true;
				}

				if(childrenIterator.hasNext()) {
					if(!kidRetrieved) {
						return true;
					}

					while(childrenIterator.hasNext()) {
						Block kid = childrenIterator.next();
						if(kid.parents().get(kid.parents().size()-1) == that) {
							kidRetrieved = false;
							child = kid.instructionIterator();
							return child.hasNext();
						}
					}

					return false;
				} else {
					return false;
				}
			}

			@Override
			public Compileable next() {
				if(instructionIterator.hasNext()) {
					return instructionIterator.next();
				}

				if(childrenIterator.hasNext() && !child.hasNext()) {
					while(childrenIterator.hasNext()) {
						Block kid = childrenIterator.next();
						if(kid.parents().get(kid.parents().size()-1) == that) {
							child = kid.instructionIterator();
							break;
						}
					}
				}

				kidRetrieved = true;
				return child.next();
			}

			@Override
			public boolean hasPrevious() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Compileable previous() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int nextIndex() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int previousIndex() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void remove() {
				if(child == null) {
					instructionIterator.remove();
				} else {
					child.remove();
				}
			}

			@Override
			public void set(Compileable compileable) {
				if(child == null) {
					instructionIterator.set(compileable);
				} else {
					child.set(compileable);
				}
			}

			@Override
			public void add(Compileable compileable) {
				if(child == null) {
					instructionIterator.add(compileable);
				} else {
					child.add(compileable);
				}
			}
		};
	}

	private void addChild(Block b) {
		children.add(b);
	}

	public LocalManager localManager() {
		return localManager;
	}

	public void createCycle(Block additionalParent) {
		this.parents().add(0, additionalParent);
		additionalParent.addChild(this);
	}

	public int countCompiledInstructions() throws InvalidCompileOrderException {
		int x = phiManager().size();
		for(Compileable c : instructions) {
			if(Opcodes.hasOpcode(c)) {
				x++;
			}
		}
		return x;
	}

	public int index() throws InvalidCompileOrderException {
		if(index == -1) {
			throw new InvalidCompileOrderException("Must number blocks first");
		}

		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public List<Block> children() {
		return children;
	}

	@Override
	public Set<Node> DOM() {
		return dom;
	}

	@Override
	public void setDOM(Set<Node> dom) {
		this.dom = dom;
	}

	@Override
	public void putInPostOrder(Set<Node> visited, List<Node> list) {
		visited.add(this);
//		try {
//			System.out.println("Entering " + index());
//		} catch(InvalidCompileOrderException e) {
//			e.printStackTrace();
//		}

		for(Block b : children) {
			if(!visited.contains(b)) {
				b.putInPostOrder(visited, list);
			}
		}

		list.add(this);
	}

	@Override
	public Collection<? extends Node> getParents() {
		return _parents;
	}

	@Override
	public Node IDOM() {
		return idom;
	}

	@Override
	public void setIDOM(Node idom) {
		this.idom = idom;
	}

	public Set<Node> DF() {
		return df;
	}

	public PhiManager phiManager() {
		return this.phiManager;
	}
}
