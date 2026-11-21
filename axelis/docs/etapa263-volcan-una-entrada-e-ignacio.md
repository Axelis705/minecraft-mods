# Etapa 263 — El volcán: una sola entrada, Piedra de regreso, lava que corre e Ignacio

Pedido (21 nov 2026): el volcán es la única estructura de su tier y tiene las dos cosas importantes (el bloque y el habitante).
- Solo se entra por arriba, saltando al lago; hay que quitar el túnel de salida.
- Poner algo que le dé al jugador la pista para tirarse.
- La lava falsa de alrededor se veía quieta y fea.
- Para salir: la **Piedra de regreso**, junto al altar, que lleva a la puerta del volcán.

## Una sola entrada: el lago del cráter

- Se quitó el túnel que iba del santuario al frente del volcán. La ladera vuelve a estar entera.
- **Pistas para tirarse**:
  - donde termina el camino en espiral, unos **faroles de almas** bajan por el cráter hasta el lago;
  - allí hay un **arco rojo de los cultistas** (dos postes y una viga de ladrillo rojo del Nether, con un farol colgado), dos **estandartes rojos** y un **muelle rojo** que entra 3 bloques en el lago;
  - delante del arco, un **cartel** que brilla: «SALTO DE FE / El fuego no quema / a quien cree. / ¡Salta!»;
  - al entrar en el cráter sale un aviso: «Este calor no quema... Los cultistas se tiraban al lago. ¿Y si la lava fuera falsa?» (como mucho, una vez cada 2 minutos).
- Ruda, Bruno, Hilda y Teo ahora dicen en su pista del volcán que se entra saltando al lago.

## La salida: la Piedra de regreso

- Bloque nuevo, al pie del altar del santuario (al lado de la escalera del altar).
- Una columna de piedra negra con ribetes dorados y una flecha de fuego azul hacia arriba; echa chispas y llamas azules.
- **Clic derecho**: te lleva a la entrada del volcán (al pie del camino, mirando a la montaña), con fuego y sonido de teletransporte.
- Al acercarse sale el aviso: «Clic derecho en la Piedra de regreso (junto al altar) para salir del volcán».
- No se puede romper.

## La lava que corre

- La lava falsa ahora sabe hacia dónde corre:
  - **ríos de las laderas**: corren ladera abajo, con la textura de la lava que fluye (se ve moverse) y algo de humo y chispas;
  - **cascadas** de las salas de dentro: los lados caen hacia abajo y gotean;
  - el **lago** sigue siendo lava quieta (la que burbujea).
- El canal de metal fundido de la Fragua Olvidada también corre (del horno a los moldes).
- Al girar la estructura, los ríos siguen corriendo ladera abajo.

## Ignacio, el cultista arrepentido (quinto habitante)

Antiguo cultista del volcán que quiso dejar la secta; lo encerraron «para que lo pensara mejor».
- **Aspecto**:
  - calvo, con barba de tres días y cejas pobladas;
  - la túnica marrón de la secta con ribete naranja, pero con la capucha bajada y remiendos verdes y azules;
  - un cordón de esparto, un colgante de madera con una flor y una amapola detrás de la oreja;
  - sandalias.
- **Su celda**: pegada al santuario del volcán por el oeste. Barrotes que dan al altar, puerta de hierro, un jergón rojo, un caldero, una vela, un estandarte y un farol de almas.
- La llave la lleva el **sumo sacerdote**, un cultista que reza junto al altar del corazón.
- Si se pierde la llave, la cerradura se fuerza con hierro, como las demás.
- **Su casa: la ermita de Ignacio**:
  - casita de barro con marcos de mangle y tejado rojizo;
  - un jardín de flores a cada lado (planta flores donde antes habría prendido fuego);
  - una pérgola con azaleas en la puerta y una fogata para cocinar con un tocón;
  - dentro: su cama roja, un atril con su libro, una mesa de trabajo, macetas, alfombras rojas y naranjas y, en un estante, su vieja máscara de cultista entre flores.
- 4 frases de preso, 3 al rescatarlo y 8 de vecino. Por costumbre le dice «hermano» a todos y se corrige («Perdón: amigo»).
  - Da consejos del volcán (la Gran Salamandra, los braseros).
  - Habla de Ruda, Hilda, Bruno y Teo.
- Da pistas de las cinco estructuras.

## Tienda con cinco vecinos

- 4 varas de blaze (120 de Esencia);
- 4 cremas de magma (60);
- una **manzana de oro encantada** (800).

## Otras mejoras

- La revisión de los planos sabe que junto a una Piedra de regreso se sale de la estructura.
- Al deshacer un volcán, los habitantes ya rescatados se quedan.

## Comprobado

- Compila.
- **Plano revisado fuera del juego**:
  - el volcán tiene 0 trampas (antes de contar la Piedra de regreso, toda la dungeon salía como «trampa»);
  - tiene 2 salas sin acceso toleradas (la celda de Ignacio y un hueco junto al corazón colgante);
  - las demás estructuras siguen en verde.
- Vistas del visor del arco y el muelle desde el lago, y del santuario con la Piedra de regreso y la celda.
- **Servidor de verdad (CI)**, todo en verde:
  - la Piedra de regreso junto al altar;
  - que ya no hay túnel;
  - el arco y el cartel del salto;
  - un río que corre ladera abajo y el lago quieto;
  - la celda de Ignacio (puerta y barrotes), Ignacio preso y el sumo sacerdote con la llave;
  - la llave cae al vencerlo, el rescate abre la puerta e Ignacio queda libre;
  - al reiniciar el volcán vuelve el sacerdote, pero no un segundo Ignacio, e Ignacio sigue libre tras deshacer el volcán;
  - su ermita crece con la semilla (puerta, atril y máscara), él vive dentro y, al mudarse, se desarma.
- **Juego real (cliente)**:
  - fotos del arco y el muelle desde el camino y desde el lago;
  - Ignacio preso (su ventana) y rescatado;
  - la Piedra de regreso con su aviso;
  - después de usarla, el jugador aparece en la entrada del volcán, entre los braseros.
- Arreglo que salió en las pruebas: la casa de Ignacio no se ponía en la prueba de cliente porque el jugador seguía en modo espectador (era la prueba, no el juego).
