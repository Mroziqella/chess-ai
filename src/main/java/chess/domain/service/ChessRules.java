package chess.domain.service;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.Player;
import chess.domain.model.Position;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for chess game rules: move validation, check/mate detection.
 */
@Service
public class ChessRules {

    /**
     * Check if a move is legal for the given player.
     */
    public boolean isLegalMove(Position from, Position to, Player player, Board board) {
        Optional<Piece> piece = board.getPiece(from);

        if (piece.isEmpty()) {
            return false;
        }

        if (piece.get().color() != player) {
            return false;
        }

        if (!piece.get().canMoveTo(from, to, board)) {
            return false;
        }

        // Check if move would leave king in check
        return !wouldBeInCheck(from, to, player, board);
    }

    /**
     * Check if the player's king is in check.
     */
    public boolean isInCheck(Player player, Board board) {
        Optional<Position> kingPos = board.getKingPosition(player);
        if (kingPos.isEmpty()) {
            return false;
        }

        Position king = kingPos.get();
        Player opponent = player.opponent();

        // Check if any opponent piece can attack the king's position
        for (Piece piece : board.getPiecesOf(opponent).values()) {
            Position piecePos = findPiecePosition(piece, board);
            if (piecePos != null && piece.canMoveTo(piecePos, king, board)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the player is in checkmate.
     */
    public boolean isCheckmate(Player player, Board board) {
        if (!isInCheck(player, board)) {
            return false;
        }

        // Check if any legal move exists
        return !hasAnyLegalMove(player, board);
    }

    /**
     * Check if the game is in stalemate.
     */
    public boolean isStalemate(Player player, Board board) {
        if (isInCheck(player, board)) {
            return false;
        }

        return !hasAnyLegalMove(player, board);
    }

    /**
     * Check if moving from 'from' to 'to' would result in the player's king being in check.
     */
    private boolean wouldBeInCheck(Position from, Position to, Player player, Board board) {
        Board tempBoard = copyBoard(board);
        tempBoard.movePiece(from, to);

        // For en passant: captured pawn is not at 'to' but one row behind it
        Position epTarget = board.getEnPassantTarget();
        if (epTarget != null && epTarget.equals(to)) {
            Optional<Piece> piece = board.getPiece(from);
            if (piece.isPresent() && piece.get().type() == chess.domain.model.PieceType.PAWN) {
                int direction = player == Player.WHITE ? 1 : -1;
                tempBoard.removePiece(new Position(to.row() - direction, to.col()));
            }
        }

        return isInCheck(player, tempBoard);
    }

    /**
     * Check if player has any legal move.
     */
    private boolean hasAnyLegalMove(Player player, Board board) {
        for (var entry : board.getPiecesOf(player).entrySet()) {
            Position from = entry.getKey();
            Piece piece = entry.getValue();

            List<Position> moves = piece.getLegalMoves(from, board);
            for (Position to : moves) {
                if (isLegalMove(from, to, player, board)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find the position of a piece on the board.
     */
    private Position findPiecePosition(Piece piece, Board board) {
        for (var entry : board.getPiecesOf(piece.color()).entrySet()) {
            if (entry.getValue() == piece) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Create a deep copy of the board.
     */
    private Board copyBoard(Board board) {
        Board copy = new Board(board.rows(), board.cols());

        for (int row = 0; row < board.rows(); row++) {
            for (int col = 0; col < board.cols(); col++) {
                Position pos = new Position(row, col);
                board.getPiece(pos).ifPresent(piece -> {
                    Piece copyPiece = createPieceCopy(piece);
                    copy.setPiece(pos, copyPiece);
                });
            }
        }

        copy.setEnPassantTarget(board.getEnPassantTarget());
        return copy;
    }

    private Piece createPieceCopy(Piece piece) {
        return switch (piece.type()) {
            case KING -> new chess.domain.piece.King(piece.color());
            case QUEEN -> new chess.domain.piece.Queen(piece.color());
            case ROOK -> new chess.domain.piece.Rook(piece.color());
            case BISHOP -> new chess.domain.piece.Bishop(piece.color());
            case KNIGHT -> new chess.domain.piece.Knight(piece.color());
            case PAWN -> new chess.domain.piece.Pawn(piece.color());
        };
    }
}