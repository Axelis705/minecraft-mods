# Etapa 249 — Jefe de fuego (etapa 4): Corazón de Magma, su arena y los enemigos del volcán

Diseño elegido por Axelis (ver `claude/dungeon4-fuego-diseno.md`): un corazón de fuego encadenado que baja del techo, escudos que se bajan haciendo cosas en la arena, larvas de lava, suelos que se derrumban, erupciones con aviso y coraza que se agrieta. Enemigos: salamandras de lava, cultistas del volcán y diablillos. Esta etapa hace las fases 1 y 2 del plan.

## El jefe: Corazón de Magma (`corazon_magma`)
- Entras con la **Piedra de Portal: Volcán** (receta: bloques de magma, ladrillos de piedra negra pulida y una carga de fuego). Ficha del jefe con ataques y consejos (`BossLore` caso 4).
- **Antesala** (cueva redonda con braseros) → **puente** sobre el mar de lava → **cúpula** de basalto (radio 21, suelo con anillos dorados, respiraderos de magma, rayos de luz).
- **Intro**: al pisar la arena la caverna tiembla, cae ceniza, el techo se agrieta en 4 golpes, el corazón baja por la grieta con una columna de fuego («Corazón de Magma — El latido del volcán») y se sella la salida.
- **Escudo de cadenas**: mientras quede un ancla de cristal en pie no recibe daño (suena a yunque). 3 anclas en la fase 1 y 4 después; su vida escala con la etapa, la fase y los jugadores.
- Sin anclas **cae al suelo** con la coraza rota y queda **expuesto** 13 s (11 s en la fase 3), recibiendo un 15 % más de daño; luego sube (empuja y quema a quien esté pegado) y se vuelve a encadenar.
- **Fases** al 66 % y al 33 %:

  | Ataque | Desde | Aviso |
  |---|---|---|
  | Bombas de lava en arco | fase 1 | círculos rojos donde caen |
  | Gas volcánico (los respiraderos echan humo y luego una nube que quema y debilita) | fase 1 | humo previo |
  | Larvas de magma | fase 1 (y a mitad del tiempo expuesto) | — |
  | Géiseres bajo los jugadores | fase 2 | círculo + runa |
  | Suelo que se derrumba (un trozo del anillo donde más gente hay cae a la lava; vuelve a los 11 s) | fase 2 | humo y chispas 2,5 s antes |
  | Lluvia de rocas del techo | fase 3 | círculos |
  | Latido: onda de fuego desde el centro (hay que saltarla) | fase 3 | — |

- Si caes a la lava, **la lava te escupe** de vuelta a la arena (con daño y ardiendo un poco).
- **Barra de jefe propia** (`boss_volcano.png`): marco de basalto con grietas de lava, corazón y llama; el relleno cambia con la fase y, mientras está encadenado, se dibujan **cadenas encima de la barra**. La barra aparece cuando el corazón baja.
- **Música propia**: `music_volcano_dungeon` y `music_volcano_boss` (sintetizadas, `gen/gen_musica_volcan.py`).
- **Premios** (`BossRewards.spec(4)`): sello, materiales y sobre seguros; trofeo del Corazón de Magma 50 %, Botas de Magma 30 %, mascota **Corazoncito de Magma** 15 % (legendaria, armadura 8), remaches dorados 55 %, materiales extra (brasa viva, ceniza oscura, eco del lamento) 65 %, esencia.
- Dimensión `boss_volcano` (+ `_2` y `_3`: tres grupos a la vez), tipo Nether, bioma de deltas de basalto (ceniza que cae).

## Enemigos del volcán (para la dungeon y la estructura de las fases 3 y 4)
Todos con malla suave (mismo motor que las mascotas 3D, `vfx/VolcanMesh.java`), inmunes al fuego y con huevo en la pestaña del mod.

| Enemigo | Comportamiento | Vida / daño |
|---|---|---|
| **Salamandra de Lava** (`salamandra_lava`) | Camina sobre la lava como sobre el suelo y en ella se cura (hay que sacarla a tierra). De cerca muerde y quema; de lejos abre la boca (½ s) y escupe una bola de lava en arco con círculo rojo donde cae. | 24 / 5 (bola 5 + fuego) |
| **Diablillo** (`diablillo`) | Vuela en círculos riéndose. Levanta una bola de fuego que crece en su mano (1 s) y te la lanza; cada tanto se lanza en picada a morder y vuelve a subir. | 12 / 3 (bola 4) |
| **Cultista del Volcán** (`cultista_volcan`) | Guarda la distancia. Levanta los brazos y aparece un círculo con runa bajo tus pies: si no te apartas, columna de fuego que quema y te levanta. Una vez, herido, hace una ofrenda y salen 2 larvas de magma. Acorralado, da bastonazos. | 28 / 4 (columna 6) |

- Botín modesto (`loot_tables/entities/*`): crema de magma, cuero, pepitas de oro, pólvora, polvo de blaze; raro: brasa viva, ceniza oscura, carga de fuego, esmeralda.

## Arreglos de paso
- **Halo de las mallas**: el tipo de dibujo «eyes» del juego suma el color sin mirar la transparencia y el brillo salía como un cuadrado naranja alrededor de las larvas, anclas y del corazón. Ahora usa un tipo propio aditivo que respeta el alfa (como los efectos de `VfxClient`).
- **Corazón más grande** (escala 3,3; caja 3,6 × 4,6): en la prueba se veía pequeño para ser el jefe.
- **Pantalla de premios**: con interfaz grande y muchos premios, el texto «Volverás a la superficie…» quedaba encima de la lista. Ahora, si no cabe, toda la pantalla se dibuja un poco más pequeña.

## Pruebas
- CI de servidor: construye la arena del volcán (`PRUEBA_CONSTRUIR_4_OK`, ~1 s) y los tres enemigos aparecen y viven.
- Prueba con el cliente real: piedra del volcán → ficha → entrar a `boss_volcano` → intro → encadenado con 3 anclas → no recibe daño con cadenas → sin anclas cae y queda expuesto → fase 2 → se vuelve a encadenar con 4 → fase 3 → muere → pantalla de premios. 23 comprobaciones, 0 fallos, sin errores de modelos ni texturas. También fotos de los tres enemigos quietos y atacando.
- Comandos de prueba: `/mundos prueba entrar4`, `/mundos prueba construir 4`.

## Pendiente (fases 3 y 4)
- Dungeon dentro del volcán: salas con suelos que se derrumban y erupciones con aviso, los enemigos nuevos, y al final el bloque del jefe.
- Estructura del volcán en el mundo (`/mundos estructura volcan`) con lava falsa para tirarse dentro, y sonidos propios del jefe.
