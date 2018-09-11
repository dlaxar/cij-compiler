package exceptions;

public class NoDefaultConstructorException extends RuntimeException {
	public NoDefaultConstructorException(String message) {
		super(message);
	}
}
