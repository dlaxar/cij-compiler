package bytecode;

import bytecode.type.ObjectType;
import com.github.javaparser.ast.expr.Expression;
import stream.AnnotatedDataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TypeTable implements Compileable {

	List<ObjectType> types = new ArrayList<>();
	List<ObjectType.Field> staticFields = new ArrayList<>();

	public TypeTable(Collection<? extends ObjectType> types) {
		this.types.addAll(types);

		byte startAt = 9;
		for(ObjectType t : types) {
			t.setId(startAt++);
			t.sortFields();
			t.sortFunctions();

			try {
				staticFields.addAll(t.getStaticFields());
			} catch(InvalidCompileOrderException e) {
				e.printStackTrace();
			}
		}

		short index = 0;
		for(ObjectType.Field f : staticFields) {
			f.index = index++;
		}
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		dos.writeShort(staticFields.size(), staticFields.size() + " global variables");
		for(ObjectType.Field global : staticFields) {
			// type
			dos.writeByte(global.type.getRepresentation(), " type: " + global.type.toString());

			// name
			dos.writeUTF(global.type.name, " name: " + global.name);
		}

		dos.writeShort(types.size(), types.size() + " types");
		for(ObjectType t : types) {
			dos.section("Type " + t.name);

			// id
			dos.writeByte(t.getRepresentation(), " id: " + t.getId());

			// name
			dos.writeUTF(t.name, " name: " + t.name);

			List<ObjectType.Field> fields = t.getFields();

			// numMembers
			dos.writeShort(fields.size(), " numMembers: " + fields.size());


			for(ObjectType.Field field : fields) {
				dos.writeByte(field.type.getRepresentation(), "  typeId: " + field.type.getRepresentation());

				dos.writeUTF(field.name, "  name: " + field.name);
			}

			List<Function> virtualFunctions = t.getVirtualFunctions();

			// vTable
			dos.writeShort(virtualFunctions.size(), " " + virtualFunctions.size() + " virtual function");
			for(Function f : virtualFunctions) {
				dos.writeShort(f.index(), "  " + f.localIndex() + ": fn " + f.name() + " (global index: " + f.index() + ")");
			}
		}
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		// todo
	}

	public Map<ObjectType.Field, Expression> getStaticInitializers() throws InvalidCompileOrderException {
		HashMap<ObjectType.Field, Expression> initializers = new HashMap<>();
		for(ObjectType objectType : types) {
			initializers.putAll(objectType.getStaticFields().stream().collect(Collectors.toMap(
					java.util.function.Function.identity(),
					f -> f.initializer
			))
			);
		}
		return initializers;
	}
}
