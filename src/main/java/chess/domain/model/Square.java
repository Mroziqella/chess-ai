package chess.domain.model;

/**
 * Represents a square on the chess board, containing an optional piece.
 */
public class Square {

    private final Position position;
    private Piece piece;

    public Square(Position position) {
        this(position, null);
    }

    public Square(Position position, Piece piece) {
        this.position = position;
        this.piece = piece;
    }

    public Position position() {
        return position;
    }

    public Piece piece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public void removePiece() {
        this.piece = null;
    }

    public boolean isEmpty() {
        return piece == null;
    }

    public boolean hasPieceOf(Player player) {
        return piece != null && piece.color() == player;
    }

    public boolean hasEnemyPieceOf(Player player) {
        return piece != null && piece.color() != player;
    }

    @Override
    public String toString() {
        if (piece == null) {
            return ".";
        }
        return piece.toString();
    }
}