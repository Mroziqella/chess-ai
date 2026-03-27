package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Pawn piece - moves forward (direction depends on color), captures diagonally.
 * Special rules: initial double move, en passant, promotion not implemented yet.
 */
public class Pawn extends Piece {

    private final int direction; // 1 for white (moves up), -1 for black (moves down)
    private final int startRow; // Starting row for this pawn

    public Pawn(Player color) {
        super(color);
        this.direction = (color == Player.WHITE) ? 1 : -1;
        this.startRow = (color == Player.WHITE) ? 1 : 6;
    }

    @Override
    public PieceType type() {
        return PieceType.PAWN;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>();

        // Single forward move
        Position forward = from.move(direction, 0);
        if (board.isValidPosition(forward) && board.isEmpty(forward)) {
            moves.add(forward);

            // Double forward move from starting position
            if (from.row() == startRow) {
                Position doubleForward = from.move(direction * 2, 0);
                if (board.isValidPosition(doubleForward) && board.isEmpty(doubleForward)) {
                    moves.add(doubleForward);
                }
            }
        }

        // Diagonal captures
        Position captureLeft = from.move(direction, -1);
        if (board.isValidPosition(captureLeft) && board.hasEnemyPieceOf(captureLeft, color())) {
            moves.add(captureLeft);
        }

        Position captureRight = from.move(direction, 1);
        if (board.isValidPosition(captureRight) && board.hasEnemyPieceOf(captureRight, color())) {
            moves.add(captureRight);
        }

        // En passant captures
        Position epTarget = board.getEnPassantTarget();
        if (epTarget != null && epTarget.row() == from.row() + direction) {
            int colDiff = epTarget.col() - from.col();
            if (colDiff == 1 || colDiff == -1) {
                moves.add(epTarget);
            }
        }

        return moves;
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♙" : "♟";
    }
}