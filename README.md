# Judoku
## the judo of sudoku

A highly-optimised puzzle generator and solver. On my 5-year old Macbook Air, average time to solve a puzzle is 88 µs.

This started out as a project to test some ideas around JVM optimisations for low-latency banking applications, but turned into a chance to explore what's been happening internally inside the JVM with its evolution and re-think my optimisation strategies. The JVM, for example, has very much been optimised for immutable objects and ultra-fast heap allocation.

Time to brute-force recursively solve puzzles started out at 21 ms. Applying different optimisation strategies, some with scale factors of improvement reduced this ultimately by 99.6%.

Anyhow, this library produces beautiful sudoku grids and solves given puzzles fast. It supports arbitrary sizes,
like 4x4 or 16x16, in addition to the normal 9x9. The sub-boxes don't even need to be square; you can have a 6x6 made up of 3x2 boxes if you like:

![4x4](https://i.imgur.com/l2uHIKX.png)
![6x6](https://i.imgur.com/zIxxBzf.png)

Or, how about this beauty?

![16x16](https://i.imgur.com/40Z018O.png)

## Optimisation strategies

I wrote some articles on what I discovered about the JVM and the way its been so aggressively optimised in recent years:

[Low-latency optimisation on the JVM](https://www.linkedin.com/pulse/low-latency-optimisation-jvm-steve-ball/)
[Low-latency optimisation on the JVM—Part 2: JVM optimisations](https://www.linkedin.com/pulse/low-latency-optimisation-jvmpart-2-jvm-optimisations-steve-ball/)

## Running the command-line utilities

If you're using Linux or a Mac, then in the `./bin` directory there are bash scripts that invoke the JAR in the right way:

```
~/Code/judoku> jdgen --size=9
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
```

All commands have a `--help` option to show what the usage text:

```
~/Code/judoku> jdsolve --help
Usage: jdsolve [OPTION]... JSONFILE...
Calculate solutions for saved Sudoku grids.

  --all          Show all solutions
  --max=N        Show maximum of N solutions (default: 1)
  --count        Show number of solutions only
  --csv=FILE     Write CSV representation of solution to FILE
  --json=FILE    Write JSON representation of solution to FILE
```

If you're using Windows, you will need to invoke the Java run-time manually:

```
C:\judoku>java -cp "%CLASSPATH%:./build/libs/Judoku-1.2.jar:./build/classes/java/main/cmdline" cmdline.jdgen --help
Usage: jdgen [OPTION]...
Generate a grid for a 'proper' puzzle. A proper puzzle is a grid that has only
one solution and, additionally, where removing any one of its clues would
yield a puzzle that would no longer have a unique solution.

  --size=N       A square grid of size N
  --box=W,H      Grid with boxes of width W and height H
  --sym=MODE     Symmetry mode, one of the following (or unique prefix of):
                 rotate180 (default), diagonal, horizontal, vertical, none,
                 random
  --fast         Quicker but puzzles may not be proper; there may be one more
                 extra clue than strictly required to have a unique solution
  --csv=FILE     Write corresponding CSV representation to FILE
  --json=FILE    Write corresponding JSON representation to FILE
  --empty        Generate an empty grid of the requested size
  --seed=SEED    Seeds the random number generator with SEED.
```
