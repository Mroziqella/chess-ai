package chess.application;

import chess.application.dto.GameState;
import chess.application.dto.MoveRequest;
import chess.domain.model.*;
import chess.domain.piece.*;
import chess.domain.service.ChessRules;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing the chess game state.
 */
@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final ChessRules chessRules;

    public GameService(ChessRules chessRules) {
        this.chessRules = chessRules;
    }

    /**
     * Get or create a game.
     */
    public Game getGame(String gameId) {
        return games.computeIfAbsent(gameId, k -> {
            Game g = new Game();
            g.board = createInitialBoard();
            return g;
        });
    }

    /**
     * Get current game state.
     */
    public GameState getGameState(String gameId) {
        Game game = getGame(gameId);
        Board board = game.board;
        Player currentPlayer = game.currentPlayer;

        GameState.GameStatus status = determineGameStatus(currentPlayer, board);
        List<String> legalMoves = getLegalMovesForCurrentPlayer(game);

        return new GameState(
                GameState.BoardDTO.fromBoard(board),
                currentPlayer,
                status,
                legalMoves
        );
    }

    /**
     * Make a move.
     */
    public GameState makeMove(String gameId, MoveRequest request) {
        Game game = getGame(gameId);

        Position from = Position.fromAlgebraic(request.from());
        Position to = Position.fromAlgebraic(request.to());

        if (!chessRules.isLegalMove(from, to, game.currentPlayer, game.board)) {
            throw new IllegalMoveException("Invalid move: " + request.from() + " -> " + request.to());
        }

        boolean enPassant = isEnPassantCapture(from, to, game.board);

        // Execute the move
        game.board.movePiece(from, to);

        // En passant: remove the captured pawn (it is beside 'from', not at 'to')
        if (enPassant) {
            int direction = game.currentPlayer == Player.WHITE ? 1 : -1;
            game.board.removePiece(new Position(to.row() - direction, to.col()));
        }

        // Update en passant target for the next move
        Optional<Piece> movedPiece = game.board.getPiece(to);
        if (movedPiece.isPresent() && movedPiece.get().type() == PieceType.PAWN
                && Math.abs(to.row() - from.row()) == 2) {
            game.board.setEnPassantTarget(new Position((from.row() + to.row()) / 2, from.col()));
        } else {
            game.board.setEnPassantTarget(null);
        }

        // Handle pawn promotion
        handlePromotion(to, game.board, request.promotion());

        // Switch player
        game.currentPlayer = game.currentPlayer.opponent();

        return getGameState(gameId);
    }

    /**
     * Reset the game.
     */
    public GameState resetGame(String gameId) {
        Game game = getGame(gameId);
        game.board = createInitialBoard();
        game.currentPlayer = Player.WHITE;

        return getGameState(gameId);
    }

    private boolean isEnPassantCapture(Position from, Position to, Board board) {
        Position epTarget = board.getEnPassantTarget();
        if (epTarget == null || !to.equals(epTarget)) return false;
        Piece piece = board.getPiece(from).orElse(null);
        return piece != null && piece.type() == PieceType.PAWN;
    }

    private void handlePromotion(Position pawnPosition, Board board, String requestedPromotion) {
        var piece = board.getPiece(pawnPosition);
        if (piece.isPresent() && piece.get().type() == PieceType.PAWN) {
            int row = pawnPosition.row();
            if (row == 0 || row == board.rows() - 1) {
                Player color = piece.get().color();
                Piece promoted = switch (requestedPromotion != null ? requestedPromotion.toUpperCase() : "QUEEN") {
                    case "ROOK"   -> new Rook(color);
                    case "BISHOP" -> new Bishop(color);
                    case "KNIGHT" -> new Knight(color);
                    default       -> new Queen(color);
                };
                board.setPiece(pawnPosition, promoted);
            }
        }
    }

    // Package-private for testing — allows setting up custom positions
    void setupPositionForTesting(String gameId, Board board, Player currentPlayer) {
        Game game = getGame(gameId);
        game.board = board;
        game.currentPlayer = currentPlayer;
    }

    private GameState.GameStatus determineGameStatus(Player currentPlayer, Board board) {
        if (chessRules.isCheckmate(currentPlayer, board)) {
            return GameState.GameStatus.CHECKMATE;
        }
        if (chessRules.isStalemate(currentPlayer, board)) {
            return GameState.GameStatus.STALEMATE;
        }
        if (chessRules.isInCheck(currentPlayer, board)) {
            return GameState.GameStatus.CHECK;
        }
        return GameState.GameStatus.IN_PROGRESS;
    }

    private List<String> getLegalMovesForCurrentPlayer(Game game) {
        List<String> moves = new ArrayList<>();
        Player player = game.currentPlayer;
        Board board = game.board;

        for (Map.Entry<Position, Piece> entry : board.getPiecesOf(player).entrySet()) {
            Position from = entry.getKey();
            Piece piece = entry.getValue();

            List<Position> legalTargets = piece.getLegalMoves(from, board).stream()
                    .filter(to -> chessRules.isLegalMove(from, to, player, board))
                    .toList();

            for (Position to : legalTargets) {
                moves.add(from.toAlgebraic() + "-" + to.toAlgebraic());
            }
        }

        return moves;
    }

    private Board createInitialBoard() {
        Board board = new Board();

        // Place white pieces
        board.setPiece(new Position(0, 0), new Rook(Player.WHITE));
        board.setPiece(new Position(0, 1), new Knight(Player.WHITE));
        board.setPiece(new Position(0, 2), new Bishop(Player.WHITE));
        board.setPiece(new Position(0, 3), new Queen(Player.WHITE));
        board.setPiece(new Position(0, 4), new King(Player.WHITE));
        board.setPiece(new Position(0, 5), new Bishop(Player.WHITE));
        board.setPiece(new Position(0, 6), new Knight(Player.WHITE));
        board.setPiece(new Position(0, 7), new Rook(Player.WHITE));

        // Place white pawns
        for (int col = 0; col < 8; col++) {
            board.setPiece(new Position(1, col), new Pawn(Player.WHITE));
        }

        // Place black pieces
        board.setPiece(new Position(7, 0), new Rook(Player.BLACK));
        board.setPiece(new Position(7, 1), new Knight(Player.BLACK));
        board.setPiece(new Position(7, 2), new Bishop(Player.BLACK));
        board.setPiece(new Position(7, 3), new Queen(Player.BLACK));
        board.setPiece(new Position(7, 4), new King(Player.BLACK));
        board.setPiece(new Position(7, 5), new Bishop(Player.BLACK));
        board.setPiece(new Position(7, 6), new Knight(Player.BLACK));
        board.setPiece(new Position(7, 7), new Rook(Player.BLACK));

        // Place black pawns
        for (int col = 0; col < 8; col++) {
            board.setPiece(new Position(6, col), new Pawn(Player.BLACK));
        }

        return board;
    }

    /**
     * Internal game state.
     */
    private static class Game {
        Board board;
        Player currentPlayer;

        Game() {
            this.board = new Board();
            this.currentPlayer = Player.WHITE;
        }
    }

    /**
     * Exception for invalid moves.
     */
    public static class IllegalMoveException extends RuntimeException {
        public IllegalMoveException(String message) {
            super(message);
        }
    }
}