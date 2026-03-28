package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.List;

/**
 * Rook — slides horizontally and vertically any number of squares.
 */
public class Rook extends SlidingPiece {

    private static final int[][] DIRECTIONS = {
        {-1,  0},
        { 0, -1}, { 0, 1},
        { 1,  0}
    };

    public Rook(Player color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.ROOK;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        return slidingMoves(from, board, DIRECTIONS);
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♖" : "♜";
    }
}
