package chess.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the chess application.
 * Uses a real HTTP server (RANDOM_PORT) and Selenium for browser interaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChessE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void loginViaBrowser() {
        driver.get(baseUrl() + "/login.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("Kamil");
        driver.findElement(By.id("password")).sendKeys("Kamil");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("board")));
    }

    // ── API sanity checks ────────────────────────────────────────────────────

    @Test
    void api_getGame_returns200() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Kamil", "Kamil")
                .getForEntity("/api/game", String.class);
        System.out.println("GET /api/game status: " + response.getStatusCode());
        System.out.println("GET /api/game body:   " + response.getBody());
        assertEquals(200, response.getStatusCode().value(),
                "GET /api/game should return 200. Body: " + response.getBody());
    }

    @Test
    void api_resetGame_returns200() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Kamil", "Kamil")
                .postForEntity("/api/game/reset", null, String.class);
        assertEquals(200, response.getStatusCode().value(),
                "POST /api/game/reset should return 200. Body: " + response.getBody());
    }

    // ── Browser tests ────────────────────────────────────────────────────────

    @Test
    void browser_boardLoadsWith64Squares() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-board");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        List<WebElement> squares = driver.findElements(By.className("square"));
        assertEquals(64, squares.size(), "Board should have 64 squares");
    }

    @Test
    void browser_boardShowsPieces() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-pieces");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        List<WebElement> squares = driver.findElements(By.className("square"));

        // Row 0 = rank 8 (black pieces). Index [0][4] = black king
        String blackKing = squares.get(4).getText();
        assertEquals("\u265A", blackKing, "Black king should be at e8");

        // Row 7 = rank 1 (white pieces). Index [7][4] = white king
        String whiteKing = squares.get(7 * 8 + 4).getText();
        assertEquals("\u2654", whiteKing, "White king should be at e1");
    }

    @Test
    void browser_clickPieceHighlightsIt() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-highlight");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        // Click on e2 pawn (display row 6, col 4)
        List<WebElement> squares = driver.findElements(By.className("square"));
        WebElement e2 = squares.get(6 * 8 + 4);
        e2.click();

        // Small wait for JS re-render
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".square.selected")));

        // Re-fetch squares after render
        squares = driver.findElements(By.className("square"));
        WebElement e2After = squares.get(6 * 8 + 4);
        assertTrue(e2After.getAttribute("class").contains("selected"),
                "Clicked piece square should be highlighted as selected");
    }

    @Test
    void browser_clickPieceThenMoveExecutesMove() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-move");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        // Click e2 pawn (display row 6, col 4)
        List<WebElement> squares = driver.findElements(By.className("square"));
        squares.get(6 * 8 + 4).click();

        // Wait for selection highlight
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".square.selected")));

        // Click e4 (display row 4, col 4) — valid pawn double move
        squares = driver.findElements(By.className("square"));
        squares.get(4 * 8 + 4).click();

        // Wait for status to update to Black's turn
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("status"), "Czarne"));

        // Verify status shows Black's turn
        WebElement status = driver.findElement(By.id("status"));
        assertTrue(status.getText().contains("Czarne"),
                "After white moves, status should show Black's turn");

        // Verify pawn is now at e4 and e2 is empty
        squares = driver.findElements(By.className("square"));
        String e4Piece = squares.get(4 * 8 + 4).getText();
        String e2Piece = squares.get(6 * 8 + 4).getText();
        assertEquals("\u2659", e4Piece, "White pawn should be at e4 after move");
        assertEquals("", e2Piece, "e2 should be empty after pawn moved");
    }

    @Test
    void browser_clickResetButtonResetsGame() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-reset");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        // Make a move first: e2 -> e4
        List<WebElement> squares = driver.findElements(By.className("square"));
        squares.get(6 * 8 + 4).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".square.selected")));
        squares = driver.findElements(By.className("square"));
        squares.get(4 * 8 + 4).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("status"), "Czarne"));

        // Click "Nowa gra" reset button
        WebElement resetButton = driver.findElement(By.id("resetBtn"));
        resetButton.click();

        // Wait for White's turn again
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("status"), "Białe"));

        // Verify pawn is back at e2
        squares = driver.findElements(By.className("square"));
        String e2Piece = squares.get(6 * 8 + 4).getText();
        assertEquals("\u2659", e2Piece, "After reset, white pawn should be back at e2");
    }

    @Test
    void browser_clickEmptySquareDoesNothing() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-empty");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        // Click on e4 (empty square, display row 4, col 4)
        List<WebElement> squares = driver.findElements(By.className("square"));
        squares.get(4 * 8 + 4).click();

        // Should not have any selected square
        List<WebElement> selected = driver.findElements(By.cssSelector(".square.selected"));
        assertEquals(0, selected.size(), "Clicking an empty square should not select anything");
    }

    @Test
    void browser_clickOpponentPieceDoesNothing() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-opponent");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        // Click on e7 (black pawn, display row 1, col 4) — it's white's turn
        List<WebElement> squares = driver.findElements(By.className("square"));
        squares.get(1 * 8 + 4).click();

        // Should not have any selected square
        List<WebElement> selected = driver.findElements(By.cssSelector(".square.selected"));
        assertEquals(0, selected.size(), "Clicking opponent's piece should not select it");
    }

    @Test
    void browser_twoMovesSequence() {
        loginViaBrowser();
        driver.get(baseUrl() + "?gameId=e2e-seq");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("square")));

        // White: e2 -> e4
        List<WebElement> squares = driver.findElements(By.className("square"));
        squares.get(6 * 8 + 4).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".square.selected")));
        squares = driver.findElements(By.className("square"));
        squares.get(4 * 8 + 4).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("status"), "Czarne"));

        // Black: e7 -> e5
        squares = driver.findElements(By.className("square"));
        squares.get(1 * 8 + 4).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".square.selected")));
        squares = driver.findElements(By.className("square"));
        squares.get(3 * 8 + 4).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("status"), "Białe"));

        // Verify both pawns moved
        squares = driver.findElements(By.className("square"));
        assertEquals("\u2659", squares.get(4 * 8 + 4).getText(), "White pawn at e4");
        assertEquals("\u265F", squares.get(3 * 8 + 4).getText(), "Black pawn at e5");

        // Verify status is back to White
        WebElement status = driver.findElement(By.id("status"));
        assertTrue(status.getText().contains("Białe"));
    }
}
