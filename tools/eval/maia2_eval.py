import argparse
import json
import time
from collections import defaultdict

from maia2 import inference, model


def game_type(position):
    estimate = position["base"] + 40 * position["inc"]
    return "blitz" if estimate < 480 else "rapid"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--models", required=True)
    args = parser.parse_args()

    positions = [json.loads(line) for line in open(args.data, encoding="utf-8") if line.strip()]
    models = {kind: model.from_pretrained(type=kind, device="cpu", save_root=args.models) for kind in ("blitz", "rapid")}
    prepared = inference.prepare()

    hits = defaultdict(int)
    top5 = defaultdict(int)
    totals = defaultdict(int)
    elapsed = []
    with open(args.out, "w", encoding="utf-8") as out:
        for position in positions:
            started = time.perf_counter()
            probs, _ = inference.inference_each(models[game_type(position)], prepared, position["fen"],
                                                position["rating"], position["opponent"])
            elapsed.append(time.perf_counter() - started)
            ranked = sorted(probs, key=probs.get, reverse=True)
            band = position["band"]
            totals[band] += 1
            hits[band] += ranked[0] == position["human"]
            top5[band] += position["human"] in ranked[:5]
            out.write(json.dumps({
                "game": position["game"], "ply": position["ply"], "band": band, "variant": "maia2",
                "human": position["human"], "engine": ranked[0], "match": ranked[0] == position["human"],
                "candidates": ranked[:5],
            }) + "\n")

    elapsed.sort()
    for band in sorted(totals):
        print(f"band {band}: top1 {hits[band]}/{totals[band]} = {100 * hits[band] / totals[band]:.1f}%  "
              f"top5 {100 * top5[band] / totals[band]:.1f}%")
    print(f"per move: median {1000 * elapsed[len(elapsed) // 2]:.0f} ms")


if __name__ == "__main__":
    main()
