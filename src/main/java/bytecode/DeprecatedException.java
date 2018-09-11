package bytecode;

public class DeprecatedException extends RuntimeException {
	public DeprecatedException() {
		super();
	}
	public DeprecatedException(String message) {
		super(message);
	}
}
