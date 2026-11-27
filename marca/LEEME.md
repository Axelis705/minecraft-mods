# Logo de Axelis

La "A con diamante": una A gruesa sin barra (como una Λ) con un diamante donde iría la barra, más el nombre en Poppins
(AXELIS en Bold, STUDIO en Light), en blanco y negro, al estilo de Eufonia Studio. Las letras están convertidas en trazos.

- `axelis_studio_*.svg`: A + AXELIS / STUDIO (dos renglones).
- `axelis_*.svg`: A + AXELIS (un renglón).
- `axelis_A_*.svg`: la A sola.
- `png/`: los mismos en PNG con fondo transparente (2400 px de ancho; la A, 1024) y `foto_de_perfil.png` (1024, fondo negro).

Para rehacerlos: `python3 logo.py <carpeta>` (necesita fontTools y Poppins en /usr/share/fonts) y
`node png.js <carpeta>` (Playwright; antes crear `<carpeta>/png`).
La A en una caja de 100: `M50 4L97 96H73L50 51L27 96H3Z M50 65L59 75L50 85L41 75Z`.

## Portada de X (1500x500)
`portada.py <carpeta>` hace `portada_axelis_studio.svg` y `portada_axelis.svg` (letras al centro, A gigante muy oscura a la
derecha, guías punteadas); los PNG están en `png/`. La esquina de abajo a la izquierda va vacía porque ahí cae la foto de perfil.
