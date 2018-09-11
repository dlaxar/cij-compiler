package stream;

import java.io.DataOutput;
import java.io.IOException;

public interface AnnotatedDataOutput extends DataOutput {

	void writeByte(int b, String s) throws IOException;
	void writeShort(int v, String s) throws IOException;
	void writeUTF(String v, String s) throws IOException;
	void writeUTFPlain(String v, String s) throws IOException;

	void annotate(String s);
	void prefix(String s);

	void section(String s);
	void hr();

	String toAnnotatedBytecode();

}
