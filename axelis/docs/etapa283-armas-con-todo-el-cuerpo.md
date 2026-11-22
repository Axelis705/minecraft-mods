# Etapa 283 — Armas: golpe con todo el cuerpo, las dos dagas y armas a dos manos

Segunda tanda de la lista de Axelis: «que el ataque se vea con todo el cuerpo desde fuera», «que se vean las dos dagas» y «que las
armas a dos manos no se puedan usar con algo en la otra mano».

## Golpe con todo el cuerpo (`client/ArmaCuerpo.java`)
- Cada tipo de arma tiene su propio golpe hecho con **fotogramas clave** (como una animación de verdad), no solo el brazo:
  - **Espada**: tajo en diagonal, de arriba a la derecha hacia abajo a la izquierda; el otro brazo equilibra.
  - **Hacha**: a dos manos por encima del hombro y abajo con todo el cuerpo.
  - **Martillo**: los brazos arriba del todo y un mazazo al suelo, agachándose.
  - **Lanza**: el asta a la altura de la cadera; el cuerpo se carga atrás y embiste con un paso largo.
  - **Dagas**: apuñala con una mano mientras la otra queda en guardia; se turnan.
- En las armas a dos manos, la otra mano va al mango. Las piernas dan un paso: la de delante (la contraria al brazo que golpea) y la
  otra atrás.
- Cada cuadro guarda 20 valores: el giro del torso, hacia dónde apunta cada brazo y cada arma, la inclinación, las piernas,
  agacharse y avanzar. El arma apunta de verdad hacia donde va el golpe (calculado con vectores, no solo siguiendo al brazo).
- Se ve desde fuera (tercera persona y otros jugadores). En primera persona, la cámara acompaña un poco el golpe: se puede quitar en
  **Opciones de Mundos → «Cámara al atacar»**.

## Las dos dagas
- Con las dagas en la mano principal y la otra mano vacía, **se ven las dos** (una en cada mano), en tercera y en primera persona.
- Los golpes alternan de mano: uno con la derecha y otro con la izquierda.

## Armas a dos manos
- **Lanza, dagas, martillo y hacha** necesitan la otra mano libre.
- Con algo en la otra mano (una antorcha, un escudo…) no golpean ni usan su poder especial, y sale un aviso.
- La ficha del arma lo dice («A dos manos»).

## Pruebas
- **Servidor** (`mundos prueba etapa283`), con cada tipo de arma: sabe si tiene la otra mano ocupada; las de dos manos no golpean ni
  usan su especial con algo en la otra mano (la espada sí, incluso con escudo); con la otra mano libre golpean.
- **Cliente** (fotos 140–159): cada arma preparando y en el impacto, de lado; las dos dagas de frente y en primera persona; el aviso
  de dos manos. Se comprueba que cada golpe mueve el cuerpo (giro, inclinación y piernas).
- Herramienta `herramientas/pose283.py`: dibuja las poses fuera del juego para ajustarlas.
