# Etapas 277 y 278: gemas en la armadura y el Altar Maldito

Axelis dijo «sigue mejorando», sin responder si quería gemas en la armadura. Entendí que sí, porque es algo que se suma: no
cambia las armas (que se van a rehacer más adelante) y las gemas en armas siguen igual.

## Etapa 277: gemas en la armadura

Viene de Apotheosis: la misma gema hace otra cosa según dónde la pongas.

- **Cómo se pone**: en la Mesa de Gemas, en el mismo hueco donde va el arma, puedes poner un casco, un peto, unas grebas o unas botas.
- **Cuántas**: cada pieza tiene un engarce.
- **Cómo se suman**: los efectos de las 4 piezas se juntan. La pureza cuenta de 1 (Agrietada) a 4 (Perfecta), así que con 4 piezas se llega hasta 16.
- **Tooltip de la pieza**: muestra la gema y su efecto de armadura.
- **Rayo y Vacío**: además se ven como atributos normales en la descripción («+X % Velocidad», «+X Vida máxima»).

| Gema | En la armadura |
|---|---|
| Veneno | Quien te golpea cuerpo a cuerpo queda envenenado (veneno II desde 6 de pureza total) |
| Filo | Espinas: le devuelves un 4 % del golpe por cada nivel de pureza |
| Escarcha | Quien te golpea queda lento |
| Fuego | −8 % de daño de fuego y lava por nivel de pureza (máximo −80 %): muy útil en el volcán |
| Arcano | −8 % de daño mágico por nivel de pureza (máximo −80 %): útil contra el Archimago |
| Vampiro | Te curas 0,25 de vida por nivel de pureza cada 5 s |
| Rayo | +2,5 % de velocidad por nivel de pureza |
| Vacío | +medio corazón de vida máxima por nivel de pureza |

**Arreglo**: el efecto de arma solo funciona con un arma de verdad en la mano. Si tienes en la mano una pieza de armadura con gema y golpeas, no hace el efecto de arma.

## Etapa 278: el Altar Maldito

Viene de los santuarios malditos de Diablo y de las cámaras de desafío.

- **Dónde está**: hay uno en cada estructura de tier 1 a 5, en una sala amplia y con techo, lejos de los cofres:
  - santuario y bosque;
  - forja y fragua;
  - hielo y fortín;
  - volcán (en el piso de la erupción);
  - torre arcana y torre.
- **Cómo es**: está hecho con bloques de Minecraft:
  - base y columna de piedra negra;
  - 4 velas rojas;
  - un cristal de obsidiana llorosa que flota y brilla, con llamas de almas.
  - Apagado, el cristal es obsidiana normal y echa humo.
- **Se puede romper**: no, en supervivencia.
- **Cómo funciona**:
  - Lo tocas cuando está cargado y despierta. Sale una barra de progreso: «Altar Maldito · Oleada 1/3».
  - Vienen 3 oleadas de enemigos de su tier: 3, 4 y 5 enemigos, más 1 por cada jugador extra (hasta 7).
  - Los enemigos aparecen con humo y llamas de almas, a 3–6 bloques del altar.
  - La última oleada siempre trae un élite (y en cualquiera puede salir uno por azar).
  - Si todos los jugadores se alejan más de 32 bloques durante medio minuto, el desafío se cancela y el altar vuelve a estar listo.
- **Al vencerlo**:
  - suelta el botín de un cofre con cerradura de nivel 3 de su tier;
  - suelta también un fragmento de corazón (40 %) o una gema vacía (60 %);
  - da Esencia a cada jugador cercano.
  - Después se apaga y se recarga al día siguiente (24 000 ticks).
- **Sellos**: si el tier está sellado para ti, el altar no responde.

## Pruebas

| Prueba | Qué comprueba |
|---|---|
| `mundos prueba gemas` | atributos de Rayo y Vacío en la armadura; el fuego baja un 24 % con un casco de Fuego Pulida; la Escarcha en las grebas ralentiza a quien te pega |
| `mundos prueba altar` | despierta; las 3 oleadas con su número de enemigos; la última trae un élite; al vencer se apaga y suelta botín y Esencia; no despierta mientras descansa; se recarga; un altar sellado no despierta; hay un altar en los 9 planos |
| `ConectividadTest` (sin el juego) | las estructuras siguen bien con los altares |
| Prueba con el cliente | 228 comprobaciones y 0 fallos: el altar cargado, la primera oleada con la barra, la oleada 3 con su élite y el botín al final con el altar apagado |

## Arreglo: barras de jefe pegadas

En las fotos de la prueba se vio que la barra de la Gran Salamandra se quedaba en pantalla mucho después de salir del volcán.
Pasaba si uno se iba de golpe (piedra de regreso, perla, /tp): la salamandra solo quitaba su barra mientras ella se movía, y
lejos ya no se mueve.

- **Arreglo**: `BarrasLejanas` revisa cada segundo las barras de la Gran Salamandra y del Altar Maldito. A más de 32 bloques, o en otra dimensión, la quita; si el dueño ya no existe, la borra.
- **Prueba**: `mundos prueba altar` lo comprueba con una barra de prueba, y en las fotos nuevas ya no aparece.

**Instalado**: el jar se puso en la instancia «test» de Axelis el 21 de noviembre, con el juego cerrado (comprobado por la fecha de latest.log).
