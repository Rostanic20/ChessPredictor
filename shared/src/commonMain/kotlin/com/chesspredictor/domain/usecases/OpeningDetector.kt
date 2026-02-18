package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessOpening
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.OpeningCategory
import com.chesspredictor.domain.entities.OpeningDifficulty
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.OpeningStatistics
import com.chesspredictor.domain.entities.OpeningTheme
import com.chesspredictor.domain.entities.PopularMove
import com.chesspredictor.domain.entities.Square

class OpeningDetector {

    companion object {
        private const val MAX_CACHE_SIZE = 256
    }

    private val openingDatabase = AdvancedOpeningDatabase.openingDatabase

    private val transpositionCache = linkedMapOf<String, ChessOpening>()
    
    private val ecoHierarchy = mapOf(
        "C60" to listOf("C65", "C68", "C77", "C89", "C95"),
        "C50" to listOf("C51", "C53", "C55"),
        "B20" to listOf("B22", "B25", "B33", "B35", "B70", "B80", "B90"),
        "D20" to listOf("D26", "D30", "D41", "D63"),
        "E60" to listOf("E68", "E81", "E97"),
        "E20" to listOf("E32"),
        "A10" to listOf("A15", "A20", "A30")
    )
    
    fun detectOpening(moveHistory: List<DetailedMove>): OpeningInfo {
        if (moveHistory.isEmpty()) {
            return OpeningInfo(null)
        }
        
        val sanMoves = moveHistory.map { detailedMove ->
            detailedMove.san.ifEmpty {
                convertMoveToSimpleNotation(detailedMove.move)
            }
        }
        
        val positionKey = sanMoves.joinToString(",")
        transpositionCache[positionKey]?.let { cachedOpening ->
            return createOpeningInfo(cachedOpening, sanMoves, true)
        }

        val primaryResult = detectPrimaryOpening(sanMoves)
        if (primaryResult.opening != null) {
            putCache(positionKey, primaryResult.opening)
            return primaryResult
        }

        val transpositionResult = detectTranspositions(sanMoves)
        if (transpositionResult.opening != null) {
            putCache(positionKey, transpositionResult.opening)
            return transpositionResult
        }
        
        return detectPartialMatch(sanMoves)
    }
    
    private fun putCache(key: String, opening: ChessOpening) {
        if (transpositionCache.size >= MAX_CACHE_SIZE) {
            val eldest = transpositionCache.keys.first()
            transpositionCache.remove(eldest)
        }
        transpositionCache[key] = opening
    }

    private fun detectPrimaryOpening(sanMoves: List<String>): OpeningInfo {
        if (openingDatabase.isEmpty()) return OpeningInfo(null)

        val sortedOpenings = openingDatabase.sortedWith(
            compareByDescending<ChessOpening> { it.moves.size }
                .thenByDescending { it.popularity }
                .thenBy { if (it.isMainLine) 0 else 1 }
        )

        for (opening in sortedOpenings) {
            val matchLength = getMatchingMoveCount(sanMoves, opening.moves)
            if (matchLength == opening.moves.size && matchLength >= 2) {
                return createOpeningInfo(opening, sanMoves, false, matchLength)
            }
        }
        
        return OpeningInfo(null)
    }
    
    private fun detectTranspositions(sanMoves: List<String>): OpeningInfo {
        for (opening in openingDatabase) {
            for (transposition in opening.transpositions) {
                if (isTransposition(sanMoves, transposition)) {
                    return createOpeningInfo(opening, sanMoves, true)
                }
            }
            
            if (couldTransposeTo(sanMoves, opening.moves)) {
                return createOpeningInfo(opening, sanMoves, true)
            }
        }
        
        return OpeningInfo(null)
    }
    
    private fun detectPartialMatch(sanMoves: List<String>): OpeningInfo {
        var bestMatch: ChessOpening? = null
        var longestMatch = 0
        
        val sortedOpenings = openingDatabase.sortedWith(
            compareByDescending<ChessOpening> { it.popularity }
                .thenBy { if (it.isMainLine) 0 else 1 }
                .thenByDescending { it.moves.size }
        )
        
        for (opening in sortedOpenings) {
            val matchLength = getMatchingMoveCount(sanMoves, opening.moves)
            if (matchLength > longestMatch && matchLength >= 2) {
                longestMatch = matchLength
                bestMatch = opening
            }
        }
        
        return if (bestMatch != null) {
            createOpeningInfo(bestMatch, sanMoves, false, longestMatch)
        } else {
            OpeningInfo(null)
        }
    }
    
