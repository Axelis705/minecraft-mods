"""Etapa 293: los mini-jefes que guardan el Templo y la Fundición (las pistas de Hilda y Teo los nombran).
Uso: python3 lang293.py <carpeta lang>"""
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


n = "npc.%s." % P
CES[n + "hilda.hint.forja"] = ("La gran fundición queda hacia %s, a unos %s bloques. En la nave manda el Capataz: cuando levante el martillo, apártate. "
                               "Y rompe un par de escorias por mí.")
CEN[n + "hilda.hint.forja"] = ("The great foundry lies to the %s, about %s blocks away. The Foreman rules the casting hall: when he raises his hammer, "
                               "step aside. And smash a couple of slags for me.")
CES[n + "teo.hint.bosque"] = ("Hay un árbol gigantesco, el Gran Templo del Bosque, hacia %s, a unos %s bloques. Dentro del tronco hay una biblioteca. "
                              "¡Dentro de un árbol! Arriba, en la copa, un gólem de musgo guarda la Piedra.")
CEN[n + "teo.hint.bosque"] = ("There is a gigantic tree, the Great Forest Temple, to the %s, about %s blocks away. There is a library inside the trunk. "
                              "Inside a tree! Up in the crown, a moss golem guards the Stone.")


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
    print("lang293:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
