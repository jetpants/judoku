package judoku

import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type
import com.google.gson.annotations.Expose
import com.google.gson.*

/*	glossaries of Sudoku terms and jargon here:
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
		require(value == EMPTY || (value in 1..size)) { "Value out of range 1-$size: $value" }
		val new = Grid(this)
		new._setCell(n, value)
		return new
	}

	fun withCell(col: Int, row: Int, value: Int): Grid {
		requireInBounds(col, row)
		return withCell(toNth(col, row), value)
	}

	// internal use only - no bounds checking
	internal fun _setCell(n: Int, value: Int) { cells[n] = value.toByte() }
	internal fun _withCell(n: Int, value: Int): Grid {
		val new = Grid(this)
		new._setCell(n, value)
		return new
	}

	fun withEmpty(n: Int) = withCell(n, EMPTY)
	fun withEmpty(col: Int, row: Int) = withCell(col, row, EMPTY)

	private fun requireInBounds(col: Int, row: Int) {
		require(col in 1..numColumns) { "Column out of range 1-${numColumns}: $col" }
		require(row in 1..numRows) { "Row out of range 1-${numRows}: $row" }
	}

	fun getOptions(n: Int) = getOptions(toColumn(n), toRow(n))

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

	fun toCsv(): String {
		val buf = StringBuffer()
		try { toCsv(buf) } catch (e: Exception) {}
		return buf.toString()
	}

	@Throws(IOException::class)
	fun toCsv(dest: Appendable) {
		for (row in 1..numRows) {
			for (col in 1..numColumns) {
				if (col > 1) dest.append(',')
				val cell = getCell(col, row)
				if (cell != EMPTY)
					if (size > 9 && size <= 26)
						dest.append(('A'.toInt() - 1 + cell).toChar())
					else
						dest.append(Integer.toString(cell))
			}
			dest.append('\n')
		}
	}

	@JvmOverloads
	fun toString(highlightNthCell: Int? = null): String {
		val result = StringBuffer()
		var col = 1
		var row = 1

		val boxesOn = if (Util.isAnsiTerminal()) Util.ANSI_LINE_DRAWING_ON else ""
		val boxesOff = if (Util.isAnsiTerminal()) Util.ANSI_LINE_DRAWING_OFF else ""
		val faint = if (Util.isAnsiTerminal()) Util.ANSI_FAINT else ""
		val reset = if (Util.isAnsiTerminal()) Util.ANSI_NORMAL else ""
		result.append(boxesOn)

		for (y in 0..numBands * (boxHeight + 1)) {
			for (x in 0..numStacks * (boxWidth + 1)) {
				var s: String

				if (y == 0) {										// first horizontal line
					if (x == 0)
						s = faint + "┌"
					else if (x == numStacks * (boxWidth + 1))
						s = "┐" + reset
					else if (x % (boxWidth + 1) == 0)
						s = "┬"
					else
						s = "───"
				} else if (y == numBands * (boxHeight + 1)) {		// last horizontal line
					if (x == 0)
						s = faint + "└"
					else if (x == numStacks * (boxWidth + 1))
						s = "┘" + reset
					else if (x % (boxWidth + 1) == 0)
						s = "┴"
					else
						s = "───"
				} else if (y % (boxHeight + 1) == 0) {				// middle horizontal line
					if (x == 0)
						s = faint + "├"
					else if (x == numStacks * (boxWidth + 1))
						s = "┤" + reset
					else if (x % (boxWidth + 1) == 0)
						s = "┼"
					else
						s = "───"
				} else if (x % (boxWidth + 1) == 0)
					s = faint + "|" + reset
				else {
					check(row <= numRows)
					check(col <= numColumns)

					val cell = getCell(col, row)
					s = when {
						cell == EMPTY -> " "
						size > 9 && size <= 26 -> (('A'.toInt() - 1 + cell).toChar()).toString()
						else -> Integer.toString(cell)
					}

					var left = " "
					var right = " "
					if (toNth(col, row) == highlightNthCell) { left = "["; right = "]" }
					if (s.length < 3) s = s + right
					if (s.length < 3) s = left + s

					if (++col > numColumns) { col = 1; ++row }
				}

				result.append(s)
			}

			result.append('\n')
		}

		result.append(boxesOff)

		return result.toString()
	}

	@Throws(IOException::class)
	fun toJson(dest: Appendable): Unit = gsonInstance.toJson(this, dest)
	fun toJson(): String = gsonInstance.toJson(this)

	private enum class DeserializedType { DESERIALIZED }
	private constructor(size: Int, boxWidth: Int, boxHeight: Int, cells: ByteArray,
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
		fun newFromJson(from: Reader): Grid = gsonInstance.fromJson(from, Grid::class.java)

		private var _gson: Gson? = null					// lazy init
		private val gsonInstance: Gson
			get() {
				// !! is because compiler warns that _gson may have been set to null by
				// another thread in between testing it and returning it
				if (_gson != null) return _gson!!

				val serializer = object : JsonSerializer<Grid> {
					override fun serialize(g: Grid, typeOfT: Type,
							context: JsonSerializationContext): JsonObject {
						val obj = JsonObject()
						obj.addProperty("size", g.size)
						obj.addProperty("boxWidth", g.boxWidth)
						obj.addProperty("boxHeight", g.boxHeight)

						val cells = JsonArray()
						for (n in 1..g.numCells) cells.add(JsonPrimitive(g.cells[n]))
				        obj.add("cells", cells)

						return obj;
					}
				}

				val deserializer = object : JsonDeserializer<Grid> {
					@Throws(JsonParseException::class)
					override fun deserialize(json: JsonElement, typeOfT: Type,
							context: JsonDeserializationContext): Grid {
						val jsonObject = json.getAsJsonObject()

						val size = jsonObject.get("size").getAsInt()
						val boxWidth = jsonObject.get("boxWidth").getAsInt()
						val boxHeight = jsonObject.get("boxHeight").getAsInt()
						if (size != boxWidth * boxHeight)
							throw JsonParseException("Mismatching grid size and box size")

						val array = jsonObject.get("cells").getAsJsonArray()
						if (array.size() != size * size)
							throw JsonParseException("Too few or too many cell values")

						val cells = ByteArray(1 + size * size, {
							/*	cells[] has an extra unused element at the front. Indexing is
							  	by a 1-based index. Rather than do a subtraction each time
							  	cell values are read, for efficiency, the index is left
							  	unchanged. */
							i -> (if (i == 0) EMPTY else array.get(i - 1).getAsInt()).toByte()
						})

						val g = Grid(size, boxWidth, boxHeight, cells, DeserializedType.DESERIALIZED)
						if (!g.isLegal()) throw JsonParseException("Illegal cell values")

						return g
					}
				}

				_gson = GsonBuilder()
					.registerTypeAdapter(Grid::class.java, serializer)
					.registerTypeAdapter(Grid::class.java, deserializer)
					.setPrettyPrinting()
					.create()

				check(_gson != null) { "GsonBuilder.create() returned null" }
				return _gson!!
			}
	}
}
