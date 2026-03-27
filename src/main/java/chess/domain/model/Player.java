package chess.domain.model;

/**
 * Represents a player in chess.
 */
public enum Player {
    WHITE,
    BLACK;

    /**
     * Get the opponent player.
     */
    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}