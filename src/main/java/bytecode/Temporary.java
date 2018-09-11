package bytecode;

public class Temporary {
	private final Block context;
	private int index = -1;

	@Override
	public String toString() {
		return "%" + index;
	}

	public Temporary(Block context) {
		this.context = context;
	}

	public int index() throws InvalidCompileOrderException {
		if(index == -1) {
			throw new InvalidCompileOrderException("Must number temporaries first");
		}

		return index;
	}

	public void setIndex(int index) {this.index = index;}

	public Block context() {
		return context;
	}
}
