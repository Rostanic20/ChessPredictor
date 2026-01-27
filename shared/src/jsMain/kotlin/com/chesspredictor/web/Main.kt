package com.chesspredictor.web

import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel
import com.chesspredictor.web.components.renderChessBoard
import com.chesspredictor.web.components.renderCapturedPieces
import com.chesspredictor.web.components.renderGameControls
import com.chesspredictor.web.components.renderGameInfo
import com.chesspredictor.web.components.renderMoveHistory
import com.chesspredictor.web.components.showDrawOfferDialog
import com.chesspredictor.web.components.showErrorDialog
import com.chesspredictor.web.components.showExportDialog
import com.chesspredictor.web.components.showEloTestDialog
import com.chesspredictor.web.components.showImportDialog
import com.chesspredictor.web.components.showNewGameDialog
import com.chesspredictor.web.utils.console
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.style
import kotlinx.html.dom.append
import org.w3c.dom.Element
import com.chesspredictor.domain.entities.CommentaryType
import com.chesspredictor.domain.entities.EmotionalState
import com.chesspredictor.domain.entities.MoveCommentary
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


fun main() {
    window.onload = {
        try {
            val root = document.getElementById("root") ?: document.body!!
            
            root.innerHTML = ""
            
            // Create the main app container first
            root.append {
                div {
                    id = "app"
                    classes = setOf("chess-app")
                    
                    div {
                        id = "loading"
                        style = "text-align: center; padding: 2rem;"
                        +"Loading..."
                    }
                }
            }
            
            val appContainer = document.getElementById("app")!!

            GlobalScope.launch {
                try {
                    val viewModel = ChessBoardViewModel()

                    renderChessApp(appContainer, viewModel)

                    observeViewModelState(viewModel, appContainer)

                    viewModel.initializeBoard()
                    
                } catch (e: Throwable) {
                    console.error("Initialization failed: ${e.message}")
                    console.error("Stack trace: ${e.stackTraceToString()}")
                    
                    appContainer.innerHTML = """
                        <div style="text-align: center; padding: 2rem; color: red;">
                            <h2>Failed to initialize chess app</h2>
                            <p>Error: ${e.message}</p>
                            <button onclick="location.reload()">Reload Page</button>
                        </div>
                    """.trimIndent()
                }
            }
            
        } catch (e: Throwable) {
            console.error("Critical error in main: ${e.message}")
            console.error("Stack trace: ${e.stackTraceToString()}")
        }
    }
}

private fun renderChessApp(container: Element, viewModel: ChessBoardViewModel) {
    container.innerHTML = """
        <div class="app-container">
            <header class="app-header">
                <div class="app-title">
                    <h1>Chess<span class="highlight">Predictor</span></h1>
                </div>
                <div class="app-actions">
                    <button id="undo-move" class="icon-button" title="Undo Move">↶</button>
                    <button id="flip-board" class="icon-button" title="Flip Board">↕️</button>
                    <button id="new-game" class="icon-button" title="New Game">➕</button>
                    <button id="import-game" class="icon-button" title="Import Game">📥</button>
                    <button id="export-game" class="icon-button" title="Export Game">📤</button>
                    <button id="test-elo" class="icon-button" title="Test ELO Accuracy">🎯</button>
                </div>
            </header>
            
            <div class="main-content">
                <div class="left-panel">
                    <div id="game-info" class="game-info-panel"></div>
                    <div id="captured-pieces" class="captured-pieces-panel"></div>
                </div>
                
                <div class="center-panel">
                    <div id="chess-board" class="chess-board"></div>
                    <div id="engine-status" class="engine-status"></div>
                </div>
                
                <div class="right-panel">
                    <div id="game-controls" class="game-controls"></div>
                    <div id="move-history" class="move-history-panel"></div>
                    <div id="human-behavior" class="human-behavior-panel"></div>
                </div>
            </div>
            
            <div id="modal-container"></div>
        </div>
    """.trimIndent()
    
    // Set up event handlers
    setupEventHandlers(viewModel)
}

