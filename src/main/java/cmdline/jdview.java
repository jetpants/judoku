package cmdline;

import java.io.IOException;
import judoku.Grid;
import judoku.Solver;
import judoku.Util;

public class jdview {
	public static void main(String[] args) {
		for (int i = 0; i < args.length && !args[i].equals("--"); ++i)
			if (args[i].startsWith("--"))
				if (args[i].equals("--help"))
					usage();				// exits
				else if (args[i].equals("--version"))
					version();				// exits
				else
					syserrln("unknown option: " + args[i]);

		int numFiles = 0;

		boolean endOfOptions = false;
		for (String arg : args)
			if (endOfOptions || !arg.startsWith("--"))
				++numFiles;
			else if (arg.equals("--"))
				endOfOptions = true;

		if (numFiles == 0) usage();		// exits

		endOfOptions = false;
		for (String arg : args)
			if (endOfOptions || !arg.startsWith("--")) {
				if (numFiles > 1) System.out.println("\n" + arg);
				loadAndShow(arg);
			} else if (arg.equals("--"))
				endOfOptions = true;

	}

	private static void usage() {
		System.out.println(
		//--------1---------2---------3---------4---------5---------6---------7---------8
		"Usage: " + Util.callersSimpleClassName() + " JSONFILE...\n" +
		"Graphically display the grid stored in the JSON file."
		);

		System.exit(0);
	}

	private static void version() {
		String me = Util.callersSimpleClassName();
		String version = Util.jarVersion();
		String title = Util.jarTitle();
		String vendor = Util.jarVendor();

		System.out.println(me + " " + (version == null ? "" : version));
		if (title != null) System.out.print(title + ". ");
		if (vendor != null) System.out.print(vendor);
		if (title != null || vendor != null) System.out.println();

		System.exit(1);
	}

	private static void syserrln(String msg) {
		System.err.println(Util.callersSimpleClassName() + ": " + msg);
		System.exit(1);
	}

	private static void loadAndShow(String file) {
		Grid g = null;

		try {
			g = Grid.newFromJson(new java.io.FileReader(file));
		} catch (IOException e) {
			syserrln("cannot read file: " + file);
		} catch (Exception e) {
			syserrln("bad JSON or values: " + file);
		}

		assert(g != null);

		System.out.println(g.toString());
		System.out.println("total cells            " + g.getNumCells());
		System.out.println("filled/empty cells     " + g.numFilledCells() + "/" + g.numEmptyCells());
		System.out.println("is viable              " + g.isViable());
		System.out.println("  - no duplicates      " + !g.hasDuplicates());
		System.out.println("  - no zombie cells    " + !g.hasZombies());

		System.out.print  ("\nnumber of solutions    ");

		final int MAX = 1000;
		Solver solver = new Solver(g);

		solver.setNodeCounting(true);
		solver.setNodeCount(0);

		int solns = solver.countSolutions(MAX + 1);
		int nodes = solver.getNodeCount();

		System.out.println(solns > MAX ? String.format(">%,d", MAX) : String.format("%,d", solns));
		System.out.println("nodes traversed        " + String.format("%,d", nodes));
		System.out.println("is proper              " + solver.isProper());
		System.out.println("  - is unique          " + (solns == 1));
		System.out.println("  - is minimal         " + solver.isMinimal());
	}
}
