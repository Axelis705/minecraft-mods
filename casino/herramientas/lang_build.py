#!/usr/bin/env python3
"""Arma los idiomas extra desde casino/herramientas/lang/<código>.txt (líneas "clave = texto", \\n = salto de línea).
Lo que falte en un idioma se toma del inglés, para que nunca salga una clave sin traducir.

    python3 casino/herramientas/lang_build.py
"""
import glob
import json
import os

H = os.path.dirname(os.path.abspath(__file__))
L = os.path.join(H, "..", "src", "main", "resources", "assets", "casino", "lang")


def main():
    with open(os.path.join(L, "en_us.json"), encoding="utf-8") as fh:
        en = json.load(fh)
    for f in sorted(glob.glob(os.path.join(H, "lang", "*.txt"))):
        code = os.path.basename(f)[:-4]
        tr = {}
        with open(f, encoding="utf-8") as fh:
            for line in fh:
                line = line.rstrip("\n")
                if not line.strip() or line.startswith("#") or " = " not in line:
                    continue
                k, v = line.split(" = ", 1)
                tr[k.strip()] = v.replace("\\n", "\n")
        out = {k: tr.get(k, v) for k, v in en.items()}
        missing = [k for k in en if k not in tr]
        extra = [k for k in tr if k not in en]
        with open(os.path.join(L, code + ".json"), "w", encoding="utf-8") as fh:
            json.dump(out, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        print(f"{code}: {len(tr)} traducidas, {len(missing)} en inglés" + (f", {len(extra)} sobran: {extra[:3]}" if extra else ""))


if __name__ == "__main__":
    main()
