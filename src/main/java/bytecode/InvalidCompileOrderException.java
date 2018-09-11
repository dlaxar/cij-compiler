package bytecode;

import java.io.IOException;

public class InvalidCompileOrderException extends IOException {
	public InvalidCompileOrderException(String s) {
		super(s);
	}
}
