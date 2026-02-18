import SwiftUI

struct ChessBoardView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    private let files: [Character] = ["a", "b", "c", "d", "e", "f", "g", "h"]

    private var orderedFiles: [Character] {
        viewModel.isFlipped ? files.reversed() : files
    }

    private var orderedRanks: [Int] {
        viewModel.isFlipped ? Array(1...8) : Array((1...8).reversed())
    }

    var body: some View {
        GeometryReader { geo in
            let showCoords = viewModel.showCoordinates
            let coordSize: CGFloat = showCoords ? 16 : 0
            let boardSize = min(geo.size.width, geo.size.height) - coordSize
            let squareSize = boardSize / 8

            VStack(spacing: 0) {
                if showCoords {
                    HStack(spacing: 0) {
                        Spacer().frame(width: coordSize)
                        ForEach(orderedFiles, id: \.self) { file in
                            Text(String(file))
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(.secondary)
                                .frame(width: squareSize, height: coordSize)
                        }
                    }
                }

                HStack(spacing: 0) {
                    if showCoords {
                        VStack(spacing: 0) {
                            ForEach(orderedRanks, id: \.self) { rank in
                                Text("\(rank)")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundColor(.secondary)
                                    .frame(width: coordSize, height: squareSize)
                            }
                        }
                    }

                    VStack(spacing: 0) {
                        ForEach(orderedRanks, id: \.self) { rank in
                            HStack(spacing: 0) {
                                ForEach(orderedFiles, id: \.self) { file in
                                    let key = "\(file)\(rank)"
                                    let piece = viewModel.boardState[key]
                                    let fileIndex = files.firstIndex(of: file)!
                                    let isLight = (fileIndex + rank) % 2 != 0

                                    ChessSquareView(
                                        file: file,
                                        rank: rank,
                                        piece: piece,
                                        isLight: isLight,
                                        isSelected: viewModel.isSquareSelected(file, rank),
                                        isPossibleMove: viewModel.isPossibleMove(file, rank),
                                        isLastMove: viewModel.isLastMoveSquare(file, rank),
                                        isKingInCheck: viewModel.isKingInCheck(file, rank),
                                        onTap: {
                                            viewModel.onSquareClick(file, Int32(rank))
                                        }
                                    )
                                    .frame(width: squareSize, height: squareSize)
                                }
                            }
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.black.opacity(0.3), lineWidth: 2)
                    )
                    .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                }

                if showCoords {
                    HStack(spacing: 0) {
                        Spacer().frame(width: coordSize)
                        ForEach(orderedFiles, id: \.self) { file in
                            Text(String(file))
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(.secondary)
                                .frame(width: squareSize, height: coordSize)
                        }
                    }
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}
