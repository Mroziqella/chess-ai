package chess.infrastructure.controller;

import chess.application.MatchmakingService;
import chess.application.dto.MatchStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    public ResponseEntity<MatchStatus> join(Principal principal) {
        return ResponseEntity.ok(matchmakingService.joinQueue(principal.getName()));
    }

    @GetMapping("/status")
    public ResponseEntity<MatchStatus> status(Principal principal) {
        return ResponseEntity.ok(matchmakingService.getStatus(principal.getName()));
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(Principal principal) {
        matchmakingService.leaveGame(principal.getName());
        return ResponseEntity.ok().build();
    }
}
