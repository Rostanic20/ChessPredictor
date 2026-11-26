package com.chesspredictor.presentation.managers

import com.chesspredictor.data.repositories.SavedGameCodec
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameEnding
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoveControllerTest {
    private val engine = ChessRulesEngine()
    private val gameStateManager = GameStateManager(engine)
    private val controller = MoveController(engine, gameStateManager, AnimationController(AnimationManager()))

    private val setUpFen = "4k3/1P2p3/8/8/8/8/4P3/4K3 w - - 0 30"

    private fun play(state: GameState, vararg moves: String): GameState =
        moves.fold(state) { current, uci ->
            val from = Square.fromString(uci.substring(0, 2))
            val to = Square.fromString(uci.substring(2, 4))
            val promotion = if (uci.length == 5) ChessPiece.Queen(current.turn) else null
            val move = ChessMove(from, to, current.board.getValue(from), current.board[to], promotion)
            assertNotNull(engine.makeMove(current, move), "$uci rejected")
        }

    @Test
    fun undoInAGameSetUpFromFenGoesBackToThatGameNotTheStandardStart() {
        val start = engine.parseGameState(setUpFen)
        gameStateManager.updateFromGameState(play(start, "e2e4", "e7e5"))

        assertTrue(controller.undoLastMove())

        assertEquals(play(start, "e2e4").toFen(), gameStateManager.currentGameState?.toFen())
    }

    @Test
    fun restoreReplaysSavedMovesIncludingPromotion() {
        val game = play(engine.parseGameState(setUpFen), "b7b8q", "e8d7")
        val saved = SavedGameCodec.decodeMoves(SavedGameCodec.encodeMoves(game.moveHistory))

        val restored = controller.restoreGame(setUpFen, saved, game.toFen())

        assertEquals(game.moveHistory.map { it.san }, restored.moveHistory.map { it.san })
        assertEquals(game.toFen(), restored.toFen())
        assertEquals(setUpFen, restored.startingFen())
    }

    @Test
    fun restoreOfAFinishedGameKeepsItsResult() {
        val start = engine.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val mated = play(start, "f2f3", "e7e5", "g2g4", "d8h4")
        assertTrue(mated.isCheckmate)

        val restored = controller.restoreGame(start.toFen(), mated.moveHistory, mated.toFen())

        assertTrue(restored.isCheckmate)
        assertEquals(4, restored.moveHistory.size)
    }

    @Test
    fun restoreFallsBackToTheSavedPositionWithItsStatusWhenMovesDoNotReplay() {
        val mated = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
        val unreplayable = SavedGameCodec.decodeMoves("e2|e4|e4|PW|1|true")

        val restored = controller.restoreGame(setUpFen, unreplayable, mated)

        assertEquals(mated, restored.toFen())
        assertTrue(restored.isCheckmate)
        assertEquals(0, restored.moveHistory.size)
    }

    @Test
    fun noMoveIsAcceptedAfterAFlagEvenAPromotion() {
        gameStateManager.updateFromGameState(engine.parseGameState(setUpFen))
        gameStateManager.endGame(GameEnding.WHITE_OUT_OF_TIME, isDraw = false)

        assertNull(controller.makeMove(Square.fromString("b7"), Square.fromString("b8"), ChessPiece.Queen(ChessColor.WHITE)))
        assertEquals(GameEnding.WHITE_OUT_OF_TIME, gameStateManager.gameStatus.value.ending)
    }

    @Test
    fun legacySaveWithoutPromotionFieldStillDecodes() {
        val decoded = SavedGameCodec.decodeMoves("e2|e4|e4|PW|1|true;b7|b8|b8=Q|PW|30|true")
        assertEquals(2, decoded.size)
        assertEquals(null, decoded[1].move.promotion)
        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), decoded[1].move.piece)
    }
}
