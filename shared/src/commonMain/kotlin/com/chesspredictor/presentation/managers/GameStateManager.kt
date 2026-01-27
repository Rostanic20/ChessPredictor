package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.presentation.ChessConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameStateManager(
    private val chessRulesEngine: ChessRulesEngine
) {
    private val _board = MutableStateFlow(createInitialBoard())
    val board: StateFlow<ChessBoard> = _board.asStateFlow()

    private val _boardState = MutableStateFlow<Map<Square, ChessPiece>>(emptyMap())
    val boardState: StateFlow<Map<Square, ChessPiece>> = _boardState.asStateFlow()

    private val _currentTurn = MutableStateFlow(ChessColor.WHITE)
    val currentTurn: StateFlow<ChessColor> = _currentTurn.asStateFlow()

    private val _moveHistory = MutableStateFlow<List<DetailedMove>>(emptyList())
    val moveHistory: StateFlow<List<DetailedMove>> = _moveHistory.asStateFlow()

    private val _capturedPieces = MutableStateFlow<List<ChessPiece>>(emptyList())
    val capturedPieces: StateFlow<List<ChessPiece>> = _capturedPieces.asStateFlow()

    private val _gameStatus = MutableStateFlow(GameStatus())
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private val _lastMove = MutableStateFlow<ChessMove?>(null)
    val lastMove: StateFlow<ChessMove?> = _lastMove.asStateFlow()

    var currentGameState: GameState? = null
        private set

    fun initialize() {
        resetToInitialPosition()
    }

    fun updateFromGameState(gameState: GameState) {
        currentGameState = gameState
        _board.value = createBoardFromGameState(gameState)
        _boardState.value = gameState.board
        _currentTurn.value = gameState.turn
        _moveHistory.value = gameState.moveHistory
        _capturedPieces.value = gameState.capturedPieces
        updateGameStatus(gameState)
    }

    fun setLastMove(move: ChessMove?) {
        _lastMove.value = move
    }

    fun setGameStatus(status: GameStatus) {
        _gameStatus.value = status
    }

    fun reset() {
        resetToInitialPosition()
        _moveHistory.value = emptyList()
        _capturedPieces.value = emptyList()
        _lastMove.value = null
    }

    private fun resetToInitialPosition() {
        val initialBoard = createInitialBoard()
        val gameState = chessRulesEngine.parseGameState(initialBoard.fen)
        currentGameState = gameState
        _board.value = initialBoard
        _boardState.value = gameState.board
        _currentTurn.value = gameState.turn
        _gameStatus.value = GameStatus()
    }

    fun isGameOver(): Boolean {
        val status = _gameStatus.value
        return status.isCheckmate || status.isStalemate || status.isDraw
    }

    private fun updateGameStatus(gameState: GameState) {
        _gameStatus.value = GameStatus(
            isCheck = gameState.isCheck,
            isCheckmate = gameState.isCheckmate,
            isStalemate = gameState.isStalemate,
            isDraw = gameState.isDraw
        )
    }

    private fun createInitialBoard(): ChessBoard {
        return ChessBoard(
            fen = ChessConstants.STARTING_POSITION_FEN,
            turn = ChessColor.WHITE,
            castlingRights = CastlingRights(
                whiteKingside = true,
                whiteQueenside = true,
                blackKingside = true,
                blackQueenside = true
            )
        )
    }

    private fun createBoardFromGameState(gameState: GameState): ChessBoard {
        return ChessBoard(
            fen = gameState.toFen(),
            moveHistory = gameState.moveHistory.map { it.move },
            turn = gameState.turn,
            castlingRights = gameState.castlingRights,
            enPassantSquare = gameState.enPassantSquare,
            halfMoveClock = gameState.halfMoveClock,
            fullMoveNumber = gameState.fullMoveNumber
        )
    }
}

data class GameStatus(
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false
)
