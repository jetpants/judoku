package judoku

import java.io.IOException
import java.io.Reader
import com.google.gson.JsonParseException

/*	NOTE - glossaries of Sudoku terms and jargon here:
	http://en.wikipedia.org/wiki/List_of_Sudoku_terms_and_jargon
	http://sudopedia.enjoysudoku.com/Terminology.html */

class Grid {
	constructor() : this(DEFAULT_SIZE)		// default is standard 9 x 9

	constructor(size: Int) {
		// Int bit-masks are used to allow rapid calculation of cell options. This number
		// could be increased to Long.SIZE by changing the masks to be Longs as well
		require(size < Integer.SIZE)

		val root = Math.round(Math.sqrt(size.toDouble())).toInt()
		require(root * root == size) { "Size must be a square number: $size" }

		this.size = size
		boxWidth = root
		boxHeight = root
		cells = ByteArray(numCells + 1)
		MEGA_MASK = _MEGA_MASK()

		assert(numColumns == boxWidth * numStacks)
		assert(numRows == boxHeight * numBands)
		assert(numBoxes == numStacks * numBands)
	}

	constructor(boxWidth: Int, boxHeight: Int) {
		require(boxWidth * boxHeight < Integer.SIZE)
		require(boxWidth > 0) { "Width must be greater than 0: $boxWidth" }
		require(boxHeight > 0) { "Height must be greater than 0: $boxHeight" }

		size = boxWidth * boxHeight
		this.boxWidth = boxWidth
		this.boxHeight = boxHeight
		cells = ByteArray(numCells + 1)
		MEGA_MASK = _MEGA_MASK()

		assert(numColumns == boxWidth * numStacks)
		assert(numRows == boxHeight * numBands)
		assert(numBoxes == numStacks * numBands)
	}

	constructor(from: Grid) {
		size = from.size
		boxWidth = from.boxWidth
		boxHeight = from.boxHeight
		cells = from.cells.copyOf()
		MEGA_MASK = from.MEGA_MASK
	}

	val size: Int
	val boxWidth: Int
	val boxHeight: Int
	val cells: ByteArray

	// dimensions
	val numColumns get() = size
	val numRows get() = size
	val numBoxes get() = size
	val numCells get() = size * size
	val numStacks get() = boxHeight			// width of grid in boxes
	val numBands get() = boxWidth			// height of grid in boxes
	val numEmptyCells get() = numCells - numFilledCells
	val numFilledCells get() = cells.count { it != EMPTY.toByte() }

	internal val MEGA_MASK: Int		// calculated at construction for efficiency
	// can't be called until after size has been initialised
	private fun _MEGA_MASK() = ((0xffffffff.toInt() shl (size + 1)) or 1).inv()

	/*	coordinate & indexing conversions. Columns, rows, stacks, bands and
		cell numbers (n) are all 1-based like the cell values themselves. */
	fun toNth(col: Int, row: Int) = (row - 1) * numColumns + col
	fun toColumn(n: Int) = (n - 1) % numColumns + 1
	fun toRow(n: Int) = (n - 1) / numColumns + 1
	fun toStackFromColumn(col: Int) = (col - 1) / boxWidth + 1
	fun toBandFromRow(row: Int) = (row - 1) / boxHeight + 1
	fun toLeftColumnFromStack(stack: Int) = (stack - 1) * boxWidth + 1
	fun toRightColumnFromStack(stack: Int) = stack * boxWidth
	fun toTopRowFromBand(band: Int) = (band - 1) * boxHeight + 1
	fun toBottomRowFromBand(band: Int) = band * boxHeight

	// box numbers are row-major order (like a telephone keypad)
    fun toBandFromBox(box: Int) = (box - 1) / numStacks + 1
    fun toStackFromBox(box: Int) = (box - 1) % numStacks + 1
    fun toBoxFromStackBand(stack: Int, band: Int) = (band - 1) * numStacks + stack

	fun getCell(n: Int) = cells[n].toInt()
	fun getCell(col: Int, row: Int): Int {
		requireInBounds(col, row)
		return getCell(toNth(col, row))
	}

	fun isEmpty(n: Int) = getCell(n) == EMPTY
	fun isEmpty(col: Int, row: Int) = getCell(col, row) == EMPTY
	fun isFilled(n: Int) = !isEmpty(n)
	fun isFilled(col: Int, row: Int) = !isEmpty(col, row)

	fun withCell(n: Int, value: Int): Grid {
		require(n in 1..numCells)
		require(value == EMPTY || (value in 1..size)) { "Value out of range 1-$size: $value" }
		return _withCell(n, value)
	}

	fun withCell(col: Int, row: Int, value: Int): Grid {
		requireInBounds(col, row)
		require(value == EMPTY || (value in 1..size)) { "Value out of range 1-$size: $value" }
		return _withCell(toNth(col, row), value)
	}

	fun withEmpty(n: Int) = withCell(n, EMPTY)
	fun withEmpty(col: Int, row: Int) = withCell(col, row, EMPTY)

	// internal use only - no bounds checking
	internal fun _setCell(n: Int, value: Int) { cells[n] = value.toByte() }
	internal fun _withCell(n: Int, value: Int): Grid {
		val new = Grid(this)
		new._setCell(n, value)
		return new
	}

	private fun requireInBounds(col: Int, row: Int) {
		require(col in 1..numColumns) { "Column out of range 1-${numColumns}: $col" }
		require(row in 1..numRows) { "Row out of range 1-${numRows}: $row" }
	}

