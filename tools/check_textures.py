#!/usr/bin/env python3
"""Revisa las texturas PNG de un mod. Uso: python3 tools/check_textures.py <carpeta-del-mod>
ERROR: textura totalmente transparente o ilegible. AVISO: tamaños raros, enormes o sin transparencia donde se espera."""
import os, sys
from PIL import Image
mod = sys.argv[1]; root = os.path.join(mod, "src/main/resources/assets")
errs, warns, n = [], [], 0
for dp, _, fs in os.walk(root):
    for fn in fs:
        if not fn.endswith(".png"): continue
        p = os.path.join(dp, fn); r = os.path.relpath(p, mod); n += 1
        try:
            im = Image.open(p); im.load()
        except Exception as e:
            errs.append(f"{r}: no se puede leer ({e})"); continue
        w, h = im.size
        im = im.convert("RGBA")
        a = im.getchannel("A")
        lo, hi = a.getextrema()
        if hi == 0:
            errs.append(f"{r}: totalmente transparente (se vera invisible)"); continue
        if w > 2048 or h > 2048: warns.append(f"{r}: muy grande ({w}x{h})")
        if w % 2 or h % 2: warns.append(f"{r}: tamaño impar ({w}x{h}), puede dar problemas de mipmap")
        sub = os.path.relpath(p, root).replace("\\", "/")
        if "/textures/item/" in "/" + sub or "/textures/block/" in "/" + sub:
            if w != h and not os.path.exists(p + ".mcmeta"):
                warns.append(f"{r}: no cuadrada ({w}x{h}); en 1.20.1 funciona pero si no es animacion revisa que sea a proposito")
        if "/textures/item/" in "/" + sub and lo == 255:
            warns.append(f"{r}: item sin ninguna transparencia (se vera como un cuadrado)")
print(f"[{mod}] texturas: {n}  errores: {len(errs)}  avisos: {len(warns)}")
for m in errs: print("ERROR:", m)
for m in warns[:30]: print("AVISO:", m)
sys.exit(1 if errs else 0)
