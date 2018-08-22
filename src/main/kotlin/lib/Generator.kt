package judoku

object Generator {
    @JvmStatic
    fun generate(size: Int, symmetry: Symmetry, minimal: Boolean = true): Grid {
        val g = Grid(size)
        return generate(g.boxWidth, g.boxHeight, symmetry, minimal)
    }

    @JvmStatic
    fun generate(boxWidth: Int, boxHeight: Int, symmetry: Symmetry, _minimal: Boolean = true): Grid {
        var minimal = _minimal           // val to var

        // for these narrow boxes, no minimal solution will exist in some symmetry modes
        if (boxWidth == 1 || boxHeight == 1) minimal = false

        /*	if the symmetry mode is NONE (asymmetric) then it's always possible to generate a
          	proper puzzle the first time and to do so quickly: you start with an empty grid,
          	randomly select one of its many solutions and then randomly remove clues, one at
          	a time, for as long as the puzzle has only one solution. When you can't remove any
          	of the remaining clues without making the puzzle unsolvable, then it's minimal.

			Similarly, if the mode is symmetric but the puzzle isn't required to be strictly
			minimal, then you can do the same by emptying pairs of cells until there's more than
			one solution; at which point, put the last pair back and deliver the puzzle. It may
			potentially have one more clue than strictly required but it will have only one
			solution.

			But symmetric puzzles that must be strictly minimal face a difficulty. Suppose you
			have removed some pairs already and the puzzle still only has one solution (the one
			you started with). But when you remove the next pair, the number of solutions
			increases above one (i.e., the puzzle no longer has a unique solution and isn't
			solvable). If you put the pair you had experimentally removed back, although you
			will return to having only one solution, it may be that only one of the clues in
			the pair was required; in which case, it has one unique solution but it's not
			minimal. It may have one clue that's not stricly needed to solve the puzzle.

			In some combinations of the initially randomly generated solution and the particular
			symmetry mode, there may not exist any proper puzzle. In that case, generate another
			solution and try again. */

        if (symmetry == Symmetry.NONE) minimal = false

        var out: Grid

        do
            out = generatePotentiallyNonMinimal(boxWidth, boxHeight, symmetry)
        while (minimal && !Solver.isMinimal(out))

        return out
    }

    private fun generatePotentiallyNonMinimal(boxWidth: Int, boxHeight: Int,
            symmetry: Symmetry): Grid {
        val solution = Solver.findSolution(Grid(boxWidth, boxHeight))
        check(solution != null)
        var puzzle = solution!!

        /*	generate indices into the grid's cells and shuffle their order. The cells of the
		  	grid will be traversed in this order looking for clues that can be removed.
		  	Traversing them in this random order means that the generated puzzles will have
			randomly placed spaces and aren't 'top heavy' */

        val shuffled = IntArray(puzzle.numCells, { it + 1 }).apply() { this.shuffle() }

        /*	start out with a complete solution with all cells filled and iteratively test
			which pairs of cells may be emptied while keeping the puzzle to only one solution */

        for (a in shuffled) {
            // a & b, the two candidate cells to be emptied
            val b = symmetry.apply(puzzle, a)

            //  if a maps to b then b maps to a. Only need to process each pair once
            if (a > b) continue

            val trial = puzzle.withEmpty(a).withEmpty(b)

            val uniqueSolution = Solver.countSolutions(trial, 2) == 1

            /*	if there's still a unique solution having removed that pair of cells, proceed
			 	with the modified grid as the base case (and try other pairs too) */
            if (uniqueSolution) puzzle = trial
        }

        return puzzle
    }
}
