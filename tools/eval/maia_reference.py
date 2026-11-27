import argparse
import json

import chess
import numpy as np
import torch

from maia3.dataset import get_legal_moves_mask
from maia3.uci import Maia3UCIEngine, parse_args


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--with-tokens", type=int, default=30)
    args = parser.parse_args()

    engine = Maia3UCIEngine(parse_args(["--model", "maia3-5m", "--use-uci-history"]))
    engine.ensure_model_loaded()
    positions = [json.loads(line) for line in open(args.data, encoding="utf-8") if line.strip()]

    with open(args.out, "w", encoding="utf-8") as out:
        for index, position in enumerate(positions):
            board = chess.Board()
            fens = [board.fen()]
            for uci in position["moves"]:
                board.push_uci(uci)
                fens.append(board.fen())
            moves = " ".join(position["moves"])
            engine.cmd_position(f"position startpos moves {moves}" if moves else "position startpos")
            tokens = engine._tokens_from_history(engine.history).unsqueeze(0)
            mask = get_legal_moves_mask(engine.board, engine.all_moves_dict).numpy()
            with torch.no_grad():
                logits = engine.model(tokens, torch.tensor([position["rating"]]), torch.tensor([position["opponent"]]))[0][0].numpy()
            best = engine._move_from_index(int(np.argmax(np.where(mask, logits, -np.inf)))).uci()
            row = {
                "fens": fens[-8:],
                "self_elo": position["rating"],
                "oppo_elo": position["opponent"],
                "best": best,
                "human": position["human"],
                "band": position["band"],
            }
            if index < args.with_tokens:
                row["tokens"] = tokens[0].numpy().reshape(-1).tolist()
            out.write(json.dumps(row) + "\n")


if __name__ == "__main__":
    main()
