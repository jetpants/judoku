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

import java.util.AbstractList;
import java.util.ArrayList;

public class Solver {
    public Solver(Grid puzzle) {
        if (!puzzle.isLegal())
            throw new IllegalArgumentException("Grid has invalid state and no solutions");

        this.puzzle = new Grid(puzzle);
    }

    public Grid[] findSolutions(int maxSolutions) {
		AbstractList<Grid> solutions = new ArrayList<Grid>(maxSolutions);
		int n = search(new Grid(puzzle), solutions, maxSolutions);
		assert n == solutions.size() : n;
		Grid[] results = new Grid[n];
		return solutions.toArray(results);
	}

    public int countSolutions(int maxSolutions) {
		return search(new Grid(puzzle), null, maxSolutions);
	}

    public boolean isProper() {
        /*  A so-called 'proper' puzzle has exactly one solution and no given is superfluous.
            I.e., removing any one of the givens would result in a puzzle with more than one
            solution. Proper puzzles are both sufficent and minimal. */

        return hasUniqueSolution() && isMinimal();
    }

    public boolean hasUniqueSolution() {
        return countSolutions(2) == 1;          // one and only one
    }

    public boolean isMinimal() {
        boolean minimal = true;

        for (int nth = 1; nth <= puzzle.numCells() && minimal; ++nth) {
            int val = puzzle.getCell(nth);

            if (val != Grid.EMPTY) {
                puzzle.setEmpty(nth);
                minimal = countSolutions(2) > 1;
                puzzle.setCell(nth, val);
            }
        }

        return minimal;
    }

    // static variants ------------------------------------------------------------------------

    public static Grid[] findSolutions(Grid puzzle, int maxSolutions) {
        return new Solver(puzzle).findSolutions(maxSolutions);
    }

    public static int countSolutions(Grid puzzle, int maxSolutions) {
        return new Solver(puzzle).countSolutions(maxSolutions);
	}

    public static boolean isProper(Grid grid) {
        return new Solver(grid).isProper();
    }

    public static boolean hasUniqueSolution(Grid grid) {
        return new Solver(grid).hasUniqueSolution();
    }

    public static boolean isMinimal(Grid grid) {
        return new Solver(grid).isMinimal();
    }

    // ----------------------------------------------------------------------------------------

	private static int search(Grid g, AbstractList<Grid> solutions, int maxSolutions) {
		assert g.isLegal();
        // lots of strategies that could be added here: http://www.sudokudragon.com/sudokustrategy.htm
		return bruteForce(g, solutions, maxSolutions);
	}

	private static int bruteForce(Grid template, AbstractList<Grid> solutions, int max) {
        if (max == 0) return 0;

		boolean incomplete = false;			// an empty cell has been found
		int col = 1, row = 1;
		search:
			for (row = 1; row <= template.numRows(); ++row)
				for (col = 1; col <= template.numColumns(); ++col)
					if (template.getCell(col, row) == Grid.EMPTY) {
						incomplete = true;
						break search;
					}

		if (!incomplete) {
			// no more empty cells - a complete solution has been found :)
			if (solutions != null) {
				assert template.isLegal();
				solutions.add(new Grid(template));
			}
			return 1;
		}

		int[] values = template.getCandidates(col, row);
        Util.shuffle(values);

        int found = 0;

		for (int value: values) {
			template.setCell(col, row, value);
			found += bruteForce(template, solutions, max - found);
		}
		template.setCell(col, row, Grid.EMPTY);

		return found;
	}

    private final Grid puzzle;
}
