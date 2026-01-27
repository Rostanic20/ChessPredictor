package com.chesspredictor.web.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel
import org.w3c.dom.Element
import org.w3c.dom.HTMLSelectElement
import kotlinx.browser.document

fun renderGameInfo(container: Element, uiState: ChessBoardUiState) {
    val status = when {
        uiState.isCheckmate -> "Checkmate!"
        uiState.isStalemate -> "Stalemate!"
        uiState.isDraw -> "Draw!"
        uiState.isCheck -> "Check!"
        else -> "${if (uiState.currentTurn == ChessColor.WHITE) "White" else "Black"} to move"
    }
    
    val gameResult = when {
        uiState.isCheckmate -> {
            if (uiState.currentTurn == ChessColor.WHITE) "Black wins!" else "White wins!"
        }
        uiState.isStalemate || uiState.isDraw -> "Draw"
        else -> ""
    }
    
    container.innerHTML = """
        <div class="game-status">
            <h3>Game Status</h3>
            <div class="status-text ${if (uiState.isCheck) "check" else ""}"">$status</div>
            ${if (gameResult.isNotEmpty()) """<div class="game-result">$gameResult</div>""" else ""}
        </div>
        
        <div class="current-turn">
            <div class="turn-indicator ${uiState.currentTurn.name.lowercase()}">
                ${if (uiState.currentTurn == ChessColor.WHITE) "♔" else "♚"}
            </div>
        </div>
    """.trimIndent()
}

