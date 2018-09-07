# Judoku
## the judo of sudoku

A highly-optimised puzzle generator and solver. On my 5-year old Macbook Air, average time to solve a puzzle is 88 µs.

This started out as a project to test some ideas around JVM optimisations for high-performance (i.e., low-latency) real-time applications, but turned into a chance to explore what's been happening internally inside the JVM with its evolution and re-think my optimisation strategies. It's easy to see with the latest versions of the JVM that it's very much been optimised for immutable objects and ultra-fast heap allocation now. It's blindingly fast.

Time to brute-force recursively solve puzzles started out at 21 ms. Applying different optimisation strategies, some with scale factors of improvement, reduced this ultimately to 0.4% of that first number.

Anyhow, this library produces beautiful sudoku grids and solves given puzzles fast. It supports arbitrary sizes,
like 4x4 or 16x16, in addition to the normal 9x9. The sub-boxes don't even need to be square; you can have a 6x6 made up of 3x2 boxes if you like:

![4x4](https://i.imgur.com/l2uHIKX.png)
![6x6](https://i.imgur.com/zIxxBzf.png)

Or, how about this beauty?

![16x16](https://i.imgur.com/40Z018O.png)

## Optimisation strategies

The JVM sets the gold standard for virtual machine efficiency and that's been the result of sustained aggressive optimisation over a quarter of a century. Particularly in the last ten years, the JVM has brutalised the VM competition. Even in the native space it can beat C/C++ for heap allocation. I wrote some articles about some of my discoveries here:

- [Low-latency optimisation on the JVM](https://www.linkedin.com/pulse/low-latency-optimisation-jvm-steve-ball/)
- [Low-latency optimisation on the JVM—Part 2: JVM optimisations](https://www.linkedin.com/pulse/low-latency-optimisation-jvmpart-2-jvm-optimisations-steve-ball/)

## Running the command-line utilities

If you're using Linux or a Mac, then in the `./bin` directory there's a bash script that invokes the JAR in the right way (and a matching batch script for Windows):
```
~/Code/judoku> bin/judoku -c -n9
┌─────────┬─────────┬─────────┐
|       5 |    8    |    6  2 |
|         |         |    3  7 |
|         |       2 | 4       |
├─────────┼─────────┼─────────┤
| 6       |    9  4 | 3       |
|         | 1     7 |         |
|       8 | 2  6    |       1 |
├─────────┼─────────┼─────────┤
|       6 | 9       |         |
| 3  5    |         |         |
| 1  9    |    4    | 8       |
└─────────┴─────────┴─────────┘

~/Code/judoku> bin/judoku -c -b 5x2
┌───────────────┬───────────────┐
|       D       |               |
| E  F     H    |    I  J       |
├───────────────┼───────────────┤
|          B    |    D  A  J    |
|          A    | E        F  G |
├───────────────┼───────────────┤
|       B     C |          A    |
|    G          | H     I       |
├───────────────┼───────────────┤
| G  I        B |    C          |
|    A  C  E    |    G          |
├───────────────┼───────────────┤
|       H  F    |    J     I  E |
|               |       H       |
└───────────────┴───────────────┘
```
There are a lot of options for creating, solving, saving and viewing stored grids. Use the `-h` help option to see them all:
```
~/Code/judoku> bin/judoku -h
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
  -i N           Run a performance test of N iterations (default 10,000)
  FILE           Test FILE rather than default of randomly-generated puzzles

Examples:
  judoku -c -n4 -j tiny.json          # create tiny.json with a 4x4 puzzle
  judoku -r bee.json                  # show contents of bee.json
  judoku -rx bee.csv bee.json         # export JSON to CSV
  judoku -s hard.json                 # find a solution to the puzzle
  judoku -sC -m1000 4x4-empty.json    # count the solutions up to a max of 1000
  judoku -p                           # run standard performance test
```
