# Etapa 251 — Escudo de lava de los cultistas y 9 cartas nuevas

Pedido de Axelis (18-nov): «Sigue agregando y puliendo».

## Escudo de lava (cultistas del volcán)
Estaba en el diseño del volcán («escudos de lava a los demás») y faltaba.
- Cada 15–20 s, si hay un aliado del volcán peleando cerca (salamandra, diablillo, larva u otro cultista; a 12 bloques y a la vista), el cultista levanta el bastón 1 s y le tiende un **hilo de magma**. Elige primero al más herido.
- El aliado queda dentro de una **burbuja de lava** (esfera naranja, runa que gira a sus pies, gotas de magma).
  - Aguanta **4 corazones de daño** o **12 s**, lo que pase antes; parpadea cuando se va a acabar.
  - Cada golpe que para hace brillar la burbuja y salta una chispa hacia quien pegó.
  - Al romperse estalla en chispas (sonido de vidrio y lava). Si se acaba el tiempo, se apaga con humo.
- El cultista no se protege a sí mismo (dos cultistas sí se protegen entre ellos).
- La ficha del jefe 4 (pestaña «La dungeon») ahora aconseja ir primero a por los cultistas.
- Código: `entity/EscudoLava.java` (todo en el servidor, sin guardar: dura segundos) y `CultistaVolcan` (nuevo hechizo).

## Cartas nuevas (41 en total)
- **Jefes:** Corazón de Magma (Legendaria) y Archimago (Legendaria).
- **Criaturas del volcán:** Salamandra de Lava, Diablillo, Larva de Magma (Comunes) y Cultista del Volcán (Rara).
- **Criaturas de la torre:** Esbirro y Libro Embrujado (Comunes) y Armadura Encantada (Rara).
- Descripciones escritas por mí. Iconos nuevos de 12×12 para el diablillo y la salamandra.
- **Arte provisional = retratos del juego de verdad.** La prueba con el cliente monta tres «estudios» cerrados (volcán, forja y torre), pone cada criatura quieta y de frente, quita la interfaz y saca la foto. El Corazón de Magma se fotografía de cerca en su arena, encadenado.
  - Con eso también tienen ilustración **Ascua** y **Fundidor**, que estaban con el dibujo sencillo.
  - Las fotos enteras están en `cartas/herramientas/arte/referencias/carta_<id>.png`: sirven de captura de referencia para Flow.
- `PROMPTS_FLOW.md`: prompts para las 9 cartas nuevas (y Ascua y Fundidor ya tienen su captura).

## Otros
- CI de estructuras: fallaba a veces «sin objetos» porque el mundo de prueba generaba aldeas y la estructura pisaba un cofre (pan, papas y manzanas por el suelo). Ahora el mundo de prueba no tiene aldeas.

## Comprobado
(se completa al terminar)
