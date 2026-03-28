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
                .map(piece -> {
                    if (isCastlingMove(from, to, piece)) {
                        // Cannot castle while in check
                        if (isInCheck(player, board)) return false;
                        // Intermediate square must not be attacked
                        Position intermediate = new Position(from.row(), (from.col() + to.col()) / 2);
                        if (isSquareAttacked(intermediate, player.opponent(), board)) return false;
                    }
                    return !wouldLeaveOwnKingInCheck(from, to, player, board);
                })
                .orElse(false);
    }

    /**
     * Return true if this move is a castling move (king moves 2 squares horizontally).
     */
    public boolean isCastlingMove(Position from, Position to, Piece piece) {
        return piece.type() == PieceType.KING
                && from.row() == to.row()
                && Math.abs(to.col() - from.col()) == 2;
    }

    /**
     * Return true if the given player's king is currently under attack.
     */
    public boolean isInCheck(Player player, Board board) {
        return board.getKingPosition(player)
                .map(kingPos -> isSquareAttacked(kingPos, player.opponent(), board))
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

    private boolean isSquareAttacked(Position square, Player attacker, Board board) {
        return board.getPiecesOf(attacker).entrySet().stream()
                .anyMatch(e -> e.getValue().canMoveTo(e.getKey(), square, board));
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

        // If castling, also move the rook in simulation
        if (board.getPiece(from).map(p -> isCastlingMove(from, to, p)).orElse(false)) {
            int rookFromCol = to.col() > from.col() ? 7 : 0;
            int rookToCol = to.col() > from.col() ? 5 : 3;
            simulated.movePiece(new Position(from.row(), rookFromCol), new Position(from.row(), rookToCol));
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
        copy.setCanCastleKingSide(Player.WHITE, original.canCastleKingSide(Player.WHITE));
        copy.setCanCastleQueenSide(Player.WHITE, original.canCastleQueenSide(Player.WHITE));
        copy.setCanCastleKingSide(Player.BLACK, original.canCastleKingSide(Player.BLACK));
        copy.setCanCastleQueenSide(Player.BLACK, original.canCastleQueenSide(Player.BLACK));
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
