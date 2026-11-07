#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
manifiesto_version.py — Manifiesto de versión: commit → digest de cada servicio (RM-06).

Es la unidad de promoción, despliegue y rollback (ADR 20261020, promoción por digest).

  generar  --rama R --commit C --corrida ID --registro ghcr.io/<owner>/<repo> --digests DIR -o M.json
           DIR tiene un archivo <servicio>.digest por servicio, con su digest (sha256:...).
  leer     M.json [--servicio S]
           Imprime "<servicio> <registro>/<servicio>@<digest>" por línea; con --servicio, solo la
           referencia de ese servicio (sale con 1 si no está).

Códigos de salida: 0 éxito · 1 entrada inválida o servicio ausente.
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime, timezone

DIGEST = re.compile(r"^sha256:[0-9a-f]{64}$")


def generar(rama, commit, corrida, registro, digests_dir):
    servicios = {}
    for nombre in sorted(os.listdir(digests_dir)):
        if not nombre.endswith(".digest"):
            continue
        servicio = nombre[: -len(".digest")]
        with open(os.path.join(digests_dir, nombre), encoding="utf-8") as f:
            digest = f.read().strip()
        if not DIGEST.match(digest):
            raise ValueError(f"digest inválido para {servicio}: {digest!r}")
        servicios[servicio] = {"imagen": f"{registro}/{servicio}", "digest": digest}
    if not servicios:
        raise ValueError(f"sin archivos .digest en {digests_dir}")
    return {
        "version": 1,
        "rama": rama,
        "commit": commit,
        "corrida": corrida,
        "fecha": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "servicios": servicios,
    }


def referencias(manifiesto):
    return {s: f"{v['imagen']}@{v['digest']}" for s, v in manifiesto["servicios"].items()}


def main(argv=None):
    parser = argparse.ArgumentParser(description="Manifiesto de versión por digest (RM-06).")
    sub = parser.add_subparsers(dest="cmd", required=True)
    g = sub.add_parser("generar")
    for opt in ("--rama", "--commit", "--corrida", "--registro", "--digests"):
        g.add_argument(opt, required=True)
    g.add_argument("-o", "--salida", required=True)
    leer = sub.add_parser("leer")
    leer.add_argument("manifiesto")
    leer.add_argument("--servicio")
    args = parser.parse_args(argv)

    try:
        if args.cmd == "generar":
            m = generar(args.rama, args.commit, args.corrida, args.registro, args.digests)
            with open(args.salida, "w", encoding="utf-8", newline="\n") as f:
                json.dump(m, f, indent=2, ensure_ascii=False)
                f.write("\n")
            print(f"Manifiesto {args.salida}: {', '.join(m['servicios'])}")
            return 0
        with open(args.manifiesto, encoding="utf-8") as f:
            refs = referencias(json.load(f))
        if args.servicio:
            if args.servicio not in refs:
                print(f"::error::{args.servicio} no está en {args.manifiesto}", file=sys.stderr)
                return 1
            print(refs[args.servicio])
        else:
            for servicio, ref in refs.items():
                print(f"{servicio} {ref}")
        return 0
    except (OSError, ValueError, KeyError) as ex:
        print(f"::error::manifiesto_version: {ex}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    sys.exit(main())
