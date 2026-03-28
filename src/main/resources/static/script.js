// ── State ──
let gameState = null;
let selectedSquare = null;
let validMoves = [];
let lastMove = null;
let currentGameId = null;
let playerColor = null;
let opponentName = null;
let isComputerGame = false;
let pollInterval = null;
let matchPollInterval = null;

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1'];

const WHITE_PIECES = new Set(['♔', '♕', '♖', '♗', '♘', '♙']);
const BLACK_PIECES = new Set(['♚', '♛', '♜', '♝', '♞', '♟']);

// ── Initialization ──

async function init() {
    const urlGameId = new URLSearchParams(window.location.search).get('gameId');
    if (urlGameId) {
        // Direct mode (backward compatible, no matchmaking)
        document.getElementById('lobby').remove();
        currentGameId = urlGameId;
        playerColor = null;
        showGameView(false);
        loadGame();
        return;
    }

    // Matchmaking mode — check current status
    try {
        const response = await fetch('/api/matchmaking/status');
        const status = await response.json();
        handleMatchStatus(status);
    } catch (error) {
        console.error('Error checking match status:', error);
        showLobby();
    }
}

function handleMatchStatus(status) {
    if (status.state === 'MATCHED') {
        currentGameId = status.gameId;
        playerColor = status.playerColor;
        opponentName = status.opponent;
        isComputerGame = (status.opponent === 'Komputer');
        showGameView(true);
        loadGame();
        if (!isComputerGame) {
            startGamePolling();
        }
    } else if (status.state === 'WAITING') {
        showWaiting();
        startMatchPolling();
    } else {
        showLobby();
    }
}

// ── UI visibility ──

function showLobby() {
    document.getElementById('lobby').classList.remove('hidden');
    document.getElementById('lobbyIdle').classList.remove('hidden');
    document.getElementById('lobbyWaiting').classList.add('hidden');
    document.getElementById('gameView').classList.add('hidden');
}

function showWaiting() {
    document.getElementById('lobby').classList.remove('hidden');
    document.getElementById('lobbyIdle').classList.add('hidden');
    document.getElementById('lobbyWaiting').classList.remove('hidden');
    document.getElementById('gameView').classList.add('hidden');
}

function showGameView(isMatchmaking) {
    document.getElementById('lobby').classList.add('hidden');
    document.getElementById('gameView').classList.remove('hidden');

    if (isMatchmaking) {
        document.getElementById('playerInfo').classList.remove('hidden');
        document.getElementById('opponentName').textContent = opponentName;
        document.getElementById('playerColorText').textContent =
            playerColor === 'WHITE' ? 'Białymi' : 'Czarnymi';
        document.getElementById('directControls').classList.add('hidden');
        document.getElementById('matchControls').classList.remove('hidden');
        document.getElementById('newGameBtn').classList.add('hidden');
    } else {
        document.getElementById('playerInfo').classList.add('hidden');
        document.getElementById('directControls').classList.remove('hidden');
        document.getElementById('matchControls').classList.add('hidden');
    }
}

// ── Matchmaking ──

async function findGame() {
    try {
        const response = await fetch('/api/matchmaking/join', { method: 'POST' });
        const status = await response.json();
        handleMatchStatus(status);
    } catch (error) {
        console.error('Error joining queue:', error);
    }
}

function startMatchPolling() {
    stopMatchPolling();
    matchPollInterval = setInterval(async () => {
        try {
            const response = await fetch('/api/matchmaking/status');
            const status = await response.json();
            if (status.state === 'MATCHED') {
                stopMatchPolling();
                handleMatchStatus(status);
            }
        } catch (error) {
            console.error('Error polling match:', error);
        }
    }, 2000);
}

function stopMatchPolling() {
    if (matchPollInterval) {
        clearInterval(matchPollInterval);
        matchPollInterval = null;
    }
}

async function cancelSearch() {
    stopMatchPolling();
    try {
        await fetch('/api/matchmaking/leave', { method: 'POST' });
    } catch (error) {
        console.error('Error canceling search:', error);
    }
    showLobby();
}

async function playComputer() {
    try {
        const response = await fetch('/api/matchmaking/computer', { method: 'POST' });
        const status = await response.json();
        handleMatchStatus(status);
    } catch (error) {
        console.error('Error starting computer game:', error);
    }
}

async function leaveAndFindNew() {
    stopGamePolling();
    try {
        await fetch('/api/matchmaking/leave', { method: 'POST' });
    } catch (error) {
        console.error('Error leaving game:', error);
    }
    currentGameId = null;
    playerColor = null;
    opponentName = null;
    gameState = null;
    selectedSquare = null;
    validMoves = [];
    lastMove = null;
    isComputerGame = false;
    showLobby();
}

// ── Game polling (for opponent moves) ──

function startGamePolling() {
    stopGamePolling();
    pollInterval = setInterval(async () => {
        await loadGame();
    }, 2000);
}

