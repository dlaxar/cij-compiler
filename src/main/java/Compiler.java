import bytecode.BytecodeFile;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import exceptions.TypeNotSupportedException;
import exceptions.UndefinedFunctionNameException;
import stream.AnnotatedDataOutput;
import stream.LittleEndianOutputStream;

import java.io.*;
import java.nio.ByteBuffer;

public class Compiler {

	public static void usage() {
		System.out.println("compiler");
		System.out.println("Usage: compiler [-d] [--hashbang mode] file.java");
		System.out.println();
		System.out.println("\t--help              This help message");
		System.out.println("\t--hashbang mode     The mode string will be passed to the vm");
		System.out.println("\t-d                  Enable debug mode");
	}

	public static void main(String[] args) throws IOException, TypeNotSupportedException, UndefinedFunctionNameException {
		boolean debug = false;
		String hashbang = null;
		String filename = null;
		File outfile = null;

		for(int i = 0; i < args.length; i++) {
			String argument = args[i];
			// long options
			if(argument.startsWith("--")) {
				if(argument.equals("--help")) {
					usage();
					return;
				}
				if(argument.equals("--hashbang")) {
					if(i+1 >= args.length || args[i+1].startsWith("-")) {
						System.err.println("--hashbang requires an argument for the vm");
						return;
					} else {
						hashbang = args[++i];
						continue;
					}
				}
			}
			// short options
			else if(argument.startsWith("-") && argument.length() > 1) {
				for(int j = 1; j < argument.length(); j++) {
					char c = argument.charAt(j);
					switch(c) {
						case 'h':
							usage();
							return;
						case 'd':
							debug = true;
							break;
					}
				}
			}
			// positional argument
			else {
				if(filename == null) {
					filename = argument;
				} else if(outfile == null) {
					outfile = new File(argument);
				} else {
					System.err.println("Only one file is supported at the moment");
					usage();
					return;
				}
			}
		}

		if(filename == null) {
			System.err.println("No file given. Aborting.");
			return;
		}

		if(outfile == null) {
			outfile = new File(new File(filename).getName() + ".cij");
		}

		FileOutputStream out = new FileOutputStream(outfile);
		AnnotatedDataOutput dos = new LittleEndianOutputStream(out);

		ParserConfiguration parserConfiguration = new ParserConfiguration();
		parserConfiguration.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
		JavaParser.setStaticConfiguration(parserConfiguration);
		CompilationUnit cu = JavaParser.parse(new FileInputStream(filename));


		BytecodeFile bytecode = new BytecodeFile(outfile, cu);

		if(hashbang != null) {
			bytecode.setHashbang(hashbang);
			outfile.setExecutable(true);
		}

		bytecode.writeToStream(dos);
		out.close();

		if(debug) {
			String map = outfile + ".debug";
			out = new FileOutputStream(map);
			out.write(dos.toAnnotatedBytecode().getBytes());
			out.close();
		}
	}
}
