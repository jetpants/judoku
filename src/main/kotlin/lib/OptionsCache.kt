package judoku

internal class OptionsCache(initial: Grid) {
    /*  we deliberately don't store the grid to which this cache initially referred. It would
        immediately get out of date and this isn't the place to keep it updated. Instead, users
        of this class need to track the changing grid to which this cache corresponds. There's a
        grid held here but it's an empty copy of the one used to initialise the cache so that it
        can be used to do dimension conversions between columns, rows, cell-n, boxes, bands,
        stacks, etc. */
    private val conv = Grid(initial.boxWidth, initial.boxHeight)

    private val rows: IntArray
    private val columns: IntArray
    private val boxes: IntArray
    private val empty: BooleanArray

    init {
        // you could upsize the masks from Int to Long if you needed larger grids
        check(initial.size < Integer.SIZE)

        empty = BooleanArray(1 + conv.numCells, { if (it == 0) false else initial.isEmpty(it) })

        /*  these are masks of which values are allowed (i.e., those that don't appear already
            in the peer cells of the column, row and box. */
        columns = IntArray(1 + conv.numColumns, { if (it == 0) -1 else columnMask(initial, it) })
        rows = IntArray(1 + conv.numRows, { if (it == 0) -1 else rowMask(initial, it) })
        boxes = IntArray(1 + conv.numBoxes, { if (it == 0) -1 else boxMask(initial, it) })
    }

    data class Cell(val n: Int, val mask: Int, val popcount: Int)

    fun fewestOptions(): Cell? = with (conv) {
        var best: Cell? = null
        var n = 0

        /*  Values are stored internally in the Grid class in row-major order, so iterate
            columns within rows */
        for (row in 1..numRows) {
            if (rows[row] == 0) { n += numColumns; continue }

            for (col in 1..numColumns) {
                if (!empty[++n]) continue;

                val band = toBandFromRow(row)
                val stack = toStackFromColumn(col)
                val box = toBoxFromStackBand(stack, band)

                val mask = rows[row] and columns[col] and boxes[box]
                val popcount = Integer.bitCount(mask)

                when {
                    popcount == 0 -> return Cell(n, mask, popcount)   // unsuccessful leaf node
                    popcount == 1 -> return Cell(n, mask, popcount)
                    best == null -> best = Cell(n, mask, popcount)
                    popcount < best.popcount -> best = Cell(n, mask, popcount)
                }
            }
        }

        return best
    }

    fun update(n: Int, old: Int, new: Int) = with (conv) {
        if (old == new) return

        assert(empty[n] == (old == Grid.EMPTY))
        empty[n] = new == Grid.EMPTY

        val row = toRow(n)
        val col = toColumn(n)
        val box = toBoxFromStackBand(toStackFromColumn(col), toBandFromRow(row))

        /*  WARNING - users of this class must ensure that the grid has no dup values */

        if (old != Grid.EMPTY) {
            val value = 1 shl old

            // check the old value was previously reset...
            assert((columns[col] and value) == 0)
            assert((rows[row] and value) == 0)
            assert((boxes[box] and value) == 0)

            // ...and now set it
            columns[col] = columns[col] or value
            rows[row] = rows[row] or value
            boxes[box] = boxes[box] or value
        }

        if (new != Grid.EMPTY) {
            val value = 1 shl new
            val inv = value.inv()

            // check the new value was previously set...
            assert((columns[col] and value) != 0)
            assert((rows[row] and value) != 0)
            assert((boxes[box] and value) != 0)

            // ...and now reset it
            columns[col] = columns[col] and inv
            rows[row] = rows[row] and inv
            boxes[box] = boxes[box] and inv
        }
    }

    companion object {
        fun fewestOptionsUncached(g: Grid): Cell? {
            /*  WARNING - this is the uncached equivalent to fewestOptions(). Its advantage is
                that you don't need to have maintained the cache up-to-date; you can just give
                it a Grid and get an answer. It's much slower though, about 2.5x */

            var best: Cell? = null

            var n = 0

            for (row in 1..g.numRows)
                for (col in 1..g.numColumns)
                    if (g.isEmpty(++n)) {
                        val mask = g._getOptionsMask(col, row)
                        val popcount = Integer.bitCount(mask)

                        when {
                            popcount == 0 -> return Cell(n, mask, popcount)   // unsuccessful leaf node
                            popcount == 1 -> return Cell(n, mask, popcount)
                            best == null -> best = Cell(n, mask, popcount)
                            popcount < best.popcount -> best = Cell(n, mask, popcount)
                        }
                    }

            return best
        }

        private fun rowMask(initial: Grid, row: Int): Int {
            var mask = initial.MEGA_MASK

            with (initial) {
                var n = toNth(1, row)
                repeat (numColumns) {
                    val value = getCell(n)
                    n += 1
                    mask = mask and (1 shl value).inv()
                }
            }

            return mask
        }

        private fun columnMask(initial: Grid, col: Int): Int {
            var mask = initial.MEGA_MASK

            with (initial) {
                var n = toNth(col, 1)
                repeat (numRows) {
                    val value = getCell(n)
                    n += numColumns
                    mask = mask and (1 shl value).inv()
                }
            }

            return mask
        }

        private fun boxMask(initial: Grid, box: Int): Int {
            var mask = initial.MEGA_MASK

            with (initial) {
                val stack = toStackFromBox(box)
                val band = toBandFromBox(box)

                for (col in toLeftColumnFromStack(stack)..toRightColumnFromStack(stack))
                    for (row in toTopRowFromBand(band)..toBottomRowFromBand(band)) {
                        val value = getCell(toNth(col, row))
                        mask = mask and (1 shl value).inv()
                    }
            }

            return mask
        }
    }
}
