package judoku;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GridTest {
    @Test
    public void constructor_grid_9() {
        Grid g = new Grid(9);

        assertEquals(g.size(), 9);
        assertEquals(g.boxWidth(), 3);
        assertEquals(g.boxHeight(), 3);
        assertEquals(g.numColumns(), 9);
        assertEquals(g.numRows(), 9);
        assertEquals(g.numBoxes(), 9);
        assertEquals(g.numCells(), 81);
        assertEquals(g.numStacks(), 3);
        assertEquals(g.numBands(), 3);

        assertEquals(g.toNth(2, 3), 20);
        assertEquals(g.toColumn(20), 2);
        assertEquals(g.toRow(20), 3);
        assertEquals(g.toStack(3), 1);
        assertEquals(g.toStack(4), 2);
        assertEquals(g.toBand(3), 1);
        assertEquals(g.toBand(4), 2);
        assertEquals(g.toLeftColumn(2), 4);
        assertEquals(g.toRightColumn(2), 6);
        assertEquals(g.toTopRow(3), 7);
        assertEquals(g.toBottomRow(3), 9);

        assertEquals(g.numEmptyCells(), 81);
        assertEquals(g.numFilledCells(), 0);
        g = g.withCell(5, 5, 1);
        assertEquals(g.numEmptyCells(), 80);
        assertEquals(g.numFilledCells(), 1);
    }

    @Test
    public void constructor_grid_6() {
        Grid g = new Grid(2, 3);        // boxes 2-wide, 3-high

        assertEquals(g.size(), 6);
        assertEquals(g.boxWidth(), 2);
        assertEquals(g.boxHeight(), 3);
        assertEquals(g.numColumns(), 6);
        assertEquals(g.numRows(), 6);
        assertEquals(g.numBoxes(), 6);
        assertEquals(g.numCells(), 36);
        assertEquals(g.numStacks(), 3);
        assertEquals(g.numBands(), 2);

        assertEquals(g.toNth(2, 3), 14);
        assertEquals(g.toColumn(14), 2);
        assertEquals(g.toRow(14), 3);
        assertEquals(g.toStack(2), 1);
        assertEquals(g.toStack(3), 2);
        assertEquals(g.toBand(3), 1);
        assertEquals(g.toBand(4), 2);
        assertEquals(g.toLeftColumn(2), 3);
        assertEquals(g.toRightColumn(2), 4);
        assertEquals(g.toTopRow(2), 4);
        assertEquals(g.toBottomRow(2), 6);
    }
}
