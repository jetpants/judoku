# Judoku
## the judo of sudoku

A highly optimised puzzle generator and solver. On my 5-year old Macbook Air, average time to solve a puzzle is 88 µs.

This started out as a project to test some ideas around JVM optimisations for low-latency banking applications, but turned into a chance to explore what's been happening internally inside the JVM with its evolution and re-think my optimisation strategies. The JVM, for example, has very much been optimised for immutable objects and ultra-fast heap allocation.

Anyhow, this library produces beautiful sudoku grids and solves given puzzles super fast. It supports arbitrary sizes,
like 4x4 or 16x16, in addition to the normal 9x9. The sub-boxes don't need to be square; you can have a 6x6 made up of 3x2
boxes if you like.

I wrote some articles on what I discovered. It's a series looking at the JVM, sudoku strategies, some Kotlin coding techniques:

[Low-latency optimisation on the JVM](https://www.linkedin.com/pulse/low-latency-optimisation-jvm-steve-ball/)
