package chess.domain.piece;

import chess.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class KnightTest {

    @Test
    void knight_inCenterHasEightMoves() {
        Board board = new Board();
        board.setPiece(new Position(4, 4), new Knight(Player.WHITE));

        List<Position> moves = new Knight(Player.WHITE).getLegalMoves(new Position(4, 4), board);

        assertThat(moves).hasSize(8);
    }

    @Test
    void knight_inCornerHasTwoMoves() {
        Board board = new Board();
        board.setPiece(new Position(0, 0), new Knight(Player.WHITE));

        List<Position> moves = new Knight(Player.WHITE).getLegalMoves(new Position(0, 0), board);

        assertThat(moves).hasSize(2);
        assertThat(moves).contains(new Position(1, 2), new Position(2, 1));
    }

    @Test
    void knight_cannotCaptureOwnPiece() {
        Board board = new Board();
        board.setPiece(new Position(4, 4), new Knight(Player.WHITE));
        board.setPiece(new Position(6, 5), new Pawn(Player.WHITE)); // own piece at L-move target

        List<Position> moves = new Knight(Player.WHITE).getLegalMoves(new Position(4, 4), board);

        assertThat(moves).doesNotContain(new Position(6, 5));
    }

    @Test
    void knight_canCaptureEnemyPiece() {
        Board board = new Board();
        board.setPiece(new Position(4, 4), new Knight(Player.WHITE));
        board.setPiece(new Position(6, 5), new Pawn(Player.BLACK)); // enemy

        List<Position> moves = new Knight(Player.WHITE).getLegalMoves(new Position(4, 4), board);

        assertThat(moves).contains(new Position(6, 5));
    }

    @Test
    void knight_atEdge_doesNotThrow() {
        // Regression: Position constructor used to throw for negative coordinates
        Board board = new Board();
        board.setPiece(new Position(0, 0), new Knight(Player.WHITE));

        assertThatNoException().isThrownBy(() ->
                new Knight(Player.WHITE).getLegalMoves(new Position(0, 0), board));
    }
}
