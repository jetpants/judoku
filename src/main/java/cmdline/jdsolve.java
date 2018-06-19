/*	Copyright (C) 2018 Steve Ball <jetpants@gmail.com>

	This file is part of Judoku. Judoku is free software: you can redistribute
	it and/or modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation, either version 3 of the License,
	or (at your option) any later version.

    Judoku is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
    details.

    You should have received a copy of the GNU General Public License along with
    Judoku. If not, see <http://www.gnu.org/licenses/>.
*/

package cmdline;

import java.io.IOException;
import java.io.FileWriter;
import java.io.Writer;
import judoku.Grid;
import judoku.Solver;
import judoku.Util;

public class jdsolve {
	public static void main(String[] args) {
		for (int i = 0; i < args.length && !args[i].equals("--"); ++i)
			if (args[i].startsWith("--"))
				if (args[i].equals("--help"))
					usage();				// exits
				else if (args[i].equals("--version"))
					version();				// exits
				else if (args[i].equals("--all"))
					optionMaxSolutions = -1;
				else if (args[i].equals("--count")) {
					optionCountOnly = true;
					optionMaxSolutions = -1;
				} else if (args[i].startsWith("--max=")) {
					try {
						optionMaxSolutions = Integer.parseInt(args[i].substring(6));
					} catch (NumberFormatException e) {
						syserrln("bad argument: " + args[i]);
					}
				} else if (args[i].startsWith("--csv=")) {
					try {
						optionCsv = new FileWriter(args[i].substring(6));
					} catch (IOException e) {
						syserrln("unable to write to file: " + e.getMessage());
					}
				} else if (args[i].startsWith("--json=")) {
					try {
						optionJson = new FileWriter(args[i].substring(7));
					} catch (IOException e) {
						syserrln("unable to write to file: " + e.getMessage());
					}
				} else
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
				if (numFiles > 1) System.out.println(arg);
				solve(arg);
			} else if (arg.equals("--"))
				endOfOptions = true;

		try {
			if (optionCsv != null) optionCsv.close();
		} catch (IOException e) {
			syserrln("unable to write to file: " + e.getMessage());
		}

		try {
			if (optionJson != null) optionJson.close();
		} catch (IOException e) {
			syserrln("unable to write to file: " + e.getMessage());
		}
	}

	private static void usage() {
		System.out.println(
		//--------1---------2---------3---------4---------5---------6---------7---------8
		"Usage: " + Util.callersSimpleClassName() + " [OPTION]... JSONFILE...\n" +
		"Calculate solutions for saved Sudoku grids.\n\n"+

		"  --all          Show all solutions\n" +
		"  --max=N        Show maximum of N solutions (default: 1)\n" +
		"  --count        Show number of solutions only\n" +
		"  --csv=FILE     Write CSV representation of solution to FILE\n" +
		"  --json=FILE    Write JSON representation of solution to FILE"
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

	private static void solve(String file) {
		Grid g;

		try {
			g = Grid.newFromJson(new java.io.FileReader(file));
			assert(g != null);
		} catch (Exception e) {
			syserrln("cannot read grid: " + file + ": " + e.getMessage());
			return;			// unreachable
		}

		if (!g.isViable()) {
			syserrln("grid has no solution: " + file);
			return;			// unreachable
		}

		Solver solver = new Solver(g);

		if (optionCountOnly) {
			int n = solver.countSolutions(optionMaxSolutions);
			System.out.println(Integer.toString(n));
		} else {
			System.out.println(g.toString());

			Grid[] solutions = solver.findSolutions(optionMaxSolutions);

			if (solutions.length == 0)
				syserrln("grid has no solution: " + file);
			else {
				for (Grid s : solutions)
					try {
						System.out.println(s.toString());
						if (optionCsv != null) s.toCsv(optionCsv);
						if (optionJson != null) s.toJson(optionJson);
					} catch (IOException e) {
						syserrln("unable to write to file: " + e.getMessage());
					}
			}
		}
	}

	private static boolean optionCountOnly = false;
	private static int optionMaxSolutions = 1;
	private static Writer optionCsv = null;
	private static Writer optionJson = null;
}
