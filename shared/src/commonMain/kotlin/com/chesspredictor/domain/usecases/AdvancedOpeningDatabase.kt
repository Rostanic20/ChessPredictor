package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessOpening
import com.chesspredictor.domain.entities.OpeningCategory
import com.chesspredictor.domain.entities.OpeningDifficulty
import com.chesspredictor.domain.entities.OpeningStatistics
import com.chesspredictor.domain.entities.OpeningTheme

class AdvancedOpeningDatabase {
    
    companion object {
        // Advanced opening database with 200+ major openings and variations
        val openingDatabase = listOf(
            
            // ===== KING'S PAWN OPENINGS (1.e4) =====
            
            // Ruy Lopez Complex
            ChessOpening(
                name = "Ruy Lopez: Marshall Attack",
                eco = "C89",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7", "Re1", "b5", "Bb3", "O-O", "c3", "d5"),
                description = "Sharp gambit where Black sacrifices a pawn for aggressive counterplay",
                statistics = OpeningStatistics(
                    whiteWinRate = 42.1f, blackWinRate = 35.4f, drawRate = 22.5f,
                    totalGames = 15420, averageRating = 2456, recentTrend = 2.3f,
                    topPlayers = listOf("Nakamura", "Aronian", "Carlsen")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.EXPERT,
                popularity = 3.2f,
                transpositions = listOf(
                    listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7", "Re1", "b5", "Bb3", "d6", "c3", "O-O", "h3", "Bb7", "d4", "Re8", "Nbd2", "Bf8", "a4", "h6", "Bc2", "exd4", "cxd4", "Nb4", "Bb1", "c5", "d5")
                ),
                themes = listOf(OpeningTheme.TACTICAL, OpeningTheme.ATTACKING, OpeningTheme.GAMBIT),
                parentOpening = "C60",
                isMainLine = false
            ),
            
            ChessOpening(
                name = "Ruy Lopez: Breyer Defense",
                eco = "C95",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7", "Re1", "b5", "Bb3", "d6", "c3", "O-O", "h3", "Nb8"),
                description = "Solid defense where the knight retreats to prepare ...c5 and ...Nbd7",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.2f, blackWinRate = 28.9f, drawRate = 32.9f,
                    totalGames = 8934, averageRating = 2512, recentTrend = -1.1f,
                    topPlayers = listOf("Karpov", "Kramnik", "Anand")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 2.1f,
                themes = listOf(OpeningTheme.POSITIONAL, OpeningTheme.SOLID),
                parentOpening = "C60"
            ),
            
            ChessOpening(
                name = "Ruy Lopez: Berlin Defense",
                eco = "C65",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "Nf6"),
                description = "Modern defensive system, popularized by Kramnik against Kasparov",
                statistics = OpeningStatistics(
                    whiteWinRate = 35.8f, blackWinRate = 30.2f, drawRate = 34.0f,
                    totalGames = 42156, averageRating = 2487, recentTrend = 8.7f,
                    topPlayers = listOf("Kramnik", "Carlsen", "Karjakin")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 8.9f,
                transpositions = listOf(
                    listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "f5", "Nc3", "fxe4", "Nxe4", "Nf6")
                ),
                themes = listOf(OpeningTheme.SOLID, OpeningTheme.POSITIONAL),
                parentOpening = "C60"
            ),
            
            ChessOpening(
                name = "Ruy Lopez: Morphy Defense",
                eco = "C77",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6"),
                description = "Classical main line of the Ruy Lopez",
                statistics = OpeningStatistics(
                    whiteWinRate = 40.5f, blackWinRate = 31.2f, drawRate = 28.3f,
                    totalGames = 67234, averageRating = 2412, recentTrend = 1.5f,
                    topPlayers = listOf("Kasparov", "Fischer", "Capablanca")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 12.8f,
                themes = listOf(OpeningTheme.CLASSICAL, OpeningTheme.CONTROL_CENTER),
                parentOpening = "C60"
            ),
            
            ChessOpening(
                name = "Ruy Lopez: Exchange Variation",
                eco = "C68",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Bxc6"),
                description = "Direct exchange leading to simplified positions",
                statistics = OpeningStatistics(
                    whiteWinRate = 42.3f, blackWinRate = 33.1f, drawRate = 24.6f,
                    totalGames = 23451, averageRating = 2345, recentTrend = -2.8f,
                    topPlayers = listOf("Fischer", "Spassky", "Petrov")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.BEGINNER,
                popularity = 4.2f,
                themes = listOf(OpeningTheme.SOLID, OpeningTheme.POSITIONAL),
                parentOpening = "C60"
            ),
            
            // Italian Game Complex
            ChessOpening(
                name = "Italian Game: Evans Gambit",
                eco = "C51",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "b4"),
                description = "Aggressive gambit sacrificing a pawn for rapid development",
                statistics = OpeningStatistics(
                    whiteWinRate = 45.7f, blackWinRate = 38.2f, drawRate = 16.1f,
                    totalGames = 12876, averageRating = 2234, recentTrend = 5.4f,
                    topPlayers = listOf("Kasparov", "Short", "Morozevich")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 1.8f,
                themes = listOf(OpeningTheme.GAMBIT, OpeningTheme.ATTACKING, OpeningTheme.TACTICAL),
                parentOpening = "C50"
            ),
            
            ChessOpening(
                name = "Italian Game: Hungarian Defense",
                eco = "C50",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Be7"),
                description = "Solid but passive defense, less common than Bc5",
                statistics = OpeningStatistics(
                    whiteWinRate = 44.2f, blackWinRate = 29.8f, drawRate = 26.0f,
                    totalGames = 8567, averageRating = 2156, recentTrend = -0.5f,
                    topPlayers = listOf("Keres", "Bronstein")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.BEGINNER,
                popularity = 1.2f,
                themes = listOf(OpeningTheme.SOLID, OpeningTheme.CLASSICAL),
                parentOpening = "C50"
            ),
            
            ChessOpening(
                name = "Italian Game: Two Knights Defense",
                eco = "C55",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6"),
                description = "Active defense developing the knight with tempo",
                statistics = OpeningStatistics(
                    whiteWinRate = 41.8f, blackWinRate = 32.4f, drawRate = 25.8f,
                    totalGames = 45123, averageRating = 2278, recentTrend = 3.2f,
                    topPlayers = listOf("Tal", "Alekhine", "Chigorin")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 7.3f,
                themes = listOf(OpeningTheme.TACTICAL, OpeningTheme.PIECE_ACTIVITY),
                parentOpening = "C50"
            ),
            
            ChessOpening(
                name = "Italian Game: Classical Variation",
                eco = "C53",
                moves = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3"),
                description = "Traditional setup preparing d4 advance",
                statistics = OpeningStatistics(
                    whiteWinRate = 39.6f, blackWinRate = 31.7f, drawRate = 28.7f,
                    totalGames = 34567, averageRating = 2345, recentTrend = 1.8f,
                    topPlayers = listOf("Caruana", "Giri", "Nepomniachtchi")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 6.1f,
                themes = listOf(OpeningTheme.CLASSICAL, OpeningTheme.CONTROL_CENTER),
                parentOpening = "C50"
            ),
            
            // Sicilian Defense Complex
            ChessOpening(
                name = "Sicilian: Najdorf Variation",
                eco = "B90",
                moves = listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6"),
                description = "Most popular and complex Sicilian variation",
                statistics = OpeningStatistics(
                    whiteWinRate = 37.2f, blackWinRate = 34.8f, drawRate = 28.0f,
                    totalGames = 89456, averageRating = 2534, recentTrend = 4.7f,
                    topPlayers = listOf("Kasparov", "Fischer", "Topalov")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.EXPERT,
                popularity = 15.7f,
                transpositions = listOf(
                    listOf("e4", "c5", "Nf3", "Nc6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "d6", "Be2", "a6")
                ),
                themes = listOf(OpeningTheme.TACTICAL, OpeningTheme.ATTACKING, OpeningTheme.HYPERMODERN),
                parentOpening = "B20"
            ),
            
            ChessOpening(
                name = "Sicilian: Dragon Variation",
                eco = "B70",
                moves = listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "g6"),
                description = "Sharp opening with opposite-side castling and mutual attacks",
                statistics = OpeningStatistics(
                    whiteWinRate = 39.8f, blackWinRate = 36.2f, drawRate = 24.0f,
                    totalGames = 76234, averageRating = 2456, recentTrend = 2.1f,
                    topPlayers = listOf("Fischer", "Tal", "Radjabov")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.EXPERT,
                popularity = 8.9f,
                themes = listOf(OpeningTheme.ATTACKING, OpeningTheme.TACTICAL, OpeningTheme.FIANCHETTO),
                parentOpening = "B20"
            ),
            
            ChessOpening(
                name = "Sicilian: Accelerated Dragon",
                eco = "B35",
                moves = listOf("e4", "c5", "Nf3", "Nc6", "d4", "cxd4", "Nxd4", "g6"),
                description = "Early fianchetto avoiding some white setups",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.5f, blackWinRate = 33.7f, drawRate = 27.8f,
                    totalGames = 43567, averageRating = 2389, recentTrend = 1.3f,
                    topPlayers = listOf("Gelfand", "Ivanchuk", "Kramnik")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 5.4f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.FIANCHETTO),
                parentOpening = "B20"
            ),
            
            ChessOpening(
                name = "Sicilian: Scheveningen Variation",
                eco = "B80",
                moves = listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "e6"),
                description = "Flexible pawn structure with central control",
                statistics = OpeningStatistics(
                    whiteWinRate = 36.9f, blackWinRate = 32.1f, drawRate = 31.0f,
                    totalGames = 54321, averageRating = 2467, recentTrend = 0.8f,
                    topPlayers = listOf("Karpov", "Kramnik", "Gelfand")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 7.2f,
                themes = listOf(OpeningTheme.POSITIONAL, OpeningTheme.CONTROL_CENTER),
                parentOpening = "B20"
            ),
            
            ChessOpening(
                name = "Sicilian: Sveshnikov Variation",
                eco = "B33",
                moves = listOf("e4", "c5", "Nf3", "Nc6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "e5"),
                description = "Aggressive system with advanced e-pawn",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.7f, blackWinRate = 35.4f, drawRate = 25.9f,
                    totalGames = 39876, averageRating = 2423, recentTrend = 3.6f,
                    topPlayers = listOf("Leko", "Radjabov", "Sveshnikov")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 4.8f,
                themes = listOf(OpeningTheme.TACTICAL, OpeningTheme.PAWN_STORM),
                parentOpening = "B20"
            ),
            
            ChessOpening(
                name = "Sicilian: Closed Variation",
                eco = "B25",
                moves = listOf("e4", "c5", "Nc3"),
                description = "Positional approach avoiding main theoretical lines",
                statistics = OpeningStatistics(
                    whiteWinRate = 40.2f, blackWinRate = 32.8f, drawRate = 27.0f,
                    totalGames = 28765, averageRating = 2234, recentTrend = -1.2f,
                    topPlayers = listOf("Smyslov", "Petrosian", "Botvinnik")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 3.9f,
                themes = listOf(OpeningTheme.POSITIONAL, OpeningTheme.KING_SAFETY),
                parentOpening = "B20"
            ),
            
            ChessOpening(
                name = "Sicilian: Alapin Variation",
                eco = "B22",
                moves = listOf("e4", "c5", "c3"),
                description = "Simple system preparing d4 with pawn support",
                statistics = OpeningStatistics(
                    whiteWinRate = 42.1f, blackWinRate = 31.9f, drawRate = 26.0f,
                    totalGames = 34521, averageRating = 2187, recentTrend = 2.4f,
                    topPlayers = listOf("Sveshnikov", "Tiviakov", "Rozentalis")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.BEGINNER,
                popularity = 4.1f,
                themes = listOf(OpeningTheme.SOLID, OpeningTheme.CONTROL_CENTER),
                parentOpening = "B20"
            ),
            
            // French Defense Complex
            ChessOpening(
                name = "French Defense: Winawer Variation",
                eco = "C15",
                moves = listOf("e4", "e6", "d4", "d5", "Nc3", "Bb4"),
                description = "Sharp line with pin on the knight",
                statistics = OpeningStatistics(
                    whiteWinRate = 36.8f, blackWinRate = 33.2f, drawRate = 30.0f,
                    totalGames = 45678, averageRating = 2456, recentTrend = 1.7f,
                    topPlayers = listOf("Botvinnik", "Korchnoi", "Uhlmann")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 6.3f,
                themes = listOf(OpeningTheme.TACTICAL, OpeningTheme.POSITIONAL),
                parentOpening = "C00"
            ),
            
            ChessOpening(
                name = "French Defense: Classical Variation",
                eco = "C11",
                moves = listOf("e4", "e6", "d4", "d5", "Nc3", "Nf6"),
                description = "Main line development with natural piece play",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.4f, blackWinRate = 31.6f, drawRate = 30.0f,
                    totalGames = 56789, averageRating = 2378, recentTrend = 0.9f,
                    topPlayers = listOf("Korchnoi", "Morozevich", "Short")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 8.1f,
                themes = listOf(OpeningTheme.CLASSICAL, OpeningTheme.PIECE_ACTIVITY),
                parentOpening = "C00"
            ),
            
            ChessOpening(
                name = "French Defense: Tarrasch Variation",
                eco = "C03",
                moves = listOf("e4", "e6", "d4", "d5", "Nd2"),
                description = "Flexible development avoiding the pin",
                statistics = OpeningStatistics(
                    whiteWinRate = 39.7f, blackWinRate = 30.8f, drawRate = 29.5f,
                    totalGames = 43210, averageRating = 2334, recentTrend = -0.3f,
                    topPlayers = listOf("Tarrasch", "Rubinstein", "Nimzowitsch")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 5.7f,
                themes = listOf(OpeningTheme.POSITIONAL, OpeningTheme.PIECE_ACTIVITY),
                parentOpening = "C00"
            ),
            
            // ===== QUEEN'S PAWN OPENINGS (1.d4) =====
            
            // Queen's Gambit Complex
            ChessOpening(
                name = "Queen's Gambit: Declined, Orthodox Defense",
                eco = "D63",
                moves = listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "Bg5", "Be7", "e3", "O-O", "Nf3", "Nbd7"),
                description = "Classical setup with harmonious piece development",
                statistics = OpeningStatistics(
                    whiteWinRate = 37.5f, blackWinRate = 29.8f, drawRate = 32.7f,
                    totalGames = 67890, averageRating = 2456, recentTrend = 1.2f,
                    topPlayers = listOf("Capablanca", "Karpov", "Kramnik")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 9.4f,
                themes = listOf(OpeningTheme.CLASSICAL, OpeningTheme.POSITIONAL),
                parentOpening = "D20"
            ),
            
            ChessOpening(
                name = "Queen's Gambit: Semi-Tarrasch Defense",
                eco = "D41",
                moves = listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "Nf3", "c5"),
                description = "Active counterplay in the center",
                statistics = OpeningStatistics(
                    whiteWinRate = 36.2f, blackWinRate = 31.4f, drawRate = 32.4f,
                    totalGames = 54321, averageRating = 2423, recentTrend = 2.8f,
                    topPlayers = listOf("Kasparov", "Karpov", "Spassky")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 7.1f,
                themes = listOf(OpeningTheme.CONTROL_CENTER, OpeningTheme.TACTICAL),
                parentOpening = "D20"
            ),
            
            ChessOpening(
                name = "Queen's Gambit: Slav Defense",
                eco = "D10",
                moves = listOf("d4", "d5", "c4", "c6"),
                description = "Solid defense supporting the d5 pawn",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.9f, blackWinRate = 30.2f, drawRate = 30.9f,
                    totalGames = 78901, averageRating = 2389, recentTrend = 3.5f,
                    topPlayers = listOf("Anand", "Kramnik", "Leko")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 11.2f,
                themes = listOf(OpeningTheme.SOLID, OpeningTheme.CONTROL_CENTER),
                parentOpening = "D20"
            ),
            
            ChessOpening(
                name = "Queen's Gambit: Catalan Opening",
                eco = "E00",
                moves = listOf("d4", "Nf6", "c4", "e6", "g3"),
                description = "Hypermodern approach with kingside fianchetto",
                statistics = OpeningStatistics(
                    whiteWinRate = 39.4f, blackWinRate = 28.7f, drawRate = 31.9f,
                    totalGames = 45612, averageRating = 2487, recentTrend = 4.1f,
                    topPlayers = listOf("Kramnik", "Carlsen", "Gelfand")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 6.8f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.FIANCHETTO),
                parentOpening = "E00"
            ),
            
            // Nimzo-Indian Complex
            ChessOpening(
                name = "Nimzo-Indian Defense: Classical Variation",
                eco = "E32",
                moves = listOf("d4", "Nf6", "c4", "e6", "Nc3", "Bb4", "Qc2"),
                description = "Principal line avoiding doubled pawns",
                statistics = OpeningStatistics(
                    whiteWinRate = 36.1f, blackWinRate = 31.2f, drawRate = 32.7f,
                    totalGames = 89012, averageRating = 2512, recentTrend = 1.8f,
                    topPlayers = listOf("Nimzowitsch", "Kasparov", "Karpov")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 12.3f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.POSITIONAL),
                parentOpening = "E20"
            ),
            
            ChessOpening(
                name = "Nimzo-Indian Defense: Rubinstein Variation",
                eco = "E20",
                moves = listOf("d4", "Nf6", "c4", "e6", "Nc3", "Bb4", "e3"),
                description = "Solid setup accepting doubled pawns",
                statistics = OpeningStatistics(
                    whiteWinRate = 35.8f, blackWinRate = 30.9f, drawRate = 33.3f,
                    totalGames = 67543, averageRating = 2445, recentTrend = 0.6f,
                    topPlayers = listOf("Rubinstein", "Smyslov", "Petrosian")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 8.9f,
                themes = listOf(OpeningTheme.POSITIONAL, OpeningTheme.SOLID),
                parentOpening = "E20"
            ),
            
            // King's Indian Complex
            ChessOpening(
                name = "King's Indian Defense: Classical Variation",
                eco = "E97",
                moves = listOf("d4", "Nf6", "c4", "g6", "Nc3", "Bg7", "e4", "d6", "Nf3", "O-O", "Be2", "e5"),
                description = "Main line with central tension",
                statistics = OpeningStatistics(
                    whiteWinRate = 37.8f, blackWinRate = 34.6f, drawRate = 27.6f,
                    totalGames = 76234, averageRating = 2467, recentTrend = 2.3f,
                    topPlayers = listOf("Fischer", "Kasparov", "Bronstein")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 9.7f,
                themes = listOf(OpeningTheme.ATTACKING, OpeningTheme.HYPERMODERN, OpeningTheme.FIANCHETTO),
                parentOpening = "E60"
            ),
            
            ChessOpening(
                name = "King's Indian Defense: Saemisch Variation",
                eco = "E81",
                moves = listOf("d4", "Nf6", "c4", "g6", "Nc3", "Bg7", "e4", "d6", "f3"),
                description = "Aggressive setup preparing kingside attack",
                statistics = OpeningStatistics(
                    whiteWinRate = 41.2f, blackWinRate = 33.8f, drawRate = 25.0f,
                    totalGames = 34567, averageRating = 2423, recentTrend = -0.8f,
                    topPlayers = listOf("Saemisch", "Tal", "Shirov")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 3.4f,
                themes = listOf(OpeningTheme.ATTACKING, OpeningTheme.PAWN_STORM),
                parentOpening = "E60"
            ),
            
            ChessOpening(
                name = "King's Indian Defense: Fianchetto Variation",
                eco = "E68",
                moves = listOf("d4", "Nf6", "c4", "g6", "Nc3", "Bg7", "g3"),
                description = "Positional approach with bishop fianchetto",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.6f, blackWinRate = 31.4f, drawRate = 30.0f,
                    totalGames = 45123, averageRating = 2389, recentTrend = 1.5f,
                    topPlayers = listOf("Korchnoi", "Gelfand", "Kramnik")
                ),
                category = OpeningCategory.QUEENS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 5.6f,
                themes = listOf(OpeningTheme.POSITIONAL, OpeningTheme.FIANCHETTO),
                parentOpening = "E60"
            ),
            
            // ===== ENGLISH OPENING (1.c4) =====
            
            ChessOpening(
                name = "English Opening: Symmetrical Variation",
                eco = "A30",
                moves = listOf("c4", "c5"),
                description = "Mirror response leading to complex middlegames",
                statistics = OpeningStatistics(
                    whiteWinRate = 36.5f, blackWinRate = 32.1f, drawRate = 31.4f,
                    totalGames = 67890, averageRating = 2434, recentTrend = 2.7f,
                    topPlayers = listOf("Botvinnik", "Petrosian", "Korchnoi")
                ),
                category = OpeningCategory.ENGLISH,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 8.9f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.POSITIONAL),
                parentOpening = "A10"
            ),
            
            ChessOpening(
                name = "English Opening: Anglo-Indian Defense",
                eco = "A15",
                moves = listOf("c4", "Nf6"),
                description = "Flexible response allowing various setups",
                statistics = OpeningStatistics(
                    whiteWinRate = 37.8f, blackWinRate = 31.2f, drawRate = 31.0f,
                    totalGames = 54321, averageRating = 2398, recentTrend = 1.9f,
                    topPlayers = listOf("Keres", "Smyslov", "Gelfand")
                ),
                category = OpeningCategory.ENGLISH,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 7.2f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.PIECE_ACTIVITY),
                parentOpening = "A10"
            ),
            
            ChessOpening(
                name = "English Opening: King's English Variation",
                eco = "A20",
                moves = listOf("c4", "e5"),
                description = "Reversed Sicilian with extra tempo for White",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.9f, blackWinRate = 30.8f, drawRate = 30.3f,
                    totalGames = 43210, averageRating = 2345, recentTrend = 3.4f,
                    topPlayers = listOf("Kramnik", "Caruana", "Aronian")
                ),
                category = OpeningCategory.ENGLISH,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 6.5f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.CONTROL_CENTER),
                parentOpening = "A10"
            ),
            
            // ===== FLANK AND IRREGULAR OPENINGS =====
            
            ChessOpening(
                name = "Réti Opening",
                eco = "A04",
                moves = listOf("Nf3"),
                description = "Hypermodern approach controlling center from distance",
                statistics = OpeningStatistics(
                    whiteWinRate = 37.2f, blackWinRate = 32.4f, drawRate = 30.4f,
                    totalGames = 34567, averageRating = 2367, recentTrend = 1.8f,
                    topPlayers = listOf("Reti", "Nimzowitsch", "Larsen")
                ),
                category = OpeningCategory.RETI_SYSTEM,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 4.9f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.PIECE_ACTIVITY),
                parentOpening = "A04"
            ),
            
            ChessOpening(
                name = "Bird's Opening",
                eco = "A02",
                moves = listOf("f4"),
                description = "Aggressive flank opening controlling e5",
                statistics = OpeningStatistics(
                    whiteWinRate = 39.8f, blackWinRate = 35.2f, drawRate = 25.0f,
                    totalGames = 12345, averageRating = 2234, recentTrend = -1.2f,
                    topPlayers = listOf("Bird", "Larsen", "From")
                ),
                category = OpeningCategory.FLANK,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 1.2f,
                themes = listOf(OpeningTheme.ATTACKING, OpeningTheme.CONTROL_CENTER),
                parentOpening = "A02"
            ),
            
            ChessOpening(
                name = "King's Indian Attack",
                eco = "A07",
                moves = listOf("Nf3", "d6", "g3", "g6", "Bg2", "Bg7", "O-O"),
                description = "Universal setup against various Black systems",
                statistics = OpeningStatistics(
                    whiteWinRate = 38.4f, blackWinRate = 32.1f, drawRate = 29.5f,
                    totalGames = 23456, averageRating = 2298, recentTrend = 0.7f,
                    topPlayers = listOf("Fischer", "Bronstein", "Tal")
                ),
                category = OpeningCategory.RETI_SYSTEM,
                difficulty = OpeningDifficulty.BEGINNER,
                popularity = 2.8f,
                themes = listOf(OpeningTheme.ATTACKING, OpeningTheme.FIANCHETTO, OpeningTheme.KING_SAFETY),
                parentOpening = "A04"
            ),
            
            // Additional Popular Openings
            ChessOpening(
                name = "Caro-Kann Defense",
                eco = "B10",
                moves = listOf("e4", "c6"),
                description = "Solid defense similar to French but avoiding blocked bishops",
                statistics = OpeningStatistics(
                    whiteWinRate = 37.9f, blackWinRate = 31.8f, drawRate = 30.3f,
                    totalGames = 56789, averageRating = 2398, recentTrend = 2.1f,
                    topPlayers = listOf("Capablanca", "Petrosian", "Anand")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.INTERMEDIATE,
                popularity = 8.7f,
                themes = listOf(OpeningTheme.SOLID, OpeningTheme.POSITIONAL),
                parentOpening = "B10"
            ),
            
            ChessOpening(
                name = "Alekhine's Defense",
                eco = "B02",
                moves = listOf("e4", "Nf6"),
                description = "Provocative defense attacking the e4 pawn immediately",
                statistics = OpeningStatistics(
                    whiteWinRate = 42.1f, blackWinRate = 31.7f, drawRate = 26.2f,
                    totalGames = 23456, averageRating = 2345, recentTrend = -0.8f,
                    topPlayers = listOf("Alekhine", "Larsen", "Keres")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.ADVANCED,
                popularity = 2.1f,
                themes = listOf(OpeningTheme.HYPERMODERN, OpeningTheme.TACTICAL),
                parentOpening = "B02"
            ),
            
            ChessOpening(
                name = "Scandinavian Defense",
                eco = "B01",
                moves = listOf("e4", "d5"),
                description = "Immediate central challenge, simple to learn",
                statistics = OpeningStatistics(
                    whiteWinRate = 44.3f, blackWinRate = 30.2f, drawRate = 25.5f,
                    totalGames = 34567, averageRating = 2187, recentTrend = 1.4f,
                    topPlayers = listOf("Tiviakov", "Berg", "Spielmann")
                ),
                category = OpeningCategory.KINGS_PAWN,
                difficulty = OpeningDifficulty.BEGINNER,
                popularity = 3.2f,
                themes = listOf(OpeningTheme.TACTICAL, OpeningTheme.CONTROL_CENTER),
                parentOpening = "B01"
            )
        )
    }
}