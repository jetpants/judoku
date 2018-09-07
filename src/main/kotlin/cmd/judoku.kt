package cmd;

import kotlin.system.exitProcess
import java.io.*
import java.util.Random
import judoku.*
import org.apache.commons.cli.*

fun main(args: Array<String>) {
    val grid = parse(args)

    if (jsonFile != null && grid != null)
        try {
            val writer = FileWriter(jsonFile)
            grid.toJson(writer)
            writer.close()
        } catch (e: IOException) {
            syserrln("judoku: Unable to write to file: ${e.message}")
            exitProcess(1)
        }

    if (csvFile != null && grid != null)
        try {
            val writer = FileWriter(csvFile)
            grid.toCsv(writer)
            writer.close()
        } catch (e: IOException) {
            syserrln("judoku: Unable to write to file: ${e.message}")
            exitProcess(1)
        }
}

private fun parse(args: Array<String>): Grid? {
    if (!(args.size > 0 && args[0].length > 1 && args[0][0] == '-')) {
        syserrln("judoku -h for help")
        exitProcess(1)
    }

    if (args[0] == "--help") args[0] = "-h"

    val mode = args[0][1]
    val parser = DefaultParser()
    val options = Options()

    when (mode) {
        'c' -> {
            options.addOption("c", false, "")
            options.addOption("b", true, "")
            options.addOption("e", false, "")
            options.addOption("j", true, "")
            options.addOption("n", true, "")
            options.addOption("q", false, "")
            options.addOption("S", true, "")
            options.addOption("x", true, "")
            options.addOption("y", true, "")
        }

        'r' -> {
            options.addOption("r", false, "")
            options.addOption("v", false, "")
            options.addOption("x", true, "")
        }

        's' -> {
            options.addOption("s", false, "")
            options.addOption("a", false, "")
            options.addOption("C", false, "")
            options.addOption("j", true, "")
            options.addOption("m", true, "")
            options.addOption("S", true, "")
            options.addOption("x", true, "")
        }

        'p' -> {
            options.addOption("p", false, "")
            options.addOption("i", true, "")
            options.addOption("S", true, "")
            options.addOption("v", false, "")
        }

        'h' -> { help(); return null }
        'V' -> { version(); return null }

        else -> {
            syserrln("judoku: First option must be one of -c, -r, -s, -p")
            syserrln("judoku -h for help")
            exitProcess(1)
        }
    }

    val cmdline: CommandLine

    try {
        cmdline = parser.parse(options, args)
    } catch (e: Exception) {
        syserrln("judoku: ${e.message}")
        syserrln("judoku -h for help")
        exitProcess(1)
    }

    val params = cmdline.getArgs()
    var min = 1
    var max = 1

    when (mode) {
        'c' -> { min = 0; max = 0 }
        'p' -> { min = 0 }
    }

    if (params.size < min ) {
        syserrln("judoku: Missing argument")
        exitProcess(1)
    }

    if (params.size > max ) {
        syserrln("judoku: Extra argument: ${params[max]}")
        exitProcess(1)
    }

    // common command-line options
    if (cmdline.hasOption("j")) jsonFile = cmdline.getOptionValue("j")
    if (cmdline.hasOption("v")) verbose = true
    if (cmdline.hasOption("x")) csvFile = cmdline.getOptionValue("x")
    if (cmdline.hasOption("S")) {
        val value = cmdline.getOptionValue("S")
        try {
            seed = value.toLong()
            Util.setRandom(Random(seed!!))
        } catch (e: NumberFormatException) {
            syserrln("judoku: Bad seed value: ${value}")
            exitProcess(1)
        }
    }

    return when (mode) {
        'c' -> { create(cmdline) }
        'r' -> { read(params[0]) }
        's' -> { solve(cmdline, params[0]) }
        'p' -> { perftest(cmdline, if (params.size == 0) null else params[0]); null }
        else -> throw IllegalStateException()
    }
}