fun renderGameControls(container: Element, uiState: ChessBoardUiState, viewModel: ChessBoardViewModel) {
    val isCustom = uiState.engineSettings.difficulty == EngineDifficulty.CUSTOM
    
    container.innerHTML = """
        <div class="controls-section">
            <h3>Game Settings</h3>
            
            <div class="control-group">
                <label>Play Mode</label>
                <select id="play-mode" class="control-select">
                    <option value="VS_ENGINE" ${if (uiState.playMode == PlayMode.VS_ENGINE) "selected" else ""}>vs Engine</option>
                    <option value="VS_HUMAN" ${if (uiState.playMode == PlayMode.VS_HUMAN) "selected" else ""}>vs Human</option>
                </select>
            </div>
            
            ${if (uiState.playMode == PlayMode.VS_ENGINE) """
                <div class="control-group">
                    <label>Play as</label>
                    <div class="radio-group">
                        <label class="radio-option">
                            <input type="radio" name="player-color" value="WHITE" 
                                ${if (uiState.playerColor == PlayerColor.WHITE) "checked" else ""}>
                            <span>White</span>
                        </label>
                        <label class="radio-option">
                            <input type="radio" name="player-color" value="BLACK"
                                ${if (uiState.playerColor == PlayerColor.BLACK) "checked" else ""}>
                            <span>Black</span>
                        </label>
                    </div>
                </div>
                
                <div class="control-group">
                    <label>Engine Difficulty</label>
                    <select id="engine-difficulty" class="control-select">
                        ${EngineDifficulty.values().map { difficulty ->
                            """<option value="${difficulty.name}" ${if (uiState.engineSettings.difficulty == difficulty) "selected" else ""}>
                                ${difficulty.displayName}
                            </option>"""
                        }.joinToString("")}
                    </select>
                    <div class="difficulty-description">
                        ${uiState.engineSettings.difficulty.description}
                    </div>
                </div>

                <div class="control-group">
                    <label>Engine Speed</label>
                    <div class="button-group">
                        <button id="fast-mode" class="mode-button ${if (uiState.engineSettings.timeLimit <= 200 && !uiState.engineSettings.humanStyle) "active" else ""}">
                            🚀 Fast Mode
                        </button>
                        <button id="human-mode" class="mode-button ${if (uiState.engineSettings.timeLimit > 200 && uiState.engineSettings.humanStyle) "active" else ""}">
                            🧠 Human Mode  
                        </button>
                    </div>
                    <div class="setting-description">Fast = perfect engine play, Human = realistic timing + mistakes</div>
                </div>

                
                ${if (isCustom) """
                    <div class="advanced-settings">
                        <h4>Advanced Engine Settings</h4>
                        
                        <div class="control-group">
                            <label>Search Depth: <span id="depth-value">${uiState.engineSettings.analysisDepth}</span></label>
                            <input type="range" id="depth-slider" min="6" max="20" value="${uiState.engineSettings.analysisDepth}" class="slider">
                            <div class="setting-description">Higher depth = stronger but slower play</div>
                        </div>
                        
                        <div class="control-group">
                            <label>Thinking Time: <span id="time-value">${uiState.engineSettings.timeLimit}ms</span></label>
                            <input type="range" id="time-slider" min="200" max="5000" step="100" value="${uiState.engineSettings.timeLimit}" class="slider">
                            <div class="setting-description">Time limit per move</div>
                        </div>
                        
                        <div class="control-group">
                            <label>Skill Level: <span id="skill-value">${uiState.engineSettings.skillLevel}</span></label>
                            <input type="range" id="skill-slider" min="0" max="20" value="${uiState.engineSettings.skillLevel}" class="slider">
                            <div class="setting-description">0 = weakest, 20 = strongest</div>
                        </div>
                        
                        <div class="control-group">
                            <label>Blunder Rate: <span id="blunder-value">${(uiState.engineSettings.blunderProbability * 100).toInt()}%</span></label>
                            <input type="range" id="blunder-slider" min="0" max="30" value="${(uiState.engineSettings.blunderProbability * 100).toInt()}" class="slider">
                            <div class="setting-description">Chance of making obvious mistakes</div>
                        </div>
                        
                        <div class="control-group">
                            <label>Inaccuracy Rate: <span id="inaccuracy-value">${(uiState.engineSettings.inaccuracyRate * 100).toInt()}%</span></label>
                            <input type="range" id="inaccuracy-slider" min="0" max="50" value="${(uiState.engineSettings.inaccuracyRate * 100).toInt()}" class="slider">
                            <div class="setting-description">Chance of playing suboptimal moves</div>
                        </div>
                        
                        <div class="control-group">
                            <label>
                                <input type="checkbox" id="use-book" ${if (uiState.engineSettings.useBook) "checked" else ""}>
                                Use Opening Book
                            </label>
                            <div class="setting-description">Play known opening moves</div>
                        </div>
                        
                        <div class="control-group">
                            <label>Contempt: <span id="contempt-value">${uiState.engineSettings.contempt}</span></label>
                            <input type="range" id="contempt-slider" min="-50" max="50" value="${uiState.engineSettings.contempt}" class="slider">
                            <div class="setting-description">Negative = prefers draws, Positive = avoids draws</div>
                        </div>
                    </div>
                """ else ""}
            """ else ""}
            
            <div class="control-group">
                <label>
                    <input type="checkbox" id="show-coordinates" ${if (uiState.showCoordinates) "checked" else ""}>
                    Show Coordinates
                </label>
            </div>
        </div>
    """.trimIndent()
    
    // Set up event handlers (with small delay to ensure DOM is ready)
    // Only attach handlers once per render to avoid conflicts
    kotlinx.browser.window.setTimeout({
        setupControlEventHandlers(uiState, viewModel)
    }, 10)
}

