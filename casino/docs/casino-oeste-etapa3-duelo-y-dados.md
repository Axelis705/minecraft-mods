# Casino del Viejo Oeste — Etapa 3: DUELO AL MEDIODÍA y DADOS DEL MENTIROSO (16-nov-2026)

Axelis no contestó las dos preguntas (cartas: faro / dados / los dos; duelo: NPC / jugadores) y pidió "sigue mejorando".
Decisión tomada: **duelo contra pistoleros NPC** (el duelo entre jugadores puede venir después) y **dados del mentiroso** como juego
de cartas/dados del saloon (es distinto del 21 y del póker que ya hay). El faro queda pendiente.

## Duelo al mediodía (calle)
- Bloque `casino:duel_bell` (poste con campana y letrero). FACING = hacia donde aparece el pistolero. La X del jugador está 2 bloques
  hacia la calle (`FACING.getCounterClockWise()`), la del pistolero 16 bloques más allá. En el saloon: campana en (22,1,49) mirando al
  este; X en (22,47) y (38,47), frente a las puertas.
- Clic → pantalla: rival (Kid Pecas x1.5, Black Bart x2, el Doctor x3.5) y apuesta. `DuelBE.begin`: cobra, teletransporta al jugador a
  su X, presta el revólver (`GunGame.lend`), aparece el pistolero (`DealerEntity.GAME_DUELIST`, piel de vaquero) y suena el silbido.
- `game/DuelRules`: 3 campanadas (ticks 40, 60, 80), silencio al azar de 1 a 3.5 s y "¡DESENFUNDA!". El revólver debe estar
  "enfundado": mirar al suelo (pitch ≥ 45°) al momento del grito. Disparar antes o no mirar al suelo = descalificado. Salirse de la X
  (más de 1.8 bloques) = pierde. El pistolero dispara a los 16/13/11 ticks (± 3/2/2) y acierta 72/82/90 %; si falla, repite cada 9 ticks
  y después de 3 s ya no falla. Gana el primero que acierta (el disparo del jugador se revisa contra la caja del pistolero). Al mediodía
  del juego (5000 a 7000) el premio sube +0.5.
- `tests/DuelTest`: tirador normal (650 ms, 70 % de puntería) devuelve 96 / 72 / 58 %; rápido (450 ms, 85 %) 137 / 162 / 210 %.
- Pistolero: poses en `DealerMotion.duelist` (mano en la cacha al grito, brazo estirado al disparar, manos arriba si pierde),
  frases `casino.tip.duelist_west.say_0..4`, nombres `casino.npc.duelist_<variante>`.
- Avisos (`WestHud`): campanadas 1-2-3, "✔ Revólver enfundado / ✖ ¡Mira al suelo!", "¡DESENFUNDA!" con cronómetro en la pantalla,
  balas y resultado. `WestRenderers.Duel` dibuja las dos X de gis y el récord del más rápido en el letrero del poste.
- Sonidos: `duel_theme` (silbido + guitarra), `duel_draw` (golpe metálico). Estadística 9.

## Dados del mentiroso (sala de cartas de arriba)
- Bloque `casino:liars_dice` (cubierta de mesa con fieltro sobre una pata). En el saloon reemplaza la segunda mesa de 21 de arriba:
  mesa en (37,10,13) mirando al sur, silla del jugador al sur, el viejo Hank (`marcador dicer 2`, GAME_DUELIST piel 3) al norte.
- `game/LiarsDice`: 5 dados cada uno, los unos son comodines (no se apuesta a los unos), cada apuesta sube (más dados o misma cantidad
  de cara mayor), "¡Mentiroso!" destapa: si hay al menos lo apostado gana quien apostó. Paga x1.9. Empieza quien gane el volado.
- IA del tahúr (`think`): grita si la apuesta de enfrente tiene menos de 38 % de ser cierta (viendo sus dados), si no sube a la apuesta
  más creíble con 18 % de farol. `tests/DiceTest`: nunca gritar devuelve 43 %, al azar 48 %, jugar como el tahúr 95 %, muy listo 98 %.
- `DiceBE` guarda dados, apuestas (quién*100 + cantidad*10 + cara), turno y racha récord. `syncTag` borra los dados del tahúr hasta
  destapar. Si el jugador se aleja más de 8 bloques, pierde. Mensajes `WestPacket` 4 (apostar), 5 (subir), 6 (gritar).
- `DiceScreen`: reglas y apuesta; en juego tus dados, el cubilete de Hank, historial, cantidad (−/+), caras 2-6, "Apostar" y
  "¡Mentiroso!"; al destapar resalta los dados que cuentan. Aviso de turno arriba si cierras la pantalla. `WestRenderers.Dice` dibuja
  los cubiletes (y los dados al destapar). Sonido `dice_shake`. Estadística 10.

## Otros
- El revólver ahora sirve para cualquier juego `GunGame` (galería y duelo).
- Guía del Casino: 19 páginas (toro, tiro, duelo, dados antes de "Juega con cabeza"; índice con renglones de 9 px).
- CI: `PRUEBA_SALOON_DUELO`, `PRUEBA_SALOON_DADOS`; DuelTest y DiceTest en las pruebas de lógica.

## Pelea de cantina (ambiente)
- La dirige la pianola del saloon (`PianoBE.serverTick` → `west/Brawl`). Si la pianola está prendida y hay alguien a menos de 18
  bloques, cada 5 a 10 minutos (la primera entre 2 y 5 min) dos vaqueros cercanos (`GAME_PATRON` del Oeste) se agarran a golpes.
- La pianola se calla (LIT=false y vuelve al terminar), los peleadores se ponen frente a frente (NoAI mientras pelean, pose de puños
  arriba con `combat = 4` en `DealerMotion.patron`, golpes `DealerAnim.PUNCH`/`PUNCH_L`), los demás vaqueros festejan.
- Apuestas: durante los primeros 3.5 s, clic derecho a un peleador = 25 fichas por él; paga x1.9 (gancho en `DealerEntity.mobInteract`).
- 4 golpes acertados (62 %) tumban al otro; el perdedor retrocede, el ganador festeja y el sheriff del Oeste dice "¡Se acabó!"
  (`casino.tip.guard_west.say_3`). Si un peleador desaparece, se devuelven las apuestas.
- Comando de operador: `/casino pelea <x y z>` (la pianola más cercana, hasta 32 bloques). CI: `PRUEBA_SALOON_PELEA`.

## CI
- `tools/boot_server.sh` ahora espera a que el servidor diga `CI_FIN_COMANDOS` (hasta 3 min) antes de mandar `stop`: en máquinas
  lentas las construcciones grandes tardaban y el servidor se cerraba antes de las últimas pruebas (le pasaba al Show).
