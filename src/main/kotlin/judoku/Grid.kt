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

import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonParseException

public class Grid {
	/*	terminology below adopted from 'List of Sudoku terms and jargon'
		http://en.wikipedia.org/wiki/List_of_Sudoku_terms_and_jargon
		More terminology here:
		http://sudopedia.enjoysudoku.com/Terminology.html */

	private val size: Int
	private val boxWidth: Int
	private val boxHeight: Int

	private val cells: IntArray

	constructor() : this(9)				// default is standard 9x9

	constructor(size: Int) {
		val root = Math.round(Math.sqrt(size.toDouble())).toInt()
		require(root * root == size) { "Size must be a square number: $size" }

		this.size = size
		boxWidth = root
		boxHeight = root

		cells = IntArray(size * size, { EMPTY } )
		assert(cells.size == numCells())
	}

	constructor(boxWidth: Int, boxHeight: Int) {
		require(boxWidth > 0) { "Width must be greater than 0: $boxWidth" }
		require(boxHeight > 0) { "Height must be greater than 0: $boxHeight" }

		size = boxWidth * boxHeight
		this.boxWidth = boxWidth
		this.boxHeight = boxHeight
		assert(boxWidth * numStacks() == size)
		assert(boxHeight * numBands() == size)

		cells = IntArray(size * size, { EMPTY } )
		assert(cells.size == numCells())
	}

	constructor(from: Grid) {
		size = from.size
		boxWidth = from.boxWidth
		boxHeight = from.boxHeight
		cells = from.cells.copyOf()
	}

	// dimensions
	fun size(): Int = size;
	fun boxWidth(): Int = boxWidth;
	fun boxHeight(): Int = boxHeight;
	fun numColumns(): Int = boxWidth * numStacks()
	fun numRows(): Int = boxHeight * numBands()
	fun numBoxes(): Int = numStacks() * numBands()
	fun numCells(): Int = numColumns() * numRows()
	fun numStacks(): Int = boxHeight			// width of grid in boxes
	fun numBands(): Int = boxWidth				// height of grid in boxes

	// indexing conversions
	fun toNth(col: Int, row: Int): Int = (row - 1) * numColumns() + col
	fun toColumn(nth: Int): Int = (nth - 1) % numColumns() + 1
	fun toRow(nth: Int): Int = (nth - 1) / numColumns() + 1
	fun toStack(col: Int): Int = (col - 1) / boxWidth + 1			// 1..nbStacks
	fun toBand(row: Int): Int = (row - 1) / boxHeight + 1 			// 1..nbBands
	fun toLeftColumn(stack: Int): Int = (stack - 1) * boxWidth + 1
	fun toRightColumn(stack: Int): Int = stack * boxWidth
	fun toTopRow(band: Int): Int = (band - 1) * boxHeight + 1
	fun toBottomRow(band: Int): Int = band * boxHeight

	fun numEmptyCells(): Int = numCells() - numFilledCells()
	fun numFilledCells(): Int = cells.count { it != EMPTY }

	fun getCell(nth: Int /*1..numCells()*/): Int = cells[nth - 1]
	fun getCell(col: Int, row: Int): Int {
		requireBoundsCheck(col, row)
		return cells[toNth(col, row) - 1]
	}

	fun isEmpty(nth: Int /*1..numCells()*/) = getCell(nth) == EMPTY
	fun isEmpty(col: Int, row: Int) = getCell(col, row) == EMPTY

	fun withCell(nth: Int /*1..numCells()*/, value: Int): Grid {
		require(value == EMPTY || (value in 1..size)) { "Cell value not in range 1-$size: $value" }
		val new = Grid(this)
		new.cells[nth - 1] = value
		return new
	}

	fun withCell(col: Int, row: Int, value: Int): Grid {
		requireBoundsCheck(col, row)
		require(value == EMPTY || (value in 1..size)) { "Cell value not in range 1-$size: $value" }
		val new = Grid(this)
		new.cells[toNth(col, row) - 1] = value
		return new
	}

	fun withCellEmpty(nth: Int /*1..numCells()*/) = withCell(nth, EMPTY)
	fun withCellEmpty(col: Int, row: Int) = withCell(col, row, EMPTY)

	private fun requireBoundsCheck(col: Int, row: Int) {
		require(col in 1..size) { "Column not in range 1-$size: $col" }
		require(row in 1..size) { "Row not in range 1-$size: $row" }
	}

	fun isLegal(): Boolean {
		/*	Check there are no illegal cell values (i.e., ones outside of the range 1..size).
		  	Internally, cell values are used as array indices so its vital that illegal values
		  	are rejected by setCell(). Also a good idea to confirm using this method  after a
		  	grid has been imported from JSON and could potentially contain any weird values. */

		cells.forEach { if (it != EMPTY && (it < 1 || it > size)) return false }
		return true
	}

	fun getCandidates(nth: Int /*1..numCells()*/): IntArray {
		return getCandidates(toColumn(nth), toRow(nth))
	}

	fun getCandidates(col: Int, row: Int): IntArray {
		requireBoundsCheck(col, row)

		val seen = BooleanArray(1 + size)		// EMPTY + 1..size

		// eliminate values from the same column
		for (r in 1..numRows())
			if (r != row) {
				val cell = getCell(col, r)
				if (cell in 1..size)
					seen[cell] = true
			}

		// eliminate values from the same row
		for (c in 1..numColumns())
			if (c != col) {
				val cell = getCell(c, row)
				if (cell in 1..size)
					seen[cell] = true
			}

		val stack = toStack(col)
		val band = toBand(row)

		// eliminate values from the same box
		for (c in toLeftColumn(stack)..toRightColumn(stack))
			for (r in toTopRow(band)..toBottomRow(band))
				if (c != col || r != row) {
					val cell = getCell(c, r)
					if (cell in 1..size)
						seen[cell] = true
				}

		// count how many values remain
		seen[EMPTY] = true
		var n = seen.count { it == false }
		val results = IntArray(n)

		var next = 0
		seen.forEachIndexed { index, bool -> if (!bool) results[next++] = index }
		return results
	}

