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
import judoku.Move;
import judoku.Solver;
import judoku.Util;

public class jdedit {
	public static void main(String[] args) {
		for (int i = 0; i < args.length && !args[i].equals("--"); ++i)
			if (args[i].startsWith("--"))
				if (args[i].equals("--help"))
					usage();				// exits
				else if (args[i].equals("--version"))
					version();				// exits
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

		Grid out = null;

		if (file == null) {
			do {
				System.out.print("What size of grid (def. 9): ");
				String input = System.console().readLine();

				if (input.equals(""))
					out = new Grid();
				else if (input.equals("q"))
					return;
				else
					try {
						Pattern p = Pattern.compile("^\\s*(\\d+)\\s*$");
						Matcher m = p.matcher(input);

						if (m.matches())
							out = new Grid(Integer.parseInt(m.group(1)));
						else
							System.out.println("(E) Unrecognised input");
					} catch (NumberFormatException e) {
						System.out.println("(E) Bad size: " + e.getMessage());
					} catch (IllegalArgumentException e) {
						System.out.println("(E) " + e.getMessage());
					}
			} while (out == null);
		} else
			try {
				out = Grid.newFromJson(new java.io.FileReader(file));
			} catch (java.io.IOException e) {
				syserrln("cannot read file: " + file);
			} catch (Exception e) {
				syserrln("bad JSON or values: " + file);
			}

		System.out.println();
		assert(out != null);
		out = edit(out, file);
	}

	private static void usage() {
		System.out.println(
		//--------1---------2---------3---------4---------5---------6---------7---------8
		"Usage: " + Util.callersSimpleClassName() + " [JSONFILE]\n" +
		"Interactive grid editor.\n\n"+

		"  JSONFILE    Load this grid for editing (otherwise, start with an empty one)."
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

	private static Grid edit(Grid in, String file) {
		var out = in;

		System.out.println(out.toString());

		while (true) {
			System.out.print("Enter 'COL ROW VAL' or command (? for help): ");
			String cmd = System.console().readLine();

			if (cmd.equals("w") || cmd.equals("wq")) {
				if (file == null)
					System.out.print("Enter filename: ");
				else
					System.out.print("Enter filename (def. " + file + "): ");

				String f = System.console().readLine().trim();
				if (!f.equals("")) file = f;

				if (file == null || file.equals(""))
					System.out.println("(E) No filename given");
				else {
					try {
						Writer writer = new FileWriter(file);
						out.toJson(writer);
						writer.close();
						System.out.println("(I) File saved: " + file);
					} catch (IOException e) {
						System.out.println("(E) Unable to write to file: " + e.getMessage());
					}
				}

				if (cmd.equals("w")) continue;
			}

			if (cmd.equals("q") || cmd.equals("wq")) break;

			if (cmd.equals("?")) {
				System.out.println();
				System.out.println("  COL ROW VAL |  set cell at column and row to value (all 1-" + out.size() + ")");
				System.out.println("  COL ROW     |  clear value at column and row");
				System.out.println("  w           |  write grid to file");
				System.out.println("  q           |  quit");
				System.out.println("  wq          |  write and quit\n");
				continue;
			}

			int col, row, value = Grid.EMPTY;
			try {
				Pattern p = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)(?:\\s+(\\d+))?\\s*$");
				Matcher m = p.matcher(cmd);
				if (m.matches()) {
					col = Integer.parseInt(m.group(1));
					row = Integer.parseInt(m.group(2));
					if (m.group(3) != null) value = Integer.parseInt(m.group(3));
				} else {
					System.out.println("(E) Unrecognised input");
					continue;
				}
			} catch (NumberFormatException e) {
				System.out.println("(E) Invalid values: " + e.getMessage());
				continue;
			}

			if (col < 1 || col > out.numColumns()) {
				System.out.println("(E) Column number out of range");
				continue;
			}

			if (row < 1 || row > out.numRows()) {
				System.out.println("(E) Row number out of range");
				continue;
			}

			if (value != Grid.EMPTY && (value < 1 || value > out.size())) {
				System.out.println("(E) Cell value out of range");
				continue;
			}

			out = out.withCell(col, row, value);

			if (out.hasDuplicates()) System.out.println("(W) There's no possible solution: duplicate values");
			if (out.hasUnusable()) System.out.println("(W) There's no possible solution: unusable cells");
			if (!out.isViable()) System.out.println("(W) Puzzle is not viable");

			Move move = Solver.suggestMove(out);

			if (move == null)
			 	System.out.println("\n" + out.toString());
			else {
				out = out.withCell(move.getNth(), move.getValue());

				System.out.println("\nSuggested move: " + move.getRule().toString());
				System.out.println(move.getExplanation() + "\n");
				System.out.println(out.toString(move.getNth(), move.getOthers()));
			}
		}

		return out;
	}
}
