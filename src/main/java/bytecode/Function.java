package bytecode;

import bytecode.dom.DominanceTree;
import bytecode.dom.N2DominanceTree;
import bytecode.dom.Node;
import bytecode.frontier.DominanceFrontier;
import bytecode.type.ObjectType;
import bytecode.type.PrimitiveType;
import bytecode.type.Type;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import exceptions.NoDefaultConstructorException;
import exceptions.TypeNotSupportedException;
import exceptions.UndefinedFunctionNameException;
import stream.AnnotatedDataOutput;
import visitors.ExpressionResult;
import visitors.ExpressionVisitor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Function {
	public final BytecodeFile file;

	private final Block initialBlock;
	private final String name;
	public final boolean isStatic;

	private final ObjectType type;
	private final BlockStmt blockStatement;
	private NodeWithBlockStmt<?> m;

	private Type returntype;
	public List<Variable> parameters = new ArrayList<>();
	public List<Variable> _locals = new ArrayList<>();
	public List<Temporary> temporaries = new ArrayList<>();
	public HashMap<String, Temporary> nameToTemporary = new HashMap<>();

	private HashMap<Boolean, Const<Boolean>> constBoolean = new HashMap<>();
	private HashMap<Integer, Const<Integer>> constInteger = new HashMap<>();
	private HashMap<Long, Const<Long>> constLong = new HashMap<>();

	private int index;
	private byte localIndex;
	private int instructionCount;

	public
	Function(BytecodeFile file, ObjectType type, Type returntype, BlockStmt blockStatement, CallableDeclaration<?> m) throws TypeNotSupportedException {
		this(file, type, returntype, blockStatement, m.getNameAsString(), m.isStatic(), m.getParameters());
	}

	private
	Function(BytecodeFile file,
	         ObjectType type,
	         Type returntype,
	         BlockStmt blockStatement,
	         String name,
	         boolean isStatic,
	         NodeList<Parameter> parameters) throws TypeNotSupportedException {

		this.file = file;
		this.type = type;

		this.name = name;
		this.blockStatement = blockStatement;
		this.returntype = returntype;

		this.initialBlock = new Block(this);

		this.isStatic = isStatic || isConstructor();

		if(!(isStatic && name.equals("main") && parameters.size() == 1)) {
			int index = 0;

			if(!isStatic || isConstructor()) {
				// create this as first parameter
				Variable variable = new Variable("this", PrimitiveType.getPointerType(type), null);
				this.parameters.add(variable);
				Temporary temporary = new Temporary(initialBlock);
				temporary.setIndex(index++);
				initialBlock.localManager().declare(variable, temporary);
			}

			for(Parameter parameter : parameters) {
				Variable variable = Variable.fromParameter(parameter, this);
				this.parameters.add(variable);
				Temporary temporary = new Temporary(initialBlock);
				temporary.setIndex(index++);
				initialBlock.localManager().declare(variable, temporary);
			}
		}

	}


	public void compile(Map<ObjectType.Field, Expression> initializers) throws InvalidCompileOrderException {
		Block context = initialBlock;

		if(isConstructor()) {
			if(type.hasParent() &&
			   (blockStatement.getStatements().isEmpty() || !(blockStatement.getStatement(0).isExplicitConstructorInvocationStmt()))) {
				// constructor is either empty or does not call super()
				ObjectType parent = type.parent();
				Function defaultConstructor = parent.getConstructor(new ArrayList<>());

				if(defaultConstructor == null) {
					throw new NoDefaultConstructorException("No default constructor for type " + parent + " in class " + type);
				}

				context.instructions.add(new VoidCall(defaultConstructor, context.localManager().readByName("this")));
			}
		}

		for(Map.Entry<ObjectType.Field, Expression> init : initializers.entrySet()) {
			ExpressionResult expressionResult = init.getValue().accept(new ExpressionVisitor(), context);
			context = expressionResult.block;

			if(isConstructor()) {
				context.instructions.add(new ObjectStore(context.localManager().readByName("this"),
				                                         type(),
				                                         init.getKey().name,
				                                         expressionResult.temporary));
			} else {
				context.instructions.add(new StoreGlobal(init.getKey(), expressionResult.temporary));
			}
		}

		new ExpressionVisitor().visit(blockStatement, context);

		if(returntype == PrimitiveType.VOID) {
			insertVoid();
		}

		removeRedundantBlocks();
		removeRedundantLoads();

		numberBlocks();
		computeDominance();
		computePhiNodePlacement();

		resolvePhiNodeUsages();
		computeUncomputedPhiNodes();

		numberParamsLocals();
		numberTemporaries();
		numberInstructions();
	}

	private void resolvePhiNodeUsages() {
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			if(b.localManager().hasUndefinedUsages()) {
				b.localManager().resolveUndefinedUsages();
			}
		}
	}

	/**
	 * An Efficient Method of Computing Static Single Assignment Form: Figure 4
	 */
	private void computePhiNodePlacement() {
		Map<String, Set<Block>> A_of_V = new HashMap<>();
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			Map<String, Temporary> written = b.localManager().written();
			for(String variable : written.keySet()) {
				if(!A_of_V.containsKey(variable)) {
					A_of_V.put(variable, new HashSet<>());
				}

				A_of_V.get(variable).add(b);
			}

//			try {
//				System.out.println("Block " + b.index() + " contains those variables: " + b.localManager().getAllAlive());
//			} catch(InvalidCompileOrderException e) {
//				e.printStackTrace();
//			}

		}

