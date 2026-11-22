"""Etapa 287: textos del volcán (el gran despertar, la reja del Altar Maldito y su pista).
Uso: python3 lang287.py <carpeta lang>"""
import json
import os
import sys
from collections import OrderedDict

P = "mundos_de_axelis"
ES, EN = OrderedDict(), OrderedDict()
CES, CEN = OrderedDict(), OrderedDict()


def k(key, es, en):
    ES[key] = es
    EN[key] = en


m = "message.%s." % P
k(m + "reja_volcan.altar", "La reja no cede. La guarda el Altar Maldito de esta sala: vence sus tres oleadas.",
  "The gate won't budge. The Cursed Altar of this hall guards it: beat its three waves.")
k(m + "altar.reja_abierta", "¡Con el altar vencido, la reja de la sala se abre!", "With the altar beaten, the hall's gate opens!")
k(m + "volcan.altar_pista", "La reja del fondo no se abre sola: despierta el Altar Maldito y vence sus oleadas.",
  "The gate at the back won't open by itself: wake the Cursed Altar and beat its waves.")
k("title.%s.volcan.despierta" % P, "¡El volcán despierta!", "The volcano awakens!")
k("title.%s.volcan.despierta.sub" % P, "Algo late bajo el cráter...", "Something beats beneath the crater...")


def patch(path, add, change):
    lines = open(path, encoding="utf-8").read().rstrip().split("\n")
    assert lines[-1] == "}", path
    for i, ln in enumerate(lines):
        s = ln.strip()
        for key, val in change.items():
            if s.startswith(json.dumps(key) + ":"):
                comma = "," if s.endswith(",") else ""
                lines[i] = "  " + json.dumps(key) + ": " + json.dumps(val, ensure_ascii=False) + comma
    body = lines[:-1]
    existing = "\n".join(body)
    new = [(key, val) for key, val in add.items() if json.dumps(key) + ":" not in existing]
    if new:
        if not body[-1].endswith(","):
            body[-1] += ","
        for j, (key, val) in enumerate(new):
            body.append("  " + json.dumps(key) + ": " + json.dumps(val, ensure_ascii=False) + ("," if j < len(new) - 1 else ""))
    open(path, "w", encoding="utf-8").write("\n".join(body + ["}"]) + "\n")
    json.load(open(path, encoding="utf-8"))


def main():
    d = sys.argv[1]
    patch(os.path.join(d, "es_mx.json"), ES, CES)
    patch(os.path.join(d, "es_es.json"), ES, CES)
    patch(os.path.join(d, "en_us.json"), EN, CEN)
    print("lang287:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
