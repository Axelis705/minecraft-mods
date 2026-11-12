#!/usr/bin/env python3
"""Copia el español a todas sus variantes de Minecraft (México, Argentina, Chile...). Correr después de cambiar es_es.json."""
import os
import shutil

L = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "lang")
for v in ("es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve"):
    shutil.copyfile(os.path.join(L, "es_es.json"), os.path.join(L, v + ".json"))
print("variantes del español listas")
