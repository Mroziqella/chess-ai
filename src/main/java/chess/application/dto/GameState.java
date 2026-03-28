package chess.application.dto;

import chess.domain.model.Board;
import chess.domain.model.Piece;
import chess.domain.model.Player;
import chess.domain.model.Position;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DTO representing the full state of a game at a single point in time.
 */
public record GameState(
        BoardDTO board,
        Player currentPlayer,
        GameStatus status,
        List<String> legalMoves,
        String whitePlayer,
        String blackPlayer,
        Player playerColor,
        String lastMove
) {
    /**
     * 2-D snapshot of the board as Unicode piece symbols, indexed
     * [displayRow][col] where display row 0 is rank 8 (black's back rank).
     */
    public record BoardDTO(List<List<String>> squares) {

        public static BoardDTO fromBoard(Board board) {
            List<List<String>> squares = IntStream.range(0, board.rows())
                    .map(i -> board.rows() - 1 - i) // display row 0 = highest backend row (rank 8)
                    .mapToObj(row -> IntStream.range(0, board.cols())
                            .mapToObj(col -> board.getPiece(new Position(row, col))
                                    .map(Piece::getSymbol)
                                    .orElse(""))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
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
