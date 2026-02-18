import Foundation
import SwiftUI
import shared

@MainActor
class ChessBoardViewModelWrapper: ObservableObject {
    let vm: ChessBoardViewModel
    private let gameStateRepo: IosGameStateRepositoryImpl

    @Published var boardState: [String: String] = [:]
    @Published var currentTurn: String = "WHITE"
    @Published var selectedSquare: Square? = nil
    @Published var possibleMoves: [Square] = []
    @Published var lastMove: ChessMove? = nil
    @Published var isCheck: Bool = false
    @Published var isCheckmate: Bool = false
    @Published var isStalemate: Bool = false
    @Published var isDraw: Bool = false
    @Published var isFlipped: Bool = false
    @Published var showCoordinates: Bool = true
    @Published var showNewGameDialog: Bool = false
    @Published var moveHistory: [DetailedMove] = []
    @Published var capturedPieces: [ChessPiece] = []
    @Published var isThinking: Bool = false
    @Published var isEngineReady: Bool = false
    @Published var playMode: PlayMode = .vsEngine
    @Published var playerColor: PlayerColor = .white
    @Published var engineSettings: EngineSettings? = nil
    @Published var currentOpening: OpeningInfo? = nil
    @Published var pendingPromotionFrom: Square? = nil
    @Published var pendingPromotionTo: Square? = nil
    @Published var showPromotionDialog: Bool = false

    @Published var showExportDialog: Bool = false
    @Published var showImportDialog: Bool = false

    nonisolated(unsafe) private var closeables: [Closeable] = []

    init() {
        self.gameStateRepo = IosGameStateRepositoryImpl()
        self.vm = ChessBoardViewModel(
            providedAppModule: nil,
            gameStateRepository: gameStateRepo,
            chessRulesEngine: ChessRulesEngine(),
            openingDetector: OpeningDetector(),
            importExportGameUseCase: ImportExportGameUseCase(chessRulesEngine: ChessRulesEngine())
        )

        observeFlows()
        vm.initializeBoard()
    }

    private func observeFlows() {
        let uiStateCloseable = FlowUtilsKt.observe(vm.uiState) { [weak self] state in
            guard let self = self, let state = state as? ChessBoardUiState else { return }
            Task { @MainActor in
                self.updateFromUiState(state)
            }
        }
        closeables.append(uiStateCloseable)

        let thinkingCloseable = FlowUtilsKt.observe(vm.isThinking) { [weak self] value in
            guard let self = self, let thinking = value as? Bool else { return }
            Task { @MainActor in
                self.isThinking = thinking
            }
        }
        closeables.append(thinkingCloseable)

        let engineReadyCloseable = FlowUtilsKt.observe(vm.isEngineReady) { [weak self] value in
            guard let self = self, let ready = value as? Bool else { return }
            Task { @MainActor in
                self.isEngineReady = ready
            }
        }
        closeables.append(engineReadyCloseable)

        let openingCloseable = FlowUtilsKt.observe(vm.currentOpening) { [weak self] value in
            guard let self = self else { return }
            Task { @MainActor in
                self.currentOpening = value as? OpeningInfo
            }
        }
        closeables.append(openingCloseable)

    }

    private func updateFromUiState(_ state: ChessBoardUiState) {
        var newBoard: [String: String] = [:]
        for entry in state.boardState {
            if let square = entry.key as? Square, let piece = entry.value as? ChessPiece {
                guard let scalar = UnicodeScalar(square.file) else { continue }
                let fileChar = Character(scalar)
                let key = "\(fileChar)\(square.rank)"
                newBoard[key] = getPieceUnicode(piece)
            }
        }
        self.boardState = newBoard
        self.currentTurn = state.currentTurn == ChessColor.white ? "WHITE" : "BLACK"
        self.selectedSquare = state.selectedSquare
        self.possibleMoves = state.possibleMoves as? [Square] ?? []
        self.lastMove = state.lastMove
        self.isCheck = state.isCheck
        self.isCheckmate = state.isCheckmate
        self.isStalemate = state.isStalemate
        self.isDraw = state.isDraw
        self.isFlipped = state.isFlipped
        self.showCoordinates = state.showCoordinates
        self.showNewGameDialog = state.showNewGameDialog
        self.moveHistory = (state.moveHistory as? [DetailedMove]) ?? []
        self.capturedPieces = (state.capturedPieces as? [ChessPiece]) ?? []
        self.playMode = state.playMode
        self.playerColor = state.playerColor
        self.engineSettings = state.engineSettings
    }

