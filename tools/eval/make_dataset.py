import argparse
import io
import json
import random
import re
import sys
import urllib.request

import chess
import chess.pgn
import zstandard

BANDS = {1200: (1100, 1300), 1600: (1500, 1700), 2000: (1900, 2100)}
CLOCK = re.compile(r"\[%clk (\d+):(\d+):(\d+(?:\.\d+)?)\]")


def band_of(rating):
    for name, (low, high) in BANDS.items():
        if low <= rating < high:
            return name
    return None


def clock_ms(comment):
    match = CLOCK.search(comment)
    if not match:
        return None
    hours, minutes, seconds = match.groups()
    return int((int(hours) * 3600 + int(minutes) * 60 + float(seconds)) * 1000)


def time_control(headers):
    parts = headers.get("TimeControl", "-").split("+")
    if len(parts) != 2 or not parts[0].isdigit() or not parts[1].isdigit():
        return None
    return int(parts[0]), int(parts[1])


def usable(game):
    headers = game.headers
    if headers.get("Variant", "Standard") != "Standard" or "Rated" not in headers.get("Event", ""):
        return False
    if headers.get("Termination") not in ("Normal", "Time forfeit"):
        return False
    control = time_control(headers)
    return control is not None and 180 <= control[0] <= 900


def positions(game, rng, per_side):
    headers = game.headers
    base, increment = time_control(headers)
    ratings = {chess.WHITE: int(headers["WhiteElo"]), chess.BLACK: int(headers["BlackElo"])}
    last_clock = {chess.WHITE: base * 1000, chess.BLACK: base * 1000}
    board = game.board()
    played = []
    candidates = {chess.WHITE: [], chess.BLACK: []}
    for ply, node in enumerate(game.mainline()):
        mover = board.turn
        after = clock_ms(node.comment)
        if after is None:
            return []
        before = last_clock[mover]
        think = max(0, before + increment * 1000 - after) if ply >= 2 else None
        if ply >= 10 and before >= 30_000 and think is not None:
            candidates[mover].append({
                "fen": board.fen(),
                "human": node.move.uci(),
                "rating": ratings[mover],
                "opponent": ratings[not mover],
                "base": base,
                "inc": increment,
                "clock_ms": before,
                "think_ms": think,
                "ply": ply,
                "moves": list(played),
                "game": headers.get("Site", ""),
            })
        last_clock[mover] = after
        board.push(node.move)
        played.append(node.move.uci())
    picked = []
    for side in (chess.WHITE, chess.BLACK):
        if band_of(ratings[side]) is not None:
            picked += rng.sample(candidates[side], min(per_side, len(candidates[side])))
    return picked


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="https://database.lichess.org/standard/lichess_db_standard_rated_2025-10.pgn.zst")
    parser.add_argument("--out", required=True)
    parser.add_argument("--games-out", required=True)
    parser.add_argument("--per-band", type=int, default=400)
    parser.add_argument("--per-side", type=int, default=1)
    parser.add_argument("--max-games", type=int, default=20000)
    parser.add_argument("--seed", type=int, default=7)
    args = parser.parse_args()

    rng = random.Random(args.seed)
    counts = {band: 0 for band in BANDS}
    kept_games = 0
    request = urllib.request.Request(args.url, headers={"User-Agent": "chess-eval-dataset"})
    with urllib.request.urlopen(request) as response, \
            open(args.out, "w", encoding="utf-8") as out, \
            open(args.games_out, "w", encoding="utf-8") as games_out:
        reader = zstandard.ZstdDecompressor(max_window_size=2**31).stream_reader(response)
        text = io.TextIOWrapper(reader, encoding="utf-8", errors="replace")
        while kept_games < args.max_games:
            game = chess.pgn.read_game(text)
            if game is None:
                break
            if not usable(game):
                continue
            rows = positions(game, rng, args.per_side)
            print(game, file=games_out, end="\n\n")
            kept_games += 1
            for row in rows:
                band = band_of(row["rating"])
                if counts[band] >= args.per_band:
                    continue
                row["band"] = band
                out.write(json.dumps(row) + "\n")
                counts[band] += 1
    print(json.dumps({"positions": counts, "games": kept_games}), file=sys.stderr)


if __name__ == "__main__":
    main()
