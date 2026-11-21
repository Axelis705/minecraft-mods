# Etapa 258 — Fortín de Escarcha, Bruno el cartógrafo y patas de mascotas unidas

Pedido (20 nov 2026):
- «sigue mejorando y puliendo cosas, de las estructuras»;
- «hay patitas de las mascotas que no están conectadas al cuerpo y se ven feas».

## Patas de las mascotas

- Herramienta nueva fuera del juego (`herramientas/PetGaps.java`): convierte el modelo 3D de cada mascota en cubitos y busca piezas que no tocan el cuerpo.
  - Revisa varias poses: quieta, cuatro momentos del paso y otra vez quieta.
  - Mide el hueco: los roces de menos de 3 cubitos no se notan.
  - Se revisaron las 58 mascotas, y también con primeros planos de la parte de abajo (frente, lado y andando).
- Arreglos:

  | Mascota | Qué pasaba | Arreglo |
  |---|---|---|
  | Conejo lunar | Dos patitas delanteras sueltas en el suelo, lejos de los brazos | Se quitaron: queda de pie sobre sus patas grandes |
  | Pingüino explorador | Los pies quedaban debajo del cuerpo con un hueco claro | Panza baja en forma de pera que llega a los pies |
  | Corazón de magma | Al andar, los pies se separaban del cuerpo | Tobillos de roca que unen pie y cuerpo |
  | Creeper | Rendija entre patas y cuerpo al andar | Patas un poco más altas, metidas bajo el cuerpo |
  | Gólem | Rendija entre piernas y cintura al andar | Piernas más altas que entran en la cintura |
  | Dragón de la noche | La aleta de luna de la cola flotaba separada | Pegada a la punta de la cola |

- Lo que sigue suelto a propósito: burbujas del slime, gotas de lava del corazón, gotas de lluvia de la nube, el mochi flotante del conejo, los libros del pequeño archimago.

## Fortín de Escarcha (la estructura del hielo)

La Ciudadela de cristal de antes era enorme y no tenía Piedra de Portal ni celda. Ahora la estructura del hielo es un fortín mediano en la nieve, hecho como el Santuario del Bosque (enemigos, la piedra al fondo y una celda con NPC).

- `/mundos estructura hielo`: el fortín nuevo.
  - Unos 35 × 43 bloques, con una sala de 17 de alto.
- `/mundos estructura ciudadela`: la ciudadela grande de antes, para videos.
- **Muralla**:
  - de pizarra con almenas y nieve entre ellas;
  - cuatro torres con chapitel de tejas y un cristal azul en la punta;
  - portón con el rastrillo subido, estandartes azules y farolas de fuego azul.
- **Patio nevado**:
  - un estanque helado (hielo compacto: no se derrite);
  - carámbanos de hielo azul y una fogata con bancos de tronco;
  - cajas, barriles y nieve suelta;
  - una armería bajo techo (yunque, piedra de afilar, mesa de herrería, alto horno, barriles);
  - un almacén con heno y leña.
- **Sala nórdica**:
  - de pizarra y abeto, con tejado muy inclinado y cuernos de madera cruzados en las puntas;
  - porche de madera en la entrada y rosetones azules en los hastiales.
  - **Dentro**: pilares, vigas con faroles azules, braseros de fuego azul, alfombra azul, mesas y bancos.
  - **Al fondo**: un estrado con la **Piedra de Portal: Hielo** (lleva a la dungeon del Lobo Ártico) entre cristales de hielo, estandartes y un **cofre de provisiones de la etapa 3**.
- **Celda**:
  - pegada a la sala por el este, con barrotes y puerta de hierro;
  - dentro, jergón de paja, caldero con nieve, vela y telarañas.
- **Enemigos** (aparecen una vez):
  - dos Lobeznos de escarcha en el patio y uno en la sala;
  - dos **Centinelas de escarcha**: el que vigila el estrado lleva la **llave de la celda**.
- **No se puede picar** en supervivencia (igual que las demás).
- **Aparece solo en el mundo**:
  - en llanuras nevadas, taigas nevadas, picos de hielo y arboledas;
  - en regiones de 768 bloques, en la mitad de ellas;
  - a partir de 300 bloques del punto de aparición.
  - `/mundos natural generar hielo [aqui]` construye uno.
- Las pistas y brújulas de los habitantes ya lo conocen: brújula al Fortín de Escarcha por 80 de Esencia.

