package com.chesspredictor.domain.humanlike

import com.chesspredictor.domain.usecases.ChessRulesEngine
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class MaiaNetworkTest {

    private class Case(val fens: List<String>, val selfElo: Int, val oppoElo: Int, val best: String)

    private val cases = listOf(
        Case(
            listOf(
                "rnb2rk1/pp3ppp/5n2/4p3/2B1P3/8/PP1N1PPP/R2NK2R b KQ - 0 11", "rn3rk1/pp3ppp/4bn2/4p3/2B1P3/8/PP1N1PPP/R2NK2R w KQ - 1 12",
                "rn3rk1/pp3ppp/4bn2/4p3/2B1P3/8/PP1N1PPP/R2N1RK1 b - - 2 12", "rn3rk1/pp3ppp/5n2/4p3/2b1P3/8/PP1N1PPP/R2N1RK1 w - - 0 13",
                "rn3rk1/pp3ppp/5n2/4p3/2N1P3/8/PP3PPP/R2N1RK1 b - - 0 13", "r4rk1/pp3ppp/2n2n2/4p3/2N1P3/8/PP3PPP/R2N1RK1 w - - 1 14",
                "r4rk1/pp3ppp/2n2n2/4p3/2N1P3/5P2/PP4PP/R2N1RK1 b - - 0 14", "r4rk1/pp3ppp/5n2/4p3/2NnP3/5P2/PP4PP/R2N1RK1 w - - 1 15"
            ),
            1170, 1167, "c4e5"
        ),
        Case(
            listOf(
                "8/pk4pp/8/3P4/P5R1/6P1/3r3P/6K1 b - - 2 30", "8/pk5p/6p1/3P4/P5R1/6P1/3r3P/6K1 w - - 0 31",
                "8/pk5p/6p1/3P4/P6R/6P1/3r3P/6K1 b - - 1 31", "8/pk6/6p1/3P3p/P6R/6P1/3r3P/6K1 w - - 0 32",
                "8/pk6/6p1/3P3p/P4R2/6P1/3r3P/6K1 b - - 1 32", "8/pk6/6p1/3r3p/P4R2/6P1/7P/6K1 w - - 0 33",
                "8/pk6/5Rp1/3r3p/P7/6P1/7P/6K1 b - - 1 33", "8/pk6/5R2/3r2pp/P7/6P1/7P/6K1 w - - 0 34"
            ),
            1639, 1607, "f6h6"
        ),
        Case(
            listOf(
                "8/8/p4k2/1pp2P1P/8/2P2K2/PP5r/8 w - - 1 37", "8/8/p4k2/1pp2P1P/6K1/2P5/PP5r/8 b - - 2 37",
                "8/8/p4k2/1pp2P1P/6K1/2P5/PP4r1/8 w - - 3 38", "8/8/p4k2/1pp2P1P/7K/2P5/PP4r1/8 b - - 4 38",
                "8/8/p4k2/1pp2PrP/7K/2P5/PP6/8 w - - 5 39", "8/8/p4k2/1pp2PrP/7K/P1P5/1P6/8 b - - 0 39",
                "8/8/p4k2/1pp2r1P/7K/P1P5/1P6/8 w - - 0 40", "8/8/p4k2/1pp2r1P/1P5K/P1P5/8/8 b - - 0 40"
            ),
            1200, 1221, "c5b4"
        ),
        Case(
            listOf(
                "r1b1k2r/1p1pnpp1/pp3q1p/8/B7/3P1Q2/PPP2PPP/R3R1K1 w kq - 4 16", "r1b1k2r/1p1pnpp1/pp3q1p/8/B3Q3/3P4/PPP2PPP/R3R1K1 b kq - 5 16",
                "r1b1k2r/1p1pnpp1/pp2q2p/8/B3Q3/3P4/PPP2PPP/R3R1K1 w kq - 6 17", "r1b1k2r/1p1pnpp1/pp2q2p/8/B2Q4/3P4/PPP2PPP/R3R1K1 b kq - 7 17",
                "r1b1k2r/1p1pnpp1/pp3q1p/8/B2Q4/3P4/PPP2PPP/R3R1K1 w kq - 8 18", "r1b1k2r/1p1pnpp1/pp3q1p/8/B3Q3/3P4/PPP2PPP/R3R1K1 b kq - 9 18",
                "r1b1k2r/1p1pnpp1/p4q1p/1p6/B3Q3/3P4/PPP2PPP/R3R1K1 w kq - 0 19", "r1b1k2r/1p1pnpp1/p4q1p/1p6/4Q3/1B1P4/PPP2PPP/R3R1K1 b kq - 1 19"
            ),
            2037, 2058, "d7d5"
        ),
        Case(
            listOf(
                "7k/1p4p1/6b1/2pB3p/2P5/p4N1P/r5P1/6KN w - - 0 38", "7k/1p4p1/6b1/2pBN2p/2P5/p6P/r5P1/6KN b - - 1 38",
                "4b2k/1p4p1/8/2pBN2p/2P5/p6P/r5P1/6KN w - - 2 39", "4b2k/1p4p1/8/2pB3p/2P5/p2N3P/r5P1/6KN b - - 3 39",
                "4b2k/1p4p1/8/2pB3p/2P5/p2N3P/3r2P1/6KN w - - 4 40", "4b2k/1p4p1/8/2NB3p/2P5/p6P/3r2P1/6KN b - - 0 40",
                "4b2k/1p4p1/8/2NB3p/2P5/7P/p2r2P1/6KN w - - 0 41", "4b2k/1p4p1/8/3B3p/2P5/1N5P/p2r2P1/6KN b - - 1 41"
            ),
            1916, 1874, "d2b2"
        ),
        Case(
            listOf(
                "rn3rk1/pppb1ppp/8/2b5/3N4/5P2/PPP2BPP/2KR1B1R b - - 2 12", "rn3rk1/pppb1ppp/1b6/8/3N4/5P2/PPP2BPP/2KR1B1R w - - 3 13",
                "rn3rk1/pppb1ppp/1b6/8/2BN4/5P2/PPP2BPP/2KR3R b - - 4 13", "r4rk1/pppb1ppp/1bn5/8/2BN4/5P2/PPP2BPP/2KR3R w - - 5 14",
                "r4rk1/pppb1ppp/1bn5/8/2BN4/5P2/PPP2BPP/2KR1R2 b - - 6 14", "r4rk1/pppb1ppp/1b6/8/2Bn4/5P2/PPP2BPP/2KR1R2 w - - 0 15",
                "r4rk1/pppb1ppp/1b6/8/2BB4/5P2/PPP3PP/2KR1R2 b - - 0 15", "r4rk1/pppb1ppp/8/8/2Bb4/5P2/PPP3PP/2KR1R2 w - - 0 16"
            ),
            2020, 2036, "d1d4"
        )
    )

    @Test
    fun theBundledWeightsPickTheSameMovesAsThePythonModel() {
        val network = MaiaNetwork(MaiaWeights.parse(File("../androidApp/src/main/assets/maia3-5m.f16.bin").readBytes()))
        val rules = ChessRulesEngine()
        val picked = cases.map { case ->
            val fen = case.fens.last()
            val legal = rules.getLegalMoves(rules.parseGameState(fen)).map { it.toUci() }
            val logits = network.logits(case.fens, case.selfElo, case.oppoElo)
            MaiaPolicy.distribution(logits, legal, MaiaEncoding.blackToMove(fen)).maxBy { it.value }.key
        }
        assertEquals(cases.map { it.best }, picked)
    }
}
