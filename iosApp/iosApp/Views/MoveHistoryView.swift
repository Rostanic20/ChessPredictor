import SwiftUI
import shared

struct MoveHistoryView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    private var movePairs: [(Int, DetailedMove, DetailedMove?)] {
        var pairs: [(Int, DetailedMove, DetailedMove?)] = []
        let moves = viewModel.moveHistory
        var i = 0
        while i < moves.count {
            let moveNumber = Int(moves[i].moveNumber)
            let whiteMove = moves[i]
            let blackMove = (i + 1 < moves.count) ? moves[i + 1] : nil
            pairs.append((moveNumber, whiteMove, blackMove))
            i += 2
        }
        return pairs
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Move History")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.secondary)

                Spacer()

                if let opening = viewModel.currentOpening?.opening {
                    Text(opening.eco)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.accentColor)
                }
            }

            if let opening = viewModel.currentOpening?.opening {
                Text(opening.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.accentColor)

                if let variation = viewModel.currentOpening?.variation {
                    Text(variation)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 2) {
                        ForEach(movePairs, id: \.0) { number, white, black in
                            HStack(spacing: 0) {
                                Text("\(number).")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                    .frame(width: 28, alignment: .leading)

                                Text(moveText(white))
                                    .font(.subheadline)
                                    .frame(width: 70, alignment: .leading)

                                if let black = black {
                                    Text(moveText(black))
                                        .font(.subheadline)
                                        .frame(width: 70, alignment: .leading)
                                }

                                Spacer()
                            }
                            .padding(.vertical, 2)
                            .id(number)
                        }
                    }
                }
                .frame(maxHeight: 150)
                .onChange(of: viewModel.moveHistory.count) { _ in
                    if let last = movePairs.last {
                        withAnimation {
                            proxy.scrollTo(last.0, anchor: .bottom)
                        }
                    }
                }
            }
        }
        .padding(12)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(8)
    }

    private func moveText(_ move: DetailedMove) -> String {
        let san = move.san
        if san.isEmpty {
            return "\(move.move.from)-\(move.move.to)"
        }
        return san
    }
}
