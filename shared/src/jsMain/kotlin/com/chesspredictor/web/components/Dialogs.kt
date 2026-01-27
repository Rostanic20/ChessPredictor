package com.chesspredictor.web.components

import com.chesspredictor.domain.entities.ExportFormat
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel
import com.chesspredictor.web.utils.copyToClipboard
import com.chesspredictor.web.utils.createBlob
import com.chesspredictor.web.utils.createObjectURL
import com.chesspredictor.web.utils.readFromClipboard
import com.chesspredictor.web.utils.revokeObjectURL
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.events.Event
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val scope = MainScope()

fun showDrawOfferDialog(viewModel: ChessBoardViewModel) {
    val modalContainer = document.getElementById("modal-container") ?: return
    
    modalContainer.innerHTML = """
        <div class="modal-overlay" id="draw-offer-modal">
            <div class="modal-content modal-small">
                <div class="modal-header">
                    <h2>🤝 Draw Offer</h2>
                </div>
                
                <div class="modal-body">
                    <div class="draw-offer-message">
                        <p>Your opponent offers a draw.</p>
                        <p class="draw-reason">The position appears difficult to win.</p>
                    </div>
                    
                    <div class="draw-offer-buttons">
                        <button id="accept-draw" class="button button-success">
                            ✓ Accept Draw
                        </button>
                        <button id="decline-draw" class="button button-secondary">
                            ✗ Continue Playing
                        </button>
                    </div>
                </div>
            </div>
        </div>
    """.trimIndent()
    
    // Event handlers
    document.getElementById("accept-draw")?.addEventListener("click", {
        viewModel.acceptDrawOffer()
        modalContainer.innerHTML = ""
    })
    
    document.getElementById("decline-draw")?.addEventListener("click", {
        viewModel.declineDrawOffer()
        modalContainer.innerHTML = ""
    })
}

fun showNewGameDialog(viewModel: ChessBoardViewModel) {
    val modalContainer = document.getElementById("modal-container") ?: return
    
    modalContainer.innerHTML = """
        <div class="modal-overlay" id="new-game-modal">
            <div class="modal-content">
                <div class="modal-header">
                    <h2>New Game</h2>
                    <button class="close-button" id="close-new-game">×</button>
                </div>
                
                <div class="modal-body">
                    <div class="form-group">
                        <label>Play Mode</label>
                        <select id="new-game-mode" class="form-control">
                            <option value="VS_ENGINE">vs Engine</option>
                            <option value="VS_HUMAN">vs Human</option>
                        </select>
                    </div>
                    
                    <div class="form-group" id="player-color-group">
                        <label>Play as</label>
                        <div class="radio-group">
                            <label class="radio-option">
                                <input type="radio" name="new-player-color" value="WHITE" checked>
                                <span>White</span>
                            </label>
                            <label class="radio-option">
                                <input type="radio" name="new-player-color" value="BLACK">
                                <span>Black</span>
                            </label>
                        </div>
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button class="button button-secondary" id="cancel-new-game">Cancel</button>
                    <button class="button button-primary" id="confirm-new-game">Start Game</button>
                </div>
            </div>
        </div>
    """.trimIndent()
    
    // Handle mode selection
    val modeSelect = document.getElementById("new-game-mode") as? HTMLSelectElement
    val colorGroup = document.getElementById("player-color-group")
    
    modeSelect?.addEventListener("change", { event ->
        val mode = PlayMode.valueOf((event.target as HTMLSelectElement).value)
        colorGroup?.setAttribute("style", 
            if (mode == PlayMode.VS_ENGINE) "display: block;" else "display: none;"
        )
    })
    
    // Handle confirm
    document.getElementById("confirm-new-game")?.addEventListener("click", {
        val mode = PlayMode.valueOf(modeSelect?.value ?: "VS_ENGINE")
        var playerColor = PlayerColor.WHITE
        
        if (mode == PlayMode.VS_ENGINE) {
            for (i in 0 until document.querySelectorAll("input[name='new-player-color']").length) {
                val radio = document.querySelectorAll("input[name='new-player-color']").item(i)!!
                val input = radio as HTMLInputElement
                if (input.checked) {
                    playerColor = PlayerColor.valueOf(input.value)
                }
            }
        }
        
        scope.launch {
            viewModel.onNewGame()
            viewModel.setPlayMode(mode)
            if (mode == PlayMode.VS_ENGINE) {
                viewModel.setPlayerColor(playerColor)
            }
            viewModel.confirmNewGame()
        }
        modalContainer.innerHTML = ""
    })
    
    // Handle cancel/close
    listOf("close-new-game", "cancel-new-game").forEach { id ->
        document.getElementById(id)?.addEventListener("click", {
            // Dialog closed via removing HTML
            modalContainer.innerHTML = ""
        })
    }
    
    // Close on overlay click
    document.getElementById("new-game-modal")?.addEventListener("click", { event ->
        if ((event.target as? Element)?.id == "new-game-modal") {
            // Dialog closed via removing HTML
            modalContainer.innerHTML = ""
        }
    })
}

