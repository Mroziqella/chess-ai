# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Two-player chess game with Java Spring Boot backend and simple HTML/JS frontend. Architecture follows DDD (Domain-Driven Design) principles for future extensibility.

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2
- **Frontend**: Plain HTML/CSS/JS
- **Build**: Maven

## Running the Application

```bash
cd "C:\Users\XPC\Projekt AI\Chess"
mvn spring-boot:run
```

Then open http://localhost:8080 in your browser.

## Key Commands

- `mvn spring-boot:run` - Start the application
- `mvn test` - Run tests
- `mvn clean package` - Build JAR file

## Architecture (DDD)

```
src/main/java/chess/
├── domain/
│   ├── model/      # Core domain objects (Board, Piece, Position, Player, Square)
│   ├── piece/      # Chess piece implementations (King, Queen, Rook, Bishop, Knight, Pawn)
│   └── service/    # Domain services (ChessRules)
├── application/    # Application services and DTOs
│   ├── GameService.java
│   └── dto/
└── infrastructure/ # REST controller
    └── controller/GameController.java
```

## Key Design Decisions

- **Board is generic**: `Board(int rows, int cols)` constructor supports custom board sizes for future extensions
- **Piece abstraction**: Each piece extends `Piece` abstract class with its own movement logic
- **In-memory game storage**: Simple `ConcurrentHashMap` for game state (production would need persistence)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/game | Get game state |
| POST | /api/game/move | Make a move |
| POST | /api/game/reset | Start new game |

## Extending the Project

To add new piece types:
1. Create new class in `src/main/java/chess/domain/piece/` extending `Piece`
2. Implement `type()` and `getLegalMoves()` methods
3. Update `ChessRules.createPieceCopy()` and `GameService.handlePromotion()`

To change board size (future):
- Constructor already supports: `new Board(rows, cols)`
- Update frontend grid in `style.css` and JS array dimensions

## Critical Implementation Notes (saves re-reading source files)

### Coordinate system
- **Backend**: `Position(row, col)` — row 0 = rank 1 (white side), row 7 = rank 8 (black side)
- **Frontend display**: squares array index 0 = rank 8 (top), index 7 = rank 1 (bottom)
- **Conversion**: display row = `8 - rank_number`; backend `fromAlgebraic("e4")` → `Position(3, 4)`

### Board serialization (GameState.BoardDTO)
- `fromBoard()` iterates `row = 7 downto 0` so `squares[0]` = rank 8 pieces
- Pieces serialized as Unicode symbols only (no color metadata in response)
- Frontend determines color via `WHITE_PIECES`/`BLACK_PIECES` Sets (♔♕♖♗♘♙ / ♚♛♜♝♞♟)

### Game initialization
- `GameService.getGame()` uses `computeIfAbsent` with `createInitialBoard()` — new games start with pieces
- `new Game()` inner class creates an empty board; always go through `getGame()` not `new Game()` directly

### Position bounds
- `Position` constructor does NOT validate bounds — pieces may create off-board positions during move calculation
- `board.isValidPosition(pos)` is the authoritative bounds gate (called inside every `getLegalMoves`)
- `Position.fromAlgebraic()` DOES validate ('a'-'h', '1'-'8') for user input

### Error handling
- `GameService.IllegalMoveException` → caught by `@ExceptionHandler` in `GameController` → returns HTTP 400
- Returns `{"message": "..."}` JSON body

### Test coverage (53 tests, all passing)
- Unit: `PositionTest`, `PawnTest`, `KnightTest`, `ChessRulesTest`, `GameServiceTest`
- Integration: `GameControllerIntegrationTest` (full Spring context, MockMvc)
- Run with: `mvn test`