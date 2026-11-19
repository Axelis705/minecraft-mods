# Etapa 252 — Rework de animaciones y mascotas nuevas

## Qué pidió Axelis
- "Pule más las entidades… dales un rework, que se vean más fluidos con animaciones" y "ponle mucho cariño… todo lo que se pueda".
- Quitar 7 mascotas feas, rehacer 7 con la misma idea y crear 8 nuevas (originales, sin copiar personajes de franquicias).

## Motor de animación común (cliente)
- **Motion**: estado por criatura simulado por tick e interpolado con el tiempo parcial: velocidad, inclinación al acelerar y
  frenar (muelle), ladeo en las curvas, inercia de colas y capas (sway/drag), aplastado al aterrizar, temblor al recibir golpes,
  aparición con rebote. Casillas suaves (`smooth`), muelles con rebote (`spring`, `kick`) y relojes de sucesos (`sinceStart`,
  `sinceEnd`, `sinceLastStart`, `pulse`) para el "después" de una acción (retroceso al disparar, golpe al soltar una carga).
- **Anim**: curvas compartidas: `strike`, `hurt`, `death`, `breath`, `bob`, `idleLook`, `blink`, `follow` (piezas que van
  sobre un torso que se inclina) y ataques largos: `swing` (ataque de N ticks desde que empieza el golpe del juego, que solo
  dura 6), `smash` (mazazo: sube, cae y se recupera), `impact` (pico del golpe) y `jab` (zarpazo).
- **Juice** + **AnimatedMobRenderer**: inclinación, ladeo, rebote, retroceso, estirar/aplastar, aparición y muertes con estilo:
  FLOP (de lado), TOPPLE (de bruces, rebota), MELT (se derrite), POOF (se infla y desaparece girando), CRUMBLE (se desmorona).
  Marco local en `setupRotations`: delante = -Z; rotación X negativa = hacia delante (comprobado con la del vuelo con élitros).
- **Inertia**: al cambiar de estado un jefe, la pose anterior se funde con la nueva (nada salta) sin retrasar las animaciones.
- **MotionFx**: polvo del bloque al pisar, anillo al aterrizar, chispas al recibir golpes, estelas, ambiente y estallido al morir.
- **CreatureStylesData**: el estilo de cada una de las 22 criaturas (peso, frecuencia de paso, muerte, partículas).

## Criaturas
- Bosque: Brote, Espinoso (pétalos con resorte, espinas que se erizan y "salen disparadas", se marchita), Polinizador
  (aleteo a destiempo, embestida, péndulo de polen), Raizal (mazazo con las dos manos, ramas que se sacuden), Guardián de Musgo
  (cuerpo desde la cadera, golpe de suelo con temblor, arbolito del hombro con inercia), Rey Brote (copa que se mece y flores
  que botan; se derrite al morir).
- Forja: Escoria, Chispero (fuelles, manómetro, tos de la chimenea, retroceso), Ascua, Fundidor (crisol que chapotea),
  Capataz (martillo, capa con inercia), Gran Herrero (pisadas de gigante, carga y temblor del martillazo).
- Hielo: Lobo de Escarcha (trote que pasa a galope, lomo que se flexiona, mordida, orejas y cola). Lobo Ártico: inercia entre
  ataques y capa/cola con la inercia; su caída final ahora va de verdad hacia delante.
- Torre: Esbirro (acecha agachado, tajos largos), Armadura Encantada (estatua que despierta con una sacudida, pasos de metal),
  Libro Embrujado (tapas con resorte, hojas a lo loco al recibir golpes, muere girando), Archimago (inercia entre hechizos).
- Volcán (mallas): Salamandra, Diablillo, Cultista y Larva con el mismo "movimiento con vida" aplicado a la malla.

## Mascotas
- Movimiento: la zancada avanza con la distancia recorrida (las patas ya no patinan), inclinación y ladeo con muelle,
  aplastado al aterrizar y polvito al apoyar cada pata.
- Quitadas: Erizo Turbo, Ladronzuelo, Tanuki Aldeano, Panda Rojo Monje, Ave Trueno, Hipocampo Real, Grifito Aprendiz.
  Los objetos viejos se convierten solos en la mascota nueva que ocupa su lugar (`PetRemap`, MissingMappingsEvent).
- Nuevas: Tiburoncito de Tierra, Nubecita, Gatito Cometa, Pulpo Astronauta, Robot Tostadora, Murcielaguito Vampiro,
  Capibara Zen, Dragoncito de Cristal (especie 68). Con iconos, figuritas, 24 sonidos sintetizados y textos.
- Rehechas: Sapo Brote, Cabrita Montañera, Perrito Ninja, Cachorro Rey, Quetzal Dorado, Grifo Imponente, Cerdito Alcancía.
- Segunda vuelta (Axelis las marcó en el catálogo): Cachorro Rey (melena de sol con gajos redondos, mofletes, ojazos),
  Cabrita Montañera (lana de nube de bolitas, patitas gorditas, bufanda de punto, mochila con manta) y Capibara Zen (cabeza
  cuadrada con ojos y mofletes sobre la superficie real, toalla de aguas termales con mandarina y un patito en el lomo), en
  `vfx/Pets24.java`. Lección: en las cabezas de caja (`sbox`) los ojos y calcomanías se ponen con `boxSurf`, no con `surf`
  (que es de elipsoide), o quedan escondidos dentro.

## Arreglos de paso
- Rey Brote: la ramita del árbol giraba alrededor de un punto lejano y se veía suelta; ahora es hija del tronco.
- CI: `axelis/settings.gradle` busca primero en Maven Central (el portal de plugins daba 404 con gson-2.9.1.jar).
- `PetBake`: si la figurita tiene más de 256 colores, junta los parecidos (4 bits por canal) en vez de fallar.

## Revisión
- Prueba con el cliente: 23 hojas de cuadros (22 criaturas + mascotas), 63 comprobaciones, 0 fallos.
- Páginas para Axelis: "Mascotas de Axelis" (catálogo para marcar cambios) y "Animaciones de Axelis" (videos de cada
  criatura con botón para marcar).

## Herramientas
- `herramientas/PetBake.java`, `PetCatalog.java`, `PetSheet.java`, `PetClip.java` (iconos, retratos, hojas de revisión y
  cuadros en movimiento de las mascotas).
- `herramientas/gen_sonidos_mascotas252.py` (sonidos de las mascotas nuevas).
- Prueba con el cliente: hoja de cuadros `axelis_anim_<criatura>.png` (quieta, andando, atacando, golpe, muerte).
