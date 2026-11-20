# Etapa 257 — Ruda, la herbolaria (el NPC de la celda del santuario)

Pedido (20 nov 2026): «okey has el npc». Se hizo como estaba acordado en las etapas 235 y 238:
- un habitante por estructura, encerrado en su celda;
- al rescatarlo te da un bloque/objeto para que su casa crezca cerca de la tuya;
- todos abren la **misma tienda**, y con más vecinos se desbloquean más cosas (como en Terraria);
- tono «misterioso con humor seco»; cuenta nuestra propia historia, vende cosas y dice dónde están otras estructuras.

## Quién es

**Ruda, la herbolaria**: una señora de pelo gris con trenza, ojos verdes y media sonrisa. Lleva capucha y chal verdes con ribete de hojas y una flor rosa, vestido marrón con delantal, cinturón con hebilla, bolsa de cuero y botas. Es un poco más bajita que un jugador y tiene el cuerpo de brazos finos (como Alex), con piel propia de 64 × 64.
- No se le puede hacer daño, no desaparece y los monstruos no la atacan.
- Si muere de todas formas (solo con `/kill` o cayendo al vacío), vuelve a aparecer en su sitio.
- Hay **una sola Ruda por mundo**: si la rescatas en un santuario, en los demás la celda queda vacía.

## Cómo se rescata

1. En la celda del Santuario del Bosque, Ruda está presa. Si le hablas (clic derecho a través de los barrotes), te cuenta quién es y que **la llave la tiene el Raizal que guarda el altar**.
2. Al vencer al Raizal, suelta la **Llave de la celda** (brilla en el suelo para que no se pierda).
3. Usas la llave en la puerta de hierro de la celda (o directamente en Ruda). La puerta se abre, Ruda sale al salón y te da su **Semilla de casa**. Sale el aviso «¡Ruda, la herbolaria está libre!».
4. Si la llave se pierde (lava, etc.) y el Raizal ya no está, Ruda ofrece **forzar la cerradura con 4 lingotes de hierro**.

## Su casa

- La **Semilla de casa** se usa sobre el suelo: delante de ti crece una cabaña, con la puerta mirándote. Ruda se muda a ella.
- Hace falta un sitio despejado de unos 9 × 10 bloques, sin árboles ni construcciones, y no muy desigual. Si no se puede, te dice por qué: no hay espacio, terreno desigual, hay alguien en medio, zona protegida, dentro de una dungeon...
- **La cabaña** tiene:
  - zócalo de piedra con musgo, muros de adobe con marco de roble oscuro y tejado de abeto a dos aguas;
  - una chimenea con humo, un camino con dos faroles, flores delante y un huerto de hierbas a un lado;
  - dentro: su cama, un barril con helechos, la mesa de trabajo, la compostera, un banco, vigas con hierbas colgando (lianas con bayas que dan algo de luz) y un farol.
- Ruda pasea por la casa y el jardín, abre y cierra la puerta. Cuando le hablas, se queda quieta mirándote.
- La casa **no** está protegida: es tuya, puedes cambiarla.
- **Mudarse**: desde su menú (pide confirmar). La casa se desarma sola y el suelo vuelve a ser pasto. Lo que tú le hayas añadido se queda. Ruda te da otra semilla para plantarla en otro sitio.

## La ventana de diálogo

Abajo de la pantalla sale un cuadro de diálogo de juego de rol (el mundo se sigue viendo):
- su retrato en 3D, que te sigue con la mirada;
- su nombre y su estado («En la celda», «Sin casa», «Vive aquí»);
- el texto, que se escribe letra a letra con un «bip» suave. Con clic, espacio o «Seguir» pasas a la siguiente frase.
- Los botones cambian según su estado:
  - **Presa**: Seguir, Forzar (si la llave se perdió) y Adiós.
  - **Libre**: Seguir, Semilla de casa (si perdiste la tuya) y Adiós.
  - **Vecina**: Seguir, Tienda, Pistas, Mudarse y Adiós.
- Frases: 4 de presa, 3 al rescatarla y 8 de vecina. Tratan del Rey Brote, la Forja, el volcán, la torre del Archimago, la ciudadela de hielo («ahí también hay alguien encerrado»)... y algo de humor seco.

## La tienda (la misma para todos los habitantes)

- Se paga con **Esencia** (la de la Mesa del Artífice).
- Se ve tu Esencia arriba. Al pasar el cursor sale el objeto y su precio. Lo bloqueado se ve en gris con un candado y cuántos vecinos faltan.
- Solo vende cuando ya vive en su casa («no vendo nada a la intemperie: el rocío arruina las hierbas»).

