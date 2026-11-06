#!/usr/bin/env python3
"""Arma los sonidos "de verdad" del show (aplausos, ovacion, campanitas, fanfarria, golpes...) a partir del kit gratuito
SFX Kit de wyzowl (licencia CC0, dominio publico): https://github.com/wyzowl/sfx-kit

Uso:  python3 sonidos_cc0.py RUTA_AL_KIT/sounds
Los .ogg resultantes reemplazan a los sintetizados con el mismo nombre en src/main/resources/assets/show_de_axelis/sounds.
Los que no se tocan aqui (boo, laugh, crickets, airhorn, trombone, rimshot, whistle_up/down, buzzer, drumroll) siguen siendo los de gen_sonidos.py.
"""
import os
import subprocess
import sys

KIT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/snd/wy/sounds"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/show_de_axelis/sounds")


def w(rel):
    p = os.path.join(KIT, rel + ".wav")
    if not os.path.exists(p):
        sys.exit("falta " + p)
    return p


def build(name, inputs, graph, last="out"):
    cmd = ["ffmpeg", "-y", "-v", "error"]
    for i in inputs:
        cmd += ["-i", w(i)]
    cmd += ["-filter_complex", graph, "-map", "[" + last + "]", "-ac", "2", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5", os.path.join(OUT, name + ".ogg")]
    subprocess.run(cmd, check=True)
    print("ok", name)


def one(name, src, vol=1.0):
    build(name, [src], "[0:a]volume=%s,alimiter=limit=0.95[out]" % vol)


A_SMALL = "celebration/celebration-applause-small"
A_BIG = "celebration/celebration-applause-big"

# aplauso normal: el aplauso chico repetido con empalme suave, que se apaga al final (~5 s)
build("applause", [A_SMALL, A_SMALL],
      "[0:a][1:a]acrossfade=d=0.8,afade=t=out:st=4.0:d=1.3,volume=1.0,alimiter=limit=0.95[out]")
# ovacion: el aplauso grande repetido + el chico encima para que suene a mas gente (~8 s)
build("applause_big", [A_BIG, A_BIG, A_SMALL, A_SMALL],
      "[0:a][1:a]acrossfade=d=1.0[a];[2:a][3:a]acrossfade=d=0.8,adelay=500|500,volume=0.55[b];"
      "[a][b]amix=inputs=2:normalize=0,afade=t=out:st=6.4:d=1.9,alimiter=limit=0.95[out]")
# grito de festejo: aplauso + cuetes + confeti
build("cheer", [A_BIG, "celebration/celebration-popper", "celebration/celebration-fireworks"],
      "[1:a]adelay=150|150[p];[2:a]volume=0.45[f];[0:a][p][f]amix=inputs=3:normalize=0,afade=t=out:st=3.4:d=1.2,alimiter=limit=0.95[out]")
one("correct", "notifications/notification-success-ding-ding", 1.0)
one("ding", "notifications/notification-success-chime", 1.0)
one("reveal", "notifications/notification-success-bubble", 1.0)
# fanfarria: golpe de tambor + acorde y un confeti justo en el golpe
build("fanfare", ["logo-stings/sting-bold-long", "celebration/celebration-popper"],
      "[1:a]adelay=1040|1040,volume=0.8[p];[0:a][p]amix=inputs=2:normalize=0,alimiter=limit=0.95[out]")
# intro del show: subida de tension, golpe brillante justo al llegar, y aplausos
build("intro", ["risers-and-fallers/riser-2s-strong", "logo-stings/sting-bright-long", A_BIG],
      "[1:a]adelay=1060|1060[s];[2:a]adelay=2000|2000,volume=0.6,afade=t=out:st=4.2:d=1.4[c];"
      "[0:a][s][c]amix=inputs=3:normalize=0,alimiter=limit=0.95[out]")
one("round_start", "logo-stings/sting-playful-short", 1.0)
# la X: doble zumbido grave + golpe
build("strike", ["digital/digital-denied", "impacts/impact-heavy-hard"],
      "[0:a]volume=1.0[a];[1:a]volume=0.8[b];[a][b]amix=inputs=2:normalize=0,alimiter=limit=0.95[out]")
one("tick", "interface/click-tap-soft", 1.0)
one("time_up", "time/time-timer", 1.0)
one("pop", "pops/pop-medium-in", 1.0)
one("whoosh", "whooshes/whoosh-medium-medium", 1.0)
one("vote", "interface/click-toggle-on-medium", 1.0)
one("glitch", "digital/digital-glitch-clear", 1.0)
one("blip", "digital/digital-blip-clear", 1.0)
one("suspense", "risers-and-fallers/riser-4s-strong", 1.0)

# nivelar el volumen de los sonidos largos para que se escuchen parejos con el resto del juego
for nm, target in [("applause", -17), ("applause_big", -16), ("cheer", -16), ("intro", -17), ("fanfare", -17), ("correct", -19), ("ding", -19),
                   ("reveal", -19), ("round_start", -18), ("strike", -17), ("time_up", -19), ("suspense", -19)]:
    src = os.path.join(OUT, nm + ".ogg")
    tmp = os.path.join(OUT, nm + "_tmp.ogg")
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", src, "-af", "loudnorm=I=%d:TP=-1.5:LRA=11" % target, "-ar", "44100", "-ac", "2",
                    "-c:a", "libvorbis", "-q:a", "5", tmp], check=True)
    os.replace(tmp, src)
    print("nivelado", nm)
