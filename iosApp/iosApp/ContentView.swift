import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = ChessBoardViewModelWrapper()
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        NavigationStack {
            ZStack {
                if horizontalSizeClass == .regular {
                    WideLayout(viewModel: viewModel)
                } else {
                    CompactLayout(viewModel: viewModel)
                }

                if viewModel.showPromotionDialog {
                    Color.black.opacity(0.4)
                        .ignoresSafeArea()
                        .onTapGesture { viewModel.cancelPromotion() }

                    PromotionDialogView(viewModel: viewModel)
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarLeading) {
                    Button(action: { viewModel.undoLastMove() }) {
                        Image(systemName: "arrow.uturn.backward")
                    }
                    Menu {
                        Button(action: { viewModel.showExportDialog = true }) {
                            Label("Export Game", systemImage: "square.and.arrow.up")
                        }
                        Button(action: { viewModel.showImportDialog = true }) {
                            Label("Import Game", systemImage: "square.and.arrow.down")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.toggleCoordinates() }) {
                        Image(systemName: viewModel.showCoordinates ? "checklist" : "list.bullet")
                            .foregroundColor(viewModel.showCoordinates ? .accentColor : .secondary)
                    }
                    Button(action: { viewModel.toggleBoardFlip() }) {
                        Image(systemName: "arrow.up.arrow.down")
                    }
                    Button(action: { viewModel.newGame() }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .alert("Start New Game?", isPresented: $viewModel.showNewGameDialog) {
                Button("Cancel", role: .cancel) { viewModel.cancelNewGame() }
                Button("New Game", role: .destructive) { viewModel.confirmNewGame() }
            } message: {
                Text("Your current game progress will be lost.")
            }
            .sheet(isPresented: $viewModel.showExportDialog) {
                ExportGameView(viewModel: viewModel)
            }
            .sheet(isPresented: $viewModel.showImportDialog) {
                ImportGameView(viewModel: viewModel)
            }
        }
    }
}

struct CompactLayout: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                GameStatusView(viewModel: viewModel)

                ChessBoardCard(viewModel: viewModel)

                if !viewModel.moveHistory.isEmpty {
                    MoveHistoryView(viewModel: viewModel)
                }

                PlayModeSelector(viewModel: viewModel)

                if viewModel.playMode == .vsEngine {
                    PlayerColorSelector(viewModel: viewModel)
                }

                DifficultySelector(viewModel: viewModel)
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }
}

struct WideLayout: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(spacing: 12) {
                if viewModel.isCheckmate || viewModel.isStalemate || viewModel.isDraw {
                    GameStatusView(viewModel: viewModel)
                }

                ChessBoardCard(viewModel: viewModel)
                    .frame(maxWidth: 500)
            }
            .frame(maxHeight: .infinity)

            ScrollView {
                VStack(spacing: 12) {
                    if !viewModel.isCheckmate && !viewModel.isStalemate && !viewModel.isDraw {
                        GameStatusView(viewModel: viewModel)
                    }

                    if !viewModel.moveHistory.isEmpty {
                        MoveHistoryView(viewModel: viewModel)
                    }

                    PlayModeSelector(viewModel: viewModel)

                    if viewModel.playMode == .vsEngine {
                        PlayerColorSelector(viewModel: viewModel)
                    }

                    DifficultySelector(viewModel: viewModel)
                }
                .padding(.vertical, 16)
            }
        }
        .padding(.horizontal, 16)
        .background(Color(.systemGroupedBackground))
    }
}

struct ChessBoardCard: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    var body: some View {
        VStack(spacing: 8) {
            ChessBoardView(viewModel: viewModel)
                .padding(.horizontal, 8)

            CapturedPiecesView(viewModel: viewModel)
        }
        .padding(8)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}
