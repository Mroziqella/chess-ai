package chess.application.dto;

import chess.domain.model.Player;

public record MatchStatus(
        String state,
        String gameId,
        Player playerColor,
        String opponent
) {
    public static MatchStatus idle() {
        return new MatchStatus("IDLE", null, null, null);
    }

    public static MatchStatus waiting() {
        return new MatchStatus("WAITING", null, null, null);
    }

    public static MatchStatus matched(String gameId, Player playerColor, String opponent) {
        return new MatchStatus("MATCHED", gameId, playerColor, opponent);
    }
}