    func onSquareClick(_ file: Character, _ rank: Int32) {
        guard let ascii = file.asciiValue else { return }
        let targetSquare = Square(file: unichar(ascii), rank: rank)

        if let sel = selectedSquare {
            let piece = boardState["\(Character(UnicodeScalar(sel.file)!))\(sel.rank)"]
            let isPawn = piece == "\u{2659}" || piece == "\u{265F}"
            let isPromotion = isPawn && (rank == 1 || rank == 8)
            if isPromotion && isPossibleMove(file, Int(rank)) {
                pendingPromotionFrom = sel
                pendingPromotionTo = targetSquare
                showPromotionDialog = true
                return
            }
        }

        vm.onSquareClick(square: targetSquare)
    }

    func completePromotion(_ piece: ChessPiece) {
        guard let from = pendingPromotionFrom, let to = pendingPromotionTo else { return }
        vm.executePromotionMove(from: from, to: to, promotionPiece: piece)
        pendingPromotionFrom = nil
        pendingPromotionTo = nil
        showPromotionDialog = false
    }

    func cancelPromotion() {
        pendingPromotionFrom = nil
        pendingPromotionTo = nil
        showPromotionDialog = false
    }

    func undoLastMove() { vm.undoLastMove() }
    func newGame() { vm.onNewGame() }
    func confirmNewGame() { vm.confirmNewGame() }
    func cancelNewGame() { vm.cancelNewGame() }
    func toggleBoardFlip() { vm.toggleBoardFlipAndSave() }
    func toggleCoordinates() { vm.toggleCoordinates() }

    func setPlayMode(_ mode: PlayMode) { vm.setPlayMode(mode: mode) }
    func setPlayerColor(_ color: PlayerColor) { vm.setPlayerColor(color: color) }
    func setEngineSettings(_ settings: EngineSettings) { vm.setEngineSettings(settings: settings) }

    func exportGame(_ format: ExportFormat) -> String {
        return vm.exportGame(format: format)
    }

    func importGame(_ content: String, _ format: ExportFormat) {
        vm.importGame(content: content, format: format)
    }

    func getPieceUnicode(_ piece: ChessPiece) -> String {
        let isWhite = piece.color == ChessColor.white
        if piece is ChessPiece.Pawn { return isWhite ? "\u{2659}" : "\u{265F}" }
        if piece is ChessPiece.Knight { return isWhite ? "\u{2658}" : "\u{265E}" }
        if piece is ChessPiece.Bishop { return isWhite ? "\u{2657}" : "\u{265D}" }
        if piece is ChessPiece.Rook { return isWhite ? "\u{2656}" : "\u{265C}" }
        if piece is ChessPiece.Queen { return isWhite ? "\u{2655}" : "\u{265B}" }
        if piece is ChessPiece.King { return isWhite ? "\u{2654}" : "\u{265A}" }
        return "?"
    }

    func isSquareSelected(_ file: Character, _ rank: Int) -> Bool {
        guard let sel = selectedSquare, let ascii = file.asciiValue else { return false }
        return sel.file == unichar(ascii) && sel.rank == Int32(rank)
    }

    func isPossibleMove(_ file: Character, _ rank: Int) -> Bool {
        guard let ascii = file.asciiValue else { return false }
        let f = unichar(ascii)
        return possibleMoves.contains { sq in
            sq.file == f && sq.rank == Int32(rank)
        }
    }

    func isLastMoveSquare(_ file: Character, _ rank: Int) -> Bool {
        guard let move = lastMove, let ascii = file.asciiValue else { return false }
        let f = unichar(ascii)
        let r = Int32(rank)
        return (move.from.file == f && move.from.rank == r) ||
               (move.to.file == f && move.to.rank == r)
    }

    func isKingInCheck(_ file: Character, _ rank: Int) -> Bool {
        guard isCheck else { return false }
        let key = "\(file)\(rank)"
        guard let piece = boardState[key] else { return false }
        let isWhiteTurn = currentTurn == "WHITE"
        return (isWhiteTurn && piece == "\u{2654}") || (!isWhiteTurn && piece == "\u{265A}")
    }

    var promotionColor: ChessColor {
        return currentTurn == "WHITE" ? .white : .black
    }

    nonisolated func cleanup() {
        let closeable = closeables
        Task { @MainActor in
            closeable.forEach { $0.close() }
        }
    }

    deinit {
        cleanup()
    }
}
