import json
import sys
from collections import defaultdict


def main(path):
    groups = defaultdict(list)
    for line in open(path, encoding="utf-8"):
        row = json.loads(line)
        if row["phase"] != "middlegame" or row["time_left"] != "50-75%" or row["recapture"]:
            continue
        budget = (row["base"] + 40 * row["inc"]) * 1000 / 40
        groups[(row["base"], row["inc"])].append((row["think_ms"], row["think_ms"] / budget))
    print("control  n       median_s  median/budget  p90/budget")
    for (base, inc), values in sorted(groups.items(), key=lambda item: -len(item[1]))[:8]:
        raw = sorted(v[0] for v in values)
        ratio = sorted(v[1] for v in values)
        n = len(values)
        print(f"{base // 60}+{inc:<5d} {n:7d} {raw[n // 2] / 1000:8.1f}  {ratio[n // 2]:13.2f}  {ratio[int(n * 0.9)]:10.2f}")


if __name__ == "__main__":
    main(sys.argv[1])