private fun create(cmdline: CommandLine): Grid {
    var size: Int? = null
    var boxWidth: Int? = null
    var boxHeight: Int? = null
    var empty = false
    var quick = false
    var symmetry: Symmetry = Symmetry.ROTATIONAL

    if (cmdline.hasOption("e")) empty = true
    if (cmdline.hasOption("q")) quick = true

    if (cmdline.hasOption("b")) {
        val value = cmdline.getOptionValue("b")
        val regex = Regex("""(\d+)[xX](\d+)""")

        val matchResults = regex.matchEntire(value)
        if (matchResults == null) {
            syserrln("judoku: Bad dimensions: $value")
            exitProcess(1)
        }

        val (w, h) = matchResults.destructured
        try {
            boxWidth = w.toInt()
            boxHeight = h.toInt()
            if (boxWidth < 1) throw NumberFormatException()
            if (boxHeight < 1) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            syserrln("judoku: Bad dimensions: $value");
            exitProcess(1)
        }
    }

    if (cmdline.hasOption("n")) {
        val value = cmdline.getOptionValue("n")
        try {
            size = value.toInt()
            if (size < 1) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            syserrln("judoku: Bad size: $value")
            exitProcess(1)
        }
    }

    if (cmdline.hasOption("y")) {
        val value = cmdline.getOptionValue("y")
        val sym = Symmetry.toSymmetry(value)
        if (sym == null) {
            syserrln("judoku: Bad symmetry: $value")
            exitProcess(1)
        }
        symmetry = sym
    }

    if (size != null && boxWidth != null && boxHeight != null && size != boxWidth * boxHeight) {
        syserrln("judoku: Conflicting dimensions: ${size} vs ${boxWidth}x${boxHeight}")
        exitProcess(1)
    }

    val grid = try {
        when {
            empty && size != null -> Grid(size)
            empty && boxWidth != null -> Grid(boxWidth, boxHeight!!)
            empty -> Grid()
            size != null -> Generator.generate(size, symmetry, !quick)
            boxWidth != null -> Generator.generate(boxWidth, boxHeight!!, symmetry, !quick)
            else -> Generator.generate(Grid.DEFAULT_SIZE, symmetry, !quick)
        }
    } catch (e: IllegalArgumentException) {
        syserrln("judoku: ${e.message}")
        exitProcess(1)
    }

    println(grid)
    return grid
}

private fun read(file: String): Grid {
    var grid = readGrid(file)
    println(grid)
    if (verbose) statistics(grid)
    return grid
}

private fun statistics(grid: Grid) {
    fun Boolean.toYN() = if (this) "Yes" else "No"

    println("Cells:                   ${grid.numCells}")
    println("Filled/empty:            ${grid.numFilledCells}/${grid.numEmptyCells}")
    println("Viable:                  ${grid.isViable().toYN()}")
    println("Duplicates:              ${grid.hasDuplicates().toYN()}")
    println("Zombies:                 ${grid.hasZombies().toYN()}")
    print(  "Calculating...")

    val MAX = 1_000_000
    val solver = Solver(grid)
    solver.nodeCounting = true
    solver.nodeCount = 0
    val n = solver.countSolutions(MAX + 1)
    val nodes = solver.nodeCount

    print("\r")
    println("Solutions:               ${if (n > MAX)
        String.format(">%,d", MAX) else String.format("%,d", n)}")
    println("Nodes traversed:         ${String.format("%,d", nodes)}")

    print(  "Calculating...")
    val unique = n == 1
    val minimal = solver.isMinimal()        // time-consuming
    val proper = unique && minimal

    print("\r")
    println("Proper:                  ${proper.toYN()}")
    println("Unique solution:         ${unique.toYN()}")
    println("Minimal:                 ${minimal.toYN()}")
}

private fun solve(cmdline: CommandLine, file: String): Grid? {
    var countOnly = false
    var max: Int? = null

    if (cmdline.hasOption("a")) max = Int.MAX_VALUE
    if (cmdline.hasOption("C")) countOnly = true
    if (cmdline.hasOption("m")) {
        val value = cmdline.getOptionValue("m")
        try {
            max = value.toInt()
            if (max < 0) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            syserrln("judoku: Bad max value: ${value}")
            exitProcess(1)
        }
    }

    val grid = readGrid(file)
    val solver = Solver(grid)

    if (countOnly) {
        val count = if (max == null) solver.countSolutions() else solver.countSolutions(max)
        println(count)
        return null
    }

    val solutions = if (max == null) solver.findSolutions(1) else solver.findSolutions(max)

    if (solutions.size == 0 && max != 0) {
        syserrln("judoku: Grid has no solution")
        exitProcess(1)
    }

    for (solution in solutions) println(solution)

    return if (solutions.size == 1) solutions[0] else null
}

private fun perftest(cmdline: CommandLine, file: String?) {
    var iterations = TEST_SIZE

    if (cmdline.hasOption("i")) {
        val value = cmdline.getOptionValue("i")
        try {
            iterations = value.toInt()
            if (iterations < 1) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            syserrln("judoku: Bad max value: ${value}")
            exitProcess(1)
        }
    }

    if (!verbose) {
        // verbose off = lean mode
        val loader = ClassLoader.getSystemClassLoader()
        loader.setDefaultAssertionStatus(false)
        loader.setPackageAssertionStatus("judoku", false)
        loader.setClassAssertionStatus("judoku.Grid", false);
        loader.setClassAssertionStatus("judoku.Solver", false);
    }

    // give a consisent benchmark of the same (non-)randomly-generated grids
    if (seed == null) Util.setRandom(Random(0L))

    val testGrids: Array<Grid>

    if (file == null) {
        val n = (iterations + TESTS_PER_GRID - 1) / TESTS_PER_GRID
        testGrids = generateTestGrids(n)
    } else {
        val grid = readGrid(file)
        val solver = Solver(grid)

        if (!solver.hasSolution()) {
            syserrln("judoku: Grid has no solution")
            exitProcess(1)
        }

        testGrids = Array<Grid>(1, { grid })
    }

    speedTest(testGrids, iterations)
}

