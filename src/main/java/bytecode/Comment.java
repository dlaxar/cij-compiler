package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Comment implements Compileable {

	String text;

	public Comment(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "# " + text;
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.annotate(toString());
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {}
}
