package com.chesspredictor.presentation.managers

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

class MoveController(
    private val chessRulesEngine: ChessRulesEngine,
    private val gameStateManager: GameStateManager,
    private val animationController: AnimationController
) {
    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    private val _possibleMoves = MutableStateFlow<List<Square>>(emptyList())
    val possibleMoves: StateFlow<List<Square>> = _possibleMoves.asStateFlow()

    fun selectSquare(square: Square, currentTurn: ChessColor, boardState: Map<Square, ChessPiece>) {
        val piece = boardState[square]
        if (piece != null && piece.color == currentTurn) {
            val gameState = gameStateManager.currentGameState ?: return
            val legalMoves = chessRulesEngine.getLegalMoves(gameState, square)
            _selectedSquare.value = square
            _possibleMoves.value = legalMoves.map { it.to }
        }
    }

    fun clearSelection() {
        _selectedSquare.value = null
        _possibleMoves.value = emptyList()
    }

    fun isSquareSelected(): Boolean = _selectedSquare.value != null

    fun isMoveValid(square: Square): Boolean = _possibleMoves.value.contains(square)

    fun getSelectedSquare(): Square? = _selectedSquare.value

    fun makeMove(from: Square, to: Square, promotionPiece: ChessPiece? = null): MoveResult? {
        if (animationController.isAnimating()) return null

        val gameState = gameStateManager.currentGameState ?: return null
        val piece = gameState.board[from] ?: return null

        val promotion = promotionPiece ?: if (piece is ChessPiece.Pawn && (to.rank == 8 || to.rank == 1)) {
            ChessPiece.Queen(piece.color)
        } else {
            null
        }

        val move = ChessMove(
            from = from,
            to = to,
            piece = piece,
            capturedPiece = gameState.board[to],
            promotion = promotion
        )

        val newGameState = chessRulesEngine.makeMove(gameState, move) ?: return null

        clearSelection()

        return MoveResult(
            move = move,
            oldGameState = gameState,
            newGameState = newGameState
        )
    }

    fun undoLastMove(): Boolean {
        if (animationController.isAnimating()) return false

        val moves = gameStateManager.moveHistory.value
        if (moves.isEmpty()) return false

        val currentState = gameStateManager.currentGameState ?: return false
        if (moves.size != currentState.moveHistory.size) return false

        val movesToKeep = moves.dropLast(1)
        rebuildPositionFromMoves(movesToKeep)
        return true
    }

    fun rebuildPositionFromMoves(moves: List<DetailedMove>) {
        val gameState = replayMoves(moves)
        gameStateManager.updateFromGameState(gameState)
        gameStateManager.setLastMove(gameState.moveHistory.lastOrNull()?.move)
        animationController.clearAnimations()
    }

    fun reconstructMoveHistory(savedMoves: List<DetailedMove>): List<DetailedMove> {
        if (savedMoves.isEmpty()) return emptyList()

        val reconstructedMoves = mutableListOf<DetailedMove>()
        var currentState = getInitialGameState()

        for ((index, savedMove) in savedMoves.withIndex()) {
            val correctPiece = currentState.board[savedMove.move.from] ?: break
            val correctedMove = savedMove.copy(
                move = savedMove.move.copy(piece = correctPiece),
                moveNumber = index + 1,
                isWhiteMove = currentState.turn == ChessColor.WHITE
            )

            val newState = chessRulesEngine.makeMove(currentState, correctedMove.move) ?: break
            currentState = newState
            reconstructedMoves.add(correctedMove)
        }
        return reconstructedMoves
    }

    private fun getInitialGameState(): GameState =
        chessRulesEngine.parseGameState(ChessConstants.STARTING_POSITION_FEN)

    private fun replayMoves(moves: List<DetailedMove>): GameState {
        var gameState = getInitialGameState()
        for (detailedMove in moves) {
            gameState = chessRulesEngine.makeMove(gameState, detailedMove.move) ?: break
        }
        return gameState
    }
}

data class MoveResult(
    val move: ChessMove,
    val oldGameState: GameState,
    val newGameState: GameState
)
