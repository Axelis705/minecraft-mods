"""Etapa 283: textos de las armas a dos manos y la opción de la cámara al atacar. Uso: python3 lang283.py <carpeta lang>"""
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


k("tooltip.%s.arma.dos_manos" % P, "✋ A dos manos: la otra mano tiene que estar vacía", "✋ Two-handed: your other hand must be empty")
for t, es, en in [("espada", "la espada", "the sword"), ("hacha", "el hacha de guerra", "the war axe"), ("martillo", "el martillo", "the hammer"),
                  ("lanza", "la lanza", "the spear"), ("dagas", "las dagas", "the daggers")]:
    k("tooltip.%s.arma.tipo_corto.%s" % (P, t), es, en)
k("message.%s.arma.dos_manos" % P, "Para usar %s necesitas las dos manos: vacía la otra mano", "You need both hands for %s: empty your other hand")
k("gui.%s.options.attack_camera" % P, "Cámara al atacar", "Attack camera")
k("gui.%s.options.attack_camera.tip" % P, "En primera persona, la cámara acompaña un poco el golpe de las armas nuevas.",
  "In first person, the camera follows the swing of the new weapons a little.")


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
    print("lang283:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