    private fun createOpeningInfo(
        opening: ChessOpening, 
        sanMoves: List<String>, 
        isTransposition: Boolean,
        moveNumber: Int = sanMoves.size
    ): OpeningInfo {
        return OpeningInfo(
            opening = opening,
            moveNumber = moveNumber,
            transposition = isTransposition,
            nextPopularMoves = getNextPopularMoves(opening, sanMoves),
            positionEvaluation = calculatePositionEvaluation(opening, sanMoves.size),
            variation = determineVariation(opening, moveNumber)
        )
    }
    
    private fun isTransposition(gameMoves: List<String>, transpositionMoves: List<String>): Boolean {
        if (gameMoves.size < transpositionMoves.size) return false
        
        val gameMovesSet = gameMoves.toSet()
        val transpositionSet = transpositionMoves.toSet()
        
        return transpositionSet.all { it in gameMovesSet }
    }
    
    private fun couldTransposeTo(gameMoves: List<String>, targetMoves: List<String>): Boolean {
        if (gameMoves.size < 3) return false

        val keyMoves = targetMoves.take(4)
        val gameMovesSet = gameMoves.toSet()
        
        return keyMoves.count { it in gameMovesSet } >= keyMoves.size - 1
    }
    
    private fun getNextPopularMoves(opening: ChessOpening, currentMoves: List<String>): List<PopularMove> {
        val nextMoveIndex = currentMoves.size
        
        if (nextMoveIndex < opening.moves.size) {
            val nextMove = opening.moves[nextMoveIndex]
            return listOf(
                PopularMove(
                    move = nextMove,
                    frequency = 85.0f,
                    performance = opening.statistics.whiteWinRate + opening.statistics.drawRate * 0.5f,
                    isTheoretical = true
                )
            )
        }
        
        val continuations = findContinuations(currentMoves)
        return continuations.take(3)
    }

    private fun findContinuations(currentMoves: List<String>): List<PopularMove> {
        val continuations = mutableListOf<PopularMove>()
        
        for (other in openingDatabase) {
            if (other.moves.size > currentMoves.size && 
                other.moves.take(currentMoves.size) == currentMoves) {
                
                val nextMove = other.moves[currentMoves.size]
                val existing = continuations.find { it.move == nextMove }
                
                if (existing == null) {
                    continuations.add(
                        PopularMove(
                            move = nextMove,
                            frequency = other.popularity,
                            performance = other.statistics.whiteWinRate + other.statistics.drawRate * 0.5f,
                            isTheoretical = true
                        )
                    )
                }
            }
        }
        
        return continuations.sortedByDescending { it.frequency }
    }
    
    private fun calculatePositionEvaluation(opening: ChessOpening, moveCount: Int): Float {
        val baseEval = (opening.statistics.whiteWinRate - opening.statistics.blackWinRate) / 100f
        val phaseAdjustment = when {
            moveCount <= 3 -> 0.8f
            moveCount <= 6 -> 0.9f
            moveCount <= 10 -> 1.0f
            else -> 1.1f
        }
        
        return baseEval * phaseAdjustment
    }
    
    private fun determineVariation(opening: ChessOpening, moveNumber: Int): String? {
        return when {
            moveNumber == opening.moves.size -> null
            moveNumber < opening.moves.size -> "In progress"
            else -> "Extended"
        }
    }
    
    private fun getMatchingMoveCount(gameMoves: List<String>, openingMoves: List<String>): Int {
        var count = 0
        val maxMoves = minOf(gameMoves.size, openingMoves.size)
        
        for (i in 0 until maxMoves) {
            if (normalizeMove(gameMoves[i]) == normalizeMove(openingMoves[i])) {
                count++
            } else {
                break
            }
        }
        
        return count
    }
    
    private fun convertMoveToSimpleNotation(move: ChessMove): String {
        return when (move.piece) {
            is ChessPiece.Pawn -> {
                if (move.capturedPiece != null) {
                    "${move.from.file}x${move.to}"
                } else {
                    "${move.to}"
                }
            }
            is ChessPiece.Knight -> "N${move.to}"
            is ChessPiece.Bishop -> "B${move.to}" 
            is ChessPiece.Rook -> "R${move.to}"
            is ChessPiece.Queen -> "Q${move.to}"
            is ChessPiece.King -> {
                when {
                    move.from.file == 'e' && move.to.file == 'g' -> "O-O"
                    move.from.file == 'e' && move.to.file == 'c' -> "O-O-O"
                    else -> "K${move.to}"
                }
            }
        }
    }
    
    private fun normalizeMove(move: String): String {
        return move.replace(Regex("[+#!?]"), "").trim()
    }
    
    fun getAllOpenings(): List<ChessOpening> = openingDatabase
    
    fun getOpeningsByCategory(category: OpeningCategory): List<ChessOpening> {
        return openingDatabase.filter { it.category == category }
            .sortedByDescending { it.popularity }
    }
    
