package exceptions;

public class TypeNotSupportedException extends RuntimeException {
	public TypeNotSupportedException(String type) {
		super("The type '" + type + "' is not supported.");
	}
}
