package visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;

public class MethodVisitor extends VoidVisitorAdapter<Set<MethodDeclaration>> {
	@Override
	public void visit(MethodDeclaration n, Set<MethodDeclaration> arg) {
		super.visit(n, arg);
		arg.add(n);
	}
}