private fun generateTestGrids(n: Int): Array<Grid> {
    val prompt = "\rGenerating test-puzzles..."
    if (verbose) print(prompt)
    var percent = -1;

    val out = Array<Grid>(n, {
        if (verbose) {
            val p = Math.round(100.0 * it / n).toInt()
            if (p != percent) { percent = p; print("$prompt $p%") }
        }

        Generator.generate(9, Symmetry.NONE, true)
    })

    if (verbose) println("$prompt 100%")

    return out
}

private fun speedTest(testGrids: Array<Grid>, iterations: Int) {
    val seconds = solveTestGrids(testGrids, iterations)

    val bold = if (Util.isAnsiTerminal()) Util.ANSI_BOLD else ""
    val norm = if (Util.isAnsiTerminal()) Util.ANSI_NORMAL else ""

    println()
    println("  Elapsed time:        $bold${String.format("%,.1f", seconds)} s$norm")
    println("  Puzzles solved:      $bold${String.format("%,d", iterations)}$norm")
    println("  Solutions/second:    $bold${String.format("%,.0f", iterations / seconds)}$norm")
    println("  Avg-time/solution:   $bold${String.format("%,.0f", seconds * 1000000 / iterations)} µs$norm")
    println()
}

private fun solveTestGrids(testGrids: Array<Grid>, iterations: Int): Double {
    val prompt = "\rSolving test-puzzles......"
    if (verbose) print(prompt)
    var percent = -1;

    /*	why use nanoTime() and not currentTimeMillis()? - for a fascinating answer,
        see here: https://stackoverflow.com/a/1776053 */
    val start = System.nanoTime()

    repeat (iterations) {
        if (verbose) {
            val p = Math.round(100.0 * it / iterations).toInt()
            if (p != percent) { percent = p; print("$prompt $p%") }
        }

        val n = Solver.countSolutions(testGrids[it % testGrids.size], 1)
        assert(n == 1)
    }

    val elapsed = System.nanoTime() - start;

    if (verbose) println("$prompt 100%")

    return elapsed / 1000000000.0
}

private fun readGrid(file: String): Grid = try {
    Grid.newFromJson(FileReader(file))
} catch (e: IOException) {
    syserrln("judoku: Cannot read file: $file")
    exitProcess(1)
} catch (e: Exception) {
    syserrln("judoku: Bad JSON or illegal values: $file")
    exitProcess(1)
}

private fun version() {
    val jar = Util.jarTitle() ?: "judoku"
    val version = Util.jarVersion()
    val vendor = Util.jarVendor()

    println("$jar ${version ?: ""}")
    if (vendor != null) println(vendor)

    exitProcess(1);
}

private fun help() {
    val help =
    //-------1---------2---------3---------4---------5---------6---------7---------8
    """
    judoku -crspV [OPTION...] [FILE]

    First option must be a mode specifier:
      -c Create  -r Read  -s Solve  -p Performance  -V Version

    Common options:
      -j JSONFILE    Write resulting grid to JSON file
      -S SEED        Seed the random number generator with the number SEED
      -v             Verbose
      -x CSVFILE     Export resulting grid to CSV file

    judoku -c[bejnqSvxy]
      Create a puzzle with one unique solution and the minimum number of clues
      -b WxH         Puzzle with boxes of width W and height H (default is 3x3)
      -e             Create an empty grid
      -n N           Size of puzzle is N x N (default is 9)
      -q             Much quicker but possibly with one extra unneeded clue
      -y MODE        Symmetry mode: rotational (default), diagonal, horizontal,
                     vertical, none (abbreviations ok)

    judoku -r[vx] FILE
      Read the JSON grid file and render it as text. Use verbose to see statistics.

    judoku -s[aCjmSx] FILE
      Solve a puzzle
      -a             Show all solutions
      -C             Count solutions instead of showing them
      -m MAX         Show/count up to a maximum of MAX solutions (def. 1)

    judoku -p[iSv] [FILE]
      -i N           Run a performance test of N iterations (default ${String.format("%,d", TEST_SIZE)})
      FILE           Test FILE rather than default of randomly-generated puzzles

    Examples:
      judoku -c -n4 -j tiny.json          # create tiny.json with a 4x4 puzzle
      judoku -r bee.json                  # show contents of bee.json
      judoku -rx bee.csv bee.json         # export JSON to CSV
      judoku -s hard.json                 # find a solution to the puzzle
      judoku -sC -m1000 4x4-empty.json    # count the solutions up to a max of 1000
      judoku -p                           # run standard performance test
    """.trimIndent()

    println(help)
    System.exit(0)
}

private fun syserrln(message: String) { System.err.println(message) }

private val TEST_SIZE = 10000		// default number of iterations in perf-test
/*  this number has been tuned such that the performance test spends about as much time
    generating puzzles as it does solving them */
private val TESTS_PER_GRID = 65

// common command-line options
private var jsonFile: String? = null        // -j
private var seed: Long? = null              // -S
private var verbose = false                 // -v
private var csvFile: String? = null         // -x
