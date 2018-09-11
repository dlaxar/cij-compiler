package exceptions;

import bytecode.type.ObjectType;

public class InvalidFieldException extends RuntimeException {
	public InvalidFieldException(String name, ObjectType objectType) {
		super("No field '" + name + "' on object of type " + objectType);
	}
}
