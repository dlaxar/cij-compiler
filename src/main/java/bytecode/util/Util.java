package bytecode.util;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;

public class Util {

	public static String toHex(ByteArrayOutputStream baos) {
		StringBuilder sb = new StringBuilder();
		for (byte b : baos.toByteArray()) {
			sb.append(String.format("%02X ", b));
		}
		baos.reset();
		return sb.toString();
	}

	public static String createStringFromBytesAndComments(List<String> bytes, List<String> comments) {
		StringBuilder sb = new StringBuilder();

		int max = 0;
		for(String s : bytes) {
			max = Math.max(s.length(), max);
		}

		Iterator<String> bytesIterator = bytes.iterator();
		Iterator<String> commentsIterator = comments.iterator();

		while(bytesIterator.hasNext()) {
			String s = bytesIterator.next();
			sb.append(s);
			for(int i = s.length(); i <= max; i++) {
				sb.append(' ');
			}
			sb.append("# ").append(commentsIterator.next()).append('\n');
		}

		return sb.toString();
	}
}
