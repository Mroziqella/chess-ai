package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Rook piece - moves any number of squares horizontally or vertically.
 */
public class Rook extends Piece {

    private static final int[][] DIRECTIONS = {
        {-1, 0},
        {0, -1}, {0, 1},
        {1, 0}
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
        List<Position> moves = new ArrayList<>();

        for (int[] dir : DIRECTIONS) {
            moves.addAll(slideInDirection(from, dir[0], dir[1], board));
        }

        return moves;
    }

    private List<Position> slideInDirection(Position from, int dRow, int dCol, Board board) {
        List<Position> moves = new ArrayList<>();
        Position current = from.move(dRow, dCol);

        while (board.isValidPosition(current)) {
            if (board.isEmpty(current)) {
                moves.add(current);
            } else if (board.hasEnemyPieceOf(current, color())) {
                moves.add(current);
                break;
            } else {
                break;
            }
            current = current.move(dRow, dCol);
        }

        return moves;
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♖" : "♜";
    }
}