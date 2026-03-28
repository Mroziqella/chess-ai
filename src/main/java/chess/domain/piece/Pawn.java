package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pawn — advances forward, captures diagonally, and has three special moves:
 * initial double push, en passant capture, and promotion on the last rank.
 */
public class Pawn extends Piece {

    private final int advanceDirection; // +1 for white (up the board), -1 for black (down)
    private final int homeRow;          // row from which the double push is allowed

    public Pawn(Player color) {
        super(color);
        this.advanceDirection = (color == Player.WHITE) ? 1 : -1;
        this.homeRow = (color == Player.WHITE) ? 1 : 6;
    }

    @Override
    public PieceType type() {
        return PieceType.PAWN;
    }

    @Override
    public List<Position> getLegalMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>();
        moves.addAll(forwardMoves(from, board));
        moves.addAll(diagonalCaptures(from, board));
        enPassantCapture(from, board).ifPresent(moves::add);
        return moves;
    }

    private List<Position> forwardMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>();
        Position oneForward = from.move(advanceDirection, 0);

        if (!board.isValidPosition(oneForward) || !board.isEmpty(oneForward)) {
            return moves; // path blocked
        }
        moves.add(oneForward);

        if (isOnHomeRow(from)) {
            Position twoForward = from.move(advanceDirection * 2, 0);
            if (board.isValidPosition(twoForward) && board.isEmpty(twoForward)) {
                moves.add(twoForward);
            }
        }

        return moves;
    }

    private List<Position> diagonalCaptures(Position from, Board board) {
        return List.of(
                from.move(advanceDirection, -1),
                from.move(advanceDirection, +1)
        ).stream()
                .filter(board::isValidPosition)
                .filter(pos -> board.hasEnemyPieceOf(pos, color()))
                .collect(Collectors.toList());
    }

    private Optional<Position> enPassantCapture(Position from, Board board) {
        Position epTarget = board.getEnPassantTarget();
        if (epTarget == null) return Optional.empty();

        boolean isOneRowForward = epTarget.row() == from.row() + advanceDirection;
        boolean isAdjacentFile   = Math.abs(epTarget.col() - from.col()) == 1;

        return (isOneRowForward && isAdjacentFile) ? Optional.of(epTarget) : Optional.empty();
    }

    private boolean isOnHomeRow(Position pos) {
        return pos.row() == homeRow;
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♙" : "♟";
    }
}