function stopGamePolling() {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

// ── Game logic ──

function getGameId() {
    return currentGameId || 'default';
}

async function loadGame() {
    try {
        const response = await fetch('/api/game?gameId=' + getGameId());
        gameState = await response.json();

        if (gameState.lastMove) {
            const parts = gameState.lastMove.split('-');
            lastMove = { from: fromAlgebraic(parts[0]), to: fromAlgebraic(parts[1]) };
        }

        renderBoard();
        updateStatus();

        // In matchmaking mode, show "new game" button when game ends
        if (playerColor && (gameState.status === 'CHECKMATE' || gameState.status === 'STALEMATE')) {
            stopGamePolling();
            document.getElementById('newGameBtn').classList.remove('hidden');
        }
    } catch (error) {
        console.error('Error loading game:', error);
    }
}

function renderBoard() {
    const board = document.getElementById('board');
    board.innerHTML = '';

    const squares = gameState.board.squares;
    const inCheck = gameState.status === 'CHECK' || gameState.status === 'CHECKMATE';

    // Find king square when in check
    let checkRow = -1, checkCol = -1;
    if (inCheck) {
        const kingSymbol = gameState.currentPlayer === 'WHITE' ? '♔' : '♚';
        outer:
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                if (squares[r][c] === kingSymbol) {
                    checkRow = r;
                    checkCol = c;
                    break outer;
                }
            }
        }
    }

    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            const square = document.createElement('div');
            const isLight = (row + col) % 2 === 0;
            square.className = `square ${isLight ? 'light' : 'dark'}`;
            square.dataset.row = row;
            square.dataset.col = col;

            // Coordinate labels via data attributes — rendered by CSS pseudo-elements,
            // invisible to Selenium getText() which ignores CSS-generated content
            if (col === 0) square.dataset.rank = RANKS[row];
            if (row === 7) square.dataset.file = FILES[col];

            // Last move highlight
            if (lastMove &&
                ((lastMove.from.row === row && lastMove.from.col === col) ||
                 (lastMove.to.row   === row && lastMove.to.col   === col))) {
                square.classList.add('last-move');
            }

            // Selected piece
            if (selectedSquare && selectedSquare.row === row && selectedSquare.col === col) {
                square.classList.add('selected');
            }

            // King in check
            if (inCheck && row === checkRow && col === checkCol) {
                square.classList.add('in-check');
            }

            const piece = squares[row][col];
            const isValidMove = validMoves.some(m => m.row === row && m.col === col);

            if (isValidMove) {
                if (piece) {
                    // Ring indicator for captures (background-image, no pseudo-elements used)
                    square.classList.add('valid-capture');
                } else {
                    // Dot indicator for empty target squares
                    const dot = document.createElement('div');
                    dot.className = 'move-dot';
                    square.appendChild(dot);
                }
            }

            // Piece element (appended last so it renders on top of indicators)
            if (piece) {
                const pieceEl = document.createElement('div');
                pieceEl.className = 'piece ' + (WHITE_PIECES.has(piece) ? 'piece-white' : 'piece-black');
                pieceEl.textContent = piece;
                square.appendChild(pieceEl);
            }

            square.addEventListener('click', () => handleSquareClick(row, col));
            board.appendChild(square);
        }
    }
}

async function handleSquareClick(row, col) {
    // In matchmaking mode, only allow interaction when it's your turn
    if (playerColor && gameState.currentPlayer !== playerColor) return;

    // Prevent interaction when game is over
    if (gameState.status === 'CHECKMATE' || gameState.status === 'STALEMATE') return;

    const notation = toAlgebraic(row, col);

    if (!selectedSquare) {
        const piece = gameState.board.squares[row][col];
        if (piece && isPieceOfCurrentPlayer(piece)) {
            selectedSquare = { row, col, notation };
            validMoves = getValidMovesForSquare(row, col);
            renderBoard();
        }
        return;
    }

    const move = validMoves.find(m => m.row === row && m.col === col);
    if (move) {
        let promotion = null;
        const movingPiece = gameState.board.squares[selectedSquare.row][selectedSquare.col];
        if (isPromotionMove(movingPiece, row)) {
            promotion = await showPromotionModal();
        }
        await makeMove(selectedSquare.notation, notation, promotion);
        selectedSquare = null;
        validMoves = [];
    } else {
        const piece = gameState.board.squares[row][col];
        if (piece && isPieceOfCurrentPlayer(piece)) {
            selectedSquare = { row, col, notation };
            validMoves = getValidMovesForSquare(row, col);
            renderBoard();
        } else {
            selectedSquare = null;
            validMoves = [];
            renderBoard();
        }
    }
}

function isPromotionMove(piece, toRow) {
    return (piece === '♙' && toRow === 0) || (piece === '♟' && toRow === 7);
}

