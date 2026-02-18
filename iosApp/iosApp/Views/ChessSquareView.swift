import SwiftUI

struct ChessSquareView: View {
    let file: Character
    let rank: Int
    let piece: String?
    let isLight: Bool
    let isSelected: Bool
    let isPossibleMove: Bool
    let isLastMove: Bool
    let isKingInCheck: Bool
    let onTap: () -> Void

    private var backgroundColor: Color {
        if isKingInCheck {
            return Color(red: 1.0, green: 0.42, blue: 0.42)
        }
        if isSelected {
            return Color(red: 0.50, green: 0.65, blue: 0.31)
        }
        if isLastMove {
            if isLight {
                return Color(red: 0.97, green: 0.93, blue: 0.51)
            } else {
                return Color(red: 0.85, green: 0.79, blue: 0.38)
            }
        }
        if isLight {
            return Color(red: 0.94, green: 0.85, blue: 0.71)
        } else {
            return Color(red: 0.71, green: 0.53, blue: 0.39)
        }
    }

    var body: some View {
        ZStack {
            Rectangle()
                .fill(backgroundColor)

            if let piece = piece {
                Text(piece)
                    .font(.system(size: 32))
            }

            if isPossibleMove {
                if piece != nil {
                    Circle()
                        .stroke(Color.black.opacity(0.3), lineWidth: 2)
                        .frame(width: 38, height: 38)
                } else {
                    Circle()
                        .fill(Color.black.opacity(0.25))
                        .frame(width: 14, height: 14)
                }
            }
        }
        .onTapGesture {
            onTap()
        }
    }
}
