package cmdline;

import java.io.IOException;
import java.io.FileWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import judoku.*;

public class jdgen {
	public static void main(String[] args) {
		Grid g = new Grid();

		for (int i = 0; i < args.length && !args[i].equals("--"); ++i)
			if (args[i].startsWith("--"))
				if (args[i].equals("--help"))
					usage();				// exits
				else if (args[i].equals("--version"))
					version();				// exits
				else if (args[i].startsWith("--size=")) {
					try {
						int size = Integer.parseInt(args[i].substring(7));
						g = new Grid(size);
					} catch (NumberFormatException e) {
						syserrln("bad argument: " + args[i]);
					} catch (IllegalArgumentException e) {
						syserrln("bad size: " + e.getMessage());
					}
				} else if (args[i].startsWith("--box=")) {
					try {
						int w, h;
						Pattern p = Pattern.compile("^(\\d+),(\\d+)$");
						Matcher m = p.matcher(args[i].substring(6));
						if (m.matches()) {
							w = Integer.parseInt(m.group(1));
							h = Integer.parseInt(m.group(2));
							g = new Grid(w, h);
						} else
							syserrln("bad dimensions: " + args[i]);
					} catch (NumberFormatException e) {
						syserrln("bad argument: " + args[i]);
					} catch (IllegalArgumentException e) {
						syserrln("bad dimensions: " + e.getMessage());
					}
				} else if (args[i].startsWith("--sym=")) {
					String mode = args[i].substring(6);
					Symmetry sym = Symmetry.toSymmetry(mode);
					if (sym == null)
						syserrln("unknown symmetry mode: " + mode);
					else
						optionSymmetry = sym;
				} else if (args[i].equals("--fast")) {
					optionFast = true;
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
				} else if (args[i].equals("--empty")) {
					optionEmpty = true;
				} else if (args[i].startsWith("--seed="))
					try {
						long n = Long.parseLong(args[i].substring(7));
						Util.setRandom(new java.util.Random(n));
					} catch (NumberFormatException e) {
						syserrln("bad argument: " + args[i]);
					}
				else
					syserrln("unknown option: " + args[i]);

		boolean endOfOptions = false;
		for (String arg : args)
			if (endOfOptions || !arg.startsWith("--"))
				syserrln("too many arguments: " + arg);
			else if (arg.equals("--"))
				endOfOptions = true;

		generate(g);

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
		"Usage: " + Util.callersSimpleClassName() + " [OPTION]...\n" +
		"Generate a grid for a 'proper' puzzle. A proper puzzle is a grid that has only\n" +
		"one solution and, additionally, where removing any one of its clues would\n" +
		"yield a puzzle that would no longer have a unique solution.\n\n" +

		"  --size=N       A square grid of size N\n" +
		"  --box=W,H      Grid with boxes of width W and height H\n" +
		"  --sym=MODE     Symmetry mode, one of the following (or unique prefix of):\n" +
		"                 rotate180 (default), diagonal, horizontal, vertical, none,\n" +
		"                 random\n" +
		"  --fast         Quicker but puzzles may not be proper; there may be one more\n" +
		"                 extra clue than strictly required to have a unique solution\n" +
		"  --csv=FILE     Write corresponding CSV representation to FILE\n" +
		"  --json=FILE    Write corresponding JSON representation to FILE\n" +
		"  --empty        Generate an empty grid of the requested size\n" +
		"  --seed=SEED    Seeds the random number generator with SEED."
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

	private static void generate(Grid g) {
		Grid result = g;

		if (!optionEmpty) {
			Generator generator;
			try {
				generator = new Generator(g);
			} catch (IllegalArgumentException e) {
				syserrln("invalid prototype: " + e.getMessage());
				return;			// unreachable
			}

			result = generator.generate(optionSymmetry, !optionFast /*fast != minimal*/);
			assert optionFast || Solver.isProper(result);
		}

		System.out.println(result.toString());

		try {
			if (optionCsv != null) result.toCsv(optionCsv);
			if (optionJson != null) result.toJson(optionJson);
		} catch (IOException e) {
			syserrln("unable to write to file: " + e.getMessage());
		}
	}

	private static Symmetry optionSymmetry = Symmetry.ROTATE180;
	private static boolean optionFast = false;
	private static Writer optionCsv = null;
	private static Writer optionJson = null;
	private static boolean optionEmpty = false;
}
