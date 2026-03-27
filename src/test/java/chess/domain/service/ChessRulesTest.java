package chess.domain.service;

import chess.domain.model.*;
import chess.domain.piece.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ChessRulesTest {

    private ChessRules rules;

    @BeforeEach
    void setUp() {
        rules = new ChessRules();
    }

    // ── isInCheck ──────────────────────────────────────────────────────────────

    @Test
    void isInCheck_returnsFalse_whenKingIsSafe() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));

        assertThat(rules.isInCheck(Player.WHITE, board)).isFalse();
    }

    @Test
    void isInCheck_returnsTrue_whenRookAttacksKing() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        board.setPiece(new Position(5, 4), new Rook(Player.BLACK)); // same file as white king

        assertThat(rules.isInCheck(Player.WHITE, board)).isTrue();
    }

    @Test
    void isInCheck_returnsTrue_whenQueenAttacksKingDiagonally() {
        Board board = new Board();
        board.setPiece(new Position(0, 0), new King(Player.WHITE));
        board.setPiece(new Position(7, 7), new King(Player.BLACK));
        board.setPiece(new Position(3, 3), new Queen(Player.BLACK)); // diagonal attack

        assertThat(rules.isInCheck(Player.WHITE, board)).isTrue();
    }

    @Test
    void isInCheck_returnsFalse_whenPieceBetweenKingAndAttacker() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        board.setPiece(new Position(5, 4), new Rook(Player.BLACK));
        board.setPiece(new Position(3, 4), new Pawn(Player.WHITE)); // blocker

        assertThat(rules.isInCheck(Player.WHITE, board)).isFalse();
    }

    // ── isLegalMove ────────────────────────────────────────────────────────────

    @Test
    void isLegalMove_returnsFalse_whenSquareEmpty() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));

        assertThat(rules.isLegalMove(new Position(3, 3), new Position(4, 3), Player.WHITE, board)).isFalse();
    }

    @Test
    void isLegalMove_returnsFalse_whenMovingOpponentPiece() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        board.setPiece(new Position(6, 0), new Pawn(Player.BLACK));

        assertThat(rules.isLegalMove(new Position(6, 0), new Position(5, 0), Player.WHITE, board)).isFalse();
    }

    @Test
    void isLegalMove_returnsTrue_forValidPawnAdvance() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        board.setPiece(new Position(1, 0), new Pawn(Player.WHITE));

        assertThat(rules.isLegalMove(new Position(1, 0), new Position(2, 0), Player.WHITE, board)).isTrue();
    }

    @Test
    void isLegalMove_returnsFalse_whenMoveExposesKingToCheck() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        // White rook is pinned — moving it exposes the king to the black rook
        board.setPiece(new Position(0, 0), new Rook(Player.WHITE)); // pinned rook
        board.setPiece(new Position(0, 7), new Rook(Player.BLACK)); // attacker on same rank

        // Moving the white rook off the first rank would expose white king
        assertThat(rules.isLegalMove(new Position(0, 0), new Position(1, 0), Player.WHITE, board)).isFalse();
    }

    // ── isCheckmate ────────────────────────────────────────────────────────────

    @Test
    void isCheckmate_returnsFalse_whenNotInCheck() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));

        assertThat(rules.isCheckmate(Player.WHITE, board)).isFalse();
    }

    @Test
    void isCheckmate_returnsTrue_forFoolsMatePosition() {
        // Fool's mate final position after: 1.f3 e5 2.g4 Qh4#
        // Black queen on h4 attacks king on e1 diagonally via g3-f2 (both empty).
        // King cannot escape: d1/f1 blocked by own pieces, d2/e2 blocked by pawns, f2 attacked by queen.
        Board board = new Board();
        // White pieces
        board.setPiece(new Position(0, 0), new Rook(Player.WHITE));
        board.setPiece(new Position(0, 1), new Knight(Player.WHITE));
        board.setPiece(new Position(0, 2), new Bishop(Player.WHITE));
        board.setPiece(new Position(0, 3), new Queen(Player.WHITE));
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(0, 5), new Bishop(Player.WHITE));
        board.setPiece(new Position(0, 6), new Knight(Player.WHITE));
        board.setPiece(new Position(0, 7), new Rook(Player.WHITE));
        // White pawns: a2-e2, h2 unmoved; f pawn moved to f3, g pawn moved to g4
        board.setPiece(new Position(1, 0), new Pawn(Player.WHITE)); // a2
        board.setPiece(new Position(1, 1), new Pawn(Player.WHITE)); // b2
        board.setPiece(new Position(1, 2), new Pawn(Player.WHITE)); // c2
        board.setPiece(new Position(1, 3), new Pawn(Player.WHITE)); // d2
        board.setPiece(new Position(1, 4), new Pawn(Player.WHITE)); // e2
        board.setPiece(new Position(2, 5), new Pawn(Player.WHITE)); // f3 (moved from f2)
        board.setPiece(new Position(3, 6), new Pawn(Player.WHITE)); // g4 (moved from g2)
        board.setPiece(new Position(1, 7), new Pawn(Player.WHITE)); // h2
        // Black queen at h4 (row=3, col=7) delivers checkmate
        board.setPiece(new Position(3, 7), new Queen(Player.BLACK));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));

        assertThat(rules.isInCheck(Player.WHITE, board)).isTrue();
        assertThat(rules.isCheckmate(Player.WHITE, board)).isTrue();
    }

    // ── isStalemate ────────────────────────────────────────────────────────────

    @Test
    void isStalemate_returnsTrue_forSimpleStalematePosition() {
        Board board = new Board();
        // Black king in corner with no legal moves but not in check
        board.setPiece(new Position(7, 7), new King(Player.BLACK));
        board.setPiece(new Position(5, 6), new King(Player.WHITE));
        board.setPiece(new Position(6, 5), new Queen(Player.WHITE)); // controls all squares around black king

        assertThat(rules.isInCheck(Player.BLACK, board)).isFalse();
        assertThat(rules.isStalemate(Player.BLACK, board)).isTrue();
    }

    @Test
    void isStalemate_returnsFalse_whenPlayerHasMoves() {
        Board board = new Board();
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));

        assertThat(rules.isStalemate(Player.WHITE, board)).isFalse();
    }
}
