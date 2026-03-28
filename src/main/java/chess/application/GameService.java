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

    public static final String COMPUTER_PLAYER_NAME = "Komputer";

    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final ChessRules chessRules;
    private final ComputerPlayerService computerPlayerService;

    public GameService(ChessRules chessRules, ComputerPlayerService computerPlayerService) {
        this.chessRules = chessRules;
        this.computerPlayerService = computerPlayerService;
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

        executeMoveOnBoard(game, from, to, request.promotion());

        // Auto-play computer move if it's a computer game and not game over
        if (game.computerColor != null
                && game.currentPlayer == game.computerColor
                && !isGameOver(game)) {
            String bestMove = computerPlayerService.findBestMove(game.board, game.currentPlayer);
            if (bestMove != null) {
                String[] parts = bestMove.split("-");
                executeMoveOnBoard(game,
                        Position.fromAlgebraic(parts[0]),
                        Position.fromAlgebraic(parts[1]),
                        "QUEEN");
            }
        }

        return buildGameState(game, resolvePlayerColor(game, username));
    }

    /**
     * Reset the game to the initial position with white to move.
     */
    public GameState resetGame(String gameId) {
        Game game = getOrCreateGame(gameId);
        game.board = createInitialBoard();
        game.currentPlayer = Player.WHITE;
        game.lastMove = null;
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

    /**
     * Create a game against the computer (player is always white).
     */
    public GameState createComputerGame(String gameId, String playerUsername) {
        Game game = new Game(createInitialBoard());
        game.whitePlayer = playerUsername;
        game.blackPlayer = COMPUTER_PLAYER_NAME;
        game.computerColor = Player.BLACK;
        games.put(gameId, game);
        return buildGameState(game, Player.WHITE);
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
                playerColor,
                game.lastMove
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

    private void executeMoveOnBoard(Game game, Position from, Position to, String promotion) {
        boolean wasEnPassant = chessRules.isEnPassantCapture(from, to, game.board);
        boolean wasCastling = game.board.getPiece(from)
                .map(p -> chessRules.isCastlingMove(from, to, p)).orElse(false);

        game.board.movePiece(from, to);

        if (wasEnPassant) {
            int dir = game.currentPlayer == Player.WHITE ? 1 : -1;
            game.board.removePiece(new Position(to.row() - dir, to.col()));
        }

        if (wasCastling) {
            int rookFromCol = to.col() > from.col() ? 7 : 0;
            int rookToCol = to.col() > from.col() ? 5 : 3;
            game.board.movePiece(
                    new Position(from.row(), rookFromCol),
                    new Position(from.row(), rookToCol));
        }

        updateCastlingRights(game.board, from, to, game.currentPlayer);

        boolean wasPawnDoublePush = game.board.getPiece(to)
                .map(p -> p.type() == PieceType.PAWN).orElse(false)
                && Math.abs(to.row() - from.row()) == 2;
        game.board.setEnPassantTarget(wasPawnDoublePush
                ? new Position((from.row() + to.row()) / 2, from.col())
                : null);

        handlePromotion(to, game.board, promotion);
        game.lastMove = from.toAlgebraic() + "-" + to.toAlgebraic();
        game.currentPlayer = game.currentPlayer.opponent();
    }

    private void updateCastlingRights(Board board, Position from, Position to, Player player) {
        int baseRow = player == Player.WHITE ? 0 : 7;
        // King moved — revoke both rights
        if (from.row() == baseRow && from.col() == 4) {
            board.setCanCastleKingSide(player, false);
            board.setCanCastleQueenSide(player, false);
        }
        // Rook moved from starting square
        if (from.row() == baseRow && from.col() == 0) {
            board.setCanCastleQueenSide(player, false);
        }
        if (from.row() == baseRow && from.col() == 7) {
            board.setCanCastleKingSide(player, false);
        }
        // Rook captured on opponent's starting square
        int oppBaseRow = player == Player.WHITE ? 7 : 0;
        Player opponent = player.opponent();
        if (to.row() == oppBaseRow && to.col() == 0) {
            board.setCanCastleQueenSide(opponent, false);
        }
        if (to.row() == oppBaseRow && to.col() == 7) {
            board.setCanCastleKingSide(opponent, false);
        }
    }

    private boolean isGameOver(Game game) {
        return chessRules.isCheckmate(game.currentPlayer, game.board)
                || chessRules.isStalemate(game.currentPlayer, game.board);
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
        String lastMove;
        Player computerColor;

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
