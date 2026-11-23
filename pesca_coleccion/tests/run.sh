#!/usr/bin/env bash
# Pruebas sin Minecraft de la pesca (se corren desde la raíz del repo): la pelea se repite igual con las mismas teclas y la
# dificultad por rareza con jugadores de mentira.
set -e
d=$(mktemp -d)
javac -encoding UTF-8 -nowarn -d "$d" pesca_coleccion/src/main/java/com/pescacoleccion/pelea/PeleaSim.java \
  pesca_coleccion/src/main/java/com/pescacoleccion/pelea/Niveles.java pesca_coleccion/src/main/java/com/pescacoleccion/pelea/Pelea.java \
  pesca_coleccion/tests/PeleaBots.java
java -cp "$d" PeleaBots 100 | tee "$d/out.txt"
grep -q PELEA_REPETIBLE_OK "$d/out.txt"
