# Pool Table (Minecraft Forge 1.20.1 · Forge 47.4.10 · Java 17)

Mesa de billar jugable con bolas que se mueven con física propia (choques, bandas, troneras) y **de 1 a 6 jugadores con 4 modos de juego** (menú en la mesa). `mod_id = pooltable`.

## Cómo se juega
- **Mesa de billar** (objeto): se coloca como un bloque y ocupa **6 x 4 bloques** (6 hacia donde miras y 4 a tu derecha; campo de juego de 5 x 2,5 bloques, necesita ese espacio libre). Receta: 3 alfombras verdes, 3 tablones de roble oscuro, 3 troncos de roble oscuro.
- **Taco de billar**: receta = 1 pepita de hierro + 2 palos (en columna).
- **Tirar:** ponte junto a la mesa con el taco, **mira en la dirección del tiro** (una línea blanca sale de la bola blanca, con la bola fantasma donde chocará; amarillo = hacia dónde saldrá la bola tocada, celeste = por dónde sigue la blanca). **Mantén clic derecho** para cargar la fuerza (barra) y **suelta** para golpear.
- **Menú de la mesa:** clic derecho en la mesa con la **mano libre**. Ahí eliges el modo, te apuntas (**Apuntarme**), y cuando el número de jugadores sirve para ese modo pulsas **Empezar partida** (arma el triángulo según el modo). Hasta 6 jugadores; se turnan en el orden en que se apuntaron. Sin partida empezada la mesa es de práctica libre. Al terminar una partida los jugadores siguen apuntados: puedes empezar la revancha.
- **Modos:**
  - **Reto en solitario** (1 jugador): embocar las 15 bolas en el menor número de tiros; si cae la blanca suma 2 tiros. Se guarda el récord de la mesa.
  - **8 bolas** (2, 4 o 6 jugadores): lisas contra rayadas. Con 4 o 6 se juega **por equipos** (jugadores 1,3,5 contra 2,4,6; turnos alternados). Gana quien emboca la negra al final sin falta.
  - **9 bolas** (2 a 6): solo bolas 1 a 9; se toca siempre primero la más baja; gana quien emboca la 9 sin falta (si cae con falta vuelve a la mesa).
  - **Rotación a 61** (2 a 6): 15 bolas, se toca primero la más baja, cada bola vale su número, falta = -1 punto; gana quien llegue a 61 (o quien más tenga al limpiar la mesa).
- **Troneras:** son 6 hoyos reales (4 esquinas + 2 laterales). La bola rueda al hoyo, cae por él y rueda por una **bandeja** que hay bajo la mesa hasta alinearse con las demás en el lado del triángulo (se ve entre las patas). La blanca que cae vuelve a la salida.
- **Reiniciar la mesa:** botón **Reiniciar mesa** del menú (antes era agachado + clic).
- **Faltas (según el modo):** la blanca cae, no toca ninguna bola, o toca primero la bola equivocada. Pierdes el turno y la blanca vuelve a la salida. Un **panel a la izquierda de la pantalla** (aparece al acercarte a una mesa con jugadores apuntados) muestra modo, jugadores y marcador, las bolas que quedan en la mesa, el turno y los últimos sucesos; ya no hay cartel flotante sobre la mesa.

## Código
- `game/PoolRules.java`: reglas de los 4 modos, sin Minecraft (se prueba sola).
- `game/PoolPhysics.java`: física pura (sin Minecraft), 16 bolas, 12 subpasos por tick, troneras, rastreo de la línea de puntería. Se puede probar sola.
- `block/PoolTableBlock.java` (mesa multibloque 6x4, solo la parte 0,0 tiene bloque-entidad) y `block/PoolTableBE.java` (física en el servidor, reglas, guardado y envío a los clientes).
- `net/PoolSyncPacket.java`: posiciones de las bolas cada tick mientras algo se mueve. `net/PoolActionPacket.java`: acciones del menú (cliente -> servidor, el servidor lo comprueba todo).
- `client/PoolTableRenderer.java` (bolas esféricas que giran al rodar, caída por la tronera y bandeja, línea de puntería, cartel), `client/BallMesh.java`, `client/PoolHud.java` (barra de fuerza), `client/PoolMatchHud.java` (panel de la partida), `client/PoolMenuScreen.java` (menú de la mesa).
- Recursos (generados con un script): 24 modelos de la mesa, uno por parte (`models/block/pool_table_U_V.json`; patas, marco, bandas, diamantes, hoyos, bandeja), 16 texturas de bolas (`textures/entity/ball_N.png`), taco 3D de 15 piezas.
- Si tenías puesta la mesa de la versión 1 (4x2), rómpela y pon una nueva: ya no es del mismo tamaño. Las mesas de la versión 2 siguen sirviendo (se pueden reiniciar desde el menú).

Ajustes de sensación en `PoolPhysics` (medidas L/W/R, tamaño de las troneras, fricción, rebote, velocidad máxima), `PoolCueItem` (carga máxima y alcance) y `PoolTableRenderer.dropPos` (tiempos de la caída).
No probado dentro del juego (solo compilado y simulado).
