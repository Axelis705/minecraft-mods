"""Etapa 289: el Gran Templo del Bosque ahora es un árbol gigante: las pistas de los habitantes y la crónica hablan del árbol.
Uso: python3 lang289.py <carpeta lang>"""
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
CES[n + "ruda.hint.bosque"] = ("Hay un árbol gigantesco, el Gran Templo del Bosque, hacia %s, a unos %s bloques. Se entra entre sus raíces y se "
                               "sube por dentro del tronco hueco: arriba, en la copa, está la Piedra de Portal.")
CEN[n + "ruda.hint.bosque"] = ("There is a gigantic tree, the Great Forest Temple, to the %s, about %s blocks away. You go in between its roots "
                               "and climb inside the hollow trunk: up in the crown is the Portal Stone.")
CES[n + "bruno.hint.bosque"] = "Hay un árbol gigantesco, el Gran Templo del Bosque, hacia %s, a unos %s bloques. Ruda dice que huele a musgo. Yo digo que huele a primavera."
CEN[n + "bruno.hint.bosque"] = "There is a gigantic tree, the Great Forest Temple, to the %s, about %s blocks away. Ruda says it smells of moss. I say it smells of spring."
CES[n + "hilda.hint.bosque"] = "Hay un árbol gigantesco, el Gran Templo del Bosque, hacia %s, a unos %s bloques. Todo madera y nada de hierro. No es lo mío, pero sí lo de Ruda."
CEN[n + "hilda.hint.bosque"] = "There is a gigantic tree, the Great Forest Temple, to the %s, about %s blocks away. All wood and no iron. Not my thing, but it is Ruda's."
CES[n + "teo.hint.bosque"] = "Hay un árbol gigantesco, el Gran Templo del Bosque, hacia %s, a unos %s bloques. Dentro del tronco hay una biblioteca. ¡Dentro de un árbol!"
CEN[n + "teo.hint.bosque"] = "There is a gigantic tree, the Great Forest Temple, to the %s, about %s blocks away. There is a library inside the trunk. Inside a tree!"
CES[n + "ignacio.hint.bosque"] = "Hay un árbol gigantesco, el Gran Templo del Bosque, hacia %s, a unos %s bloques. Ruda dice que allí todo crece. Hasta el valor, supongo."
CEN[n + "ignacio.hint.bosque"] = "There is a gigantic tree, the Great Forest Temple, to the %s, about %s blocks away. Ruda says everything grows there. Even courage, I guess."
# etapa 287: el título del despertar del volcán no cabía en pantallas pequeñas
CES["title.%s.volcan.despierta" % P] = "¡Erupción!"
CEN["title.%s.volcan.despierta" % P] = "Eruption!"
CES["title.%s.volcan.despierta.sub" % P] = "El volcán despierta..."
CEN["title.%s.volcan.despierta.sub" % P] = "The volcano awakens..."
CES["lore.%s.bosque_3.p1" % P] = "En la copa del Árbol del Mundo, por encima de todas sus ramas, la Piedra de Portal late como un corazón verde."
CEN["lore.%s.bosque_3.p1" % P] = "In the crown of the World Tree, above all its branches, the Portal Stone beats like a green heart."


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
    print("lang289:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
