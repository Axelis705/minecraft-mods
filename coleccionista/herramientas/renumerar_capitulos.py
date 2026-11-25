"""Corre un lugar los capítulos viejos (Compartir, A ciegas, El sacrificio, Las puertas, Las copas) para meter uno nuevo antes.

Uso: python3 herramientas/renumerar_capitulos.py <desde>   (p. ej. 6: el c6 pasa a c7, el c7 a c8, ...)
Cambia: las claves coleccionista.linea.cN.* (y "Capítulo N" / "Chapter N" del título) en los tres idiomas, la tabla de LineaScreen,
el número romano de la presentación de esas pruebas y lo que esperan ci_commands.txt / real_commands.txt.
"""
import json
import os
import re
import sys

MOD = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(MOD, "src/main/resources/assets/coleccionista/lang")
JAVA = os.path.join(MOD, "src/main/java/com/coleccionista")
ROMANOS = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV"]
# las pruebas viejas que se corren (archivo, prefijo de la presentación)
VIEJAS = [("Compartir.java", "com"), ("ACiegas.java", "cie"), ("Sacrificio.java", "sac"), ("LasPuertas.java", "pue"),
          ("LasCopas.java", "cop")]


def main(desde):
    # 1) lang: cN -> cN+1 para N >= desde (de mayor a menor)
    for archivo in ("es_mx.json", "es_es.json", "en_us.json"):
        p = os.path.join(LANG, archivo)
        d = json.load(open(p, encoding="utf-8"))
        nuevo = {}
        for k, v in d.items():
            m = re.match(r"coleccionista\.linea\.c(\d+)\.(.*)$", k)
            if m and int(m.group(1)) >= desde:
                n = int(m.group(1)) + 1
                k = f"coleccionista.linea.c{n}.{m.group(2)}"
                v = re.sub(r"^(Capítulo|Chapter) \d+", lambda x: f"{x.group(1)} {n}", v)
            nuevo[k] = v
        json.dump(nuevo, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
        open(p, "a", encoding="utf-8").write("\n")
    # 2) LineaScreen: {"cN", zona, prefijo}
    p = os.path.join(JAVA, "client/LineaScreen.java")
    s = open(p, encoding="utf-8").read()
    s = re.sub(r'\{"c(\d+)", "(\w+)", "(\w+)"\}',
               lambda m: f'{{"c{int(m.group(1)) + 1 if int(m.group(1)) >= desde else int(m.group(1))}", "{m.group(2)}", "{m.group(3)}"}}', s)
    open(p, "w", encoding="utf-8").write(s)
    # 3) el número romano de la presentación de las pruebas viejas, y lo que espera la prueba del servidor
    cambios = {}
    for archivo, pre in VIEJAS:
        p = os.path.join(JAVA, "prueba", archivo)
        s = open(p, encoding="utf-8").read()
        m = re.search(r'Escena\.presentacion\(s, "([IVX]+)", MOD \+ "\.' + pre + r'\.titulo"', s)
        if not m:
            raise SystemExit("no encontré la presentación de " + archivo)
        viejo = m.group(1)
        n = ROMANOS.index(viejo)
        if n < desde:
            continue
        cambios[viejo] = ROMANOS[n + 1]
        s = s[:m.start(1)] + ROMANOS[n + 1] + s[m.end(1):]
        open(p, "w", encoding="utf-8").write(s)
    for archivo in ("ci_commands.txt", "real_commands.txt"):
        p = os.path.join(MOD, archivo)
        lineas = open(p, encoding="utf-8").read().split("\n")
        for i, l in enumerate(lineas):
            m = re.match(r"(#expect COLECCIONISTA_PRESENTACION )([IVX]+)$", l)
            if m and m.group(2) in cambios:
                lineas[i] = m.group(1) + cambios[m.group(2)]
        open(p, "w", encoding="utf-8").write("\n".join(lineas))
    print("corridos desde c%d; presentaciones: %s" % (desde, cambios))


if __name__ == "__main__":
    main(int(sys.argv[1]))
