# Etapa 255 — El volcán, más natural y con más que hacer

Pedido: «puedes ir haciendo lo del volcán, mejorándolo», con las cuatro opciones elegidas:
por fuera más natural, dungeon con premios y reto, ambiente por dentro, y que aparezca solo en el mundo.

## 1. Por fuera: se funde con el terreno

- **Adiós a la placa cuadrada.** El volcán ya no pone un cuadrado de piedra negra debajo. Ahora "se mezcla" con el terreno:
  - bajo el volcán y hasta 5 bloques de su pie, todo queda a su altura (se quita lo que estorbe y se rellena por debajo con basalto);
  - después hay una rampa suave de 13 a 20 bloques hasta la altura del terreno de alrededor (sube o baja según el sitio);
  - la ceniza cubre la rampa y sigue en lenguas irregulares, más largas donde bajan los ríos de lava. Más allá, el terreno queda como estaba.
- **Árboles**: los que quedan en la zona que se aplana se quitan enteros (tronco y hojas, sin dejar hojas flotando). Los que quedan en la ceniza se quedan **quemados**: tronco oscuro, sin hojas.
- **Colores en manchas, no "sal y pimienta"**:
  - faldas con ceniza clara (grava y toba);
  - en medio, pizarra;
  - arriba, roca negra y algo rojiza;
  - **coladas viejas** que bajan en franjas oscuras;
  - chispas de magma que abundan cerca del cráter;
  - donde es muy empinado, roca desnuda.
- **Tres conos pequeños** al pie del volcán, cada uno con una fumarola en su cráter.
- **Fumarolas** (bloque nuevo): bocas con costra amarilla de azufre que echan columnas de vapor sin parar (se ven de lejos) y silban de vez en cuando. No hacen daño. Hay 11 en la ladera, 3 en los conos y 6 por el llano.
- **Coladas enfriadas**: al final de cada río de lava, una lengua de obsidiana, basalto y algo de magma baja hasta el llano y se abre en abanico.
- **Rocas sueltas** de basalto y de obsidiana por el llano, y matojos secos.
- Todo lo que va "sobre el suelo de verdad" (árboles quemados, rocas, fumarolas del llano, los braseros de la entrada y el camino de losas) se apoya en la superficie real, aunque el terreno suba o baje.

## 2. Dungeon con premios y reto

- **7 cofres del volcán** (bloques nuevos, con su propio aspecto: basalto con grietas de magma y oro oscuro). Cada jugador tiene su propio botín, como en las otras dungeons:

  | Dónde | Tipo |
  |---|---|
  | Cámara del cráter, al aterrizar | Provisiones |
  | Al otro lado del puente que se derrumba | Premio |
  | Junto al altar de los cultistas | Cerradura nivel 3 (minijuego de ganzúa) |
  | Sala del tesoro | Premio |
  | Sala del tesoro | Cerradura nivel 5, la más difícil |
  | Tras la reja del guardián | Gran cofre del guardián |
  | Santuario, antes del jefe | Provisiones |

  El botín del volcán trae su propio "sabor": poción de resistencia al fuego, polvo y varas de blaze, crema de magma, oro y obsidiana. En los mejores cofres a veces sale escombro ancestral.
- **Acertijo de fuego**: en la sala de erupciones hay **4 braseros**, dos a cada lado de las filas de respiraderos.
  - Clic derecho enciende uno, que arde 25 s.
  - Si los cuatro arden a la vez, se quedan encendidos y se abre la **reja de la sala del tesoro**. Para lograrlo hay que cruzar los respiraderos corriendo.
  - En la sala esperan dos diablillos.
  - Al tocar la reja cerrada te da una pista.
- **Mini jefe: la Gran Salamandra**, en una cámara nueva entre la sala de erupciones y el santuario.
  - Tiene 220 de vida, barra de jefe con el estilo del volcán y música de jefe.
  - **Ataques**: muerde y quema; escupe abanicos de 3 bolas de lava; si te acercas, golpea el suelo y salen columnas de fuego bajo tus pies.
  - **Pozas de lava**: en ellas se cura deprisa, así que hay que sacarla a tierra firme.
  - **A media vida** ruge y llama a dos salamandras.
  - **Al morir** suelta ascuas, crema de magma y a veces ceniza oscura, y se abre la reja hacia el santuario y su gran cofre.
- **Reaparición para volver a jugar**:
  - Si nadie se acerca a un volcán en 10 minutos, al volver vuelven sus enemigos, incluida la Gran Salamandra, se cierran las rejas y se apagan los braseros.
  - Los cofres se rellenan como mucho una vez por hora; cada jugador recibe un botín nuevo.
  - Un aviso en pantalla y un retumbar lo anuncian.

