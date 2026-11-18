# Etapa 250 — El Volcán: estructura enorme con la dungeon del jefe de fuego (fases 3 y 4)

Respuestas de Axelis (18-nov): si caes del puente, **foso de magma** con salamandras y escalera para reintentar; volcán **enorme**; por fuera **humo, ríos de lava falsa, erupciones de adorno y camino en espiral**.

## Cómo se usa
`/mundos estructura volcan` lo construye 16 bloques delante de quien escribe, mirando hacia él (unos 280 000 bloques: tarda unos segundos). `/mundos estructura deshacer` lo quita (bloques y enemigos).

## Por fuera (`DesignVolcan`)
- Cono de **69 bloques de alto y 108 de ancho**, con laderas cóncavas, ruido y barrancos. Franjas de color por altura (ceniza clara abajo, roca oscura y manchas de magma arriba).
- **Cráter**: borde dentado, cuenco que se baja andando y en el centro un **lago de lava falsa**.
- **Humo**: 6 fogatas de señal alrededor del lago.
- **Ríos de lava falsa**: 5 ríos que bajan por las laderas. Brillan, no queman ni se derraman.
- **Camino en espiral**: de la entrada sale una calzada hasta el pie del camino. El camino da una vuelta entera al volcán, con escalones y farolas, hasta el borde del cráter.
- **Erupciones de adorno**: el bloque central del lago (`nucleo_volcan`, se ve igual que la lava falsa) actúa cada 1–2 minutos si hay alguien a menos de 140 bloques:
  - retumbar con sonido propio y temblor de pantalla;
  - columna de fuego y humo;
  - 7–11 bolas de fuego que caen por las laderas.
  
  No hace daño ni rompe nada.
- Llanura de ceniza alrededor, rocas de basalto sueltas, arbustos secos, braseros en la entrada.

## La dungeon de dentro
1. **Te tiras al lago** → lava falsa: caída lenta 7 s y sin daño ("¡La lava era falsa!") → pozo de 30 bloques.
2. **Cámara del cráter** (aterrizaje): sala abovedada con anillos de piedra negra dorada, cascadas de lava falsa, cadenas y faroles.
3. **Puente de basalto agrietado** (`basalto_agrietado`):
   - Al pisarlo cruje (grietas que brillan, chispas); al segundo se cae y vuelve a formarse a los 10 s.
   - El crujido se contagia a los vecinos: no conviene quedarse quieto.
   - Debajo hay un **foso de magma** (3 bloques, sin daño de caída) con 3 salamandras y una escalera para volver a la repisa.
4. **Sala de las erupciones**: 4 filas de **respiraderos** (`respiradero_volcan`).
   - Cada uno pasa 4–7 s tranquilo, avisa 1,5 s (humo, chispas, retumbar) y escupe una columna de fuego de 5 bloques que quema y lanza hacia arriba.
   - Cada uno tiene su propio ritmo y solo funcionan si hay alguien cerca.
   - Hay columnas para cubrirse, 3 diablillos y una salamandra.
5. **Altar de los cultistas** (nicho al oeste): 2 cultistas, altar con obsidiana llorona y velas.
6. Bajada (con un cultista en el camino) al **Santuario del corazón**: altar escalonado con la **Piedra de Portal: Volcán**, un corazón de magma de piedra colgado de cadenas, fogatas de almas, cascadas de lava falsa.
7. **Túnel de salida**: del santuario al frente del volcán. Termina en un escalón de 2: se sale, pero no se puede entrar por ahí.

Iluminado con hongos de luz, faroles y magma (no deberían aparecer monstruos del juego dentro). Los enemigos se ponen una sola vez.

## Sonidos propios del jefe (`gen/gen_sonidos_volcan.py`)
`corazon_latido` (2 variantes), `corazon_rugido`, `cadenas_rompen`, `corazon_cae`, `corazon_muerte`, `volcan_retumbo`, sintetizados (sin muestras externas), con subtítulos en ES/MX/EN. Sustituyen a los del warden y la explosión genérica en el Corazón de Magma, su intro y las erupciones.

## Otros cambios
- La ficha del jefe 4, pestaña «La dungeon», explica la arena y cómo llegar por el Volcán (antes mostraba el texto de las dungeons del bosque y la forja).
- `Linker`: la lava falsa cuenta como paso (se cae por ella desde cualquier altura), para revisar que el volcán está bien conectado.
- Fallo encontrado por la prueba del servidor y corregido: al cambiar de etapa, el bloque se volvía a "poner" y programaba un tick antes que el suyo (el juego ignora el segundo). El puente se rehacía en 1 s en vez de 10, y los respiraderos se habrían quedado a medias. Ahora solo programa al colocarlo.
- Los matojos secos de la ceniza van sobre tierra gruesa (sobre grava o toba el juego los quitaba).

## Comprobado
- Plano revisado fuera del juego: 0 trampas, entrada transitable, 1 «sala» sin acceso tolerada (lo alto del corazón de piedra colgado, adorno), 11 enemigos con sitio válido, nada sin apoyo. Fotos con el visor de estructuras.
- CI de servidor: se construye, están la piedra, el núcleo, el lago, el puente, los respiraderos y los tres tipos de enemigo; el basalto se derrumba; deshacer lo quita todo (enemigos incluidos). Conectividad de las 5 estructuras en verde.
- Prueba con el cliente real: ver el resultado en la sección de pruebas al final.

## Pendiente
- Generación natural de las estructuras (para todas): sigue siendo con comando.
