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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import judoku.Grid;
import judoku.Generator;
import judoku.Solver;
import judoku.Util;

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
						syserrln("bad size: " + e.toString());
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
						syserrln("bad dimensions: " + e.toString());
					}
				} else if (args[i].startsWith("--sym=")) {
					String mode = args[i].substring(6);
					Generator.Symmetry sym = Generator.Symmetry.toSymmetry(mode);
					if (sym == null)
						syserrln("unknown symmetry mode: " + mode);
					else
						optionSymmetry = sym;
				} else if (args[i].equals("--strict")) {
					optionStrict = true;
				} else if (args[i].startsWith("--csv=")) {
					try {
						optionCsv = new FileWriter(args[i].substring(6));
					} catch (IOException e) {
						syserrln("unable to write to file: " + e.toString());
					}
				} else if (args[i].startsWith("--json=")) {
					try {
						optionJson = new FileWriter(args[i].substring(7));
					} catch (IOException e) {
						syserrln("unable to write to file: " + e.toString());
					}
				} else if (args[i].equals("--empty")) {
					optionEmpty = true;
				} else
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
			syserrln("unable to write to file: " + e.toString());
		}

		try {
			if (optionJson != null) optionJson.close();
		} catch (IOException e) {
			syserrln("unable to write to file: " + e.toString());
		}
	}

	private static void usage() {
		System.out.println(
		//--------1---------2---------3---------4---------5---------6---------7---------8
		"Usage: " + Util.callersSimpleClassName() + " [OPTION]...\n" +
		"Generate a grid for a 'proper' puzzle. A proper puzzle is a grid that only has\n" +
		"one solution and, additionally, where removing any one of its givens would\n" +
		"yield a puzzle that had more than one solution.\n\n" +

		"  --size=N       A square grid of size N\n" +
		"  --box=W,H      Grid with boxes of width W and height H\n" +
		"  --sym=MODE     Symmetry mode, one of the following (or unique prefix of):\n" +
		"                 rotate180 (default), diagonal, horizontal, vertical, none,\n" +
		"                 random\n" +
		"  --strict       Stricly proper (but slower). Default is that puzzles may have\n" +
		"                 one additional given more than needed to be solvable\n" +
		"  --csv=FILE     Write corresponding CSV representation to FILE\n" +
		"  --json=FILE    Write corresponding JSON representation to FILE\n" +
		"  --empty        Generate an empty grid of the requested size"
		);

		System.exit(0);
	}

	private static void version() {
		String me = Util.callersSimpleClassName();
		String version = Util.jarVersion();
		String title = Util.jarTitle();
		String copyright = Util.jarCopyright();

		System.out.println(me + " " + (version == null ? "" : version));
		if (title != null) System.out.print(title + ". ");
		if (copyright != null) System.out.print(copyright);
		if (title != null || copyright != null) System.out.println();

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
				syserrln("invalid prototype: " + e.toString());
				return;			// unreachable
			}

			result = generator.generate(optionSymmetry, optionStrict);
			assert !optionStrict || Solver.isProper(result);
		}

		System.out.println(result.toString());

		try {
			if (optionCsv != null) result.toCsv(optionCsv);
			if (optionJson != null) result.toJson(optionJson);
		} catch (IOException e) {
			syserrln("unable to write to file: " + e.toString());
		}
	}

	private static Generator.Symmetry optionSymmetry = Generator.Symmetry.ROTATE180;
	private static boolean optionStrict = false;
	private static Writer optionCsv = null;
	private static Writer optionJson = null;
	private static boolean optionEmpty = false;
}
