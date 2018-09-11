package bytecode;

import bytecode.type.InvalidTypeException;
import bytecode.type.ObjectType;
import bytecode.type.PrimitiveType;
import bytecode.type.Type;
import bytecode.type.system.TypeSystem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import exceptions.TypeNotSupportedException;
import exceptions.UndefinedFunctionNameException;
import stream.AnnotatedDataOutput;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class BytecodeFile implements Compileable {

	private TypeSystem typeSystem = new TypeSystem();

	private List<Function> functions = new ArrayList<>();
	private String hashbang = null;

	public BytecodeFile(File output, CompilationUnit cu) throws TypeNotSupportedException, UndefinedFunctionNameException, InvalidCompileOrderException {
		// register all types
		for(TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
			if(typeDeclaration.isClassOrInterfaceDeclaration() && !typeDeclaration.asClassOrInterfaceDeclaration().isInterface()) {

				ObjectType type = new ObjectType(typeDeclaration.getNameAsString(),
						typeDeclaration.asClassOrInterfaceDeclaration());
				typeSystem.registerType(type);
			} else {
				throw new InvalidTypeException();
			}
		}

		// register all "extend" relationships
		for(TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
			if(typeDeclaration.isClassOrInterfaceDeclaration() && !typeDeclaration.asClassOrInterfaceDeclaration().isInterface()) {
				if(typeDeclaration.asClassOrInterfaceDeclaration().getExtendedTypes().size() != 0) {
					String parent = typeDeclaration.asClassOrInterfaceDeclaration().getExtendedTypes().get(0).getNameAsString();
					typeSystem().findType(typeDeclaration.getNameAsString()).setParent(typeSystem().findType(parent));
				}
			} else {
				throw new InvalidTypeException();
			}
		}

		// fill types with members
		for(ObjectType type : typeSystem.getTypes()) {
			for(BodyDeclaration<?> member : type.getDeclaration().getMembers()) {
				if(member.isFieldDeclaration()) {
					if(member.asFieldDeclaration().isStatic()) {
						type.addStaticField(member.asFieldDeclaration(), typeSystem);
					} else {
						type.addField(member.asFieldDeclaration(), typeSystem);
					}
				}
			}
		}

		int index = 0;
		// add member functions
		for(ObjectType type : typeSystem.getTypes()) {
			if(type.getDeclaration().getConstructors().size() == 0) {
				Function f = Function.createEmptyFunction(this, type, type.name);
				f.setIndex(index++);
				functions.add(f);

				type.addStaticFunction(f);
			} else {
				for(ConstructorDeclaration constructor : type.getDeclaration().getConstructors()) {
					Function f = new Function(this, type, PrimitiveType.VOID, constructor.getBody(), constructor);
					f.setIndex(index++);
					functions.add(f);

					type.addStaticFunction(f);
				}
			}

			for(BodyDeclaration<?> member : type.getDeclaration().getMembers()) {
				if(member.isMethodDeclaration()){
					MethodDeclaration methodDeclaration = member.asMethodDeclaration();
					Function f = new Function(this,
					                          type,
					                          Type.getVariableType(methodDeclaration.getType(), typeSystem),
					                          methodDeclaration.getBody().get(),
					                          methodDeclaration);
					f.setIndex(index++);
					functions.add(f);

					if(!methodDeclaration.isStatic()) {
						type.addVirtual(f);
					} else {
						type.addStaticFunction(f);
					}
				}
			}
		}

		// freeze type system
		typeSystem().toTypeTable();

		for(Function f : functions) {
			if(f.isMain()) {
				f.compile(typeSystem().toTypeTable().getStaticInitializers());
			} else if(f.isConstructor()) {
				f.compile(f.type().getOwnFieldInitializers());
			} else {
				f.compile(new HashMap<>());
			}
		}
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Function f : functions) {
			sb.append(f.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		if(hashbang != null) {
			dos.writeUTFPlain("#!/usr/bin/env vm " + hashbang + "\n", "#!/usr/bin/env vm ");

		}

		dos.writeShort(1706, "magic");

		typeSystem().toTypeTable().writeToStream(dos);
		dos.hr();
		dos.writeShort(functions.size(), functions.size() + " functions");
		dos.hr();
		for(Function f : functions) {
			f.writeToStream(dos);
			dos.hr();
		}
	}

	public static void writeToStream(String s, DataOutput dos) throws IOException {
		dos.writeUTF(s);
	}

	public Function findFunctionWithName(String name) throws UndefinedFunctionNameException {
		for(Function f : functions) {
			if(f.name().equals(name)) {
				return f;
			}
		}

		throw new UndefinedFunctionNameException("Cannot find function with name `" + name + "`");
	}

	public void setHashbang(String hashbang) {
		this.hashbang = hashbang;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		for(Function f : functions) {
			f.substituteTemporaries(substitute);
		}
	}

	public TypeSystem typeSystem() {
		return typeSystem;
	}
}