fun showImportDialog(viewModel: ChessBoardViewModel) {
    val modalContainer = document.getElementById("modal-container") ?: return
    
    modalContainer.innerHTML = """
        <div class="modal-overlay" id="import-modal">
            <div class="modal-content modal-large">
                <div class="modal-header">
                    <h2>Import Game</h2>
                    <button class="close-button" id="close-import">×</button>
                </div>
                
                <div class="modal-body">
                    <div class="form-group">
                        <label>Format</label>
                        <select id="import-format" class="form-control">
                            <option value="PGN">PGN - Standard chess notation</option>
                            <option value="JSON">JSON - Full game data with analysis</option>
                            <option value="FEN">FEN - Position string</option>
                        </select>
                    </div>
                    
                    <div class="form-group">
                        <label>Game Data</label>
                        <textarea 
                            id="import-content" 
                            class="form-control monospace"
                            rows="10"
                            placeholder="Paste your game data here..."></textarea>
                    </div>
                    
                    <div class="button-group">
                        <button class="button button-secondary" id="paste-import">
                            📋 Paste from Clipboard
                        </button>
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button class="button button-secondary" id="cancel-import">Cancel</button>
                    <button class="button button-primary" id="confirm-import">Import</button>
                </div>
            </div>
        </div>
    """.trimIndent()
    
    // Handle paste
    document.getElementById("paste-import")?.addEventListener("click", {
        readFromClipboard(
            onSuccess = { text ->
                (document.getElementById("import-content") as? HTMLTextAreaElement)?.value = text
            },
            onError = {
                showErrorDialog("Unable to access clipboard") {}
            }
        )
    })
    
    // Handle confirm
    document.getElementById("confirm-import")?.addEventListener("click", {
        val format = ExportFormat.valueOf(
            (document.getElementById("import-format") as? HTMLSelectElement)?.value ?: "PGN"
        )
        val content = (document.getElementById("import-content") as? HTMLTextAreaElement)?.value ?: ""
        
        if (content.isNotBlank()) {
            viewModel.importGame(content, format)
            modalContainer.innerHTML = ""
        }
    })
    
    // Handle cancel/close
    listOf("close-import", "cancel-import").forEach { id ->
        document.getElementById(id)?.addEventListener("click", {
            modalContainer.innerHTML = ""
        })
    }
    
    // Close on overlay click
    document.getElementById("import-modal")?.addEventListener("click", { event ->
        if ((event.target as? Element)?.id == "import-modal") {
            modalContainer.innerHTML = ""
        }
    })
}

