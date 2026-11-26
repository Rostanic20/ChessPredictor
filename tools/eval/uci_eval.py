import argparse
import json
import subprocess
import sys
import time
from collections import defaultdict


class Engine:
    def __init__(self, command):
        self.process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                        stderr=subprocess.DEVNULL, text=True, bufsize=1)

    def send(self, line):
        self.process.stdin.write(line + "\n")
        self.process.stdin.flush()

    def until(self, prefix):
        lines = []
        while True:
            line = self.process.stdout.readline()
            if not line:
                raise RuntimeError("engine exited")
            line = line.strip()
            lines.append(line)
            if line.startswith(prefix):
                return lines

    def close(self):
        self.send("quit")
        self.process.wait(timeout=10)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine", required=True)
    parser.add_argument("--engine-arg", action="append", default=[])
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--variant", required=True)
    parser.add_argument("--go", default="go")
    parser.add_argument("--limit", type=int, default=0)
    args = parser.parse_args()

    positions = [json.loads(line) for line in open(args.data, encoding="utf-8") if line.strip()]
    if args.limit:
        positions = positions[:args.limit]

    engine = Engine([args.engine] + args.engine_arg)
    engine.send("uci")
    engine.until("uciok")
    engine.send("setoption name Temperature value 0")
    engine.send("setoption name MultiPV value 5")
    engine.send("isready")
    engine.until("readyok")

    hits = defaultdict(int)
    top5 = defaultdict(int)
    totals = defaultdict(int)
    elapsed = []
    with open(args.out, "w", encoding="utf-8") as out:
        for index, position in enumerate(positions):
            engine.send(f"setoption name SelfElo value {position['rating']}")
            engine.send(f"setoption name OppoElo value {position['opponent']}")
            moves = " ".join(position["moves"])
            engine.send(f"position startpos moves {moves}" if moves else "position startpos")
            started = time.perf_counter()
            engine.send(args.go)
            lines = engine.until("bestmove")
            elapsed.append(time.perf_counter() - started)
            best = lines[-1].split()[1]
            candidates = [line.split(" pv ")[1].split()[0] for line in lines if " multipv " in line and " pv " in line]
            band = position["band"]
            totals[band] += 1
            hits[band] += best == position["human"]
            top5[band] += position["human"] in candidates[:5]
            out.write(json.dumps({
                "game": position["game"], "ply": position["ply"], "band": band, "variant": args.variant,
                "human": position["human"], "engine": best, "match": best == position["human"],
                "candidates": candidates[:5],
            }) + "\n")
            if index % 100 == 99:
                print(f"{index + 1}/{len(positions)}", file=sys.stderr)
    engine.close()

    elapsed.sort()
    for band in sorted(totals):
        print(f"band {band}: top1 {hits[band]}/{totals[band]} = {100 * hits[band] / totals[band]:.1f}%  "
              f"top5 {100 * top5[band] / totals[band]:.1f}%")
    print(f"per move: median {1000 * elapsed[len(elapsed) // 2]:.0f} ms, p95 {1000 * elapsed[int(len(elapsed) * 0.95)]:.0f} ms")


if __name__ == "__main__":
    main()
