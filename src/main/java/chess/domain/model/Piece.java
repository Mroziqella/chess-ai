package chess.domain.model;

import java.util.List;

/**
 * Abstract representation of a chess piece.
 */
public abstract class Piece {

    private final Player color;

    protected Piece(Player color) {
        this.color = color;
    }

    public Player color() {
        return color;
    }

    /**
     * Get the type of this piece.
     */
    public abstract PieceType type();

    /**
     * Get all legal moves for this piece from the given position.
     * Does not check for pin or check conditions - only raw movement rules.
     */
    public abstract List<Position> getLegalMoves(Position from, Board board);

    /**
     * Check if this piece can move to the target position (raw rules).
     */
    public boolean canMoveTo(Position from, Position to, Board board) {
        return getLegalMoves(from, board).contains(to);
    }

    /**
     * Get the symbol for display.
     */
    public abstract String getSymbol();

    @Override
    public String toString() {
        return getSymbol();
    }
}