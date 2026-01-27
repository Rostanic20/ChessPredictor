package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.opposite
import kotlin.math.abs // Used in generateKingMoves for castling detection

class MoveGenerator {
    
    fun generateLegalMoves(gameState: GameState): List<ChessMove> {
        val pseudoLegalMoves = generatePseudoLegalMoves(gameState)
        return pseudoLegalMoves.filter { move ->
            val newState = simulateMove(gameState, move)
            !wouldKingBeInCheck(newState, gameState.turn)
        }
    }
    
    fun getLegalMoves(gameState: GameState): List<ChessMove> {
        return generateLegalMoves(gameState)
    }
    
    fun generatePseudoLegalMoves(gameState: GameState): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        
        for ((square, piece) in gameState.board) {
            if (piece.color == gameState.turn) {
                moves.addAll(generatePieceMovesFrom(gameState, square, piece))
            }
        }
        
        return moves
    }
    
    private fun generatePieceMovesFrom(gameState: GameState, from: Square, piece: ChessPiece): List<ChessMove> {
        return when (piece) {
            is ChessPiece.Pawn -> generatePawnMoves(gameState, from, piece)
            is ChessPiece.Rook -> generateRookMoves(gameState, from, piece)
            is ChessPiece.Knight -> generateKnightMoves(gameState, from, piece)
            is ChessPiece.Bishop -> generateBishopMoves(gameState, from, piece)
            is ChessPiece.Queen -> generateQueenMoves(gameState, from, piece)
            is ChessPiece.King -> generateKingMoves(gameState, from, piece)
        }
    }
    
    private fun generatePawnMoves(gameState: GameState, from: Square, pawn: ChessPiece.Pawn): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val direction = if (pawn.color == ChessColor.WHITE) 1 else -1
        val startRank = if (pawn.color == ChessColor.WHITE) 2 else 7
        val promotionRank = if (pawn.color == ChessColor.WHITE) 8 else 1

        generatePawnForwardMoves(gameState, from, pawn, direction, startRank, promotionRank, moves)
        generatePawnCaptures(gameState, from, pawn, direction, promotionRank, moves)

        return moves
    }

    private fun generatePawnForwardMoves(
        gameState: GameState, from: Square, pawn: ChessPiece.Pawn,
        direction: Int, startRank: Int, promotionRank: Int, moves: MutableList<ChessMove>
    ) {
        val oneForward = Square(from.file, from.rank + direction)
        if (!isValidSquare(oneForward) || gameState.board[oneForward] != null) return

        addPawnMoveOrPromotion(from, oneForward, pawn, null, promotionRank, moves)

        if (from.rank == startRank) {
            val twoForward = Square(from.file, from.rank + 2 * direction)
            if (isValidSquare(twoForward) && gameState.board[twoForward] == null) {
                moves.add(ChessMove(from, twoForward, pawn))
            }
        }
    }

    private fun generatePawnCaptures(
        gameState: GameState, from: Square, pawn: ChessPiece.Pawn,
        direction: Int, promotionRank: Int, moves: MutableList<ChessMove>
    ) {
        for (fileDelta in listOf(-1, 1)) {
            val captureFile = (from.file.code + fileDelta).toChar()
            if (captureFile !in 'a'..'h') continue

            val captureSquare = Square(captureFile, from.rank + direction)
            if (!isValidSquare(captureSquare)) continue

            val targetPiece = gameState.board[captureSquare]
            if (targetPiece != null && targetPiece.color != pawn.color) {
                addPawnMoveOrPromotion(from, captureSquare, pawn, targetPiece, promotionRank, moves)
            }

            if (captureSquare == gameState.enPassantSquare) {
                val capturedPawn = gameState.board[Square(captureFile, from.rank)]
                moves.add(ChessMove(from, captureSquare, pawn, capturedPawn))
            }
        }
    }

    private fun addPawnMoveOrPromotion(
        from: Square, to: Square, pawn: ChessPiece.Pawn,
        captured: ChessPiece?, promotionRank: Int, moves: MutableList<ChessMove>
    ) {
        if (to.rank == promotionRank) {
            for (piece in getPromotionPieces(pawn.color)) {
                moves.add(ChessMove(from, to, pawn, captured, piece))
            }
        } else {
            moves.add(ChessMove(from, to, pawn, captured))
        }
    }

    private fun getPromotionPieces(color: ChessColor) = listOf(
        ChessPiece.Queen(color), ChessPiece.Rook(color),
        ChessPiece.Bishop(color), ChessPiece.Knight(color)
    )
    
    private fun generateRookMoves(gameState: GameState, from: Square, rook: ChessPiece.Rook): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
        
        for ((deltaFile, deltaRank) in directions) {
            var file = from.file.code + deltaFile
            var rank = from.rank + deltaRank
            
            while (file in 'a'.code..'h'.code && rank in 1..8) {
                val targetSquare = Square(file.toChar(), rank)
                val targetPiece = gameState.board[targetSquare]
                
                if (targetPiece == null) {
                    moves.add(ChessMove(from, targetSquare, rook))
                } else {
                    if (targetPiece.color != rook.color) {
                        moves.add(ChessMove(from, targetSquare, rook, targetPiece))
                    }
                    break
                }
                
                file += deltaFile
                rank += deltaRank
            }
        }
        
        return moves
    }
    
    private fun generateKnightMoves(gameState: GameState, from: Square, knight: ChessPiece.Knight): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val knightMoves = listOf(
            Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1),
            Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2)
        )
        
        for ((deltaFile, deltaRank) in knightMoves) {
            val targetFile = (from.file.code + deltaFile).toChar()
            val targetRank = from.rank + deltaRank
            
            if (targetFile in 'a'..'h' && targetRank in 1..8) {
                val targetSquare = Square(targetFile, targetRank)
                val targetPiece = gameState.board[targetSquare]
                
                if (targetPiece == null || targetPiece.color != knight.color) {
                    moves.add(ChessMove(from, targetSquare, knight, targetPiece))
                }
            }
        }
        
        return moves
    }
    
    private fun generateBishopMoves(gameState: GameState, from: Square, bishop: ChessPiece.Bishop): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val directions = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
        
        for ((deltaFile, deltaRank) in directions) {
            var file = from.file.code + deltaFile
            var rank = from.rank + deltaRank
            
            while (file in 'a'.code..'h'.code && rank in 1..8) {
                val targetSquare = Square(file.toChar(), rank)
                val targetPiece = gameState.board[targetSquare]
                
                if (targetPiece == null) {
                    moves.add(ChessMove(from, targetSquare, bishop))
                } else {
                    if (targetPiece.color != bishop.color) {
                        moves.add(ChessMove(from, targetSquare, bishop, targetPiece))
                    }
                    break
                }
                
                file += deltaFile
                rank += deltaRank
            }
        }
        
        return moves
    }
    
    private fun generateQueenMoves(gameState: GameState, from: Square, queen: ChessPiece.Queen): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        // Queen moves like rook + bishop
        val directions = listOf(
            Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0),  // Rook directions
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)  // Bishop directions
        )
        
        for ((deltaFile, deltaRank) in directions) {
            var file = from.file.code + deltaFile
            var rank = from.rank + deltaRank
            
            while (file in 'a'.code..'h'.code && rank in 1..8) {
                val targetSquare = Square(file.toChar(), rank)
                val targetPiece = gameState.board[targetSquare]
                
                if (targetPiece == null) {
                    moves.add(ChessMove(from, targetSquare, queen))
                } else {
                    if (targetPiece.color != queen.color) {
                        moves.add(ChessMove(from, targetSquare, queen, targetPiece))
                    }
                    break
                }
                
                file += deltaFile
                rank += deltaRank
            }
        }
        
        return moves
    }
    
    private fun generateKingMoves(gameState: GameState, from: Square, king: ChessPiece.King): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        
        // Regular king moves
        for (deltaFile in -1..1) {
            for (deltaRank in -1..1) {
                if (deltaFile == 0 && deltaRank == 0) continue
                
                val targetFile = (from.file.code + deltaFile).toChar()
                val targetRank = from.rank + deltaRank
                
                if (targetFile in 'a'..'h' && targetRank in 1..8) {
                    val targetSquare = Square(targetFile, targetRank)
                    val targetPiece = gameState.board[targetSquare]
                    
                    if (targetPiece == null || targetPiece.color != king.color) {
                        moves.add(ChessMove(from, targetSquare, king, targetPiece))
                    }
                }
            }
        }
        
        // Castling
        if (!wouldKingBeInCheck(gameState, king.color)) {
            // Kingside castling
            if (canCastleKingside(gameState, king.color)) {
                val targetSquare = Square('g', from.rank)
                moves.add(ChessMove(from, targetSquare, king))
            }
            
            // Queenside castling
            if (canCastleQueenside(gameState, king.color)) {
                val targetSquare = Square('c', from.rank)
                moves.add(ChessMove(from, targetSquare, king))
            }
        }
        
        return moves
    }
    
    private fun canCastleKingside(gameState: GameState, color: ChessColor): Boolean {
        val rank = if (color == ChessColor.WHITE) 1 else 8
        val canCastle = if (color == ChessColor.WHITE) gameState.castlingRights.whiteKingside else gameState.castlingRights.blackKingside
        
        if (!canCastle) return false
        
        // Check squares are empty
        val squaresToCheck = listOf(Square('f', rank), Square('g', rank))
        if (squaresToCheck.any { gameState.board[it] != null }) return false
        
        // Check squares are not attacked
        return squaresToCheck.none { isSquareAttackedBy(gameState, it, color.opposite()) }
    }
    
    private fun canCastleQueenside(gameState: GameState, color: ChessColor): Boolean {
        val rank = if (color == ChessColor.WHITE) 1 else 8
        val canCastle = if (color == ChessColor.WHITE) gameState.castlingRights.whiteQueenside else gameState.castlingRights.blackQueenside
        
        if (!canCastle) return false
        
        // Check squares are empty
        val squaresToCheck = listOf(Square('b', rank), Square('c', rank), Square('d', rank))
        if (squaresToCheck.any { gameState.board[it] != null }) return false
        
        // Check squares are not attacked (only c and d need to be safe)
        val squaresToCheckAttack = listOf(Square('c', rank), Square('d', rank))
        return squaresToCheckAttack.none { isSquareAttackedBy(gameState, it, color.opposite()) }
    }
    
    private fun isValidSquare(square: Square): Boolean {
        return square.file in 'a'..'h' && square.rank in 1..8
    }

    private fun wouldKingBeInCheck(gameState: GameState, color: ChessColor): Boolean {
        val kingSquare = AttackDetector.findKingSquare(gameState, color) ?: return false
        return AttackDetector.isSquareAttackedBy(gameState, kingSquare, color.opposite())
    }

    private fun isSquareAttackedBy(gameState: GameState, square: Square, byColor: ChessColor): Boolean {
        return AttackDetector.isSquareAttackedBy(gameState, square, byColor)
    }

    private fun simulateMove(gameState: GameState, move: ChessMove): GameState {
        val newBoard = gameState.board.toMutableMap()
        
        // Remove piece from source
        newBoard.remove(move.from)
        
        // Place piece on destination
        val finalPiece = move.promotion ?: move.piece
        newBoard[move.to] = finalPiece
        
        // Handle special moves
        when {
            // Castling - move the rook
            move.piece is ChessPiece.King && abs(move.from.file.code - move.to.file.code) == 2 -> {
                val isKingside = move.to.file > move.from.file
                val rookFromFile = if (isKingside) 'h' else 'a'
                val rookToFile = if (isKingside) 'f' else 'd'
                val rank = move.from.rank
                
                val rook = newBoard.remove(Square(rookFromFile, rank))
                if (rook != null) {
                    newBoard[Square(rookToFile, rank)] = rook
                }
            }
            
            // En passant capture
            move.piece is ChessPiece.Pawn && move.to == gameState.enPassantSquare -> {
                newBoard.remove(Square(move.to.file, move.from.rank))
            }
        }
        
        return gameState.copy(board = newBoard)
    }
}