package chess.infrastructure.controller;

import chess.application.GameService;
import chess.application.dto.GameState;
import chess.application.dto.MoveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * REST controller for the chess game.
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Get the current game state.
     */
    @GetMapping
    public ResponseEntity<GameState> getGame(
            @RequestParam(defaultValue = "default") String gameId,
            Principal principal) {
        return ResponseEntity.ok(gameService.getGameState(gameId, principal.getName()));
    }

    /**
     * Make a move.
     */
    @PostMapping("/move")
    public ResponseEntity<GameState> makeMove(
            @RequestParam(defaultValue = "default") String gameId,
            @Valid @RequestBody MoveRequest request,
            Principal principal) {
        return ResponseEntity.ok(gameService.makeMove(gameId, request, principal.getName()));
    }

    /**
     * Reset the game.
     */
    @PostMapping("/reset")
    public ResponseEntity<GameState> resetGame(
            @RequestParam(defaultValue = "default") String gameId) {
        return ResponseEntity.ok(gameService.resetGame(gameId));
    }

    @ExceptionHandler(GameService.IllegalMoveException.class)
    public ResponseEntity<Map<String, String>> handleIllegalMove(GameService.IllegalMoveException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}