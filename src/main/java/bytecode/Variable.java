package bytecode;

import bytecode.type.Type;
import com.github.javaparser.ast.body.Parameter;
import exceptions.TypeNotSupportedException;
import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.Map;

public class Variable implements Compileable {

	public final String name;
	public final Type type;
	private Block context;
	private int index;

	public Variable(String name, Type type, Block context) {
		this.name = name;
		this.type = type;
		this.context = context;
	}

	public static Variable fromParameter(Parameter parameter, Function context) throws TypeNotSupportedException {
		return new Variable(parameter.getNameAsString(),
		                    Type.getVariableType(parameter.getType(), context.file.typeSystem()),
		                    null);
	}

	public String usageToString() {
		return "$" + name;
	}

	public String declarationToString() {
		return type + " $" + name;
	}

	@Override
	public String toString() {
		return String.format("%s $%s", type, name);
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeByte(type.getRepresentation());
		BytecodeFile.writeToStream(name, dos);

		dos.annotate(toString());
	}

	public int index() throws InvalidCompileOrderException {
		if(index == -1) {
			throw new InvalidCompileOrderException("Must number variables first");
		}

		return index;
	}

	public void setIndex(int i) {
		index = i;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {}
}