function showPromotionModal() {
    return new Promise(resolve => {
        const isWhite = gameState.currentPlayer === 'WHITE';
        const colorClass = isWhite ? 'p-white' : 'p-black';
        const pieces = isWhite
            ? [{ symbol: '♕', type: 'QUEEN',  name: 'Hetman'  },
               { symbol: '♖', type: 'ROOK',   name: 'Wieża'   },
               { symbol: '♗', type: 'BISHOP', name: 'Goniec'  },
               { symbol: '♘', type: 'KNIGHT', name: 'Skoczek' }]
            : [{ symbol: '♛', type: 'QUEEN',  name: 'Hetman'  },
               { symbol: '♜', type: 'ROOK',   name: 'Wieża'   },
               { symbol: '♝', type: 'BISHOP', name: 'Goniec'  },
               { symbol: '♞', type: 'KNIGHT', name: 'Skoczek' }];

        const modal = document.getElementById('promotionModal');
        const container = document.getElementById('promotionPieces');
        container.innerHTML = '';

        pieces.forEach(p => {
            const btn = document.createElement('div');
            btn.className = 'promotion-piece';
            btn.innerHTML = `<span class="p-symbol ${colorClass}">${p.symbol}</span><span class="p-name">${p.name}</span>`;
            btn.addEventListener('click', () => {
                modal.classList.remove('active');
                resolve(p.type);
            });
            container.appendChild(btn);
        });

        modal.classList.add('active');
    });
}

function toAlgebraic(row, col) {
    return FILES[col] + RANKS[row];
}

function fromAlgebraic(notation) {
    return {
        col: FILES.indexOf(notation[0]),
        row: RANKS.indexOf(notation[1])
    };
}

function isPieceOfCurrentPlayer(piece) {
    return gameState.currentPlayer === 'WHITE' ? WHITE_PIECES.has(piece) : BLACK_PIECES.has(piece);
}

function getValidMovesForSquare(row, col) {
    const notation = toAlgebraic(row, col);
    return gameState.legalMoves
        .filter(move => move.startsWith(notation + '-'))
        .map(move => {
            const toNotation = move.split('-')[1];
            const toCol = toNotation.charCodeAt(0) - 'a'.charCodeAt(0);
            const toRow = 8 - parseInt(toNotation.charAt(1));
            return { row: toRow, col: toCol };
        });
}

async function makeMove(from, to, promotion = null) {
    try {
        const body = { from, to };
        if (promotion) body.promotion = promotion;

        const response = await fetch('/api/game/move?gameId=' + getGameId(), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Nieprawidłowy ruch');
            return;
        }

        gameState = await response.json();

        // Use server-side lastMove (shows computer's response in computer games)
        if (gameState.lastMove) {
            const parts = gameState.lastMove.split('-');
            lastMove = { from: fromAlgebraic(parts[0]), to: fromAlgebraic(parts[1]) };
        } else {
            lastMove = { from: fromAlgebraic(from), to: fromAlgebraic(to) };
        }

        renderBoard();
        updateStatus();

        // In matchmaking mode, manage polling after move
        if (playerColor) {
            if (gameState.status === 'CHECKMATE' || gameState.status === 'STALEMATE') {
                stopGamePolling();
                document.getElementById('newGameBtn').classList.remove('hidden');
            } else if (!isComputerGame) {
                startGamePolling();
            }
        }
    } catch (error) {
        console.error('Error making move:', error);
    }
}

async function resetGame() {
    try {
        const response = await fetch('/api/game/reset?gameId=' + getGameId(), { method: 'POST' });
        gameState = await response.json();
        selectedSquare = null;
        validMoves = [];
        lastMove = null;
        renderBoard();
        updateStatus();
    } catch (error) {
        console.error('Error resetting game:', error);
    }
}

function updateStatus() {
    const bar = document.getElementById('statusBar');
    const statusEl = document.getElementById('status');
    const dot = document.getElementById('playerDot');
    const isWhite = gameState.currentPlayer === 'WHITE';

    bar.className = 'status-bar';
    dot.className = 'player-dot ' + (isWhite ? 'white' : 'black');

    if (gameState.status === 'CHECK') {
        bar.classList.add('check');
        statusEl.textContent = 'Szach! Ruch: ' + (isWhite ? 'Białe' : 'Czarne');
    } else if (gameState.status === 'CHECKMATE') {
        bar.classList.add('checkmate');
        statusEl.textContent = 'Mat! Wygrały: ' + (isWhite ? 'Czarne' : 'Białe');
        dot.className = 'player-dot ' + (isWhite ? 'black' : 'white');
    } else if (gameState.status === 'STALEMATE') {
        bar.classList.add('stalemate');
        statusEl.textContent = 'Pat! Remis';
    } else {
        statusEl.textContent = 'Ruch: ' + (isWhite ? 'Białe' : 'Czarne');
    }
}

init();
