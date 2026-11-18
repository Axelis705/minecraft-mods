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
- Prueba del servidor (`/mundos prueba escudo`): el escudo se pone a una salamandra. Primer golpe de 5: vida 24 → 24 y al escudo le quedan 3. Segundo golpe de 5: el escudo se rompe y pasan 2 (vida 22). CI verde en todos los módulos.
- Prueba con el cliente real:
  - un cultista de verdad pone el escudo a un aliado durante la pelea (foto: el diablillo dentro de la burbuja);
  - 11 retratos sin interfaz, uno por carta;
  - el juego carga las 41 cartas;
  - fotos del álbum con las páginas de jefes y criaturas.

  40 comprobaciones, 0 fallos, sin errores de texturas ni modelos.
- Hizo falta un arreglo: al principio el escudo esperaba a que terminara la espera de la columna de fuego y tardaba 8–10 s. Ahora tiene su propia espera: el primero llega a los 3–4 s de empezar la pelea.

## Pendiente
- Arte final de Flow para las 9 cartas nuevas, y Ascua y Fundidor (opcional: los retratos ya sirven).
- Generación natural de las estructuras (sigue con comando).
