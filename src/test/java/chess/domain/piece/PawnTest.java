package chess.domain.piece;

import chess.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PawnTest {

    @Test
    void whitePawn_movesForward() {
        Board board = new Board();
        board.setPiece(new Position(3, 4), new Pawn(Player.WHITE));

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(3, 4), board);

        assertThat(moves).contains(new Position(4, 4));
    }

    @Test
    void whitePawn_canMoveTwoSquaresFromStart() {
        Board board = new Board();
        board.setPiece(new Position(1, 0), new Pawn(Player.WHITE));

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(1, 0), board);

        assertThat(moves).contains(new Position(2, 0), new Position(3, 0));
    }

    @Test
    void whitePawn_cannotMoveTwoSquaresWhenNotOnStartRow() {
        Board board = new Board();
        board.setPiece(new Position(3, 0), new Pawn(Player.WHITE));

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(3, 0), board);

        assertThat(moves).doesNotContain(new Position(5, 0));
        assertThat(moves).hasSize(1);
    }

    @Test
    void whitePawn_blockedByPieceInFront() {
        Board board = new Board();
        board.setPiece(new Position(1, 0), new Pawn(Player.WHITE));
        board.setPiece(new Position(2, 0), new Pawn(Player.BLACK)); // blocker

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(1, 0), board);

        assertThat(moves).isEmpty();
    }

    @Test
    void whitePawn_capturesDiagonally() {
        Board board = new Board();
        board.setPiece(new Position(3, 3), new Pawn(Player.WHITE));
        board.setPiece(new Position(4, 2), new Pawn(Player.BLACK)); // capturable left
        board.setPiece(new Position(4, 4), new Pawn(Player.BLACK)); // capturable right

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(3, 3), board);

        assertThat(moves).contains(new Position(4, 2), new Position(4, 4));
    }

    @Test
    void whitePawn_cannotCaptureOwnPiece() {
        Board board = new Board();
        board.setPiece(new Position(3, 3), new Pawn(Player.WHITE));
        board.setPiece(new Position(4, 4), new Pawn(Player.WHITE)); // own piece

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(3, 3), board);

        assertThat(moves).doesNotContain(new Position(4, 4));
    }

    @Test
    void blackPawn_movesDownward() {
        Board board = new Board();
        board.setPiece(new Position(4, 4), new Pawn(Player.BLACK));

        List<Position> moves = new Pawn(Player.BLACK).getLegalMoves(new Position(4, 4), board);

        assertThat(moves).contains(new Position(3, 4));
        assertThat(moves).doesNotContain(new Position(5, 4));
    }

    @Test
    void blackPawn_canMoveTwoSquaresFromStart() {
        Board board = new Board();
        board.setPiece(new Position(6, 0), new Pawn(Player.BLACK));

        List<Position> moves = new Pawn(Player.BLACK).getLegalMoves(new Position(6, 0), board);

        assertThat(moves).contains(new Position(5, 0), new Position(4, 0));
    }

    @Test
    void pawn_atColumnA_doesNotThrowWhenCheckingLeftCapture() {
        // Regression: Position constructor used to throw for negative column
        Board board = new Board();
        board.setPiece(new Position(3, 0), new Pawn(Player.WHITE));

        assertThatNoException().isThrownBy(() ->
                new Pawn(Player.WHITE).getLegalMoves(new Position(3, 0), board));
    }

    @Test
    void pawn_atColumnH_doesNotThrowWhenCheckingRightCapture() {
        Board board = new Board();
        board.setPiece(new Position(3, 7), new Pawn(Player.WHITE));

        assertThatNoException().isThrownBy(() ->
                new Pawn(Player.WHITE).getLegalMoves(new Position(3, 7), board));
    }

    @Test
    void whitePawn_enPassant_includesTargetSquare() {
        Board board = new Board();
        // White pawn on e5 (row=4, col=4), black pawn just moved d7->d5 -> ep target d6 (row=5, col=3)
        board.setPiece(new Position(4, 4), new Pawn(Player.WHITE));
        board.setPiece(new Position(4, 3), new Pawn(Player.BLACK));
        board.setEnPassantTarget(new Position(5, 3));

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(4, 4), board);

        assertThat(moves).contains(new Position(5, 3));
    }

    @Test
    void blackPawn_enPassant_includesTargetSquare() {
        Board board = new Board();
        // Black pawn on d4 (row=3, col=3), white pawn just moved e2->e4 -> ep target e3 (row=2, col=4)
        board.setPiece(new Position(3, 3), new Pawn(Player.BLACK));
        board.setPiece(new Position(3, 4), new Pawn(Player.WHITE));
        board.setEnPassantTarget(new Position(2, 4));

        List<Position> moves = new Pawn(Player.BLACK).getLegalMoves(new Position(3, 3), board);

        assertThat(moves).contains(new Position(2, 4));
    }

    @Test
    void pawn_noEnPassant_whenTargetIsNull() {
        Board board = new Board();
        board.setPiece(new Position(4, 4), new Pawn(Player.WHITE));
        board.setPiece(new Position(4, 3), new Pawn(Player.BLACK));
        // enPassantTarget is null (default)

        List<Position> moves = new Pawn(Player.WHITE).getLegalMoves(new Position(4, 4), board);

        assertThat(moves).doesNotContain(new Position(5, 3));
    }
}
