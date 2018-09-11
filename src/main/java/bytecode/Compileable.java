package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public interface Compileable {
	public void writeToStream(AnnotatedDataOutput dos) throws IOException;

	void substituteTemporaries(Map<Temporary, Temporary> substitute);
}
