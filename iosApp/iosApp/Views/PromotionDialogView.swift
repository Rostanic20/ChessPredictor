import SwiftUI
import shared

struct PromotionDialogView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    private var isWhite: Bool {
        viewModel.promotionColor == .white
    }

    private var pieces: [(String, () -> ChessPiece)] {
        let color = viewModel.promotionColor
        return [
            (isWhite ? "\u{2655}" : "\u{265B}", { ChessPiece.Queen(color: color) }),
            (isWhite ? "\u{2656}" : "\u{265C}", { ChessPiece.Rook(color: color) }),
            (isWhite ? "\u{2657}" : "\u{265D}", { ChessPiece.Bishop(color: color) }),
            (isWhite ? "\u{2658}" : "\u{265E}", { ChessPiece.Knight(color: color) })
        ]
    }

    var body: some View {
        VStack(spacing: 16) {
            Text("Promote Pawn")
                .font(.headline)
                .fontWeight(.bold)

            HStack(spacing: 12) {
                ForEach(0..<4, id: \.self) { index in
                    let piece = pieces[index]
                    Button(action: {
                        viewModel.completePromotion(piece.1())
                    }) {
                        Text(piece.0)
                            .font(.system(size: 40))
                            .frame(width: 60, height: 60)
                            .background(Color(.secondarySystemGroupedBackground))
                            .cornerRadius(12)
                    }
                }
            }

            Button("Cancel") {
                viewModel.cancelPromotion()
            }
            .font(.subheadline)
            .foregroundColor(.secondary)
        }
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(20)
        .shadow(color: .black.opacity(0.3), radius: 20, x: 0, y: 10)
    }
}
