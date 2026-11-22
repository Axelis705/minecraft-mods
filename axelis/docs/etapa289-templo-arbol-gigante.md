# Etapa 289 — El Gran Templo del Bosque es un árbol gigante

Pedido de Axelis: «el Templo, rehacerlo como un árbol gigante».

## Por fuera (`world/DesignBosque.java`, rehecho entero)
- Un **tronco hueco** de unos 22 bloques de ancho, que en la base se abre como una campana hasta unos 36, y **56 de alto**.
- **Nueve raíces enormes** salen de la base y se hunden en el suelo. Entre las dos de delante está la **entrada**: un arco de 5 de ancho
  con marco de corteza pelada y faroles.
- Arriba, **la Copa**: siete ramas gruesas que salen hacia fuera y hacia arriba, cada una con su bola de hojas (roble y azalea con
  flores), y una copa grande encima.
- Corteza de roble con manchas de roble oscuro y musgo. Un camino de tierra y musgo entre faroles lleva a la entrada; flores y helechos
  por el claro.

## Por dentro
| Piso | Altura | Qué hay |
|---|---|---|
| **Salón de las Raíces** | 0 | Sala enorme en forma de cúpula, con suelo de musgo y tierra, nudos de raíces, un estanque con nenúfares, hongos de luz en la pared, barriles, el Altar Maldito y la bajada a la cripta |
| **Cripta de las raíces** | -10 | Bajo el tronco: raíces colgando, velas, un sarcófago con musgo y el cofre con cerradura de nivel 3 |
| **Biblioteca de corteza** | 14 | Estanterías en curva, atril, mesa con vela y el cofre con cerradura de nivel 2 |
| **Jardín de setas** | 28 | Suelo de micelio y musgo, setas gigantes pequeñas, luz de hongos |
| **Armería de los guardianes** | 42 | Yunque, afiladora, mesa de herrería, estandarte |
| **La Copa** | 56 | Plataforma con barandilla entre las ramas; la Piedra de Portal: Naturaleza sobre un pedestal de musgo, entre azaleas, el gran tesoro al lado y el Raizal que la guarda |

- Se sube por una **escalera de caracol** de 2 de ancho pegada a la pared (una vuelta por piso), con barandilla del lado del hueco. En
  cada piso, la barandilla de la plataforma se abre justo donde llega la escalera. En la Copa queda el hueco por donde sube.
- **Agujeros de nudo**: dos ventanas en cada piso para mirar fuera.
- Cofres: provisiones en el salón (con un mímico cerca), prueba en el jardín y en la armería, cerraduras en la biblioteca y la cripta,
  y el tesoro en la Copa.
- Enemigos: Brotes en el claro, en el salón y en los pisos; Espinosos en el salón, la biblioteca, el jardín y la armería; el Raizal en
  la Copa.

## Textos
- Las pistas de los cinco habitantes hablan del árbol («se entra entre sus raíces y se sube por dentro del tronco»; Teo: «¡una
  biblioteca dentro de un árbol!»). La crónica del bosque también.

## Pruebas
- **Planos**: conectividad (todas las salas con acceso salvo 2 huecos de adorno, sin trampas), cofres mirando a la sala, el altar con
  sitio y el mímico junto a un cofre. Vistas del visor por fuera, el salón, la biblioteca y la Copa.
- **CI**: la Piedra de Portal y los seis cofres en su sitio nuevo, el barril, los enemigos y deshacer. También el templo natural.
- **Cliente** (`solo=templo`): fotos por fuera, la entrada, el salón, la biblioteca y la Copa con el Raizal.