## 3. Ambiente por dentro

- **Paredes**:
  - manchas grandes de basalto, piedra negra y pizarra, con forma natural (no al azar);
  - **vetas de magma** que brillan en la oscuridad (bloque nuevo, con brillo propio en las grietas) y sueltan alguna chispa.
- **Techos**:
  - **roca de ceniza** (bloque nuevo) que deja caer ceniza gris y, muy de vez en cuando, hace retumbar el volcán;
  - **estalactitas** de basalto en las salas altas.
- **Música**: dentro de la dungeon (bajo la ladera) suena la música del volcán, con fundido; fuera, la normal. Con la Gran Salamandra cerca suena la de jefe.

## 4. Volcanes que aparecen solos

- El mundo normal se divide en regiones de 1024 × 1024 bloques. En más o menos una de cada tres hay un volcán, siempre las mismas para la misma semilla.
- Nunca aparecen a menos de 1200 bloques del punto de aparición.
- Se construye **poco a poco** (unos milisegundos por tick, sin trabar el juego) cuando alguien pasa a unos 300 bloques. Antes comprueba que el terreno sirva:
  - sin agua ni mar, río o playa;
  - no muy desigual;
  - sin aldeas;
  - que nadie viva allí (zonas donde algún jugador pasó más de 20 minutos);
  - con hondura suficiente para la dungeon.
  - Si no sirve, prueba otro sitio de la región; tras 3 intentos, esa región se queda sin volcán.
- Los volcanes hechos con el comando también se reinician y tienen música.

### Comandos (operadores)

| Comando | Qué hace |
|---|---|
| `/mundos volcan cerca` | Volcanes apuntados, el más cercano y qué pasa con la región donde estás. |
| `/mundos volcan reiniciar` | Reinicia ya el volcán más cercano: enemigos, rejas, braseros y cofres. |
| `/mundos volcan generar` | Construye un volcán "natural" a 230 bloques hacia donde miras, con las comprobaciones del terreno. |
| `/mundos volcan generar aqui` | Igual, pero con el centro donde estás y sin comprobar nada (para pruebas). |
| `/mundos volcan natural true/false` | Activa o quita la aparición sola (los que ya hay se quedan). |
| `/mundos estructura volcan` | Sigue funcionando, ahora con la mezcla con el terreno. `deshacer` lo quita, incluidos los enemigos que volvieron. |

## Parte técnica

- `StructBp` aprende tres cosas nuevas:
  - `blend`: columnas mezcladas con el terreno;
  - `anchors`: grupos de bloques apoyados en la superficie real;
  - `nbt`: datos de los cofres por posición.
- `StructPlacer` (nuevo) coloca un plano en este orden: cimientos, mezcla, plano, anclas, conexiones, apoyo, datos y enemigos. Puede hacerlo de golpe (el comando) o por pasos con tiempo limitado (los volcanes naturales). `Estructuras` ahora lo usa.
- `Volcanes` (nuevo, datos guardados con el mundo) lleva:
  - el registro de volcanes y las regiones ya vistas;
  - la vuelta de enemigos (marcados con su volcán y su número);
  - la zona de música (paquete `VolcanZonaPacket`);
  - la obra natural, que carga el terreno con un ticket, lo comprueba y construye por pasos.
- Bloques nuevos: `fumarola` (con entidad de bloque para el humo continuo), `ceniza_volcan`, `veta_magma` (capa de grietas con `forge_data` de luz propia), `brasero_volcan`, `reja_volcan`, `vault_chest_volcano` y `vault_chest_locked_volcano`.
- Entidad nueva: `gran_salamandra`.
- Texturas y modelos: `gen/gen_volcan255.py`.
- `DesignVolcan`: conos, paleta por manchas, coladas, fumarolas, árboles quemados, sala del tesoro, cámara del guardián (suelo a −8, totalmente bajo tierra), cofres, ambiente interior y `inside()` para la música.
- El Linker cuenta las rejas como paso (se abren).

## Comprobado

- Compila (732 clases).
- Prueba de conectividad del volcán: 1 sala sin acceso tolerada (el adorno del corazón colgado), 0 trampas, entrada transitable. Las otras 4 estructuras siguen en verde.
- Revisión de agujeros: ninguna sala queda abierta al cielo. La cámara del guardián se bajó a −8 tras encontrar un hueco en la ladera.
- Vistas del visor: fuera (sin placa cuadrada, borde irregular, conos, árboles quemados, coladas) y dentro (vetas, tesoro, guardián, puente, santuario).
- Pruebas en CI y con el cliente: ver la entrega.
