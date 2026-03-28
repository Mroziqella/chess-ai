package chess.application;

import chess.application.dto.MatchStatus;
import chess.domain.model.Player;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchmakingService {

    private final GameService gameService;
    private final ConcurrentLinkedQueue<String> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, MatchedGame> playerGames = new ConcurrentHashMap<>();

    public MatchmakingService(GameService gameService) {
        this.gameService = gameService;
    }

    public synchronized MatchStatus joinQueue(String username) {
        // If already in a game, return that
        MatchedGame existing = playerGames.get(username);
        if (existing != null) {
            return MatchStatus.matched(existing.gameId, existing.getColor(username), existing.getOpponent(username));
        }

        // Remove self from queue if already there
        waitingQueue.remove(username);

        // Find an opponent
        String opponent = null;
        while (!waitingQueue.isEmpty()) {
            String candidate = waitingQueue.poll();
            if (candidate != null && !candidate.equals(username)) {
                opponent = candidate;
                break;
            }
        }

        if (opponent != null) {
            String gameId = UUID.randomUUID().toString();
            MatchedGame mg = new MatchedGame(gameId, opponent, username);
            playerGames.put(opponent, mg);
            playerGames.put(username, mg);
            gameService.createMatchedGame(gameId, opponent, username);
            return MatchStatus.matched(gameId, Player.BLACK, opponent);
        }

        waitingQueue.add(username);
        return MatchStatus.waiting();
    }

    public MatchStatus getStatus(String username) {
        MatchedGame mg = playerGames.get(username);
        if (mg != null) {
            return MatchStatus.matched(mg.gameId, mg.getColor(username), mg.getOpponent(username));
        }
        if (waitingQueue.contains(username)) {
            return MatchStatus.waiting();
        }
        return MatchStatus.idle();
    }

    public void leaveGame(String username) {
        playerGames.remove(username);
        waitingQueue.remove(username);
    }

    static class MatchedGame {
        final String gameId;
        final String whitePlayer;
        final String blackPlayer;

        MatchedGame(String gameId, String whitePlayer, String blackPlayer) {
            this.gameId = gameId;
            this.whitePlayer = whitePlayer;
            this.blackPlayer = blackPlayer;
        }

        Player getColor(String username) {
            return username.equals(whitePlayer) ? Player.WHITE : Player.BLACK;
        }

        String getOpponent(String username) {
            return username.equals(whitePlayer) ? blackPlayer : whitePlayer;
        }
    }
}
