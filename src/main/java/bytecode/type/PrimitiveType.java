package bytecode.type;

import bytecode.InvalidCompileOrderException;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;

public abstract class PrimitiveType extends Type {

	private PrimitiveType(String name) {
		super(name);
	}

	public static Type getPointerType(ObjectType objectType) {
		return new PtrType(objectType);
	}

	private static final class VoidType extends PrimitiveType {

		private VoidType() {
			super("void");
		}

		@Override
		public byte getRepresentation() {
			return 0;
		}

		@Override
		public Expression defaultInitializer() {
			throw new InvalidTypeException();
		}
	}

	private static final class BoolType extends PrimitiveType {

		private BoolType() {
			super("boolean");
		}

		@Override
		public byte getRepresentation() {
			return 1;
		}

		@Override
		public Expression defaultInitializer() {
			return new BooleanLiteralExpr();
		}
	}

	private static final class ByteType extends PrimitiveType {

		private ByteType() {
			super("byte");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 2;
		}

		@Override
		public Expression defaultInitializer() {
			return new CastExpr(
					com.github.javaparser.ast.type.PrimitiveType.byteType(),
					new IntegerLiteralExpr()
			);
		}
	}

	private static final class CharType extends PrimitiveType {

		private CharType() {
			super("char");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 3;
		}

		@Override
		public Expression defaultInitializer() {
			return new CharLiteralExpr();
		}
	}

	private static final class ShortType extends PrimitiveType {

		private ShortType() {
			super("short");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 4;
		}

		@Override
		public Expression defaultInitializer() {
			return new CastExpr(
					com.github.javaparser.ast.type.PrimitiveType.shortType(),
					new IntegerLiteralExpr()
			);
		}
	}

	private static final class IntType extends PrimitiveType {

		private IntType() {
			super("int");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 5;
		}

		@Override
		public Expression defaultInitializer() {
			return new IntegerLiteralExpr();
		}
	}

	private static final class LongType extends PrimitiveType {

		private LongType() {
			super("long");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 6;
		}

		@Override
		public Expression defaultInitializer() {
			return new LongLiteralExpr();
		}
	}

	private static final class FloatType extends PrimitiveType {

		private FloatType() {
			super("float");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 7;
		}

		@Override
		public Expression defaultInitializer() {
			return new CastExpr(
					com.github.javaparser.ast.type.PrimitiveType.floatType(),
					new DoubleLiteralExpr()
			);
		}
	}

	private static final class DoubleType extends PrimitiveType {

		private DoubleType() {
			super("double");
		}

		@Override
		public byte getRepresentation() {
			return (byte) 8;
		}

		@Override
		public Expression defaultInitializer() {
			return new DoubleLiteralExpr();
		}
	}

	private static final class PtrType extends PrimitiveType {

		Type baseType;

		private PtrType(Type type) {
			super("ptr:" + type.name);
			baseType = type;
		}

		@Override
		public byte getRepresentation() {
			try {
				return baseType.getRepresentation();
			} catch(InvalidCompileOrderException e) {
				return 0;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof PtrType)) {
				return false;
			}

			return ((PtrType) obj).name.equals(this.name);
		}

		@Override
		public boolean isSpecialOf(Type type) {
			if(!(type instanceof PtrType)) {
				return false;
			}

			return baseType.isSpecialOf(((PtrType) type).baseType);
		}

		@Override
		public Expression defaultInitializer() {
			return new NullLiteralExpr();
		}
	}

	public static final VoidType VOID = new VoidType();
	public static final BoolType BOOL = new BoolType();
	public static final ByteType BYTE = new ByteType();
	public static final CharType CHAR = new CharType();
	public static final ShortType SHORT = new ShortType();
	public static final IntType INT = new IntType();
	public static final LongType LONG = new LongType();
	public static final FloatType FLOAT = new FloatType();
	public static final DoubleType DOUBLE = new DoubleType();


	public static PrimitiveType getType(com.github.javaparser.ast.type.Type astType) {
		if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.booleanType())) {
			return BOOL;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.byteType())) {
			return BYTE;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.charType())) {
			return CHAR;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.shortType())) {
			return SHORT;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.intType())) {
			return INT;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.longType())) {
			return LONG;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.floatType())) {
			return FLOAT;
		} else if(astType.equals(com.github.javaparser.ast.type.PrimitiveType.doubleType())) {
			return DOUBLE;
		} else {
			throw new InvalidTypeException();
		}
	}

	public static Type getType(ResolvedPrimitiveType resolvedType) {
		if(resolvedType.equals(ResolvedPrimitiveType.BOOLEAN)) {
			return BOOL;
		} else if(resolvedType.equals(ResolvedPrimitiveType.BYTE)) {
			return BYTE;
		} else if(resolvedType.equals(ResolvedPrimitiveType.CHAR)) {
			return CHAR;
		} else if(resolvedType.equals(ResolvedPrimitiveType.SHORT)) {
			return SHORT;
		} else if(resolvedType.equals(ResolvedPrimitiveType.INT)) {
			return INT;
		} else if(resolvedType.equals(ResolvedPrimitiveType.LONG)) {
			return LONG;
		} else if(resolvedType.equals(ResolvedPrimitiveType.FLOAT)) {
			return FLOAT;
		} else if(resolvedType.equals(ResolvedPrimitiveType.DOUBLE)) {
			return DOUBLE;
		} else{
			throw new InvalidTypeException();
		}
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean isSpecialOf(Type type) {
		return this.equals(type);
	}

	public abstract Expression defaultInitializer();

	public static Expression getDefaultInitializerExpression(PrimitiveType type) {
		return type.defaultInitializer();
	}
}
