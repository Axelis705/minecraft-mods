# Casino del Viejo Oeste — Etapa 4: duelo por teclas (NPC o jugadores) y MESA DE VENCIDAS (16-nov-2026)

Pedido de Axelis: "que el que dispare primero gana, que se pueda hacer contra jugadores aparte de con un NPC, que para disparar sea
una tecla totalmente aleatoria del teclado y gane quien la presione más rápido" y "una mesa donde hacer luchas de manos (vencidas),
igual contra un NPC y contra jugadores".

## Base común: `west/MatchBE` (retos de dos lados)
- Lado 0 = quien retó; lado 1 = el rival del pueblo (NPC `GAME_DUELIST`) o el otro jugador.
- Fases: IDLE → (WAITING, solo entre jugadores) → RUNNING → RESULT (4 s) → IDLE.
- Contra el pueblo: `challengeNpc(jugador, rival, apuesta)` cobra, aparece el NPC en `spot(1)` y empieza.
- Entre jugadores: `challengePvp` cobra y deja el reto abierto 60 s (aviso a todos a menos de 48 bloques y texto flotante encima
  del bloque). Otro jugador lo acepta (`accept`, paga lo mismo) o el que retó lo cancela (`cancel`, se le devuelve). Si nadie acepta,
  si quien retó se desconecta, se cambia de dimensión o está a más de 64 bloques, se le devuelve.
- Al empezar se teletransporta a cada jugador a su lugar. Salirse de su lugar (más allá de `leash()`) = pierde. Esc dos veces = rendirse.
- Premios: contra el pueblo según el rival; entre jugadores el ganador cobra x1.9 (las dos apuestas menos 5 % de la casa).
  Empate = se devuelven las apuestas. Estadísticas por jugador (`CasinoStats.DUEL` = 9, `ARM` = 11).
- Mensajes `WestPacket`: 7 retar NPC (rival*100000 + apuesta), 8 retar jugador, 9 aceptar, 10 cancelar, 11 rendirse,
  12 tecla del duelo (ms*256 + letra), 13 golpes de vencidas.
- Pantalla `ChallengeScreen` (clic en la campana o en la mesa): reglas, rivales del pueblo con su premio, apuesta, "¡Retar a X!",
  "Retar a un jugador", y si hay un reto abierto "Aceptar el reto de X" o "Cancelar mi reto". Récord y saldo.
- Cuando el reto empieza, el juego abre solo la pantalla de juego del que participa (`WestClient.duelSeen/armSeen`).

## Duelo por teclas (`DuelBE`, `DuelRules`, `DuelScreen`)
- Tres campanadas (ticks 40/60/80), silencio al azar (1 a 3.5 s) y "¡DESENFUNDA!" con una tecla al azar (A-Z y 1-9).
  La tecla no se manda a los clientes hasta el grito (nadie la puede saber antes).
- Gana quien la apriete primero. Cada cliente mide su tiempo desde el primer cuadro que muestra la tecla (así el ping no le quita a
  nadie); el servidor revisa que el tiempo sea creíble (≥ 80 ms, no mayor que lo que pasó en el servidor + 150 ms, ni 1.5 s menor).
  Tras el primer disparo espera 8 ticks por si el aviso del otro viene en camino.
- Apretar una letra o número antes del grito = pierde (los primeros 2 s no cuentan, por si el jugador venía caminando). Las teclas
  que ya venían apretadas al abrir la pantalla no cuentan hasta soltarlas. Tecla equivocada = el revólver se traba 0.5 s.
- La tecla se acepta por su lugar (teclado de EE. UU.) o por lo que dice en el teclado del jugador (GLFW `glfwGetKeyName`), así
  funciona con teclados en español, francés, etc.
- Pistoleros: Kid Pecas (900 ± 150 ms, x1.5), Black Bart (740 ± 110 ms, x2), el Doctor (620 ± 80 ms, x4). Al mediodía +0.5.
  `tests/DuelTest` con jugadores lentos/normales/rápidos.
- Pantalla transparente (se ve la calle): campanas, "¡DESENFUNDA!" con la tecla en grande, cronómetro, "¡BANG! Tu tiempo",
  resultado con los tiempos de los dos. El récord del pueblo ahora es en milisegundos (`BestMs`).

## Mesa de vencidas (`ArmBE`, `ArmRules`, `ArmScreen`)
- Bloque `casino:arm_table` (mesa alta de nogal, se juega de pie). FACING = hacia el lado 0. En el saloon: (26,1,28) mirando al este,
  en el salón entre las máquinas de rasca y gana.
- Cuenta 3, 2, 1, ¡YA! y luego cada jugador aprieta A y D alternando (o flechas). Solo cuenta si alterna. El ritmo se suaviza
  (EMA 0.12) y las manos se mueven `x += 0.095 * (ritmo0 - ritmo1) / 20`. Gana quien llegue a ±1; a los 30 s gana quien vaya ganando.
- Forzudos: Gus el Herrero (6.6 golpes/s, x1.5), el Oso (7.6, x2), Rosie la Brava (8.6, x3.5), con empujones (senos) y algo de
  cansancio. `tests/ArmTest`: lento 5.5/s pierde, normal 7.5/s casi se recupera contra Gus, rápido 10/s gana.
- El forzudo pone el brazo en la mesa (`DealerMotion.duelist`, combat 100..200 = posición de las manos) y se alegra o sacude la mano
  al final; frases `say_5..7`. `WestRenderers.Arm` dibuja cojines, agarraderas, antebrazos y las manos agarradas inclinándose.
- Pantalla: barra de fuerza (verde si vas ganando), brazos inclinándose, ritmos de los dos, teclas A/D que se iluminan.
- Sonido `arm_slam` (manotazo con vasos que tiemblan). Guía: página 19 "Vencidas" (20 páginas, índice de 8 px).

## Pruebas
- Lógica: `DuelTest`, `ArmTest` (en la lista del CI).
- CI de servidor: `PRUEBA_SALOON_DUELO` y `PRUEBA_SALOON_VENCIDAS` (bloques y datos en la estructura).
