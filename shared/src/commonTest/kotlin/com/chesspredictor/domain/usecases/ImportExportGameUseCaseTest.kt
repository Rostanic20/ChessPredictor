package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.ExportFormat
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.presentation.ChessConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImportExportGameUseCaseTest {
    private val engine = ChessRulesEngine()
    private val useCase = ImportExportGameUseCase(engine)

    private fun play(state: GameState, vararg moves: String): GameState =
        moves.fold(state) { current, uci ->
            val from = Square.fromString(uci.substring(0, 2))
            val to = Square.fromString(uci.substring(2, 4))
            val move = ChessMove(from, to, current.board.getValue(from), current.board[to])
            assertNotNull(engine.makeMove(current, move), "$uci rejected")
        }

    private fun startPosition() = engine.parseGameState(ChessConstants.STARTING_POSITION_FEN)

    private fun importPgn(pgn: String) = useCase.importGame(pgn, ExportFormat.PGN).getOrThrow().gameState

    @Test
    fun exportOfGameFromStartHasNoFenHeader() {
        val pgn = useCase.exportGame(play(startPosition(), "e2e4", "e7e5", "g1f3"), ExportFormat.PGN)
        assertFalse(pgn.contains("[FEN"), pgn)
        assertTrue(pgn.contains("1. e4 e5 2. Nf3"), pgn)
    }

    @Test
    fun pgnRoundTripKeepsEveryMove() {
        val game = play(startPosition(), "e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1")
        val imported = importPgn(useCase.exportGame(game, ExportFormat.PGN))
        assertEquals(game.moveHistory.map { it.san }, imported.moveHistory.map { it.san })
        assertEquals(game.toFen(), imported.toFen())
    }

    @Test
    fun importKeepsCastling() {
        val game = importPgn("[Event \"x\"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O Nf6 *")
        assertEquals(8, game.moveHistory.size)
        assertEquals(ChessPiece.King(ChessColor.WHITE), game.board[Square('g', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), game.board[Square('f', 1)])
    }

    @Test
    fun importAcceptsBareMoveTextWithCommentsVariationsAndAnnotations() {
        val game = importPgn("1.e4 {best by test} e5 \$1 2. Nf3 (2. f4 exf4 3. Nf3) Nc6!? 3. Bb5+ a6 ; Ruy Lopez\n4. Ba4 1-0")
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4"), game.moveHistory.map { it.san })
    }

    @Test
    fun importRejectsAnIllegalMoveInsteadOfSkippingIt() {
        val result = useCase.importGame("1. e4 e5 2. Ke3 *", ExportFormat.PGN)
        assertTrue(result.isFailure)
    }

    @Test
    fun exportFromSetUpPositionWithBlackToMoveRoundTrips() {
        val startFen = "4k3/4p3/8/8/8/8/4P3/4K3 b - - 0 12"
        val game = play(engine.parseGameState(startFen), "e7e5", "e2e4")
        val pgn = useCase.exportGame(game, ExportFormat.PGN)
        assertTrue(pgn.contains("[FEN \"$startFen\"]"), pgn)
        assertTrue(pgn.contains("12... e5 13. e4"), pgn)
        assertEquals(game.toFen(), importPgn(pgn).toFen())
    }

    private fun importJson(json: String) = useCase.importGame(json, ExportFormat.JSON).getOrThrow().gameState

    private fun withoutStartFen(json: String) = json.replace(Regex(",\\s*\"startFen\":\\s*\"[^\"]*\""), "")

    @Test
    fun jsonRoundTripKeepsEveryMove() {
        val game = play(startPosition(), "e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1")
        val imported = importJson(useCase.exportGame(game, ExportFormat.JSON))
        assertEquals(game.moveHistory.map { it.san }, imported.moveHistory.map { it.san })
        assertEquals(game.toFen(), imported.toFen())
    }

    @Test
    fun jsonRoundTripFromSetUpPositionWithPromotion() {
        val start = engine.parseGameState("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
        val promotion = ChessMove(Square('e', 7), Square('e', 8), ChessPiece.Pawn(ChessColor.WHITE), promotion = ChessPiece.Queen(ChessColor.WHITE))
        val game = assertNotNull(engine.makeMove(start, promotion))
        val imported = importJson(useCase.exportGame(game, ExportFormat.JSON))
        assertEquals(listOf("e8=Q+"), imported.moveHistory.map { it.san })
        assertEquals(game.toFen(), imported.toFen())
    }

    @Test
    fun legacyJsonWithoutStartFenStillReplaysAStandardGame() {
        val game = play(startPosition(), "e2e4", "e7e5", "g1f3")
        val legacy = withoutStartFen(useCase.exportGame(game, ExportFormat.JSON))
        assertFalse(legacy.contains("startFen"))
        val imported = importJson(legacy)
        assertEquals(3, imported.moveHistory.size)
        assertEquals(game.toFen(), imported.toFen())
    }

    @Test
    fun legacyJsonFromSetUpPositionKeepsTheFinalPosition() {
        val game = play(engine.parseGameState("4k3/4p3/8/8/8/8/4P3/4K3 b - - 0 12"), "e7e5", "e2e4")
        val imported = importJson(withoutStartFen(useCase.exportGame(game, ExportFormat.JSON)))
        assertEquals(game.toFen(), imported.toFen())
    }

    @Test
    fun jsonWithUnknownKeyMomentTypeStillImports() {
        val json = useCase.exportGame(
            play(startPosition(), "e2e4"),
            ExportFormat.JSON,
            ExportAdditionalData(whiteAccuracy = 90f, keyMoments = emptyList())
        ).replace("\"keyMoments\": []", "\"keyMoments\": [{\"moveNumber\": 1, \"description\": \"x\", \"type\": \"FROM_THE_FUTURE\"}]")
        assertTrue(json.contains("FROM_THE_FUTURE"))
        val result = useCase.importGame(json, ExportFormat.JSON)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(emptyList(), result.getOrThrow().additionalData?.keyMoments)
    }

    @Test
    fun fenImportOfAMatedPositionReportsTheMate() {
        val result = useCase.importGame("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3\n", ExportFormat.FEN)
        assertTrue(result.getOrThrow().gameState.isCheckmate)
    }

    @Test
    fun knightShuffleIsDrawnOnTheThirdOccurrence() {
        val twice = play(startPosition(), "g1f3", "g8f6", "f3g1", "f6g8")
        assertFalse(twice.isDraw)
        assertTrue(play(twice, "g1f3", "g8f6", "f3g1", "f6g8").isDraw)
    }
}
