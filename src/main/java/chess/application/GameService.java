package chess.application;

import chess.application.dto.GameState;
import chess.application.dto.MoveRequest;
import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.PieceType;
import chess.domain.model.Player;
import chess.domain.model.Position;
import chess.domain.piece.Bishop;
import chess.domain.piece.Knight;
import chess.domain.piece.Pawn;
import chess.domain.piece.Queen;
import chess.domain.piece.Rook;
import chess.domain.piece.King;
import chess.domain.service.ChessRules;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Application service that manages active chess games.
 * Each game is identified by a string ID and stored in memory.
 */
@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final ChessRules chessRules;

    public GameService(ChessRules chessRules) {
        this.chessRules = chessRules;
    }

    /**
     * Return the current state of the game, creating it if it does not yet exist.
     */
    public GameState getGameState(String gameId) {
        return getGameState(gameId, null);
    }

    public GameState getGameState(String gameId, String username) {
        Game game = getOrCreateGame(gameId);
        return buildGameState(game, resolvePlayerColor(game, username));
    }

    /**
     * Apply the requested move and return the resulting game state.
     * Throws {@link IllegalMoveException} if the move is not legal.
     */
    public GameState makeMove(String gameId, MoveRequest request) {
        return makeMove(gameId, request, null);
    }

    public GameState makeMove(String gameId, MoveRequest request, String username) {
        Game game = getOrCreateGame(gameId);

        // For matched games, validate player identity
        if (game.whitePlayer != null && username != null) {
            if (!username.equals(game.whitePlayer) && !username.equals(game.blackPlayer)) {
                throw new IllegalMoveException("You are not a player in this game");
            }
            Player playerColor = resolvePlayerColor(game, username);
            if (playerColor != game.currentPlayer) {
                throw new IllegalMoveException("It's not your turn");
            }
        }

        Position from = Position.fromAlgebraic(request.from());
        Position to   = Position.fromAlgebraic(request.to());

        if (!chessRules.isLegalMove(from, to, game.currentPlayer, game.board)) {
            throw new IllegalMoveException("Invalid move: " + request.from() + " -> " + request.to());
        }

        boolean wasEnPassant = chessRules.isEnPassantCapture(from, to, game.board);
        game.board.movePiece(from, to);

        if (wasEnPassant) {
            removeEnPassantCapturedPawn(to, game.currentPlayer, game.board);
        }

        updateEnPassantTarget(from, to, game.board);
        handlePromotion(to, game.board, request.promotion());
        game.currentPlayer = game.currentPlayer.opponent();

        return buildGameState(game, resolvePlayerColor(game, username));
    }

    /**
     * Reset the game to the initial position with white to move.
     */
    public GameState resetGame(String gameId) {
        Game game = getOrCreateGame(gameId);
        game.board = createInitialBoard();
        game.currentPlayer = Player.WHITE;
        return buildGameState(game, null);
    }

    /**
     * Create a game with assigned players (used by matchmaking).
     */
    public void createMatchedGame(String gameId, String whitePlayer, String blackPlayer) {
        Game game = new Game(createInitialBoard());
        game.whitePlayer = whitePlayer;
        game.blackPlayer = blackPlayer;
        games.put(gameId, game);
    }

    // ── Package-private test hook ─────────────────────────────────────────────

    /**
     * Directly set a custom position for testing purposes.
     * Not part of the public API.
     */
    void setupPositionForTesting(String gameId, Board board, Player currentPlayer) {
        Game game = getOrCreateGame(gameId);
        game.board = board;
        game.currentPlayer = currentPlayer;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Game getOrCreateGame(String gameId) {
        return games.computeIfAbsent(gameId, id -> new Game(createInitialBoard()));
    }

    private GameState buildGameState(Game game, Player playerColor) {
        GameState.GameStatus status = determineStatus(game.currentPlayer, game.board);
        List<String> legalMoves = legalMovesForCurrentPlayer(game);
        return new GameState(
                GameState.BoardDTO.fromBoard(game.board),
                game.currentPlayer,
                status,
                legalMoves,
                game.whitePlayer,
                game.blackPlayer,
                playerColor
        );
    }

    private Player resolvePlayerColor(Game game, String username) {
        if (username == null || game.whitePlayer == null) return null;
        if (username.equals(game.whitePlayer)) return Player.WHITE;
        if (username.equals(game.blackPlayer)) return Player.BLACK;
        return null;
    }

    private GameState.GameStatus determineStatus(Player currentPlayer, Board board) {
        if (chessRules.isCheckmate(currentPlayer, board)) return GameState.GameStatus.CHECKMATE;
        if (chessRules.isStalemate(currentPlayer, board)) return GameState.GameStatus.STALEMATE;
        if (chessRules.isInCheck(currentPlayer, board))   return GameState.GameStatus.CHECK;
        return GameState.GameStatus.IN_PROGRESS;
    }

    private List<String> legalMovesForCurrentPlayer(Game game) {
        return game.board.getPiecesOf(game.currentPlayer).entrySet().stream()
                .flatMap(e -> legalMovesFrom(e.getKey(), e.getValue(), game))
                .collect(Collectors.toList());
    }

    private Stream<String> legalMovesFrom(Position from, Piece piece, Game game) {
        return piece.getLegalMoves(from, game.board).stream()
                .filter(to -> chessRules.isLegalMove(from, to, game.currentPlayer, game.board))
                .map(to -> from.toAlgebraic() + "-" + to.toAlgebraic());
    }

    private void removeEnPassantCapturedPawn(Position landingSquare, Player capturingPlayer, Board board) {
        int pawnAdvanceDirection = capturingPlayer == Player.WHITE ? 1 : -1;
        board.removePiece(new Position(landingSquare.row() - pawnAdvanceDirection, landingSquare.col()));
    }

    private void updateEnPassantTarget(Position from, Position to, Board board) {
        boolean wasPawnDoublePush = board.getPiece(to)
                .map(p -> p.type() == PieceType.PAWN)
                .orElse(false)
                && Math.abs(to.row() - from.row()) == 2;

        if (wasPawnDoublePush) {
            board.setEnPassantTarget(new Position((from.row() + to.row()) / 2, from.col()));
        } else {
            board.setEnPassantTarget(null);
        }
    }

    private void handlePromotion(Position square, Board board, String requestedPiece) {
        var pawn = board.getPiece(square);
        if (pawn.isEmpty() || pawn.get().type() != PieceType.PAWN) return;

        boolean hasReachedLastRank = square.row() == 0 || square.row() == board.rows() - 1;
        if (!hasReachedLastRank) return;

        Player color = pawn.get().color();
        Piece promoted = switch (requestedPiece != null ? requestedPiece.toUpperCase() : "QUEEN") {
            case "ROOK"   -> new Rook(color);
            case "BISHOP" -> new Bishop(color);
            case "KNIGHT" -> new Knight(color);
            default       -> new Queen(color);
        };
        board.setPiece(square, promoted);
    }

    private Board createInitialBoard() {
        Board board = new Board();

        placeBackRow(board, 0, Player.WHITE);
        placePawns(board, 1, Player.WHITE);
        placePawns(board, 6, Player.BLACK);
        placeBackRow(board, 7, Player.BLACK);

        return board;
    }

    private void placeBackRow(Board board, int row, Player player) {
        board.setPiece(new Position(row, 0), new Rook(player));
        board.setPiece(new Position(row, 1), new Knight(player));
        board.setPiece(new Position(row, 2), new Bishop(player));
        board.setPiece(new Position(row, 3), new Queen(player));
        board.setPiece(new Position(row, 4), new King(player));
        board.setPiece(new Position(row, 5), new Bishop(player));
        board.setPiece(new Position(row, 6), new Knight(player));
        board.setPiece(new Position(row, 7), new Rook(player));
    }

    private void placePawns(Board board, int row, Player player) {
        for (int col = 0; col < board.cols(); col++) {
            board.setPiece(new Position(row, col), new Pawn(player));
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private static class Game {
        Board board;
        Player currentPlayer = Player.WHITE;
        String whitePlayer;
        String blackPlayer;

        Game(Board board) {
            this.board = board;
        }
    }

    public static class IllegalMoveException extends RuntimeException {
        public IllegalMoveException(String message) {
            super(message);
        }
    }
}
