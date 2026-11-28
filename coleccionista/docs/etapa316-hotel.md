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
- Servidor (CI con bots, servidor de desarrollo): sala → hotel → bajan 4 → un bot abre una trampa (sin cabeza) → dos salen por el
  tobogán → terminar → regreso a la sala y equipos. **Bien** (corrida 46091982373). El servidor Forge real usa `real_commands.txt`
  (ahí el hotel va apagado para probar los equipos directos).
- Cliente con fotos: escenario `coleccionista_hotel` (rama `fotos-coleccionista-1.20.1`): **24 fotos, 0 fallos** (corrida 46095457806,
  commit 68d5a7bd). Se ve la torre, el ascensor por dentro, la llegada, las tres clases de puerta, el Botones con su carrito, la rata,
  una pista, el tubo en el piso y en la mano, el disparo y el cuerpo sin cabeza, el hoyo de colores, la caída por el tubo, el colchón,
  la sala de espera con los monitores y el regreso a la sala.
- Arreglos tras las fotos: más luz en el hotel (pasillos 10, cuartos 9, vestíbulo 12), monitores que brillan solos, luz de la salida más
  sutil, el hueco de la puerta deja pasar al abrirse (la salida vuelve a cerrarse sola), el fogonazo solo dura un instante, aviso
  "— Piso N —" al cambiar de piso, el tubo se llama `tubo_suelto` (chocaba con el bloque `tubo`).

## Mejoras de luz, ambiente y jugabilidad (después de "mejora")
- **Lámparas que se ven:** apliques en las paredes de los pasillos (138) y lámparas de techo en los cuartos (253), más faroles en el
  ático (23). Algunas parpadean de vez en cuando. Los ascensores de llegada tienen letrero "PISO N".
- **Ruidos por piso:** cada quien oye cosas cerca, de vez en cuando: portazos, pasos, cadenas y el timbre a lo lejos; y según el piso,
  notas sueltas del piano (piso 2), máquinas de la lavandería (4), fuego en los cuartos quemados (5), goteo (6 y 8) o viento (10).
- **El Coleccionista ayuda:** a los 5:00 dice si la salida está en piso par o impar, y a los 2:00 dice el piso exacto.
- **Pistas:** en cada piso, dos dicen hacia dónde ir (más arriba / más abajo / en este piso); hay consejos nuevos (agacharse cerca del
  Botones, no correr). Se leen un poco mejor en lo oscuro.
- **Sigilo contra el Botones:** agachado solo te ve muy de cerca; corriendo te oye aunque no te vea.
- **Adrenalina:** después de un golpe del Botones te da velocidad un momento para huir.
- Fotos de la prueba con cliente (corrida 46100479918, commit 1439c2fc): **24 fotos, 0 fallos**; los pasillos junto a las puertas se
  ven con el doble de luz que antes.

## Detalles (después de "perfecciona más y detalles")
- **Placas en las puertas de los cuartos** (105): 301, 302… en el sentido del reloj desde la esquina noroeste; los cuartos especiales
  dicen qué son (SALÓN, COCINA, LAVANDERÍA, OFICINA, SPA). Sirven para orientarse ("estoy junto al 704").
- **Pasillos con más vida:** cuadros viejos colgados entre puertas (75), telarañas arriba (27) y manchas en el piso de los cuartos (111).
- **El elevador al bajar:** el letrero (barra de abajo) cuenta los pisos desde el 13 hasta el de cada quien; a la mitad se va la luz un
  instante y el número se vuelve loco (ahí pasan a la cabina del hotel sin que se note).
- **El disparo:** los que están lejos oyen el eco sordo por todo el hotel (no saben dónde fue). Debajo del cuerpo sin cabeza se va
  haciendo un charco de sangre.
- **Sala de espera:** pizarrón verde con gis en la pared oeste, "ORDEN DE SALIDA": cada quien que sale con su tiempo, y los que caen en
  una trampa con una cruz roja (si "El último cae", su renglón se pone rojo). Al caer al colchón: confeti de colores.
- **Monitores:** el de quien acaba de caer en una trampa dice "SEÑAL PERDIDA" en rojo, con su nombre tachado, unos segundos.
- **El último minuto:** el Coleccionista avisa ("Les queda un minuto… y la luz es mía"), se apagan las lámparas cerca de cada quien
  2 segundos (con oscuridad) y vuelven; los últimos 30 segundos suena un reloj de péndulo (más fuerte los últimos 10).
- Si un apagón se queda a medias (se cierra el servidor justo entonces), al empezar la siguiente partida se prenden todas otra vez.
- Prueba con fotos (corrida 46111000812, commit fc520f87): **27 fotos, 0 fallos**; se vio el letrero "▼ Piso" al bajar, la placa 301,
  el pizarrón y los cuadros; el monitor dijo "señal perdida" y el apagón apagó 68 lámparas. Arreglos tras verlas: el charco usa una
  textura propia (oscura y brillosa: la salpicadura no se veía sobre la alfombra roja), el pizarrón lleva letra más grande en 4 columnas
  de 9, cruces en gis rojo claro y dos lámparas arriba; las placas de los cuartos, en dorado más claro.
- Segunda prueba con fotos (corrida 46114133111, commit 0f229b0d): **27 fotos, 0 fallos**; el pizarrón ya se lee (la cruz en rojo
  claro y el tiempo). El charco seguía sin verse: el cuerpo quedaba hundido en la alfombra (1 px) y el charco se dibujaba debajo de ella.
  Ahora se dibuja encima de lo que haya en el piso.
- Tercera prueba con fotos (corrida 46119328756, commit 788b770c): **27 fotos, 0 fallos**; el charco ya sale, pero oscuro sobre alfombra
  roja en un cuarto con poca luz casi no se distinguía. Ahora lleva una segunda capa con reflejos de líquido y una orilla fina de sangre
  fresca, dibujada con un poco más de luz (se ve mojado aunque el cuarto esté oscuro), y sale más allá del cuello (antes la mitad
  quedaba debajo del cuerpo).

## Pendiente
- Axelis: grabar las voces nuevas (`hotel`, `hotel_ascensor`, `hotel_fin`, `hotel_regreso`).
- Probarlo en el juego real con amigos (el hotel se ve oscuro con brillo bajo: subir el brillo del juego).
