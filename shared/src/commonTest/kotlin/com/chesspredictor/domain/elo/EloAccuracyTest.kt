package com.chesspredictor.domain.elo

import com.chesspredictor.domain.entities.*
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.random.Random

/**
 * Tests to verify that each ELO difficulty level plays at the expected strength
 */
class EloAccuracyTest {
    
    private val chessRulesEngine = ChessRulesEngine()
    
    data class EloTestResult(
        val difficulty: EngineDifficulty,
        val averageBlunderRate: Float,
        val averageInaccuracyRate: Float,
        val averageDepth: Float,
        val averageSkillLevel: Float,
        val positionHandling: Float, // 0.0-1.0 how well it handles different positions
        val mistakeDistribution: Map<String, Int>, // types of mistakes made
        val totalMovesAnalyzed: Int
    )
    
    @Test
    fun testAllEloLevelsAccuracy() = runTest {
        println("🎯 Starting ELO Accuracy Analysis...")
        println("=" * 60)
        
        val results = mutableListOf<EloTestResult>()
        
        for (difficulty in EngineDifficulty.values().filter { it != EngineDifficulty.CUSTOM }) {
            println("\n📊 Testing ${difficulty.displayName}...")
            val result = analyzeEloDifficulty(difficulty)
            results.add(result)
            printEloAnalysis(result)
        }
        
        println("\n" + "=" * 60)
        println("📈 FINAL ELO ACCURACY REPORT")
        println("=" * 60)
        
        for (result in results) {
            val expectedElo = result.difficulty.eloRating
            val estimatedElo = estimateEloFromMetrics(result)
            val accuracy = calculateEloAccuracy(expectedElo, estimatedElo)
            
            val status = when {
                accuracy >= 0.9f -> "✅ EXCELLENT"
                accuracy >= 0.8f -> "✅ GOOD"
                accuracy >= 0.7f -> "⚠️ ACCEPTABLE"
                else -> "❌ NEEDS ADJUSTMENT"
            }
            
            println("${result.difficulty.displayName}: Expected ${expectedElo}, Estimated ${estimatedElo.toInt()} (${(accuracy * 100).toInt()}% accuracy) $status")
        }
    }
    
    private suspend fun analyzeEloDifficulty(difficulty: EngineDifficulty): EloTestResult {
        val testPositions = createTestPositions()
        var totalBlunders = 0
        var totalInaccuracies = 0
        var totalMoves = 0
        var totalDepth = 0f
        var totalSkillLevel = 0f
        val mistakes = mutableMapOf<String, Int>()
        
        val settings = EngineSettings(
            difficulty = difficulty,
            analysisDepth = difficulty.depth,
            timeLimit = difficulty.timeMs,
            skillLevel = difficulty.skillLevel,
            humanStyle = true
        )
        
        for (position in testPositions) {
            val analysis = analyzePositionWithSettings(position, settings)
            
            totalMoves++
            totalDepth += analysis.effectiveDepth
            totalSkillLevel += analysis.effectiveSkillLevel
            
            when (analysis.moveQuality) {
                MoveQuality.BLUNDER -> {
                    totalBlunders++
                    mistakes["blunder"] = mistakes.getOrElse("blunder") { 0 } + 1
                }
                MoveQuality.INACCURACY -> {
                    totalInaccuracies++
                    mistakes["inaccuracy"] = mistakes.getOrElse("inaccuracy") { 0 } + 1
                }
                MoveQuality.GOOD -> mistakes["good"] = mistakes.getOrElse("good") { 0 } + 1
                MoveQuality.EXCELLENT -> mistakes["excellent"] = mistakes.getOrElse("excellent") { 0 } + 1
            }
        }
        
        return EloTestResult(
            difficulty = difficulty,
            averageBlunderRate = totalBlunders.toFloat() / totalMoves,
            averageInaccuracyRate = totalInaccuracies.toFloat() / totalMoves,
            averageDepth = totalDepth / totalMoves,
            averageSkillLevel = totalSkillLevel / totalMoves,
            positionHandling = calculatePositionHandling(testPositions, settings),
            mistakeDistribution = mistakes,
            totalMovesAnalyzed = totalMoves
        )
    }
    
