#!/usr/bin/env python3
"""Escribe docs/guion-voces.md (el guion de voces para grabar) a partir de Pantallas.java y es_mx.json."""
import json
import os
import re

AQUI = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(AQUI)
src = open(os.path.join(MOD, "src/main/java/com/coleccionista/evento/Pantallas.java"), encoding="utf-8").read()
lang = json.load(open(os.path.join(MOD, "src/main/resources/assets/coleccionista/lang/es_mx.json"), encoding="utf-8"))
orden = [("bienvenida", "El despertar: el discurso de la tele (después de investigar)"),
         ("salida", "El despertar: cuando todas salen de sus celdas (pantalla grande)"), ("montacargas", "El montacargas (entre pruebas)"),
         ("encadenados", "Prueba 1 · Encadenados"),
         ("traidor", "Prueba 2 · El traidor"), ("compartir", "Prueba 3 · Compartir o robar"), ("ciegas", "Prueba 4 · A ciegas"),
         ("sacrificio", "Prueba 5 · El sacrificio"), ("puertas", "Prueba 6 · Las puertas"), ("copas", "Prueba 7 (final) · Las copas"),
         ("tres", "Prueba extra · Encadenados de tres")]
out = ["# Guion de voces del Coleccionista (para grabar)\n",
       "Generado desde el mod (`herramientas/guion.py`). Cada línea es **un archivo de audio**; el texto es el subtítulo que sale en la tele y "
       "abajo en pantalla. Puedes decirlo con tus palabras si suena mejor: avísame y cambio el subtítulo.\n",
       "**Cómo grabar:** un archivo por línea, voz sola (sin música), con un poco de silencio al principio y al final. Mejor en **OGG**; si "
       "grabas en MP3 o WAV yo los convierto (`herramientas/voces.py`). El nombre del archivo es la columna *Archivo*. *Dura* es el tiempo que "
       "la línea está en pantalla ahora: si tu grabación dura más, se ajusta solo a tu audio.\n",
       "Las risas por ahora suenan con un efecto del juego; si grabas tu risa, se usa la tuya.\n"]
total, seg = 0, 0.0
for gid, titulo in orden:
    m = re.search(r"public static List<Frase> " + gid + r"\(\) \{(.*?)return l;", src, re.S)
    if not m:
        continue
    fr = re.findall(r'new Frase\("([a-z_]+)", (\d+)', m.group(1))
    out.append(f"## {titulo}\n")
    out.append("| # | Archivo | Dura | Texto |\n|---|---|---|---|")
    for i, (k, t) in enumerate(fr, 1):
        total += 1
        seg += int(t) / 20
        out.append(f"| {i} | `{gid}_{k}.ogg` | {int(t) / 20:.1f} s | {lang['coleccionista.voz.%s.%s' % (gid, k)]} |")
    out.append("")
out.append(f"**Total: {total} líneas (≈{seg / 60:.0f} minutos de voz).** Cuando las tengas, pásamelas todas juntas y yo las meto al mod.\n")
open(os.path.join(MOD, "docs/guion-voces.md"), "w", encoding="utf-8").write("\n".join(out))
print(total, "líneas")