//		for(Map.Entry<String, Set<Block>> writes : A_of_V.entrySet()) {
//			System.out.print("Assignments to " + writes.getKey() + ": ");
//			for(Block b : writes.getValue()) {
//				try {
//					System.out.print(b.index() + " ");
//				} catch(InvalidCompileOrderException e) {
//					e.printStackTrace();
//				}
//			}
//			System.out.println();
//		}


		for(String variable : A_of_V.keySet()) {
			Set<Block> domFronPlus = new HashSet<>();
			Queue<Block> w = new ArrayDeque<>();
			Set<Block> work = new HashSet<>();

			// for each X in A(V) do
			for(Block x : A_of_V.get(variable)) {
				work.add(x);
				w.add(x);
			}

			Block x;
			// while W != empty set do take X from W
			while((x = w.poll()) != null) {
				// for each Y in DF(X) do
				for(Node y : x.DF()) {
					// if DomFronPlus(Y) = 0 then do
					if(domFronPlus.contains(y) == false) {
						((Block) y).phiManager().placePhiNode(variable, x);
						domFronPlus.add((Block)y);

						// if Work(Y) = 0 then do
						if(work.contains(y) == false) {
							work.add((Block)y);
							w.offer((Block) y);
						}
					}
				}
			}
		}

	}

	private void computeDominance() {
		DominanceTree dominanceTree = new N2DominanceTree(initialBlock);
		new DominanceFrontier(initialBlock);

//		System.out.println(name + "---");
//		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
//			Node n = it.next();
//			try {
//				System.out.println(((Block)n).index());
//				for(Node x : n.DOM()) {
//					System.out.println("> dominated by " + ((Block)x).index());
//				}
//
//				Node x = n.IDOM();
//				if(x != null) {
//					System.out.println(">> idom " + ((Block) x).index());
//				}
//
//				for(Node f : n.DF()) {
//					System.out.println("> frontier: " + ((Block)f).index());
//				}
//
//			} catch(InvalidCompileOrderException e) {
//				e.printStackTrace();
//			}
//		}
	}

	private void numberBlocks() {
		int blockCount = 0;
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();
			b.setIndex(blockCount++);
		}
	}

	private void removeRedundantBlocks() {
		// todo remove blocks with 0 instructions - need to implement a listIterator in Block for that
		Map<Block, Block> blocksToRemove = new HashMap<>();

		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();
			if(b.instructions.stream().noneMatch(Opcodes::hasOpcode)) {
				if(b.children().size() != 1) {
					throw new UnsupportedOperationException("Cannot remove blocks with more than one kid");
				}

				blocksToRemove.put(b, b.children().get(0));
			}
		}

		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			for(Compileable c : b.instructions) {
				if(c instanceof Goto) {
					Block target = ((Goto) c).label();
					((Goto) c).setLabel(blocksToRemove.getOrDefault(target, target));
				} else if(c instanceof ConditionalGoto) {
					((ConditionalGoto) c).replaceJumpTargets(blocksToRemove);
				}
			}

		}

		for(Block b : blocksToRemove.keySet()) {
			Block kid = b.children().get(0);

			kid.parents().remove(b);

			for(Block parent : b.parents()) {
				if(parent.children().size() != 1) {
					parent.children().replaceAll(child -> {
						if(child == b) {
							return kid;
						} else {
							return child;
						}
					});
				} else {
					parent.children().remove(b);
					parent.children().add(kid);
				}
			}

			kid.instructions.addAll(0, b.instructions);

			kid.parents().addAll(b.parents());
		}
	}

	private void computeUncomputedPhiNodes() throws InvalidCompileOrderException {

		for(ListIterator<Block> it1 = initialBlock.blockIterator(); it1.hasNext(); ) {
			Block b = it1.next();

			b.phiManager().computePhiNodes();
		}
	}

	private void removeRedundantLoads() {
		Iterator<Compileable> it = initialBlock.instructionIterator();

		// variable -> temporary to use
		HashMap<Variable, Temporary> lookup = new HashMap<>();

		// invalid temporary -> temporary to use
		HashMap<Temporary, Temporary> useActual = new HashMap<>();

		while(it.hasNext()) {
			Compileable compileable = it.next();

			if(compileable instanceof Load) {
				Variable v = ((Load) compileable).variable();
				if(lookup.containsKey(v)) {
					useActual.put(((Load) compileable).result(), lookup.get(v));
					it.remove();
				} else {
					lookup.put(((Load) compileable).variable(), ((Load) compileable).result());
				}
			} else if(compileable instanceof Store) {
				Variable v = ((Store) compileable).variable();
				if(lookup.containsKey(v)) {
					Temporary t = lookup.get(v);
					Set<Map.Entry<Temporary, Temporary>> entries = useActual.entrySet();
					useActual.clear();

					for(Map.Entry<Temporary, Temporary> e : entries) {
						if(!e.getValue().equals(t)) {
							useActual.put(e.getKey(), e.getValue());
						}
					}
				}

				lookup.put(v, ((Store) compileable).value());
				compileable.substituteTemporaries(useActual);
			} else {
				compileable.substituteTemporaries(useActual);
			}
		}
	}

	private void insertVoid() {
		Block b = initialBlock;
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			b = it.next();
		}

		if(b.instructions.isEmpty() || !(b.instructions.get(b.instructions.size() - 1) instanceof Return)) {
			b.instructions.add(new Return());
		}
	}

	private void numberParamsLocals() {
		int index = 0;
		for(Variable v : parameters) {
			v.setIndex(index++);
		}

		for(Variable v : _locals) {
			v.setIndex(index++);
		}
	}

	private void numberTemporaries() throws InvalidCompileOrderException {
		int index = parameters.size();
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			index = b.phiManager().numberPhiNodes(index);

			for(Compileable c : b.instructions) {
				if(c instanceof Instruction) {
					Temporary result = ((Instruction) c).result();
					if(result != null) {
						result.setIndex(index++);
					}
				}
			}
		}
	}

	private void numberInstructions() throws InvalidCompileOrderException {
		int index = 0;
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			index =  b.phiManager().numberPhiInstructions(index);

			for(Compileable c : b.instructions) {
				if(Opcodes.hasOpcode(c)) {
					index++;
				}
			}
		}

		this.instructionCount = index;
	}

