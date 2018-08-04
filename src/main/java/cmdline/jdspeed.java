package cmdline;

import judoku.Generator;
import judoku.Grid;
import judoku.Solver;
import judoku.Symmetry;
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
				else if (args[i].equals("--lean")) {
					optionLean = true;
					ClassLoader loader = ClassLoader.getSystemClassLoader();
    				loader.setDefaultAssertionStatus(false);
					loader.setPackageAssertionStatus("judoku", false);
				} else if (args[i].startsWith("--seed="))
					try {
						optionSeed = Long.parseLong(args[i].substring(7));
					} catch (NumberFormatException e) {
						syserrln("bad argument: " + args[i]);
					}
				else if (args[i].startsWith("--force="))
					try {
						optionForceSeconds = Double.parseDouble(args[i].substring(8));
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

		Util.setRandom(new java.util.Random(optionSeed));

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
		"  --n=N       Run N tests (default: " + optionN + ").\n" +
		"  --real      Disable assertions and don't display the percentage progress.\n" +
		"  --seed=N    Seeds the random number generator with N (default: " + optionSeed + ")."
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
		if (optionForceSeconds != 0.0) return null;

		/*	increasing 'n' doesn't increase the number of solutions that are part of the test,
		  	only the number of test grids. Increasing the number of test grids should minimise
		  	variance in the average time. Some grids are much quicker to solve than others. */

		Generator generator = new Generator(new Grid(9));

		Grid[] out = new Grid[n];

		String prompt = "\rGenerating test-puzzles... ";
		System.out.print(prompt);

		int percent = -1;

		for (int i = 0; i < out.length; ++i) {
			if (!optionLean) {
				int p = (int) Math.round(100.0 * i / out.length);
				if (p != percent) System.out.print(prompt + (percent = p) + "%");
			}

			out[i] = generator.generate(Symmetry.NONE, false /*minimal*/);
		}

		System.out.println(prompt + "100%");
		return out;
	}

	private static void speedtest(Grid[] testGrids) {
		double seconds = solveTestGrids(testGrids);

		String bold = Util.isAnsiTerminal() ? Util.ANSI_BOLD : "";
		String norm = Util.isAnsiTerminal() ? Util.ANSI_NORMAL : "";

		System.out.println();
		System.out.println("  Elapsed time:        " + bold + String.format("%,.1f", seconds) + " s" + norm);
		System.out.println("  Puzzles solved:      " + bold + String.format("%,d", optionN) + norm);
		System.out.println("  Solutions/second:    " + bold + String.format("%,.0f", optionN / seconds) + norm);
		System.out.println("  Avg-time/solution:   " + bold + String.format("%,.0f", seconds * 1000000 / optionN) + " µs" + norm);
	}

	private static double solveTestGrids(Grid[] testGrids) {
		if (optionForceSeconds != 0.0) return optionForceSeconds;

		String prompt = "\rSolving test-puzzles...... ";
		System.out.print(prompt);

		int percent = -1;

		/*	why use nanoTime() and not currentTimeMillis()? - for a fascinating answer,
			see here: https://stackoverflow.com/a/1776053 */
		long start = System.nanoTime();

		for (int i = 0; i < optionN; ++i) {
			if (!optionLean) {
				int p = (int) Math.round(100.0 * i / optionN);
				if (p != percent) System.out.print(prompt + (percent = p) + "%");
			}

			int n = Solver.countSolutions(testGrids[i % testGrids.length], 1);
			assert n == 1;
		}

		long elapsed = System.nanoTime() - start;

		System.out.println(prompt + "100%");

		return elapsed / 1000000000.0;
	}

	private static int optionN = 10000;		// default number of solutions in test

	/*	Default random number seed. Very important for consistent timings as grids vary greatly
		in complexity and time to solve. */
	private static long optionSeed = 0L;

	/*	This option disables assertions and suppresses the displaying of the percentage
		progress bar, which gives slightly more accurate timings. */
	private static boolean optionLean = false;

	// this was useful for quickly re-generating screenshots of the timings if I changed the
	// format of the output for some optimisation articles I was writing
	private static double optionForceSeconds = 0.0;

	// this number has been tuned such that the program spends about as much time generating
	// puzzles as solving them
	private static final int TESTS_PER_GRID = 65;
}
