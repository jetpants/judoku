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

package judoku;

import java.util.Random;

public class Util {
	public static String callersClassName() {
		// [0]=Thread, [1]=Util, [2]=caller
		return Thread.currentThread().getStackTrace()[2].getClassName();	// fully qualified
	}

	public static String callersSimpleClassName() {
		// [0]=Thread, [1]=Util, [2]=caller
		String me = Thread.currentThread().getStackTrace()[2].getClassName();

		try {
			me = Class.forName(me).getSimpleName();
		} catch (ClassNotFoundException e) {}

		return me;
	}

	public static String jarVersion() {
		// configured in <project-root>/build.gradle
		return Util.class.getPackage().getImplementationVersion();
	}

	public static String jarTitle() {
		// configured in <project-root>/build.gradle
		return Util.class.getPackage().getImplementationTitle();
	}

	public static String jarCopyright() {
		// configured in <project-root>/build.gradle
		java.net.URLClassLoader cl = (java.net.URLClassLoader) Util.class.getClassLoader();

		try {
  			java.net.URL url = cl.findResource("META-INF/MANIFEST.MF");
			if (cl != null) {
	  			java.util.jar.Manifest manifest = new java.util.jar.Manifest(url.openStream());
	  			java.util.jar.Attributes mainAttributes = manifest.getMainAttributes();
	  			return mainAttributes.getValue("Implementation-Copyright");
			}
		} catch (java.io.IOException E) {}

		return null;
	}

    /*package*/ static void shuffle(int[] array) {
		// Fisher-Yates shuffle algorithm
		for (int i = array.length - 1; i > 0; --i) {
			int j = random.nextInt(i + 1);
			int temp = array[i];
			array[i] = array[j];
			array[j] = temp;
			}
    }

    private static Random random = new Random(System.currentTimeMillis());
}
