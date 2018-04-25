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
import judoku.Grid;
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
				if (numFiles > 1) System.out.println(arg);
				loadAndShow(arg);
			} else if (arg.equals("--"))
				endOfOptions = true;

	}

	private static void usage() {
		System.out.println(
		//--------1---------2---------3---------4---------5---------6---------7---------8
		"Usage: " + Util.callersSimpleClassName() + " FILE...\n" +
		"Graphically display the grid stored in the JSON file."
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

	private static void loadAndShow(String file) {
		try {
			Grid g = Grid.newFromJson(new java.io.FileReader(file));
			assert(g != null && g.isLegal());

			System.out.println(g.toString());
		} catch (IOException e) {
			syserrln("cannot read file: " + file);
		} catch (Exception e) {
			syserrln("bad JSON or values: " + file);
		}
	}
}
