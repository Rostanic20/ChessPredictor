package com.chesspredictor.domain.elo

import com.chesspredictor.domain.entities.*
import kotlin.test.*
import kotlin.random.Random

/**
 * Validates that ELO difficulty settings are realistic and properly configured
 */
class EloValidationTest {
    
    @Test
    fun testEloSettingsRealism() {
        println("🎯 ELO Settings Realism Test")
        println("=" * 50)
        
        val results = mutableListOf<EloValidationResult>()
        
        EngineDifficulty.values().filter { it != EngineDifficulty.CUSTOM }.forEach { difficulty ->
            val result = validateEloDifficulty(difficulty)
            results.add(result)
            printValidationResult(result)
        }
        
        // Overall assessment
        println("\n📊 OVERALL ASSESSMENT")
        println("=" * 50)
        
        val passedTests = results.count { it.overallScore >= 0.7f }
        val totalTests = results.size
        
        println("Passed: $passedTests/$totalTests ELO levels")
        
        if (passedTests == totalTests) {
            println("✅ ALL ELO LEVELS APPEAR REALISTIC")
        } else {
            println("⚠️ Some ELO levels may need adjustment")
            results.filter { it.overallScore < 0.7f }.forEach {
                println("  ❌ ${it.difficulty.displayName}: ${it.overallScore.toPercentage()} realistic")
            }
        }
        
        // Verify progression is logical
        validateEloProgression(results)
    }
    
    @Test 
    fun testMistakeRateSimulation() {
        println("\n🎲 Mistake Rate Simulation Test")
        println("=" * 50)
        
        // Test each difficulty level's mistake rates over many moves
        EngineDifficulty.values().filter { it != EngineDifficulty.CUSTOM }.forEach { difficulty ->
            val simulationResult = simulateMistakeRates(difficulty, 1000)
            printMistakeSimulation(difficulty, simulationResult)
        }
    }
    
    @Test
    fun testPositionComplexityHandling() {
        println("\n🧩 Position Complexity Handling Test")
        println("=" * 50)
        
        val complexities = listOf(0.1f, 0.5f, 0.9f) // simple, medium, complex
        
        EngineDifficulty.values().filter { it != EngineDifficulty.CUSTOM }.take(3).forEach { difficulty ->
            println("\n${difficulty.displayName}:")
            complexities.forEach { complexity ->
                val handling = testComplexityHandling(difficulty, complexity)
                val complexityName = when {
                    complexity < 0.3f -> "Simple"
                    complexity < 0.7f -> "Medium" 
                    else -> "Complex"
                }
                println("  $complexityName positions: ${handling.toPercentage()}")
            }
        }
    }
    
    private data class EloValidationResult(
        val difficulty: EngineDifficulty,
        val skillLevelRealism: Float,
        val blunderRateRealism: Float,
        val inaccuracyRateRealism: Float,
        val depthRealism: Float,
        val overallScore: Float,
        val issues: List<String>
    )
    
    private data class MistakeSimulation(
        val expectedBlunders: Float,
        val actualBlunders: Float,
        val expectedInaccuracies: Float, 
        val actualInaccuracies: Float,
        val accuracy: Float
    )
    
    private fun validateEloDifficulty(difficulty: EngineDifficulty): EloValidationResult {
        val issues = mutableListOf<String>()
        
        // Validate skill level for ELO
        val skillRealism = validateSkillLevel(difficulty.eloRating, difficulty.skillLevel).also {
            if (it < 0.7f) issues.add("Skill level too high for ELO rating")
        }
        
        // Validate blunder rate for ELO  
        val blunderRealism = validateBlunderRate(difficulty.eloRating, difficulty.blunderRate).also {
            if (it < 0.7f) issues.add("Blunder rate unrealistic for ELO rating")
        }
        
        // Validate inaccuracy rate for ELO
        val inaccuracyRealism = validateInaccuracyRate(difficulty.eloRating, difficulty.inaccuracyRate).also {
            if (it < 0.7f) issues.add("Inaccuracy rate unrealistic for ELO rating")
        }
        
        // Validate search depth for ELO
        val depthRealism = validateSearchDepth(difficulty.eloRating, difficulty.depth).also {
            if (it < 0.7f) issues.add("Search depth too high for ELO rating")
        }
        
        val overallScore = (skillRealism + blunderRealism + inaccuracyRealism + depthRealism) / 4f
        
        return EloValidationResult(
            difficulty = difficulty,
            skillLevelRealism = skillRealism,
            blunderRateRealism = blunderRealism,
            inaccuracyRateRealism = inaccuracyRealism,
            depthRealism = depthRealism,
            overallScore = overallScore,
            issues = issues
        )
    }
    
