package exceptions;

public class UnhandledCaseException extends RuntimeException {
	public UnhandledCaseException(String s) {
		super(s);
	}
}
