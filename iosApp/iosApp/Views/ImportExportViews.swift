import SwiftUI
import shared

struct ExportGameView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper
    @Environment(\.dismiss) private var dismiss
    @State private var selectedFormat: ExportFormat = .pgn
    @State private var exportedContent: String? = nil

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                formatSelector

                Button(action: {
                    exportedContent = viewModel.exportGame(selectedFormat)
                }) {
                    HStack {
                        Image(systemName: "square.and.arrow.up")
                        Text("Export")
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .padding(.horizontal)

                if let content = exportedContent {
                    exportedContentView(content)
                }

                Spacer()
            }
            .padding(.top)
            .navigationTitle("Export Game")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var formatSelector: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach([ExportFormat.pgn, .json, .fen], id: \.self) { format in
                Button(action: { selectedFormat = format }) {
                    HStack {
                        Image(systemName: selectedFormat == format ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(selectedFormat == format ? .accentColor : .secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(formatName(format))
                                .font(.body)
                                .foregroundColor(.primary)
                            Text(formatDescription(format))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
                }
            }
        }
    }

    private func exportedContentView(_ content: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Exported Content")
                .font(.subheadline)
                .fontWeight(.medium)
                .padding(.horizontal)

            ScrollView {
                Text(content)
                    .font(.system(.caption, design: .monospaced))
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxHeight: 200)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(8)
            .padding(.horizontal)

            HStack(spacing: 12) {
                Button(action: {
                    UIPasteboard.general.string = content
                }) {
                    HStack {
                        Image(systemName: "doc.on.doc")
                        Text("Copy")
                    }
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(8)
                }

                Button(action: {
                    let av = UIActivityViewController(activityItems: [content], applicationActivities: nil)
                    if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                       let root = windowScene.windows.first?.rootViewController {
                        root.present(av, animated: true)
                    }
                }) {
                    HStack {
                        Image(systemName: "square.and.arrow.up")
                        Text("Share")
                    }
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(8)
                }
            }
            .padding(.horizontal)
        }
    }

    private func formatName(_ format: ExportFormat) -> String {
        switch format {
        case .pgn: return "PGN"
        case .json: return "JSON"
        case .fen: return "FEN"
        default: return "Unknown"
        }
    }

    private func formatDescription(_ format: ExportFormat) -> String {
        switch format {
        case .pgn: return "Standard chess notation"
        case .json: return "Full game data with analysis"
        case .fen: return "Current position only"
        default: return ""
        }
    }
}

struct ImportGameView: View {
    @ObservedObject var viewModel: ChessBoardViewModelWrapper
    @Environment(\.dismiss) private var dismiss
    @State private var selectedFormat: ExportFormat = .pgn
    @State private var importContent: String = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                formatSelector

                HStack(spacing: 12) {
                    Button(action: {
                        if let text = UIPasteboard.general.string {
                            importContent = text
                        }
                    }) {
                        HStack {
                            Image(systemName: "doc.on.clipboard")
                            Text("Paste")
                        }
                        .font(.subheadline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(8)
                    }
                }
                .padding(.horizontal)

                VStack(alignment: .leading, spacing: 4) {
                    Text("Game Data")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)

                    TextEditor(text: $importContent)
                        .font(.system(.caption, design: .monospaced))
                        .frame(minHeight: 150)
                        .padding(8)
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(8)
                        .padding(.horizontal)
                }

                Button(action: {
                    if !importContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        viewModel.importGame(importContent, selectedFormat)
                        dismiss()
                    }
                }) {
                    HStack {
                        Image(systemName: "square.and.arrow.down")
                        Text("Import Game")
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        importContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                            ? Color.gray : Color.accentColor
                    )
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(importContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .padding(.horizontal)

                Spacer()
            }
            .padding(.top)
            .navigationTitle("Import Game")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private var formatSelector: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach([ExportFormat.pgn, .json, .fen], id: \.self) { format in
                Button(action: { selectedFormat = format }) {
                    HStack {
                        Image(systemName: selectedFormat == format ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(selectedFormat == format ? .accentColor : .secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(formatName(format))
                                .font(.body)
                                .foregroundColor(.primary)
                            Text(formatDescription(format))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
                }
            }
        }
    }

    private func formatName(_ format: ExportFormat) -> String {
        switch format {
        case .pgn: return "PGN"
        case .json: return "JSON"
        case .fen: return "FEN"
        default: return "Unknown"
        }
    }

    private func formatDescription(_ format: ExportFormat) -> String {
        switch format {
        case .pgn: return "Standard chess notation"
        case .json: return "Full game data with analysis"
        case .fen: return "Position string"
        default: return ""
        }
    }
}