fun showExportDialog(viewModel: ChessBoardViewModel) {
    val modalContainer = document.getElementById("modal-container") ?: return
    
    modalContainer.innerHTML = """
        <div class="modal-overlay" id="export-modal">
            <div class="modal-content modal-large">
                <div class="modal-header">
                    <h2>Export Game</h2>
                    <button class="close-button" id="close-export">×</button>
                </div>
                
                <div class="modal-body">
                    <div class="form-group">
                        <label>Format</label>
                        <select id="export-format" class="form-control">
                            <option value="PGN">PGN - Standard chess notation</option>
                            <option value="JSON">JSON - Full game data with analysis</option>
                            <option value="FEN">FEN - Current position only</option>
                        </select>
                    </div>
                    
                    <button class="button button-primary" id="generate-export">Generate</button>
                    
                    <div id="export-result" style="display: none;">
                        <div class="form-group">
                            <label>Exported Data</label>
                            <textarea 
                                id="export-content" 
                                class="form-control monospace"
                                rows="10"
                                readonly></textarea>
                        </div>
                        
                        <div class="button-group">
                            <button class="button button-secondary" id="copy-export">
                                📋 Copy to Clipboard
                            </button>
                            <button class="button button-secondary" id="download-export">
                                💾 Download
                            </button>
                        </div>
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button class="button button-secondary" id="close-export-bottom">Close</button>
                </div>
            </div>
        </div>
    """.trimIndent()
    
    var currentExportContent = ""
    var currentFormat = ExportFormat.PGN
    
    // Handle generate
    document.getElementById("generate-export")?.addEventListener("click", {
        currentFormat = ExportFormat.valueOf(
            (document.getElementById("export-format") as? HTMLSelectElement)?.value ?: "PGN"
        )
        currentExportContent = viewModel.exportGame(currentFormat)
        
        (document.getElementById("export-content") as? HTMLTextAreaElement)?.value = currentExportContent
        document.getElementById("export-result")?.setAttribute("style", "display: block;")
    })
    
    // Handle copy
    document.getElementById("copy-export")?.addEventListener("click", {
        copyToClipboard(
            text = currentExportContent,
            onSuccess = {
                val button = document.getElementById("copy-export") as? HTMLButtonElement
                val originalText = button?.textContent
                button?.textContent = "✓ Copied!"
                window.setTimeout({
                    button?.textContent = originalText
                }, 2000)
            },
            onError = {
                showErrorDialog("Unable to copy to clipboard") {}
            }
        )
    })
    
    // Handle download
    document.getElementById("download-export")?.addEventListener("click", {
        val blob = createBlob(currentExportContent, "text/plain")
        val url = createObjectURL(blob)
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = url
        
        val filename = when (currentFormat) {
            ExportFormat.PGN -> "chess_game.pgn"
            ExportFormat.JSON -> "chess_game.json"
            ExportFormat.FEN -> "chess_position.fen"
        }
        a.download = filename
        a.click()
        revokeObjectURL(url)
    })
    
    // Handle close
    listOf("close-export", "close-export-bottom").forEach { id ->
        document.getElementById(id)?.addEventListener("click", {
            modalContainer.innerHTML = ""
        })
    }
    
    // Close on overlay click
    document.getElementById("export-modal")?.addEventListener("click", { event ->
        if ((event.target as? Element)?.id == "export-modal") {
            modalContainer.innerHTML = ""
        }
    })
}

fun showErrorDialog(message: String, onDismiss: () -> Unit) {
    val modalContainer = document.getElementById("modal-container") ?: return
    
    modalContainer.innerHTML = """
        <div class="modal-overlay" id="error-modal">
            <div class="modal-content modal-small">
                <div class="modal-header error">
                    <h2>Error</h2>
                    <button class="close-button" id="close-error">×</button>
                </div>
                
                <div class="modal-body">
                    <p class="error-message">$message</p>
                </div>
                
                <div class="modal-footer">
                    <button class="button button-primary" id="ok-error">OK</button>
                </div>
            </div>
        </div>
    """.trimIndent()
    
    // Handle close
    listOf("close-error", "ok-error").forEach { id ->
        document.getElementById(id)?.addEventListener("click", {
            onDismiss()
            modalContainer.innerHTML = ""
        })
    }
    
    // Close on overlay click
    document.getElementById("error-modal")?.addEventListener("click", { event ->
        if ((event.target as? Element)?.id == "error-modal") {
            onDismiss()
            modalContainer.innerHTML = ""
        }
    })
}

