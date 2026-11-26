import argparse
import json
import os
import time
from collections import defaultdict

import numpy as np
import onnxruntime as ort
import torch
from onnxruntime.quantization import QuantType, quantize_dynamic
from torch.onnx import symbolic_helper

from maia3.dataset import get_legal_moves_mask
from maia3.uci import Maia3UCIEngine, parse_args


def rms_norm_symbolic(g, x, normalized_shape, weight, eps):
    eps_value = symbolic_helper._maybe_get_const(eps, "f")
    if symbolic_helper._is_none(eps) or not isinstance(eps_value, float):
        eps_value = float(torch.finfo(torch.float32).eps)
    axes = g.op("Constant", value_t=torch.tensor([-1], dtype=torch.long))
    mean_square = g.op("ReduceMean", g.op("Mul", x, x), axes, keepdims_i=1)
    denominator = g.op("Sqrt", g.op("Add", mean_square, g.op("Constant", value_t=torch.tensor(eps_value))))
    normalized = g.op("Div", x, denominator)
    return normalized if symbolic_helper._is_none(weight) else g.op("Mul", normalized, weight)


def best_move(engine, logits, mask):
    masked = np.where(mask, logits, -np.inf)
    move = engine._move_from_index(int(np.argmax(masked)))
    return move.uci() if move else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--model", default="maia3-5m")
    args = parser.parse_args()

    engine = Maia3UCIEngine(parse_args(["--model", args.model, "--use-uci-history"]))
    engine.ensure_model_loaded()
    model = engine.model
    torch.set_num_threads(1)

    engine.cmd_position("position startpos")
    tokens = engine._tokens_from_history(engine.history).unsqueeze(0)
    elos = torch.tensor([1500], dtype=torch.long)

    torch.onnx.register_custom_op_symbolic("aten::rms_norm", rms_norm_symbolic, 18)
    fp32_path = os.path.join(args.out_dir, f"{args.model}.onnx")
    int8_path = os.path.join(args.out_dir, f"{args.model}-int8.onnx")
    torch.onnx.export(model, (tokens, elos, elos), fp32_path,
                      input_names=["tokens", "self_elo", "oppo_elo"],
                      output_names=["move_logits", "value_logits", "ponder"],
                      opset_version=18, dynamo=False)
    quantize_dynamic(fp32_path, int8_path, weight_type=QuantType.QInt8)

    options = ort.SessionOptions()
    options.intra_op_num_threads = 1
    sessions = {
        "onnx-fp32": ort.InferenceSession(fp32_path, options, providers=["CPUExecutionProvider"]),
        "onnx-int8": ort.InferenceSession(int8_path, options, providers=["CPUExecutionProvider"]),
    }

    positions = [json.loads(line) for line in open(args.data, encoding="utf-8") if line.strip()]
    hits = defaultdict(lambda: defaultdict(int))
    timings = defaultdict(list)
    disagreements = defaultdict(int)
    totals = defaultdict(int)
    for position in positions:
        moves = " ".join(position["moves"])
        engine.cmd_position(f"position startpos moves {moves}" if moves else "position startpos")
        tokens = engine._tokens_from_history(engine.history).unsqueeze(0)
        self_elo = torch.tensor([position["rating"]], dtype=torch.long)
        oppo_elo = torch.tensor([position["opponent"]], dtype=torch.long)
        mask = get_legal_moves_mask(engine.board, engine.all_moves_dict).numpy()

        started = time.perf_counter()
        with torch.no_grad():
            reference = model(tokens, self_elo, oppo_elo)[0][0].numpy()
        timings["torch"].append(time.perf_counter() - started)
        reference_move = best_move(engine, reference, mask)

        band = position["band"]
        totals[band] += 1
        hits["torch"][band] += reference_move == position["human"]
        feeds = {"tokens": tokens.numpy(), "self_elo": self_elo.numpy(), "oppo_elo": oppo_elo.numpy()}
        for name, session in sessions.items():
            started = time.perf_counter()
            logits = session.run(["move_logits"], feeds)[0][0]
            timings[name].append(time.perf_counter() - started)
            move = best_move(engine, logits, mask)
            hits[name][band] += move == position["human"]
            disagreements[name] += move != reference_move

    with open(os.path.join(args.out_dir, "bench_inputs.bin"), "wb") as bench:
        sample = positions[::len(positions) // 20][:20]
        bench.write(np.array([len(sample), tokens.shape[1], tokens.shape[2], len(engine.all_moves_dict)],
                             dtype=">i4").tobytes())
        for position in sample:
            moves = " ".join(position["moves"])
            engine.cmd_position(f"position startpos moves {moves}" if moves else "position startpos")
            sample_tokens = engine._tokens_from_history(engine.history).unsqueeze(0)
            elo_pair = np.array([position["rating"], position["opponent"]], dtype=">i8")
            mask = get_legal_moves_mask(engine.board, engine.all_moves_dict).numpy()
            with torch.no_grad():
                logits = model(sample_tokens, torch.tensor(elo_pair[:1].astype(np.int64)),
                               torch.tensor(elo_pair[1:].astype(np.int64)))[0][0].numpy()
            expected = int(np.argmax(np.where(mask, logits, -np.inf)))
            bench.write(sample_tokens.numpy().astype(">f4").tobytes())
            bench.write(elo_pair.tobytes())
            bench.write(np.array([expected], dtype=">i4").tobytes())
            bench.write(np.packbits(mask.astype(np.uint8)).tobytes())

    for name in ("torch", "onnx-fp32", "onnx-int8"):
        per_band = "  ".join(f"{band}: {100 * hits[name][band] / totals[band]:.1f}%" for band in sorted(totals))
        median = 1000 * sorted(timings[name])[len(timings[name]) // 2]
        extra = f"  differs from torch on {disagreements[name]}/{len(positions)}" if name != "torch" else ""
        print(f"{name:10s} {per_band}  median {median:.1f} ms{extra}")
    for path in (fp32_path, int8_path):
        print(f"{os.path.basename(path)}: {os.path.getsize(path) / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
