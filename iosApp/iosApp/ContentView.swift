import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = ChessBoardViewModelWrapper()

    var body: some View {
        NavigationStack {
            ZStack {
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

                if viewModel.showPromotionDialog {
                    Color.black.opacity(0.4)
                        .ignoresSafeArea()
                        .onTapGesture { viewModel.cancelPromotion() }

                    PromotionDialogView(viewModel: viewModel)
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 4) {
                        Text("Chess")
                            .font(.title2)
                            .fontWeight(.bold)
                        Text("Predictor")
                            .font(.title2)
                            .fontWeight(.light)
                            .foregroundColor(.accentColor)
                    }
                }
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
