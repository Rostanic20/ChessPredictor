package com.chesspredictor.web.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.MoveQuality
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

fun renderChessBoard(container: Element, uiState: ChessBoardUiState, viewModel: ChessBoardViewModel) {
    val boardHtml = buildString {
        append("""<div class="board-container">""")
        
        // Add coordinates if enabled
        if (uiState.showCoordinates) {
            append("""<div class="coordinates files">""")
            val files = if (uiState.isFlipped) "hgfedcba" else "abcdefgh"
            files.forEach { file ->
                append("""<div class="coordinate">$file</div>""")
            }
            append("</div>")
            
            append("""<div class="coordinates ranks">""")
            val ranks = if (uiState.isFlipped) (1..8) else (8 downTo 1)
            ranks.forEach { rank ->
                append("""<div class="coordinate">$rank</div>""")
            }
            append("</div>")
        }
        
        append("""<div class="board" ${if (uiState.isFlipped) "data-flipped='true'" else ""}>""")
        
        // Render squares - adjust order based on flip state
        val ranks = if (uiState.isFlipped) (1..8) else (8 downTo 1)
        val files = if (uiState.isFlipped) ('h' downTo 'a') else ('a'..'h')
        
        for (rank in ranks) {
            for (file in files) {
                val square = Square(file, rank)
                val piece = uiState.boardState[square]
                val isDark = (file - 'a' + rank) % 2 == 0
                val isSelected = square == uiState.selectedSquare
                val isPossibleMove = square in uiState.possibleMoves
                val isLastMoveFrom = square == uiState.lastMove?.from
                val isLastMoveTo = square == uiState.lastMove?.to
                val isCheck = uiState.isCheck && piece?.let { 
                    it is ChessPiece.King && it.color == uiState.currentTurn 
                } == true
                
                val classes = buildString {
                    append("square ")
                    append(if (isDark) "dark" else "light")
                    if (isSelected) append(" selected")
                    if (isPossibleMove) append(" possible-move")
                    if (isLastMoveFrom || isLastMoveTo) append(" last-move")
                    if (isCheck) append(" check")
                }
                
                append("""<div class="$classes" data-square="$square">""")
                
                if (piece != null && !uiState.animatingPieces.contains(square)) {
                    val pieceSymbol = getPieceSymbol(piece)
                    val pieceColor = if (piece.color == ChessColor.WHITE) "white" else "black"
                    append("""<div class="piece $pieceColor" data-piece="$pieceSymbol">$pieceSymbol</div>""")
                }
                
                if (isPossibleMove) {
                    val captureClass = if (piece != null) "capture-dot" else "move-dot"
                    append("""<div class="$captureClass"></div>""")
                }
                
                append("</div>")
            }
        }
        
        // Render animating pieces
        uiState.animations.forEach { animation ->
            if (animation.isActive) {
                val piece = animation.piece
                val pieceSymbol = getPieceSymbol(piece)
                val pieceColor = if (piece.color == ChessColor.WHITE) "white" else "black"
                
                // Calculate coordinates based on board orientation to match grid rendering
                val fromFile = if (uiState.isFlipped) ('h' - animation.fromSquare.file) else (animation.fromSquare.file - 'a')
                val fromRank = if (uiState.isFlipped) (8 - animation.fromSquare.rank) else (animation.fromSquare.rank - 1)
                val toFile = if (uiState.isFlipped) ('h' - animation.toSquare.file) else (animation.toSquare.file - 'a')
                val toRank = if (uiState.isFlipped) (8 - animation.toSquare.rank) else (animation.toSquare.rank - 1)
                
                append("""
                    <div class="piece animating $pieceColor" 
                         data-piece="$pieceSymbol"
                         data-from="${animation.fromSquare}"
                         data-to="${animation.toSquare}"
                         style="--from-file: $fromFile; 
                                --from-rank: $fromRank;
                                --to-file: $toFile; 
                                --to-rank: $toRank;">
                        $pieceSymbol
                    </div>
                """.trimIndent())
            }
        }
        
        append("</div>") // board
        append("</div>") // board-container
    }
    
    container.innerHTML = boardHtml
    
    // Add click handlers to squares
    val squares = container.querySelectorAll(".square")
    for (i in 0 until squares.length) {
        val square = squares.item(i) as HTMLElement
        square.addEventListener("click", { event ->
            val squareNotation = square.getAttribute("data-square") ?: return@addEventListener
            val file = squareNotation[0]
            val rank = squareNotation[1].digitToInt()
            viewModel.onSquareClick(Square(file, rank))
        })
    }
}

private fun getPieceSymbol(piece: ChessPiece): String {
    return when (piece) {
        is ChessPiece.Pawn -> if (piece.color == ChessColor.WHITE) "♟" else "♟"
        is ChessPiece.Knight -> if (piece.color == ChessColor.WHITE) "♞" else "♞"
        is ChessPiece.Bishop -> if (piece.color == ChessColor.WHITE) "♗" else "♗"
        is ChessPiece.Rook -> if (piece.color == ChessColor.WHITE) "♜" else "♜"
        is ChessPiece.Queen -> if (piece.color == ChessColor.WHITE) "♛" else "♛"
        is ChessPiece.King -> if (piece.color == ChessColor.WHITE) "♚" else "♚"
    }
}

fun renderMoveHistory(container: Element, moveHistory: List<DetailedMove>) {
    val movesHtml = buildString {
        append("""<h3>Move History</h3>""")
        append("""<div class="moves-list">""")
        
        moveHistory.chunked(2).forEachIndexed { index, moves ->
            append("""<div class="move-pair">""")
            append("""<span class="move-number">${index + 1}.</span>""")
            
            // White move
            moves.getOrNull(0)?.let { move ->
                val quality = move.evaluation?.quality?.let { getQualitySymbol(it) } ?: ""
                append("""<span class="move white-move">${move.san}$quality</span>""")
            }
            
            // Black move
            moves.getOrNull(1)?.let { move ->
                val quality = move.evaluation?.quality?.let { getQualitySymbol(it) } ?: ""
                append("""<span class="move black-move">${move.san}$quality</span>""")
            }
            
            append("</div>")
        }
        
        append("</div>")
    }
    
    container.innerHTML = movesHtml
}

private fun getQualitySymbol(quality: MoveQuality): String {
    return when (quality) {
        MoveQuality.BRILLIANT -> "!!"
        MoveQuality.GREAT -> "!"
        MoveQuality.GOOD -> ""
        MoveQuality.INACCURACY -> "?!"
        MoveQuality.MISTAKE -> "?"
        MoveQuality.BLUNDER -> "??"
    }
}

fun renderCapturedPieces(container: Element, capturedPieces: List<ChessPiece>) {
    val whitePieces = capturedPieces.filter { it.color == ChessColor.WHITE }
    val blackPieces = capturedPieces.filter { it.color == ChessColor.BLACK }
    
    container.innerHTML = """
        <h3>Captured Pieces</h3>
        <div class="captured-section">
            <div class="captured-row white">
                ${whitePieces.joinToString("") { getPieceSymbol(it) }}
            </div>
            <div class="captured-row black">
                ${blackPieces.joinToString("") { getPieceSymbol(it) }}
            </div>
        </div>
    """.trimIndent()
}