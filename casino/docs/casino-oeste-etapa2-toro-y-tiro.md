# Casino del Viejo Oeste — Etapa 2: TORO MECÁNICO y TIRO AL BLANCO (16-nov-2026)

## Toro mecánico (anexo RODEO)
- Bloque `casino:mechanical_bull` (pestaña principal). Clic → pantalla de apuesta (10, 25, 50, 100, 250, 500) → `WestPacket.BULL_BET`.
- La montada es la entidad invisible `casino:bull_ride` (`BullRideEntity`): el jugador la monta en el pivote del toro. En el servidor corre
  `game/BullRide` (física de equilibrio), con la tecla que manda el cliente (`WestPacket.BULL_INPUT`, A = -1, D = +1, se reenvía al cambiar y
  cada 10 ticks). Lo que se ve (inclinación del jinete, ladeo, cabeceo, giro, reloj, apuesta) va en datos sincronizados de la entidad.
- Reglas: cuenta regresiva de 3 s (si te bajas antes, se devuelve la apuesta). Luego el toro da jalones (58 %), giros (26 %) y se para de
  manos (16 %); cada vez más bravo (`intensity = 1 + s/9`). Te caes si la aguja llega a la orilla (|lean| ≥ 1). Shift = bajarte y cobrar.
- Premios por tiempo: 4 s x1, 7 s x1.5, 10 s x2.5, 14 s x4, 18 s x6, 22 s x10 (domado, termina solo).
- Calibración (`tests/BullTest`, jinetes simulados de 4000 montadas): sin tocar nada devuelve 2 %; lento (350 ms) 63 %;
  normal (250 ms, controlador óptimo) ~104 %; muy bueno (150 ms) ~180 % y lo doma 0.4 % de las veces. Una persona real suele quedar
  por debajo del "normal".
- Al caer sale volando hacia el lado de la aguja (velocidad + `ClientboundSetEntityMotionPacket`), sin daño de caída.
- `BullBE` guarda el récord (nombre y ticks) y lo dibuja en un letrero junto a la puerta del ruedo. `WestRenderers.Bull` dibuja la base,
  el pistón y el toro (cajas de color) con el mismo giro/cabeceo/ladeo que usa `BullRideEntity.seat()` para colocar al jinete.
- Aviso en pantalla (`WestHud`): cuenta 3-2-1, "¡ARRE!", barra verde/amarilla/roja con la aguja, teclas, tiempo grande, lo que cobras si te
  bajas y cuánto falta para el siguiente premio.

## Galería de tiro (anexo TIRO)
- Bloque `casino:shooting_gallery` (caja con campana sobre el mostrador; FACING = hacia el tirador). Clic → apuesta → `GalleryBE.begin`:
  cobra, presta un `casino:gallery_revolver` (en la mano si está vacía) y arranca tras 2 s.
- 12 balas, 30 s, 6 ticks entre disparos. Clic derecho con el revólver = disparo: el servidor convierte ojo y mirada a coordenadas del
  mostrador (u derecha, v arriba, w hacia el tirador) y prueba `GalleryPlan.hit` en t, t-1, t-2 y t-3 (por el retraso de la red).
- `game/GalleryPlan`: todo sale de una semilla (el cliente dibuja lo mismo). Patitos (1 pt, fila w = -5, nadan a la derecha), botellas
  (2 pts, w = -7.5, a la izquierda), bandidos (3 pts) y señoritas (-3 pts, 28 %) que se levantan 1.3 s en el fondo (w = -10), y 3 estrellas
  del sheriff (5 pts) muy rápidas arriba.
- Premios por puntos: 14 x1, 18 x1.5, 22 x2, 26 x3, 30+ x5. `tests/GalleryTest`: mala puntería devuelve 8 %, normal ~81 %, buena ~178 %.
- Si te alejas más de 5 bloques del mostrador se termina tu partida (se paga lo que lleves). Al terminar se recogen los revólveres; uno que
  quede suelto se borra al usarlo fuera de una partida.
- `WestRenderers.Gallery`: agua con olitas, repisa, riel, alambre de la estrella, blancos (texturas en `textures/entity/west/target_*.png`,
  cartón de dos caras; los del fondo giran sobre su orilla de abajo; los derribados caen hacia atrás, las botellas desaparecen) y el
  marcador colgado (jugador, puntos, balas, segundos / resultado / récord).
- Sonidos (sintetizados, `herramientas/west_juegos_synth.py`): disparo, campanita, patito, zumbido de error, cencerro, motor del toro.

## Estructura
- `el_saloon.py`: toro en el centro del ruedo (9,1,29) mirando al sur; galería: caja en (50,2,30), telón pintado del desierto en z = 19,
  canal de patitos (z = 25), postes de la repisa, faroles a los lados.
- CI: `PRUEBA_SALOON_JUEGOS`, `PRUEBA_SALOON_TORO_DATOS`, `PRUEBA_SALOON_GALERIA_DATOS`; BullTest y GalleryTest en las pruebas de lógica.

## Otros cambios
- `CasinoStats`: juegos 7 (toro) y 8 (tiro); el logro "probar todos" sigue contando solo los 7 de antes (`CLASSIC_MASK`).
- Guía del Casino: 17 páginas (toro y tiro antes de "Juega con cabeza"; los idiomas extra movieron su p14 → p16).
- Protocolo de red del casino: 4 (mensaje 32 = `WestPacket`).
