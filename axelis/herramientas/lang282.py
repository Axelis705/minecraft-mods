"""Etapa 282: textos nuevos y cambiados (estallido de las armas, confirmar la salida, guardián de la llave, rumbos).
Uso: python3 lang282.py <carpeta lang>   (añade lo que falta y cambia lo indicado en es_es, es_mx y en_us)"""
import json
import os
import sys
from collections import OrderedDict

P = "mundos_de_axelis"
ES, EN = OrderedDict(), OrderedDict()
CES, CEN = OrderedDict(), OrderedDict()


def k(key, es, en):
    ES[key] = es
    EN[key] = en


def c(key, es, en):
    CES[key] = es
    CEN[key] = en


# ---- el estallido de las armas (los arrojadizos pasaron a ser un efecto del golpe cargado)
for tid, es, en in [
    ("bomba_esporas", "✸ Estallido de esporas: a veces, el golpe cargado suelta una nube venenosa",
     "✸ Spore burst: charged hits sometimes release a poison cloud"),
    ("granada_hierro", "✸ Metralla: a veces, el golpe cargado explota y empuja a los de alrededor",
     "✸ Shrapnel: charged hits sometimes explode and push enemies back"),
    ("orbe_escarcha", "✸ Estallido de escarcha: a veces, el golpe cargado congela alrededor",
     "✸ Frost burst: charged hits sometimes freeze everything around"),
    ("bomba_volcanica", "✸ Erupción: a veces, el golpe cargado revienta en fuego y lava",
     "✸ Eruption: charged hits sometimes burst into fire and lava"),
    ("orbe_arcano", "✸ Pulso arcano: a veces, el golpe cargado suelta un pulso de magia",
     "✸ Arcane pulse: charged hits sometimes release a magic pulse"),
]:
    k("tooltip.%s.arma.estallido.%s" % (P, tid), es, en)
c("encargo.%s.arrojadizos" % P, "Provoca %s estallidos con tus armas (golpe cargado)", "Trigger %s weapon bursts (charged hits)")

# ---- confirmar antes de salir del volcán
k("confirmar.%s.salida_volcan.titulo" % P, "¿Salir del volcán?", "Leave the volcano?")
k("confirmar.%s.salida_volcan.texto" % P,
  "La Piedra de regreso te llevará a la entrada del volcán, al pie del camino. Para volver a entrar tendrás que tirarte otra vez al lago del cráter.",
  "The Return Stone will take you to the volcano entrance, at the foot of the path. To come back in you will have to jump into the crater lake again.")
k("gui.%s.confirmar.no" % P, "Cancelar", "Cancel")
k("gui.%s.confirmar.si" % P, "Confirmar", "Confirm")
k("gui.%s.confirmar.espera" % P, "Confirmar (%s)", "Confirm (%s)")

# ---- el guardián de la llave (ya no se fuerza la cerradura con hierro)
k("entity.%s.con_llave" % P, "%s con la llave", "%s with the key")
k("message.%s.llave.lleva" % P, "La llave la lleva %s: %s, a %s bloques", "%s has the key: %s, %s blocks away")
k("message.%s.llave.suelo" % P, "La llave está en el suelo: %s, a %s bloques", "The key is on the ground: %s, %s blocks away")
k("message.%s.llave.cerca" % P, "El guardián con la llave anda por la estructura. ¡Búscalo!", "The key guard is somewhere in this place. Find it!")
k("message.%s.llave.nueva" % P, "Ha llegado otro guardián con la llave (%s): %s, a %s bloques",
  "Another key guard has arrived (%s): %s, %s blocks away")
for d, es, en in [("n", "al norte", "to the north"), ("ne", "al noreste", "to the north-east"), ("e", "al este", "to the east"),
                  ("se", "al sureste", "to the south-east"), ("s", "al sur", "to the south"), ("so", "al suroeste", "to the south-west"),
                  ("o", "al oeste", "to the west"), ("no", "al noroeste", "to the north-west"), ("aqui", "aquí mismo", "right here"),
                  ("arriba", "%s, más arriba", "%s, higher up"), ("abajo", "%s, más abajo", "%s, further down")]:
    k("dir.%s.%s" % (P, d), es, en)
c("npc.%s.ruda.preso.sin_llave" % P,
  "¿Y la llave? ¿Se perdió? Mira, ya viene otro Raizal a vigilarme... y la trae colgando de una raíz. Quítasela.",
  "The key? Lost it? Look, another Raizal is coming to guard me... and it has the key hanging from a root. Take it.")
c("npc.%s.bruno.preso.sin_llave" % P,
  "¿Perdiste la llave? Acaba de llegar otro centinela al fortín y la lleva colgada del cinturón. Ya lo tengo apuntado en el mapa.",
  "Lost the key? Another sentinel just arrived at the fort, with the key on its belt. I've already marked it on my map.")
c("npc.%s.hilda.preso.sin_llave" % P,
  "¿Sin llave? Ha bajado otro fundidor a vigilar la celda y la lleva encima. Dale fuerte: la escoria no aguanta mucho.",
  "No key? Another smelter came down to guard the cell and it carries the key. Hit it hard: slag doesn't last long.")
c("npc.%s.teo.preso.sin_llave" % P,
  "¿Se perdió la llave? Según el capítulo cuatro de «Cerraduras para curiosos», siempre hay una copia. La tiene la armadura que acaba de llegar.",
  "Lost the key? According to chapter four of 'Locks for the Curious', there is always a copy. The armor that just arrived has it.")
c("npc.%s.ignacio.preso.sin_llave" % P,
  "¿Sin llave? Los cultistas tienen copias de todo. Acaba de llegar otro con la llave al cinto. Ve con cuidado.",
  "No key? The cultists have copies of everything. Another one just arrived with the key on its belt. Be careful.")


def patch(path, add, change):
    lines = open(path, encoding="utf-8").read().rstrip().split("\n")
    assert lines[-1] == "}", path
    for i, ln in enumerate(lines):
        s = ln.strip()
        for key, val in change.items():
            if s.startswith(json.dumps(key) + ":"):
                comma = "," if s.endswith(",") else ""
                lines[i] = "  " + json.dumps(key) + ": " + json.dumps(val, ensure_ascii=False) + comma
    body = lines[:-1]
    existing = "\n".join(body)
    new = [(key, val) for key, val in add.items() if json.dumps(key) + ":" not in existing]
    if new:
        if not body[-1].endswith(","):
            body[-1] += ","
        for j, (key, val) in enumerate(new):
            body.append("  " + json.dumps(key) + ": " + json.dumps(val, ensure_ascii=False) + ("," if j < len(new) - 1 else ""))
    open(path, "w", encoding="utf-8").write("\n".join(body + ["}"]) + "\n")
    json.load(open(path, encoding="utf-8"))


def main():
    d = sys.argv[1]
    patch(os.path.join(d, "es_mx.json"), ES, CES)
    patch(os.path.join(d, "es_es.json"), ES, CES)
    patch(os.path.join(d, "en_us.json"), EN, CEN)
    print("lang282:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
