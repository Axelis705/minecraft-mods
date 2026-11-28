# Etapa 316 · El hotel (capítulo 1, antes de los equipos) — 27-nov-2026

Minijuego **individual** que va entre la sala grande y la formación de equipos. Diseño y decisiones de Axelis: `claude/hotel-diseno.md`.

## Cómo va (para el organizador)
1. Cuando todos entran a la sala grande, baja el portón y el Coleccionista presenta **la torre de 32 ascensores** que está en medio de la
   sala ("uno por persona"). Al organizador le llega en privado cuál es la salida y cuáles son las trampas.
2. Se abren los 32 ascensores angostos (60 s). Si se meten dos en uno, no cierra. La flecha "ir acá" apunta al libre más cercano. Al
   estar todos (o al acabarse el tiempo, el Coleccionista mete a los que faltan) **bajan todos juntos** (~45 s) mientras él explica las
   reglas. Cada quien llega a un lugar distinto del hotel, repartidos por los 10 pisos.
3. **10 minutos** para encontrar la salida. Sin regeneración natural (solo curan los botiquines: +4 corazones, uno por botiquín).
4. Al acabarse: los que siguen adentro quedan eliminados; si está activo **"El último cae"**, también el último en salir. Los demás se
   duermen con el gas en la sala de espera y despiertan en la sala grande, donde el Coleccionista pide los equipos (guion `hotel_regreso`).

Botones en la línea de tiempo (capítulo 1): Hotel ya · ¿Cuál es la salida? · Bajar ya · El último cae · Terminar hotel · Con / sin hotel.
Comandos: `/evento hotel [empezar|ascensores|bajar|terminar|limpiar|usar <si>|ultimo <si>|tiempos <juego> <entrar> <viaje>|trampas <n>]`
y para probar `abrir <salida|trampa|n> <pieza>`, `salir <pieza>`, `frente <n> <pieza>`.

## El hotel (mapa)
- Diez pisos de 51 x 51 alrededor de un patio central, hechos a mano en texto (`herramientas/mapas/hotel_plano.py` → `planos/hotel.txt`):
  vestíbulo con fuente y recepción, salón de baile con candelabros y piano, habitaciones, lavandería, cuartos quemados, spa, oficinas,
  piso inundado, suites y ático. Solo dos escaleras entre cada par de pisos (en esquinas opuestas): hay que cruzar el piso.
- 32 ascensores de llegada (idénticos por dentro a los de la torre), 20 puertas de servicio (2 por piso: "SERVICIO 4A", "4B").
- Lejos al noreste del complejo (zona `hotel`, x +300, z −130) y la sala de espera aparte (zona `hotel_espera`). Se construyen solos al
  actualizar el mod (huella).
- Revisado sin Minecraft (`vistas.sh … hotel`): desde cualquier ascensor se llega a todos los demás y al frente de las 20 puertas.

## Las puertas
- Cada partida: **1 salida y 3 trampas al azar** (casi siempre una trampa es la otra puerta del mismo piso que la salida).
- **Trampa:** la puerta se abre rechinando, el hilo se tensa y a los 0.65 s ¡pum!: la escopeta del tripié le vuela la cabeza solo a
  quien abrió; el cuerpo se queda parado un instante sin cabeza (con el cuello cortado) y cae de espaldas. Eliminado al instante.
- **Detalles:** las de trampa tienen un hilo de nylon que sube a la manija, una mancha en el piso y la placa chueca; la salida deja pasar
  luz de colores por abajo (y polvito de colores).
- **Pistas** en las paredes (cada quien las lee en su idioma): en cada piso una dice "más arriba / más abajo / en este piso"; las demás
  son consejos (el hilo, la luz de colores, los tubos, el timbre, los cuartos, los botiquines).
- **Salida:** el clóset tiene el hoyo de colores; al pisarlo se cae por un tubo de rayas en espiral (rojo, amarillo, verde, azul) hasta
  el colchón de brincolín de la sala de espera.

## Peligros
- **2 Botones** (no se les puede hacer daño): ronda por el pasillo con su carrito; al ver a alguien azota el timbre, suelta el carrito y
  persigue (6 de daño y se queda un momento quieto). No cabe por las puertas ni por las escaleras: los cuartos son refugio. Cambia de piso
  por el "elevador de servicio" cuando en el suyo no hay nadie.
- **Ratas de cuerda** (1 por nido, 20 nidos; reaparecen a los 45 s): muerden poquito (0.75 corazón); se les acaba la cuerda y se quedan
  quietas un rato. Con el **tubo** (en el piso, con cinta amarilla y negra; clic derecho para agarrarlo) se mueren de un golpe. El tubo no
  le hace nada a nadie más; en el hotel no hay golpes entre jugadores.
- Tablas con clavos (2 de daño y lentitud), charcos con electricidad (chispean de aviso y luego dan toque), candelabros que se caen al
  pasar abajo.

## Sala de espera
Cuarto de brincolín/backrooms (papel amarillo, lámparas de tubo, piso de hule de colores), bancas, sillones y **12 monitores**: cada uno
muestra la cara, el nombre, el piso y la vida de alguien que sigue adentro (si son más, se turnan). **Clic en un monitor = ver desde los
ojos de esa persona**; agacharse (Shift) para volver.

## Modelos, sonidos y voces
- Modelos a mano por código (`geokit.py`): Botones (+ carrito), rata, escopeta del tripié, puerta de servicio (abrir, abrir de golpe,
  portazo). Bloques nuevos: piano viejo, tubo suelto, tabla con clavos, botiquín (lleno y vacío), boca del tobogán, colchón de brincolín,
  monitor.
- Sonidos hechos a mano (`hotel_sonidos.py`): timbre, ruedas del carrito, cuerda de juguete, chillido, puerta que rechina, tobogán.
- Guiones nuevos para grabar (en `docs/guion-voces.md`): `hotel`, `hotel_ascensor`, `hotel_fin`, `hotel_regreso`.

## Pruebas
- Servidor (CI con bots): sala → hotel → bajan 4 → un bot abre una trampa (sin cabeza) → dos salen por el tobogán → terminar → regreso
  a la sala y equipos.
- Cliente con fotos: escenario `coleccionista_hotel` (rama `fotos-coleccionista-1.20.1`).
