"""Etapa 288: textos de la Dríade del Roble (mini-jefa del Santuario del Bosque) y de Ruda, que ahora habla de ella.
Uso: python3 lang288.py <carpeta lang>"""
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


m = "message.%s." % P
k("entity.%s.driade_roble" % P, "Dríade del Roble", "Oak Dryad")
k("item.%s.driade_roble_spawn_egg" % P, "Huevo generador de Dríade del Roble", "Oak Dryad Spawn Egg")
k(m + "driade.despierta", "¡La Dríade del Roble despierta entre las raíces!", "The Oak Dryad awakens among the roots!")
k(m + "driade.florece", "¡La Dríade florece! Brotan aliados del suelo.", "The Dryad blooms! Allies sprout from the ground.")
k(m + "driade.vencida", "La Dríade se deshace en pétalos... y algo cae entre las raíces.", "The Dryad falls apart into petals... and something drops among the roots.")
# cambiados: la llave del santuario ya no la lleva el Raizal
C = "CHANGE"
CES["tooltip.%s.llave_celda" % P] = "La lleva quien vigila la celda de un preso."
CEN["tooltip.%s.llave_celda" % P] = "Carried by whoever guards a prisoner's cell."
CES["npc.%s.ruda.preso.2" % P] = ("La llave la tiene la Dríade del Roble, el espíritu del santuario. Antes cuidaba las flores... ahora duerme entre "
                                  "raíces y no deja salir a nadie. Cuando despierte, aléjate de sus flores rosas.")
CEN["npc.%s.ruda.preso.2" % P] = ("The Oak Dryad has the key, the spirit of this sanctuary. She used to tend the flowers... now she sleeps among roots "
                                  "and lets no one leave. When she wakes, stay away from her pink flowers.")
CES["npc.%s.ruda.preso.sin_llave" % P] = "¿Y la llave? ¿Se perdió? Escucha... la Dríade vuelve a brotar entre las raíces, y la trae consigo. Quítasela."
CEN["npc.%s.ruda.preso.sin_llave" % P] = "The key? Lost it? Listen... the Dryad is sprouting again among the roots, and she has it. Take it from her."
CES["lore.%s.bosque_2.title" % P] = "La herbolaria y la Dríade"
CEN["lore.%s.bosque_2.title" % P] = "The Herbalist and the Dryad"
CES["lore.%s.bosque_2.p2" % P] = ("Pero la Dríade del Roble, el espíritu del santuario al que la corrupción volvió amargo, decidió que quien conoce tan "
                                  "bien las plantas no debe irse nunca. La encerró tras el musgo y se durmió entre las raíces, con la llave.")
CEN["lore.%s.bosque_2.p2" % P] = ("But the Oak Dryad, the sanctuary's spirit soured by the corruption, decided that someone who knows plants so well "
                                  "must never leave. She locked her behind the moss and fell asleep among the roots, with the key.")


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
    print("lang288:", len(ES), "nuevas,", len(CES), "cambiadas")


if __name__ == "__main__":
    main()
