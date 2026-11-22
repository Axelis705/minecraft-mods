# Etapa 281: armas nuevas

Axelis dijo que las armas no le gustaban. Le pregunté qué cambiar y respondió:

- No le gustaba cómo se veían, que hubiera muchas parecidas ni sus habilidades.
- Quería **varios tipos de arma**, cada uno con su forma de pelear.
- La habilidad especial debía seguir **gastando almas** (clic derecho).
- Debían seguir saliendo **de las cajas**.

## Qué cambió

- Las 16 espadas de los sets (dos por caja, todas en diagonal y parecidas) ya **no salen de las cajas**.
- En su lugar, cada caja da **5 armas distintas**, todas de su tema: 8 cajas × 5 tipos = **40 armas**.
- Picos, hachas de talar y palas siguen igual.
- **Las espadas viejas siguen existiendo**, así nadie las pierde en sus mundos:
  - ya no están en la pestaña creativa ni en el Archivo de la Mesa;
  - siguen funcionando igual si ya las tenías.

## Los cinco tipos

El daño de cada golpe se calcula desde el daño de referencia del tier (D), que es el de la espada del set B de la caja: 8, 14, 20, 26, 32, 38, 44, 50. Así el equilibrio de los jefes y de las dungeons (`LoadoutRef`) no cambia.

| Tipo | Daño por golpe | Golpes/s | Estilo (pasivas) | Especial (clic derecho) | Almas | Enfriamiento |
|---|---|---|---|---|---|---|
| Espada | D | 1,6 | **Barrido**: los golpes cargados hieren a los de al lado (40 %). **Contraataque**: si te golpean, tu siguiente golpe hace +35 %. | **Tajo creciente**: una onda cortante avanza 10 bloques e hiere a todo lo que cruza (1,5 D). | 8 | 6 s |
| Hacha de guerra | 1,35 D | 1,0 | **Quiebra armaduras**: +2,5 % por punto de armadura del enemigo (hasta +50 %). **Rompe escudos**. | **Torbellino**: giras 2 s y golpeas 5 veces a todos los de alrededor (0,6 D cada vez), atrayéndolos. | 12 | 10 s |
| Martillo | 1,7 D | 0,8 | **Aturde** con los golpes cargados. **Onda**: los de alrededor reciben un 35 %. | **Terremoto**: saltas y al caer golpeas el suelo (radio 5, 1,2 D): los lanzas por el aire y los aturdes 2 s. | 15 | 12 s |
| Lanza | 0,95 D | 1,4 | **Alcance** +1,5 bloques. **Carga**: +30 % corriendo; el golpe cargado atraviesa al de detrás (50 %). | **Embestida**: te lanzas 9 bloques atravesando y apartando a todos (1,6 D). | 10 | 8 s |
| Dagas | 0,6 D | 2,5 | **Puñalada**: por la espalda +75 %. **Ágiles**: golpes muy seguidos sin perder daño. | **Paso sombrío**: apareces a la espalda del enemigo que miras y lo apuñalas (2,5 D); 3 s de golpes por la espalda. Sin enemigo, avanzas y te vuelves invisible un momento. | 10 | 8 s |

- El enfriamiento baja un 4 % por cada tier.
- Los jefes del mod se aturden mucho menos (un tercio del tiempo).
- Solo la espada hace el barrido de Minecraft; el hacha de guerra deja sin escudo, como el hacha normal.

## El elemento de cada tier

Se aplica en todos los golpes y en la especial.

| Caja | Elemento | Qué hace |
|---|---|---|
| Naturaleza | Veneno | envenena (a los no-muertos y arañas, a los que no les afecta, les pega un 10 % más) |
| Forja | Sangrado | el enemigo sangra 3 s (3 × 10 % del golpe, sin armadura) |
| Hielo | Escarcha | ralentiza y congela |
| Fuego | Fuego | prende fuego 4 s |
| Magia | Arcano | +15 % de daño mágico que ignora la armadura |
| Dragón | Vampiro | te curas un 7 % de lo que pegas |
| Cielo | Rayo | 25 % de soltar un rayo que salta a 3 enemigos |
| Abismo | Vacío | marchita y a veces desgarra (+25 %) |

Además, las armas de las cajas altas dan **más almas** al matar (×1,5 desde Hielo, ×2 desde Magia, ×3 desde Cielo) y **Botín** (+1 desde Hielo, +2 desde Dragón, +3 en Abismo).

## Nombres

