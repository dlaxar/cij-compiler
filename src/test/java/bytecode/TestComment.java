package bytecode;

import org.junit.Test;
import stream.LittleEndianOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class TestComment {
	@Test
	public void byteOutput() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		new Comment("comment1").writeToStream(new LittleEndianOutputStream(byteArrayOutputStream));

		assertArrayEquals(new byte[]{}, byteArrayOutputStream.toByteArray());
	}
}
