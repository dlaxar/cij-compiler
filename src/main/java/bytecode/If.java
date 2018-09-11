package bytecode;

import stream.AnnotatedDataOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class If implements Compileable {

	private Temporary condition;
	private Block then;
	private Block elze;
	private Block postif;

	private List<Compileable> logicals = new ArrayList<>();

	public If(Temporary condition, Block context, Block then, Block elze, Block postif) {
		this.condition = condition;
		this.then = then;
		this.elze = elze;
		this.postif = postif;

		this.logicals.add(new Comment("if:"));
		this.logicals.add(new ConditionalGoto(condition, context, elze, then));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Compileable c : logicals) {
			sb.append("\t").append(c.toString()).append("\n");
		}
		return sb.toString();
	}

	@Override
	public void writeToStream(AnnotatedDataOutput dos) throws IOException {
		throw new IOException();
	}

	public List<Compileable> flatten() {
		return logicals;
	}

	@Override
	public void substituteTemporaries(Map<Temporary, Temporary> substitute) {
		condition = substitute.getOrDefault(condition, condition);

		then.substituteTemporaries(substitute);
		elze.substituteTemporaries(substitute);
		postif.substituteTemporaries(substitute);
	}
}