private fun setupEventHandlers(viewModel: ChessBoardViewModel) {
    document.getElementById("undo-move")?.addEventListener("click", {
        viewModel.undoLastMove()
    })
    
    document.getElementById("flip-board")?.addEventListener("click", {
        viewModel.toggleBoardFlip()
    })
    
    document.getElementById("new-game")?.addEventListener("click", {
        viewModel.onNewGame()
    })
    
    document.getElementById("import-game")?.addEventListener("click", {
        showImportDialog(viewModel)
    })
    
    document.getElementById("export-game")?.addEventListener("click", {
        showExportDialog(viewModel)
    })
    
    document.getElementById("test-elo")?.addEventListener("click", {
        showEloTestDialog(viewModel)
    })
    
}

private fun observeViewModelState(viewModel: ChessBoardViewModel, @Suppress("UNUSED_PARAMETER") container: Element) {
    var lastEngineSettings: EngineSettings? = null
    
    // Observe UI state changes
    viewModel.uiState.onEach { uiState ->
        // Update board
        val boardElement = document.getElementById("chess-board")
        if (boardElement != null) {
            renderChessBoard(boardElement, uiState, viewModel)
        }
        
        // Update move history
        val moveHistoryElement = document.getElementById("move-history")
        if (moveHistoryElement != null) {
            renderMoveHistory(moveHistoryElement, uiState.moveHistory)
        }
        
        // Update captured pieces
        val capturedElement = document.getElementById("captured-pieces")
        if (capturedElement != null) {
            renderCapturedPieces(capturedElement, uiState.capturedPieces)
        }
        
        // Update game info
        val gameInfoElement = document.getElementById("game-info")
        if (gameInfoElement != null) {
            renderGameInfo(gameInfoElement, uiState)
        }
        
        // Check if engine settings changed before updating lastEngineSettings
        val settingsChanged = lastEngineSettings != uiState.engineSettings
        
        val controlsElement = document.getElementById("game-controls")
        if (controlsElement != null && settingsChanged) {
            renderGameControls(controlsElement, uiState, viewModel)
        }

        if (settingsChanged) {
            val behaviorElement = document.getElementById("human-behavior")
            if (behaviorElement != null) {
                val currentEmotionalState = viewModel.currentEmotionalState.value
                val commentary = viewModel.lastCommentary.value
                val isShowingThoughts = viewModel.isShowingThoughts.value
                renderHumanBehavior(behaviorElement, currentEmotionalState, commentary, isShowingThoughts, uiState.engineSettings)
            }
            
            // Update lastEngineSettings AFTER both updates
            lastEngineSettings = uiState.engineSettings
        }
        
        // Show new game dialog if needed
        if (uiState.showNewGameDialog) {
            showNewGameDialog(viewModel)
        }
    }.launchIn(MainScope())
    
    // Observe engine status
    viewModel.isThinking.onEach { isThinking ->
        val statusElement = document.getElementById("engine-status")
        if (statusElement != null) {
            statusElement.innerHTML = if (isThinking) {
                """<div class="thinking">🤔 Engine thinking...</div>"""
            } else {
                ""
            }
        }
    }.launchIn(MainScope())
    
    // Observe human behavior state
    viewModel.currentEmotionalState.onEach { emotionalState ->
        val behaviorElement = document.getElementById("human-behavior")
        if (behaviorElement != null) {
            val currentEngineSettings = viewModel.uiState.value.engineSettings
            renderHumanBehavior(behaviorElement, emotionalState, null, false, currentEngineSettings)
        }
    }.launchIn(MainScope())
    
    viewModel.lastCommentary.onEach { commentary ->
        val behaviorElement = document.getElementById("human-behavior")
        if (behaviorElement != null) {
            val currentEmotionalState = viewModel.currentEmotionalState.value
            val currentEngineSettings = viewModel.uiState.value.engineSettings
            renderHumanBehavior(behaviorElement, currentEmotionalState, commentary, false, currentEngineSettings)
        }
    }.launchIn(MainScope())
    
    viewModel.isShowingThoughts.onEach { isShowingThoughts ->
        val behaviorElement = document.getElementById("human-behavior")
        if (behaviorElement != null) {
            val currentEmotionalState = viewModel.currentEmotionalState.value
            val commentary = viewModel.lastCommentary.value
            val currentEngineSettings = viewModel.uiState.value.engineSettings
            renderHumanBehavior(behaviorElement, currentEmotionalState, commentary, isShowingThoughts, currentEngineSettings)
        }
    }.launchIn(MainScope())
    
    // Observe errors
    viewModel.errorState.onEach { error ->
        if (error != null) {
            showErrorDialog(error.message) {
                viewModel.dismissError()
            }
        }
    }.launchIn(MainScope())
    
    // Observe draw offers
    viewModel.shouldOfferDraw.onEach { shouldOffer ->
        if (shouldOffer) {
            showDrawOfferDialog(viewModel)
        }
    }.launchIn(MainScope())
}

