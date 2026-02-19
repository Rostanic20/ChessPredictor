import SwiftUI
import shared

struct PlayModeSelector: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "play.fill")
                    .font(.caption)
                    .foregroundColor(.accentColor)
                Text("Play Mode")
                    .font(.subheadline)
                    .fontWeight(.bold)
            }

            HStack(spacing: 8) {
                PlayModeChip(
                    label: "vs AI",
                    isSelected: viewModel.playMode == .vsEngine,
                    action: { viewModel.setPlayMode(.vsEngine) }
                )
                PlayModeChip(
                    label: "vs Human",
                    isSelected: viewModel.playMode == .vsHuman,
                    action: { viewModel.setPlayMode(.vsHuman) }
                )
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }
}

struct PlayModeChip: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .fontWeight(.medium)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .background(isSelected ? Color.accentColor.opacity(0.15) : Color(.secondarySystemGroupedBackground))
                .foregroundColor(isSelected ? .accentColor : .primary)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 1)
                )
        }
    }
}

struct PlayerColorSelector: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "pencil")
                    .font(.caption)
                    .foregroundColor(.accentColor)
                Text("Play as")
                    .font(.subheadline)
                    .fontWeight(.bold)
            }

            HStack(spacing: 8) {
                colorButton(label: "White", fill: Color.white, hasBorder: true, selected: viewModel.playerColor == .white) {
                    viewModel.setPlayerColor(.white)
                }
                colorButton(label: "Black", fill: Color.black, hasBorder: false, selected: viewModel.playerColor == .black) {
                    viewModel.setPlayerColor(.black)
                }
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }

    private func colorButton(label: String, fill: Color, hasBorder: Bool, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(fill)
                    .frame(width: 16, height: 16)
                    .overlay(
                        hasBorder ? RoundedRectangle(cornerRadius: 4).stroke(Color.gray, lineWidth: 1) : nil
                    )
                Text(label)
                    .font(.caption)
                    .fontWeight(.medium)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
            .background(selected ? Color.accentColor.opacity(0.15) : Color(.secondarySystemGroupedBackground))
            .foregroundColor(selected ? .accentColor : .primary)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(selected ? Color.accentColor : Color.clear, lineWidth: 1)
            )
        }
    }
}

struct DifficultySelector: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper
    @State private var expanded = false

    private var currentDifficulty: EngineDifficulty {
        viewModel.engineSettings?.difficulty ?? .clubPlayer
    }

    private static let difficulties: [EngineDifficulty] = [
        .novice, .beginner, .casual, .intermediate, .clubPlayer, .strongClub, .expert, .master, .maximum
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: { withAnimation { expanded.toggle() } }) {
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "gearshape.fill")
                            .font(.caption)
                            .foregroundColor(.accentColor)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("AI Difficulty")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                            Text(currentDifficulty.displayName)
                                .font(.caption)
                                .foregroundColor(.accentColor)
                        }
                    }

                    Spacer()

                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
                .padding(16)
            }

            if expanded {
                difficultyList
                    .padding(.bottom, 8)
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }

    private var difficultyList: some View {
        VStack(spacing: 0) {
            ForEach(Self.difficulties, id: \.self) { difficulty in
                difficultyRow(difficulty)
            }
        }
    }

    private func difficultyRow(_ difficulty: EngineDifficulty) -> some View {
        Button(action: {
            selectDifficulty(difficulty)
        }) {
            HStack(spacing: 8) {
                Image(systemName: currentDifficulty == difficulty ? "largecircle.fill.circle" : "circle")
                    .foregroundColor(currentDifficulty == difficulty ? .accentColor : .secondary)
                    .font(.body)

                VStack(alignment: .leading, spacing: 2) {
                    Text(difficulty.displayName)
                        .font(.subheadline)
                        .fontWeight(currentDifficulty == difficulty ? .bold : .regular)
                        .foregroundColor(.primary)
                    Text(difficultyDescription(difficulty))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("~\(800 + Int(difficulty.skillLevel) * 100) Elo")
                        .font(.caption2)
                        .foregroundColor(.accentColor)
                }

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    private func selectDifficulty(_ difficulty: EngineDifficulty) {
        guard let settings = viewModel.engineSettings else { return }
        let newSettings = EngineSettings(
            difficulty: difficulty,
            analysisDepth: difficulty.depth,
            timeLimit: difficulty.timeMs,
            threads: settings.threads,
            hashSize: settings.hashSize,
            skillLevel: difficulty.skillLevel,
            useBook: settings.useBook,
            contempt: settings.contempt,
            multiPV: settings.multiPV,
            humanStyle: settings.humanStyle,
            behaviorProfile: settings.behaviorProfile
        )
        viewModel.setEngineSettings(newSettings)
        withAnimation { expanded = false }
    }

    private func difficultyDescription(_ d: EngineDifficulty) -> String {
        switch d {
        case .novice: return "Beginner level with frequent mistakes"
        case .beginner: return "Learning player, makes obvious errors"
        case .casual: return "Recreational player level"
        case .intermediate: return "Club beginner strength"
        case .clubPlayer: return "Average club player"
        case .strongClub: return "Strong club player"
        case .expert: return "Expert level player"
        case .master: return "Master strength"
        case .maximum: return "Near maximum engine strength"
        default: return ""
        }
    }
}
