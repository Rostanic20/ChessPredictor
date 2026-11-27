import argparse
import hashlib
import json
import math
import os
from collections import defaultdict
from multiprocessing import Pool

import chess
import chess.engine
import numpy as np
import torch

BLUNDER = 0.15
TEMPERATURES = [1.0, 0.9, 0.8, 0.7, 0.6]
VETO_BELOW = [None, 0.02, 0.05, 0.1, 0.2, 1.0]


def win_probability(score):
    if score.is_mate():
        return 1.0 if score.mate() > 0 else 0.0
    return 1.0 / (1.0 + math.exp(-0.00368208 * score.score()))


_engine = None
_limit = None


def start_engine(command, nodes):
    global _engine, _limit
    _engine = chess.engine.SimpleEngine.popen_uci(command)
    _limit = chess.engine.Limit(nodes=nodes)


def evaluate(job):
    fen, moves = job
    board = chess.Board(fen)
    best = win_probability(_engine.analyse(board, _limit, game=object())["score"].pov(board.turn))
    losses = {}
    for uci in moves:
        after_board = board.copy()
        after_board.push_uci(uci)
        if after_board.is_checkmate():
            after = 1.0
        elif after_board.is_game_over():
            after = 0.5
        else:
            after = 1.0 - win_probability(_engine.analyse(after_board, _limit, game=object())["score"].pov(after_board.turn))
        losses[uci] = max(0.0, best - after)
    return fen, losses


def maia_logits(positions):
    from maia3.dataset import get_legal_moves_mask
    from maia3.uci import Maia3UCIEngine, mirror_move, parse_args

    engine = Maia3UCIEngine(parse_args(["--model", "maia3-5m", "--use-uci-history"]))
    engine.ensure_model_loaded()
    out = []
    for position in positions:
        moves = " ".join(position["moves"])
        engine.cmd_position(f"position startpos moves {moves}" if moves else "position startpos")
        tokens = engine._tokens_from_history(engine.history).unsqueeze(0)
        elo = torch.tensor([position["band"]])
        with torch.no_grad():
            logits = engine.model(tokens, elo, elo)[0][0].numpy()
        board = engine.board
        scores = {}
        for move in board.legal_moves:
            uci = move.uci()
            key = mirror_move(uci) if board.turn == chess.BLACK else uci
            scores[uci] = float(logits[engine.all_moves_dict[key]])
        out.append(scores)
    return out


def softmax(scores, temperature):
    top = max(scores.values())
    weights = {m: math.exp((s - top) / temperature) for m, s in scores.items()}
    total = sum(weights.values())
    return {m: w / total for m, w in weights.items()}


def candidates(scores, human, coverage, cap):
    probabilities = sorted(softmax(scores, 1.0).items(), key=lambda item: -item[1])
    chosen, mass = [], 0.0
    for uci, p in probabilities:
        if mass >= coverage or len(chosen) >= cap:
            break
        chosen.append(uci)
        mass += p
    if human not in chosen:
        chosen.append(human)
    return chosen


def analyse(args):
    positions = [json.loads(line) for line in open(args.positions, encoding="utf-8") if line.strip()]
    print(f"maia logits for {len(positions)} positions")
    all_scores = maia_logits(positions)

    cache = {}
    if os.path.exists(args.cache):
        for line in open(args.cache, encoding="utf-8"):
            row = json.loads(line)
            cache[(row["fen"], row["move"])] = row["loss"]

    rows, jobs = [], defaultdict(list)
    for position, scores in zip(positions, all_scores):
        moves = candidates(scores, position["human"], args.coverage, args.cap)
        rows.append((position, scores, moves))
        for uci in moves:
            if (position["fen"], uci) not in cache:
                jobs[position["fen"]].append(uci)
    pending = sum(len(v) for v in jobs.values())
    print(f"{pending} new evaluations over {len(jobs)} positions, {args.workers} engines")

    with open(args.cache, "a", encoding="utf-8") as cache_out, \
            Pool(args.workers, initializer=start_engine, initargs=(args.engine, args.nodes)) as pool:
        done = 0
        for fen, losses in pool.imap_unordered(evaluate, list(jobs.items())):
            for uci, loss in losses.items():
                cache[(fen, uci)] = loss
                cache_out.write(json.dumps({"fen": fen, "move": uci, "loss": loss}) + "\n")
            cache_out.flush()
            done += 1
            if done % 100 == 0:
                print(f"{done}/{len(jobs)} positions")

    with open(args.out, "w", encoding="utf-8") as out:
        for position, scores, moves in rows:
            out.write(json.dumps({
                "game": position["game"], "ply": position["ply"], "band": position["band"],
                "human": position["human"], "scores": scores,
                "losses": {uci: cache[(position["fen"], uci)] for uci in moves},
            }) + "\n")