private fun renderHumanBehavior(
    element: Element,
    emotionalState: EmotionalState,
    commentary: MoveCommentary?,
    isShowingThoughts: Boolean,
    engineSettings: EngineSettings? = null
) {
    val emotionIcon = when (emotionalState) {
        EmotionalState.CONFIDENT -> "😎"
        EmotionalState.WORRIED -> "😰"
        EmotionalState.EXCITED -> "⚡"
        EmotionalState.FRUSTRATED -> "😤"
        EmotionalState.SURPRISED -> "😲"
        EmotionalState.FOCUSED -> "🎯"
        EmotionalState.PRESSURED -> "⏰"
        EmotionalState.SATISFIED -> "😊"
    }

    val emotionColor = when (emotionalState) {
        EmotionalState.CONFIDENT -> "#4CAF50"
        EmotionalState.WORRIED -> "#FF9800"
        EmotionalState.EXCITED -> "#E91E63"
        EmotionalState.FRUSTRATED -> "#F44336"
        EmotionalState.SURPRISED -> "#9C27B0"
        EmotionalState.FOCUSED -> "#2196F3"
        EmotionalState.PRESSURED -> "#FF5722"
        EmotionalState.SATISFIED -> "#8BC34A"
    }

    val thinkingIndicator = if (isShowingThoughts) {
        """<div class="thinking-indicator">💭 Thinking...</div>"""
    } else ""

    val commentaryHtml = commentary?.let { comment ->
        val typeIcon = when (comment.type) {
            CommentaryType.TEACHING_MOMENT -> "🎓"
            CommentaryType.EMOTIONAL_REACTION -> "❤️"
            CommentaryType.MISTAKE_ACKNOWLEDGMENT -> "🤦"
            CommentaryType.TACTICAL_OBSERVATION -> "⚔️"
            else -> "💬"
        }
        
        val confidenceStars = "★".repeat((comment.confidence * 5).toInt())
        
        """
        <div class="commentary-card">
            <div class="commentary-header">
                <span class="commentary-icon">$typeIcon</span>
                <span class="commentary-type">${comment.type.name.lowercase().replace('_', ' ')}</span>
                <span class="confidence">$confidenceStars</span>
            </div>
            <div class="commentary-text">${comment.text}</div>
        </div>
        """
    } ?: ""
    
    val currentDifficulty = engineSettings?.difficulty?.displayName ?: "Unknown"
    val humanStyleEnabled = engineSettings?.humanStyle ?: false

    element.innerHTML = """
        <div class="behavior-panel">
            <h3>🎯 Engine Status</h3>
            
            <div class="engine-info">
                <div><strong>Level:</strong> $currentDifficulty</div>
                <div><strong>Human Style:</strong> ${if (humanStyleEnabled) "✓ ON" else "✗ OFF"}</div>
            </div>
            
            <div class="thinking-status">
                ${if (isShowingThoughts) "💭 Analyzing position..." else "✓ Ready"}
            </div>
            
            ${if (humanStyleEnabled) """
            <div class="human-features">
                <div class="feature-item">🕐 Variable thinking time</div>
                <div class="feature-item">🎯 Position-based mistakes</div>
                <div class="feature-item">🧠 ELO-appropriate decisions</div>
                <div class="feature-item">🏳️ Human-like resignations</div>
            </div>
            """ else """
            <div class="engine-mode">
                <div>⚡ Perfect engine play</div>
                <div>🚀 Maximum strength</div>
            </div>
            """}
        </div>
    """.trimIndent()
}