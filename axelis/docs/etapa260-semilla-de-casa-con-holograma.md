# Etapa 260 — La Semilla de casa es un bloque con holograma

Pedido (20 nov 2026): «recuerda que después de rescatar al NPC de la estructura, te dé un bloque que ese lo pones en tu casa a un lado y te sale un holograma, de las medidas de su casa, para que no haya problemas».

Era lo acordado en la etapa 238 («te dan un bloque; ese bloque se coloca para construirles su casa cerca de la del jugador»). Hasta ahora la semilla era un objeto que, al usarlo, hacía crecer la casa al instante, sin ver antes dónde ni cuánto ocupaba.

## Cómo funciona ahora

1. Al rescatar a un habitante (Ruda, Bruno, Hilda...) te da su **Semilla de casa**, igual que antes.
2. La semilla **se pone en el suelo como un bloque**: un montoncito de tierra con musgo y un brote con un capullo dorado que brilla y suelta chispitas.
3. Mientras está puesta sale un **holograma**:
   - la casa entera, bloque a bloque, en luz azulada, justo donde va a crecer;
   - un recuadro azul con el tamaño total;
   - en **rojo**, cada bloque del mundo que estorba (árboles, muros, cofres...), con su recuadro;
   - en **naranja**, los sitios donde debajo del suelo de la casa hay un hueco de más de 3 bloques (terreno muy desigual);
   - un cartel encima de la semilla:
     - «Casa de Ruda, la herbolaria»;
     - «9 × 10 bloques · 10 de alto» (el ancho, el fondo y el alto que ocupará);
     - y «¡Cabe! Clic derecho en la semilla para que crezca», o «4 bloques estorban (en rojo)», o «Terreno muy desigual (en naranja)».
4. **Clic derecho** en la semilla: si cabe, la casa crece, la semilla se gasta y el habitante se muda. Si no cabe, dice cuántos bloques estorban y la semilla se queda.
5. **Agachado, con la mano vacía + clic derecho**: la casa gira 90° (el holograma gira con ella).
6. **Si la rompes, vuelve a tu mano**: se puede poner en otro sitio todas las veces que quieras.

Otros detalles:
- La casa crece hacia donde mirabas al poner la semilla, con la puerta hacia ti; la semilla queda en el camino de la entrada.
- No se la llevan las explosiones, ni los pistones, ni el agua.
- Si ya tiene casa, si sigue preso o si estás en una dimensión del mod, no deja poner la semilla y dice por qué.
- El holograma se ve desde 64 bloques. Lo que estorba se revisa cada medio segundo, con las mismas reglas que usa el juego al construir.

## Comandos (pruebas)

- `/mundos vecinos semilla <tipo>`: pone la semilla de ese habitante donde estás, mirando hacia donde miras.
- `/mundos vecinos crecer`: hace crecer la semilla que hay donde estás (como el clic derecho).
- `/mundos vecinos plantar` sigue construyendo la casa directamente, sin semilla.

## Comprobado

- Compila (774 clases).
- **Servidor de verdad (CI)**: las 7 comprobaciones nuevas en verde, y todas las anteriores siguen bien.
  - La semilla de Hilda queda puesta como bloque, mirando al sur, y sabe que es de Hilda.
  - Con un tronco en medio, la casa no crece y la semilla sigue puesta.
  - Al quitar el tronco, crece: aparece la puerta, la semilla se gasta e Hilda vive dentro.
- **Juego real (cliente)**: 142 comprobaciones y 0 fallos, con 104 fotos.
  - Al poner la semilla de Ruda: queda como bloque, sabe que es de Ruda, se gasta de la mano y el holograma no marca nada en rojo en un sitio libre.
  - Con un tronco de 3 bloques en la pared de la ventana, el holograma marca exactamente esos 3 en rojo y la casa no crece.
  - Agachado y con clic derecho, la casa gira (de sur a oeste) y el holograma gira con ella.
  - Con clic derecho crece la casa, la semilla desaparece y Ruda vive dentro.
  - Bruno e Hilda también se mudan con su semilla puesta en el suelo.
  - Fotos del holograma: en un sitio libre, con el tronco en rojo y girado.
- Arreglos que salieron al probar:
  - en la primera prueba las fotos del holograma salían mirando al suelo, porque el jugador caía mientras tanto: ahora se toman en modo espectador;
  - el holograma se veía casi gris: ahora es más azul.

## Pendiente

- Los otros 5 habitantes (uno por estructura).
- La torre todavía no sigue el modelo nuevo (estructura mediana con celda y NPC).
- Reciclar cartas repetidas con los habitantes («polvo de carta»).
