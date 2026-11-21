# Etapas 269 a 271: sellos por jugador, botín nuevo, Gran Yunque y gemas

Este documento resume lo que pidió Axelis después de la etapa 268 y cómo quedó hecho.

## Lo que se pidió

- Las llaves se siguen haciendo con fragmentos, pero no en la Mesa. Se hacen en un yunque grande con lava, con un juego de habilidad. Si fallas, pierdes 1 fragmento.
- Cada jugador solo puede abrir los cofres y celdas de su nivel. El orden es bosque → forja → hielo → volcán → magia. Si entras en una zona más alta, ves un aviso y te da lentitud.
- Hay 8 rarezas de cofre, una por tier.
- Todos los tiers tienen el mismo tipo de botín, incluidos los reparadores de su armadura y armas.
- No hay pociones. Hay objetos nuevos con la idea de Diablo 4 y Elden Ring, y también ideas de otros juegos (ver `ideas-botin-referencias.md`).
- Hay gemas como en Diablo 4. Las gemas vacías salen poco. Se infunden en otra mesa y después se engarzan en las armas.
- Hay pergaminos con historia.

## Etapa 269: sellos y botín nuevo

### Sellos por jugador (`world/Sellos.java`)

- **Etapa del jugador**: es el sello de jefe más alto que tiene en su Archivo.
- **Qué puede saquear**: tiers hasta su etapa + 1. Así, el tier 1 siempre está abierto. Los modos creativo y espectador no tienen sellos.
- **Tier de cada zona**:

  | Zona | Tier |
  |---|---|
  | Bosque y Santuario | 1 |
  | Forja y Fragua | 2 |
  | Hielo y Fortín | 3 |
  | Volcán (a 72 bloques o menos) | 4 |
  | Torre y Torre Arcana | 5 |

- **Si no puedes abrir algo**: no se abre y sale un mensaje que dice qué jefe te falta vencer. Esto aplica a cofres, barriles y celdas. Las llaves de celda no se gastan.
- **Si estás en una zona sellada**: tienes lentitud II y oyes el chillido de sculk con un aviso cada 12 s.

### Las 8 rarezas

- El título del cofre lleva el color de su rareza.
- Encima del cofre sale un haz de luz del color de la rareza. Es más alto cuanto mayor es el tier.
- El haz se apaga cuando tú ya lo abriste, o si el cofre está sellado para ti.

| Tier | Rareza | Color |
|---|---|---|
| 1 | Común | gris |
| 2 | Poco común | verde |
| 3 | Raro | azul |
| 4 | Épico | violeta |
| 5 | Legendario | dorado |
| 6 | Mítico | rosa |
| 7 | Estelar | lila |
| 8 | Celestial | celeste |

### Objetos nuevos

- **Reparadores de cada tier**:
  - Escama de Escarcha y su versión pura (hielo).
  - Brasa Templada y el Corazón de Brasa (volcán).
  - Runa de Remiendo y su versión mayor (magia).
  - Los tiers 6 a 8 también tienen los suyos.
- **Runas de Esencia I a VIII**: se rompen para conseguir Esencia (de 25 a 340). Con mayúsculas se rompe la pila entera.
- **Fragmento de corazón**: 4 fragmentos dan un corazón más de vida para siempre. El máximo es la etapa + 1, hasta 8 corazones.
- **Pergaminos de la crónica**: 3 historias por tier, del 1 al 5. Se leen como un libro. Al leer los tres de un tier recibes Esencia y un fragmento de corazón.
- **Pieza estrella**: cuando sale una, hay sonido, partículas y un mensaje. Es un fragmento de corazón, una llave entera, una gema vacía, una manzana encantada o un tótem.

### Botín por tipo de cofre

| Cofre | Qué trae |
|---|---|
| Provisiones (ANTE) | comida, antorchas, un reparador, una runa |
| Prueba (TRIAL) | materiales, runas, monedas, reparador al 35 %, fragmento al 50 % |
| Cerradura (LOCKED) | mejor cuanto más difícil es la cerradura; corazón desde el nivel 4 |
| Gran tesoro (TREASURE) | siempre un fragmento de corazón, 2 o 3 fragmentos de llave y un reparador mayor |
| Cripta (CRYPT) | una llave, corazón al 50 % |

## Etapa 270: el Gran Yunque de Fundición

- **Cómo se consigue**: se crea en la Mesa del Artífice (pestaña Crear). Pide un yunque, un alto horno y 2 bloques de hierro, más 100 de Esencia. La Mesa ya no forja llaves.
- **Cómo es el bloque**: tiene dos partes, como una cama. Una es el yunque con una llave al rojo y un martillo. La otra es un crisol con lava que brilla, con chimenea y fuelle. Si rompes cualquiera de las dos partes, sale el objeto.
- **Antes de empezar**: eliges la llave en una cuadrícula de 8 tiers. Necesitas 4 fragmentos de esa caja.
- **Las tres fases del juego de habilidad**:
  1. **Fundir**: mantén pulsado el fuelle y deja la aguja en la zona dorada hasta llenar la barra. Tienes un límite de 15 s.
  2. **Martillar**: golpea cuando el martillo pase por el punto dorado. Hay de 3 a 6 golpes.
  3. **Templar**: mete la llave al agua cuando brille en rojo cereza.
