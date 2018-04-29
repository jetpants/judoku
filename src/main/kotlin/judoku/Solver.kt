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

class Solver(private val puzzle: Grid) {
    init {
        require(puzzle.isLegal()) { "Grid has invalid state and no solutions" }
    }

    fun findSolutions(maxSolutions: Int): Array<Grid> {
        val solutions = ArrayList<Grid>(maxSolutions)
        val n = search(puzzle, solutions, maxSolutions)
        assert(n == solutions.size) { n }
        val results = arrayOfNulls<Grid>(n)
        return solutions.toArray(results)
    }

    fun countSolutions(maxSolutions: Int): Int = search(puzzle, null, maxSolutions)

    fun hasUniqueSolution(): Boolean  = countSolutions(2) == 1          // one and only one

    fun isMinimal(): Boolean {
        for (nth in 1..puzzle.numCells())
            if (!puzzle.isEmpty(nth) && countSolutions(puzzle.withCellEmpty(nth), 2) < 2)
                return false

        return true
    }

    fun isProper(): Boolean {
        /*  A so-called 'proper' puzzle has exactly one solution and no given is superfluous.
            I.e., removing any one of the givens would result in a puzzle with more than one
            solution. Proper puzzles are both sufficent and minimal. */
        return hasUniqueSolution() && isMinimal()
    }

    companion object {
        @JvmStatic
        fun findSolutions(puzzle: Grid, maxSolutions: Int): Array<Grid> =
            Solver(puzzle).findSolutions(maxSolutions)

        @JvmStatic
        fun countSolutions(puzzle: Grid, maxSolutions: Int): Int =
            Solver(puzzle).countSolutions(maxSolutions)

        @JvmStatic
        fun hasUniqueSolution(grid: Grid): Boolean = Solver(grid).hasUniqueSolution()

        @JvmStatic
        fun isMinimal(grid: Grid): Boolean = Solver(grid).isMinimal()

        @JvmStatic
        fun isProper(grid: Grid): Boolean = Solver(grid).isProper()

        private fun search(template: Grid, solutions: ArrayList<Grid>?, maxSolutions: Int): Int {
            assert(template.isLegal())

            // lots of strategies that could be added here: http://www.sudokudragon.com/sudokustrategy.htm
            return bruteForce(template, solutions, maxSolutions)
        }

        private fun bruteForce(template: Grid, solutions: ArrayList<Grid>?, max: Int): Int {
            if (max == 0) return 0

            var nth = -1

            for (i in 1..template.numCells())
                if (template.isEmpty(i)) {
                    nth = i
                    break
                }

            if (nth < 0) {
                // no more empty cells, a solution has been found
                assert(template.isLegal())
                if (solutions != null) solutions.add(template)
                return 1
            }

            val values = template.getCandidates(nth)
            Util.shuffle(values)

            var found = 0

            for (value in values) {
                val candidate = template.withCell(nth, value)
                found += bruteForce(candidate, solutions, max - found)
            }

            return found
        }
    }
}
