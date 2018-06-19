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

import judoku.Generator;
import judoku.Grid;
import judoku.Solver;
import judoku.Util;

public class jdspeed {
	public static void main(String[] args) {
		for (int i = 0; i < args.length && !args[i].equals("--"); ++i)
			if (args[i].startsWith("--"))
				if (args[i].equals("--help"))
					usage();				// exits
				else if (args[i].equals("--version"))
					version();				// exits
				else if (args[i].startsWith("--n="))
					try {
						int n = Integer.parseInt(args[i].substring(4));
						if (n < 1)
							syserrln("bad argument: " + args[i]);
						else
							optionN = n;
					} catch (NumberFormatException e) {
						syserrln("bad argument: " + args[i]);
					}
				else
					syserrln("unknown option: " + args[i]);

		boolean endOfOptions = false;
		String file = null;
		for (String arg : args)
			if (endOfOptions || !arg.startsWith("--")) {
				if (file != null)
					syserrln("too many arguments: " + arg);
				file = arg;
			} else if (arg.equals("--"))
				endOfOptions = true;

		if (file == null) {
			int n = (optionN + TESTS_PER_GRID - 1) / TESTS_PER_GRID;
			Grid[] testGrids = genTestGrids(n);
			speedtest(testGrids);
		} else
			try {
				Grid g = Grid.newFromJson(new java.io.FileReader(file));
				assert(g != null);

				if (!g.isViable()) {
					syserrln("grid has no solution: " + file);
					return;
				}

				System.out.println(g.toString());

				speedtest(new Grid[] { g });
			} catch (java.io.IOException e) {
				syserrln("cannot read file: " + e.getMessage());
			} catch (Exception e) {
				syserrln("bad JSON or values: " + e.getMessage());
			}
	}

	private static void usage() {
		System.out.println(
	    //--------1---------2---------3---------4---------5---------6---------7---------8
		"Usage: " + Util.callersSimpleClassName() + " [OPTION]... [JSONFILE]\n" +
		"Tests speed of solution finding heuristic.\n\n" +

		"  JSONFILE    Test the speed by solving this puzzle multiple times. If no file\n" +
		"              is given, testing is performed using randomly generated puzzles.\n" +
		"  --n=N       Run N tests (default: " + optionN + ")."
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

	private static Grid[] genTestGrids(int n) {
		/*	increasing 'n' doesn't increase the number of solutions that are part of the test,
		  	only the number of test grids. Increasing the number of test grids should minimise
		  	variance in the average time. Some grids are much quicker to solve than others. */

		Generator generator = new Generator(new Grid(9));

		Grid[] out = new Grid[n];

		String prompt = "\rGenerating test-grids... ";

		for (int i = 0; i < out.length; ++i) {
			System.out.print(prompt + String.format("%.0f", 100.0 * i / out.length) + "%");
			out[i] = generator.generate(Generator.Symmetry.ROTATE180, false);
		}

		System.out.println(prompt + "100%");
		return out;
	}

	private static void speedtest(Grid[] testGrids) {
		String prompt = "\rSolving test-grids...... ";

		/*	why use nanoTime() and not currentTimeMillis()? - for a fascinating answer, see
			here: https://stackoverflow.com/a/1776053 */
		long start = System.nanoTime();

		for (int i = 0; i < optionN; ++i) {
			System.out.print(prompt + String.format("%.0f", 100.0 * i / optionN) + "%");
			int n = Solver.countSolutions(testGrids[i % testGrids.length], 1);
			assert n == 1;
		}

		long elapsed = System.nanoTime() - start;
		double seconds = elapsed / 1000000000.0;

		System.out.println(prompt + "100%\n");
		System.out.println("Elapsed time:    " + String.format("%,.1f", seconds) + "s");
		System.out.println("Number grids:    " + String.format("%,d", optionN));
		System.out.println("Grids/second:    " + String.format("%,.0f", optionN / seconds));
	}

	private static int optionN = 1000;		//number of solutions in test

	// this number has been tuned such that the program spends about as much time generating
	// puzzles as solving them
	private static final int TESTS_PER_GRID = 20;
}