	fun getOptions(n: Int) = _getOptions(toColumn(n), toRow(n))

	fun getOptions(col: Int, row: Int): IntArray {
		requireInBounds(col, row)
		return _getOptions(col, row)
	}

	internal fun _getOptions(col: Int, row: Int): IntArray {
		/*	WARNING  This is a highly optimised function; be very careful about doing
		 	anything that would slow it down. */

		var mask = _getOptionsMask(col, row)
		val options = IntArray(Integer.bitCount(mask))

		var value = 0
		var i = 0

        while (mask != 0) {
            val zeroes = Integer.numberOfTrailingZeros(mask)
            value += zeroes; mask = mask ushr zeroes
			options[i++] = value
			++value; mask = mask ushr 1
        }

		return options
	}

	internal fun _getOptionsMask(col: Int, row: Int): Int {
		/*	WARNING  This is a highly optimised function; be very careful about doing
		 	anything that would slow it down. */

		var mask = 0x0

		for (n in 1..size) {
			if (n != row) mask = mask or (1 shl getCell(toNth(col, n)))
			if (n != col) mask = mask or (1 shl getCell(toNth(n, row)))
		}

		val stack = toStackFromColumn(col)
		val band = toBandFromRow(row)

		for (c in toLeftColumnFromStack(stack)..toRightColumnFromStack(stack))
			for (r in toTopRowFromBand(band)..toBottomRowFromBand(band))
				if (c != col && r != row)		// && [sic]
					mask = mask or (1 shl getCell(toNth(c, r)))

		/*	MEGA_MASK is a mask that represents all values excluding EMPTY; for
			a 9 x 9 grid, this will be 0b00000000_00000000_00000011_11111110 */
		return mask.inv() and MEGA_MASK
	}

	fun isViable(): Boolean {
		/*	If the grid has any duplicate values in any group or if there are cells that have
		 	no possible allowed values then an incorrect move has been previously made and the
			grid has no solution.

			WARNING - this is an expensive function. Don't call it in any tight loops. */

		return !hasDuplicates() && !hasZombies()
	}

	fun hasDuplicates(): Boolean {
		/*	Checks whether there are any groups (columns, rows, boxes) that have more than one
			of any value. */

		val seen = BooleanArray(1 + size)		// EMPTY + 1..size

		/*	getCell(toNth(col, row)) below is quicker than getCell(col, row) because it
			doesn't have the bounds check on the col & row */

		// check for duplicates in each column
		for (col in 1..numColumns) {
			seen.fill(false)

			for (row in 1..numRows) {
				val cell = getCell(toNth(col, row))
				if (cell in 1..size)
					if (seen[cell])
						return true
					else
						seen[cell] = true
			}
		}

		// check for duplicates in each row
		for (row in 1..numRows) {
			seen.fill(false)

			for (col in 1..numColumns) {
				val cell = getCell(toNth(col, row))
				if (cell in 1..size)
					if (seen[cell])
						return true
					else
						seen[cell] = true
			}
		}

		// check for duplicates in each box
		for (band in 1..numBands)
			for (stack in 1..numStacks) {
				seen.fill(false)

				for (col in toLeftColumnFromStack(stack)..toRightColumnFromStack(stack))
					for (row in toTopRowFromBand(band)..toBottomRowFromBand(band)) {
						val cell = getCell(toNth(col, row))
						if (cell in 1..size)
							if (seen[cell])
								return true
							else
								seen[cell] = true
					}
			}

		return false
	}

	fun hasZombies(): Boolean {
		/*	Checks whether there are any cells that have no possible allowed values; i.e., if
		 	there's a cell for whom all values have been eliminated as possibilities by the
			other values in its column, row or box. */

		for (n in 1..numCells)
			if (getOptions(n).size == 0)
				return true;

		return false
	}

	internal fun isLegal(): Boolean {
		/*	Should always return true; it shouldn't be possible through the public interface to
			make a grid with illegal values in it. This method confirms that there are no illegal
			cell values (i.e., ones outside of the range 1..size). Internally, cell values are
			used as array indices so it's vital that illegal values are rejected when setting
			cell values and when grids are imported from JSON. */

		for (n in 1..numCells) {
			val cell = getCell(n)
			if (cell != EMPTY && (cell < 1 || cell > size))
				return false
		}

		return true
	}

	override fun toString(): String = GridSerializer.toString(this, null)
	fun toString(highlightNthCell: Int): String = GridSerializer.toString(this, highlightNthCell)

	@Throws(IOException::class)
	fun toCsv(dest: Appendable) = GridSerializer.toCsv(this, dest)
	fun toCsv(): String = GridSerializer.toCsv(this)

	@Throws(IOException::class)
	fun toJson(dest: Appendable): Unit = GridSerializer.toJson(this, dest)
	fun toJson(): String = GridSerializer.toJson(this)

	internal enum class DeserializedType { DESERIALIZED }
	internal constructor(size: Int, boxWidth: Int, boxHeight: Int, cells: ByteArray,
			type: DeserializedType) {
		check(type == DeserializedType.DESERIALIZED)
		this.size = size
		this.boxWidth = boxWidth
		this.boxHeight = boxHeight
		this.cells = cells
		this.MEGA_MASK = this._MEGA_MASK()
	}

	companion object {
		const val EMPTY = 0
		const val DEFAULT_SIZE = 9

		@JvmStatic
		@Throws(JsonParseException::class)
		fun newFromJson(from: Reader): Grid = GridSerializer.newFromJson(from)
	}
}