| Caja | Espada | Hacha de guerra | Martillo | Lanza | Dagas |
|---|---|---|---|---|---|
| Naturaleza | Espada Silvestre | Hacha de Raíz | Mazo de Tronco | Lanza de Espino | Dagas de Zarza |
| Forja | Espada de Acero Templado | Hacha del Herrero | Martillo de Yunque | Lanza Remachada | Dagas de Chispa |
| Hielo | Espada Glaciar | Hacha de Escarcha | Martillo Ártico | Lanza Carámbano | Dagas de Nieve |
| Fuego | Espada de Ascua | Hacha Volcánica | Martillo de Magma | Lanza Ígnea | Dagas de Brasa |
| Magia | Espada Rúnica | Hacha Hechizada | Martillo Arcano | Lanza Mística | Dagas Encantadas |
| Dragón | Espada Dracónica | Hacha Escamada | Martillo del Dragón | Lanza Colmillo de Dragón | Dagas de Rubí |
| Cielo | Espada Celeste | Hacha de Tormenta | Martillo del Trueno | Lanza Relámpago | Dagas de Viento |
| Abismo | Espada del Vacío | Hacha Abisal | Martillo Cósmico | Lanza Eclipse | Dagas Umbrías |

## Aspecto

- Pixel art nuevo de 32×32, con contorno oscuro, luz desde arriba a la izquierda y arista en las hojas.
- Cada tipo tiene su silueta:
  - la espada, con una guarda distinta en cada tier (hojas, cruz, cristales, cuernos de lava, anillo rúnico, cuernos de dragón, alas, estrellas);
  - el hacha, una media luna (doble desde Fuego);
  - el martillo, una cabeza ancha;
  - la lanza, un asta fina con borla;
  - las dagas, dos cruzadas en el inventario y una sola en la mano.
- Animación: cada 3 segundos un brillo recorre el filo, y la lava, las runas, los rayos y las estrellas laten.
- En la mano, el hacha, el martillo y la lanza se ven más grandes que una espada.
- Generador: `axelis/herramientas/gen_armas281.py` (texturas, modelos y la hoja de muestra).

## Animación al atacar

Axelis vio la hoja de muestra («me gustan las nuevas armas») y pidió animación al atacar.

- **Primera persona** (`client/ArmaAnim`, con `applyForgeHandTransform` de Forge): cada tipo tiene su golpe, hecho con fotogramas clave suavizados.
  - Espada: tajo en diagonal, de arriba a la derecha hacia abajo a la izquierda.
  - Hacha de guerra: la levanta y la baja.
  - Martillo: subida más alta y golpe más pesado hacia abajo.
  - Lanza: se echa atrás y embiste hacia delante.
  - Dagas: puñalada corta y rápida.
- **Tercera persona**: el hacha de guerra, el martillo y la lanza se llevan con las dos manos.
  - Es una pose nueva de Forge (`ArmPose.create`), que se crea al arrancar el cliente.
  - El brazo que no lleva el arma acompaña el golpe, para que no suelte el mango.
- **Estela del golpe** (servidor, la ven todos), del color del tier, en los golpes cargados (las dagas, en todos):
  - tajo en diagonal con la espada;
  - tajo vertical con el hacha de guerra y el martillo;
  - línea recta con la lanza;
  - cortes pequeños con las dagas.

## Cajas

- Las 5 armas de una caja comparten rareza, la del set A de esa caja.
- Pesan 0,12 cada una: juntas salen algo más que las dos espadas de antes, para que sea posible conseguir el tipo que quieres.
- La regla «lo mejor sale menos» se sigue cumpliendo en las 8 cajas.

## Otros arreglos

- Las capacidades de las herramientas (Prisa, Veta, Botín, Más almas…) no tenían texto y se veía la clave en la descripción. Ya tienen texto en los tres idiomas.
- La Bellota y sus variantes reparan también las armas nuevas de su caja.

## Pruebas

| Prueba | Qué comprueba |
|---|---|
| `mundos prueba armas` | las 40 armas: daño, velocidad, alcance, barrido y gemas; cada caja da sus 5 armas y ninguna espada vieja; el orden de rarezas; contraataque, barrido, quiebra armaduras, aturdir y onda, carga y atravesar, puñalada; veneno, fuego, escarcha, vacío, vampiro y sangrado; matar da almas; la especial gasta sus almas, se enfría y no sale sin almas; Tajo, Torbellino, Terremoto (con aturdimiento), Embestida y Paso sombrío hieren |
| Prueba con el cliente | las 5 armas en la mano (primera persona), martillo y lanza en tercera persona, las 40 en un cofre, el Tajo creciente y el Terremoto con clic derecho sobre muñecos |