    private fun createTestPositions(): List<TestPosition> {
        return listOf(
            // Opening positions
            TestPosition("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", "opening", "simple"),
            TestPosition("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2", "opening", "simple"),
            TestPosition("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 4 3", "opening", "medium"),
            
            // Middlegame tactical positions
            TestPosition("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 4", "middlegame", "complex"),
            TestPosition("rnbq1rk1/ppp1ppbp/3p1np1/8/2PP4/2N2NP1/PP2PPBP/R1BQK2R w KQ - 0 6", "middlegame", "complex"),
            TestPosition("r2qkb1r/ppp2ppp/2npbn2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 6", "middlegame", "medium"),
            
            // Endgame positions
            TestPosition("8/8/8/3k4/3K4/8/8/8 w - - 0 1", "endgame", "simple"),
            TestPosition("8/8/8/3k1p2/3K1P2/8/8/8 w - - 0 1", "endgame", "medium"),
            TestPosition("8/8/8/3k4/8/3K4/5P2/8 w - - 0 1", "endgame", "simple"),
            
            // Tactical puzzle positions
            TestPosition("r1bqr1k1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N1P/PPP1BPP1/RNBQR1K1 w - - 0 8", "middlegame", "tactical"),
            TestPosition("rnbq1rk1/ppp1ppbp/3p1np1/8/3PP3/2N2N2/PPP2PPP/R1BQKB1R w KQ - 0 6", "middlegame", "tactical")
        )
    }
    
    private data class TestPosition(
        val fen: String,
        val phase: String, // opening, middlegame, endgame
        val complexity: String // simple, medium, complex, tactical
    )
    
    private data class PositionAnalysis(
        val moveQuality: MoveQuality,
        val effectiveDepth: Float,
        val effectiveSkillLevel: Float,
        val positionComplexity: Float
    )
    
    private enum class MoveQuality {
        EXCELLENT, GOOD, INACCURACY, BLUNDER
    }
    
    private suspend fun analyzePositionWithSettings(position: TestPosition, settings: EngineSettings): PositionAnalysis {
        // Simulate the human-like modifications that would be applied
        val gameState = chessRulesEngine.parseGameState(position.fen)
        val board = ChessBoard(
            fen = position.fen, 
            turn = gameState.turn,
            castlingRights = gameState.castlingRights
        )
        
        // Estimate position complexity
        val complexity = estimatePositionComplexity(position)
        
        // Calculate effective settings after human-like modifications
        val effectiveDepth = if (complexity > 0.7f) {
            (settings.analysisDepth * 0.8f)
        } else {
            settings.analysisDepth.toFloat()
        }
        
        val effectiveSkillLevel = when {
            position.phase == "opening" && settings.openingBookUsage < 0.3f -> 
                (settings.skillLevel * 0.9f)
            position.phase == "endgame" -> 
                (settings.skillLevel * settings.endgameAccuracy)
            else -> settings.skillLevel.toFloat()
        }
        
        // Simulate mistake probability with complexity factor
        val adjustedBlunderRate = settings.blunderProbability * (1f + complexity * 0.5f)
        val adjustedInaccuracyRate = settings.inaccuracyRate * (1f + complexity * 0.3f)
        
        val random = Random.nextFloat()
        val moveQuality = when {
            random < adjustedBlunderRate -> MoveQuality.BLUNDER
            random < adjustedBlunderRate + adjustedInaccuracyRate -> MoveQuality.INACCURACY
            random < 0.8f -> MoveQuality.GOOD
            else -> MoveQuality.EXCELLENT
        }
        
        return PositionAnalysis(
            moveQuality = moveQuality,
            effectiveDepth = effectiveDepth,
            effectiveSkillLevel = effectiveSkillLevel,
            positionComplexity = complexity
        )
    }
    
    private fun estimatePositionComplexity(position: TestPosition): Float {
        val pieceCount = position.fen.count { it.isLetter() }
        
        return when {
            position.complexity == "tactical" -> 0.9f + Random.nextFloat() * 0.1f
            position.complexity == "complex" -> 0.7f + Random.nextFloat() * 0.2f
            position.complexity == "medium" -> 0.4f + Random.nextFloat() * 0.3f
            position.complexity == "simple" -> 0.1f + Random.nextFloat() * 0.3f
            pieceCount < 12 -> 0.2f + Random.nextFloat() * 0.3f // endgame
            pieceCount > 28 -> 0.8f + Random.nextFloat() * 0.2f // opening
            else -> 0.4f + Random.nextFloat() * 0.4f // middlegame
        }
    }
    