private fun setupControlEventHandlers(uiState: ChessBoardUiState, viewModel: ChessBoardViewModel) {
    document.getElementById("play-mode")?.addEventListener("change", { event ->
        val select = event.target as HTMLSelectElement
        val mode = PlayMode.valueOf(select.value)
        viewModel.setPlayMode(mode)
    })
    
    for (i in 0 until document.querySelectorAll("input[name='player-color']").length) {
        val radio = document.querySelectorAll("input[name='player-color']").item(i)!!
        radio.addEventListener("change", { event ->
            val input = event.target as org.w3c.dom.HTMLInputElement
            if (input.checked) {
                val color = PlayerColor.valueOf(input.value)
                viewModel.setPlayerColor(color)
            }
        })
    }
    
    document.getElementById("engine-difficulty")?.addEventListener("change", { event ->
        val select = event.target as HTMLSelectElement
        val difficulty = EngineDifficulty.valueOf(select.value)
        val newSettings = when (difficulty) {
            EngineDifficulty.CUSTOM -> uiState.engineSettings.copy(difficulty = difficulty)
            else -> {
                val preserveTimeLimit = uiState.engineSettings.timeLimit != uiState.engineSettings.difficulty.timeMs
                val finalTimeLimit = if (preserveTimeLimit) uiState.engineSettings.timeLimit else difficulty.timeMs
                uiState.engineSettings.copy(
                    difficulty = difficulty,
                    analysisDepth = difficulty.depth,
                    timeLimit = finalTimeLimit,
                    skillLevel = difficulty.skillLevel
                )
            }
        }
        viewModel.setEngineSettings(newSettings)
    })
    
    document.getElementById("fast-mode")?.addEventListener("click", { event ->
        val newSettings = uiState.engineSettings.copy(
            timeLimit = 100,
            humanStyle = false
        )
        viewModel.setEngineSettings(newSettings)
    })

    document.getElementById("human-mode")?.addEventListener("click", { event ->
        val newSettings = uiState.engineSettings.copy(
            timeLimit = 1500,
            humanStyle = true
        )
        viewModel.setEngineSettings(newSettings)
    })
    
    document.getElementById("show-coordinates")?.addEventListener("change", { event ->
        val checkbox = event.target as org.w3c.dom.HTMLInputElement
        if (checkbox.checked != uiState.showCoordinates) {
            viewModel.toggleCoordinates()
        }
    })
    
    // Advanced settings event handlers (only when Custom is selected)
    if (uiState.engineSettings.difficulty == EngineDifficulty.CUSTOM) {
        setupAdvancedSettingsHandlers(uiState, viewModel)
    }
}

private fun setupAdvancedSettingsHandlers(uiState: ChessBoardUiState, viewModel: ChessBoardViewModel) {
    document.getElementById("depth-slider")?.addEventListener("input", { event ->
        val slider = event.target as org.w3c.dom.HTMLInputElement
        val value = slider.value.toInt()
        document.getElementById("depth-value")?.textContent = value.toString()
        viewModel.setEngineSettings(uiState.engineSettings.copy(analysisDepth = value))
    })
    
    document.getElementById("time-slider")?.addEventListener("input", { event ->
        val slider = event.target as org.w3c.dom.HTMLInputElement
        val value = slider.value.toInt()
        document.getElementById("time-value")?.textContent = "${value}ms"
        viewModel.setEngineSettings(uiState.engineSettings.copy(timeLimit = value))
    })
    
    document.getElementById("skill-slider")?.addEventListener("input", { event ->
        val slider = event.target as org.w3c.dom.HTMLInputElement
        val value = slider.value.toInt()
        document.getElementById("skill-value")?.textContent = value.toString()
        viewModel.setEngineSettings(uiState.engineSettings.copy(skillLevel = value))
    })
    
    // Note: Blunder and inaccuracy rates are now read-only properties from difficulty level
    document.getElementById("blunder-slider")?.addEventListener("input", { event ->
        val slider = event.target as org.w3c.dom.HTMLInputElement
        val value = slider.value.toInt()
        document.getElementById("blunder-value")?.textContent = "${value}%"
        // This is now read-only - controlled by difficulty level
    })
    
    document.getElementById("inaccuracy-slider")?.addEventListener("input", { event ->
        val slider = event.target as org.w3c.dom.HTMLInputElement
        val value = slider.value.toInt()
        document.getElementById("inaccuracy-value")?.textContent = "${value}%"
        // This is now read-only - controlled by difficulty level
    })
    
    document.getElementById("use-book")?.addEventListener("change", { event ->
        val checkbox = event.target as org.w3c.dom.HTMLInputElement
        viewModel.setEngineSettings(uiState.engineSettings.copy(useBook = checkbox.checked))
    })
    
    document.getElementById("contempt-slider")?.addEventListener("input", { event ->
        val slider = event.target as org.w3c.dom.HTMLInputElement
        val value = slider.value.toInt()
        document.getElementById("contempt-value")?.textContent = value.toString()
        viewModel.setEngineSettings(uiState.engineSettings.copy(contempt = value))
    })
}