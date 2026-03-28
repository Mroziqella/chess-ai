package chess.domain.service;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;
import chess.domain.piece.Bishop;
import chess.domain.piece.King;
import chess.domain.piece.Knight;
import chess.domain.piece.Pawn;
import chess.domain.piece.Queen;
import chess.domain.piece.Rook;
import org.springframework.stereotype.Service;

/**
 * Domain service that encapsulates the rules of chess:
 * move legality, check, checkmate, stalemate, and en passant.
 */
@Service
public class ChessRules {

    /**
     * Return true if the player is allowed to move the piece from 'from' to 'to'.
     * A move is legal when: the piece belongs to the player, the piece can reach
     * the target square by its movement rules, and the move does not leave the
     * player's own king in check.
     */
    public boolean isLegalMove(Position from, Position to, Player player, Board board) {
        return board.getPiece(from)
                .filter(piece -> piece.color() == player)
                .filter(piece -> piece.canMoveTo(from, to, board))
                .map(piece -> !wouldLeaveOwnKingInCheck(from, to, player, board))
                .orElse(false);
    }

    /**
     * Return true if the given player's king is currently under attack.
     */
    public boolean isInCheck(Player player, Board board) {
        return board.getKingPosition(player)
                .map(kingPosition -> isKingUnderAttack(kingPosition, player.opponent(), board))
                .orElse(false);
    }

    /**
     * Return true if the player is in checkmate: the king is in check
     * and no legal move can escape it.
     */
    public boolean isCheckmate(Player player, Board board) {
        return isInCheck(player, board) && hasNoLegalMove(player, board);
    }

    /**
     * Return true if the player is in stalemate: not in check,
     * but every move would place the king in check.
     */
    public boolean isStalemate(Player player, Board board) {
        return !isInCheck(player, board) && hasNoLegalMove(player, board);
    }

    /**
     * Return true if moving from 'from' to 'to' is an en passant capture.
     * Used both in move validation and in executing the side-effect (removing
     * the captured pawn from its actual square).
     */
    public boolean isEnPassantCapture(Position from, Position to, Board board) {
        Position epTarget = board.getEnPassantTarget();
        return epTarget != null
                && epTarget.equals(to)
                && board.getPiece(from).map(p -> p.type() == PieceType.PAWN).orElse(false);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isKingUnderAttack(Position kingPosition, Player attacker, Board board) {
        return board.getPiecesOf(attacker).entrySet().stream()
                .anyMatch(e -> e.getValue().canMoveTo(e.getKey(), kingPosition, board));
    }

    private boolean hasNoLegalMove(Player player, Board board) {
        return board.getPiecesOf(player).entrySet().stream()
                .noneMatch(e -> pieceHasAnyLegalMove(e.getKey(), e.getValue(), player, board));
    }

    private boolean pieceHasAnyLegalMove(Position from, Piece piece, Player player, Board board) {
        return piece.getLegalMoves(from, board).stream()
                .anyMatch(to -> isLegalMove(from, to, player, board));
    }

    private boolean wouldLeaveOwnKingInCheck(Position from, Position to, Player player, Board board) {
        Board simulated = copyBoard(board);
        simulated.movePiece(from, to);

        if (isEnPassantCapture(from, to, board)) {
            removeCapturedEnPassantPawn(to, player, simulated);
        }

        return isInCheck(player, simulated);
    }

    private void removeCapturedEnPassantPawn(Position landingSquare, Player capturingPlayer, Board board) {
        int pawnAdvanceDirection = capturingPlayer == Player.WHITE ? 1 : -1;
        board.removePiece(new Position(landingSquare.row() - pawnAdvanceDirection, landingSquare.col()));
    }

    private Board copyBoard(Board original) {
        Board copy = new Board(original.rows(), original.cols());
        original.allPieces().forEach((pos, piece) -> copy.setPiece(pos, createPieceCopy(piece)));
        copy.setEnPassantTarget(original.getEnPassantTarget());
        return copy;
    }

    private Piece createPieceCopy(Piece piece) {
        return switch (piece.type()) {
            case KING   -> new King(piece.color());
            case QUEEN  -> new Queen(piece.color());
            case ROOK   -> new Rook(piece.color());
            case BISHOP -> new Bishop(piece.color());
            case KNIGHT -> new Knight(piece.color());
            case PAWN   -> new Pawn(piece.color());
        };
    }
}
