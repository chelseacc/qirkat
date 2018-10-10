package qirkat;

import java.util.ArrayList;

import static qirkat.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Chelsea Chen
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 8;

    /**
     * A position magnitude indicating a win (for white if positive, black
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;

    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI for GAME that will play MYCOLOR.
     */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        return move;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == b.whoseMove()) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {


        if (depth == 0 || board.gameOver()) {
            return staticScore(board);
        }
        ArrayList<Board> childBoards = new ArrayList<>();

        for (Move mov : board.getMoves()) {
            if (mov != null && board.legalMove(mov)) {
                Board child = new Board();
                child.copy(board);
                child.makeMove(mov);
                childBoards.add(child);
            }
        }
        if (sense == 1) {
            int v = -INFTY;
            for (Board c : childBoards) {
                v = Math.max(v, findMove(c, depth - 1, false, -1, alpha, beta));
                alpha = Math.max(alpha, v);
                if (beta <= alpha) {
                    if (saveMove) {
                        _lastFoundMove = c.getStack().pop();
                    }
                    break;
                }
                if (saveMove) {
                    _lastFoundMove = c.getStack().pop();
                }
            }
            return v;
        } else {
            int v = INFTY;
            for (Board c2 : childBoards) {
                v = Math.min(v, findMove(c2, depth - 1, false, 1, alpha, beta));
                beta = Math.min(beta, v);
                if (beta <= alpha) {
                    if (saveMove) {
                        _lastFoundMove = c2.getStack().pop();
                    }
                    break;
                }
                if (saveMove) {
                    _lastFoundMove = c2.getStack().pop();
                }
            }
            return v;
        }
    }


    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {

        if (board.gameOver()) {
            if (board.getWinner().equals(myColor())) {
                return INFTY;
            } else {
                return -INFTY;
            }
        } else {
            int score = 0;
            for (int i = 0; i < board.getnewBoard().length; i++) {
                if (board.getnewBoard()[i].equals(myColor())) {
                    score++;
                } else {
                    score--;
                }
            }
            return score;
        }
    }
}



