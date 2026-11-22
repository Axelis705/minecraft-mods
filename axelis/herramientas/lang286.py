"""Etapa 286: textos de la Mesa del Artífice (recetas por pestañas, reparadores que se aprenden) y de la Guía del Gemista.
Uso: python3 lang286.py <carpeta lang>"""
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


t = "gui.%s.table." % P
k(t + "craft_cat.reparar", "Reparar %s/%s", "Repair %s/%s")
k(t + "craft_cat.utiles", "Útiles %s/%s", "Useful %s/%s")
k(t + "craft_cat.mesa", "Base %s/%s", "Base %s/%s")
k(t + "craft_unknown", "??? (aún no lo conoces)", "??? (not known yet)")
k(t + "craft_unknown_hint", "Tenlo una vez y aprenderás a crearlo.", "Hold one once to learn how to make it.")
k("message.%s.table.craft_unknown" % P, "Aún no conoces esa receta: tenlo una vez para aprenderla.", "You don't know that recipe yet: hold one once to learn it.")
k("message.%s.receta_nueva" % P, "Nueva receta en la Mesa del Artífice: %s", "New recipe at the Artificer's Table: %s")
k("item.%s.guia_gemas" % P, "Guía del Gemista", "Gemcutter's Guide")
k("tooltip.%s.guia_gemas" % P, "Clic derecho para leerla: todo sobre las gemas.", "Right click to read it: everything about gems.")
k("message.%s.guia_gemas" % P, "Te llevas la Guía del Gemista: ábrela con clic derecho.", "You got the Gemcutter's Guide: open it with right click.")
b = "libro.%s.gemas." % P
PAGES = [
    ("Guía del Gemista", "Las gemas dan poderes a tus armas y armaduras.\nTodo se hace en la Mesa de Gemas: infundir, combinar, engarzar y quitar.\n\nPasa la página.",
     "Gemcutter's Guide", "Gems give powers to your weapons and armor.\nEverything happens at the Gem Table: infuse, combine, socket and remove.\n\nTurn the page."),
    ("Gemas vacías", "Salen en cofres, barriles, élites, Saqueadores y encargos.\nSu pureza depende del tier:\n1-2 Agrietada\n3-4 Tallada\n5-6 Pulida\n7-8 Perfecta",
     "Empty gems", "They come from chests, barrels, elites, Looters and tasks.\nTheir purity depends on the tier:\n1-2 Cracked\n3-4 Cut\n5-6 Polished\n7-8 Perfect"),
    ("Esencias", "Cada elemento tiene su esencia. Caen de élites, encargos y cofres de su tier:\n1 Veneno · 2 Filo\n3 Escarcha · 4 Fuego\n5 Arcano · 6 Vampiro\n7 Rayo · 8 Vacío",
     "Essences", "Each element has its essence. They drop from elites, tasks and chests of their tier:\n1 Poison · 2 Edge\n3 Frost · 4 Fire\n5 Arcane · 6 Vampire\n7 Lightning · 8 Void"),
    ("Infundir", "Pon una gema vacía y una esencia: sale una gema de ese elemento con la misma pureza.\n\nCuesta 20 ◆ por cada nivel de pureza.",
     "Infuse", "Put an empty gem and an essence: you get a gem of that element with the same purity.\n\nIt costs 20 ◆ per purity level."),
    ("Combinar", "Tres gemas iguales (mismo elemento y pureza, o tres vacías) se funden en una de la pureza siguiente.\n\nCuesta 30 ◆ por nivel. La Perfecta es la mejor.",
     "Combine", "Three equal gems (same element and purity, or three empty ones) fuse into one of the next purity.\n\nIt costs 30 ◆ per level. Perfect is the best."),
    ("Engarzar", "Pon un arma (espadas, hachas, tridentes y las armas nuevas) o una pieza de armadura y una gema.\nUna gema por pieza.\nQuitarla cuesta 15 ◆ y te la devuelve.",
     "Socket", "Put a weapon (swords, axes, tridents and the new weapons) or an armor piece and a gem.\nOne gem per piece.\nRemoving it costs 15 ◆ and gives it back."),
    ("La pureza", "Cada nivel de pureza hace más fuerte el efecto.\nEn la armadura, las gemas de las piezas se suman.\n\nEn las páginas siguientes: qué hace cada elemento.",
     "Purity", "Each purity level makes the effect stronger.\nOn armor, the gems of every piece add up.\n\nNext pages: what each element does."),
]
for i, (tes, es, ten, en) in enumerate(PAGES, 1):
    k(b + "%d.titulo" % i, tes, ten)
    k(b + "%d" % i, es, en)
k(b + "fin.titulo", "Consejos", "Tips")
k(b + "fin", "· Guarda las vacías de tiers bajos para combinarlas.\n· Una gema de Vacío en cada pieza da mucha vida.\n· Si pierdes la guía, se crea en la Mesa del Artífice.",
  "· Keep low-tier empty gems to combine them.\n· A Void gem in every piece gives a lot of health.\n· If you lose this guide, craft it at the Artificer's Table.")


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
    print("lang286:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
