package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.GameEnding
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.AnimationController
import com.chesspredictor.presentation.managers.GameClock
import com.chesspredictor.presentation.managers.GameStateManager
import com.chesspredictor.presentation.managers.GameStatus
import com.chesspredictor.presentation.managers.MoveController
import com.chesspredictor.presentation.managers.PieceAnimation
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ChessBoardUiState(
    val board: ChessBoard = ChessBoard(
        fen = ChessConstants.STARTING_POSITION_FEN,
        turn = ChessColor.WHITE,
        castlingRights = CastlingRights(true, true, true, true)
    ),
    val boardState: Map<Square, ChessPiece> = emptyMap(),
    val currentTurn: ChessColor = ChessColor.WHITE,
    val selectedSquare: Square? = null,
    val possibleMoves: List<Square> = emptyList(),
    val lastMove: ChessMove? = null,
    val playMode: PlayMode = PlayMode.VS_ENGINE,
    val playerColor: PlayerColor = PlayerColor.WHITE,
    val engineSettings: EngineSettings = EngineSettings(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false,
    val moveHistory: List<DetailedMove> = emptyList(),
    val capturedPieces: List<ChessPiece> = emptyList(),
    val isFlipped: Boolean = false,
    val showCoordinates: Boolean = true,
    val showNewGameDialog: Boolean = false,
    val animations: List<PieceAnimation> = emptyList(),
    val isAnimating: Boolean = false,
    val animatingPieces: Set<Square> = emptySet(),
    val ending: GameEnding? = null,
    val timeControl: TimeControl? = null
) {
    val isGameOver: Boolean get() = isCheckmate || isStalemate || isDraw || ending != null
}

internal fun chessBoardUiState(
    gameStateManager: GameStateManager,
    moveController: MoveController,
    animationController: AnimationController,
    settingsManager: SettingsManager,
    clock: GameClock
): Flow<ChessBoardUiState> = combine(
    combine(
        gameStateManager.board,
        gameStateManager.boardState,
        gameStateManager.currentTurn,
        gameStateManager.moveHistory
    ) { board, boardState, turn, history ->
        GameData(board, boardState, turn, history)
    },
    combine(
        gameStateManager.capturedPieces,
        gameStateManager.gameStatus,
        gameStateManager.lastMove
    ) { captured, status, lastMove ->
        StatusData(captured, status, lastMove)
    },
    combine(
        moveController.selectedSquare,
        moveController.possibleMoves,
        animationController.animations,
        animationController.isAnimatingState
    ) { selected, possible, anims, animating ->
        InteractionData(selected, possible, anims, animating)
    },
    combine(
        settingsManager.isFlipped,
        settingsManager.showCoordinates,
        settingsManager.showNewGameDialog,
        settingsManager.playMode
    ) { flipped, coords, dialog, mode ->
        PrefsData(flipped, coords, dialog, mode)
    },
    combine(
        settingsManager.playerColor,
        settingsManager.engineSettings,
        animationController.animatingPieces,
        clock.timeControl
    ) { color, engine, animPieces, timeControl ->
        SettingsData(color, engine, animPieces, timeControl)
    }
) { game, status, interaction, prefs, settings ->
    ChessBoardUiState(
        board = game.board,
        boardState = game.boardState,
        currentTurn = game.turn,
        moveHistory = game.history,
        capturedPieces = status.captured,
        isCheck = status.status.isCheck,
        isCheckmate = status.status.isCheckmate,
        isStalemate = status.status.isStalemate,
        isDraw = status.status.isDraw,
        selectedSquare = interaction.selected,
        possibleMoves = interaction.possible,
        lastMove = status.lastMove,
        isFlipped = prefs.flipped,
        showCoordinates = prefs.coords,
        showNewGameDialog = prefs.dialog,
        playMode = prefs.mode,
        playerColor = settings.color,
        engineSettings = settings.engine,
        animations = interaction.anims,
        isAnimating = interaction.animating,
        animatingPieces = settings.animPieces,
        ending = status.status.ending,
        timeControl = settings.timeControl
    )
}

private data class GameData(
    val board: ChessBoard,
    val boardState: Map<Square, ChessPiece>,
    val turn: ChessColor,
    val history: List<DetailedMove>
)

private data class StatusData(
    val captured: List<ChessPiece>,
    val status: GameStatus,
    val lastMove: ChessMove?
)

private data class InteractionData(
    val selected: Square?,
    val possible: List<Square>,
    val anims: List<PieceAnimation>,
    val animating: Boolean
)

private data class PrefsData(
    val flipped: Boolean,
    val coords: Boolean,
    val dialog: Boolean,
    val mode: PlayMode
)

private data class SettingsData(
    val color: PlayerColor,
    val engine: EngineSettings,
    val animPieces: Set<Square>,
    val timeControl: TimeControl?
)
