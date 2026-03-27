package chess.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PositionTest {

    @Test
    void fromAlgebraic_parsesCorrectly() {
        assertThat(Position.fromAlgebraic("a1")).isEqualTo(new Position(0, 0));
        assertThat(Position.fromAlgebraic("h8")).isEqualTo(new Position(7, 7));
        assertThat(Position.fromAlgebraic("e4")).isEqualTo(new Position(3, 4));
        assertThat(Position.fromAlgebraic("a8")).isEqualTo(new Position(7, 0));
    }

    @Test
    void fromAlgebraic_rejectsInvalidInput() {
        assertThatThrownBy(() -> Position.fromAlgebraic("z9")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic("a0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic("i1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic("a9")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toAlgebraic_formatsCorrectly() {
        assertThat(new Position(0, 0).toAlgebraic()).isEqualTo("a1");
        assertThat(new Position(7, 7).toAlgebraic()).isEqualTo("h8");
        assertThat(new Position(3, 4).toAlgebraic()).isEqualTo("e4");
    }

    @Test
    void move_appliesDelta() {
        Position p = new Position(3, 3);
        assertThat(p.move(1, 0)).isEqualTo(new Position(4, 3));
        assertThat(p.move(0, -1)).isEqualTo(new Position(3, 2));
        assertThat(p.move(-2, -3)).isEqualTo(new Position(1, 0));
    }

    @Test
    void move_canProduceNegativeCoordinates_forBoundsCheckingPurposes() {
        // Pieces compute candidate positions then filter via board.isValidPosition().
        // Position must not throw for negative values — the board is the gate.
        Position edge = new Position(0, 0);
        assertThatNoException().isThrownBy(() -> edge.move(-1, -1));
        Position offBoard = edge.move(-1, -1);
        assertThat(offBoard.isWithin(8, 8)).isFalse();
    }

    @Test
    void isWithin_checksCorrectly() {
        assertThat(new Position(0, 0).isWithin(8, 8)).isTrue();
        assertThat(new Position(7, 7).isWithin(8, 8)).isTrue();
        assertThat(new Position(8, 0).isWithin(8, 8)).isFalse();
        assertThat(new Position(0, 8).isWithin(8, 8)).isFalse();
    }

    @Test
    void roundTrip_algebraicIsSymmetric() {
        for (char file = 'a'; file <= 'h'; file++) {
            for (char rank = '1'; rank <= '8'; rank++) {
                String notation = "" + file + rank;
                assertThat(Position.fromAlgebraic(notation).toAlgebraic()).isEqualTo(notation);
            }
        }
    }
}
