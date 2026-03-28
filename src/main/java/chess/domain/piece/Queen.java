package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.List;

/**
 * Queen — combines the Rook and Bishop: slides in all eight directions.
 */
public class Queen extends SlidingPiece {

    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},          { 0,  1},
        { 1, -1}, { 1, 0}, { 1,  1}
    };

    public Queen(Player color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.QUEEN;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        return slidingMoves(from, board, DIRECTIONS);
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♕" : "♛";
    }
}
