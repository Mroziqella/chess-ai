package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Knight — jumps in an L-shape: two squares in one direction, one square perpendicular.
 * The only piece that can leap over other pieces.
 */
public class Knight extends Piece {

    private static final int[][] JUMPS = {
        {-2, -1}, {-2,  1},
        {-1, -2}, {-1,  2},
        { 1, -2}, { 1,  2},
        { 2, -1}, { 2,  1}
    };

    public Knight(Player color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.KNIGHT;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        return Arrays.stream(JUMPS)
                .map(jump -> from.move(jump[0], jump[1]))
                .filter(board::isValidPosition)
                .filter(to -> board.isEmpty(to) || board.hasEnemyPieceOf(to, color()))
                .collect(Collectors.toList());
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♘" : "♞";
    }
}
