#Judoku
##the judo of sudoku

A highly optimised puzzle generator and solver. On my 5-year old Macbook Air, average time to solve a puzzle is 88 µs.

This started out as a project to test some ideas around JVM optimisations for low-latency banking applications, but turned
into an opportunity to explore what's been happening into the JVM in subsequent releases and re-think my optimisation
strategies. The JVM, for example, has very much been optimised for immutable objects and ultra-fast heap allocation.

Anyhow, this library produces beautiful sudoku grids and solves arbitrary puzzles super fast. It supports arbitrary sizes,
like 4x4 or 16x16, as well as the normal 9x9. the sub-boxes don't need to be square; you can have a 6x6 made up of 3x2
boxes if you like.
