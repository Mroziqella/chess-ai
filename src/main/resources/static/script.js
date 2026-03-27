let gameState = null;
let selectedSquare = null;
let validMoves = [];
let lastMove = null;

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1'];

const WHITE_PIECES = new Set(['♔', '♕', '♖', '♗', '♘', '♙']);
const BLACK_PIECES = new Set(['♚', '♛', '♜', '♝', '♞', '♟']);

function getGameId() {
    return new URLSearchParams(window.location.search).get('gameId') || 'default';
}

async function loadGame() {
    try {
        const response = await fetch('/api/game?gameId=' + getGameId());
        gameState = await response.json();
        renderBoard();
        updateStatus();
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

        lastMove = { from: fromAlgebraic(from), to: fromAlgebraic(to) };
        gameState = await response.json();
        renderBoard();
        updateStatus();
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

loadGame();