    fun getOpeningsByDifficulty(difficulty: OpeningDifficulty): List<ChessOpening> {
        return openingDatabase.filter { it.difficulty == difficulty }
            .sortedByDescending { it.popularity }
    }
    
    fun getPopularOpenings(limit: Int = 10): List<ChessOpening> {
        return openingDatabase.sortedByDescending { it.popularity }.take(limit)
    }
    
    fun getTrendingOpenings(limit: Int = 10): List<ChessOpening> {
        return openingDatabase.filter { it.statistics.recentTrend > 0 }
            .sortedByDescending { it.statistics.recentTrend }
            .take(limit)
    }
    
    fun getOpeningsByTheme(theme: OpeningTheme): List<ChessOpening> {
        return openingDatabase.filter { theme in it.themes }
            .sortedByDescending { it.popularity }
    }
    
    fun searchOpenings(query: String): List<ChessOpening> {
        val lowerQuery = query.lowercase()
        return openingDatabase.filter { 
            it.name.lowercase().contains(lowerQuery) ||
            it.eco.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery)
        }.sortedByDescending { it.popularity }
    }
    
    fun getOpeningStatistics(eco: String): OpeningStatistics? {
        return openingDatabase.find { it.eco == eco }?.statistics
    }
    
    fun getVariationsOf(parentEco: String): List<ChessOpening> {
        val variations = ecoHierarchy[parentEco] ?: emptyList()
        return openingDatabase.filter { it.eco in variations }
            .sortedByDescending { it.popularity }
    }
    
    fun getParentOpening(childEco: String): ChessOpening? {
        val parentEco = ecoHierarchy.entries.find { 
            childEco in it.value 
        }?.key ?: return null
        
        return openingDatabase.find { it.eco == parentEco }
    }
    
    fun analyzeOpeningRepertoire(playerMoves: List<List<String>>): OpeningRepertoireAnalysis {
        val openingCounts = mutableMapOf<String, Int>()
        val categoryDistribution = mutableMapOf<OpeningCategory, Int>()
        val difficultyDistribution = mutableMapOf<OpeningDifficulty, Int>()
        
        for (gameMoves in playerMoves) {
            val openingInfo = detectOpening(gameMoves.mapIndexed { index, san -> 
                DetailedMove(
                    move = ChessMove(Square('a', 1), Square('a', 1), ChessPiece.Pawn(ChessColor.WHITE)),
                    moveNumber = (index / 2) + 1,
                    isWhiteMove = index % 2 == 0,
                    san = san,
                    previousCastlingRights = CastlingRights(true, true, true, true),
                    previousEnPassantSquare = null
                )
            })
            
            openingInfo.opening?.let { opening ->
                openingCounts[opening.eco] = (openingCounts[opening.eco] ?: 0) + 1
                categoryDistribution[opening.category] = (categoryDistribution[opening.category] ?: 0) + 1
                difficultyDistribution[opening.difficulty] = (difficultyDistribution[opening.difficulty] ?: 0) + 1
            }
        }
        
        return OpeningRepertoireAnalysis(
            totalGames = playerMoves.size,
            openingFrequency = openingCounts,
            categoryDistribution = categoryDistribution,
            difficultyDistribution = difficultyDistribution,
            averageDifficulty = calculateAverageDifficulty(difficultyDistribution),
            diversityScore = calculateDiversityScore(openingCounts, playerMoves.size)
        )
    }
    
    private fun calculateAverageDifficulty(distribution: Map<OpeningDifficulty, Int>): Float {
        val total = distribution.values.sum()
        if (total == 0) return 0f
        
        val weightedSum = distribution.entries.sumOf { (difficulty, count) ->
            val weight = when (difficulty) {
                OpeningDifficulty.BEGINNER -> 1
                OpeningDifficulty.INTERMEDIATE -> 2  
                OpeningDifficulty.ADVANCED -> 3
                OpeningDifficulty.EXPERT -> 4
            }
            weight * count
        }
        
        return weightedSum.toFloat() / total
    }
    
    private fun calculateDiversityScore(openingCounts: Map<String, Int>, totalGames: Int): Float {
        if (totalGames == 0) return 0f
        
        val entropy = openingCounts.values.sumOf { count ->
            val p = count.toDouble() / totalGames
            if (p > 0) -p * kotlin.math.ln(p) else 0.0
        }
        
        return (entropy / kotlin.math.ln(openingCounts.size.toDouble())).toFloat()
    }
}

data class OpeningRepertoireAnalysis(
    val totalGames: Int,
    val openingFrequency: Map<String, Int>,
    val categoryDistribution: Map<OpeningCategory, Int>,
    val difficultyDistribution: Map<OpeningDifficulty, Int>,
    val averageDifficulty: Float,
    val diversityScore: Float
)