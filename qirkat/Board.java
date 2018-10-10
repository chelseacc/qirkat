package qirkat;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;
import static qirkat.PieceColor.*;
import static qirkat.Move.*;
import java.util.Stack;

/** A Qirkat board.   The squares are labeled by column (a char value between
 *  'a' and 'e') and row (a char value between '1' and '5'.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (with row 0 being the bottom row)
 *  counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Chelsea Chen
 */
class Board extends Observable {

    /** A new, cleared board at the start of the game. */
    Board() {
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        internalCopy(b);
    }

    /** Return a constant view of me (allows any access method, but no
     *  method that modifies it). */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions. */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        status = new int[SIDE * SIDE];
        allmoves = new Stack<Move>();
        winner = null;
        newBoard = new PieceColor[]{WHITE, WHITE, WHITE, WHITE, WHITE,
            WHITE, WHITE, WHITE, WHITE, WHITE,
            BLACK, BLACK, EMPTY, WHITE, WHITE,
            BLACK, BLACK, BLACK, BLACK, BLACK,
            BLACK, BLACK, BLACK, BLACK, BLACK};

        setChanged();
        notifyObservers();
    }

    /** Copy B into me. */
    void copy(Board b) {
        internalCopy(b);
    }

    /** Copy B into me. */
    private void internalCopy(Board b) {
        for (int i = 0; i < newBoard.length; i++) {
            set(i, b.newBoard[i]);
        }

        for (PieceColor value : PIECE_VALUES) {
            if (b._whoseMove == value) {
                this._whoseMove = value;
            }
            if (b.winner == value) {
                this.winner = value;
            }
        }

        for (int i = 0; i < status.length; i++) {
            this.status[i] = b.status[i];
        }

        ArrayList<Move> arraymove = new ArrayList<>();

        for (int i = 0; i < b.allmoves.size(); i++) {
            arraymove.add(b.allmoves.pop());
        }

        Stack<Move> allmovesnew = new Stack<Move>();

        for (int i = arraymove.size(); i > 0; i--) {
            allmovesnew.push(arraymove.get(i - 1));
            b.allmoves.push(arraymove.get(i - 1));
        }
        allmoves = allmovesnew;

        this._gameOver = b.gameOver();

    }

    /** Set my contents as defined by STR.  STR consists of 25 characters,
     *  each of which is b, w, or -, optionally interspersed with whitespace.
     *  These give the contents of the Board in row-major order, starting
     *  with the bottom row (row 1) and left column (column a). All squares
     *  are initialized to allow horizontal movement in either direction.
     *  NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }

        _whoseMove = nextMove;
        _gameOver = false;
        status = new int[SIDE * SIDE];
        allmoves = new Stack<Move>();
        winner = null;

        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b': case 'B':
                set(k, BLACK);
                break;
            case 'w': case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }

        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if the current player has
     *  no moves. */
    boolean gameOver() {
        return _gameOver;
    }

    /** Return the current contents of square C R, where 'a' <= C <= 'e',
     *  and '1' <= R <= '5'.  */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /** Return the current contents of the square at linearized index K. */
    PieceColor get(int k) {
        assert validSquare(k);
        return newBoard[k];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'e', and
     *  '1' <= R <= '5'. */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /** Set get(K) to V, where K is the linearized index of a square. */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        newBoard[k] = v;
    }

    /** Return true iff MOV is legal on the current board. */
    boolean legalMove(Move mov) {
        int a = mov.fromIndex();
        if (_whoseMove.equals(get(a)) && getMoves().contains(mov)) {
            if (!mov.isJump() && ((status[a] == -1 && mov.isRightMove())
                    || (status[a] == 1 && mov.isLeftMove()))) {
                return false;
            } else if ((mov.isLeftMove() || mov.isRightMove())
                && ((whoseMove().equals(WHITE) && row(a) == '5')
                || (whoseMove().equals(BLACK) && row(a) == '1'))) {
                return false;
            }
            return true;
        }
        return false;
    }

    /** Return a list of all legal moves from the current position. */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /** Add all legal moves from the current position to MOVES. */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }

    /** Add all legal non-capturing moves from the position
     *  with linearized index K to MOVES. */
    private void getMoves(ArrayList<Move> moves, int k) {
        int[] evenw = {-1, 1, 4, 5, 6};
        int[] oddw = {-1, 1, 5};
        int[] eveno = {-6, -5, -4, -1, 1};
        int[] oddo = {-5, -1, 1};
        int[] ind;

        if (whoseMove().equals(WHITE)) {
            if (k % 2 == 0) {
                ind = evenw;
            } else {
                ind = oddw;
            }
        } else {
            if (k % 2 == 0) {
                ind = eveno;
            } else {
                ind = oddo;
            }
        }

        if (whoseMove().equals(get(k)) && !EMPTY.equals(get(k))) {
            for (int m : ind) {
                boolean val = validSquare(k + m) && EMPTY.equals(get(k + m));
                boolean up = m % 5 == 0;
                boolean left =  (m - 1) % 5 == 0 && col(k) < col(k + m);
                boolean right = (m + 1) % 5 == 0 && col(k + m) < col(k);

                if (val && (up || left || right)) {
                    Move newmove = move(col(k), row(k), col(k + m), row(k + m));
                    moves.add(newmove);
                }
            }
        }
    }

    /** Add all legal captures from the position with linearized index K
     *  to MOVES. */
    private void getJumps(ArrayList<Move> moves, int k) {

        Move tmove;

        int[] even = {-6, -5, -4, -1, 1, 4, 5, 6};
        int[] odd = {-5, -1, 1, 5};
        int[] gen;

        if (k % 2 == 0) {
            gen = even;
        } else {
            gen = odd;
        }

        if (get(k).equals(whoseMove()) && !get(k).equals(EMPTY)) {
            for (int m : gen) {
                boolean v = validSquare(k + m + m)
                        && EMPTY.equals(get(k + m + m));
                boolean up = col(k) == col(k + m)
                        && col(k + m) == col(k + m + m);
                boolean right = col(k) < col(k + m)
                        && col(k + m) < col(k + m + m);
                boolean left = col(k) > col(k + m)
                        && col(k + m) > col(k + m + m);

                if (v && (up || right || left)) {
                    ArrayList<Move> possJumps = new ArrayList<>();
                    Board t = new Board();
                    t.copy(this);
                    tmove = Move.move(col(k), row(k),
                            col(k + m + m), row(k + m + m));

                    if (checkJump(tmove, this)) {
                        t.set(tmove.toIndex(), _whoseMove);
                        t.set(tmove.jumpedIndex(), EMPTY);
                        t.set(tmove.fromIndex(), EMPTY);
                        t.getJumps(possJumps, k + m + m);
                        if (possJumps.size() == 0) {
                            moves.add(tmove);
                        } else {
                            for (Move p : possJumps) {
                                moves.add(Move.move(tmove, p));
                            }
                        }
                    }
                }
            }
        }
    }

    /** Return true iff MOV is a valid jump sequence on the current board.
     *  MOV must be a jump or null. Testing valid jumps on board SET.  */
    boolean checkJump(Move mov, Board set) {
        if (mov == null) {
            return true;
        } else {
            Board t = new Board();
            t.copy(set);

            if (validSquare(mov.toIndex())
                    && whoseMove().equals(get(mov.fromIndex()))
                    && EMPTY.equals(get(mov.toIndex()))
                    && whoseMove().opposite().equals(get(mov.jumpedIndex()))) {

                t.set(mov.toIndex(), whoseMove());
                t.set(mov.jumpedIndex(), EMPTY);
                t.set(mov.fromIndex(), EMPTY);
                return checkJump(mov.jumpTail(), t);
            } else {
                return false;
            }
        }
    }

    /** Return true iff a jump is possible for a piece at position C R. */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /** Return true iff a jump is possible for a piece at position with
     *  linearized index K. */
    boolean jumpPossible(int k) {
        int[] emoves = {-6, -5, -4, -1, 1, 4, 5, 6};
        int[] omoves = {-5, -1, 1, 5};
        int[] jumps;
        if (k % 2 == 0) {
            jumps = emoves;
        } else {
            jumps = omoves;
        }

        if (whoseMove().equals(get(k))) {
            for (int move : jumps) {
                if (validSquare(k + move + move)
                        && get(k + move + move).equals(EMPTY)
                        && get(k).opposite().equals(get(k + move))
                        && ((col(k) == col(k + move)
                        && col(k + move) == col(k + move + move))
                        || (col(k) < col(k + move)
                        && col(k + move) < col(k + move + move))
                        || (col(k) > col(k + move)
                        && col(k + move) > col(k + move + move)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Perform the move C0R0-C1R1. Assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /** Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     *  Assumes the result is legal. */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /** Make the Move MOV on this Board, assuming it is legal. */
    void makeMove(Move mov) {
        assert legalMove(mov);
        Move makemove = mov;

        if (legalMove(makemove)) {
            while (makemove != null) {
                if (makemove.isLeftMove()) {
                    status[makemove.toIndex()] = -1;
                } else if (makemove.isRightMove()) {
                    status[makemove.toIndex()] = 1;
                } else if (makemove.isJump()) {
                    status[makemove.jumpedIndex()] = 0;
                } else {
                    status[makemove.toIndex()] = 0;
                }
                status[makemove.fromIndex()] = 0;

                if (makemove.isJump()) {
                    set(makemove.jumpedIndex(), EMPTY);
                }
                set(makemove.toIndex(), get(makemove.fromIndex()));
                set(makemove.fromIndex(), EMPTY);

                makemove = makemove.jumpTail();
            }
            _whoseMove = whoseMove().opposite();
            if (!isMove()) {
                _gameOver = true;
                winner = _whoseMove.opposite();
            }
            allmoves.push(mov);

            setChanged();
            notifyObservers();
        }
    }


    /** Undo the last move, if any. */
    void undo() {

        _whoseMove = _whoseMove.opposite();
        Move undomove = allmoves.pop();

        int from = undomove.fromIndex();
        int to = undomove.toIndex();
        set(from, _whoseMove);

        if (undomove.isJump()) {
            while (undomove != null) {
                set(undomove.jumpedIndex(), _whoseMove.opposite());
                to = undomove.toIndex();
                undomove = undomove.jumpTail();
            }
        }
        set(to, EMPTY);

        if (_gameOver) {
            _gameOver = false;
        }

        setChanged();
        notifyObservers();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        for (int j = 4; j >= 0; j--) {
            out.format(" ");
            for (int i = j * 5; i < (j + 1) * 5; i++) {
                out.format(" ");
                out.format(newBoard[i].shortName());
            }
            if (j == 0) {
                break;
            }
            out.format("\n", newBoard[j]);
        }
        return out.toString();
    }


    /** Return true iff there is a move for the current player. */
    private boolean isMove() {
        for (Move m: getMoves()) {
            if (legalMove(m)) {
                return true;
            }
        }
        return false;
    }

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Set true when game ends. */
    private boolean _gameOver;

    /** Convenience value giving values of pieces at each ordinal position. */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /** One cannot create arrays of ArrayList<Move>, so we introduce
     *  a specialized private list type for this purpose. */
    private static class MoveList extends ArrayList<Move> {
    }

    /** newBoard of PieceColors. */
    private PieceColor[] newBoard = new PieceColor[SIDE * SIDE];

    /** Public method to return content of my newBoard. */
    public PieceColor[] getnewBoard() {
        return newBoard;
    }

    /** Array of piece values to ensure pieces don't
     * go back to where it came from horizontally. */
    private int[] status = new int[SIDE * SIDE];

    /** Stack to keep track of all moves. */
    private Stack<Move> allmoves = new Stack<Move>();

    /** Public method to return stack of moves. */
    public Stack<Move> getStack() {
        return allmoves;
    }

    /** Winner variable to keep track of winner. */
    private PieceColor winner = null;

    /** Public method to return winner variable. */
    public PieceColor getWinner() {
        return winner;
    }

    /** A read-only view of a Board. */
    private class ConstantBoard extends Board implements Observer {
        /** A constant view of this Board. */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /** Undo the last move. */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Board) {
            Board b = (Board) o;
            return (b.toString().equals(toString())
                    && _whoseMove == b.whoseMove()
                    && _gameOver == b.gameOver());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return newBoard.toString().hashCode();
    }
}



