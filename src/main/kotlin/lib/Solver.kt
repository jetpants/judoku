package judoku

class Solver(private val puzzle: Grid) {
    init { check(puzzle.isLegal()) }

    fun findSolution(): Grid? {
        val solutions = findSolutions(1)
        return if (solutions.size == 0) null else solutions[0]
    }

    fun findSolutions(max: Int): ArrayList<Grid> {
        val solutions = ArrayList<Grid>(max)
        val n = search(puzzle, solutions, max, puzzle.numFilledCells() == 0)
        check(n == solutions.size) { "$n <> ${solutions.size}" }
        return solutions
    }

    fun countSolutions(max: Int): Int = search(puzzle, null, max, false)

    fun isUnique() = countSolutions(2) == 1      // exactly one solution

    fun isMinimal(): Boolean {
        if (!puzzle.isViable()) return false;

        for (n in 1..puzzle.numCells)
            if (!puzzle.isEmpty(n) && countSolutions(puzzle.withEmpty(n), 2) < 2)
                return false

        return true
    }

    fun isProper(): Boolean {
        /*  A so-called 'proper' puzzle has exactly one unique solution and no given is
            superfluous. I.e., removing any one of the givens would result in a puzzle with more
            than one solution. Proper puzzles are both sufficent and minimal. */
        return isUnique() && isMinimal()
    }

    var nodeCounting = false
    var nodeCount: Int = 0      // incremented with each node traversed

    companion object {
        @JvmStatic fun findSolution(puzzle: Grid) = Solver(puzzle).findSolution()
        @JvmStatic fun findSolutions(puzzle: Grid, max: Int) = Solver(puzzle).findSolutions(max)
        @JvmStatic fun countSolutions(puzzle: Grid, max: Int) = Solver(puzzle).countSolutions(max)
        @JvmStatic fun isUnique(grid: Grid) = Solver(grid).isUnique()
        @JvmStatic fun isMinimal(grid: Grid) = Solver(grid).isMinimal()
        @JvmStatic fun isProper(grid: Grid) = Solver(grid).isProper()
    }

    private data class Frame(
        val cache: OptionsCache,
        val solutions: ArrayList<Grid>?,
        val max: Int,                       // maximum solutions to find/count
        val randomise: Boolean,             // generate solutions in random order
        var total: Int                      // number of solutions found
    )

    private fun search(g: Grid, solutions: ArrayList<Grid>?, max: Int, randomise: Boolean): Int {
        if (!g.isViable() || max <= 0) return 0
        val frame = Frame(OptionsCache(g), solutions, max, randomise, 0)
        return traverse(g, max, frame)
    }

    private fun traverse(g: Grid, remaining: Int, frame: Frame): Int {
        if (nodeCounting) ++nodeCount
        if (remaining == 0) return 0

        val target = frame.cache.fewestOptions()
        if (target == null) { frame.solutions?.add(Grid(g)); return 1 }
        if (target.mask == 0) return 0      // zombie leaf node

        var value = 0
        var mask = target.mask

        if (frame.randomise) {
            assert(frame.solutions != null)
            val rot = Util.random.nextInt(g.size) + 1   // +1 for bit-0 (EMPTY)
            value += rot
            mask = Integer.rotateRight(mask, rot)
        }

        var found = 0
        var current = Grid.EMPTY

        repeat (target.popcount) {
            val zeroes = Integer.numberOfTrailingZeros(mask)
            if (zeroes > 0) {
                value = (value + zeroes) % Integer.SIZE
                mask = mask ushr zeroes
            }

            frame.cache.update(target.n, current, value); current = value

            g._setCell(target.n, value)
            found += traverse(g, remaining - found, frame)

            ++value; mask = mask ushr 1
        }

        g._setCell(target.n, Grid.EMPTY)
        frame.cache.update(target.n, current, Grid.EMPTY)

        return found
    }
}

/*  EXPERIMENTAL - splitting the traversal across multiple threads

    // new thread per sub-root

        val threads = Array<Thread>(target.options.size, {
                val child = g.withCell(target.n, target.options[it])
                Thread(Runnable { traverse(child, max, frame) })
            })

            for (t in threads) t.start()
            for (t in threads) t.join()

            val total = minOf(frame.total.get(), max)
            check(solutions == null || solutions.size == total) { "${solutions!!.size} <> ${total}" }
            return total

    // re-use threads from POOL_SIZE

        companion object {
            // executor.shutdown() needs to be called to allow the program to exit
            private final val POOL_SIZE = 8
            private val executor = Executors.newFixedThreadPool(POOL_SIZE);
        }

        val futures = Array<Future<*>>(POOL_SIZE, { executor.submit(
            Runnable {
                var i = it
                var child = Grid(g)
                var found = 0

                while (i < target.options.size) {
                    child = child.withCell(target.n, target.options[i])
                    found += traverse(child, max - found, frame)
                    i += POOL_SIZE
                }
            }
        )})

        for (f in futures) f.get()

        val total = minOf(frame.total.get(), max)
        check(solutions == null || solutions.size == total) { "${solutions!!.size} <> ${total}" }
        return total

    // multi-thread-sade traverse()

        private fun traverse(g: Grid, remaining: Int, frame: Frame): Int {
            if (remaining == 0 || frame.stop) return 0

            val target = fewestOptions(g)

            if (target == null) {
                if (frame.total.incrementAndGet() == frame.max)
                    frame.stop = true

                if (frame. solutions != null)
                    synchronized(frame.solutions) {
                        if (frame.solutions.size < frame.max)
                            frame.solutions.add(Grid(g))
                    }

                return 1
            }

            //  optimisation - if you're only counting them, the order doesn't matter
            if (frame.solutions != null) target.options.shuffle()

            var found = 0

            for (possibility in target.options) {
                if (frame.stop) break
                g.setCell(target.n, possibility)
                found += traverse(g, remaining - found, frame)
            }

            g.setCell(target.n, Grid.EMPTY)

            return found
        }

    // mostOptions()

        private fun mostOptions(g: Grid): Options? {
            var best: Options? = null

            for (n in 1..g.numCells)
                if (g.isEmpty(n)) {
                    val opts = g.getOptions(n)

                    when {
                        opts.size == g.size() -> return Options(n, opts)
                        best == null -> best = Options(n, opts)
                        best.options.size < opts.size -> best = Options(n, opts)
                    }
                }

            return best
        }
*/
