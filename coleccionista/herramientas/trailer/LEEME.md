# El tráiler y el teaser de El Coleccionista

1. Tomas del juego: correr el workflow **Tráiler del Coleccionista (rodaje)** (`.github/workflows/trailer.yml`); deja los videos en la rama
   `trailer-coleccionista`. Copiarlos a `tomas/` (aquí junto).
2. Tomas del Coleccionista en 3D: `python3 render3d.py out3d presenta:6.5 mira:5.2 risa:2.5 ojos:6` (y lo mismo con `--vertical`).
3. Letreros: `python3 cartas.py` (PNG transparentes en `cartas/`, horizontales y `_v` verticales).
4. Armar: `bash entregar.sh` (tráiler) y `bash entregar_teaser.sh` (teaser). Salen en `out/entrega/`: horizontal, vertical y copias de
   menos de 10 MB para Discord.

El guion (qué toma va en qué segundo, los letreros y los sonidos) está arriba de `montar.py` y de `teaser.py`. `VOZ` sube o baja la voz.

## Cuenta regresiva (7, 5, 3 y 1 día)
`bash entregar_cuenta.sh`: cuatro videos de unos 12 s (tres tomas de un capítulo con una frase del Coleccionista y el letrero
"FALTAN N DÍAS" / "MAÑANA"). Salen en vertical (alta calidad) y en horizontal para Discord. El guion de cada uno está en `VIDEOS` de `cuenta.py`.

## Portadas
`python3 portadas.py [trailer teaser faltan7 faltan5 faltan3 manana]`: una por video, horizontal 1280x720 (YouTube) y vertical 1080x1920,
en `out/portadas/`. El fondo es un cuadro de la toma y el Coleccionista sale en 3D sin fondo (toma `retrato` de `escena3d.js`).
