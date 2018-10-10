package qirkat;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author Chelsea Chen
 */
public class BoardTest {

    private static final String INIT_BOARD =
        "  b b b b b\n  b b b b b\n  b b - w w\n  w w w w w\n  w w w w w";

    private static final String[] GAME1 =
    { "c2-c3", "c4-c2",
      "c1-c3", "a3-c1",
      "c3-a3", "c5-c4",
      "a3-c5-c3",
    };

    private static final String GAME1_BOARD =
        "  b b - b b\n  b - - b b\n  - - w w w\n  w - - w w\n  w w b w w";

    private static final String[] GAME2 = { "d3-c3", };

    private static final Move GAME3 = Move.move('b', '3', 'd', '3');

    private static final String GAME2_BOARD =
        "  b b b b b\n  b b b b b\n  b b w - w\n  w w w w w\n  w w w w w";

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(Move.parseMove(s));
        }
    }

    @Test
    public void testInit1() {
        Board b0 = new Board();
        assertEquals(INIT_BOARD, b0.toString());
    }

    @Test
    public void testMoves1() {
        Board b0 = new Board();
        makeMoves(b0, GAME1);
        assertEquals(GAME1_BOARD, b0.toString());
    }

    @Test
    public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test
    public void testGet1() {
        Board b0 = new Board();
        assertEquals(PieceColor.BLACK, b0.get('d', '4'));
    }

    @Test
    public void testGet2() {
        Board b0 = new Board();
        assertEquals(PieceColor.WHITE, b0.get(13));
    }

    @Test
    public void testToString() {
        Board b0 = new Board();
        assertEquals(INIT_BOARD, b0.toString());
    }

    @Test
    public void testJumpPossible() {
        Board b0 = new Board();
        makeMoves(b0, GAME2);
        assertEquals(true, b0.jumpPossible(11));
    }

    @Test
    public void testCheckJump() {
        Board b0 = new Board();
        makeMoves(b0, GAME2);
        assertEquals(true, b0.checkJump(GAME3, b0));

    }

    @Test
    public void testLegalMove() {
        Board b0 = new Board();
        makeMoves(b0, GAME2);
        assertEquals(true, b0.legalMove(GAME3));
    }
}

