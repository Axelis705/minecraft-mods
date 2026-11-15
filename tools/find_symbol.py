#!/usr/bin/env python3
"""Busca en el jar de Minecraft las clases que mencionan un texto (nombre de campo o método) y muestra esos miembros con javap.
Uso: python3 tools/find_symbol.py <jar> <texto> [<texto> ...]"""
import subprocess
import sys
import zipfile

jar = sys.argv[1]
with zipfile.ZipFile(jar) as z:
    for word in sys.argv[2:]:
        hits = []
        for n in z.namelist():
            if n.endswith(".class") and (n.startswith("net/minecraft") or n.startswith("com/mojang")):
                if word.encode() in z.read(n):
                    hits.append(n[:-6].replace("/", "."))
        print(f"===== '{word}' aparece en {len(hits)} clases")
        for c in hits[:25]:
            out = subprocess.run(["javap", "-cp", jar, "-p", c], capture_output=True, text=True).stdout
            lines = [l.strip() for l in out.splitlines() if word.lower() in l.lower()]
            print(f"  {c}: " + (" | ".join(lines[:4]) if lines else "(solo lo usa)"))
