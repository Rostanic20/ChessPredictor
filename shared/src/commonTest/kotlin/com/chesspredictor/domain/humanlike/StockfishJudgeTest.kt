package com.chesspredictor.domain.humanlike

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineAnalysis
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.domain.repositories.PositionAnalysis
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StockfishJudgeTest {

    private class Evaluations(private val byFen: Map<String, Float>) : ChessEngineRepository {
        val settingsSeen = mutableListOf<EngineSettings>()

        override suspend fun analyzePosition(board: ChessBoard, settings: EngineSettings): PositionAnalysis {
            settingsSeen += settings
            return PositionAnalysis(bestMove = null, evaluation = byFen.getValue(board.fen))
        }

        override suspend fun analyzeBestMove(board: ChessBoard, settings: EngineSettings): ChessMove? = null
        override suspend fun analyzeWithEvaluation(board: ChessBoard, settings: EngineSettings): EngineAnalysis? = null
        override suspend fun validateMove(board: ChessBoard, move: ChessMove) = true
        override suspend fun getAvailableMoves(board: ChessBoard) = emptyList<ChessMove>()
        override fun observeEvaluation(): Flow<Float> = emptyFlow()
    }

    private val rules = ChessRulesEngine()
    private val before = rules.parseGameState("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
    private val after = requireNotNull(rules.makeMove(before, rules.getLegalMoves(before).first { it.toUci() == "f7f6" }))

    @Test
    fun theLossIsTheMoversDropInWinningChances() = runTest {
        val repository = Evaluations(mapOf(before.toFen() to 0.0f, after.toFen() to 3.0f))

        val loss = StockfishJudge(repository).winChanceLoss(before, after)

        assertEquals(0.5 - 1 / (1 + kotlin.math.exp(0.368208 * 3.0)), loss, 1e-6)
    }

    @Test
    fun aMoveThatImprovesThePositionCostsNothing() = runTest {
        val repository = Evaluations(mapOf(before.toFen() to 0.0f, after.toFen() to -999f))

        assertEquals(0.0, StockfishJudge(repository).winChanceLoss(before, after))
    }

    @Test
    fun thePositionBeforeTheMoveIsAnalysedOnce() = runTest {
        val other = requireNotNull(rules.makeMove(before, rules.getLegalMoves(before).first { it.toUci() == "g7g5" }))
        val repository = Evaluations(mapOf(before.toFen() to 0.0f, after.toFen() to 0.0f, other.toFen() to 0.0f))
        val judge = StockfishJudge(repository)

        judge.winChanceLoss(before, after)
        judge.winChanceLoss(before, other)

        assertEquals(3, repository.settingsSeen.size)
    }

    @Test
    fun theJudgeSearchesAtFullStrength() = runTest {
        val repository = Evaluations(mapOf(before.toFen() to 0.0f, after.toFen() to 0.0f))

        StockfishJudge(repository).winChanceLoss(before, after)

        assertEquals(listOf(EngineDifficulty.MAXIMUM, EngineDifficulty.MAXIMUM), repository.settingsSeen.map { it.difficulty })
        assertEquals(listOf(false, false), repository.settingsSeen.map { it.useBook })
    }
}
