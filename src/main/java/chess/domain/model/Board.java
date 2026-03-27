package chess.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a chess board with configurable dimensions.
 * Supports non-standard board sizes for future extensions.
 */
public class Board {

    private final int rows;
    private final int cols;
    private final Map<Position, Square> squares;
    private Position enPassantTarget; // square a pawn can capture en passant; null if not available

    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.squares = new HashMap<>();
        initializeEmptyBoard();
    }

    /**
     * Create a standard 8x8 chess board.
     */
    public Board() {
        this(8, 8);
    }

    private void initializeEmptyBoard() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Position pos = new Position(row, col);
                squares.put(pos, new Square(pos));
            }
        }
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    /**
     * Get a square at the given position.
     */
    public Optional<Square> getSquare(Position position) {
        return Optional.ofNullable(squares.get(position));
    }

    /**
     * Get a square at the given row and column.
     */
    public Optional<Square> getSquare(int row, int col) {
        return getSquare(new Position(row, col));
    }

    /**
     * Get the piece at the given position.
     */
    public Optional<Piece> getPiece(Position position) {
        return getSquare(position).map(Square::piece);
    }

    /**
     * Set a piece at the given position.
     */
    public void setPiece(Position position, Piece piece) {
        Square square = squares.get(position);
        if (square != null) {
            square.setPiece(piece);
        }
    }

    /**
     * Remove a piece from the given position.
     */
    public void removePiece(Position position) {
        Square square = squares.get(position);
        if (square != null) {
            square.removePiece();
        }
    }

    /**
     * Move a piece from one position to another.
     */
    public void movePiece(Position from, Position to) {
        Optional<Piece> piece = getPiece(from);
        if (piece.isPresent()) {
            removePiece(from);
            setPiece(to, piece.get());
        }
    }

    /**
     * Check if a position is within board bounds.
     */
    public boolean isValidPosition(Position position) {
        return position.isWithin(rows, cols);
    }

    /**
     * Check if a position is empty.
     */
    public boolean isEmpty(Position position) {
        return getSquare(position).map(Square::isEmpty).orElse(false);
    }

    /**
     * Check if a position contains a piece of the given player.
     */
    public boolean hasPieceOf(Position position, Player player) {
        return getSquare(position).map(square -> square.hasPieceOf(player)).orElse(false);
    }

    /**
     * Check if a position contains an enemy piece of the given player.
     */
    public boolean hasEnemyPieceOf(Position position, Player player) {
        return getSquare(position).map(square -> square.hasEnemyPieceOf(player)).orElse(false);
    }

    /**
     * Get all pieces of a given player.
     */
    public Map<Position, Piece> getPiecesOf(Player player) {
        Map<Position, Piece> result = new HashMap<>();
        for (Map.Entry<Position, Square> entry : squares.entrySet()) {
            Piece piece = entry.getValue().piece();
            if (piece != null && piece.color() == player) {
                result.put(entry.getKey(), piece);
            }
        }
        return result;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(Position enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    /**
     * Get the king position for a player.
     */
    public Optional<Position> getKingPosition(Player player) {
        for (Map.Entry<Position, Square> entry : squares.entrySet()) {
            Piece piece = entry.getValue().piece();
            if (piece != null && piece.color() == player && piece.type() == PieceType.KING) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int row = rows - 1; row >= 0; row--) {
            sb.append(row + 1).append(" ");
            for (int col = 0; col < cols; col++) {
                Square square = squares.get(new Position(row, col));
                sb.append(square).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}