    private fun validateSkillLevel(eloRating: Int, skillLevel: Int): Float {
        // Based on Stockfish research and Chess.com data
        val expectedSkill = when {
            eloRating < 900 -> 2..4    // Novice level
            eloRating < 1100 -> 3..5   // Beginner level  
            eloRating < 1300 -> 4..6   // Casual level
            eloRating < 1500 -> 5..7   // Intermediate level
            eloRating < 1700 -> 7..9   // Club player level
            eloRating < 1900 -> 9..11  // Strong club level
            eloRating < 2100 -> 11..13 // Expert level
            eloRating < 2300 -> 13..15 // Master level
            else -> 15..17             // Maximum level
        }
        
        return if (skillLevel in expectedSkill) 1.0f else {
            val distance = minOf(
                kotlin.math.abs(skillLevel - expectedSkill.first),
                kotlin.math.abs(skillLevel - expectedSkill.last)
            )
            (1f - distance / 10f).coerceAtLeast(0f)
        }
    }
    
    private fun validateBlunderRate(eloRating: Int, blunderRate: Float): Float {
        // Based on Chess.com community research data
        val expectedRate = when {
            eloRating < 900 -> 0.18f..0.25f  // High blunder rate for beginners
            eloRating < 1100 -> 0.12f..0.18f // Learning players make many blunders
            eloRating < 1300 -> 0.10f..0.15f // Recreational level
            eloRating < 1500 -> 0.06f..0.10f // Club beginner
            eloRating < 1700 -> 0.04f..0.07f // Average club player
            eloRating < 1900 -> 0.025f..0.04f // Strong club - blunders become rare
            eloRating < 2100 -> 0.015f..0.025f // Expert level - very few blunders
            eloRating < 2300 -> 0.008f..0.015f // Master strength
            else -> 0.003f..0.008f           // Near-perfect play
        }
        
        return if (blunderRate in expectedRate) 1.0f else {
            val distance = minOf(
                kotlin.math.abs(blunderRate - expectedRate.start),
                kotlin.math.abs(blunderRate - expectedRate.endInclusive)
            )
            (1f - distance * 10f).coerceAtLeast(0f)
        }
    }
    
    private fun validateInaccuracyRate(eloRating: Int, inaccuracyRate: Float): Float {
        // Based on Chess.com accuracy research - realistic human patterns
        val expectedRate = when {
            eloRating < 900 -> 0.35f..0.45f  // Frequent suboptimal moves
            eloRating < 1100 -> 0.25f..0.35f // Many inaccuracies
            eloRating < 1300 -> 0.20f..0.30f // Recreational play
            eloRating < 1500 -> 0.16f..0.24f // Improving club player
            eloRating < 1700 -> 0.12f..0.18f // Solid club level
            eloRating < 1900 -> 0.10f..0.15f // Strong play with some inaccuracies
            eloRating < 2100 -> 0.06f..0.10f // Expert precision
            eloRating < 2300 -> 0.04f..0.07f // Master level accuracy
            else -> 0.015f..0.025f           // Near-master precision
        }
        
        return if (inaccuracyRate in expectedRate) 1.0f else {
            val distance = minOf(
                kotlin.math.abs(inaccuracyRate - expectedRate.start),
                kotlin.math.abs(inaccuracyRate - expectedRate.endInclusive)
            )
            (1f - distance * 2f).coerceAtLeast(0f)
        }
    }
    
    private fun validateSearchDepth(eloRating: Int, depth: Int): Float {
        // Expected search depths (humans can't search very deep)
        val expectedDepth = when {
            eloRating < 900 -> 4..7
            eloRating < 1100 -> 5..8
            eloRating < 1300 -> 6..9
            eloRating < 1500 -> 7..10
            eloRating < 1700 -> 8..11
            eloRating < 1900 -> 9..13
            eloRating < 2100 -> 11..15
            eloRating < 2300 -> 13..17
            else -> 15..20
        }
        
        return if (depth in expectedDepth) 1.0f else {
            val distance = minOf(
                kotlin.math.abs(depth - expectedDepth.first),
                kotlin.math.abs(depth - expectedDepth.last)
            )
            (1f - distance / 5f).coerceAtLeast(0f)
        }
    }
    
    private fun simulateMistakeRates(difficulty: EngineDifficulty, numMoves: Int): MistakeSimulation {
        var actualBlunders = 0
        var actualInaccuracies = 0
        
        repeat(numMoves) {
            val complexity = Random.nextFloat() // Random position complexity
            val adjustedBlunderRate = difficulty.blunderRate * (1f + complexity * 0.5f)
            val adjustedInaccuracyRate = difficulty.inaccuracyRate * (1f + complexity * 0.3f)
            
            val random = Random.nextFloat()
            when {
                random < adjustedBlunderRate -> actualBlunders++
                random < adjustedBlunderRate + adjustedInaccuracyRate -> actualInaccuracies++
            }
        }
        
        val expectedBlunders = difficulty.blunderRate * numMoves * 1.25f // Account for complexity factor
        val expectedInaccuracies = difficulty.inaccuracyRate * numMoves * 1.15f
        
        val blunderAccuracy = 1f - kotlin.math.abs(actualBlunders - expectedBlunders) / expectedBlunders
        val inaccuracyAccuracy = 1f - kotlin.math.abs(actualInaccuracies - expectedInaccuracies) / expectedInaccuracies
        val overallAccuracy = (blunderAccuracy + inaccuracyAccuracy) / 2f
        
        return MistakeSimulation(
            expectedBlunders = expectedBlunders,
            actualBlunders = actualBlunders.toFloat(),
            expectedInaccuracies = expectedInaccuracies,
            actualInaccuracies = actualInaccuracies.toFloat(),
            accuracy = overallAccuracy
        )
    }
    
