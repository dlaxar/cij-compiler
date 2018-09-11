package stream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static bytecode.util.Util.createStringFromBytesAndComments;
import static bytecode.util.Util.toHex;

public class LittleEndianOutputStream implements AnnotatedDataOutput {

	private DataOutputStream dos;

	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private List<String> bytes = new ArrayList<>();
	private List<String> comments = new ArrayList<>();
	private String prefix = "";

	public LittleEndianOutputStream(OutputStream out) {
		dos = new DataOutputStream(out);
	}

	@Override
	public void write(int b) throws IOException {
		dos.write(b);

		buffer.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		dos.write(b, off, len);
		buffer.write(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		write(v ? 1 : 0);
	}

	@Override
	public void writeByte(int v) throws IOException {
		write((byte)v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		write((byte) (v & 255));
		write((byte) (v >>> 8));
	}

	@Override
	public void writeChar(int v) throws IOException {
		write((byte) v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		write((byte) (v & 255));
		write((byte) ((v >>> 8) & 255));
		write((byte) ((v >>> 16) & 255));
		write((byte) (v >>> 24));
	}

	@Override
	public void writeLong(long v) throws IOException {
		write(new byte[]{
				(byte) (v & 255),
				(byte) ((v >>> 8) & 255),
				(byte) ((v >>> 16) & 255),
				(byte) ((v >>> 24) & 255),
				(byte) ((v >>> 32) & 255),
				(byte) ((v >>> 40) & 255),
				(byte) ((v >>> 48) & 255),
				(byte) ((v >>> 56))
		});
	}

	@Override
	public void writeFloat(float v) throws IOException {
		writeInt(Float.floatToRawIntBits(v));
	}

	@Override
	public void writeDouble(double v) throws IOException {
		writeLong(Double.doubleToRawLongBits(v));
	}

	@Override
	public void writeBytes(String s) throws IOException {
		throw new IOException();
	}

	@Override
	public void writeChars(String s) throws IOException {
		throw new IOException();
	}

	@Override
	public void writeUTF(String s) throws IOException {
		writeShort(s.length());
		dos.write(s.getBytes(StandardCharsets.UTF_8));
		buffer.write(s.getBytes(StandardCharsets.UTF_8));
	}

	public void writeUTFPlain(String s) throws IOException {
		dos.write(s.getBytes(StandardCharsets.UTF_8));
		buffer.write(s.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public void writeByte(int b, String s) throws IOException {
		writeByte(b);
		annotate(s);
	}

	@Override
	public void writeShort(int v, String s) throws IOException {
		write((byte) (v & 255));
		write((byte) (v >>> 8));

		annotate(s);
	}

	@Override
	public void writeUTF(String v, String s) throws IOException {
		writeUTF(v);
		annotate(s);
	}

	@Override
	public void writeUTFPlain(String v, String s) throws IOException {
		writeUTFPlain(v);
		annotate(s);
	}

	public void annotate(String s) {
		bytes.add(toHex(buffer));
		comments.add(prefix + s);
	}

	@Override
	public void prefix(String s) {
		prefix = s;
	}

	@Override
	public void section(String s) {
		bytes.add("");
		comments.add("--------- " + s);
	}

	@Override
	public void hr() {
		bytes.add("");
		comments.add("------------------------------------");
	}

	@Override
	public String toAnnotatedBytecode() {
		annotate("");
		return createStringFromBytesAndComments(bytes, comments);
	}
}
