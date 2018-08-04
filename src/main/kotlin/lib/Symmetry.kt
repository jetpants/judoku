package judoku

enum class Symmetry {
    ROTATE180, DIAGONAL, HORIZONTAL, VERTICAL,
    NONE;           // NONE should be last to be excluded from random selection

    fun apply(g: Grid, n: Int): Int {
        var col = g.toColumn(n)
        var row = g.toRow(n)

        when (this) {
            ROTATE180 -> return g.numCells + 1 - n
            DIAGONAL -> { val temp = col; col = row; row = temp }
            HORIZONTAL -> row = g.numRows + 1 - row
            VERTICAL -> col = g.numColumns + 1 - col
            NONE -> return n
        }

        return g.toNth(col, row)
    }

    companion object {
        @JvmStatic
        fun toSymmetry(prefix: String): Symmetry? {
            var matches = 0
            var sym: Symmetry? = null

            if ("RANDOM".startsWith(prefix.toUpperCase())) {
                ++matches
                sym = random()
            }

            for (v in Symmetry.values())
                if (v.toString().startsWith(prefix.toUpperCase())) {
                    ++matches
                    sym = v
                }

            return if (matches == 1) sym else null
        }

        @JvmStatic
        fun random(): Symmetry {
            val values = Symmetry.values()
            return values[Util.random.nextInt(values.size - 1)]       // exclude NONE
        }
    }
}
