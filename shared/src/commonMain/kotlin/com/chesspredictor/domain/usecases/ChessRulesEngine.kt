package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.components.CheckDetector
import com.chesspredictor.domain.usecases.components.DrawDetector
import com.chesspredictor.domain.usecases.components.MoveExecutor
import com.chesspredictor.domain.usecases.components.MoveGenerator
import com.chesspredictor.domain.usecases.components.MoveValidator
import com.chesspredictor.domain.usecases.components.SANGenerator
import com.chesspredictor.domain.utils.FenParser

class ChessRulesEngine {
    private val fenParser = FenParser()
    private val moveGenerator = MoveGenerator()
    private val moveExecutor = MoveExecutor()
    private val checkDetector = CheckDetector(moveGenerator)
    private val moveValidator = MoveValidator(moveGenerator)
    private val drawDetector = DrawDetector()
    private val sanGenerator = SANGenerator(moveGenerator, checkDetector)
    
    fun parseGameState(fen: String): GameState {
        return fenParser.parseGameState(fen)
    }
    
    fun makeMove(gameState: GameState, move: ChessMove): GameState? {
        // Validate move
        if (!moveValidator.isMoveLegal(gameState, move)) {
            return null
        }
        
        // Execute move
        var newState = moveExecutor.executeMove(gameState, move)
        
        // Update game status
        newState = newState.copy(
            isCheck = checkDetector.isKingInCheck(newState, newState.turn),
            isCheckmate = checkDetector.isCheckmate(newState),
            isStalemate = checkDetector.isStalemate(newState),
            isDraw = drawDetector.isDraw(newState)
        )
        
        // Generate SAN notation and update the last move
        if (newState.moveHistory.isNotEmpty()) {
            val lastMove = newState.moveHistory.last()
            val san = sanGenerator.generateSAN(gameState, move, newState)
            val updatedMove = lastMove.copy(
                isCheck = newState.isCheck,
                isCheckmate = newState.isCheckmate,
                san = san
            )
            val updatedHistory = newState.moveHistory.dropLast(1) + updatedMove
            newState = newState.copy(moveHistory = updatedHistory)
        }
        
        return newState
    }
    
    fun getLegalMoves(gameState: GameState, from: Square? = null): List<ChessMove> {
        val allMoves = moveGenerator.getLegalMoves(gameState)
        return if (from != null) {
            allMoves.filter { it.from == from }
        } else {
            allMoves
        }
    }
    
    fun isKingInCheck(gameState: GameState, color: ChessColor): Boolean {
        return checkDetector.isKingInCheck(gameState, color)
    }
    
    fun isSquareAttacked(gameState: GameState, square: Square, byColor: ChessColor): Boolean {
        return checkDetector.isSquareAttacked(gameState, square, byColor)
    }
    
    fun isCheckmate(gameState: GameState): Boolean {
        return checkDetector.isCheckmate(gameState)
    }
    
    fun isStalemate(gameState: GameState): Boolean {
        return checkDetector.isStalemate(gameState)
    }
    
    fun isDraw(gameState: GameState): Boolean {
        return drawDetector.isDraw(gameState)
    }
    
    fun isInsufficientMaterial(board: Map<Square, ChessPiece>): Boolean {
        // Create a minimal GameState for the call
        val gameState = GameState(
            board = board,
            turn = ChessColor.WHITE,
            castlingRights = CastlingRights(true, true, true, true),
            enPassantSquare = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        return drawDetector.isInsufficientMaterial(gameState)
    }
}