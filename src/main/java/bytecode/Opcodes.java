package bytecode;

public class Opcodes {

	public static final byte LOAD = 1;
	public static final byte STORE = 2;
	public static final byte CONST = 3;

	public static final byte ADD = 4;
	public static final byte SUB = 5;
	public static final byte MUL = 6;
	public static final byte DIV = 7;
	public static final byte MOD = 8;

	public static final byte NEG = 9;

	public static final byte GT = 10;
	public static final byte GTE = 11;
	public static final byte EQ = 12;
	public static final byte NEQ = 13;
	public static final byte LTE = 14;
	public static final byte LT = 15;
	public static final byte AND = 16;
	public static final byte OR = 17;
	public static final byte LOGICAL_AND = 18;
	public static final byte LOGICAL_OR = 19;
	public static final byte NOT = 20;

	public static final byte NEW = 21;

	public static final int GOTO = 22;
	public static final int CONDITIONAL_GOTO = 23;

	public static final int LABEL = 24;

	public static final int LENGTH = 25;
	public static final int PHI = 26;

	public static final int CALL = 28;
	/**
	 * Reserved for future use
	 */
	public static final int SPECIAL_CALL = 29;
	public static final int VOID_CALL = 30;
	public static final int VOID_SPECIAL_CALL = 31;

	public static final int RETURN_VOID = 32;
	public static final int RETURN = 33;

	public static final byte ALLOCATE = 100;
	public static final byte LOAD_OBJ = 101;
	public static final byte STORE_OBJ = 102;

	public static final byte LOAD_GLOBAL = 103;
	public static final byte STORE_GLOBAL = 104;

	public static final byte VOID_MEMBER_CALL = 105;
	public static final byte MEMBER_CALL = 106;


	public static final int LOAD_INDEX = 129; // high bit | load
	public static final int STORE_INDEX = 130; // high bit | store

	public static boolean hasOpcode(Compileable c) {
		return !(c instanceof Comment);
	}
}
