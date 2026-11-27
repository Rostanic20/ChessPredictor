import argparse
import json
import math
import re
import statistics
from collections import defaultdict

import chess
import chess.pgn

CLOCK = re.compile(r"\[%clk (\d+):(\d+):(\d+(?:\.\d+)?)\]")


def clock_ms(comment):
    match = CLOCK.search(comment)
    if not match:
        return None
    hours, minutes, seconds = match.groups()
    return int((int(hours) * 3600 + int(minutes) * 60 + float(seconds)) * 1000)


def phase(ply, board):
    pieces = len(board.piece_map())
    if ply < 20:
        return "opening"
    return "endgame" if pieces <= 12 else "middlegame"


def time_bucket(fraction):
    for limit, name in ((0.1, "<10%"), (0.25, "10-25%"), (0.5, "25-50%"), (0.75, "50-75%")):
        if fraction < limit:
            return name
    return ">=75%"


def moves(games_path):
    with open(games_path, encoding="utf-8") as pgn:
        while True:
            game = chess.pgn.read_game(pgn)
            if game is None:
                return
            parts = game.headers.get("TimeControl", "-").split("+")
            if len(parts) != 2:
                continue
            base, increment = int(parts[0]), int(parts[1])
            ratings = {chess.WHITE: int(game.headers["WhiteElo"]), chess.BLACK: int(game.headers["BlackElo"])}
            last_clock = {chess.WHITE: base * 1000, chess.BLACK: base * 1000}
            board = game.board()
            previous_capture_square = None
            for ply, node in enumerate(game.mainline()):
                mover = board.turn
                after = clock_ms(node.comment)
                if after is None:
                    break
                before = last_clock[mover]
                move = node.move
                is_capture = board.is_capture(move)
                if ply >= 2:
                    yield {
                        "think_ms": max(0, before + increment * 1000 - after),
                        "before_ms": before,
                        "base": base,
                        "inc": increment,
                        "ply": ply,
                        "rating": ratings[mover],
                        "phase": phase(ply, board),
                        "legal": board.legal_moves.count(),
                        "in_check": board.is_check(),
                        "capture": is_capture,
                        "recapture": is_capture and move.to_square == previous_capture_square,
                        "gives_check": board.gives_check(move),
                        "time_left": time_bucket(before / (base * 1000)),
                    }
                previous_capture_square = move.to_square if is_capture else None
                last_clock[mover] = after
                board.push(move)


def describe(label, values):
    values = sorted(values)
    count = len(values)
    if count < 30:
        return f"{label:28s} n={count:6d} (too few)"
    quantile = lambda q: values[min(count - 1, int(q * count))] / 1000
    instant = sum(1 for v in values if v == 0) / count
    return (f"{label:28s} n={count:6d} median={quantile(0.5):5.1f}s p25={quantile(0.25):5.1f}s "
            f"p75={quantile(0.75):5.1f}s p95={quantile(0.95):5.1f}s instant={100 * instant:4.1f}%")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--games", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    rows = list(moves(args.games))
    with open(args.out, "w", encoding="utf-8") as out:
        for row in rows:
            out.write(json.dumps(row) + "\n")

    blitz = [r for r in rows if r["base"] in (180, 300) and r["inc"] == 0]
    print(f"moves: {len(rows)}, of which 3+0/5+0: {len(blitz)}")
    groups = [
        ("phase", lambda r: r["phase"]),
        ("time left", lambda r: r["time_left"]),
        ("recapture", lambda r: r["recapture"]),
        ("in check", lambda r: r["in_check"]),
        ("legal moves", lambda r: "1" if r["legal"] == 1 else "2-5" if r["legal"] <= 5 else "6-25" if r["legal"] <= 25 else "26+"),
        ("rating", lambda r: "<1300" if r["rating"] < 1300 else "1300-1800" if r["rating"] < 1800 else "1800+"),
    ]
    for name, key in groups:
        print(f"\n== 3+0/5+0 by {name}")
        buckets = defaultdict(list)
        for row in blitz:
            buckets[key(row)].append(row["think_ms"])
        for bucket in sorted(buckets, key=str):
            print(describe(str(bucket), buckets[bucket]))

    positive = [math.log(r["think_ms"] / 1000) for r in blitz if r["think_ms"] > 0]
    print(f"\nlog-seconds of non-instant 3+0/5+0 moves: mean={statistics.mean(positive):.2f} sd={statistics.stdev(positive):.2f}")


if __name__ == "__main__":
    main()