    private fun testComplexityHandling(difficulty: EngineDifficulty, complexity: Float): Float {
        // Test how well the difficulty handles different position complexities
        val baseBlunderRate = difficulty.blunderRate
        val baseInaccuracyRate = difficulty.inaccuracyRate
        
        val adjustedBlunderRate = baseBlunderRate * (1f + complexity * 0.5f)
        val adjustedInaccuracyRate = baseInaccuracyRate * (1f + complexity * 0.3f)
        
        // Calculate appropriateness - complex positions should have higher mistake rates
        val blunderIncrease = adjustedBlunderRate / baseBlunderRate
        val inaccuracyIncrease = adjustedInaccuracyRate / baseInaccuracyRate
        
        val expectedIncrease = 1f + complexity * 0.4f // Expected average increase
        val blunderScore = 1f - kotlin.math.abs(blunderIncrease - expectedIncrease) / expectedIncrease
        val inaccuracyScore = 1f - kotlin.math.abs(inaccuracyIncrease - expectedIncrease) / expectedIncrease
        
        return (blunderScore + inaccuracyScore) / 2f
    }
    
    private fun validateEloProgression(results: List<EloValidationResult>) {
        println("\n📈 ELO PROGRESSION VALIDATION")
        println("=" * 50)
        
        val sortedByElo = results.sortedBy { it.difficulty.eloRating }
        var progressionValid = true
        
        for (i in 1 until sortedByElo.size) {
            val lower = sortedByElo[i-1]
            val higher = sortedByElo[i]
            
            val issues = mutableListOf<String>()
            
            // Skill level should generally increase
            if (higher.difficulty.skillLevel <= lower.difficulty.skillLevel) {
                issues.add("Skill level doesn't increase")
            }
            
            // Blunder rate should generally decrease
            if (higher.difficulty.blunderRate >= lower.difficulty.blunderRate) {
                issues.add("Blunder rate doesn't decrease")
            }
            
            // Search depth should generally increase
            if (higher.difficulty.depth < lower.difficulty.depth) {
                issues.add("Search depth decreases")
            }
            
            if (issues.isNotEmpty()) {
                println("⚠️ ${lower.difficulty.displayName} → ${higher.difficulty.displayName}: ${issues.joinToString(", ")}")
                progressionValid = false
            }
        }
        
        if (progressionValid) {
            println("✅ ELO progression is logical")
        } else {
            println("❌ ELO progression has issues")
        }
    }
    
    private fun printValidationResult(result: EloValidationResult) {
        val status = when {
            result.overallScore >= 0.9f -> "✅ EXCELLENT"
            result.overallScore >= 0.8f -> "✅ GOOD"
            result.overallScore >= 0.7f -> "⚠️ ACCEPTABLE"
            else -> "❌ NEEDS WORK"
        }
        
        println("\n${result.difficulty.displayName} (${result.difficulty.eloRating} ELO): $status")
        println("  Overall Realism: ${result.overallScore.toPercentage()}")
        println("  Skill Level: ${result.skillLevelRealism.toPercentage()} (${result.difficulty.skillLevel})")
        println("  Blunder Rate: ${result.blunderRateRealism.toPercentage()} (${(result.difficulty.blunderRate * 100).formatDecimal(1)}%)")
        println("  Inaccuracy Rate: ${result.inaccuracyRateRealism.toPercentage()} (${(result.difficulty.inaccuracyRate * 100).formatDecimal(1)}%)")
        println("  Search Depth: ${result.depthRealism.toPercentage()} (${result.difficulty.depth})")
        
        if (result.issues.isNotEmpty()) {
            println("  Issues: ${result.issues.joinToString(", ")}")
        }
    }
    
    private fun printMistakeSimulation(difficulty: EngineDifficulty, simulation: MistakeSimulation) {
        println("\n${difficulty.displayName}:")
        println("  Blunders: ${simulation.actualBlunders.toInt()}/1000 (expected: ${simulation.expectedBlunders.toInt()})")
        println("  Inaccuracies: ${simulation.actualInaccuracies.toInt()}/1000 (expected: ${simulation.expectedInaccuracies.toInt()})")
        println("  Simulation Accuracy: ${simulation.accuracy.toPercentage()}")
    }
    
    private fun Float.toPercentage(): String = "${(this * 100).toInt()}%"
    private fun Float.formatDecimal(decimals: Int): String {
        return when (decimals) {
            0 -> this.toInt().toString()
            1 -> (kotlin.math.round(this * 10) / 10).toString()
            else -> this.toString()
        }
    }
}

private operator fun String.times(count: Int): String = repeat(count)