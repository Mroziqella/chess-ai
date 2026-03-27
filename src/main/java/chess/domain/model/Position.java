package chess.domain.model;

/**
 * Represents a position on the chess board using row and column coordinates.
 * Row 0 is at the bottom, column 0 is at the left (from white's perspective).
 */
public record Position(int row, int col) {

    public Position {
        // No constructor validation - positions can temporarily be out of bounds during
        // piece move calculations. Board.isValidPosition() is the authoritative bounds check.
    }

    /**
     * Check if position is within given board dimensions.
     */
    public boolean isWithin(int rows, int cols) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /**
     * Move position by delta row and delta col.
     */
    public Position move(int deltaRow, int deltaCol) {
        return new Position(row + deltaRow, col + deltaCol);
    }

    /**
     * Convert from standard chess notation (e.g., "e4" -> Position(4, 4))
     */
    public static Position fromAlgebraic(String notation) {
        if (notation == null || notation.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + notation);
        }
        char file = notation.charAt(0); // a-h
        char rank = notation.charAt(1); // 1-8

        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            throw new IllegalArgumentException("Invalid algebraic notation: " + notation);
        }

        int col = file - 'a';
        int row = rank - '1';

        return new Position(row, col);
    }

    /**
     * Convert to algebraic notation (e.g., Position(4, 4) -> "e4")
     */
    public String toAlgebraic() {
        if (!isWithin(8, 8)) {
            return row + "," + col;
        }
        char file = (char) ('a' + col);
        char rank = (char) ('1' + row);
        return "" + file + rank;
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}