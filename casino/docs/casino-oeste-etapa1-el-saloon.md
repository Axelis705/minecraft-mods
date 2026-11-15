# Casino del Viejo Oeste — Etapa 1: EL SALOON (15-nov-2026)

## Decisiones de Axelis
- Exterior **como su foto**: saloon de 2 pisos de tablas crema, frontón escalonado con "SALOON", balcón con barandal sobre el porche de tablones, puertas batientes, ventanas con marco rojo, edificios de madera al lado y torre de agua.
- Juegos exclusivos (los 4): **duelo al mediodía, toro mecánico, tiro al blanco y cartas del saloon**. Misión especial: **ninguna por ahora**.
- Ambiente: **pianola que toca sola, pelea de cantina y sheriff de guardia**.
- Pendiente de respuesta: cartas = faro, dados del mentiroso o los dos; duelo contra NPC y/o entre jugadores.

## Hecho en la etapa 1
- **Plantilla** `casino/herramientas/estructuras/el_saloon.py` → `data/casino/structures/el_saloon.nbt` (60 × 26 × 50, sin sótano; eje x = 29.5).
  - Calle de tierra con surcos (z 43..49), banqueta con escalones, barandas para caballos y bebederos.
  - SALOON (x 15..44, z 8..38, muros dobles: tablas por fuera, lambrín + tapiz por dentro), frontón hasta y 22 con "SALOON" y estrellas, placa "1880", balcón con barandal (piso y 8).
  - Planta baja: barra larga al oeste (espejo, repisas de botellas, "WHISKEY", 2 cantineros), pianola y mesas de cartas al frente-oeste, caja con rejas ("CAJA", 2 cajeros) y tienda de premios al frente-este, ruleta de la suerte entre 6 tragamonedas (pared este), 2 mesas de 21 con crupier del Oeste, 2 rasca y gana, salón de doble altura con galería, candiles de rueda de carreta, carteles de "Se busca".
  - Escalera al centro del fondo con **puertitas VIP (Plata)** y **dos sheriffs**.
  - Primer piso (Plata): sala de cartas con 2 mesas de 21 de alto riesgo (reservadas para las cartas del saloon), galería con barandal, cuarto con cama, cuarto de juego y el **palco "JEFE" (Oro)** con salida al balcón.
  - Anexo oeste **RODEO**: ruedo de arena y paja con cerca redonda (el toro llega en la etapa 2). Anexo este **TIRO**: mostrador y blancos al fondo (la galería de tiro llega en la etapa 2).
  - Patio: torre de agua, letrina, barriles, paja y cactus. 10+ vaqueros (`wpatron`).
  - `recorrido.py el_saloon`: se puede caminar a todo (0 problemas).
- **Aparece** en desierto, badlands (3) y sabana (3) (`has_structure/el_saloon`), en el mismo conjunto `casinos.json` que THE DIAMOND. Comando: `/casino build <x y z> <giro> saloon`.
- **Bloques nuevos** (`com.casino.west`, pestaña de decoración):
  - `saloon_door`: hoja batiente sin colisión; física de resorte amortiguado en el cliente (`SaloonDoorBE`), se abre hacia donde camina quien pasa, se queda abierta mientras pasa, rebota al soltarse; rechina (`saloon_door.ogg`). Dibujo en `WestRenderers.Door`.
  - `vip_gate`: igual pero con terciopelo y placa "VIP"; sólida para quien no **lleva encima** una tarjeta del nivel (`access` 1 Plata / 2 Oro, agachado + clic de operador lo cambia).
  - `player_piano` (pianola): música `music_piano` (3 ragtimes hechos con código: `herramientas/ragtime_synth.py`), teclas que se hunden y rollo que corre (`WestRenderers.Piano`). `SpeakerMusic` ahora maneja dos canales (jazz y pianola).
  - `painted_letter`: letra pintada (fuente Rye, OFL) en 6 colores; usa la misma pantalla que el neón.
  - `wanted_poster`: 4 carteles ("WANTED"/"SE BUSCA").
- **Personajes del Oeste**: variante `DealerEntity.WEST = 64` (+ n para vaqueros, 8 pieles). Sheriff, cantinero, crupier con visera, anfitrión con bombín y vaqueros con **sombreros 3D** (`DealerProps.drawHat`). Frases propias (`casino.tip.<rol>_west.*`, si falta alguna usa la normal). Marcadores: `guard 2 w`, `bartender 1 w`, `wheelhost 3 w`, `wpatron dir piel`, `m blackjack 2 w` (BlackjackBE.theme = 1).
- Recursos: `herramientas/west_assets.py`, textos `herramientas/west_lang.py`.
- CI: pruebas `PRUEBA_SALOON_*` (puertas, decoración, mesas, sheriffs, personal, vaqueros, estructura natural).

## Siguientes etapas
2. Toro mecánico (RODEO) y tiro al blanco (TIRO).
3. Duelo al mediodía en la calle y cartas del saloon (faro / dados del mentiroso) en la sala de arriba.
4. Pelea de cantina.
