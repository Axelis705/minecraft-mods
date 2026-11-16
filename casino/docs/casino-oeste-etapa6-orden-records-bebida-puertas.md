# Casino del Viejo Oeste — Etapa 6: orden del saloon, récords del mundo, cóctel por nivel y sonido de las puertas (16-nov-2026)

Pedido de Axelis: un solo récord para todo el mundo; ordenar el saloon ("entras y no está clara la estructura, máquinas por allá, mesas
en medio sin sentido"; en el palco había una mesa justo en medio de la entrada); el cóctel del bartender con efecto corto (30 s y más
con más nivel) y espera larga para el siguiente (menor con más nivel); mejor sonido para las puertas del Oeste, sin derechos.

## Récords del mundo (`west/WestRecords`, guardados en `CasinoData`)
- Toro, tiro, duelo, vencidas y racha de dados: un solo récord para todo el mundo. Todos los carteles (de todos los saloons que
  aparezcan) lo muestran: cada uno lo revisa cada 2 s y se actualiza solo. En duelo y vencidas gana el tiempo menor.
- Si un cartel traía un récord de antes y es mejor que el del mundo, se sube al del mundo (no se pierde nada de los mundos viejos).
- Las pantallas del Diamond ya eran de todo el mundo (leen `CasinoStats`).

## Orden de la planta baja (el_saloon.py)
- Pasillo del centro con alfombra roja de la puerta a la escalera, sin nada en medio.
- Oeste = la cantina: barra larga, pianola y tres mesas redondas con sillas.
- Centro = las dos mesas de 21, una a cada lado del pasillo (x 24 y 35), bajo los candiles.
- Este = máquinas en la pared: tragamonedas, ruleta de la suerte y las dos de rasca y gana (antes estaban en medio del salón).
- Frente-este = caja con rejas (cajeros al salón) y la tienda de premios en la cara norte de la caja (da al pasillo del TIRO);
  rincón de las vencidas (mesa en (34,1,33) mirando al este) con banca para mirones.
- Arriba: el póker de la sala de cartas pasa junto a los dados (x 41). Palco del dueño: camino libre (alfombra) de la puertita al
  balcón; al este el escritorio del jefe (bloque nuevo `saloon_desk`, dos juntos) con su sillón detrás, sillas para visitas y libreros;
  al oeste la sala con sillones de piel, mesita con whisky y sillón; candil de rueda.
- CI: mesas de 21 en (324|335, 101, 162), crupier en x 324.5, vencidas en (334, 101, 173).

## Cóctel del bartender (`DealerEntity.serve`, `Membership.DRINK_*`)
- Efecto Suerte: 30 / 45 / 60 / 90 / 120 s (Bronce, Plata, Oro, Platino, Diamante).
- Espera para el siguiente: 10 / 8 / 6 / 5 / 4 min. La espera es del jugador (se guarda en `CasinoStats`, campo `ld`), ya no de cada
  bartender (antes bastaba con ir con otro). Si pides antes, te dice cuánto falta. La tarjeta de socio muestra la línea del cóctel.
- Nota: la Suerte de Minecraft no cambia los juegos del casino (solo pesca y cofres).

## Puertas batientes
- Sonido nuevo hecho con grabaciones CC0 (`herramientas/west_puerta_sonido.py`): rechinido corto de bisagra (OpenGameArt,
  "Creaky door hinge") en 3 versiones y golpecito de madera al pasar por el centro (BigSoundBank 0224 "Light door").
- Antes sonaban las dos hojas a la vez con tonos al azar y rechinaba en cada vaivén. Ahora suena una sola hoja por puerta, el
  rechinido solo al empujar, y en cada vaivén un golpecito cada vez más suave.
- Los archivos se bajan con `.github/workflows/descargas.yml` (rama `descargas`).
