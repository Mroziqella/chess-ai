package chess.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        initializeEmptySquares();
    }

    /**
     * Create a standard 8×8 chess board.
     */
    public Board() {
        this(8, 8);
    }

    private void initializeEmptySquares() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Position pos = new Position(row, col);
                squares.put(pos, new Square(pos));
            }
        }
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    /**
     * Get the piece at the given position.
     */
    public Optional<Piece> getPiece(Position position) {
        return getSquare(position).map(Square::piece);
    }

    /**
     * Place a piece on the given square.
     */
    public void setPiece(Position position, Piece piece) {
        Square square = squares.get(position);
        if (square != null) {
            square.setPiece(piece);
        }
    }

    /**
     * Remove the piece from the given square.
     */
    public void removePiece(Position position) {
        Square square = squares.get(position);
        if (square != null) {
            square.removePiece();
        }
    }

    /**
     * Move the piece from one square to another.
     */
    public void movePiece(Position from, Position to) {
        getPiece(from).ifPresent(piece -> {
            removePiece(from);
            setPiece(to, piece);
        });
    }

    /**
     * Check if a position is within board bounds.
     */
    public boolean isValidPosition(Position position) {
        return position.isWithin(rows, cols);
    }

    /**
     * Check if a square contains no piece.
     */
    public boolean isEmpty(Position position) {
        return getSquare(position).map(Square::isEmpty).orElse(false);
    }

    /**
     * Check if a square contains a piece belonging to the given player.
     */
    public boolean hasPieceOf(Position position, Player player) {
        return getSquare(position).map(square -> square.hasPieceOf(player)).orElse(false);
    }

    /**
     * Check if a square contains a piece belonging to the given player's opponent.
     */
    public boolean hasEnemyPieceOf(Position position, Player player) {
        return getSquare(position).map(square -> square.hasEnemyPieceOf(player)).orElse(false);
    }

    /**
     * Get all pieces belonging to the given player, keyed by their positions.
     */
    public Map<Position, Piece> getPiecesOf(Player player) {
        return squares.entrySet().stream()
                .filter(e -> e.getValue().hasPieceOf(player))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().piece()));
    }

    /**
     * Get all pieces on the board from both players, keyed by their positions.
     */
    public Map<Position, Piece> allPieces() {
        return squares.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().piece()));
    }

    /**
     * Find the king's position for the given player.
     */
    public Optional<Position> getKingPosition(Player player) {
        return squares.entrySet().stream()
                .filter(e -> e.getValue().hasPieceOf(player) && e.getValue().piece().type() == PieceType.KING)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(Position enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int row = rows - 1; row >= 0; row--) {
            sb.append(row + 1).append(" ");
            for (int col = 0; col < cols; col++) {
                sb.append(squares.get(new Position(row, col))).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private Optional<Square> getSquare(Position position) {
        return Optional.ofNullable(squares.get(position));
    }
}
