package chess.domain.piece;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.ArrayList;
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
        List<Position> moves = new ArrayList<>(Arrays.stream(DIRECTIONS)
                .map(dir -> from.move(dir[0], dir[1]))
                .filter(board::isValidPosition)
                .filter(to -> board.isEmpty(to) || board.hasEnemyPieceOf(to, color()))
                .collect(Collectors.toList()));

        // Kingside castling
        if (board.canCastleKingSide(color())) {
            Position f = new Position(from.row(), from.col() + 1);
            Position g = new Position(from.row(), from.col() + 2);
            Position rookPos = new Position(from.row(), 7);
            if (board.isValidPosition(f) && board.isEmpty(f)
                    && board.isValidPosition(g) && board.isEmpty(g)
                    && board.getPiece(rookPos)
                        .map(p -> p.type() == PieceType.ROOK && p.color() == color()).orElse(false)) {
                moves.add(g);
            }
        }

        // Queenside castling
        if (board.canCastleQueenSide(color())) {
            Position d = new Position(from.row(), from.col() - 1);
            Position c = new Position(from.row(), from.col() - 2);
            Position b = new Position(from.row(), from.col() - 3);
            Position rookPos = new Position(from.row(), 0);
            if (board.isValidPosition(d) && board.isEmpty(d)
                    && board.isValidPosition(c) && board.isEmpty(c)
                    && board.isValidPosition(b) && board.isEmpty(b)
                    && board.getPiece(rookPos)
                        .map(p -> p.type() == PieceType.ROOK && p.color() == color()).orElse(false)) {
                moves.add(c);
            }
        }

        return moves;
    }

    @Override
    public String getSymbol() {
        return color() == Player.WHITE ? "♔" : "♚";
    }
}
