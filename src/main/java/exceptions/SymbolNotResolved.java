package exceptions;

public class SymbolNotResolved extends RuntimeException {
	public SymbolNotResolved(String s) {
		super(s);
	}
}
