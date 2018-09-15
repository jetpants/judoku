package judoku

import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type
import com.google.gson.*

internal object GridSerializer {
	fun toString(g: Grid, highlightNthCell: Int?): String {
		val result = StringBuffer()
		var col = 1
		var row = 1

		val ansi = Util.isAnsiTerminal()

		for (y in 0..g.numBands * (g.boxHeight + 1)) {
			for (x in 0..g.numStacks * (g.boxWidth + 1)) {
				var s: String

				if (y == 0) {										// first horizontal line
					if (x == 0)
						s = if (ansi) "┌" else "+"
					else if (x == g.numStacks * (g.boxWidth + 1))
						s = if (ansi) "┐" else "+"
					else if (x % (g.boxWidth + 1) == 0)
						s = if (ansi) "┬" else "+"
					else
						s = if (ansi) "───" else "---"
				} else if (y == g.numBands * (g.boxHeight + 1)) {		// last horizontal line
					if (x == 0)
						s = if (ansi) "└" else "+"
					else if (x == g.numStacks * (g.boxWidth + 1))
						s = if (ansi) "┘" else "+"
					else if (x % (g.boxWidth + 1) == 0)
						s = if (ansi) "┴" else "+"
					else
						s = if (ansi) "───" else "---"
				} else if (y % (g.boxHeight + 1) == 0) {				// middle horizontal line
					if (x == 0)
						s = if (ansi) "├" else "+"
					else if (x == g.numStacks * (g.boxWidth + 1))
						s = if (ansi) "┤" else "+"
					else if (x % (g.boxWidth + 1) == 0)
						s = if (ansi) "┼" else "+"
					else
						s = if (ansi) "───" else "---"
				} else if (x % (g.boxWidth + 1) == 0)
					s = if (ansi) "│" else "|"
				else {
					check(row in 1..g.numRows)
					check(col in 1..g.numColumns)

					val cell = g.getCell(col, row)
					s = when {
						cell == Grid.EMPTY -> " "
						g.size > 9 && g.size <= 26 -> (('A'.toInt() - 1 + cell).toChar()).toString()
						else -> Integer.toString(cell)
					}

					var left = " "
					var right = " "
					if (g.toNth(col, row) == highlightNthCell) { left = "["; right = "]" }
					if (s.length < 3) s = s + right
					if (s.length < 3) s = left + s

					if (++col > g.numColumns) { col = 1; ++row }
				}

				result.append(s)
			}

			result.append('\n')
		}

		return result.toString()
	}

	@Throws(IOException::class)
	fun toCsv(g:Grid, dest: Appendable) {
		for (row in 1..g.numRows) {
			for (col in 1..g.numColumns) {
				if (col > 1) dest.append(',')
				val cell = g.getCell(col, row)
				dest.append(when {
					cell == Grid.EMPTY -> " "
					g.size > 9 && g.size <= 26 -> ('A'.toInt() - 1 + cell).toChar().toString()
					else -> Integer.toString(cell)
				})
			}
			dest.append('\n')
		}
	}

	fun toCsv(g: Grid): String {
		val buf = StringBuffer()
		try { toCsv(g, buf) } catch (e: Exception) {}
		return buf.toString()
	}

	@Throws(IOException::class)
	fun toJson(g: Grid, dest: Appendable): Unit = gsonInstance.toJson(g, dest)
	fun toJson(g: Grid): String = gsonInstance.toJson(g)

	fun newFromJson(from: Reader): Grid = gsonInstance.fromJson(from, Grid::class.java)

	private var _gson: Gson? = null					// lazy init
	private val gsonInstance: Gson
		get() {
			/*	!! is because compiler warns that _gson may have been set to null by
				another thread in between testing it and returning it, but the var
				never goes from non-null to null (only the other way around) */
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

					val cells = ByteArray(1 + size * size) {
						/*	cells[] has an extra unused element at the front. Indexing is
						  	by a 1-based index. Rather than do a subtraction each time
						  	cell values are indexed and read, for efficiency, the index is
							left unchanged. */
						i -> (if (i == 0) Grid.EMPTY else array.get(i - 1).getAsInt()).toByte()
					}

					val g = Grid(size, boxWidth, boxHeight, cells,
						Grid.DeserializedType.DESERIALIZED)
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
