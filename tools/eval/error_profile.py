import argparse
import json
import math
from collections import defaultdict

import chess
import chess.engine


def win_probability(score):
    if score.is_mate():
        return 1.0 if score.mate() > 0 else 0.0
    return 1.0 / (1.0 + math.exp(-0.00368208 * score.score()))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--positions", required=True)
    parser.add_argument("--results", nargs="+", required=True, help="name=path of harness or uci_eval results")
    parser.add_argument("--engine", nargs="+", required=True, help="UCI engine command")
    parser.add_argument("--nodes", type=int, default=200000)
    parser.add_argument("--cache", required=True)
    parser.add_argument("--limit", type=int)
    args = parser.parse_args()

    positions = {}
    for line in open(args.positions, encoding="utf-8"):
        if args.limit is not None and len(positions) >= args.limit:
            break
        if line.strip():
            row = json.loads(line)
            positions[(row["game"], row["ply"])] = row

    players = {"human": {key: row["human"] for key, row in positions.items()}}
    for spec in args.results:
        name, path = spec.split("=", 1)
        moves = {}
        for line in open(path, encoding="utf-8"):
            if line.strip():
                row = json.loads(line)
                moves[(row["game"], row["ply"])] = row["engine"]
        players[name] = moves

    cache = {}
    try:
        for line in open(args.cache, encoding="utf-8"):
            row = json.loads(line)
            cache[(row["fen"], row["move"])] = row["loss"]
    except FileNotFoundError:
        pass

    engine = chess.engine.SimpleEngine.popen_uci(args.engine)
    limit = chess.engine.Limit(nodes=args.nodes)
    best_by_fen = {}
    with open(args.cache, "a", encoding="utf-8") as cache_out:
        def loss(fen, uci):
            if (fen, uci) in cache:
                return cache[(fen, uci)]
            board = chess.Board(fen)
            if fen not in best_by_fen:
                best_by_fen[fen] = win_probability(engine.analyse(board, limit, game=object())["score"].pov(board.turn))
            best = best_by_fen[fen]
            board.push_uci(uci)
            if board.is_checkmate():
                after = 1.0
            elif board.is_game_over():
                after = 0.5
            else:
                after = 1.0 - win_probability(engine.analyse(board, limit, game=object())["score"].pov(board.turn))
            value = max(0.0, best - after)
            cache[(fen, uci)] = value
            cache_out.write(json.dumps({"fen": fen, "move": uci, "loss": value}) + "\n")
            cache_out.flush()
            return value

        table = defaultdict(lambda: defaultdict(list))
        for key, row in positions.items():
            for name, moves in players.items():
                if key in moves:
                    table[name][row["band"]].append(loss(row["fen"], moves[key]))
    engine.quit()

    print(f"{'player':<16}{'band':>6}{'n':>6}{'mean':>12}{'>5%':>12}{'>10%':>12}{'>15%':>12}")
    for name in players:
        for band in sorted(table[name]):
            values = table[name][band]
            n = len(values)
            mean = sum(values) / n
            spread = math.sqrt(sum((v - mean) ** 2 for v in values) / (n - 1) / n) if n > 1 else 0.0

            def share(threshold):
                p = sum(v > threshold for v in values) / n
                return f"{100 * p:.1f}+-{196 * math.sqrt(p * (1 - p) / n):.1f}"

            print(f"{name:<16}{band:>6}{n:>6}{f'{100 * mean:.1f}+-{196 * spread:.1f}':>12}{share(0.05):>12}{share(0.10):>12}{share(0.15):>12}")


if __name__ == "__main__":
    main()