- **Resultados**:
  - **Bien**: gastas 4 fragmentos y un poco de Esencia, y recibes la llave.
  - **Perfecto** (sin fallos): además, el yunque te devuelve 1 fragmento.
  - **Falla** (2 fallos, o te quedas sin tiempo al fundir, o sales a mitad): la llave se quiebra y pierdes 1 fragmento.
- **Dificultad por tier**: cuanto más alto el tier, más difícil. En las pruebas simuladas, el tier 1 sale perfecto un 82 % de las veces y el tier 8 falla un 43 %.

## Etapa 271: las gemas

### Cómo se consiguen

- **Gema vacía**: sale con poca probabilidad. Cuenta como pieza estrella.

  | Cofre | Probabilidad |
  |---|---|
  | Prueba | 4 % |
  | Cerradura | 4 % por cada nivel de cerradura |
  | Gran tesoro | 35 % |
  | Cripta | 45 % |

- **Pureza de la gema vacía**: depende del tier del cofre.

  | Tiers | Pureza |
  |---|---|
  | 1–2 | Agrietada |
  | 3–4 | Tallada |
  | 5–6 | Pulida |
  | 7–8 | Perfecta |

- **Esencias de infusión**: hay una por elemento y salen del elemento de su tier. Salen más a menudo que las gemas vacías:
  - provisiones: 8 %;
  - prueba: 25 %;
  - cerradura: según el nivel;
  - gran tesoro: siempre 1 o 2;
  - cripta: siempre 2;
  - barriles: también pueden traerlas.

### La Mesa de Gemas

Se crea en la Mesa del Artífice: 8 fragmentos de amatista, una mesa de herrería, un bloque de lapislázuli y 80 de Esencia.

- **Infundir**: gema vacía + esencia = gema de ese elemento, con la misma pureza. Cuesta 20 de Esencia por nivel de pureza.
- **Engarzar**: pones un arma (espada, hacha o tridente) y una gema. Si el arma ya tenía otra gema, esa vuelve al hueco.
- **Quitar**: saca la gema del arma. Cuesta 15 de Esencia.
- **Al cerrar**: lo que dejaste en la mesa vuelve a tu inventario.

### Los 8 elementos (un efecto al golpear, más fuerte cuanto más pura)

| Tier | Gema | Efecto |
|---|---|---|
| 1 | Veneno | envenena (veneno II desde Pulida) |
| 2 | Filo | +7 % de daño por pureza |
| 3 | Escarcha | lentitud |
| 4 | Fuego | prende fuego |
| 5 | Arcana | +0,8 de daño por pureza |
| 6 | Vampírica | te cura parte del daño |
| 7 | Rayo | 6 % por pureza de que caiga un rayo (+2 de daño por pureza) |
| 8 | Vacío | marchita |

- **Descripción del arma**: muestra `◆ Gema … (pureza)` y su efecto.
- **Dibujos**: cada gema tiene 4 dibujos, uno por pureza. La Agrietada tiene grietas, la Pulida brilla y la Perfecta lleva ribete de oro.
- **Armas**: Axelis quiere cambiarlas más adelante. Las gemas se guardan en el arma como un dato (`MdcGema`), así que servirán igual con las armas nuevas.

## Pruebas

| Prueba | Qué comprueba |
|---|---|
| `mundos prueba botin` | sin pociones; runas y reparadores del tier correcto; corazón en cada gran tesoro; gemas vacías con la pureza correcta; esencias del elemento del tier; gran tesoro y cripta siempre con esencia |
| `mundos prueba gemas` | con un jugador falso: engarza los 8 elementos y golpea a un cerdo para comprobar cada efecto; hace el camino completo de la Mesa de Gemas (infundir, engarzar y quitar) con su coste |
| Comandos de CI | el Gran Yunque se pone con sus dos partes, se rompe entero y suelta el objeto; la Mesa de Gemas se coloca |
| Prueba con el cliente: sellos | olvidar el progreso → lentitud en la zona sellada → el cofre no se abre |
| Prueba con el cliente: botín | corazones, runas y pergaminos |
| Prueba con el cliente: Gran Yunque | forja perfecta y forja fallida |
| Prueba con el cliente: Mesa de Gemas | infundir, engarzar, la descripción del arma y el cerdo que arde al golpearlo |

## Arreglos que salieron en las pruebas

- **Sellos**: el comando de prueba que borra el progreso ahora también quita los sellos del inventario. Si no, el Archivo los volvía a apuntar en 2 segundos.
- **Llave de la celda**: ya no se quema en la lava. El fundidor de la isla de los hornos podía soltarla junto a la lava y se perdía.
- **Mesa de Gemas**:
  - al abrirla, la Esencia que muestra está al día;
  - los textos de ayuda tienen 3 líneas y ya no se cortan.
- **Resultado final de la prueba con el cliente**: 215 comprobaciones y 0 fallos.

## Qué queda para después

- El contenido de los tiers 6 a 8.
- Más ideas de la lista: cofres mímicos, óbolos, objetos arrojadizos, cenizas de invocación y piedras de forja cuando cambien las armas.