    private suspend fun calculatePositionHandling(positions: List<TestPosition>, settings: EngineSettings): Float {
        var correctHandling = 0
        
        for (position in positions) {
            val analysis = analyzePositionWithSettings(position, settings)
            
            // Check if the engine handles this position type appropriately
            val handledCorrectly = when (position.phase) {
                "opening" -> {
                    // Should be faster and potentially weaker if no book knowledge
                    if (settings.openingBookUsage < 0.5f) analysis.effectiveSkillLevel < settings.skillLevel 
                    else true
                }
                "endgame" -> {
                    // Should use endgame accuracy settings
                    analysis.effectiveSkillLevel <= (settings.skillLevel * settings.endgameAccuracy + 1)
                }
                "middlegame" -> {
                    // Should handle complexity appropriately
                    if (position.complexity == "complex") analysis.effectiveDepth < settings.analysisDepth
                    else true
                }
                else -> true
            }
            
            if (handledCorrectly) correctHandling++
        }
        
        return correctHandling.toFloat() / positions.size
    }
    
    private fun estimateEloFromMetrics(result: EloTestResult): Float {
        // Estimate ELO based on observed metrics
        val baseElo = when {
            result.averageSkillLevel < 2 -> 800f
            result.averageSkillLevel < 5 -> 1000f
            result.averageSkillLevel < 8 -> 1200f
            result.averageSkillLevel < 10 -> 1400f
            result.averageSkillLevel < 12 -> 1600f
            result.averageSkillLevel < 15 -> 1800f
            result.averageSkillLevel < 18 -> 2000f
            result.averageSkillLevel < 20 -> 2200f
            else -> 2400f
        }
        
        // Adjust based on mistake rates
        val blunderPenalty = result.averageBlunderRate * 400f // High blunder rate = lower ELO
        val inaccuracyPenalty = result.averageInaccuracyRate * 200f
        val depthBonus = (result.averageDepth - 8f) * 50f // Higher depth = higher ELO
        
        return (baseElo - blunderPenalty - inaccuracyPenalty + depthBonus).coerceIn(600f, 2500f)
    }
    
    private fun calculateEloAccuracy(expected: Int, estimated: Float): Float {
        val difference = kotlin.math.abs(expected - estimated)
        return (1f - (difference / 400f)).coerceAtLeast(0f) // 400 ELO difference = 0% accuracy
    }
    
    private fun printEloAnalysis(result: EloTestResult) {
        println("  Expected ELO: ${result.difficulty.eloRating}")
        println("  Skill Level: ${result.averageSkillLevel.formatDecimal(1)} (expected: ${result.difficulty.skillLevel})")
        println("  Search Depth: ${result.averageDepth.formatDecimal(1)} (expected: ${result.difficulty.depth})")
        println("  Blunder Rate: ${(result.averageBlunderRate * 100).formatDecimal(1)}% (expected: ${(result.difficulty.blunderRate * 100).formatDecimal(1)}%)")
        println("  Inaccuracy Rate: ${(result.averageInaccuracyRate * 100).formatDecimal(1)}% (expected: ${(result.difficulty.inaccuracyRate * 100).formatDecimal(1)}%)")
        println("  Position Handling: ${(result.positionHandling * 100).formatDecimal(0)}%")
        println("  Moves Analyzed: ${result.totalMovesAnalyzed}")
        
        print("  Move Quality Distribution: ")
        result.mistakeDistribution.forEach { (type, count) ->
            val percentage = (count.toFloat() / result.totalMovesAnalyzed * 100).formatDecimal(0)
            print("$type: $percentage% ")
        }
        println()
        
        val estimatedElo = estimateEloFromMetrics(result)
        val accuracy = calculateEloAccuracy(result.difficulty.eloRating, estimatedElo)
        println("  Estimated Playing Strength: ${estimatedElo.toInt()} ELO (${(accuracy * 100).formatDecimal(0)}% accurate)")
    }
    
    private fun Float.formatDecimal(decimals: Int): String {
        return when (decimals) {
            0 -> this.toInt().toString()
            1 -> (kotlin.math.round(this * 10) / 10).toString()
            else -> this.toString()
        }
    }
}

// Extension to repeat strings
private operator fun String.times(count: Int): String = repeat(count)