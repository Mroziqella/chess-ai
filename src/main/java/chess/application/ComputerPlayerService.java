package chess.application;

import chess.domain.model.*;
import chess.domain.piece.*;
import chess.domain.service.ChessRules;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI chess engine using minimax with alpha-beta pruning and piece-square tables.
 */
@Service
public class ComputerPlayerService {

    private final ChessRules chessRules;

    private static final int MAX_DEPTH = 3;

    private static final Map<PieceType, Integer> PIECE_VALUES = Map.of(
            PieceType.PAWN, 100,
            PieceType.KNIGHT, 320,
            PieceType.BISHOP, 330,
            PieceType.ROOK, 500,
            PieceType.QUEEN, 900,
            PieceType.KING, 20000
    );

    // Piece-square tables (from white's perspective, row 0 = rank 1)
    private static final int[][] PAWN_TABLE = {
            {  0,  0,  0,  0,  0,  0,  0,  0},
            {  5, 10, 10,-20,-20, 10, 10,  5},
            {  5, -5,-10,  0,  0,-10, -5,  5},
            {  0,  0,  0, 20, 20,  0,  0,  0},
            {  5,  5, 10, 25, 25, 10,  5,  5},
            { 10, 10, 20, 30, 30, 20, 10, 10},
            { 50, 50, 50, 50, 50, 50, 50, 50},
            {  0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_TABLE = {
            {-50,-40,-30,-30,-30,-30,-40,-50},
            {-40,-20,  0,  5,  5,  0,-20,-40},
            {-30,  5, 10, 15, 15, 10,  5,-30},
            {-30,  0, 15, 20, 20, 15,  0,-30},
            {-30,  5, 15, 20, 20, 15,  5,-30},
            {-30,  0, 10, 15, 15, 10,  0,-30},
            {-40,-20,  0,  0,  0,  0,-20,-40},
            {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_TABLE = {
            {-20,-10,-10,-10,-10,-10,-10,-20},
            {-10,  5,  0,  0,  0,  0,  5,-10},
            {-10, 10, 10, 10, 10, 10, 10,-10},
            {-10,  0, 10, 10, 10, 10,  0,-10},
            {-10,  5,  5, 10, 10,  5,  5,-10},
            {-10,  0,  5, 10, 10,  5,  0,-10},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_TABLE = {
            {  0,  0,  0,  5,  5,  0,  0,  0},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            {  5, 10, 10, 10, 10, 10, 10,  5},
            {  0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] QUEEN_TABLE = {
            {-20,-10,-10, -5, -5,-10,-10,-20},
            {-10,  0,  5,  0,  0,  0,  0,-10},
            {-10,  5,  5,  5,  5,  5,  0,-10},
            {  0,  0,  5,  5,  5,  5,  0, -5},
            { -5,  0,  5,  5,  5,  5,  0, -5},
            {-10,  0,  5,  5,  5,  5,  0,-10},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    private static final int[][] KING_TABLE = {
            { 20, 30, 10,  0,  0, 10, 30, 20},
            { 20, 20,  0,  0,  0,  0, 20, 20},
            {-10,-20,-20,-20,-20,-20,-20,-10},
            {-20,-30,-30,-40,-40,-30,-30,-20},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30}
    };

    public ComputerPlayerService(ChessRules chessRules) {
        this.chessRules = chessRules;
    }

    /**
     * Find the best move for the given player on the given board.
     * Returns the move as "from-to" algebraic notation (e.g., "e2-e4"), or null if no moves.
     */
    public String findBestMove(Board board, Player player) {
        List<Move> moves = generateLegalMoves(board, player);
        if (moves.isEmpty()) return null;

        int bestScore = Integer.MIN_VALUE;
        Move bestMove = moves.get(0);

        for (Move move : moves) {
            Board copy = copyBoard(board);
            executeMove(copy, move, player);
            int score = -minimax(copy, player.opponent(), MAX_DEPTH - 1,
                    Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove.from.toAlgebraic() + "-" + bestMove.to.toAlgebraic();
    }

    private int minimax(Board board, Player player, int depth, int alpha, int beta) {
        if (depth == 0) {
            return evaluate(board, player);
        }

        if (chessRules.isCheckmate(player, board)) {
            return -20000 - depth;
        }
        if (chessRules.isStalemate(player, board)) {
            return 0;
        }

        List<Move> moves = generateLegalMoves(board, player);
        int bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            Board copy = copyBoard(board);
            executeMove(copy, move, player);
            int score = -minimax(copy, player.opponent(), depth - 1, -beta, -alpha);
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }

        return bestScore;
    }

    private int evaluate(Board board, Player player) {
        int score = 0;
        for (var entry : board.allPieces().entrySet()) {
            Position pos = entry.getKey();
            Piece piece = entry.getValue();
            int value = PIECE_VALUES.get(piece.type()) + getPositionalBonus(piece.type(), pos, piece.color());
            score += (piece.color() == player) ? value : -value;
        }
        return score;
    }

    private int getPositionalBonus(PieceType type, Position pos, Player color) {
        int[][] table = switch (type) {
            case PAWN   -> PAWN_TABLE;
            case KNIGHT -> KNIGHT_TABLE;
            case BISHOP -> BISHOP_TABLE;
            case ROOK   -> ROOK_TABLE;
            case QUEEN  -> QUEEN_TABLE;
            case KING   -> KING_TABLE;
        };
        int row = color == Player.WHITE ? pos.row() : 7 - pos.row();
        return table[row][pos.col()];
    }

    private List<Move> generateLegalMoves(Board board, Player player) {
        List<Move> moves = new ArrayList<>();
        for (var entry : board.getPiecesOf(player).entrySet()) {
            Position from = entry.getKey();
            Piece piece = entry.getValue();
            for (Position to : piece.getLegalMoves(from, board)) {
                if (chessRules.isLegalMove(from, to, player, board)) {
                    moves.add(new Move(from, to));
                }
            }
        }
        // Move ordering: captures first for better alpha-beta pruning
        moves.sort((a, b) -> {
            boolean aCapture = board.getPiece(a.to).isPresent();
            boolean bCapture = board.getPiece(b.to).isPresent();
            return Boolean.compare(bCapture, aCapture);
        });
        return moves;
    }

    private void executeMove(Board board, Move move, Player player) {
        if (chessRules.isEnPassantCapture(move.from, move.to, board)) {
            int dir = player == Player.WHITE ? 1 : -1;
            board.removePiece(new Position(move.to.row() - dir, move.to.col()));
        }

        boolean wasCastling = board.getPiece(move.from)
                .map(p -> chessRules.isCastlingMove(move.from, move.to, p)).orElse(false);

        board.movePiece(move.from, move.to);

        if (wasCastling) {
            int rookFromCol = move.to.col() > move.from.col() ? 7 : 0;
            int rookToCol = move.to.col() > move.from.col() ? 5 : 3;
            board.movePiece(
                    new Position(move.from.row(), rookFromCol),
                    new Position(move.from.row(), rookToCol));
        }

        updateCastlingRights(board, move.from, move.to, player);

        boolean pawnDoublePush = board.getPiece(move.to)
                .map(p -> p.type() == PieceType.PAWN).orElse(false)
                && Math.abs(move.to.row() - move.from.row()) == 2;
        board.setEnPassantTarget(pawnDoublePush
                ? new Position((move.from.row() + move.to.row()) / 2, move.from.col())
                : null);

        // Auto-promote to queen
        board.getPiece(move.to).ifPresent(p -> {
            if (p.type() == PieceType.PAWN && (move.to.row() == 0 || move.to.row() == 7)) {
                board.setPiece(move.to, new Queen(player));
            }
        });
    }

    private void updateCastlingRights(Board board, Position from, Position to, Player player) {
        int baseRow = player == Player.WHITE ? 0 : 7;
        if (from.row() == baseRow && from.col() == 4) {
            board.setCanCastleKingSide(player, false);
            board.setCanCastleQueenSide(player, false);
        }
        if (from.row() == baseRow && from.col() == 0) {
            board.setCanCastleQueenSide(player, false);
        }
        if (from.row() == baseRow && from.col() == 7) {
            board.setCanCastleKingSide(player, false);
        }
        int oppBaseRow = player == Player.WHITE ? 7 : 0;
        Player opponent = player.opponent();
        if (to.row() == oppBaseRow && to.col() == 0) {
            board.setCanCastleQueenSide(opponent, false);
        }
        if (to.row() == oppBaseRow && to.col() == 7) {
            board.setCanCastleKingSide(opponent, false);
        }
    }

    private Board copyBoard(Board original) {
        Board copy = new Board(original.rows(), original.cols());
        original.allPieces().forEach((pos, piece) -> copy.setPiece(pos, createPieceCopy(piece)));
        copy.setEnPassantTarget(original.getEnPassantTarget());
        copy.setCanCastleKingSide(Player.WHITE, original.canCastleKingSide(Player.WHITE));
        copy.setCanCastleQueenSide(Player.WHITE, original.canCastleQueenSide(Player.WHITE));
        copy.setCanCastleKingSide(Player.BLACK, original.canCastleKingSide(Player.BLACK));
        copy.setCanCastleQueenSide(Player.BLACK, original.canCastleQueenSide(Player.BLACK));
        return copy;
    }

    private Piece createPieceCopy(Piece piece) {
        return switch (piece.type()) {
            case KING   -> new King(piece.color());
            case QUEEN  -> new Queen(piece.color());
            case ROOK   -> new Rook(piece.color());
            case BISHOP -> new Bishop(piece.color());
            case KNIGHT -> new Knight(piece.color());
            case PAWN   -> new Pawn(piece.color());
        };
    }

    private record Move(Position from, Position to) {}
}
