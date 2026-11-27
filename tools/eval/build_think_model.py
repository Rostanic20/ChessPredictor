import argparse
import json
from bisect import bisect_right
import math
import random
from collections import defaultdict

PHASES = ["opening", "middlegame", "endgame"]
TIME_LEFT = [">=75%", "50-75%", "25-50%", "10-25%", "<10%"]
KINDS = ["normal", "forced", "recapture"]
QUANTILES = [i / 40 for i in range(1, 40)]
REFERENCE_BUDGET_S = 4.5


def kind(row):
    if row["recapture"]:
        return "recapture"
    if row["in_check"] or row["legal"] <= 5:
        return "forced"
    return "normal"


def budget_s(row):
    return (row["base"] + 40 * row["inc"]) / 40


def quantile(values, q):
    position = q * (len(values) - 1)
    low = math.floor(position)
    high = min(low + 1, len(values) - 1)
    return values[low] + (values[high] - values[low]) * (position - low)


def fit_alpha(rows):
    by_control = defaultdict(list)
    for row in rows:
        if row["think_ms"] > 0 and row["phase"] == "middlegame" and kind(row) == "normal" and row["time_left"] == "50-75%":
            by_control[(row["base"], row["inc"])].append(math.log(row["think_ms"] / 1000))
    points = []
    for control, logs in by_control.items():
        if len(logs) >= 500:
            logs.sort()
            points.append((math.log((control[0] + 40 * control[1]) / 40), logs[len(logs) // 2], len(logs)))
    weight = sum(p[2] for p in points)
    mean_x = sum(p[0] * p[2] for p in points) / weight
    mean_y = sum(p[1] * p[2] for p in points) / weight
    covariance = sum(p[2] * (p[0] - mean_x) * (p[1] - mean_y) for p in points)
    variance = sum(p[2] * (p[0] - mean_x) ** 2 for p in points)
    return covariance / variance


def actual_seconds(measured_s, rng):
    if measured_s == 0:
        return max(0.05, 1 - math.sqrt(rng.random()))
    return max(0.05, measured_s + rng.uniform(-0.5, 0.5) + rng.uniform(-0.5, 0.5))


def as_clock_reads(seconds, rng):
    return math.ceil(seconds - rng.random())


def normalized_log(row, alpha, rng):
    seconds = actual_seconds(round(row["think_ms"] / 1000), rng)
    return math.log(seconds) - alpha * math.log(budget_s(row) / REFERENCE_BUDGET_S)


def build(rows, alpha, rng):
    cells = defaultdict(list)
    for row in rows:
        cells[(row["phase"], row["time_left"], kind(row))].append(normalized_log(row, alpha, rng))
    table = {}
    for key, logs in cells.items():
        if len(logs) < 200:
            continue
        logs.sort()
        table[key] = {"quantiles": [quantile(logs, q) for q in QUANTILES], "n": len(logs)}
    return table


def sample(table, row, alpha, rng):
    cell = table.get((row["phase"], row["time_left"], kind(row)))
    if cell is None:
        return None
    q = cell["quantiles"]
    u = rng.random() * (len(q) + 1) - 0.5
    index = min(max(u, 0), len(q) - 1)
    low = int(index)
    high = min(low + 1, len(q) - 1)
    log_seconds = q[low] + (q[high] - q[low]) * (index - low)
    return math.exp(log_seconds + alpha * math.log(budget_s(row) / REFERENCE_BUDGET_S))


def ks_distance(a, b):
    a, b = sorted(a), sorted(b)
    return max(abs(bisect_right(a, v) / len(a) - bisect_right(b, v) / len(b)) for v in set(a) | set(b))


def report(name, test, simulated, rng):
    human = [round(r["think_ms"] / 1000) for r in test]
    model = [as_clock_reads(s, rng) for s in simulated if s is not None]
    human_sorted, model_sorted = sorted(human), sorted(model)
    pick = lambda values, q: values[min(len(values) - 1, int(q * len(values)))]
    zero_h = sum(1 for v in human if v == 0) / len(human)
    zero_m = sum(1 for v in model if v == 0) / len(model)
    print(f"{name:16s} n={len(human):6d}  median {pick(human_sorted, .5):3d}/{pick(model_sorted, .5):3d}s  "
          f"p90 {pick(human_sorted, .9):3d}/{pick(model_sorted, .9):3d}s  "
          f"p99 {pick(human_sorted, .99):3d}/{pick(model_sorted, .99):3d}s  0s {100 * zero_h:4.1f}/{100 * zero_m:4.1f}%  "
          f"KS {ks_distance(human, model):.3f}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--moves", required=True)
    parser.add_argument("--kotlin-out", required=True)
    args = parser.parse_args()

    rows = [json.loads(line) for line in open(args.moves, encoding="utf-8")]
    split = int(len(rows) * 0.8)
    train, test = rows[:split], rows[split:]
    rng = random.Random(11)

    alpha = fit_alpha(train)
    table = build(train, alpha, rng)
    print(f"alpha={alpha:.3f} cells={len(table)} train={len(train)} test={len(test)}")
    print("human/model on held-out moves")

    simulated = [sample(table, row, alpha, rng) for row in test]
    pairs = list(zip(test, simulated))
    views = [
        ("all", lambda r: True),
        ("3+0", lambda r: r["base"] == 180 and r["inc"] == 0),
        ("10+0", lambda r: r["base"] == 600 and r["inc"] == 0),
        ("3+2", lambda r: r["base"] == 180 and r["inc"] == 2),
        ("recapture", lambda r: kind(r) == "recapture"),
        ("forced", lambda r: kind(r) == "forced"),
        ("time left <10%", lambda r: r["time_left"] == "<10%"),
        ("opening", lambda r: r["phase"] == "opening"),
        ("endgame", lambda r: r["phase"] == "endgame"),
    ]
    for name, keep in views:
        chosen = [(r, s) for r, s in pairs if keep(r)]
        report(name, [r for r, _ in chosen], [s for _, s in chosen], rng)

    with open(args.kotlin_out, "w", encoding="utf-8", newline="\n") as out:
        out.write("package com.chesspredictor.domain.humanlike\n\n")
        out.write("internal object ThinkTimeTable {\n")
        out.write(f"    const val ALPHA = {alpha:.4f}\n")
        out.write(f"    const val REFERENCE_BUDGET_SECONDS = {REFERENCE_BUDGET_S}\n\n")
        out.write("    val logSecondQuantiles: Map<ThinkCell, DoubleArray> = mapOf(\n")
        entries = []
        for phase in PHASES:
            for left in TIME_LEFT:
                for move_kind in KINDS:
                    cell = table.get((phase, left, move_kind))
                    if cell is None:
                        continue
                    quantiles = ", ".join(f"{q:.3f}" for q in cell["quantiles"])
                    entries.append(
                        f"        ThinkCell(GamePhase.{phase.upper()}, TimeLeft.{TIME_LEFT_NAMES[left]}, MoveKind.{move_kind.upper()}) to\n"
                        f"            doubleArrayOf({quantiles})"
                    )
        out.write(",\n".join(entries))
        out.write("\n    )\n}\n")


TIME_LEFT_NAMES = {">=75%": "PLENTY", "50-75%": "MOST", "25-50%": "HALF", "10-25%": "LOW", "<10%": "CRITICAL"}

if __name__ == "__main__":
    main()
