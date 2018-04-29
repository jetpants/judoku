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

package judoku

import java.util.Random

object Util {
    private val random = Random(System.currentTimeMillis())

    @JvmStatic
    fun callersClassName(): String {
        // [0]=Thread, [1]=Util, [2]=caller
        return Thread.currentThread().getStackTrace()[2].getClassName()    // fully qualified
    }

    @JvmStatic
    fun callersSimpleClassName(): String {
        // [0]=Thread, [1]=Util, [2]=caller
        var me = Thread.currentThread().getStackTrace()[2].getClassName()

        try {
            me = Class.forName(me).getSimpleName()
        } catch (e: ClassNotFoundException) {
        }

        return me
    }

    @JvmStatic
    fun jarVersion(): String? {
        // configured in <project-root>/build.gradle
        return Util::class.java.getPackage().getImplementationVersion()
    }

    @JvmStatic
    fun jarTitle(): String? {
        // configured in <project-root>/build.gradle
        return Util::class.java.getPackage().getImplementationTitle()
    }

    @JvmStatic
    fun jarCopyright(): String? {
        // configured in <project-root>/build.gradle
        val cl = Util::class.java.getClassLoader() as java.net.URLClassLoader

        try {
            val url = cl.findResource("META-INF/MANIFEST.MF")

            if (url != null) {
                val manifest = java.util.jar.Manifest(url.openStream())
                val mainAttributes = manifest.getMainAttributes()
                return mainAttributes.getValue("Implementation-Copyright")
            }
        } catch (E: java.io.IOException) {
        }

        return null
    }

    @JvmStatic
    /*package*/ fun shuffle(array: IntArray) {
        // Fisher-Yates shuffle algorithm
        for (i in array.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
    }
}
