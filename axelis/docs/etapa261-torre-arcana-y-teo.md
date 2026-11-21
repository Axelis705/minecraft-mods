# Etapa 261 — Torre Arcana, Teo el bibliotecario y la tienda de cuatro vecinos

Pedido (21 nov 2026): «me gusta, sigue así mejorando».

La torre era la última estructura de jefe que no seguía el modelo nuevo (estructura mediana, con enemigos, la Piedra de Portal y una celda con un habitante). La fortaleza de antes era enorme (52 000 bloques) y no aparecía sola en el mundo.

## Torre Arcana (la estructura de la torre)

La torre donde estudiaba el Archimago antes de encerrarse en su fortaleza.

- `/mundos estructura torre`: la torre nueva.
  - Unos 29 × 36 bloques de patio y una torre de 36 de alto.
- `/mundos estructura fortaleza`: la fortaleza grande de antes, para videos.
- **El patio**:
  - muro de piedra oscura en ruinas, con musgo;
  - entrada entre dos columnas de púrpura con un cristal de amatista encima y farolas de almas;
  - un **círculo arcano** en el suelo (calcita, amatista, obsidiana llorona y velas moradas);
  - una fuente seca con un cristal, columnas caídas, un cerezo y helechos.
- **La torre**:
  - octogonal, de piedra oscura con esquinas de púrpura;
  - cornisas en cada piso, enredaderas y estandartes morados en la puerta;
  - **escalera de caracol** en el centro que sube por todos los pisos.
  - **Planta baja (vestíbulo)**: alfombra morada, estanterías, un pedestal con un cristal, barriles y faroles.
  - **Primer piso (biblioteca)**: estanterías altas por todo el muro, atriles, una mesa de estudio con velas y una alfombra.
  - **Segundo piso (laboratorio)**: soportes de pociones, calderos, frascos, amatistas y un ahumador.
  - **Observatorio**:
    - cúpula de cristal violeta con una ranura;
    - un telescopio de cobre apuntando a la ranura;
    - un mapa de estrellas en el suelo (losas negras con puntos de luz);
    - en el centro, la **Piedra de Portal: Torre** (lleva a la arena del Archimago);
    - un **cofre de provisiones de la etapa 4**;
    - una puerta al **balcón** que rodea la cúpula.
- **Celda** pegada a la torre por el este: barrotes que dan al vestíbulo, puerta de hierro, jergón, caldero, una estantería, telarañas.
- **Enemigos** (aparecen una vez):
  - dos esbirros en el patio y uno en el laboratorio;
  - una armadura encantada en el vestíbulo;
  - dos libros embrujados en la biblioteca;
  - otra armadura encantada en el observatorio, que lleva la **llave de la celda**: hay que subir hasta arriba y volver a bajar.
- **No se puede picar** en supervivencia (igual que las demás).
- **Aparece sola en el mundo**:
  - en bosques oscuros, bosques de abedules (normales y antiguos), taigas antiguas y arboledas de cerezos;
  - en regiones de 832 bloques, en algo menos de la mitad de ellas;
  - a partir de 350 bloques del punto de aparición.
  - `/mundos natural generar torre [aqui]` construye una.

## Teo, el bibliotecario (cuarto habitante)

Joven bibliotecario de la torre, nervioso y hablador, que lo sabe todo por los libros.

- **Aspecto**:
  - pelo castaño revuelto, gafas cuadradas de montura oscura, pecas;
  - una pluma de escribir detrás de la oreja;
  - casaca índigo abierta con botones de latón sobre una camisa crema;
  - correa de cuero cruzada con libros atados a la espalda;
  - dedos manchados de tinta, pantalón gris y zapatos marrones;
  - cuerpo de brazos anchos.
- Se rescata igual que los demás: con la llave de la armadura del observatorio, o forzando la cerradura con 4 de hierro si se perdió.
- **Su casa: la biblioteca de Teo**:
  - casita de calcita blanca con entramado de roble oscuro y tejado empinado de madera carmesí;
  - una torrecilla de lectura con un catalejo de cobre arriba;
  - un farol de almas en la puerta;
  - dentro: estanterías, su cama morada, un escritorio con velas y atril, un sillón de lectura y alfombras.
  - Como las demás, se pone con la Semilla de casa y su holograma.
- 4 frases de preso, 3 al rescatarlo y 8 de vecino. Habla del Archimago («decía que las estrellas le dictaban; yo creo que solo tenía insomnio»), de cómo esquivar las armaduras, y de Ruda, Bruno e Hilda.
- Da pistas de las cinco estructuras. Ruda, Bruno e Hilda también dan pistas de la torre.

## Tienda

- Nueva **Brújula a la Torre Arcana** (100 de Esencia, desde el primer vecino).
- **Con cuatro vecinos con casa se desbloquean**:
  - 8 botellas de experiencia (90);
  - un **libro encantado de Reparación** (400).

## Comandos (pruebas)

- `/mundos vecinos semilla teo`, `/mundos vecinos crecer`, `/mundos vecinos plantar teo`, `/mundos vecinos mudar teo`.

## Otros cambios

- El aviso al rescatar a alguien ahora dice «Pon su Semilla de casa junto a tu hogar: verás su casa en holograma».

## Comprobado

- Compila (776 clases).
- **Plano revisado fuera del juego**:
  - la torre tiene 1 «sala» sin acceso tolerada (la celda, cerrada a propósito), 0 trampas y la entrada transitable; se sube por el caracol a todos los pisos y se vuelve a bajar;
  - las otras 8 estructuras siguen en verde;
  - nada sin apoyo (torre: 9375 bloques; biblioteca de Teo: 379).
  - Vistas del visor de la torre por fuera y por dentro (vestíbulo, biblioteca, laboratorio, observatorio, celda) y de la casa de Teo.
- **Servidor de verdad (CI)**: las 22 comprobaciones nuevas en verde, y todas las anteriores siguen bien.
  - Torre: la Piedra de Portal en el observatorio, la puerta y los barrotes de la celda, el cofre (etapa 4), la cúpula, el caracol, los esbirros, los libros y la armadura con la llave.
  - Teo preso; la llave cae al vencer a la armadura; al rescatarlo se abre la puerta y queda libre.
  - Deshacer quita la torre y sus enemigos.
  - Con la semilla crece su biblioteca (puerta, estantería y catalejo) y él vive dentro; al mudarse se desarma.
  - Una torre «natural» construida por pasos, con Teo preso y sus esbirros.
  - La prueba de la torre de antes (esbirros, libros y armaduras) sigue pasando con la torre nueva.
- **Juego real (cliente)**: 157 comprobaciones y 0 fallos, con 114 fotos.
  - La torre por fuera, el patio, la biblioteca (con libros embrujados) y el observatorio (con la Piedra de Portal y la armadura).
  - La ventana de Teo preso; la armadura del observatorio suelta la llave; con ella se abre la celda, Teo queda libre (aviso y semilla) y sale.
  - El holograma de su biblioteca, la casa crecida por fuera y por dentro, con él dentro.
  - **Con cuatro vecinos** (Ruda, Bruno, Hilda y Teo) se compró el libro de Reparación: la Esencia bajó 400 y llegó el libro.
- Arreglo que salió al ver las fotos: el rincón de la celda en el vestíbulo quedaba muy oscuro; se añadieron una antorcha de almas y un farol.

## Pendiente

- Los otros 4 habitantes. El volcán es la estructura del jefe 5 y todavía no tiene celda.
- Reciclar cartas repetidas con los habitantes («polvo de carta»).
