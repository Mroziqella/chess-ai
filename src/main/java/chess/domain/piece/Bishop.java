package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.List;

/**
 * Bishop — slides diagonally any number of squares.
 */
public class Bishop extends SlidingPiece {

    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 1},
        { 1, -1}, { 1, 1}
    };

    public Bishop(Player color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.BISHOP;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        return slidingMoves(from, board, DIRECTIONS);
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♗" : "♝";
    }
}