	fun hasDuplicates(): Boolean {
		val seen = BooleanArray(1 + size)		// EMPTY + 1..size

		// check for duplicates in each column
		for (col in 1..numColumns()) {
			seen.fill(false)

			for (row in 1..numRows()) {
				val cell = getCell(col, row)
				if (cell in 1..size)
					if (seen[cell])
						return true
					else
						seen[cell] = true
			}
		}

		// check for duplicates in each row
		for (row in 1..numRows()) {
			seen.fill(false)

			for (col in 1..numColumns()) {
				val cell = getCell(col, row)
				if (cell in 1..size)
					if (seen[cell])
						return true
					else
						seen[cell] = true
			}
		}

		// check for duplicates in each box
		for (band in 1..numBands())
			for (stack in 1..numStacks()) {
				seen.fill(false)

				for (col in toLeftColumn(stack)..toRightColumn(stack))
					for (row in toTopRow(band)..toBottomRow(band)) {
						val cell = getCell(col, row)
						if (cell in 1..size)
							if (seen[cell])
								return true
							else
								seen[cell] = true
					}
			}

		return false
	}

	fun toCsv(): String {
		val buf = StringBuffer()
		try { toCsv(buf) } catch (e: Exception) {}
		return buf.toString()
	}

	@Throws(IOException::class)
	fun toCsv(dest: Appendable) {
		for (row in 1..numRows()) {
			for (col in 1..numColumns()) {
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
	fun toString(highlightNth: Int = -1): String {
		val result = StringBuffer()
		var col = 1
		var row = 1
		var nth = 1

		// ASCI 14 - turn on Unix terminal line graphics mode
		result.append(14.toChar())

		for (y in 0..numBands() * (boxHeight + 1)) {
			for (x in 0..numStacks() * (boxWidth + 1)) {
				var s: String

				if (y == 0) {										// first horizontal line
					if (x == 0)
						s = "┌"
					else if (x == numStacks() * (boxWidth + 1))
						s = "┐"
					else if (x % (boxWidth + 1) == 0)
						s = "┬"
					else
						s = "───"
				} else if (y == numBands() * (boxHeight + 1)) {		// last horizontal line
					if (x == 0)
						s = "└"
					else if (x == numStacks() * (boxWidth + 1))
						s = "┘"
					else if (x % (boxWidth + 1) == 0)
						s = "┴"
					else
						s = "───"
				} else if (y % (boxHeight + 1) == 0) {				// middle horizontal line
					if (x == 0)
						s = "├"
					else if (x == numStacks() * (boxWidth + 1))
						s = "┤"
					else if (x % (boxWidth + 1) == 0)
						s = "┼"
					else
						s = "───"
				} else if (x % (boxWidth + 1) == 0)
				// vertical line
					s = "|"
				else {
					assert(row <= numRows())
					assert(col <= numColumns())

					if (getCell(col, row) == EMPTY)
						s = "   "
					else {
						val cell = getCell(col, row)
						if (size > 9 && size <= 26)
							s = " " + ('A'.toInt() - 1 + cell).toChar() + " "
						else {
							s = Integer.toString(cell)
							var left = " "
							var right = " "
							if (nth++ == highlightNth) {
								left = "("
								right = ")"
							}
							if (s.length < 3) s = left + s
							if (s.length < 3) s = s + right
						}
					}

					if (++col > numColumns()) {
						col = 1
						++row
					}
				}

				result.append(s)
			}

			result.append('\n')
		}

		// ASCI 15 - turn off Unix terminal line graphics mode
		result.append(15.toChar())

		return result.toString()
	}

	@Throws(IOException::class)
	fun toJson(dest: Appendable): Unit = gsonInstance.toJson(this, dest)
	fun toJson(): String = gsonInstance.toJson(this)

	private enum class DeserializedType { DESERIALIZED }
	private constructor(size: Int, boxWidth: Int, boxHeight: Int, cells: IntArray,
			type: DeserializedType) {
		assert(type == DeserializedType.DESERIALIZED)
		this.size = size
		this.boxWidth = boxWidth
		this.boxHeight = boxHeight
		this.cells = cells
	}

	companion object {
		const val EMPTY = 0

		@JvmStatic
		@Throws(JsonParseException::class)
		fun newFromJson(from: Reader): Grid = gsonInstance.fromJson(from, Grid::class.java)

		private var _gson: Gson? = null					// lazy init
		private val gsonInstance: Gson
			get() {
				// !! is because compiler warns that _gson may have been set to null by
				// another thread in between testing it and returning it
				if (_gson != null) return _gson!!

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

						val cells = IntArray(size * size)
						for (i in cells.indices)
							cells[i] = array.get(i).getAsInt()

						val g = Grid(size, boxWidth, boxHeight, cells, DeserializedType.DESERIALIZED)
						if (!g.isLegal()) throw JsonParseException("Illegal (out of range) cell values")

						return g
					}
				}

				val builder = GsonBuilder()
				builder.registerTypeAdapter(Grid::class.java, deserializer)

				_gson = builder
					.setPrettyPrinting()
					.create()

				assert(_gson != null) { "GsonBuilder.create() returned null" }
				return _gson!!
			}
	}
}
