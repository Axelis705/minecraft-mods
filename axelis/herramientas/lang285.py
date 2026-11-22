"""Etapa 285: textos de la tienda nueva de los habitantes. Uso: python3 lang285.py <carpeta lang>"""
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


g = "gui.%s.npc." % P
k(g + "buy", "Comprar", "Buy")
k(g + "tab.destinos", "Destinos (%s)", "Places (%s)")
k(g + "tab.provisiones", "Provisiones (%s)", "Supplies (%s)")
k(g + "tab.especiales", "Especiales (%s)", "Specials (%s)")
k(g + "tab_vacia", "Aquí no hay nada todavía.", "Nothing here yet.")
k(g + "shop_hello.destinos", "Te digo por dónde seguir. Lo dorado (★) es el camino hasta el próximo habitante.",
  "I'll tell you where to go next. The golden one (★) leads to the next villager.")
k(g + "shop_hello.cosas", "Mira con calma. Doble clic o «Comprar» para llevártelo.", "Take your time. Double click or 'Buy' to take it.")
k(g + "desc.siguiente", "Brújula hasta %s. Allí está la celda de %s: con su rescate se abre el siguiente camino.",
  "Compass to %s. %s is locked in a cell there: rescuing them opens the next road.")
k(g + "desc.brujula", "Brújula hasta %s, para volver cuando quieras.", "Compass to %s, to go back whenever you want.")
for oid, es, en in [("berries", "Comida fresca del santuario.", "Fresh food from the sanctuary."),
                    ("healing", "Te cura al momento.", "Heals you at once."),
                    ("regen", "Recuperas vida poco a poco.", "Regain health little by little."),
                    ("saplings", "Para plantar tu propio bosque.", "To plant your own forest."),
                    ("bonemeal", "Hace crecer las plantas.", "Makes plants grow."),
                    ("acorn", "Una bellota mágica del santuario.", "A magic acorn from the sanctuary."),
                    ("pack_basic", "Un sobre de cartas básico.", "A basic card pack."),
                    ("fire_res", "Ocho minutos sin quemarte.", "Eight minutes without burning."),
                    ("pack_silver", "Un sobre de cartas de plata.", "A silver card pack."),
                    ("golden_acorn", "Una bellota dorada, muy rara.", "A golden acorn, very rare."),
                    ("golden_apple", "Absorción y regeneración.", "Absorption and regeneration."),
                    ("pack_gold", "Un sobre de cartas de oro.", "A gold card pack."),
                    ("exp_bottles", "Experiencia embotellada.", "Bottled experience."),
                    ("book_mending", "Reparación: tus cosas se arreglan con experiencia.", "Mending: your gear repairs itself with experience."),
                    ("blaze_rods", "Varas de blaze traídas del volcán.", "Blaze rods brought from the volcano."),
                    ("magma_cream", "Crema de magma del volcán.", "Magma cream from the volcano."),
                    ("god_apple", "La manzana más poderosa que existe.", "The most powerful apple there is.")]:
    k(g + "desc." + oid, es, en)


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
    print("lang285:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
