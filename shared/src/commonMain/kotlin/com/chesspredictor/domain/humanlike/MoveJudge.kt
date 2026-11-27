package com.chesspredictor.domain.humanlike

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.repositories.ChessEngineRepository
import kotlin.math.exp

fun interface MoveJudge {
    suspend fun winChanceLoss(before: GameState, after: GameState): Double
}

class StockfishJudge(
    private val repository: ChessEngineRepository,
    private val settings: EngineSettings = EngineSettings()
        .withDifficulty(EngineDifficulty.MAXIMUM)
        .copy(timeLimit = 400, analysisDepth = 14, useBook = false, humanStyle = false)
) : MoveJudge {

    private var lastBefore: Pair<String, Double>? = null

    override suspend fun winChanceLoss(before: GameState, after: GameState): Double {
        val fen = before.toFen()
        val best = lastBefore?.takeIf { it.first == fen }?.second
            ?: winChance(repository.analyzePosition(board(before), settings).evaluation).also { lastBefore = fen to it }
        val reply = winChance(repository.analyzePosition(board(after), settings).evaluation)
        return (best - (1 - reply)).coerceAtLeast(0.0)
    }

    private fun board(state: GameState) = ChessBoard(
        fen = state.toFen(),
        turn = state.turn,
        castlingRights = state.castlingRights,
        enPassantSquare = state.enPassantSquare,
        halfMoveClock = state.halfMoveClock,
        fullMoveNumber = state.fullMoveNumber
    )

    private fun winChance(pawns: Float): Double = 1.0 / (1.0 + exp(-WIN_SCALE_PER_PAWN * pawns))

    private companion object {
        const val WIN_SCALE_PER_PAWN = 0.368208
    }
}
