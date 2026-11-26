package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessOpening
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.PopularMove
import com.chesspredictor.domain.utils.ChessConstants

class OpeningDetector {

    companion object {
        private const val MAX_OPENING_PLIES = 30
        private const val MIN_FAMILY_PLIES = 2
    }

    private val openingDatabase = AdvancedOpeningDatabase.openingDatabase
    private val rulesEngine = ChessRulesEngine()

    private val linePositions: List<Pair<ChessOpening, String>> by lazy {
        openingDatabase.flatMap { opening ->
            (listOf(opening.moves) + opening.transpositions).mapNotNull { line ->
                finalPositionKey(line)?.let { opening to it }
            }
        }
    }

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
        if (moveHistory.isEmpty()) return OpeningInfo(null)

        val sanMoves = moveHistory.map { detailedMove ->
            detailedMove.san.ifEmpty { convertMoveToSimpleNotation(detailedMove.move) }
        }
        val gamePositions = gamePositionKeys(moveHistory.take(MAX_OPENING_PLIES)) ?: return OpeningInfo(null)

        reachedOpening(gamePositions)?.let { opening ->
            val playedInBookOrder = getMatchingMoveCount(sanMoves, opening.moves) == opening.moves.size
            return createOpeningInfo(opening, sanMoves, !playedInBookOrder, opening.moves.size)
        }
        return familyOpening(sanMoves)
    }

    private fun positionKey(state: GameState): String = state.toFen().split(" ").take(3).joinToString(" ")

    private fun finalPositionKey(sans: List<String>): String? {
        var state = rulesEngine.parseGameState(ChessConstants.STARTING_FEN)
        for (san in sans) {
            val move = SanMoveMatcher.match(san, rulesEngine.getLegalMoves(state), state.turn) ?: return null
            state = rulesEngine.makeMove(state, move) ?: return null
        }
        return positionKey(state)
    }

    private fun gamePositionKeys(moves: List<DetailedMove>): List<String>? {
        var state = rulesEngine.parseGameState(ChessConstants.STARTING_FEN)
        return moves.map { detailed ->
            val piece = state.board[detailed.move.from] ?: return null
            state = rulesEngine.makeMove(state, detailed.move.copy(piece = piece)) ?: return null
            positionKey(state)
        }
    }

    private fun reachedOpening(gamePositions: List<String>): ChessOpening? {
        val lastPlyOf = gamePositions.withIndex().associate { (ply, key) -> key to ply }
        return linePositions
            .mapNotNull { (opening, key) -> lastPlyOf[key]?.let { ply -> opening to ply } }
            .maxWithOrNull(
                compareBy<Pair<ChessOpening, Int>> { it.second }
                    .thenBy { it.first.moves.size }
                    .thenBy { it.first.popularity }
            )
            ?.first
    }

    private fun familyOpening(sanMoves: List<String>): OpeningInfo {
        val matches = openingDatabase.map { it to getMatchingMoveCount(sanMoves, it.moves) }
        val longest = matches.maxOfOrNull { it.second } ?: 0
        if (longest < MIN_FAMILY_PLIES) return OpeningInfo(null)

        val candidates = matches.filter { it.second == longest }.map { it.first }
        val family = candidates.map { it.name.substringBefore(':').trim() }.distinct().singleOrNull()
            ?: return OpeningInfo(null)
        val ecos = candidates.map { it.eco }.toSet()
        val familyEco = ecoHierarchy.entries.find { (_, children) -> children.containsAll(ecos) }?.key ?: ""

        return OpeningInfo(
            opening = ChessOpening(name = family, eco = familyEco, moves = candidates.first().moves.take(longest)),
            moveNumber = longest,
            nextPopularMoves = findContinuations(sanMoves).take(3)
        )
    }

    private fun createOpeningInfo(
        opening: ChessOpening,
        sanMoves: List<String>,
        isTransposition: Boolean,
        moveNumber: Int
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
        val baseEval = opening.statistics.whiteWinRate - opening.statistics.blackWinRate
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
}