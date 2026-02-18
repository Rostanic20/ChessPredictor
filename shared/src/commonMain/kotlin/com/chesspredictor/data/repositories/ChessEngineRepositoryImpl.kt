package com.chesspredictor.data.repositories

import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineAnalysis
import com.chesspredictor.domain.entities.EngineEvaluation
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.domain.repositories.PositionAnalysis
import com.chesspredictor.domain.usecases.SimpleOpeningBook
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChessEngineRepositoryImpl(
    private val engineDataSource: ChessEngineDataSource,
    private val moveParser: MoveParser
) : ChessEngineRepository {

    companion object {
        private const val TAG = "ChessEngineRepository"
    }

    private val openingBook = SimpleOpeningBook()
    
    override suspend fun analyzeBestMove(
        board: ChessBoard,
        settings: EngineSettings
    ): ChessMove? {
        if (settings.useBook && board.moveHistory.size < 15) {
            val bookMove = openingBook.getBookMove(board.fen, settings.skillLevel)
            if (bookMove != null) {
                val move = moveParser.parseUciMove(bookMove, board)
                if (move != null) return move
            }
        }

        engineDataSource.setPosition(board.fen)
        val analysis = engineDataSource.analyze(settings)
        return analysis.bestMove.takeIf { it.isNotEmpty() }?.let {
            moveParser.parseUciMove(it, board)
        }
    }

    override suspend fun analyzePosition(
        board: ChessBoard,
        settings: EngineSettings
    ): PositionAnalysis {
        engineDataSource.setPosition(board.fen)
        val analysis = engineDataSource.analyze(settings)
        
        val bestMove = analysis.bestMove.takeIf { it.isNotEmpty() }?.let {
            moveParser.parseUciMove(it, board)
        }
        
        val pvMoves = analysis.principalVariation.mapNotNull {
            moveParser.parseUciMove(it, board)
        }
        
        return PositionAnalysis(
            bestMove = bestMove,
            evaluation = analysis.evaluation,
            principalVariation = pvMoves
        )
    }

    override suspend fun analyzeWithEvaluation(
        board: ChessBoard,
        settings: EngineSettings
    ): EngineAnalysis? {
        if (settings.useBook && board.moveHistory.size < 10) {
            val bookMove = openingBook.getBookMove(board.fen, settings.skillLevel)
            if (bookMove != null) {
                val move = moveParser.parseUciMove(bookMove, board)
                if (move != null) {
                    return EngineAnalysis(
                        bestMove = move,
                        evaluation = EngineEvaluation(score = 0.0, mate = null, isMateScore = false),
                        principalVariation = listOf(bookMove),
                        depth = 0,
                        nodes = 0,
                        time = 100 + kotlin.random.Random.nextInt(300).toLong()
                    )
                }
            }
        }
        
        engineDataSource.setPosition(board.fen)
        val analysis = engineDataSource.analyze(settings)
        
        val bestMove = if (analysis.bestMove.isNotEmpty()) {
            val parsedMove = moveParser.parseUciMove(analysis.bestMove, board)
            if (parsedMove == null) {
                ChessLogger.warning(TAG, "Failed to parse best move: ${analysis.bestMove}")
                null
            } else {
                parsedMove
            }
        } else {
            ChessLogger.warning(TAG, "Engine returned empty best move")
            null
        }
        
        val evaluation = EngineEvaluation(
            score = analysis.evaluation.toDouble(),
            mate = analysis.mate,
            isMateScore = analysis.mate != null
        )
        
        if (bestMove == null) {
            ChessLogger.warning(TAG, "No valid best move available for position: ${board.fen}")
            return null
        }
        
        return EngineAnalysis(
            bestMove = bestMove,
            evaluation = evaluation,
            principalVariation = analysis.principalVariation,
            depth = analysis.depth,
            nodes = analysis.nodes,
            time = analysis.time
        )
    }
    
    override suspend fun validateMove(board: ChessBoard, move: ChessMove): Boolean {
        val legalMoves = getAvailableMoves(board)
        return legalMoves.contains(move)
    }

    override suspend fun getAvailableMoves(board: ChessBoard): List<ChessMove> {
        return emptyList()
    }

    override fun observeEvaluation(): Flow<Float> = flow {
        emit(0f)
    }
}

interface MoveParser {
    fun parseUciMove(uciMove: String, board: ChessBoard): ChessMove?
    fun toUciMove(move: ChessMove): String
}