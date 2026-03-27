package chess.application.dto;

import chess.domain.model.Board;
import chess.domain.model.Player;

import java.util.List;

/**
 * DTO for representing the current game state.
 */
public record GameState(
        BoardDTO board,
        Player currentPlayer,
        GameStatus status,
        List<String> legalMoves
) {
    public record BoardDTO(List<List<String>> squares) {
        public static BoardDTO fromBoard(Board board) {
            List<List<String>> squares = new java.util.ArrayList<>();
            for (int row = board.rows() - 1; row >= 0; row--) {
                List<String> rowSquares = new java.util.ArrayList<>();
                for (int col = 0; col < board.cols(); col++) {
                    var piece = board.getPiece(new chess.domain.model.Position(row, col));
                    rowSquares.add(piece.map(p -> p.getSymbol()).orElse(""));
                }
                squares.add(rowSquares);
            }
            return new BoardDTO(squares);
        }
    }

    public enum GameStatus {
        IN_PROGRESS,
        CHECK,
        CHECKMATE,
        STALEMATE
    }
}