#!/usr/bin/env python3
"""Mete las voces grabadas del Coleccionista al mod.

Uso: python3 herramientas/voces.py <carpeta_con_grabaciones> [--sin-ajustar]

- Corta solo el silencio de antes y después de la voz. Acepta también videos (MP4, MKV, MOV: se toma el audio).
- Cada archivo se llama como en el guion (docs/guion-voces.md): <guion>_<clave>.<mp3|wav|ogg|m4a|...>, por ejemplo ciegas_hola.mp3.
- Los convierte a OGG mono 44.1 kHz (lo que usa Minecraft), con el volumen parejo, en
  src/main/resources/assets/coleccionista/sounds/voz/<guion>_<clave>.ogg
- Escribe sounds.json con un evento por línea (coleccionista:voz.<guion>.<clave>), solo para las que existen.
- Ajusta en Pantallas.java el tiempo de cada frase a lo que dura su audio (+0.6 s), salvo con --sin-ajustar.
- Avisa de archivos con nombre desconocido y de líneas del guion que faltan.
"""
import json
import math
import os
import re
import subprocess
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(AQUI)
RES = os.path.join(MOD, "src/main/resources/assets/coleccionista")
PANTALLAS = os.path.join(MOD, "src/main/java/com/coleccionista/evento/Pantallas.java")


def guiones():
    src = open(PANTALLAS, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r"public static List<Frase> (\w+)\(\) \{(.*?)return l;", src, re.S):
        out[m.group(1)] = [k for k, _ in re.findall(r'new Frase\("([a-z_]+)", (\d+)', m.group(2))]
    return out


def recorte(f):
    """Dónde empieza y termina la voz (segundos): corta el silencio o el ruido de antes y después (con un poquito de aire)."""
    try:
        import numpy as np
        r = subprocess.run(["ffmpeg", "-v", "error", "-i", f, "-ac", "1", "-ar", "16000", "-f", "s16le", "-"], capture_output=True)
        a = np.frombuffer(r.stdout, dtype=np.int16).astype(float)
        w = 320  # 20 ms
        rms = np.array([np.sqrt(np.mean(a[i:i + w] ** 2)) for i in range(0, len(a) - w, w)])
        if len(rms) == 0 or rms.max() <= 0:
            return None
        ruido = np.percentile(rms, 20)
        umbral = max(ruido * 3, rms.max() * 0.05)
        voz = np.where(rms > umbral)[0]
        if len(voz) == 0:
            return None
        ini = max(0.0, voz[0] * 0.02 - 0.25)
        fin = min(len(a) / 16000, (voz[-1] + 1) * 0.02 + 0.35)
        return ini, fin
    except Exception:
        return None


def duracion(f):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f], capture_output=True, text=True)
    return float(r.stdout.strip() or 0)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    carpeta = sys.argv[1]
    ajustar = "--sin-ajustar" not in sys.argv
    g = guiones()
    validas = {f"{gid}_{k}": (gid, k) for gid, ks in g.items() for k in ks}
    destino = os.path.join(RES, "sounds/voz")
    os.makedirs(destino, exist_ok=True)
    hechas, desconocidas = {}, []
    for nombre in sorted(os.listdir(carpeta)):
        base, ext = os.path.splitext(nombre)
        if ext.lower() not in (".mp3", ".wav", ".ogg", ".m4a", ".flac", ".aac", ".opus", ".webm", ".mp4", ".mkv", ".mov"):
            continue
        if base not in validas:
            desconocidas.append(nombre)
            continue
        sal = os.path.join(destino, base + ".ogg")
        corte = recorte(os.path.join(carpeta, nombre))
        rango = ["-ss", f"{corte[0]:.2f}", "-to", f"{corte[1]:.2f}"] if corte else []
        subprocess.run(["ffmpeg", "-y", "-v", "error", *rango, "-i", os.path.join(carpeta, nombre), "-vn", "-ac", "1", "-ar", "44100",
                        "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", "-c:a", "libvorbis", "-q:a", "5", sal], check=True)
        hechas[base] = duracion(sal)
        print(f"ok  {base}.ogg  {hechas[base]:.1f} s")
    # sounds.json: solo las que existen (si no, Minecraft avisa de archivos que faltan)
    sj = os.path.join(RES, "sounds.json")
    datos = json.load(open(sj, encoding="utf-8")) if os.path.exists(sj) else {}
    datos = {k: v for k, v in datos.items() if not k.startswith("voz.")}
    for f in sorted(os.listdir(destino)):
        if f.endswith(".ogg") and f[:-4] in validas:
            gid, k = validas[f[:-4]]
            datos[f"voz.{gid}.{k}"] = {"category": "voice", "subtitle": f"coleccionista.voz.{gid}.{k}",
                                       "sounds": [{"name": f"coleccionista:voz/{f[:-4]}", "stream": True}]}
    json.dump(datos, open(sj, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    # los tiempos de cada frase según su audio
    if ajustar and hechas:
        lang = json.load(open(os.path.join(RES, "lang/es_mx.json"), encoding="utf-8"))
        src = open(PANTALLAS, encoding="utf-8").read()
        partes, ultimo = [], 0
        for m in re.finditer(r"public static List<Frase> (\w+)\(\) \{(.*?)return l;", src, re.S):
            gid = m.group(1)

            def rep(mm):
                base = f"{gid}_{mm.group(1)}"
                if base not in hechas:
                    return mm.group(0)
                # lo que dura el audio, pero nunca menos de lo que se tarda en leer el subtítulo (≈12 letras por segundo)
                texto = lang.get(f"coleccionista.voz.{gid}.{mm.group(1)}", "")
                t = max(40, math.ceil((hechas[base] + 0.6) * 20), math.ceil(len(texto) * 20 / 12))
                # si ya hay risa grabada, ya no suena el efecto del juego encima
                return f'new Frase("{mm.group(1)}", {t}, null, 1F));'

            partes.append(src[ultimo:m.start(2)])
            partes.append(re.sub(r'new Frase\("([a-z_]+)", (\d+), [^;]*?\)\);', rep, m.group(2)))
            ultimo = m.end(2)
        partes.append(src[ultimo:])
        open(PANTALLAS, "w", encoding="utf-8").write("".join(partes))
        print("tiempos ajustados en Pantallas.java")
    faltan = sorted(set(validas) - set(f[:-4] for f in os.listdir(destino) if f.endswith(".ogg")))
    print(f"\nlistas: {len(validas) - len(faltan)} de {len(validas)}")
    if faltan:
        print("faltan:", ", ".join(faltan))
    if desconocidas:
        print("nombres que no están en el guion:", ", ".join(desconocidas))


if __name__ == "__main__":
    main()