| Con | Objeto | Esencia |
|---|---|---|
| 1 vecino | 3 Bayas del santuario | 15 |
| 1 vecino | Bellota | 35 |
| 1 vecino | Poción de curación | 25 |
| 1 vecino | Poción de regeneración | 40 |
| 1 vecino | 4 Brotes de roble | 10 |
| 1 vecino | 8 de Polvo de hueso | 10 |
| 1 vecino | Sobre Básico de cartas | 60 |
| 1 vecino | Brújula: Santuario del Bosque | 40 |
| 1 vecino | Brújula: Volcán | 120 |
| 2 vecinos | Sobre de Plata | 140 |
| 2 vecinos | Bellota dorada | 200 |
| 2 vecinos | Poción de resistencia al fuego (larga) | 60 |
| 3 vecinos | Sobre de Oro | 300 |
| 3 vecinos | Manzana dorada | 120 |

- Las **brújulas** son brújulas normales que apuntan a la estructura de ese tipo más cercana: una ya construida o el sitio donde aparecerá. Llevan su nombre.
- **Pistas**: Ruda te dice hacia dónde y a cuántos bloques está el volcán o el santuario más cercano (va alternando). Ejemplo: «hay un volcán hacia el noreste, a unos 900 bloques».

## Comandos (para probar)

| Comando | Qué hace |
|---|---|
| `/mundos vecinos estado` | Cómo está cada habitante y dónde. |
| `/mundos vecinos liberar` | Rescata al preso más cercano. |
| `/mundos vecinos plantar` | Planta la casa de Ruda donde estás, mirando a donde miras. |
| `/mundos vecinos mudar` | Ruda se muda (la casa se desarma). |
| `/mundos vecinos reiniciar` | Todos vuelven a estar presos (en los santuarios nuevos vuelve a haber preso). |

## Para los siguientes habitantes

Todo está preparado para los otros 7:
- cada uno es un tipo en `NpcKinds`, con su color, el diseño de su casa y cuántas frases tiene;
- su piel va en `textures/entity/npc/<id>.png`;
- en el plano de su estructura se pone `mob(..., "habitante", "<id>")`, y el guardián de la llave lleva la marca `llave_celda`;
- la tienda es la misma: los objetos de 2 y 3 vecinos se desbloquean al rescatar más.

## Comprobado

- Compila (761 clases). Conectividad: las 6 estructuras en verde.
- Vista del visor de la casa por fuera y por dentro.
- **Servidor de verdad (CI)**: las 15 comprobaciones en verde.
  - Ruda presa en la celda, y el Raizal con la marca de la llave.
  - Al vencer al Raizal cae la llave.
  - Al rescatarla se abre la puerta y Ruda queda libre; sigue ahí aunque se deshaga el santuario.
  - Se planta la casa: la puerta, el farol y Ruda dentro como vecina. Hay una sola Ruda.
  - Al mudarse, la casa se desarma, el suelo vuelve a ser pasto y Ruda queda libre.
  - `reiniciar` la quita, y un santuario natural nuevo vuelve a tener a Ruda presa.
- **Juego real (cliente)**: todas las comprobaciones en verde (0 fallos), con fotos.
  - Ruda presa y su ventana de diálogo.
  - Al vencer al Raizal cae la llave; con la llave se abre la puerta.
  - Ruda queda libre, te da la semilla, sale el aviso y ella sale de la celda al salón.
  - Crece la casa con la semilla (la semilla se gasta) y Ruda vive dentro. Fotos de fuera y de dentro.
  - Su ventana de vecina y la tienda. Se compraron 3 bayas: la Esencia bajó de 969 a 954 y llegaron las bayas.
  - La brújula comprada apunta al santuario y lleva su nombre.
  - Pista: «hay un volcán hacia el suroeste, a unos 300 bloques».
  - Al mudarse, la casa se desarma y Ruda queda libre con otra semilla.
- Arreglos que salieron al probar:
  - La vela de la celda estaba justo detrás de la puerta y no dejaba salir a Ruda: se movió a la esquina de los barrotes.
  - Ruda ahora vuelve a su sitio por el camino directo. Si algo le tapa el paso, a los 15 segundos aparece allí entre hojas.

## Pendiente

- Los otros 7 habitantes (uno por estructura).
- Reciclar cartas repetidas con el habitante para obtener «polvo de carta».
