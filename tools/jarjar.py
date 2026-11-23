#!/usr/bin/env python3
"""
Mete un mod dentro de otro (Jar-in-Jar de Forge), sin depender de ForgeGradle (sirve igual en 1.20.1, 1.21.x y 26.x).

    python3 tools/jarjar.py <mod.jar> <libreria.jar> [grupo] [artefacto] [rango]

- El jar de la librería queda en META-INF/jarjar/<nombre>.jar dentro del jar del mod.
- Se escribe (o se completa) META-INF/jarjar/metadata.json, que Forge lee al arrancar: si varios mods traen la misma librería
  (mismo grupo y artefacto), carga UNA sola, la de versión más alta dentro de los rangos que piden.
- La versión se saca del mods.toml de la librería. Por defecto: grupo com.axeliscore, artefacto axelis_core, rango [1.0.0,).
"""
import json
import os
import re
import shutil
import sys
import tempfile
import zipfile


def lib_version(lib):
    with zipfile.ZipFile(lib) as z:
        toml = z.read("META-INF/mods.toml").decode("utf-8")
    m = re.search(r'^\s*version\s*=\s*"([^"]+)"', toml, re.M)
    if not m:
        sys.exit("la librería no tiene version en su mods.toml")
    return m.group(1)


def main(mod, lib, group="com.axeliscore", artifact="axelis_core", rng="[1.0.0,)"):
    ver = lib_version(lib)
    inner = "META-INF/jarjar/" + os.path.basename(lib)
    meta = {"jars": []}
    fd, tmp = tempfile.mkstemp(suffix=".jar")
    os.close(fd)
    with zipfile.ZipFile(mod) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
        for info in src.infolist():
            if info.filename == "META-INF/jarjar/metadata.json":
                meta = json.loads(src.read(info).decode("utf-8"))
                continue
            if info.filename.startswith("META-INF/jarjar/") and info.filename.endswith(".jar"):
                # una copia anterior de la misma librería (otra versión): se quita
                old = [j for j in meta.get("jars", []) if j.get("path") == info.filename]
                if os.path.basename(info.filename).startswith(artifact + "-") or old:
                    continue
            dst.writestr(info, src.read(info))
        # la librería va guardada sin comprimir (Forge la abre como un sistema de archivos dentro del jar)
        li = zipfile.ZipInfo(inner)
        li.compress_type = zipfile.ZIP_STORED
        li.date_time = (2026, 1, 1, 0, 0, 0)
        with open(lib, "rb") as fh:
            dst.writestr(li, fh.read())
        meta["jars"] = [j for j in meta.get("jars", [])
                        if not (j.get("identifier", {}).get("group") == group and j.get("identifier", {}).get("artifact") == artifact)]
        meta["jars"].append({
            "identifier": {"group": group, "artifact": artifact},
            "version": {"range": rng, "artifactVersion": ver},
            "path": inner,
            "isObfuscated": False,
        })
        dst.writestr("META-INF/jarjar/metadata.json", json.dumps(meta, indent=2))
    shutil.move(tmp, mod)
    print(f"{os.path.basename(mod)}: lleva dentro {os.path.basename(lib)} ({group}:{artifact} {ver}, rango {rng})")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    main(*sys.argv[1:])
