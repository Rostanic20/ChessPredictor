package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.di.AppModule
import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.EmotionalState
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.ExportFormat
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.MoveCommentary
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.domain.usecases.OpeningDetector
import com.chesspredictor.domain.usecases.ImportExportGameUseCase
import com.chesspredictor.domain.usecases.ExportAdditionalData
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.AnimationController
import com.chesspredictor.presentation.managers.AnimationManager
import com.chesspredictor.presentation.managers.EngineError
import com.chesspredictor.presentation.managers.EngineManager
import com.chesspredictor.presentation.managers.GameStateManager
import com.chesspredictor.presentation.managers.GameStatus
import com.chesspredictor.presentation.managers.HumanBehaviorManager
import com.chesspredictor.presentation.managers.MoveController
import com.chesspredictor.presentation.managers.MoveResult
import com.chesspredictor.presentation.managers.PieceAnimation
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager
import com.chesspredictor.presentation.ErrorStateManager
import com.chesspredictor.presentation.ChessError
import com.chesspredictor.utils.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class ChessBoardViewModel(
    providedAppModule: AppModule? = null,
    private val gameStateRepository: GameStateRepository? = null,
    private val chessRulesEngine: ChessRulesEngine = ChessRulesEngine(),
    private val openingDetector: OpeningDetector = OpeningDetector(),
    private val importExportGameUseCase: ImportExportGameUseCase = ImportExportGameUseCase()
) {
    private val appModule: AppModule = providedAppModule ?: getOrCreateAppModule()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val animationManager = AnimationManager()
    private val humanBehaviorManager = HumanBehaviorManager(viewModelScope)
    private val engineManager = EngineManager(appModule.chessEngineRepository, viewModelScope, humanBehaviorManager)
    private val errorManager = ErrorStateManager()

    private val gameStateManager = GameStateManager(chessRulesEngine)
    private val animationController = AnimationController(animationManager)
    private val settingsManager = SettingsManager()
    private val moveController = MoveController(chessRulesEngine, gameStateManager, animationController)

    private val _isInitializing = MutableStateFlow(true)
    private val _currentOpening = MutableStateFlow<OpeningInfo?>(null)
    private var moveStartTime = 0L

    val currentOpening: StateFlow<OpeningInfo?> = _currentOpening.asStateFlow()
    val isThinking: StateFlow<Boolean> = engineManager.isThinking
    val isEngineReady: StateFlow<Boolean> = engineManager.isReady
    val engineError: StateFlow<EngineError?> = engineManager.error
    val shouldOfferDraw: StateFlow<Boolean> = engineManager.shouldOfferDraw
    val currentEmotionalState: StateFlow<EmotionalState> = humanBehaviorManager.currentEmotionalState
    val lastCommentary: StateFlow<MoveCommentary?> = humanBehaviorManager.lastCommentary
    val isShowingThoughts: StateFlow<Boolean> = humanBehaviorManager.isShowingThoughts
    val errorState: StateFlow<ChessError?> = errorManager.currentError
    val currentTimeControl: StateFlow<TimeControl> = settingsManager.currentTimeControl
    val playerTimeRemaining: StateFlow<Map<ChessColor, Long>> = settingsManager.playerTimeRemaining
    val glitchedSquares: StateFlow<Set<Square>> = MutableStateFlow<Set<Square>>(emptySet()).asStateFlow()

    val uiState: StateFlow<ChessBoardUiState> = combine(
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
            animationController.animatingPieces
        ) { color, engine, animPieces ->
            SettingsData(color, engine, animPieces)
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
            animatingPieces = settings.animPieces
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(ChessConstants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ChessBoardUiState()
    )

    init {
        gameStateManager.initialize()
        initializeEngine()
    }

    private fun initializeEngine() {
        viewModelScope.launch {
            initEngineWithRetry()
            _isInitializing.value = false
        }
    }

    private suspend fun initEngineWithRetry(): Boolean {
        repeat(ChessConstants.EngineInit.MAX_ATTEMPTS) { attempt ->
            try {
                appModule.initializeEngine()
                engineManager.setReady(true)
                return true
            } catch (e: Exception) {
                if (attempt == ChessConstants.EngineInit.MAX_ATTEMPTS - 1) {
                    errorManager.reportError(ChessError.EngineInitError(
                        message = "Failed to initialize chess engine: ${e.message}"
                    ))
                } else {
                    delay(ChessConstants.EngineInit.RETRY_DELAY_MS)
                }
            }
        }
        return false
    }

    private suspend fun ensureEngineReadyAndMaybeMove() {
        try {
            appModule.initializeEngine()
            engineManager.setReady(true)
            if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
                getEngineMove()
            }
        } catch (e: Exception) {
            engineManager.setReady(false)
            errorManager.reportError(ChessError.EngineInitError(
                message = "Failed to start engine: ${e.message}"
            ))
        }
    }

    private fun detectOpeningSafe(history: List<DetailedMove>): OpeningInfo? {
        return try {
            if (history.isNotEmpty()) openingDetector.detectOpening(history) else null
        } catch (e: Exception) { null }
    }

    private fun clearAndStartNewGame() {
        viewModelScope.launch {
            gameStateRepository?.clearGameState()
            resetToNewGame()
        }
    }

    fun initializeBoard() {
        viewModelScope.launch {
            loadSavedGame()
            if (gameStateManager.currentGameState == null) {
                resetToNewGame()
            }
            _isInitializing.value = false
        }
    }

    private fun resetToNewGame() {
        gameStateManager.reset()
        moveController.clearSelection()
        animationController.clearAnimations()
        settingsManager.resetPlayerTimes()
        moveStartTime = 0L
        _currentOpening.value = null

        viewModelScope.launch { ensureEngineReadyAndMaybeMove() }
    }

    fun onSquareClick(square: Square) {
        if (animationController.isAnimating()) return
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) return

        val boardState = gameStateManager.boardState.value
        val currentTurn = gameStateManager.currentTurn.value
        val selectedSquare = moveController.getSelectedSquare()

        when {
            selectedSquare == null -> {
                val piece = boardState[square]
                if (piece != null && piece.color == currentTurn) {
                    moveController.selectSquare(square, currentTurn, boardState)
                    moveStartTime = TimeProvider.currentTimeMillis()
                }
            }
            selectedSquare == square -> {
                moveController.clearSelection()
            }
            moveController.isMoveValid(square) -> {
                executeMove(selectedSquare, square)
            }
            else -> {
                val piece = boardState[square]
                if (piece != null && canSelectPiece(piece, currentTurn)) {
                    moveController.selectSquare(square, currentTurn, boardState)
                    if (moveStartTime == 0L) moveStartTime = TimeProvider.currentTimeMillis()
                } else {
                    moveController.clearSelection()
                    moveStartTime = 0L
                }
            }
        }
    }

    private fun canSelectPiece(piece: ChessPiece, currentTurn: ChessColor): Boolean {
        return when (settingsManager.playMode.value) {
            PlayMode.ANALYSIS, PlayMode.VS_HUMAN -> true
            PlayMode.VS_ENGINE -> piece.color == currentTurn
        }
    }

    fun executePromotionMove(from: Square, to: Square, promotionPiece: ChessPiece) {
        executeMove(from, to, promotionPiece)
    }

    private fun executeMove(from: Square, to: Square, promotionPiece: ChessPiece? = null) {
        val timeSpent = if (moveStartTime > 0) TimeProvider.currentTimeMillis() - moveStartTime else ChessConstants.DEFAULT_MOVE_TIME_MS
        moveStartTime = 0L

        val result = moveController.makeMove(from, to, promotionPiece) ?: return

        updatePlayerTime(result.oldGameState.turn, timeSpent)
        animationController.startMoveAnimation(result.move, result.oldGameState, result.newGameState, settingsManager.isFlipped.value)

        viewModelScope.launch {
            delay(animationController.getAnimationDuration())
            applyMoveResult(result, timeSpent)
        }
    }

    private fun applyMoveResult(result: MoveResult, timeSpent: Long) {
        val enhancedHistory = enhanceMoveHistory(result.newGameState.moveHistory, timeSpent)
        val finalState = result.newGameState.copy(moveHistory = enhancedHistory)

        gameStateManager.updateFromGameState(finalState)
        gameStateManager.setLastMove(result.move)
        animationController.clearAnimations()

        _currentOpening.value = detectOpeningSafe(finalState.moveHistory)

        saveGameState()

        if (!gameStateManager.isGameOver() && settingsManager.shouldBotMove(finalState.turn)) {
            getEngineMove()
        }
    }

    private fun enhanceMoveHistory(history: List<DetailedMove>, timeSpent: Long): List<DetailedMove> {
        val lastMove = history.lastOrNull() ?: return history
        val timeRemaining = settingsManager.playerTimeRemaining.value[
            if (lastMove.isWhiteMove) ChessColor.WHITE else ChessColor.BLACK
        ] ?: 0L
        val enhanced = lastMove.copy(timeSpent = timeSpent, remainingTime = timeRemaining)
        return history.dropLast(1) + enhanced
    }

    private fun updatePlayerTime(color: ChessColor, timeSpent: Long) {
        val current = settingsManager.playerTimeRemaining.value[color] ?: return
        val increment = settingsManager.currentTimeControl.value.increment
        val newTime = (current - timeSpent + increment).coerceAtLeast(0)
        settingsManager.updatePlayerTime(color, newTime)
    }

    private fun getEngineMove() {
        if (engineManager.isThinking.value) return

        if (!engineManager.isReady.value) {
            viewModelScope.launch {
                if (initEngineWithRetry()) {
                    requestEngineMoveInternal()
                }
            }
            return
        }

        requestEngineMoveInternal()
    }

    private fun requestEngineMoveInternal() {
        moveStartTime = TimeProvider.currentTimeMillis()
        engineManager.requestEngineMove(
            board = gameStateManager.board.value,
            settings = settingsManager.engineSettings.value
        ) { move ->
            executeMove(move.from, move.to, move.promotion)
        }
    }

    fun undoLastMove() {
        if (_isInitializing.value) return
        if (moveController.undoLastMove()) {
            _currentOpening.value = detectOpeningSafe(gameStateManager.moveHistory.value)
            saveGameState()
        }
    }

    fun onNewGame() {
        when {
            gameStateManager.isGameOver() -> clearAndStartNewGame()
            gameStateManager.moveHistory.value.isNotEmpty() -> settingsManager.openNewGameDialog()
            else -> clearAndStartNewGame()
        }
    }

    fun confirmNewGame() {
        settingsManager.hideNewGameDialog()
        clearAndStartNewGame()
    }

    fun cancelNewGame() = settingsManager.hideNewGameDialog()
    fun dismissError() = errorManager.clearError()
    fun toggleBoardFlip() = settingsManager.toggleFlip()
    fun toggleCoordinates() { settingsManager.toggleCoordinates(); saveGameState() }
    fun toggleBoardFlipAndSave() { settingsManager.toggleFlip(); saveGameState() }

    fun acceptDrawOffer() {
        engineManager.clearDrawOffer()
        gameStateManager.setGameStatus(gameStateManager.gameStatus.value.copy(isDraw = true))
    }

    fun declineDrawOffer() = engineManager.clearDrawOffer()

    fun setPlayMode(mode: PlayMode) {
        settingsManager.setPlayMode(mode)
        saveGameState()
    }

    fun setPlayerColor(color: PlayerColor) {
        settingsManager.setPlayerColor(color)
        saveGameState()
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
            getEngineMove()
        }
    }

    fun setEngineSettings(settings: EngineSettings) {
        val finalSettings = settings.copy(
            analysisDepth = settings.difficulty.depth,
            skillLevel = settings.difficulty.skillLevel,
            behaviorProfile = settings.difficulty.getDefaultBehaviorProfile()
        )
        settingsManager.setEngineSettings(finalSettings)
    }

    fun setTimeControl(timeControl: TimeControl) = settingsManager.setTimeControl(timeControl)

    fun saveGameState() {
        gameStateRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    val gameState = gameStateManager.currentGameState ?: return@launch
                    val savedState = SavedGameState(
                        fen = gameState.toFen(),
                        moveHistory = gameState.moveHistory,
                        capturedPieces = gameState.capturedPieces,
                        playMode = settingsManager.playMode.value.name,
                        playerColor = settingsManager.playerColor.value.name,
                        engineSettings = settingsManager.engineSettings.value,
                        isFlipped = settingsManager.isFlipped.value,
                        showCoordinates = settingsManager.showCoordinates.value,
                        positionEvaluations = emptyList()
                    )
                    repo.saveGameState(savedState)
                } catch (e: Exception) {
                    errorManager.reportError(ChessError.SaveGameError(
                        message = "Could not save game: ${e.message}"
                    ))
                }
            }
        }
    }

    suspend fun loadSavedGame() {
        gameStateRepository?.let { repo ->
            try {
                val savedState = repo.loadGameState() ?: return
                val gameState = chessRulesEngine.parseGameState(savedState.fen)
                val correctedHistory = moveController.reconstructMoveHistory(savedState.moveHistory)

                gameStateManager.updateFromGameState(gameState.copy(
                    moveHistory = correctedHistory,
                    capturedPieces = savedState.capturedPieces
                ))
                gameStateManager.setLastMove(savedState.moveHistory.lastOrNull()?.move)

                settingsManager.setFlipped(savedState.isFlipped)
                settingsManager.setShowCoordinates(savedState.showCoordinates)
                settingsManager.setPlayMode(PlayMode.valueOf(savedState.playMode))
                settingsManager.setPlayerColor(PlayerColor.valueOf(savedState.playerColor))
                settingsManager.setEngineSettings(savedState.engineSettings)

                moveController.clearSelection()
                animationController.clearAnimations()

                viewModelScope.launch { ensureEngineReadyAndMaybeMove() }
            } catch (e: Exception) {
                errorManager.reportError(ChessError.LoadGameError(
                    message = "Could not load saved game: ${e.message}"
                ))
                resetToNewGame()
            }
        } ?: resetToNewGame()
    }

    fun exportGame(format: ExportFormat): String {
        val gameState = gameStateManager.currentGameState ?: return ""
        val additionalData = ExportAdditionalData(
            event = "ChessPredictor Game",
            site = "ChessPredictor App",
            date = Clock.System.now().toString().substring(0, 10).replace("-", "."),
            white = if (settingsManager.playerColor.value == PlayerColor.WHITE) "Player" else "Engine",
            black = if (settingsManager.playerColor.value == PlayerColor.BLACK) "Player" else "Engine",
            whiteAccuracy = null,
            blackAccuracy = null,
            evaluations = emptyList(),
            keyMoments = null,
            openingName = _currentOpening.value?.opening?.name,
            openingEco = _currentOpening.value?.opening?.eco,
            timeControl = settingsManager.currentTimeControl.value.let { "${it.initialTime / 60000}+${it.increment / 1000}" },
            playMode = settingsManager.playMode.value.name,
            playerColor = settingsManager.playerColor.value.name,
            engineDifficulty = settingsManager.engineSettings.value.difficulty.name
        )
        return importExportGameUseCase.exportGame(gameState, format, additionalData)
    }

    fun importGame(content: String, format: ExportFormat) {
        viewModelScope.launch {
            importExportGameUseCase.importGame(content, format)
                .onSuccess { importedData ->
                    gameStateManager.updateFromGameState(importedData.gameState)
                    gameStateManager.setLastMove(importedData.gameState.moveHistory.lastOrNull()?.move)
                    moveController.clearSelection()
                    animationController.clearAnimations()

                    importedData.additionalData?.let { data ->
                        data.playMode?.let { settingsManager.setPlayMode(PlayMode.valueOf(it)) }
                        data.playerColor?.let { settingsManager.setPlayerColor(PlayerColor.valueOf(it)) }
                        data.engineDifficulty?.let { difficulty ->
                            settingsManager.setEngineSettings(
                                settingsManager.engineSettings.value.copy(
                                    difficulty = EngineDifficulty.valueOf(difficulty)
                                )
                            )
                        }
                    }

                    _currentOpening.value = openingDetector.detectOpening(importedData.gameState.moveHistory)
                    saveGameState()
                }
                .onFailure { error ->
                    errorManager.reportError(ChessError.ImportGameError(
                        message = "Failed to import game: ${error.message}"
                    ))
                }
        }
    }
}

expect fun getOrCreateAppModule(): AppModule

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
    val animPieces: Set<Square>
)

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
    val animatingPieces: Set<Square> = emptySet()
)
