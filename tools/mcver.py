#!/usr/bin/env python3
"""
Un solo código para varias versiones de Minecraft.

El código se guarda con la versión 1.20.1 activa. Las partes de otras versiones van comentadas así:

    //#if MC >= 1.21
    //$$ ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, name);
    //#else
    ResourceLocation id = new ResourceLocation(MODID, name);
    //#endif

Uso:
    python3 tools/mcver.py <mod> <version> <carpeta_salida>
        Genera src/ (java + recursos) para esa versión: activa/comenta las ramas y adapta los JSON (carpetas que cambiaron
        de nombre, formato de recetas, definiciones de ítems de 1.21.4+, pack.mcmeta, mods.toml).
    python3 tools/mcver.py --check <mod>
        Revisa que todas las marcas estén bien cerradas.

Condiciones: MC >= x, MC > x, MC <= x, MC < x, MC == x, MC != x, unidas con && y ||.
"""
import json
import os
import re
import shutil
import sys

VERSIONS = ["1.20.1", "1.21.1", "1.21.4", "26.2", "26.3"]


def vkey(v):
    return tuple(int(p) for p in v.split("."))


COND = re.compile(r"MC\s*(>=|<=|==|!=|>|<)\s*([0-9.]+)")


def evaluate(expr, mc):
    m = vkey(mc)

    def one(op, v):
        k = vkey(v)
        return {">=": m >= k, "<=": m <= k, "==": m == k, "!=": m != k, ">": m > k, "<": m < k}[op]

    py = COND.sub(lambda g: str(one(g.group(1), g.group(2))), expr)
    py = py.replace("&&", " and ").replace("||", " or ").replace("!", " not ")
    if re.search(r"[^ TrueFalsandotr()]", py.replace("True", "").replace("False", "")):
        raise ValueError("condición rara: " + expr)
    return bool(eval(py))


DIRECTIVE = re.compile(r"^(\s*)//#(if|elif|else|endif)\b(.*)$")
COMMENTED = re.compile(r"^(\s*)//\$\$ ?(.*)$")


def process(lines, mc, path="?"):
    out = []
    # pila de (rama_ya_tomada, activa_ahora, padre_activo)
    stack = []
    active = True
    for n, line in enumerate(lines, 1):
        raw = line.rstrip("\n")
        d = DIRECTIVE.match(raw)
        if d:
            kind, rest = d.group(2), d.group(3).strip()
            if kind == "if":
                taken = active and evaluate(rest, mc)
                stack.append([taken, active])
                active = taken
            elif kind == "elif":
                if not stack:
                    raise ValueError(f"{path}:{n} elif sin if")
                top = stack[-1]
                now = top[1] and not top[0] and evaluate(rest, mc)
                top[0] = top[0] or now
                active = now
            elif kind == "else":
                if not stack:
                    raise ValueError(f"{path}:{n} else sin if")
                top = stack[-1]
                active = top[1] and not top[0]
                top[0] = True
            else:
                if not stack:
                    raise ValueError(f"{path}:{n} endif sin if")
                top = stack.pop()
                active = top[1]
            out.append(raw)
            continue
        c = COMMENTED.match(raw)
        if active:
            out.append(c.group(1) + c.group(2) if c else raw)
        else:
            if c or raw.strip() == "":
                out.append(raw)
            else:
                ind = re.match(r"^(\s*)", raw).group(1)
                out.append(ind + "//$$ " + raw[len(ind):])
    if stack:
        raise ValueError(f"{path}: falta #endif")
    return [l + "\n" for l in out]


# ------------------------------------------------------------------------------------------------------------ recursos

PACK = {"1.20.1": (15, 15), "1.21.1": (34, 48), "1.21.4": (46, 61), "26.2": (88, 107), "26.3": (97, 121)}


def ingredient(x):
    """1.21.2+: un ingrediente simple es solo el id (o #tag)."""
    if isinstance(x, dict) and set(x) == {"item"}:
        return x["item"]
    if isinstance(x, dict) and set(x) == {"tag"}:
        return "#" + x["tag"]
    if isinstance(x, list):
        return [ingredient(i) for i in x]
    return x


def recipe(obj, mc):
    m = vkey(mc)
    r = dict(obj)
    if m >= (1, 20, 5) and isinstance(r.get("result"), dict) and "item" in r["result"]:
        res = dict(r["result"])
        res["id"] = res.pop("item")
        r["result"] = res
    if m >= (1, 21, 2):
        if "key" in r:
            r["key"] = {k: ingredient(v) for k, v in r["key"].items()}
        if "ingredients" in r:
            r["ingredients"] = [ingredient(i) for i in r["ingredients"]]
        if "ingredient" in r:
            r["ingredient"] = ingredient(r["ingredient"])
    return r


