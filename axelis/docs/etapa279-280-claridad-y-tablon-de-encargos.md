# Etapas 279 y 280: el botín se entiende mejor y el Tablón de Encargos

Axelis pidió otra vez «sigue mejorando». Con tantos sistemas nuevos (gemas, mímicos, élites, altares…), pensé en dos cosas:

- que el jugador entienda y no pierda su botín (etapa 279);
- que cada día tenga un motivo para salir a explorar (etapa 280).

## Etapa 279: claridad del botín

- **Protección contra la mala suerte** (como en Diablo):
  - El juego cuenta los cofres de santuario que abres sin «pieza estrella» (fragmento de corazón, llave, gema vacía, manzana encantada o tótem).
  - Si abres 7 seguidos sin ninguna, el séptimo trae una segura: una gema vacía de su tier o 2 fragmentos de llave.
  - La cuenta vuelve a 0 cada vez que sale una pieza estrella.
  - Se guarda en la parte de los datos del jugador que sobrevive a la muerte.
- **El botín bueno brilla en el suelo**: las piezas estrella y las gemas que caen al suelo llevan un contorno brillante que se ve a través de las paredes. Así no se pierden entre la hierba, la lava o en medio de un combate.
- **Guía de gemas**:
  - En la Mesa de Gemas hay un botón «?» arriba a la derecha.
  - Abre una guía con las 8 gemas y lo que hace cada una en un arma y en la armadura. Cualquier clic la cierra.
  - Las gemas y las esencias muestran en su descripción los dos efectos (arma y armadura), así puedes elegir antes de infundir.

## Etapa 280: el Tablón de Encargos

Viene de los «susurros» del Árbol de Diablo 4 y de los tablones de misiones de otros juegos.

- **Cómo se consigue**: en la Mesa del Artífice, con 6 tablones de abeto, 4 papeles, 1 tinta y 30 de Esencia.
- **Cómo es**: un tablero de madera con notas clavadas con chinchetas rojas, un mapa y un tejadillo.
- **Cómo funciona**:
  - Cada día del juego (al amanecer), cada jugador tiene 3 encargos distintos. Ese día salen siempre los mismos para ese jugador, aunque abras el tablón varias veces.
  - Avanzan solos mientras juegas, y al cumplir uno sale un aviso.
  - Se recogen en el Tablón con el botón «Recoger».

| Encargo | Cuántos |
|---|---|
| Vence enemigos de estructura | 15–25 |
| Vence enemigos de élite | 2–3 |
| Abre cofres de santuario | 4–6 |
| Vence a un mímico | 1 |
| Vence un Altar Maldito | 1 |
| Lanza arrojadizos | 4–6 |
| Infunde o combina una gema | 1 |
| Forja una llave en el Gran Yunque | 1 |

- **Recompensa de cada encargo**:
  - Esencia: 40 + 20 por cada tier de recompensa.
  - Algo según el encargo: runas de esencia, esencias de gema, fragmentos de llave, arrojadizos o una gema vacía.
  - El tier de recompensa es el siguiente a tu último jefe vencido (del 1 al 5).
- **Gran recompensa** (al recoger los 3):
  - un fragmento de corazón o una gema vacía;
  - 2 runas;
  - 100 de Esencia por cada tier de recompensa.
- **Pantalla**: cada nota muestra el encargo, una barra de avance, lo que da y su botón. Abajo están la gran recompensa, el tier de las recompensas y el aviso de que mañana salen otros 3.

## Pruebas

| Prueba | Qué comprueba |
|---|---|
| `mundos prueba suerte` | la protección salta justo en el séptimo cofre; una pieza estrella reinicia la cuenta; un fragmento de corazón en el suelo brilla y la tierra no |
| `mundos prueba encargos` | 3 encargos distintos y estables; cumplir y recoger los 3; no se recoge dos veces; la gran recompensa solo con los 3; el día siguiente trae encargos nuevos; matar enemigos, élites y mímicos avanza los encargos solo |
| Prueba con el cliente | foto de la guía de gemas; el Tablón en el mundo; su pantalla; recoger un encargo cumplido da Esencia |

**Resultado final de la prueba con el cliente**: 231 comprobaciones y 0 fallos.

**Arreglo de la pantalla**: en la primera foto, el título se pisaba con el tier y el aviso de abajo con la gran recompensa. Ahora el tier y el aviso van juntos en el pie, más pequeños.

**Instalado**: el jar se puso en la instancia «test» de Axelis el 21 de noviembre, con el juego cerrado (comprobado por la fecha de latest.log).

## Ideas para seguir (para decidir con Axelis)

- Encargos semanales más difíciles, con recompensas mayores.
- Que el Tablón aparezca en la casa del jugador (la Semilla de casa).
- Contenido de los tiers 6 a 8.
