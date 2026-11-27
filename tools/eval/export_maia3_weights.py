import argparse
import struct

import numpy as np
import torch

from maia3.uci import Maia3UCIEngine, parse_args

LAYER_KEYS = [
    "self_attn.mha.in_proj_weight",
    "self_attn.mha.out_proj.weight",
    "self_attn.sm2.weight", "self_attn.sm2.bias",
    "self_attn.ln1.weight", "self_attn.ln1.bias",
    "self_attn.sm3.weight", "self_attn.sm3.bias",
    "self_attn.ln2.weight", "self_attn.ln2.bias",
    "linear1.weight", "linear1.bias",
    "linear2.weight", "linear2.bias",
    "norm1.weight", "norm2.weight",
]
HEAD_KEYS = [
    "gab_shared_weight",
    "elo_embedding_low.weight", "elo_embedding_high.weight",
    "token_projection.weight", "token_projection.bias",
    "transformer.norm.weight", "transformer.norm.bias",
    "proj_sq_from.weight", "proj_sq_to.weight", "promo_bias_proj.weight",
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--model", default="maia3-5m")
    args = parser.parse_args()

    engine = Maia3UCIEngine(parse_args(["--model", args.model]))
    engine.ensure_model_loaded()
    state = engine.model.state_dict()
    blocks = len(engine.model.transformer.layers)
    for layer in range(blocks):
        shared = state["gab_shared_weight"]
        assert torch.equal(state[f"transformer.layers.{layer}.self_attn.gab_weight"], shared)

    names = HEAD_KEYS + [f"transformer.layers.{layer}.{key}" for layer in range(blocks) for key in LAYER_KEYS]
    with open(args.out, "wb") as out:
        out.write(b"MAIA3F16")
        out.write(struct.pack("<i", len(names)))
        for name in names:
            tensor = state[name].detach().cpu().numpy().astype(np.float32)
            encoded = name.encode("utf-8")
            out.write(struct.pack("<H", len(encoded)))
            out.write(encoded)
            out.write(struct.pack("<B", tensor.ndim))
            out.write(struct.pack(f"<{tensor.ndim}i", *tensor.shape))
            out.write(tensor.astype("<f2").tobytes())
    print(f"{len(names)} tensors, {sum(state[n].numel() for n in names)} parameters, {blocks} blocks")


if __name__ == "__main__":
    main()
