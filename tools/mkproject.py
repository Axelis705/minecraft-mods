#!/usr/bin/env python3
"""
Crea un proyecto Gradle listo para compilar un mod en otra versión de Minecraft (1.21.1, 1.21.4, 26.2, 26.3).

    python3 tools/mkproject.py <mod> <version> <carpeta_salida>

1) mcver.py genera el código y los recursos de esa versión.
2) Se copia la plantilla de ForgeGradle 7 (tools/plantilla-fg7) con la versión de Forge, Java y Gradle que toca.
3) Se rellena META-INF/mods.toml con los datos de gradle.properties del mod (en 1.20.1 lo hace Gradle).
La 1.20.1 se compila directo desde la carpeta del mod (el código se guarda con esa versión activa).
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mcver  # noqa: E402

V = {
    "1.21.1": dict(forge="52.1.20", java=21, fg="[7.0.3,8)", gradle="9.3.1", validator=None, loader="[52,)", mc="[1.21.1,1.21.2)"),
    "1.21.4": dict(forge="54.1.22", java=21, fg="[7.0.3,8)", gradle="9.3.1", validator=None, loader="[54,)", mc="[1.21.4,1.21.5)"),
    "26.2": dict(forge="65.1.5", java=25, fg="[7.0.17,8)", gradle="9.5.0", validator="7.0.5", loader="[65,)", mc="[26.2,26.3)"),
    "26.3": dict(forge="66.1.5", java=25, fg="[7.0.17,8)", gradle="9.7.1", validator="7.0.6", loader="[66,)", mc="[26.3,26.4)"),
}


def props(base):
    out = {}
    with open(os.path.join(base, "gradle.properties"), encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                out[k.strip()] = v.strip()
    return out


def main(mod, mc, out):
    if mc not in V:
        sys.exit("versión no soportada aquí: " + mc)
    cfg = V[mc]
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    base = os.path.join(root, mod)
    p = props(base)
    mcver.gen(mod, mc, out)
    tpl = os.path.join(os.path.dirname(os.path.abspath(__file__)), "plantilla-fg7")
    shutil.copytree(os.path.join(tpl, "gradle"), os.path.join(out, "gradle"), dirs_exist_ok=True)
    shutil.copy(os.path.join(tpl, "gradlew"), os.path.join(out, "gradlew"))
    os.chmod(os.path.join(out, "gradlew"), 0o755)
    with open(os.path.join(out, "gradle", "wrapper", "gradle-wrapper.properties"), "w") as fh:
        fh.write("distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\n"
                 f"distributionUrl=https\\://services.gradle.org/distributions/gradle-{cfg['gradle']}-bin.zip\n"
                 "validateDistributionUrl=true\nzipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists\n")
    rep = {
        "@MC@": mc, "@FORGE@": cfg["forge"], "@FG@": cfg["fg"], "@JAVA@": str(cfg["java"]),
        "@MODID@": p["mod_id"], "@MODVER@": p.get("mod_version", "1.0.0"), "@GROUP@": p.get("mod_group_id", "com." + mod),
        "@MAPPINGS@": f"    mappings channel: 'official', version: '{mc}'" if mc.startswith("1.") else "",
        "@STRICT@": "            systemProperty 'eventbus.api.strictRuntimeChecks', 'true'" if cfg["validator"] else "",
        "@VALIDATOR@": f"    annotationProcessor 'net.minecraftforge:eventbus-validator:{cfg['validator']}'" if cfg["validator"] else "",
    }
    for f in ("build.gradle", "settings.gradle", "gradle.properties"):
        with open(os.path.join(tpl, f), encoding="utf-8") as fh:
            t = fh.read()
        for a, b in rep.items():
            t = t.replace(a, b)
        with open(os.path.join(out, f), "w", encoding="utf-8") as fh:
            fh.write(t)
    # mods.toml con los valores reales
    toml = os.path.join(out, "src", "main", "resources", "META-INF", "mods.toml")
    if os.path.exists(toml):
        vals = dict(p)
        vals.update(minecraft_version=mc, forge_version=cfg["forge"], loader_version_range=cfg["loader"],
                    forge_version_range=cfg["loader"], minecraft_version_range=cfg["mc"],
                    mod_version=p.get("mod_version", "1.0.0") + "+" + mc)
        with open(toml, encoding="utf-8") as fh:
            t = fh.read()
        for k, v in vals.items():
            t = t.replace("${" + k + "}", v)
        with open(toml, "w", encoding="utf-8") as fh:
            fh.write(t)
        if "${" in t:
            print("AVISO: quedan valores sin rellenar en mods.toml")
    # cosas del mod para el servidor de prueba
    for f in ("ci_commands.txt", "ci_server.properties"):
        if os.path.exists(os.path.join(base, f)):
            shutil.copy(os.path.join(base, f), os.path.join(out, f))
    print("proyecto listo:", out)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
