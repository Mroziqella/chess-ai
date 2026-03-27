package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Knight piece - moves in an L-shape: two squares in one direction + one square perpendicular.
 */
public class Knight extends Piece {

    private static final int[][] MOVES = {
        {-2, -1}, {-2, 1},
        {-1, -2}, {-1, 2},
        {1, -2},  {1, 2},
        {2, -1},  {2, 1}
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
        List<Position> moves = new ArrayList<>();

        for (int[] move : MOVES) {
            Position to = from.move(move[0], move[1]);
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
        return color() == Player.WHITE ? "♘" : "♞";
    }
}