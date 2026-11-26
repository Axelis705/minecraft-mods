# Navidad de Axelis

Una Navidad enorme para Minecraft 1.20.1 (Forge 47.4.10): decoraciones, regalos, posadas con piñatas, comida navideña, Santa y sus
duendes, renos que se montan, muñecos de nieve malvados, el Krampus de jefe, villancicos y la aldea de Santa.

Usa Axelis Core (va dentro del jar).

## La temporada

Muchas cosas solo pasan en temporada: **del 1 de diciembre al 6 de enero** (hora del centro de México). Para probar o para jugar fuera de
fecha: `/navidad temporada si` (o `no`, o `auto` para volver a la fecha real).

- Al entrar: "Faltan N días para Navidad" (desde 30 días antes); el 24 avisa que es Nochebuena.
- **25 de diciembre**: al entrar, Santa deja una caja de regalo envuelta a tu nombre con un premio épico.
- **6 de enero**: rosca de Reyes. Al comerla, a veces sale el muñequito (y te toca invitar los tamales).
- **De noche**: el trineo de Santa cruza el cielo con cascabeles y suelta regalos misteriosos sobre los jugadores (en Nochebuena, tres veces).
- Muñecos de nieve malvados (de noche, en la nieve) y hombres de jengibre (de día, en praderas y bosques) solo salen solos en temporada.

## Cosas

- **57 bloques de decoración** (pestaña "Navidad: decoración"): árboles, esferas, luces, coronas, calcetines, muérdago, nochebuenas,
  regalos, muñeco de nieve, cascanueces, bastones gigantes, trineo, chimenea, ponche, farolitos, papel picado, piñata de estrella...
  Se consiguen con la **caja de adornos** (papel + tinte rojo + tinte verde + pepita de oro = 4) en el **cortapiedras**.
- **Comida**: galleta de jengibre, bastón de caramelo, dulce, tejocote, mandarina, caña, tamal, buñuelo, pavo navideño, rosca de Reyes;
  bebidas que quitan el frío: chocolate caliente, ponche, champurrado.
- **Regalo misterioso**: común, raro (azul) o épico (morado), con confeti.
- **Caja de regalo**: ponla y dale clic con lo que quieras regalar; con una etiqueta con nombre queda "Para: alguien" y solo esa persona la
  abre. Al romperla se lleva lo que tiene adentro.
- **Calendario de Adviento**: una puerta por día del 1 al 24 de diciembre (el 24 es la grande). Cada quien lleva su cuenta con el mundo.
- **Piñata** (se cuelga del techo), **palo de piñata** y **venda** (te tapa los ojos y te marea).
- **Ropa** (se ve encima de tu skin, clic derecho para ponértela o quitártela): gorro de Santa, gorro de duende, cuernos de reno,
  suéter de reno, de pinos y de copos, traje de Santa (con barba).
- **Discos** (villancicos de dominio público en caja de música): Jingle Bells, We Wish You a Merry Christmas, Noche de paz, Deck the Halls.
- **Luz de bengala**: chispas en tu mano.
- **Campana del Krampus**: tócala de noche para llamarlo. **Cuerno del Krampus**: trofeo.

## Criaturas

- **Muñeco de nieve malvado**: avienta bolas de nieve que congelan. Suelta nieve, carbón, dulces y a veces la campana del Krampus.
- **Reno**: con 3 zanahorias (o manzanas, trigo) se hace tu amigo; con la mano vacía lo montas. Corre rápido, salta alto y de noche en
  temporada cae despacito, como si volara.
- **Hombre de jengibre**: corre y se burla. Si lo alcanzas, suelta galletas.
- **Krampus** (jefe, 360 de vida, 2 fases): cadenazo, pisotón (salta para esquivarlo), carbones encendidos, rugido que llama muñecos de
  nieve; a la mitad se enfurece: el saco (un círculo te persigue y te aturde) y cascabeles malditos. Al vencerlo, regalos épicos para
  todos los que estaban cerca, su cuerno, diamantes y a veces un disco.

## La aldea de Santa

`/navidad aldea` la construye donde estés parado (61 x 61 bloques, limpia el lugar): plaza nevada con un pino gigante lleno de luces y
regalos, casa de Santa (chimenea, calcetines, árbol), taller de juguetes, casita de jengibre de la Señora Claus, establo con renos.
`/navidad aldea sin_gente` la hace sin poner a nadie.

### La gente (tienda, misiones, amistad)

- **Santa**: misiones (galletas y leche, muñecos rebeldes, atrapar hombres de jengibre, romper una piñata, el espíritu de dar, la noche
  del Krampus, cartas diarias). Premios: regalos, discos, traje de Santa.
- **Señora Claus**: vende comida navideña; misiones de ingredientes, pavo y horno diario.
- **Chispita, el duende juguetero**: cobra en **dulces**: cajas de adornos, piñatas, cajas de regalo, calendario, regalos misteriosos, discos.
- **Botones, el duende sastre**: gorros, suéteres y el traje de Santa.
- **Copito, el duende de los renos**: zanahorias, manzanas, heno; misión de hacerte amigo de un reno.

Se pueden poner sueltos: `/navidad gente santa|senora_claus|duende_juguetero|duende_sastre|duende_renos`.

## Comandos (operadores)

| Comando | Qué hace |
|---|---|
| `/navidad` | Dice si funciona y si es temporada |
| `/navidad temporada si\|no\|auto` | Forzar la temporada |
| `/navidad calendario dia <n>` | Fingir el día de diciembre (0 = el de verdad) |
| `/navidad regalo [jugadores]` | Dar regalos misteriosos |
| `/navidad pinata` | Poner una piñata enfrente |
| `/navidad posada` | Posada: piñata, palo y venda para todos, la canción y confeti |
| `/navidad intercambio` | Amigo secreto: a cada quien le llega en secreto a quién le regala |
| `/navidad trineo` | Mandar el trineo de Santa sobre ti |
| `/navidad krampus` | Llamar al Krampus |
| `/navidad aldea [sin_gente]` | Construir la aldea de Santa |
| `/navidad gente <quien>` | Poner a alguien del Polo Norte |
| `/navidad ropa <prenda>` | Ponerte o quitarte una prenda |

## Herramientas (para regenerar los recursos)

- `herramientas/deco/build.py`: los 57 bloques de decoración, la caja de regalo y el calendario (modelos, texturas, botín, nombres).
- `herramientas/iconos.py`: iconos de los objetos. `herramientas/ropa.py`: la ropa. `herramientas/pieles.py`: pieles de la gente.
- `herramientas/criaturas.py`: muñeco malvado, reno (sobre la Bestia del Core) y Krampus (sobre el Coloso del Core).
- `herramientas/musica.py`: villancicos y efectos (tarda unos 5 minutos). `herramientas/recetas.py`, `herramientas/lang_cosas.py`.
