#!/usr/bin/env bash
# Pruebas sin Minecraft: la cuerda que se enreda en los postes (Encadenados de tres).
set -e
cd "$(dirname "$0")/.."
d=$(mktemp -d)
javac -encoding UTF-8 -d "$d" src/main/java/com/coleccionista/prueba/Cuerda.java
java -cp "$d" com.coleccionista.prueba.Cuerda
# El marco girado (celdas, ascensores): de lo local al mundo en los cuatro giros.
javac -encoding UTF-8 -d "$d" src/main/java/com/coleccionista/mapa/Plano.java src/main/java/com/coleccionista/mapa/Marco.java
java -cp "$d" com.coleccionista.mapa.Marco
