#!/usr/bin/env python3
"""Mete los textos de las cartas en los archivos de idioma del mod principal (Forge no mezcla archivos de idioma
con el mismo nombre: si dos carpetas tienen es_mx.json, gana uno solo). Es seguro repetirlo.
Uso: python3 herramientas/fusionar_lang.py <carpeta lang del mod principal>
   p. ej. ...\\Mundos de Axelis\\src\\main\\resources\\assets\\mundos_de_axelis\\lang"""
import json, os, sys

dst = sys.argv[1]
src = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "mundos_de_axelis", "lang")
for name in sorted(os.listdir(src)):
    if not name.endswith(".json"):
        continue
    mine = json.load(open(os.path.join(src, name), encoding="utf-8"))
    path = os.path.join(dst, name)
    cur = json.load(open(path, encoding="utf-8")) if os.path.isfile(path) else {}
    n = sum(1 for k in mine if cur.get(k) != mine[k])
    cur.update(mine)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(cur, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"{name}: {n} textos nuevos o cambiados")
