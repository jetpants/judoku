package judoku

import java.util.Random

fun <T> Array<T>.shuffle() {
    val random = Util.random

    // Fisher-Yates shuffle algorithm
    for (i in this.size - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val temp = this[i]
        this[i] = this[j]
        this[j] = temp
    }
}

fun IntArray.shuffle() {
    val random = Util.random

    // Fisher-Yates shuffle algorithm
    for (i in this.size - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val temp = this[i]
        this[i] = this[j]
        this[j] = temp
    }
}

object Util {
    @JvmStatic fun getRandom() = random
    @JvmStatic fun setRandom(r: Random) { random = r }
    internal var random = Random()

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
    fun jarVendor(): String? {
        // configured in <project-root>/build.gradle
        return Util::class.java.getPackage().getImplementationVendor()
    }

    @JvmStatic
    fun isAnsiTerminal(): Boolean {
        val TERM = java.lang.System.getenv("TERM")
        return TERM?.matches(Regex("ansi|xterm")) != null
    }

    const val ANSI_NORMAL = "\u001b[0m"
    const val ANSI_BOLD   = "\u001b[1m"
    const val ANSI_FAINT  = "\u001b[2m"

    const val ANSI_LINE_DRAWING_ON  = "\u000e"
    const val ANSI_LINE_DRAWING_OFF = "\u000f"
}