## Centinela de escarcha (enemigo nuevo)

Soldado con armadura de placas cubierta de hielo, yelmo con visera en T y ojos azules que brillan, capa azul y espada.
- 34 de vida, armadura 6. Cada golpe enfría (frío del mod, escarcha en la pantalla) y frena un poco.
- **Onda helada**: si te quedas cerca, se agacha un momento con un crujido y escarcha en el suelo (aviso). Luego clava la espada y frena mucho a quien esté a menos de 4 bloques, con algo de daño y un empujón. Se esquiva alejándose.
- Suelta bolas de nieve, pepitas de hierro y a veces hielo azul. Tiene huevo generador en la pestaña del mod.

## Bruno, el cartógrafo (segundo habitante)

Viejo explorador polar:
- gorro de piel con orejeras y gafas de ventisca, barba blanca enorme y nariz roja;
- parka azul marino con ribete de piel, bufanda roja, brújula de latón y tubo de mapas a la espalda;
- cuerpo de brazos anchos (Ruda usa los finos).

Tono: viejo gruñón con humor, que exagera sus aventuras.
- Está preso en la celda del fortín; se rescata igual que Ruda (la llave del centinela, o forzar la cerradura con 4 de hierro si se perdió).
- **Su casa**: la cabaña del cartógrafo.
  - De troncos de abeto con las esquinas entrelazadas, tejado a dos aguas de lado a lado, chimenea de piedra con humo, banderín azul, leña apilada y un poste indicador.
  - Dentro: su cama, la mesa de cartografía, el atril, estanterías, un catalejo junto a la ventana y una alfombra roja.
- 4 frases de preso, 3 al rescatarlo y 8 de vecino. Habla de mapas, del Lobo Ártico y de Ruda («dice que mis mapas tienen más manchas de café que caminos»).
- Da pistas del volcán, del santuario y del fortín.
- **Con dos vecinos con casa se desbloquean** el Sobre de Plata, la Bellota dorada y la poción de resistencia al fuego larga.

## Comandos (pruebas)

- `/mundos vecinos plantar [ruda|bruno]` y `/mundos vecinos mudar [ruda|bruno]`.
- Los demás igual que en la etapa 257.

## Comprobado

- Compila (766 clases).
- **Plano revisado fuera del juego**:
  - el fortín tiene 1 «sala» sin acceso tolerada (la celda, cerrada a propósito), 0 trampas y la entrada transitable;
  - las otras 6 estructuras siguen en verde;
  - nada sin apoyo.
  - Vistas del visor del fortín y de la cabaña.
- **Patas**: el detector ya no encuentra piezas sueltas en las patas de ninguna mascota (solo los adornos que flotan a propósito). Hay imágenes de antes y ahora.
- **Servidor de verdad (CI)**: las 19 comprobaciones nuevas en verde, y las de Ruda y del santuario siguen bien.
  - Fortín: la Piedra de Portal: Hielo, la puerta y los barrotes de la celda, el cofre (etapa 3), el cristal de las torres, los lobeznos y el centinela con la llave.
  - Bruno preso; la llave cae al vencer al centinela; al rescatarlo se abre la puerta y queda libre.
  - Deshacer quita el fortín y sus enemigos.
  - La cabaña de Bruno crece (puerta y mesa de cartografía) y él vive dentro; al mudarse se desarma.
  - Un fortín «natural» construido por pasos, con Bruno preso y sus centinelas.
- **Juego real (cliente)**: 0 fallos, con fotos.
  - El fortín por fuera y por dentro, con los centinelas (2) y los lobeznos.
  - La ventana de Bruno preso. Al vencer al centinela cae la llave, la puerta se abre y Bruno queda libre (aviso, semilla) y sale de la celda.
  - Su cabaña por fuera y por dentro, con él dentro.
  - **Con dos vecinos** (Ruda y Bruno) se compró el Sobre de Plata: la Esencia bajó 140 y llegó el sobre.
- Arreglo que salió al probar: con la interfaz grande la tienda (ahora con 15 cosas) no cabía en la pantalla. Ahora se ven las filas que caben y el resto se baja con la rueda del ratón (con barra a la derecha).

## Pendiente

- Los otros 6 habitantes (uno por estructura).
- La forja y la torre todavía no siguen el modelo nuevo (estructura mediana con celda y NPC). La torre ya tiene su Piedra de Portal en el observatorio.
- Reciclar cartas repetidas con los habitantes («polvo de carta»).
