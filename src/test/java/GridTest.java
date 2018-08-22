package judoku;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GridTest {
    @Test
    public void constructor_grid_9() {
        Grid g = new Grid(9);

        assertEquals(9, g.getSize());
        assertEquals(3, g.getBoxWidth());
        assertEquals(3, g.getBoxHeight());
        assertEquals(9, g.getNumColumns());
        assertEquals(9, g.getNumRows());
        assertEquals(9, g.getNumBoxes());
        assertEquals(81, g.getNumCells());
        assertEquals(3, g.getNumStacks());
        assertEquals(3, g.getNumBands());

        assertEquals(20, g.toNth(2, 3));
        assertEquals(2, g.toColumn(20));
        assertEquals(3, g.toRow(20));
        assertEquals(1, g.toStackFromColumn(3));
        assertEquals(2, g.toStackFromColumn(4));
        assertEquals(1, g.toBandFromRow(3));
        assertEquals(2, g.toBandFromRow(4));
        assertEquals(4, g.toLeftColumnFromStack(2));
        assertEquals(6, g.toRightColumnFromStack(2));
        assertEquals(7, g.toTopRowFromBand(3));
        assertEquals(9, g.toBottomRowFromBand(3));

        assertEquals(81, g.getNumEmptyCells());
        assertEquals(0, g.getNumFilledCells());

        g = g.withCell(5, 5, 1);
        assertEquals(80, g.getNumEmptyCells());
        assertEquals(1, g.getNumFilledCells());
    }


    @Test
    public void constructor_grid_6() {
        Grid g = new Grid(2, 3);        // boxes 2-wide, 3-high

        assertEquals(g.getSize(), 6);
        assertEquals(g.getBoxWidth(), 2);
        assertEquals(g.getBoxHeight(), 3);
        assertEquals(g.getNumColumns(), 6);
        assertEquals(g.getNumRows(), 6);
        assertEquals(g.getNumBoxes(), 6);
        assertEquals(g.getNumCells(), 36);
        assertEquals(g.getNumStacks(), 3);
        assertEquals(g.getNumBands(), 2);

        assertEquals(g.toNth(2, 3), 14);
        assertEquals(g.toColumn(14), 2);
        assertEquals(g.toRow(14), 3);
        assertEquals(g.toStackFromColumn(2), 1);
        assertEquals(g.toStackFromColumn(3), 2);
        assertEquals(g.toBandFromRow(3), 1);
        assertEquals(g.toBandFromRow(4), 2);
        assertEquals(g.toLeftColumnFromStack(2), 3);
        assertEquals(g.toRightColumnFromStack(2), 4);
        assertEquals(g.toTopRowFromBand(2), 4);
        assertEquals(g.toBottomRowFromBand(2), 6);
    }
}