def variant_distribution(row, temperature, veto_below):
    probabilities = softmax(row["scores"], temperature)
    losses = row["losses"]
    analysed = {m: probabilities[m] for m in losses}
    if veto_below is not None:
        model = softmax(row["scores"], 1.0)
        kept = {m: p for m, p in analysed.items() if not (losses[m] > BLUNDER and model[m] < veto_below)}
        if not kept:
            safest = min(losses, key=losses.get)
            kept = {safest: 1.0}
        analysed = kept
    tail = 1.0 - sum(probabilities[m] for m in losses)
    total = sum(analysed.values())
    return {m: p / total for m, p in analysed.items()}, tail


def summarise(rows, temperature, veto_below):
    match = loss = blunder = tail = 0.0
    for row in rows:
        distribution, residual = variant_distribution(row, temperature, veto_below)
        match += distribution.get(row["human"], 0.0)
        loss += sum(p * row["losses"][m] for m, p in distribution.items())
        blunder += sum(p for m, p in distribution.items() if row["losses"][m] > BLUNDER)
        tail += residual
    n = len(rows)
    return 100 * match / n, 100 * loss / n, 100 * blunder / n, 100 * tail / n


def human_profile(rows):
    n = len(rows)
    losses = [row["losses"][row["human"]] for row in rows]
    return 100 * sum(losses) / n, 100 * sum(v > BLUNDER for v in losses) / n


def half(row):
    return int(hashlib.sha1(row["game"].encode()).hexdigest(), 16) % 2


def report(args):
    rows = [json.loads(line) for line in open(args.out, encoding="utf-8")]
    bands = sorted({row["band"] for row in rows})
    for split, name in ((0, "tune"), (1, "validate")):
        print(f"\n== {name} half ==")
        for band in bands:
            subset = [row for row in rows if row["band"] == band and half(row) == split]
            human_loss, human_blunder = human_profile(subset)
            print(f"band {band} (n={len(subset)}): human mean loss {human_loss:.2f}, blunders {human_blunder:.1f}%")
            print(f"  {'T':>4} {'veto<':>6} {'match%':>7} {'loss':>6} {'blund%':>7} {'tail%':>6}")
            for temperature in TEMPERATURES:
                for veto in VETO_BELOW:
                    match, loss, blunder, tail = summarise(subset, temperature, veto)
                    veto_label = "-" if veto is None else f"{veto:g}"
                    print(f"  {temperature:>4} {veto_label:>6} {match:>7.1f} {loss:>6.2f} {blunder:>7.1f} {tail:>6.2f}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--positions", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--cache", required=True)
    parser.add_argument("--engine", nargs="+", required=True)
    parser.add_argument("--nodes", type=int, default=200000)
    parser.add_argument("--coverage", type=float, default=0.99)
    parser.add_argument("--cap", type=int, default=12)
    parser.add_argument("--workers", type=int, default=12)
    parser.add_argument("--report-only", action="store_true")
    args = parser.parse_args()
    if not args.report_only:
        analyse(args)
    report(args)


if __name__ == "__main__":
    main()
