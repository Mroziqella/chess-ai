package chess.infrastructure.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── GET /api/game ──────────────────────────────────────────────────────────

    @Test
    void getGame_returnsInitialState() throws Exception {
        mockMvc.perform(get("/api/game").param("gameId", "it-get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayer").value("WHITE"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.board.squares").isArray())
                .andExpect(jsonPath("$.board.squares", hasSize(8)))
                .andExpect(jsonPath("$.legalMoves", hasSize(20)));
    }

    @Test
    void getGame_boardHasPiecesOnCorrectRows() throws Exception {
        mockMvc.perform(get("/api/game").param("gameId", "it-board"))
                .andExpect(status().isOk())
                // Display row 0 = rank 8 = black pieces
                .andExpect(jsonPath("$.board.squares[0][4]").value("♚"))
                // Display row 1 = rank 7 = black pawns
                .andExpect(jsonPath("$.board.squares[1][0]").value("♟"))
                // Display row 6 = rank 2 = white pawns
                .andExpect(jsonPath("$.board.squares[6][0]").value("♙"))
                // Display row 7 = rank 1 = white pieces
                .andExpect(jsonPath("$.board.squares[7][4]").value("♔"));
    }

    @Test
    void getGame_usesDefaultGameIdWhenNotProvided() throws Exception {
        mockMvc.perform(get("/api/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayer").value("WHITE"));
    }

    // ── POST /api/game/move ────────────────────────────────────────────────────

    @Test
    void makeMove_validPawnAdvance_returnsUpdatedState() throws Exception {
        mockMvc.perform(post("/api/game/move")
                        .param("gameId", "it-move1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayer").value("BLACK"))
                // e4 display row 4, col 4 should have white pawn
                .andExpect(jsonPath("$.board.squares[4][4]").value("♙"))
                // e2 display row 6, col 4 should be empty
                .andExpect(jsonPath("$.board.squares[6][4]").value(""));
    }

    @Test
    void makeMove_invalidMove_returns400() throws Exception {
        mockMvc.perform(post("/api/game/move")
                        .param("gameId", "it-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void makeMove_movingOpponentPiece_returns400() throws Exception {
        mockMvc.perform(post("/api/game/move")
                        .param("gameId", "it-opponent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e7\",\"to\":\"e5\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void makeMove_knightMove_isValid() throws Exception {
        mockMvc.perform(post("/api/game/move")
                        .param("gameId", "it-knight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"g1\",\"to\":\"f3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayer").value("BLACK"));
    }

    @Test
    void makeMove_sequenceOfMoves_alternatesPlayers() throws Exception {
        mockMvc.perform(post("/api/game/move")
                        .param("gameId", "it-seq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e4\"}"))
                .andExpect(jsonPath("$.currentPlayer").value("BLACK"));

        mockMvc.perform(post("/api/game/move")
                        .param("gameId", "it-seq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e7\",\"to\":\"e5\"}"))
                .andExpect(jsonPath("$.currentPlayer").value("WHITE"));
    }

    // ── POST /api/game/reset ───────────────────────────────────────────────────

    @Test
    void resetGame_afterMoves_restoresInitialState() throws Exception {
        // Make a move
        mockMvc.perform(post("/api/game/move")
                .param("gameId", "it-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"e2\",\"to\":\"e4\"}"));

        // Reset
        mockMvc.perform(post("/api/game/reset").param("gameId", "it-reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayer").value("WHITE"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.legalMoves", hasSize(20)))
                // e2 should have white pawn back
                .andExpect(jsonPath("$.board.squares[6][4]").value("♙"));
    }
}
