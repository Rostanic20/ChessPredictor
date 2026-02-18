import SwiftUI
import shared

struct GameStatusView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    private var isGameOver: Bool {
        viewModel.isCheckmate || viewModel.isStalemate || viewModel.isDraw
    }

    var body: some View {
        if isGameOver {
            gameOverCard
        } else {
            activeGameCard
        }
    }

    private var gameOverCard: some View {
        HStack {
            Spacer()
            Text(gameOverText)
                .font(.title3)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(16)
        .background(viewModel.isCheckmate ? Color.red.opacity(0.15) : Color.secondary.opacity(0.15))
        .cornerRadius(12)
    }

    private var gameOverText: String {
        if viewModel.isCheckmate {
            let winner = viewModel.currentTurn == "WHITE" ? "Black" : "White"
            return "CHECKMATE! \(winner) wins!"
        }
        if viewModel.isStalemate { return "STALEMATE! Game is a draw." }
        return "DRAW! Game ended in a draw."
    }

    private var activeGameCard: some View {
        HStack(spacing: 12) {
            HStack(spacing: 8) {
                Text("Turn:")
                    .font(.subheadline)

                RoundedRectangle(cornerRadius: 4)
                    .fill(viewModel.currentTurn == "WHITE" ? Color.white : Color.black)
                    .frame(width: 20, height: 20)
                    .overlay(
                        RoundedRectangle(cornerRadius: 4)
                            .stroke(Color.gray, lineWidth: 1)
                    )

                if viewModel.isCheck {
                    Text("CHECK!")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.red)
                }
            }

            Spacer()

            if viewModel.isThinking {
                HStack(spacing: 8) {
                    ProgressView()
                        .scaleEffect(0.7)
                    Text("Thinking...")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            } else if !viewModel.isEngineReady {
                Text("Engine loading...")
                    .font(.caption)
                    .foregroundColor(.red)
            }
        }
        .padding(12)
        .background(
            viewModel.isCheck
                ? Color.red.opacity(0.1)
                : Color(.secondarySystemGroupedBackground)
        )
        .cornerRadius(8)
    }
}
