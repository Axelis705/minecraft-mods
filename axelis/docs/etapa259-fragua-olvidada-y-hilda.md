# Etapa 259 — Fragua Olvidada, Hilda la herrera y la tienda de tres vecinos

Pedido (20 nov 2026): «sigue puliendo y mejorando».

La forja era la única estructura (junto con la torre) que no seguía el modelo nuevo: estructura mediana con enemigos, Piedra de Portal y una celda con un habitante. Ahora sí.

## Fragua Olvidada (la estructura de la forja)

El viejo taller del Gran Herrero de Brasas, abandonado desde que su fuego se volvió loco y la escoria cobró vida.

- `/mundos estructura forja`: la fragua nueva.
  - Unos 31 × 41 bloques; las chimeneas llegan a 20 de alto.
- `/mundos estructura fundicion`: la gran fundición de antes, para videos.
- **Patio de trabajo**:
  - muro bajo de piedra negra con rejas encima;
  - entrada entre dos pilares con braseros y un letrero colgado de cadenas;
  - montones de mineral de hierro, de cobre y de carbón;
  - una vía de vagonetas que acaba en un tope;
  - una pila de agua para templar y un yunque con piedra de afilar al aire libre;
  - suelo de grava, tierra, toba y basalto.
- **La nave**:
  - de ladrillo y piedra negra, con tejado bajo de pizarra y dos grandes chimeneas que echan humo;
  - **dentro**: el gran horno (altos hornos y un crisol de lava), un canal de metal fundido por el suelo, yunques, piedras de afilar y un martillo gigante colgado de cadenas;
  - **al fondo**: un altar con forma de yunque con la **Piedra de Portal: Forja** (lleva a la arena del Gran Herrero) y un **cofre de provisiones de la etapa 2**.
- **Celda** pegada a la nave por el este: barrotes, puerta de hierro, jergón de paja, caldero, vela, cadenas y telarañas.
- **Enemigos** (aparecen una vez):
  - tres **Escorias** (dos en el patio y una en la nave);
  - un **Chispero** en un balcón;
  - un **Fundidor** que guarda el altar y lleva la **llave de la celda**.
- **No se puede picar** en supervivencia (igual que las demás).
- **Aparece sola en el mundo**:
  - en tierras baldías (badlands), colinas azotadas por el viento (normales, de grava y con bosque), sabana azotada y meseta de sabana;
  - en regiones de 704 bloques, en la mitad de ellas, a partir de 250 bloques del punto de aparición;
  - `/mundos natural generar forja [aqui]` construye una.

## Hilda, la herrera (tercera habitante)

Antigua aprendiz del Gran Herrero. Orgullosa, directa y con humor seco.

- **Aspecto**:
  - pelo cobrizo con pañuelo rojo y unas gafas de fragua subidas en la frente;
  - hollín en las mejillas;
  - camisa de lino con las mangas remangadas, brazales de cuero;
  - delantal de cuero con remaches y tiras cruzadas en la espalda;
  - un martillo colgado del cinturón, pantalón oscuro y botas;
  - cuerpo de brazos finos.
- Está presa en la celda de la fragua; se rescata igual que Ruda y Bruno (la llave del Fundidor, o forzar la cerradura con 4 de hierro si se perdió).
- **Su casa: la herrería**.
  - De ladrillo y piedra con entramado de roble oscuro y tejado de pizarra.
  - Chimenea alta de ladrillo con humo.
  - Porche lateral con un hogar de ascuas, piedra de afilar y una pila de agua.
  - Dentro: su cama naranja, horno, mesa de herrería, barriles, un cofre, alfombra y farol.
- 4 frases de presa, 3 al rescatarla y 8 de vecina. Habla del Gran Herrero, de cómo esquivar al Fundidor, de los Chisperos, y de Ruda y Bruno.
- Da pistas del volcán, del santuario, del fortín y de la fragua.
- Ruda y Bruno también dan pistas de la fragua.

## Tienda

- Nueva **Brújula a la Fragua Olvidada** (80 de Esencia, desde el primer vecino).
- **Con tres vecinos con casa se desbloquean** el Sobre de Oro y la manzana dorada.

## Arreglo de seguridad al mudarse

Antes, cuando un habitante se mudaba, su casa se desarmaba entera, también lo que el jugador hubiera guardado dentro.

Ahora:
- los cofres, barriles y hornos **con cosas dentro se quedan**;
- los atriles con libro también.

Lo vacío se desarma como siempre.

## Comandos (pruebas)

- `/mundos vecinos plantar [ruda|bruno|hilda]` y `/mundos vecinos mudar [ruda|bruno|hilda]`.

## Comprobado

- Compila (768 clases).
- **Plano revisado fuera del juego**:
  - la fragua tiene 1 «sala» sin acceso tolerada (la celda, cerrada a propósito), 0 trampas y la entrada transitable;
  - las otras 7 estructuras siguen en verde;
  - nada sin apoyo (fragua: 6102 bloques; herrería: 377).
  - Vistas del visor de la fragua y de la herrería.
- **Servidor de verdad (CI)**: las 23 comprobaciones nuevas en verde, y las de Ruda, Bruno, el santuario y el fortín siguen bien.
  - Fragua: la Piedra de Portal: Forja, la puerta y los barrotes de la celda, el cofre (etapa 2), la fogata de la chimenea, las escorias, el chispero y el fundidor con la llave.
  - Hilda presa; la llave cae al vencer al fundidor; al rescatarla se abre la puerta y queda libre.
  - Deshacer quita la fragua y sus enemigos.
  - La herrería crece (puerta, mesa de herrería y chimenea) y Hilda vive en ella.
  - Al mudarse: la casa se desarma, el cofre con hierro dentro se queda y el barril vacío se va.
  - Una fragua «natural» construida por pasos, con Hilda presa y su fundidor.
- **Juego real (cliente)**: 134 comprobaciones y 0 fallos, con 101 fotos.
  - La fragua por fuera, el patio y la nave, con escorias, chispero y fundidor.
  - La ventana de Hilda presa; el fundidor suelta la llave, la puerta se abre, Hilda queda libre (aviso, semilla) y sale de la celda.
  - Su herrería por fuera y por dentro, con ella dentro.
  - **Con tres vecinos** (Ruda, Bruno e Hilda) se compró el Sobre de Oro: la Esencia bajó 300 y llegó el sobre.
- Arreglos que salieron al probar:
  - la prueba de «vecina en casa» miraba una zona muy pequeña y a veces fallaba si la habitante salía a pasear (pasean hasta 7 bloques): ahora mira una zona más amplia;
  - la prueba del puente del volcán a veces miraba antes de que el derrumbe llegara a los pies: ahora espera un poco más (no cambia nada del juego).

## Pendiente

- Los otros 5 habitantes (uno por estructura).
- La torre todavía no sigue el modelo nuevo (estructura mediana con celda y NPC).
- Reciclar cartas repetidas con los habitantes («polvo de carta»).
