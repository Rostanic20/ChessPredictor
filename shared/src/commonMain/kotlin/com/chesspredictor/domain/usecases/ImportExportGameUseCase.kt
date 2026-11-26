package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.AnalysisData
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.ExportedGame
import com.chesspredictor.domain.entities.ExportedKeyMoment
import com.chesspredictor.domain.entities.ExportedMove
import com.chesspredictor.domain.entities.ExportFormat
import com.chesspredictor.domain.entities.GameData
import com.chesspredictor.domain.entities.GameSettings
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.KeyMoment
import com.chesspredictor.domain.entities.KeyMomentType
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.opposite
import com.chesspredictor.domain.utils.FenValidator
import com.chesspredictor.utils.TimeProvider
import com.chesspredictor.presentation.ChessConstants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.abs

class ImportExportGameUseCase(
    private val chessRulesEngine: ChessRulesEngine = ChessRulesEngine()
) {
    private companion object {
        val PGN_RESULTS = setOf("1-0", "0-1", "1/2-1/2", "*")
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    fun exportGame(
        gameState: GameState,
        format: ExportFormat,
        additionalData: ExportAdditionalData? = null
    ): String {
        return when (format) {
            ExportFormat.PGN -> exportAsPGN(gameState, additionalData)
            ExportFormat.JSON -> exportAsJSON(gameState, additionalData)
            ExportFormat.FEN -> gameState.toFen()
        }
    }
    
    fun importGame(
        content: String,
        format: ExportFormat
    ): Result<ImportedGameData> {
        return try {
            when (format) {
                ExportFormat.PGN -> importFromPGN(content)
                ExportFormat.JSON -> importFromJSON(content)
                ExportFormat.FEN -> importFromFEN(content)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.message ?: "unreadable game data", e))
        }
    }
    
    private fun exportAsPGN(gameState: GameState, additionalData: ExportAdditionalData?): String {
        val pgn = StringBuilder()
        
        pgn.appendLine("[Event \"${additionalData?.event ?: "Chess Game"}\"]")
        pgn.appendLine("[Site \"${additionalData?.site ?: "ChessPredictor App"}\"]")
        pgn.appendLine("[Date \"${additionalData?.date ?: getCurrentDate()}\"]")
        pgn.appendLine("[Round \"${additionalData?.round ?: "-"}\"]")
        pgn.appendLine("[White \"${additionalData?.white ?: "Player"}\"]")
        pgn.appendLine("[Black \"${additionalData?.black ?: "Engine"}\"]")
        pgn.appendLine("[Result \"${getGameResult(gameState, additionalData)}\"]")
        
        val startFen = gameState.startingFen()
        if (startFen != ChessConstants.STARTING_POSITION_FEN) {
            pgn.appendLine("[FEN \"$startFen\"]")
            pgn.appendLine("[SetUp \"1\"]")
        }
        
        additionalData?.let { data ->
            data.whiteAccuracy?.let { pgn.appendLine("[WhiteAccuracy \"$it\"]") }
            data.blackAccuracy?.let { pgn.appendLine("[BlackAccuracy \"$it\"]") }
            data.openingName?.let { pgn.appendLine("[Opening \"$it\"]") }
            data.openingEco?.let { pgn.appendLine("[ECO \"$it\"]") }
            data.timeControl?.let { pgn.appendLine("[TimeControl \"$it\"]") }
        }
        
        pgn.appendLine()
        
        val start = chessRulesEngine.parseGameState(startFen)
        val blackMovesFirst = start.turn == ChessColor.BLACK
        gameState.moveHistory.forEachIndexed { index, move ->
            val ply = if (blackMovesFirst) index + 1 else index
            val moveNumber = start.fullMoveNumber + ply / 2
            when {
                ply % 2 == 0 -> pgn.append("$moveNumber. ")
                index == 0 -> pgn.append("$moveNumber... ")
            }

            pgn.append(move.san)
            
            additionalData?.moveComments?.get(index)?.let { comment ->
                pgn.append(" {$comment}")
            }
            
            additionalData?.evaluations?.getOrNull(index + 1)?.let { eval ->
                val evalStr = (eval / 100f).let { value ->
                    val rounded = (value * 100).toInt() / 100f
                    rounded.toString()
                }
                pgn.append(" [%eval $evalStr]")
            }
            
            pgn.append(" ")
        }
        
        pgn.append(getGameResult(gameState, additionalData))
        
        return pgn.toString().trim()
    }
    
    private fun exportAsJSON(gameState: GameState, additionalData: ExportAdditionalData?): String {
        val exportedMoves = gameState.moveHistory.mapIndexed { index, move ->
            ExportedMove(
                san = move.san,
                from = "${move.move.from}",
                to = "${move.move.to}",
                piece = getPieceTypeName(move.move.piece),
                captured = move.move.capturedPiece?.let { getPieceTypeName(it) },
                promotion = move.promotion?.let { getPieceTypeName(it) },
                isCheck = move.isCheck,
                isCheckmate = move.isCheckmate,
                timeSpent = move.timeSpent,
                evaluation = additionalData?.evaluations?.getOrNull(index + 1),
                comment = additionalData?.moveComments?.get(index)
            )
        }
        
        val gameData = GameData(
            event = additionalData?.event ?: "Chess Game",
            site = additionalData?.site ?: "ChessPredictor App",
            date = additionalData?.date ?: getCurrentDate(),
            round = additionalData?.round ?: "-",
            white = additionalData?.white ?: "Player",
            black = additionalData?.black ?: "Engine",
            result = getGameResult(gameState, additionalData),
            fen = gameState.toFen(),
            moves = exportedMoves,
            currentPly = gameState.fullMoveNumber * 2 - (if (gameState.turn == ChessColor.WHITE) 2 else 1),
            timeControl = additionalData?.timeControl,
            startFen = gameState.startingFen()
        )
        
        val analysisData = if (additionalData?.whiteAccuracy != null || additionalData?.blackAccuracy != null) {
            AnalysisData(
                whiteAccuracy = additionalData.whiteAccuracy ?: 0f,
                blackAccuracy = additionalData.blackAccuracy ?: 0f,
                evaluations = additionalData.evaluations ?: listOf(),
                keyMoments = additionalData.keyMoments?.map { moment ->
                    ExportedKeyMoment(
                        moveNumber = moment.moveNumber,
                        description = moment.description,
                        type = moment.type.name
                    )
                } ?: listOf(),
                openingName = additionalData.openingName,
                openingEco = additionalData.openingEco
            )
        } else null
        
        val settings = GameSettings(
            playMode = additionalData?.playMode ?: "VS_ENGINE",
            playerColor = additionalData?.playerColor ?: "WHITE",
            engineDifficulty = additionalData?.engineDifficulty ?: "MEDIUM",
            timeControl = additionalData?.timeControl
        )
        
        val exportedGame = ExportedGame(
            exportDate = getCurrentDateTime(),
            gameData = gameData,
            analysis = analysisData,
            settings = settings
        )
        
        return json.encodeToString(exportedGame)
    }
    
    private fun importFromPGN(pgn: String): Result<ImportedGameData> {
        val headers = mutableMapOf<String, String>()
        val moveText = StringBuilder()
        val headerPattern = Regex("^\\[(\\w+)\\s+\"(.*)\"\\]$")

        pgn.lines().forEach { rawLine ->
            val line = rawLine.trim()
            val header = headerPattern.find(line)
            if (header != null) {
                headers[header.groupValues[1]] = header.groupValues[2]
            } else {
                moveText.append(line.substringBefore(';')).append(' ')
            }
        }

        var gameState = parseUserFen(headers["FEN"] ?: ChessConstants.STARTING_POSITION_FEN)

        for ((index, san) in pgnMoveTokens(moveText.toString()).withIndex()) {
            val move = parseSANMove(san, gameState)
                ?: return Result.failure(IllegalArgumentException("Unrecognized move '$san' at ply ${index + 1}"))
            gameState = chessRulesEngine.makeMove(gameState, move)
                ?: return Result.failure(IllegalArgumentException("Illegal move '$san' at ply ${index + 1}"))
        }

        val additionalData = ExportAdditionalData(
            event = headers["Event"],
            site = headers["Site"],
            date = headers["Date"],
            round = headers["Round"],
            white = headers["White"],
            black = headers["Black"],
            whiteAccuracy = headers["WhiteAccuracy"]?.toFloatOrNull(),
            blackAccuracy = headers["BlackAccuracy"]?.toFloatOrNull(),
            openingName = headers["Opening"],
            openingEco = headers["ECO"],
            timeControl = headers["TimeControl"]
        )
        
        return Result.success(
            ImportedGameData(
                gameState = gameState,
                additionalData = additionalData
            )
        )
    }
    
    private fun importFromJSON(jsonStr: String): Result<ImportedGameData> {
        val exportedGame = json.decodeFromString<ExportedGame>(jsonStr)
        val data = exportedGame.gameData

        val gameState = if (data.startFen != null) {
            replayExportedMoves(data.startFen, data.moves)
                ?: return Result.failure(IllegalArgumentException("Moves do not replay from the start position"))
        } else {
            val finalPosition = chessRulesEngine.withStatus(parseUserFen(data.fen))
            replayExportedMoves(ChessConstants.STARTING_POSITION_FEN, data.moves)
                ?.takeIf { it.toPositionKey() == finalPosition.toPositionKey() }
                ?: finalPosition
        }

        val additionalData = ExportAdditionalData(
            event = exportedGame.gameData.event,
            site = exportedGame.gameData.site,
            date = exportedGame.gameData.date,
            round = exportedGame.gameData.round,
            white = exportedGame.gameData.white,
            black = exportedGame.gameData.black,
            whiteAccuracy = exportedGame.analysis?.whiteAccuracy,
            blackAccuracy = exportedGame.analysis?.blackAccuracy,
            evaluations = exportedGame.analysis?.evaluations,
            openingName = exportedGame.analysis?.openingName,
            openingEco = exportedGame.analysis?.openingEco,
            timeControl = exportedGame.gameData.timeControl,
            playMode = exportedGame.settings.playMode,
            playerColor = exportedGame.settings.playerColor,
            engineDifficulty = exportedGame.settings.engineDifficulty,
            moveComments = exportedGame.gameData.moves
                .mapIndexedNotNull { index, move -> 
                    move.comment?.let { index to it }
                }.toMap(),
            keyMoments = exportedGame.analysis?.keyMoments?.mapNotNull { km ->
                val type = KeyMomentType.entries.find { it.name == km.type } ?: return@mapNotNull null
                KeyMoment(
                    moveNumber = km.moveNumber,
                    description = km.description,
                    evaluationSwing = 0f,
                    type = type
                )
            }
        )
        
        return Result.success(
            ImportedGameData(
                gameState = gameState,
                additionalData = additionalData
            )
        )
    }
    
    private fun parseUserFen(fen: String): GameState {
        FenValidator.problemWith(fen)?.let { throw IllegalArgumentException("Invalid FEN: $it") }
        val state = chessRulesEngine.parseGameState(fen.trim())
        require(!chessRulesEngine.isKingInCheck(state, state.turn.opposite())) {
            "Invalid FEN: the side not to move is in check"
        }
        return state
    }

    private fun replayExportedMoves(startFen: String, moves: List<ExportedMove>): GameState? {
        var gameState = parseUserFen(startFen)
        for (exportedMove in moves) {
            val from = Square.fromString(exportedMove.from)
            val to = Square.fromString(exportedMove.to)
            val piece = gameState.board[from] ?: return null
            val promotion = when (exportedMove.promotion) {
                "QUEEN" -> ChessPiece.Queen(gameState.turn)
                "ROOK" -> ChessPiece.Rook(gameState.turn)
                "BISHOP" -> ChessPiece.Bishop(gameState.turn)
                "KNIGHT" -> ChessPiece.Knight(gameState.turn)
                else -> null
            }
            val move = ChessMove(from, to, piece, gameState.board[to], promotion)
            gameState = chessRulesEngine.makeMove(gameState, move) ?: return null
        }
        return gameState
    }

    private fun importFromFEN(fen: String): Result<ImportedGameData> =
        Result.success(ImportedGameData(gameState = chessRulesEngine.withStatus(parseUserFen(fen))))
    
    private fun pgnMoveTokens(moveText: String): List<String> {
        val mainLine = StringBuilder()
        var variationDepth = 0
        var inComment = false
        for (char in moveText) {
            when {
                inComment -> if (char == '}') inComment = false
                char == '{' -> inComment = true
                char == '(' -> variationDepth++
                char == ')' -> if (variationDepth > 0) variationDepth--
                variationDepth == 0 -> mainLine.append(char)
            }
        }

        return mainLine.split(Regex("\\s+"))
            .map { it.replace(Regex("^\\d+\\.+"), "") }
            .filter { it.isNotEmpty() && !it.startsWith("$") && it !in PGN_RESULTS }
            .map { it.trimEnd('!', '?').replace("0-0-0", "O-O-O").replace("0-0", "O-O") }
            .filter { it.isNotEmpty() }
    }

    private fun parseSANMove(san: String, gameState: GameState): ChessMove? {
        val wanted = normalizeSan(san)
        return chessRulesEngine.getLegalMoves(gameState).find { move ->
            val moveState = chessRulesEngine.makeMove(gameState, move)
            moveState?.moveHistory?.lastOrNull()?.san?.let(::normalizeSan) == wanted
        }
    }

    private fun normalizeSan(san: String): String = san.trimEnd('+', '#')

    private fun getGameResult(gameState: GameState, additionalData: ExportAdditionalData?): String {
        additionalData?.result?.let { return it }
        return when {
            gameState.isCheckmate -> {
                if (gameState.turn == ChessColor.WHITE) "0-1" else "1-0"
            }
            gameState.isDraw || gameState.isStalemate -> "1/2-1/2"
            else -> "*"
        }
    }
    
    private fun getCurrentDate(): String {
        val now = TimeProvider.currentTimeMillis()
        val date = Instant.fromEpochMilliseconds(now)
            .toString().substring(0, 10).replace("-", ".")
        return date
    }
    
    private fun getCurrentDateTime(): String {
        return Clock.System.now().toString()
    }
    
    private fun getPieceTypeName(piece: ChessPiece): String {
        return when (piece) {
            is ChessPiece.Pawn -> "PAWN"
            is ChessPiece.Knight -> "KNIGHT"
            is ChessPiece.Bishop -> "BISHOP"
            is ChessPiece.Rook -> "ROOK"
            is ChessPiece.Queen -> "QUEEN"
            is ChessPiece.King -> "KING"
        }
    }
}

data class ExportAdditionalData(
    val event: String? = null,
    val site: String? = null,
    val date: String? = null,
    val round: String? = null,
    val white: String? = null,
    val black: String? = null,
    val whiteAccuracy: Float? = null,
    val blackAccuracy: Float? = null,
    val evaluations: List<Float>? = null,
    val keyMoments: List<KeyMoment>? = null,
    val openingName: String? = null,
    val openingEco: String? = null,
    val timeControl: String? = null,
    val playMode: String? = null,
    val playerColor: String? = null,
    val engineDifficulty: String? = null,
    val moveComments: Map<Int, String>? = null,
    val result: String? = null
)

data class ImportedGameData(
    val gameState: GameState,
    val additionalData: ExportAdditionalData? = null
)