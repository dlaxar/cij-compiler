package exceptions;

public class UnsupportedAssignmentException extends RuntimeException {
	public UnsupportedAssignmentException(String s) {
		super("Unsupported assignment of type " + s);
	}
}
