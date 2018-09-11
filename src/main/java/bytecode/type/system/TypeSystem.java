package bytecode.type.system;

import bytecode.InvalidCompileOrderException;
import bytecode.TypeTable;
import bytecode.type.InvalidTypeException;
import bytecode.type.ObjectType;
import bytecode.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;

public class TypeSystem {

	private List<ObjectType> types = new ArrayList<>();
	private TypeTable typeTable = null;

	public void registerType(ObjectType type) throws InvalidCompileOrderException {
		if(typeTable != null) {
			throw new InvalidCompileOrderException("Cannot register types after type table has been generated");
		}
		types.add(type);
	}

	public Iterable<ObjectType> getTypes() {
		return types;
	}

	public ObjectType findType(String name) {
		for(ObjectType t : types) {
			if(t.name.equals(name)) {
				return t;
			}
		}

		throw new InvalidTypeException();
	}

	public TypeTable toTypeTable() {
		if(typeTable == null) {
			typeTable = new TypeTable(types);
		}

		return typeTable;
	}

	public Type getVariableType(com.github.javaparser.ast.type.Type type) {
		return Type.getVariableType(type, this);
	}

	public Type getVariableType(ResolvedType type) {
		return Type.getVariableType(type, this);
	}
}
