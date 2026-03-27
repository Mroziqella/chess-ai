package chess.application;

import chess.application.dto.GameState;
import chess.application.dto.MoveRequest;
import chess.domain.model.Board;
import chess.domain.model.Player;
import chess.domain.model.Position;
import chess.domain.piece.*;
import chess.domain.service.ChessRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(new ChessRules());
    }

    @Test
    void getGameState_returnsInitialBoardWithPieces() {
        GameState state = gameService.getGameState("test");

        // Board should have pieces, not be empty
        var squares = state.board().squares();
        // White pawns on rank 2 (display row 6 = squares index 6)
        assertThat(squares.get(6)).allMatch(s -> !s.isEmpty() || squares.get(6).indexOf(s) >= 0);

        // White pieces on rank 1 (display row 7)
        long whitePiecesCount = squares.get(7).stream().filter(s -> !s.isEmpty()).count();
        assertThat(whitePiecesCount).isEqualTo(8);

        // Black pieces on rank 8 (display row 0)
        long blackPiecesCount = squares.get(0).stream().filter(s -> !s.isEmpty()).count();
        assertThat(blackPiecesCount).isEqualTo(8);
    }

    @Test
    void getGameState_initialPlayerIsWhite() {
        GameState state = gameService.getGameState("test");

        assertThat(state.currentPlayer().name()).isEqualTo("WHITE");
    }

    @Test
    void getGameState_initialStatusIsInProgress() {
        GameState state = gameService.getGameState("test");

        assertThat(state.status()).isEqualTo(GameState.GameStatus.IN_PROGRESS);
    }

    @Test
    void getGameState_initialLegalMovesAreNonEmpty() {
        GameState state = gameService.getGameState("test");

        // White has 20 legal opening moves (16 pawn + 4 knight)
        assertThat(state.legalMoves()).hasSize(20);
    }

    @Test
    void makeMove_validMove_updatesBoard() {
        MoveRequest request = new MoveRequest("e2", "e4");
        GameState state = gameService.makeMove("test", request);

        // After e2-e4, it's black's turn
        assertThat(state.currentPlayer().name()).isEqualTo("BLACK");

        // e4 square (display row 4, col 4) should have a pawn
        assertThat(state.board().squares().get(4).get(4)).isEqualTo("♙");
        // e2 square (display row 6, col 4) should be empty
        assertThat(state.board().squares().get(6).get(4)).isEmpty();
    }

    @Test
    void makeMove_illegalMove_throwsException() {
        // Pawn cannot move backwards
        MoveRequest request = new MoveRequest("e2", "e1");

        assertThatThrownBy(() -> gameService.makeMove("test", request))
                .isInstanceOf(GameService.IllegalMoveException.class);
    }

    @Test
    void makeMove_movingOpponentPiece_throwsException() {
        // White cannot move black's pawn
        MoveRequest request = new MoveRequest("e7", "e5");

        assertThatThrownBy(() -> gameService.makeMove("test", request))
                .isInstanceOf(GameService.IllegalMoveException.class);
    }

    @Test
    void resetGame_restoresInitialState() {
        // Make a move first
        gameService.makeMove("test", new MoveRequest("e2", "e4"));

        // Reset
        GameState state = gameService.resetGame("test");

        assertThat(state.currentPlayer().name()).isEqualTo("WHITE");
        assertThat(state.status()).isEqualTo(GameState.GameStatus.IN_PROGRESS);
        assertThat(state.legalMoves()).hasSize(20);
        // e2 should have white pawn again
        assertThat(state.board().squares().get(6).get(4)).isEqualTo("♙");
    }

    @Test
    void makeMove_pawnPromotion_defaultsToQueen() {
        Board board = new Board();
        board.setPiece(new Position(6, 4), new Pawn(Player.WHITE)); // e7, promoting to e8
        board.setPiece(new Position(0, 0), new King(Player.WHITE)); // a1
        board.setPiece(new Position(7, 7), new King(Player.BLACK)); // h8 (away from e8)
        gameService.setupPositionForTesting("promo-default", board, Player.WHITE);

        GameState state = gameService.makeMove("promo-default", new MoveRequest("e7", "e8"));

        // e8 = display row 0, col 4
        assertThat(state.board().squares().get(0).get(4)).isEqualTo("♕");
    }

    @Test
    void makeMove_pawnPromotion_toRook() {
        Board board = new Board();
        board.setPiece(new Position(6, 4), new Pawn(Player.WHITE));
        board.setPiece(new Position(0, 0), new King(Player.WHITE));
        board.setPiece(new Position(7, 7), new King(Player.BLACK));
        gameService.setupPositionForTesting("promo-rook", board, Player.WHITE);

        GameState state = gameService.makeMove("promo-rook", new MoveRequest("e7", "e8", "ROOK"));

        assertThat(state.board().squares().get(0).get(4)).isEqualTo("♖");
    }

    @Test
    void makeMove_pawnPromotion_toKnight() {
        Board board = new Board();
        board.setPiece(new Position(6, 4), new Pawn(Player.WHITE));
        board.setPiece(new Position(0, 0), new King(Player.WHITE));
        board.setPiece(new Position(7, 7), new King(Player.BLACK));
        gameService.setupPositionForTesting("promo-knight", board, Player.WHITE);

        GameState state = gameService.makeMove("promo-knight", new MoveRequest("e7", "e8", "KNIGHT"));

        assertThat(state.board().squares().get(0).get(4)).isEqualTo("♘");
    }

    @Test
    void makeMove_blackPawnPromotion_toBishop() {
        Board board = new Board();
        board.setPiece(new Position(1, 3), new Pawn(Player.BLACK)); // d2
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        gameService.setupPositionForTesting("promo-bishop-black", board, Player.BLACK);

        GameState state = gameService.makeMove("promo-bishop-black", new MoveRequest("d2", "d1", "BISHOP"));

        // d1 = backend row 0 = display row 7, col 3
        assertThat(state.board().squares().get(7).get(3)).isEqualTo("♝");
    }

    @Test
    void multipleGames_areIndependent() {
        gameService.makeMove("game1", new MoveRequest("e2", "e4"));

        GameState game1 = gameService.getGameState("game1");
        GameState game2 = gameService.getGameState("game2");

        assertThat(game1.currentPlayer().name()).isEqualTo("BLACK");
        assertThat(game2.currentPlayer().name()).isEqualTo("WHITE");
    }

    @Test
    void makeMove_enPassantCapture_removesOpponentPawn() {
        // 1.e2-e4 (white), a7-a6 (black), e4-e5 (white), d7-d5 (black double move) -> ep target d6
        // 5. e5xd6 (white en passant capture)
        gameService.makeMove("ep1", new MoveRequest("e2", "e4"));
        gameService.makeMove("ep1", new MoveRequest("a7", "a6"));
        gameService.makeMove("ep1", new MoveRequest("e4", "e5"));
        gameService.makeMove("ep1", new MoveRequest("d7", "d5")); // double move -> ep target d6

        GameState state = gameService.makeMove("ep1", new MoveRequest("e5", "d6"));

        // Black pawn that was at d5 (backend row4 -> display row 3, col 3) must be gone
        assertThat(state.board().squares().get(3).get(3)).isEmpty();
        // White pawn now at d6 (backend row5 -> display row 2, col 3)
        assertThat(state.board().squares().get(2).get(3)).isEqualTo("♙");
    }

    @Test
    void makeMove_enPassantNotAvailableAfterSubsequentMove() {
        // White skips the en passant opportunity — it must no longer be in legal moves
        gameService.makeMove("ep2", new MoveRequest("e2", "e4"));
        gameService.makeMove("ep2", new MoveRequest("a7", "a6"));
        gameService.makeMove("ep2", new MoveRequest("e4", "e5"));
        gameService.makeMove("ep2", new MoveRequest("d7", "d5")); // ep target d6 set
        gameService.makeMove("ep2", new MoveRequest("a2", "a3")); // white skips ep
        gameService.makeMove("ep2", new MoveRequest("a6", "a5")); // black plays something

        GameState state = gameService.getGameState("ep2");
        assertThat(state.legalMoves()).noneMatch(m -> m.endsWith("-d6"));
    }

    @Test
    void makeMove_enPassantIsAvailableImmediatelyAfterDoubleMove() {
        gameService.makeMove("ep3", new MoveRequest("e2", "e4"));
        gameService.makeMove("ep3", new MoveRequest("a7", "a6"));
        gameService.makeMove("ep3", new MoveRequest("e4", "e5"));
        gameService.makeMove("ep3", new MoveRequest("d7", "d5")); // double move -> ep on d6

        GameState state = gameService.getGameState("ep3");
        assertThat(state.legalMoves()).anyMatch(m -> m.equals("e5-d6"));
    }
}
