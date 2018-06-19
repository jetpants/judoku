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
        check(puzzle.isLegal())
    }

    fun findSolutions(maxSolutions: Int): Array<Grid> {
        val solutions = ArrayList<Grid>(maxSolutions)
        val n = traverse(puzzle, solutions, maxSolutions)
        assert(n == solutions.size) { n }
        val results = arrayOfNulls<Grid>(n)
        return solutions.toArray(results)
    }

    fun countSolutions(maxSolutions: Int): Int = traverse(puzzle, null, maxSolutions)

    fun suggestMove(): Move? {
        /*  If the puzzle has duplicates or unusable cells, then no sequence of moves will
            yield a solution. */
        if (!puzzle.isViable()) return null;

        check(puzzle.isLegal());
        check(puzzle.isViable());

        val matrix = Matrix(puzzle)
        return matrix.suggestMove()
    }

    fun hasUniqueSolution(): Boolean  = countSolutions(2) == 1      // one and only one

    fun isMinimal(): Boolean {
        if (!puzzle.isViable()) return false;

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
        fun suggestMove(puzzle: Grid): Move? = Solver(puzzle).suggestMove()

        @JvmStatic
        fun hasUniqueSolution(grid: Grid): Boolean = Solver(grid).hasUniqueSolution()

        @JvmStatic
        fun isMinimal(grid: Grid): Boolean = Solver(grid).isMinimal()

        @JvmStatic
        fun isProper(grid: Grid): Boolean = Solver(grid).isProper()

        private fun traverse(template: Grid, solutions: ArrayList<Grid>?, maxSolutions: Int): Int {
            if (!template.isViable()) return 0;           // there are no solutions
            return _traverse(template, solutions, maxSolutions)
        }

        private fun _traverse(template: Grid, solutions: ArrayList<Grid>?, max: Int): Int {
            if (max == 0) return 0

            var nth = nextCell(template)

            if (nth == null) {
                // no more empty cells, a solution has been found
                check(template.isLegal())
                if (solutions != null) solutions.add(template)
                return 1
            }

            val possibilities = template.getPossibilities(nth)
            Util.shuffle(possibilities)

            var found = 0

            for (possibility in possibilities) {
                val next = template.withCell(nth, possibility)
                found += _traverse(next, solutions, max - found)
            }

            return found
        }

        private fun nextCell(g: Grid): Int? {
            /*  The search performs a depth-first traversal of the tree of all possible grid
                cell-value combinations. From a given board there is a tree of grids that
                descends from this node that terminates in leaf nodes that are either complete
                solutions or in nodes where the grids are incomplete but have no further
                possible moves. This method determines the order in which that sub-tree is
                searched.

                If there are two empty cells and one could have any of six possible values and
                another could only have two, then it makes sense to search the sub-tree
                descending from the latter cell first because, a priori, if there is a solution
                then there's a one-in-two chance of finding it here, as opposed to a one-in-six
                chance of finding it in the sub-tree off the other empty cell.

                This method finds the empty cell that has the fewest possible values. This
                method used to search simply for the first empty cell. When I changed it to look
                for the empty cell with the fewest possibilities, the time to find a grid's
                solution dropped to a third! Thanks to Norvig for the optimisation!
                http://norvig.com/sudoku.html */

            var best : Int? = null
            var count = Int.MAX_VALUE

            for (nth in 1..g.numCells())
                if (g.isEmpty(nth)) {
                    val c = g.getPossibilities(nth).size
                    if (c < count) {
                        best = nth
                        count = c
                    }
                }

            return best
        }
    }
}
