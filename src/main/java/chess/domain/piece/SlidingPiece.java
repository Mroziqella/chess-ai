package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for pieces that slide along rays: Queen, Rook, Bishop.
 * A sliding piece moves any number of squares in its allowed directions,
 * stopping when it hits a piece (capturing if enemy, blocked if friendly).
 */
abstract class SlidingPiece extends Piece {

    protected SlidingPiece(Player color) {
        super(color);
    }

    protected List<Position> slidingMoves(Position from, Board board, int[][] directions) {
        return Arrays.stream(directions)
                .flatMap(dir -> slideUntilBlocked(from, dir[0], dir[1], board).stream())
                .collect(Collectors.toList());
    }

    private List<Position> slideUntilBlocked(Position from, int dRow, int dCol, Board board) {
        List<Position> moves = new ArrayList<>();
        Position current = from.move(dRow, dCol);

        while (board.isValidPosition(current)) {
            if (board.isEmpty(current)) {
                moves.add(current);
            } else if (board.hasEnemyPieceOf(current, color())) {
                moves.add(current); // capture and stop
                break;
            } else {
                break; // blocked by own piece
            }
            current = current.move(dRow, dCol);
        }

        return moves;
    }
}
