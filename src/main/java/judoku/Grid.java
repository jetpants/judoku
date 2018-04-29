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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Arrays;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class Grid {
	public Grid() {
		this(3, 3);		// default is standard 9x9 grid with boxes of 3x3
	}

	public Grid(int size) {
		int root = (int) Math.round(Math.sqrt((double) size));
		if (root * root != size)
			throw new IllegalArgumentException("size=" + size);

		this.size = size;
		boxWidth = boxHeight = root;
		assert boxWidth * numStacks() == size;
		assert boxHeight * numBands() == size;

		cells = new int[boxWidth * boxHeight * numBoxes()];
		Arrays.fill(cells, EMPTY);
	}

	public Grid(int boxWidth, int boxHeight) {
		if (boxWidth < 1 || boxHeight < 1)
			throw new IllegalArgumentException("boxes=" + boxWidth + "x" + boxHeight);

		size = boxWidth * boxHeight;
		this.boxWidth = boxWidth;
		this.boxHeight = boxHeight;
		assert boxWidth * numStacks() == size;
		assert boxHeight * numBands() == size;

		cells = new int[boxWidth * boxHeight * numBoxes()];
		Arrays.fill(cells, EMPTY);
	}

	public Grid(Grid from) {
		size = from.size;
		boxWidth = from.boxWidth;
		boxHeight = from.boxHeight;
		cells = from.cells.clone();
	}

	// dimensions
	public int size() { return size; }					// cell values may be 1..size
	public int numColumns() { return boxWidth * numStacks(); }
	public int numRows() { return boxHeight * numBands(); }
	public int numCells() { return numColumns() * numRows(); }
	public int boxWidth() { return boxWidth; }			// width of boxes in cells
	public int boxHeight() { return boxHeight; }		// height of boxes in cells
	public int numBoxes() { return numStacks() * numBands(); }
	public int numStacks() { return boxHeight; }		// width of grid in boxes
	public int numBands() { return boxWidth; }			// height of grid in boxes

	// indexing conversions
	public int toNth(int col, int row) { return (row - 1) * numColumns() + col; }
	public int toColumn(int nth) { return (nth - 1) % numColumns() + 1; }
	public int toRow(int nth) { return (nth - 1) / numColumns() + 1; }
	public int toStack(int col) { return (col - 1) / boxWidth + 1; }		// 1..nbStacks
	public int toBand(int row) { return (row - 1) / boxHeight + 1; } 		// 1..nbBands
	public int toLeftColumn(int stack) { return (stack - 1) * boxWidth + 1; }
	public int toRightColumn(int stack) { return stack * boxWidth; }
	public int toTopRow(int band) { return (band - 1) * boxHeight + 1; }
	public int toBottomRow(int band) { return band * boxHeight; }

	public int numEmptyCells() { return numCells() - numFilledCells(); }
	public int numFilledCells() {
		int n = 0;
		for (int i = 0; i < cells.length; ++i)
			if (cells[i] != EMPTY)
				++n;
		return n;
	}

	public int getCell(int nth /*1..numCells()*/) { return cells[nth - 1]; }
	public int getCell(int col, int row) {
		checkBounds(col, row);
		return cells[toNth(col, row) - 1];
	}

	public boolean isEmpty(int nth /*1..numCells()*/) { return getCell(nth) == EMPTY; }
	public boolean isEmpty(int col, int row) { return getCell(col, row) == EMPTY; }

	public Grid withCell(int nth /*1..numCells()*/, int value) {
		if (value != EMPTY && (value < 1 || value > size))
			throw new IllegalArgumentException("Illegal cell value: " + value);

		Grid derived = new Grid(this);
		derived.cells[nth - 1] = value;
		return derived;
	}

	public Grid withCell(int col, int row, int value) {
		checkBounds(col, row);
		return withCell(toNth(col, row), value);
	}

	public Grid withCellEmpty(int nth /*1..numCells()*/) { return withCell(nth, EMPTY); }
	public Grid withCellEmpty(int col, int row) { return withCell(col, row, EMPTY); }

	public int[] getCandidates(int nth /*1..numCells()*/) {
		return getCandidates(toColumn(nth), toRow(nth));
	}

	public int[] getCandidates(int col, int row) {
		boolean seen[] = new boolean[size + 1];		// EMPTY + 1..size

		// eliminate values from the same column
		for (int r = 1; r <= numRows(); ++r)
			if (r != row) {
				int cell = getCell(col, r);
				if (cell >= 1 && cell <= size)
					seen[cell] = true;
			}

		// eliminate values from the same row
		for (int c = 1; c <= numColumns(); ++c)
			if (c != col) {
				int cell = getCell(c, row);
				if (cell >= 1 && cell <= size)
					seen[cell] = true;
			}

		int stack = toStack(col);
		int band = toBand(row);

		// eliminate values from the same box
		for (int c = toLeftColumn(stack); c <= toRightColumn(stack); ++c)
			for (int r = toTopRow(band); r <= toBottomRow(band); ++r)
				if (c != col || r != row) {
					int cell = getCell(c, r);
					if (cell >= 1 && cell <= size)
						seen[cell] = true;
				}

		// count how many values remain
		int n = 0;
		for (int i = 1; i <= size; ++i)
			if (!seen[i])
				++n;
		int[] results = new int[n];

		int next = 0;
		for (int i = 1; i <= size; ++i)
			if (!seen[i])
				results[next++] = i;

		return results;
	}

	public boolean isLegal() {
		/*	Check there are no illegal values (i.e., ones outside of the range 1..size).
			Internally, cell values are used as array indices so this function might be
			called prior to those methods. Also a good idea to confirm with this method after
			a grid has been imported from JSON and could potentially contain any weird values. */

		for (int i = 0; i < cells.length; ++i)
			if (cells[i] != EMPTY && (cells[i] < 1 || cells[i] > size))
				return false;

		return true;
	}

	public boolean hasDuplicates() {
		boolean[] seen = new boolean[size + 1];		// 1..size

		// check for duplicates in each column
		for (int col = 1; col <= numColumns(); ++col) {
			Arrays.fill(seen, false);

			for (int row = 1; row <= numRows(); ++row) {
				int cell = getCell(col, row);
				if (cell >= 1 && cell <= size)
					if (seen[cell])
						return true;
					else
						seen[cell] = true;
			}
		}

		// check for duplicates in each row
		for (int row = 1; row <= numRows(); ++row) {
			Arrays.fill(seen, false);

			for (int col = 1; col <= numColumns(); ++col) {
				int cell = getCell(col, row);
				if (cell >= 1 && cell <= size)
					if (seen[cell])
						return true;
					else
						seen[cell] = true;
			}
		}

		// check for duplicates in each box
		for (int band = 1; band <= numBands(); ++band)
			for (int stack = 1; stack <= numStacks(); ++stack) {
				Arrays.fill(seen, false);

				for (int col = toLeftColumn(stack); col <= toRightColumn(stack); ++col)
					for (int row = toTopRow(band); row <= toBottomRow(band); ++row) {
						int cell = getCell(col, row);
						if (cell >= 1 && cell <= size)
							if (seen[cell])
								return true;
							else
								seen[cell] = true;
					}
			}

		return false;
	}

	public String toCsv() {
		StringBuffer buf = new StringBuffer();
		try {
			toCsv(buf);
		} catch (Exception e) {}
		return buf.toString();
	}

	public void toCsv(Appendable dest) throws IOException {
		for (int row = 1; row <= numRows(); ++row) {
			for (int col = 1; col <= numColumns(); ++col) {
				if (col > 1) dest.append(',');
				int val = getCell(col, row);
				if (val != EMPTY)
					if (size > 9 && size <= 26)
						dest.append((char) ('A' - 1 + val));
					else
						dest.append(Integer.toString(val));
			}
			dest.append('\n');
		}
	}

	public static Grid newFromJson(Reader from) throws JsonParseException {
		return getGsonInstance().fromJson(from, Grid.class);
	}

	public String toJson() { return getGsonInstance().toJson(this); }

	public void toJson(Appendable dest) throws IOException {
		getGsonInstance().toJson(this, dest);
	}

	public String toString() { return toString(-1); }

	public String toString(int highlightNth) {
		StringBuffer result = new StringBuffer();
		int col = 1, row = 1, nth = 1;

		// ASCI 14 - turn on Unix terminal line graphics mode
		result.append((char) 14);

		for (int y = 0; y < numBands() * (boxHeight + 1) + 1; ++y) {
			for (int x = 0; x < numStacks() * (boxWidth + 1) + 1; ++x) {
				String s;

				if (y == 0) {										// first horizontal line
					if (x == 0)
						s = "┌";
					else if (x == numStacks() * (boxWidth + 1))
						s = "┐";
					else if (x % (boxWidth + 1) == 0)
						s = "┬";
					else
						s = "───";
				} else if (y == numBands() * (boxHeight + 1)) {		// last horizontal line
					if (x == 0)
						s = "└";
					else if (x == numStacks() * (boxWidth + 1))
						s = "┘";
					else if (x % (boxWidth + 1) == 0)
						s = "┴";
					else
						s = "───";
				} else if (y % (boxHeight + 1) == 0) {				// middle horizontal line
					if (x == 0)
						s = "├";
					else if (x == numStacks() * (boxWidth + 1))
						s = "┤";
					else if (x % (boxWidth + 1) == 0)
						s = "┼";
					else
						s = "───";
				} else if (x % (boxWidth + 1) == 0)					// vertical line
					s = "|";
				else {
					assert row <= numRows() : row;
					assert col <= numColumns() : col;

					if (getCell(col, row) == EMPTY)
						s = "   ";
					else {
						int val = getCell(col, row);
						if (size > 9 && size <= 26)
							s = " " + (char) ('A' - 1 + val) + " ";
						else {
							s = Integer.toString(val);
							String left = " ", right = " ";
							if (nth++ == highlightNth) { left = "("; right = ")"; }
							if (s.length() < 3) s = left + s;
							if (s.length() < 3) s = s + right;
						}
					}

					if (++col > numColumns()) {
						col = 1;
						++row;
					}
				}

				result.append(s);
			}

			result.append('\n');
		}

		// ASCI 15 - turn off Unix terminal line graphics mode
		result.append((char) 15);

		return result.toString();
	}

	private void checkBounds(int col, int row) {
		if (col < 1)
			throw new IllegalArgumentException("Column " + col + " less than 1");
		if (col > numColumns())
			throw new IllegalArgumentException("Column " + col + " greater than number of columns " + numColumns());

		if (row < 1)
			throw new IllegalArgumentException("Row " + row + " less than 1");
		if (row > numRows())
			throw new IllegalArgumentException("Row " + row + " greater than number of rows " + numRows());
	}

	private static Gson getGsonInstance() {
		if (_gson != null) return _gson;

		JsonDeserializer<Grid> deserializer = new JsonDeserializer<Grid>() {
    		@Override
    		public Grid deserialize(JsonElement json, Type typeOfT,
					JsonDeserializationContext context) throws JsonParseException {
        		JsonObject jsonObject = json.getAsJsonObject();

        		int size = jsonObject.get("size").getAsInt();
        		int boxWidth = jsonObject.get("boxWidth").getAsInt();
        		int boxHeight = jsonObject.get("boxHeight").getAsInt();
				if (size != boxWidth * boxHeight)
					throw new JsonParseException("Mismatching grid size and box size");

				JsonArray array = jsonObject.get("cells").getAsJsonArray();
				if (array.size() != size * size)
					throw new JsonParseException("Too few/many elements for \"cells\"");

				int[] cells = new int[size * size];
				for (int i = 0; i < cells.length; ++i)
					cells[i] = array.get(i).getAsInt();

				Grid g = new Grid(size, boxWidth, boxHeight, cells, DeserializedType.DESERIALIZED);
				if (!g.isLegal()) throw new JsonParseException("Illegal (out of bounds) cell values");

				return g;
			}
		};

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Grid.class, deserializer);

		return _gson = gsonBuilder
				.setPrettyPrinting()
				.create();
	}

	private enum DeserializedType { DESERIALIZED };
	private Grid(int size, int boxWidth, int boxHeight, int[] cells, DeserializedType type) {
		assert type == DeserializedType.DESERIALIZED;
		this.size = size;
		this.boxWidth = boxWidth;
		this.boxHeight = boxHeight;
		this.cells = cells;
	}

	/*	terminology below adopted from 'List of Sudoku terms and jargon'
		http://en.wikipedia.org/wiki/List_of_Sudoku_terms_and_jargon
		More terminology here:
		http://sudopedia.enjoysudoku.com/Terminology.html */

  	private final int size;						// number of cells in each group
  	private final int boxWidth, boxHeight;		// box dimensions (usually 3x3)
  	private final int[] cells;					// row-major order

	public static final int EMPTY = 0;					// value for empty cell

	private static Gson _gson = null;					// lazy init
}
