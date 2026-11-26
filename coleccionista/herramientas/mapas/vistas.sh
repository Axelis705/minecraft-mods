#!/usr/bin/env bash
# Revisa los mapas sin Minecraft y saca sus vistas (desde arriba y en 3D). Uso: bash herramientas/mapas/vistas.sh <carpeta> [mansion] [--estricto]
set -e
cd "$(dirname "$0")/../.."
d=$(mktemp -d)
javac -encoding UTF-8 -nowarn -d "$d" src/main/java/com/coleccionista/mapa/*.java herramientas/mapas/*.java
java -Djava.awt.headless=true -cp "$d:src/main/resources" com.coleccionista.mapa.RevisarMapas "$@"
