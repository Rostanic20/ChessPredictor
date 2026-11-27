package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.di.AppModule
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.EmotionalState
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.ExportFormat
import com.chesspredictor.domain.entities.GameEnding
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.MoveCommentary
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.domain.humanlike.HumanMovePredictor
import com.chesspredictor.domain.humanlike.MaiaMoveChooser
import com.chesspredictor.domain.humanlike.ThinkPace
import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.domain.usecases.OpeningDetector
import com.chesspredictor.domain.usecases.ImportExportGameUseCase
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.AnimationController
import com.chesspredictor.presentation.managers.AnimationManager
import com.chesspredictor.presentation.managers.EngineError
import com.chesspredictor.presentation.managers.EngineManager
import com.chesspredictor.presentation.managers.GameClock
import com.chesspredictor.presentation.managers.GameStateManager
import com.chesspredictor.presentation.managers.HumanBehaviorManager
import com.chesspredictor.presentation.managers.MoveController
import com.chesspredictor.presentation.managers.MoveResult
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager
import com.chesspredictor.presentation.ErrorStateManager
import com.chesspredictor.presentation.ChessError
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChessBoardViewModel(
    providedAppModule: AppModule? = null,
    gameStateRepository: GameStateRepository? = null,
    private val chessRulesEngine: ChessRulesEngine = ChessRulesEngine(),
    private val openingDetector: OpeningDetector = OpeningDetector(),
    private val importExportGameUseCase: ImportExportGameUseCase = ImportExportGameUseCase(),
    private val background: CoroutineDispatcher = Dispatchers.Default,
    humanMoves: HumanMovePredictor? = null
) {
    private val appModule: AppModule = providedAppModule ?: getOrCreateAppModule()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val animationManager = AnimationManager()
    private val humanBehaviorManager = HumanBehaviorManager(viewModelScope)
    private val engineManager = EngineManager(
        repository = appModule.chessEngineRepository,
        scope = viewModelScope,
        humanBehaviorManager = humanBehaviorManager,
        rules = chessRulesEngine,
        moveChooser = humanMoves?.let { MaiaMoveChooser(it, chessRulesEngine) },
        background = background
    )
    private val errorManager = ErrorStateManager()

    private val gameStateManager = GameStateManager(chessRulesEngine)
    private val animationController = AnimationController(animationManager)
    private val settingsManager = SettingsManager()
    private val moveController = MoveController(chessRulesEngine, gameStateManager, animationController)
    private val clock = GameClock(viewModelScope) { color -> onTimeout(color) }
    private val persistence = gameStateRepository?.let { GamePersistence(it, settingsManager, moveController, clock) }

    private val _isInitializing = MutableStateFlow(true)
    private val _currentOpening = MutableStateFlow<OpeningInfo?>(null)
    private var pendingMoveJob: Job? = null
    private var openingJob: Job? = null

    val currentOpening: StateFlow<OpeningInfo?> = _currentOpening.asStateFlow()
    val isThinking: StateFlow<Boolean> = engineManager.isThinking
    val isEngineReady: StateFlow<Boolean> = engineManager.isReady
    val engineError: StateFlow<EngineError?> = engineManager.error
    val shouldOfferDraw: StateFlow<Boolean> = engineManager.shouldOfferDraw
    val currentEmotionalState: StateFlow<EmotionalState> = humanBehaviorManager.currentEmotionalState
    val lastCommentary: StateFlow<MoveCommentary?> = humanBehaviorManager.lastCommentary
    val isShowingThoughts: StateFlow<Boolean> = humanBehaviorManager.isShowingThoughts
    val errorState: StateFlow<ChessError?> = errorManager.currentError
    val currentTimeControl: StateFlow<TimeControl?> = clock.timeControl
    val playerTimeRemaining: StateFlow<Map<ChessColor, Long>> = clock.remaining

    val uiState: StateFlow<ChessBoardUiState> =
        chessBoardUiState(gameStateManager, moveController, animationController, settingsManager, clock).stateIn(
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            engineManager.setReady(false)
            errorManager.reportError(ChessError.EngineInitError(
                message = "Failed to start engine: ${e.message}"
            ))
            if (engineManager.playsWithoutEngine(settingsManager.engineSettings.value) && botMovesNow()) {
                requestEngineMoveInternal()
            }
            return
        }
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
            getEngineMove()
        }
    }

    private fun botMovesNow(): Boolean =
        settingsManager.shouldBotMove(gameStateManager.currentTurn.value) &&
            !engineManager.isThinking.value && !gameStateManager.isGameOver()

    private fun refreshOpening(history: List<DetailedMove>) {
        openingJob?.cancel()
        if (history.isEmpty()) {
            _currentOpening.value = null
            return
        }
        openingJob = viewModelScope.launch {
            _currentOpening.value = withContext(background) {
                runCatching { openingDetector.detectOpening(history) }.getOrNull()
            }
        }
    }

    private fun clearAndStartNewGame() {
        viewModelScope.launch {
            persistence?.clear()
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

    private fun cancelPendingWork() {
        engineManager.cancelPendingMove()
        engineManager.clearDrawOffer()
        pendingMoveJob?.cancel()
        pendingMoveJob = null
        animationController.clearAnimations()
        clock.stop()
    }

    private fun restartClock() {
        if (gameStateManager.isGameOver()) {
            clock.stop()
        } else {
            clock.startTurn(gameStateManager.currentTurn.value, counting = gameStateManager.moveHistory.value.isNotEmpty())
        }
    }

    private fun resetToNewGame() {
        cancelPendingWork()
        gameStateManager.reset()
        moveController.clearSelection()
        clock.reset()
        restartClock()
        refreshOpening(emptyList())

        viewModelScope.launch { ensureEngineReadyAndMaybeMove() }
    }

    private fun onTimeout(color: ChessColor) {
        if (gameStateManager.isGameOver()) return
        engineManager.cancelPendingMove()
        engineManager.clearDrawOffer()
        moveController.clearSelection()
        val ending = GameEnding.outOfTime(color)
        val gameState = gameStateManager.currentGameState
        gameStateManager.endGame(ending, isDraw = gameState != null && endingIsDraw(ending, gameState))
        saveGameState()
    }

    private fun endingIsDraw(ending: GameEnding, gameState: GameState): Boolean = when (ending) {
        GameEnding.DRAW_AGREED -> true
        GameEnding.WHITE_OUT_OF_TIME -> !chessRulesEngine.hasMatingMaterial(gameState, ChessColor.BLACK)
        GameEnding.BLACK_OUT_OF_TIME -> !chessRulesEngine.hasMatingMaterial(gameState, ChessColor.WHITE)
    }

    fun onSquareClick(square: Square) {
        if (animationController.isAnimating()) return
        if (gameStateManager.isGameOver()) return
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) return

        val boardState = gameStateManager.boardState.value
        val currentTurn = gameStateManager.currentTurn.value
        val selectedSquare = moveController.getSelectedSquare()

        when {
            selectedSquare == null -> {
                val piece = boardState[square]
                if (piece != null && piece.color == currentTurn) {
                    moveController.selectSquare(square, currentTurn, boardState)
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
                } else {
                    moveController.clearSelection()
                }
            }
        }
    }

    private fun canSelectPiece(piece: ChessPiece, currentTurn: ChessColor): Boolean {
        return when (settingsManager.playMode.value) {
            PlayMode.VS_HUMAN -> true
            PlayMode.VS_ENGINE -> piece.color == currentTurn
        }
    }

    fun isPromotionMove(to: Square): Boolean {
        val from = moveController.getSelectedSquare() ?: return false
        val piece = gameStateManager.boardState.value[from]
        return piece is ChessPiece.Pawn && (to.rank == 1 || to.rank == 8) && moveController.isMoveValid(to)
    }

    fun executePromotionMove(from: Square, to: Square, promotionPiece: ChessPiece) {
        executeMove(from, to, promotionPiece)
    }

    private fun executeMove(from: Square, to: Square, promotionPiece: ChessPiece? = null) {
        val result = moveController.makeMove(from, to, promotionPiece) ?: return
        val timeSpent = clock.completeTurn()

        animationController.startMoveAnimation(result.move, result.oldGameState, result.newGameState, settingsManager.isFlipped.value)

        pendingMoveJob = viewModelScope.launch {
            delay(animationController.getAnimationDuration())
            pendingMoveJob = null
            applyMoveResult(result, timeSpent)
        }
    }

    private fun applyMoveResult(result: MoveResult, timeSpent: Long) {
        val enhancedHistory = enhanceMoveHistory(result.newGameState.moveHistory, timeSpent)
        val finalState = result.newGameState.copy(moveHistory = enhancedHistory)

        gameStateManager.updateFromGameState(finalState)
        gameStateManager.setLastMove(result.move)
        animationController.clearAnimations()
        restartClock()

        refreshOpening(finalState.moveHistory)

        saveGameState()

        if (!gameStateManager.isGameOver() && settingsManager.shouldBotMove(finalState.turn)) {
            getEngineMove()
        }
    }

    private fun enhanceMoveHistory(history: List<DetailedMove>, timeSpent: Long): List<DetailedMove> {
        val lastMove = history.lastOrNull() ?: return history
        val mover = if (lastMove.isWhiteMove) ChessColor.WHITE else ChessColor.BLACK
        val timeRemaining = clock.remaining.value[mover]?.takeIf { clock.timeControl.value != null }
        val enhanced = lastMove.copy(timeSpent = timeSpent, remainingTime = timeRemaining)
        return history.dropLast(1) + enhanced
    }

    private fun getEngineMove() {
        if (engineManager.isThinking.value) return
        if (gameStateManager.isGameOver()) return

        if (!engineManager.isReady.value) {
            viewModelScope.launch {
                if (initEngineWithRetry() || engineManager.playsWithoutEngine(settingsManager.engineSettings.value)) {
                    requestEngineMoveInternal()
                }
            }
            return
        }

        requestEngineMoveInternal()
    }

    private fun requestEngineMoveInternal() {
        val board = gameStateManager.board.value
        val budget = clock.budgetFor(board.turn)
        val settings = settingsManager.engineSettings.value.let { settings ->
            if (budget == null) settings else settings.copy(timeLimit = settings.timeLimit.coerceAtMost(budget.toInt()))
        }
        val timeControl = clock.timeControl.value
        val remaining = clock.remaining.value.getValue(board.turn)
        engineManager.requestEngineMove(
            board = board,
            settings = settings,
            maxThinkMs = timeControl?.let { remaining / 3 },
            pace = timeControl?.let { ThinkPace.of(it.initialTime, it.increment, remaining) } ?: ThinkPace.CASUAL,
            history = gameStateManager.currentGameState?.let { it.positionHistory + it.toFen() } ?: listOf(board.fen)
        ) { move ->
            if (gameStateManager.board.value.fen != board.fen) return@requestEngineMove
            executeMove(move.from, move.to, move.promotion)
        }
    }

    fun undoLastMove() {
        if (_isInitializing.value) return
        val ending = gameStateManager.gameStatus.value.ending
        if (ending != null && ending != GameEnding.DRAW_AGREED) return
        if (!moveController.undoLastMove()) return
        clock.completeTurn(withIncrement = false)
        engineManager.cancelPendingMove()
        while (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
            if (!moveController.undoLastMove()) break
        }
        restartClock()
        refreshOpening(gameStateManager.moveHistory.value)
        saveGameState()
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
            getEngineMove()
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
    fun dismissEngineError() = engineManager.clearError()
    fun toggleBoardFlip() = settingsManager.toggleFlip()
    fun toggleCoordinates() { settingsManager.toggleCoordinates(); saveGameState() }
    fun toggleBoardFlipAndSave() { settingsManager.toggleFlip(); saveGameState() }

    fun acceptDrawOffer() {
        engineManager.clearDrawOffer()
        if (gameStateManager.isGameOver()) return
        engineManager.cancelPendingMove()
        gameStateManager.endGame(GameEnding.DRAW_AGREED, isDraw = true)
        clock.stop()
        saveGameState()
    }

    fun declineDrawOffer() = engineManager.clearDrawOffer()

    fun setPlayMode(mode: PlayMode) {
        engineManager.cancelPendingMove()
        settingsManager.setPlayMode(mode)
        saveGameState()
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
            getEngineMove()
        }
    }

    fun setPlayerColor(color: PlayerColor) {
        engineManager.cancelPendingMove()
        settingsManager.setPlayerColor(color)
        saveGameState()
        if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
            getEngineMove()
        }
    }

    fun setEngineSettings(settings: EngineSettings) {
        settingsManager.setEngineSettings(settings.withDifficulty(settings.difficulty))
    }

    fun setTimeControl(timeControl: TimeControl?) {
        if (gameStateManager.moveHistory.value.isNotEmpty() && !gameStateManager.isGameOver()) return
        clock.setTimeControl(timeControl)
        restartClock()
        saveGameState()
    }

    fun close() {
        engineManager.cancelPendingMove()
        pendingMoveJob?.cancel()
        openingJob?.cancel()
        clock.pause()
        val persistence = persistence
        val gameState = gameStateManager.currentGameState
        if (persistence == null || gameState == null) {
            viewModelScope.cancel()
            return
        }
        val ending = gameStateManager.gameStatus.value.ending
        viewModelScope.launch {
            try {
                persistence.save(gameState, ending)
            } catch (e: Exception) {
                ChessLogger.error("ChessBoardViewModel", "Final save failed", e)
            } finally {
                viewModelScope.cancel()
            }
        }
    }

    fun pauseClock() = clock.pause()

    fun resumeClock() = clock.resume()

    fun saveGameState() {
        val persistence = persistence ?: return
        viewModelScope.launch {
            try {
                val gameState = gameStateManager.currentGameState ?: return@launch
                persistence.save(gameState, gameStateManager.gameStatus.value.ending)
            } catch (e: Exception) {
                errorManager.reportError(ChessError.SaveGameError(
                    message = "Could not save game: ${e.message}"
                ))
            }
        }
    }

    suspend fun loadSavedGame() {
        val persistence = persistence ?: return resetToNewGame()
        try {
            val (gameState, ending) = persistence.load() ?: return

            cancelPendingWork()
            gameStateManager.updateFromGameState(gameState)
            gameStateManager.setLastMove(gameState.moveHistory.lastOrNull()?.move)
            if (ending != null && !gameStateManager.isGameOver()) {
                gameStateManager.endGame(ending, endingIsDraw(ending, gameState))
            }
            restartClock()
            refreshOpening(gameState.moveHistory)
            moveController.clearSelection()

            viewModelScope.launch { ensureEngineReadyAndMaybeMove() }
        } catch (e: Exception) {
            errorManager.reportError(ChessError.LoadGameError(
                message = "Could not load saved game: ${e.message}"
            ))
            resetToNewGame()
        }
    }

    fun exportGame(format: ExportFormat): String {
        val gameState = gameStateManager.currentGameState ?: return ""
        val metadata = settingsManager.exportMetadata(_currentOpening.value, clock.timeControl.value, gameStateManager.gameStatus.value)
        return importExportGameUseCase.exportGame(gameState, format, metadata)
    }

    fun importGame(content: String, format: ExportFormat) {
        viewModelScope.launch {
            withContext(background) { importExportGameUseCase.importGame(content, format) }
                .onSuccess { importedData ->
                    cancelPendingWork()
                    gameStateManager.updateFromGameState(importedData.gameState)
                    gameStateManager.setLastMove(importedData.gameState.moveHistory.lastOrNull()?.move)
                    moveController.clearSelection()
                    importedData.additionalData?.let(settingsManager::applyImportedSettings)
                    clock.reset()
                    restartClock()

                    refreshOpening(importedData.gameState.moveHistory)
                    saveGameState()
                    if (settingsManager.shouldBotMove(gameStateManager.currentTurn.value)) {
                        getEngineMove()
                    }
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
