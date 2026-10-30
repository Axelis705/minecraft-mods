# Casino (Minecraft Forge 1.20.1 · Forge 47.4.10 · Java 17)

Mod de casino (`mod_id = casino`), separado de Kensei y de Pool Table. **Etapa 1:** fichas, cajero y la **Ruleta de la suerte** diaria al estilo GTA 5. **Plinko** (mesa grande 3x4).

## Ruleta de la suerte (como la de GTA)
- Estructura de **3 de ancho x 4 de alto** (se coloca el objeto *Ruleta de la suerte*; la cara mira a quien la pone).
- **20 casillas, cada una con 5%** de probabilidad (igual que GTA). Un **giro gratis al día** por jugador (se reinicia a medianoche, hora del servidor).
- Clic derecho en cualquier parte de la ruleta para girar. Giro de ~9 s con animación suave, "tic" del puntero en cada casilla, luces que corren, fanfarria, fuegos artificiales y un cartel de premio con confeti.
- Casillas: **Premio gordo (podio)**, Misterio, fichas (50/100/150/250/500), esmeraldas (8/16/24), experiencia (3/5/8/10/15 niveles), oro (6/12), diamantes (3), zanahorias doradas (12), manzanas doradas (2).
- **Premio gordo configurable:** agáchate y dale clic a la ruleta con el objeto que quieras de premio en la mano (copia el objeto y la cantidad). Solo operadores, jugadores en creativo o el dueño de un mundo de un jugador. Agachado con la mano vacía: muestra el premio actual. El premio flota girando sobre la ruleta con halo y nombre.
- Si el jugador se desconecta durante el giro, el premio se le entrega al volver a entrar.

## Fichas y cajero
- 5 fichas: blanca (1), roja (5), verde (25), negra (100), morada (500). Se pueden juntar/separar en la mesa de crafteo.
- **Cajero:** clic derecho → pantalla en 2 pasos: arriba eliges COMPRAR fichas o CANJEARLAS; eliges esmeralda (1 = 10 fichas, y 10 fichas = 1 al canjear) o diamante (1 = 80, y 80 fichas = 1 al canjear; mismo precio de ida y vuelta, sin comisión); eliges la cantidad (-10, -1, +1, +10, Máx) y abajo ves lo que *entregas* y lo que *recibes* antes de pulsar CONFIRMAR. El paquete `CashierPacket` lleva `(pos, kind, buy, amount)` y el servidor limita la cantidad a lo que realmente tienes.
- Todos los juegos futuros cobran y pagan con las fichas del inventario (`chip/Chips.java`: `balance`, `take`, `give`).

## Comandos (nivel de operador)
- `/casino resetwheel [jugadores]`: el jugador puede volver a girar hoy.
- `/casino chips <jugadores> <cantidad>`: regala fichas.

## Recetas
- Ruleta: 4 bloques de oro + diamante + reloj + 3 obsidiana. Cajero: 5 lingotes de oro + 2 hierro + esmeralda + redstone. Fichas: 5 de un valor = 1 del siguiente, y al revés.

## Código
- `game/WheelLayout.java` (las 20 casillas, probabilidad y ángulos, sin Minecraft), `game/WheelRewards.java` y `game/Prize.java` (premios reales).
- `block/LuckyWheelBlock.java` (multibloque 3x4), `block/LuckyWheelBE.java` (giro, premio, podio, guardado), `block/CashierBlock.java`, `block/Cashier.java`.
- `data/CasinoData.java`: día del último giro por jugador y premios pendientes (se guarda con el mundo).
- `net/`: canal `casino:main` (`CashierPacket` cliente→servidor, `WheelWinPacket` servidor→ganador).
- `client/LuckyWheelRenderer.java` (disco, luces, puntero, premio flotante), `CashierScreen.java`, `WinBanner.java` (cartel de premio).
- Recursos generados con scripts (texturas con PIL, 8 sonidos sintetizados en .ogg, modelos y recetas).

## Próximas etapas
2) tragamonedas · 3) blackjack y ruleta de casino · 4) póker, plinko y rasca y gana · 5) edificio de casino generado en el mundo.

No probado dentro del juego (solo compilado, con las probabilidades verificadas en 2 millones de tiradas).

## Mesa de Plinko
- Bloque multiestructura 3x4 (igual que la ruleta); receta: 2 lingotes de oro + esmeralda + 2 hierro + bola de slime + 3 blackstone pulido (`GEG/IBI/PPP`).
- Clic derecho → panel de apuestas abajo (no oscurece la pantalla). Apuestas: 10, 25, 50, 100, 250, 500 fichas. Riesgo BAJO / MEDIO / ALTO (cambia los multiplicadores). Se suelta con el botón, Espacio/Enter o en modo AUTO.
- 10 filas de clavijas, 11 casilleros; cada rebote es 50/50, el casillero es la cantidad de rebotes a la derecha (probabilidades 1,10,45,120,210,252,210,120,45,10,1 de 1024).
- Multiplicadores (de la orilla al centro): bajo x8, x3, x1.5, x1.1, x0.9, x0.5; medio x22, x5.5, x2.2, x1.3, x0.6, x0.3; alto x55, x10, x3, x1, x0.3, x0.2. Retorno medio: 95.6% / 96.8% / 97.3% (probado con todas las combinaciones, `PlinkoTest`).
- El servidor cobra la apuesta, sortea el camino al soltar la bola y paga al llegar al casillero (hasta 8 bolas a la vez por mesa). Si el jugador no está conectado, el premio queda pendiente y se entrega al volver. Pagos de x5 o más muestran el cartel de premio; x10 o más avisan en el chat a los cercanos.
- Código: `game/PlinkoLayout` (reglas + trayectoria, sin Minecraft), `block/PlinkoBlock` (extiende la ruleta), `block/PlinkoBE`, `net/PlinkoPacket` (id 2: pos, apuesta, riesgo), `client/PlinkoRenderer`, `client/PlinkoScreen`.
