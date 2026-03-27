package chess.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for move request.
 */
public record MoveRequest(
        @NotBlank String from,
        @NotBlank String to,
        String promotion
) {
    public MoveRequest(String from, String to) {
        this(from, to, null);
    }
}