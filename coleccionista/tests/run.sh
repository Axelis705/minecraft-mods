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
# Los mapas hechos con planos de texto: que todo se pueda caminar desde la entrada (cuartos y escondites), sin Minecraft.
javac -encoding UTF-8 -nowarn -d "$d" src/main/java/com/coleccionista/mapa/*.java herramientas/mapas/*.java
java -Djava.awt.headless=true -cp "$d:src/main/resources" com.coleccionista.mapa.RevisarMapas "$d/vistas" mansion --estricto
# Los caminos hacia adelante: de donde terminan todos en cada sala, a su salida, por el pasillo, hasta el pasillo de servicio de la siguiente.
java -Djava.awt.headless=true -cp "$d:src/main/resources" com.coleccionista.mapa.RevisarMapas "$d/vistas" caminos --estricto
