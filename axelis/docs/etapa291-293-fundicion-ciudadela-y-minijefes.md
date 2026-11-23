# Etapas 291 a 293 — La Fundición industrial, los cristales de la Ciudadela y mini-jefes en el Templo y la Fundición

Pedido de Axelis: rehacer la Ciudadela y la Fundición, y «seguir mejorando y puliendo». Las dos se parecían demasiado: un castillo
con muralla, cuatro torres en las esquinas con tejado picudo y una nave en medio.

## Etapa 291 — La Gran Fundición ya no es un castillo (`DesignForja`)
- **Fuera**: la muralla, las torres con tejado de cobre y la puerta de castillo.
- **Dentro del recinto ahora hay**:
  - una **cerca de hierro**: zócalo de piedra negra con barrotes encima y postes con faroles;
  - **cuatro altos hornos** de ladrillo en las esquinas, que se estrechan hacia arriba, con aros de hierro, la **boca del horno
    brillando** hacia el patio (magma tras una reja) y una **chimenea que echa humo**;
  - un **portón de grúa**: pilares de ladrillo, viga de acero, cadenas con faroles y la puerta corrediza abierta;
  - **vías** del portón a la nave, con cajas de mineral al lado;
  - **montones de carbón, de mineral y de escoria** en el patio, y una **grúa de pórtico** con un cubo colgando;
  - la **nave** con **tejado de dientes de sierra** y claraboyas, mirando a la entrada (por dentro el techo sigue igual).
- Sigue igual: el canal de lava del frente con su puente, los talleres del patio y sus cofres, la nave con el río de lava, el crisol,
  el yunque y el martillo gigantes, y los hornos profundos con la Piedra de Portal, la celda de Hilda y el Fundidor de la llave.
- Ruda dice ahora «busca las cuatro chimeneas que echan humo».

## Etapa 292 — Los cristales de la Ciudadela (`DesignHielo`)
- Antes había **20 manchas de hielo puestas al azar**, también dentro del patio. Ahora hay **ocho grupos de cristales de verdad**,
  colocados con intención: a los lados del camino, en las esquinas de delante y a los costados, fuera de la muralla.
- Cada grupo tiene:
  - un montículo de hielo y nieve;
  - un cristal grande casi vertical y cinco más pequeños que salen inclinados hacia fuera;
  - cristales en forma de prisma con punta: corazón de hielo azul (a veces con una luz dentro), capa de hielo compacto y la punta de
    cristal celeste.
- **Escarcha en la muralla**: vetas de hielo que bajan desde arriba por las piedras de fuera.

## Etapa 293 — Mini-jefes que guardan el Templo y la Fundición
- **Templo (el árbol)**: la Copa la guarda el **Guardián de Musgo**, detrás del altar de la Piedra de Portal. El Raizal pasa a la
  armería.
- **Fundición**: en la nave de colada, junto al crisol, manda el **Capataz**.
- Los dos son los mini-jefes de las dungeons del Rey Brote y del Gran Herrero. Su barra ahora **solo se ve de cerca** (a 28 bloques,
  se quita a más de 36), también en las dungeons. Antes salía en cuanto el jugador «los veía», a muchos chunks.
- Hilda («en la nave manda el Capataz: cuando levante el martillo, apártate») y Teo («arriba, en la copa, un gólem de musgo guarda la
  Piedra») los nombran en sus pistas.

## Pruebas
- **Planos**:
  - conectividad: la Fundición tiene 4 salas sin acceso, con tope de 16, y ninguna trampa;
  - cofres, puertas y altares bien puestos;
  - el enemigo que quedaba sobre la muralla vieja pasa al patio.
- **Visor**: vistas de la Fundición nueva (de frente, de lado y desde el patio) y de la Ciudadela con los cristales.
- **CI**: los cofres, la Piedra de Portal y la celda de la Fundición siguen en su sitio. Además, el Guardián de Musgo está en el
  Templo y el Capataz en la Fundición.
- **Cliente** (`solo=ciudadela,fragua` y `solo=templo,fragua`): fotos de la Ciudadela, la Fragua y la Fundición, y de la Copa con el
  Guardián.
