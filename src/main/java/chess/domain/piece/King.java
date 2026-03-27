package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * King piece - moves one square in any direction.
 */
public class King extends Piece {

    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},          {0, 1},
        {1, -1},  {1, 0},  {1, 1}
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
        List<Position> moves = new ArrayList<>();

        for (int[] dir : DIRECTIONS) {
            Position to = from.move(dir[0], dir[1]);
            if (board.isValidPosition(to)) {
                if (board.isEmpty(to) || board.hasEnemyPieceOf(to, color())) {
                    moves.add(to);
                }
            }
        }

        return moves;
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♔" : "♚";
    }
}