//	private void addTemporary(Temporary type) {
//		temporaries.add(type);
//		nameToTemporary.put(type.name, type);
//	}

//	public Temporary temporaryForVariable(Variable v) {
//		String name = v.name;
//
//		if(nameToTemporary.get(v.name) != null) {
//			int x = 1;
//
//			while(nameToTemporary.get(name = (v.name + x)) != null) {
//				x++;
//			}
//		}
//
//		Temporary temporary = new Temporary(name, this);
//		addTemporary(temporary);
//		return temporary;
//	}

	private String generateName(int x) {
		StringBuilder name = new StringBuilder();
		if(x == 0) {
			return "a";
		}

		while(x > 0) {
			char next = (char) ((x % 26) + 'a');

			x /= 26;

			if(x == 0) {
				name.insert(0, (char)(next -1));
			} else {
				name.insert(0, next);
			}


		}

		return name.toString();
	}

//	public Temporary createTemporary() {
//		Temporary x = new Temporary(generateName(temporaries.size()), this);
//		addTemporary(x);
//		return x;
//	}

	@Override
	public String toString() {
		String paramString = parameters.toString().replaceAll("\\[|\\]", "");
		StringBuilder sb = new StringBuilder("fn " + name + "(" + paramString + ") => " + returntype);
		sb.append("\n\t");
		sb.append(String.join(", ", _locals.stream().map(Object::toString).collect(Collectors.toList())));
		sb.append('\n');

		sb.append("{\n");

		for(Compileable i : initialBlock.instructions()) {

			sb.append(i);
			sb.append('\n');
		}

		sb.append("}\n");

		return sb.toString();
	}

	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		// print name
		dos.writeUTF(name,"fn " + name + " (index: " + index + ")");

		if(isMain()) {
			dos.writeShort(0, "no arguments");
		} else {
			// print param count
			dos.writeShort(parameters.size(), parameters.size() + " arguments");

			for(Variable parameter : parameters) {
				parameter.writeToStream(dos);
			}
		}

		// return type
		dos.writeByte(returntype.getRepresentation(), "returns " + returntype.toString());

		// blocks
		int blockCount = 0;
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();
			blockCount++;
		}

		dos.writeShort(blockCount, blockCount + " blocks in total: ");

		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();
			dos.writeShort(b.countCompiledInstructions(), " " + b.countCompiledInstructions() + " instructions      (block " + b.index() + ")");
			dos.writeShort(b.children().size(), "  " + b.children().size() + " successors");
			for(Block successor : b.children()) {
				dos.writeShort(successor.index(), "   block " + successor.index());
			}
		}

		dos.writeShort(instructionCount, instructionCount + " instructions in total");

		int instructionCounter = 0;
		for(ListIterator<Block> it = initialBlock.blockIterator(); it.hasNext(); ) {
			Block b = it.next();

			dos.section("Block " + b.index());

			for(PhiNode phiNode : b.phiManager().computedPhiNodes()) {
				dos.prefix(instructionNumberString(instructionCounter++, instructionCount));
				phiNode.writeToStream(dos);
			}

			for(Compileable i : b.instructions) {
				if(Opcodes.hasOpcode(i)) {
					dos.prefix(instructionNumberString(instructionCounter++, instructionCount));
				} else {
					dos.prefix("");
				}
				i.writeToStream(dos);
			}

			dos.prefix("");
		}
	}

	private static String instructionNumberString(int instructionCounter, int maxInstructionNumber) {
		String comment = "[";
		String instructionNumber = instructionCounter + "";
		for(int x = instructionNumber.length(); x < (maxInstructionNumber + "").length(); x++) {
			comment += " ";
		}
		comment += instructionNumber + "] ";
		return comment;
	}

