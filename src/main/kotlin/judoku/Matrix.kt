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

internal class Matrix(val puzzle: Grid) {
    private val posmap = Array<MutableSet<Int>>(puzzle.numCells() + 1, { _ -> mutableSetOf<Int>() })

    init {
        check(puzzle.isLegal())
        check(puzzle.isViable()) { "Grid has no solution" }

        for (i in 1..puzzle.numCells())
            puzzle.getPossibilities(i).toCollection(posmap[i])
    }

    fun suggestMove(): Move? {
        val rules: Array<Matrix.() -> Move?> = arrayOf(
            Matrix::ONE_MISSING,        // special case of ONE_POSSIBILITY and should precede it
            Matrix::ONE_POSSIBILITY
        )

        for (rule in rules) {
            val move = this.rule()
            if (move != null) return move
        }

        return null
    }

    // lots of strategies that could be added here: http://www.sudokudragon.com/sudokustrategy.htm

    private fun ONE_MISSING(): Move? {
        return null
    }

    private fun ONE_POSSIBILITY(): Move? {
        return null
    }
}
