#!/usr/bin/env python3
"""Revisa los recursos de un mod Forge 1.20.1 y avisa de errores tipicos.
Uso: python3 tools/check_resources.py <carpeta-del-mod>
Sale con codigo 1 si encuentra ERRORES (los AVISOS no fallan)."""
import json, os, re, sys

mod = sys.argv[1]
res = os.path.join(mod, "src/main/resources")
errors, warns = [], []
E = lambda m: errors.append(m)
W = lambda m: warns.append(m)

def load(p):
    try:
        with open(p, encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        E(f"JSON invalido: {rel(p)} ({e})")
        return None

def rel(p): return os.path.relpath(p, mod)

assets = os.path.join(res, "assets")
if not os.path.isdir(assets):
    print("sin assets, nada que revisar"); sys.exit(0)

def exists_tex(ns, path): return os.path.isfile(os.path.join(assets, ns, "textures", path + ".png"))
def exists_model(ns, path): return os.path.isfile(os.path.join(assets, ns, "models", path + ".json"))
def split(r, d):
    return r.split(":", 1) if ":" in r else (d, r)

def check_display(model, p):
    for slot, d in (model.get("display") or {}).items():
        for k in ("rotation", "translation", "scale"):
            if k in d and not (isinstance(d[k], list) and len(d[k]) == 3 and all(isinstance(x, (int, float)) for x in d[k])):
                E(f"{rel(p)}: display.{slot}.{k} debe ser lista de 3 numeros (si no, el item sale morado)")

for ns in sorted(os.listdir(assets)):
    base = os.path.join(assets, ns)
    if not os.path.isdir(base): continue
    # modelos
    mdir = os.path.join(base, "models")
    for root, _, files in os.walk(mdir):
        for fn in files:
            if not fn.endswith(".json"): continue
            p = os.path.join(root, fn); m = load(p)
            if not isinstance(m, dict): continue
            check_display(m, p)
            parent = m.get("parent")
            if parent:
                pn, pp = split(parent, "minecraft")
                if pn != "minecraft" and not exists_model(pn, pp) and not pp.startswith("builtin/"):
                    E(f"{rel(p)}: parent '{parent}' no existe")
            tex = m.get("textures") or {}
            for k, v in tex.items():
                if v.startswith("#"):
                    continue
                tn, tp = split(v, "minecraft")
                if tn != "minecraft" and not exists_tex(tn, tp):
                    E(f"{rel(p)}: textura '{v}' no existe")
            for ov in m.get("overrides", []) or []:
                mm = ov.get("model")
                if mm:
                    on, op = split(mm, "minecraft")
                    if on != "minecraft" and not exists_model(on, op):
                        E(f"{rel(p)}: override modelo '{mm}' no existe")
            # Minecraft no carga un modelo con cajas fuera de -16..32 (el bloque falta en el juego)
            for el in m.get("elements", []) or []:
                for k in ("from", "to"):
                    if any(v < -16 or v > 32 for v in el.get(k, [])):
                        E(f"{rel(p)}: '{k}' {el[k]} se sale de -16..32")
                        break
            # texturas usadas por elementos pero no definidas
            used = set()
            for el in m.get("elements", []) or []:
                for face in (el.get("faces") or {}).values():
                    t = face.get("texture", "")
                    if t.startswith("#"): used.add(t[1:])
            if used and not parent:
                for u in used:
                    if u not in tex and u != "particle":
                        E(f"{rel(p)}: usa #{u} pero no la define")
            if "elements" in m and "particle" not in tex and not parent and m.get("textures") is not None:
                W(f"{rel(p)}: sin textura 'particle'")
    # blockstates
    bdir = os.path.join(base, "blockstates")
    if os.path.isdir(bdir):
        for fn in os.listdir(bdir):
            p = os.path.join(bdir, fn); b = load(p)
            if not isinstance(b, dict): continue
            def models(o):
                if isinstance(o, dict):
                    if "model" in o: yield o["model"]
                    for v in o.values(): yield from models(v)
                elif isinstance(o, list):
                    for v in o: yield from models(v)
            for mm in models(b):
                if isinstance(mm, str):
                    mn, mp = split(mm, ns)
                    if mn != "minecraft" and not exists_model(mn, mp):
                        E(f"{rel(p)}: modelo '{mm}' no existe")
    # lang
    ldir = os.path.join(base, "lang")
    langs = {}
    if os.path.isdir(ldir):
        for fn in os.listdir(ldir):
            if fn.endswith(".json"):
                l = load(os.path.join(ldir, fn))
                if isinstance(l, dict): langs[fn] = l
        if len(langs) > 1:
            allk = set().union(*[set(v) for v in langs.values()])
            for fn, l in langs.items():
                miss = sorted(allk - set(l))
                if miss: W(f"lang {ns}/{fn}: faltan {len(miss)} claves, p.ej. {miss[:3]}")
    # sonidos
    sj = os.path.join(base, "sounds.json")
    if os.path.isfile(sj):
        s = load(sj)
        for name, ent in (s or {}).items():
            for snd in ent.get("sounds", []):
                n = snd["name"] if isinstance(snd, dict) else snd
                sn, sp = split(n, ns)
                if not os.path.isfile(os.path.join(assets, sn, "sounds", sp + ".ogg")):
                    E(f"sounds.json: '{n}' no tiene .ogg")
    # texturas: solo se avisa de PNG corruptos
    tdir = os.path.join(base, "textures")
    for root, _, files in os.walk(tdir):
        for fn in files:
            if fn.endswith(".png"):
                with open(os.path.join(root, fn), "rb") as f:
                    if f.read(8) != b"\x89PNG\r\n\x1a\n":
                        E(f"{rel(os.path.join(root, fn))}: no es PNG valido")

# claves de lang usadas en codigo (translatable) que no existen
code_keys = set()
jdir = os.path.join(mod, "src/main/java")
for root, _, files in os.walk(jdir):
    for fn in files:
        if fn.endswith(".java"):
            with open(os.path.join(root, fn), encoding="utf-8", errors="ignore") as f:
                for m in re.finditer(r'(?:Component\.translatable|translatable)\(\s*"([a-z0-9_.\-]+)"', f.read()):
                    code_keys.add(m.group(1))
allkeys = set()
for ns in os.listdir(assets):
    ld = os.path.join(assets, ns, "lang")
    if os.path.isdir(ld):
        for fn in os.listdir(ld):
            if fn.endswith(".json"):
                try: allkeys |= set(json.load(open(os.path.join(ld, fn), encoding="utf-8")))
                except Exception: pass
for k in sorted(code_keys - allkeys):
    W(f"clave de texto usada en codigo pero sin traduccion: {k}")

# recetas
ddir = os.path.join(res, "data")
if os.path.isdir(ddir):
    for root, _, files in os.walk(ddir):
        for fn in files:
            if fn.endswith(".json"): load(os.path.join(root, fn))

# mods.toml / pack.mcmeta
if not os.path.isfile(os.path.join(res, "pack.mcmeta")): E("falta pack.mcmeta")
if not os.path.isfile(os.path.join(res, "META-INF/mods.toml")): E("falta META-INF/mods.toml")

print(f"[{mod}] errores: {len(errors)}  avisos: {len(warns)}")
for m in errors: print("ERROR:", m)
for m in warns[:40]: print("AVISO:", m)
if len(warns) > 40: print(f"... y {len(warns)-40} avisos mas")
sys.exit(1 if errors else 0)