RENAMES_121 = [("/recipes/", "/recipe/"), ("/loot_tables/", "/loot_table/"), ("/advancements/", "/advancement/"),
               ("/tags/blocks/", "/tags/block/"), ("/tags/items/", "/tags/item/"), ("/tags/entity_types/", "/tags/entity_type/"),
               ("/tags/worldgen/structure/", "/tags/worldgen/structure/")]


def res_path(rel, mc):
    p = "/" + rel.replace(os.sep, "/")
    if vkey(mc) >= (1, 21):
        for a, b in RENAMES_121:
            p = p.replace(a, b)
    return p[1:]


def gen_resources(src_res, dst_res, mc, modid):
    m = vkey(mc)
    item_models = []
    for root, _, files in os.walk(src_res):
        for f in files:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, src_res)
            out = os.path.join(dst_res, res_path(rel, mc))
            os.makedirs(os.path.dirname(out), exist_ok=True)
            relp = rel.replace(os.sep, "/")
            if f.endswith(".json") and "/recipes/" in "/" + relp:
                with open(full, encoding="utf-8") as fh:
                    obj = json.load(fh)
                with open(out, "w", encoding="utf-8") as fh:
                    json.dump(recipe(obj, mc), fh, indent=2, ensure_ascii=False)
                continue
            if relp == "pack.mcmeta":
                r, d = PACK[mc]
                meta = {"pack": {"description": f"{modid} resources", "pack_format": r}}
                if m >= (1, 21, 9):
                    meta["pack"]["min_format"] = [r, 0]
                    meta["pack"]["max_format"] = [r, 0]
                with open(out, "w", encoding="utf-8") as fh:
                    json.dump(meta, fh, indent=2)
                continue
            if relp.startswith(f"assets/{modid}/models/item/") and f.endswith(".json"):
                item_models.append(f[:-5])
            # una versión puede traer su propio archivo: nombre.json + nombre@1.21.4.json (gana el más alto que aplique)
            if "@" in f:
                continue
            best = None
            stem, ext = os.path.splitext(f)
            for g in os.listdir(root):
                if g.startswith(stem + "@") and g.endswith(ext):
                    v = g[len(stem) + 1:-len(ext)]
                    if vkey(v) <= m and (best is None or vkey(v) > vkey(best[0])):
                        best = (v, g)
            shutil.copyfile(os.path.join(root, best[1]) if best else full, out)
    # 1.21.4+: cada ítem necesita su definición en assets/<mod>/items/
    if m >= (1, 21, 4):
        items_dir = os.path.join(dst_res, "assets", modid, "items")
        os.makedirs(items_dir, exist_ok=True)
        for name in item_models:
            path = os.path.join(items_dir, name + ".json")
            if not os.path.exists(path):
                with open(path, "w", encoding="utf-8") as fh:
                    json.dump({"model": {"type": "minecraft:model", "model": f"{modid}:item/{name}"}}, fh, indent=2)


def gen(mod, mc, out):
    base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", mod)
    src = os.path.join(base, "src", "main")
    if os.path.exists(out):
        shutil.rmtree(out)
    jdst = os.path.join(out, "src", "main", "java")
    for root, _, files in os.walk(os.path.join(src, "java")):
        for f in files:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, os.path.join(src, "java"))
            dst = os.path.join(jdst, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(full, encoding="utf-8") as fh:
                lines = fh.readlines()
            with open(dst, "w", encoding="utf-8") as fh:
                fh.writelines(process(lines, mc, rel))
    modid = mod_id(base)
    gen_resources(os.path.join(src, "resources"), os.path.join(out, "src", "main", "resources"), mc, modid)


def mod_id(base):
    with open(os.path.join(base, "gradle.properties"), encoding="utf-8") as fh:
        for line in fh:
            if line.startswith("mod_id="):
                return line.split("=", 1)[1].strip()
    return os.path.basename(base)


def check(mod):
    base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", mod, "src", "main", "java")
    bad = 0
    for root, _, files in os.walk(base):
        for f in files:
            p = os.path.join(root, f)
            with open(p, encoding="utf-8") as fh:
                lines = fh.readlines()
            for v in VERSIONS:
                try:
                    res = process(lines, v, p)
                except Exception as e:  # noqa: BLE001
                    print("ERROR", e)
                    bad += 1
                    break
                if v == "1.20.1" and [l.rstrip("\n") for l in res] != [l.rstrip("\n") for l in lines]:
                    print("AVISO: el archivo no está guardado con 1.20.1 activa:", p)
                    bad += 1
                    break
    print("marcas OK" if not bad else f"{bad} problema(s)")
    return bad


if __name__ == "__main__":
    if sys.argv[1] == "--check":
        sys.exit(1 if check(sys.argv[2]) else 0)
    gen(sys.argv[1], sys.argv[2], sys.argv[3])
    print("listo", sys.argv[2])
