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
 * King — moves exactly one square in any direction.
 */
public class King extends Piece {

    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},          { 0,  1},
        { 1, -1}, { 1, 0}, { 1,  1}
    };

    public King(Player color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.KING;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        return Arrays.stream(DIRECTIONS)
                .map(dir -> from.move(dir[0], dir[1]))
                .filter(board::isValidPosition)
                .filter(to -> board.isEmpty(to) || board.hasEnemyPieceOf(to, color()))
                .collect(Collectors.toList());
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♔" : "♚";
    }
}