//	@Override
//	public Variable createVariable(DeprecatedType type, String name) {
//		for(Variable f : _locals) {
//			if(f.name.equals(name)) {
//				return f;
//			}
//		}
//
//		Variable v = new Variable(name, type, this);
//		_locals.add(v);
//		return v;
//	}

	public Variable getVariable(String s) {
		for(Variable f : _locals) {
			if(f.name.equals(s)) {
				return f;
			}
		}

		for(Variable f : parameters) {
			if(f.name.equals(s)) {
				return f;
			}
		}

		return null;
	}

//	@Override
//	public <T> Temporary getTemporaryForConstant(DeprecatedType type, T value) {
//		Map.Entry<DeprecatedType, Object> key = new AbstractMap.SimpleEntry<DeprecatedType, Object>(type, value);
//
//		if(constants.get(key) != null) {
//			return constants.get(key).result();
//		} else {
//			Const<T> c = new Const<>(new Temporary(value + "", this), type, value);
//			constants.put(key, c);
//			instructions.add(c);
//			return c.result();
//		}
//	}

	public List<Variable> locals() {
		return _locals;
	}

	/**
	 * This sets the global function index
	 *
	 * @param index
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * This sets the vTable index
	 *
	 * @param index
	 */
	public void setLocalIndex(byte index) {
		this.localIndex = index;
	}

	public int index() {
		return index;
	}

	public byte localIndex() {
		return localIndex;
	}

	public String name() {
		return name;
	}

	public ObjectType type() {
		return type;
	}

	public int instructionCount() {
		return instructionCount;
	}

	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		for(Compileable c : initialBlock.instructions()) {
			c.substituteTemporaries(substitute);
		}
	}

	public boolean isMain() {
		return (isStatic && name().equals("main") && parameters.size() == 0);
	}

	public boolean isConstructor() {
		return name().equals(type.name) && returntype == PrimitiveType.VOID;
	}

	public static Function createEmptyFunction(BytecodeFile file, ObjectType type, String name) {
		return new Function(file, type, PrimitiveType.VOID, new BlockStmt(), name, false, new NodeList<>());
	}

}
