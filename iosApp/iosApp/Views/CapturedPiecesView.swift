import SwiftUI
import shared

struct CapturedPiecesView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    var body: some View {
        HStack(spacing: 8) {
            CapturedSideView(
                label: "White captured:",
                pieces: piecesForSide(isWhiteSide: true),
                viewModel: viewModel
            )
            CapturedSideView(
                label: "Black captured:",
                pieces: piecesForSide(isWhiteSide: false),
                viewModel: viewModel
            )
        }
        .padding(.horizontal, 4)
    }

    private func piecesForSide(isWhiteSide: Bool) -> [ChessPiece] {
        return viewModel.capturedPieces
            .filter { piece in
                let pieceIsWhite = piece.color == ChessColor.white
                return isWhiteSide ? !pieceIsWhite : pieceIsWhite
            }
            .sorted { a, b in pieceValue(a) > pieceValue(b) }
    }

    private func pieceValue(_ piece: ChessPiece) -> Int {
        if piece is ChessPiece.Queen { return 9 }
        if piece is ChessPiece.Rook { return 5 }
        if piece is ChessPiece.Bishop { return 3 }
        if piece is ChessPiece.Knight { return 3 }
        if piece is ChessPiece.Pawn { return 1 }
        return 0
    }
}

struct CapturedSideView: View {
    let label: String
    let pieces: [ChessPiece]
    let viewModel: ChessBoardViewModelWrapper

    private var materialValue: Int {
        pieces.reduce(0) { total, piece in
            if piece is ChessPiece.Queen { return total + 9 }
            if piece is ChessPiece.Rook { return total + 5 }
            if piece is ChessPiece.Bishop { return total + 3 }
            if piece is ChessPiece.Knight { return total + 3 }
            if piece is ChessPiece.Pawn { return total + 1 }
            return total
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundColor(.secondary)
                Spacer()
                if materialValue > 0 {
                    Text("+\(materialValue)")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.accentColor)
                }
            }

            HStack(spacing: 2) {
                if pieces.isEmpty {
                    Text("None")
                        .font(.caption2)
                        .foregroundColor(.secondary.opacity(0.5))
                } else {
                    ForEach(Array(pieces.enumerated()), id: \.offset) { _, piece in
                        Text(viewModel.getPieceUnicode(piece))
                            .font(.system(size: 18))
                    }
                }
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(8)
    }
}
