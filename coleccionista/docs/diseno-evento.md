# Evento "Los Juegos del Coleccionista" — diseño (actualizado 23-nov-2026)

Mod nuevo de Axelis (`coleccionista/` en el repo, mod id `coleccionista`) para un **evento en vivo** con sus amigos, inspirado en los eventos de juegos macabros de Minecraft, pero **todo original**: anfitrión, nombres, pruebas y trampas propias. Macabro y tenso al estilo Minecraft, **sin sangre**. Usa **Axelis Core** por dentro. Solo 1.20.1 (Forge 47.4.10, Java 17).

## Decisiones de Axelis
- **15–20 amigos, un solo día, 2–3 horas**, servidor abierto por Axelis, **mundo nuevo** (superplano) y todo en el overworld. Se transmite.
- **Anfitrión: El Coleccionista** (máscara de porcelana, traje y sombrero de copa), voz de Axelis. **Axelis no juega**: organiza y aparece **disfrazado** cuando el guion lo pide.
- **Una sola vida.** Pruebas **para cualquiera**, con **un poco de suerte**. **Equipos y parejas al azar.**
- **Producción de evento famoso**: fundidos, presentaciones de cine, franjas de eliminación, cuerpos 3D, filtro de terror propio.
- **Fantasmas** invisibles para los vivos, **no pueden ayudar ni perjudicar**; voz por cercanía con **canal de fantasmas**.
- **Sin errores**: bots de prueba y pruebas automáticas (servidor y cliente con fotos). Más adelante: un launcher.

## Orden del evento (≈2 h 30 min) — las 7 pruebas están hechas
0. **El despertar** (Ala de celdas): sala de espera a oscuras → ¡pum! → cinemática → 10 min para investigar → discurso en la tele (R acerca la cámara) → candados.
1. **Encadenados** (las Cloacas) — parejas encadenadas, la llave en el barril del otro, gas detrás, cupo en el Refugio.
2. **El traidor** (el Archivo) — cada sala abre su bóveda con un código repartido en notas; uno tiene notas falsas.
3. **Compartir o robar** (el Salón) — parejas frente a frente, eligen en secreto.
4. **A ciegas** (los Laberintos) — el guía arriba en el vidrio, los demás vendados.
5. **El sacrificio** (la Capilla) — la reja solo se abre si alguien se queda.
6. **Las puertas** (la Sala de las puertas) — 3 puertas, una sin piso; una pista que a veces miente.
7. **Final: Las copas** — una copa envenenada; la última pieza gana.

**Prueba extra (comodín): Encadenados de tres** (la Galería de los postes). Ver abajo.

**Regla de seguridad:** las pruebas 1–6 nunca dejan menos de **2 piezas vivas** (si alguien "caería" y quedarían menos, se salva: "Necesito piezas para mi final"). El ganador solo sale de la final. Las eliminaciones a mano del organizador no tienen ese límite.

## Cómo se corre cada prueba
Todas igual: **Preparar** (fundido a negro, todos a su lugar, presentación "PRUEBA N · TÍTULO") → **Discurso y empezar** (el Coleccionista explica en las pantallas) o **Empezar ya** → la prueba → **Terminar** (limpia todo). Desde el panel (tecla **O**, pestaña **Pruebas**) o con `/evento <prueba> ...`. Si se pulsa Terminar durante un discurso, el discurso se corta y la prueba no arranca sola.

| Prueba | Mapa | Cómo se juega | Ajustes |
|---|---|---|---|
| 1 Encadenados | Cloacas | Parejas encadenadas a la pared y entre sí (4.5 bloques). Cada llave está en el barril del otro; se lanza con Q y vuela a su dueño. Compuertas de dos placas, tres tuberías (una sirve, al azar), puente, gas que avanza. El Refugio tiene cupo (75 % de las parejas); al llenarse se cierra y el gas se lleva a los de afuera. | `tiempo`, `gracia`, `cupo`, `soltar`, `cerrar` |
| 2 El traidor | Archivo (6 salas de lectura) | Equipos (4) en salas; cada uno recibe en secreto su papel (leal/traidor) y una nota con dos números del código (cada número sale en dos notas). El traidor tiene números falsos. 3 intentos en el teclado de la bóveda: si abren, cae el traidor; si no, el traidor elige a quién se lleva el Coleccionista. | `tamano`, `tiempo`, `cortar` |
| 3 Compartir o robar | Salón (12 mesas) | Parejas nuevas al azar, frente a frente. Hablan 1 min y eligen en secreto COMPARTIR o ROBAR (cartas). Se revela mesa por mesa en la pantalla gigante: comparten → siguen los dos; uno roba → cae el que compartió; los dos roban → caen los dos. | `hablar`, `tiempo_elegir`, `elegir`, `revelar`, `ritmo` |
| 4 A ciegas | Laberintos (4 iguales) | Equipos (3); uno al azar es el **guía**: camina arriba sobre un piso de vidrio, ve todo y las **trampas en rojo**. Los demás caminan **vendados** (niebla negra a 2 bloques y orillas oscuras) y solo oyen su voz. Pisar trampa = regresar al inicio. El equipo más lento pierde a su guía. | `tamano`, `tiempo`, `trampas`, `guia <pieza>`, `cortar` |
| 5 El sacrificio | Capilla (6 cuartos) | Equipos (4) encerrados; la reja solo se abre si alguien se queda: quien se pare 5 s en el altar se ofrece; si nadie, votan en secreto. Quien se queda termina en la vitrina del pasillo. | `tamano`, `tiempo A B`, `votar` |
| 6 Las puertas | Sala de las puertas | Cada ronda las 3 puertas cambian de color y el Coleccionista da una pista (miente 30 %). Todos entran a una cabina antes de que cierren las rejas; en una se abre el piso al agua. Si todos eligen la misma, esa vez aguanta. | `rondas`, `tiempo`, `discurso`, `ronda`, `cerrar` |
| 7 Las copas | Cámara final | 2–4 piezas; hay dos copas más que piezas y una tiene veneno. Por turnos, clic derecho para beber; si alguien cae y quedan varios, se vuelve a servir. | `tiempo`, `beber` |

### Encadenados de tres (extra)
Tríos al azar (si sobran, tríos de cuatro), cada uno en su carril de la **Galería de los postes** (6 carriles separados por barrotes).
1. **La mesa:** en su sala hay una **venda** (casi no ve), una **cinta** (no puede hablar ni escribir; con Simple Voice Chat se le corta el micrófono) y unas **esposas** (no puede tocar nada: palancas, puertas, golpes). La pantalla lo explica, tienen 90 s para decidir y cada uno hace clic derecho a su objeto; lo que sobre se reparte solo.
2. **El recorrido:** se abren las rejas y van **encadenados en fila** (cadena de 4 bloques). La cadena **se atora en los postes** si cada quien pasa por un lado y hay que regresar para soltarla. Obstáculos: bosque de postes, zigzag de un bloque, túnel bajo (agachados) y una reja que se abre con **dos palancas a la vez**, lejos una de la otra (las esposas no pueden jalar).
3. **El último trío en salir pierde** (el trío completo, o uno al azar con `/evento tres pierde uno`).
Comandos: `/evento tres preparar|empezar [ya]|arrancar|tiempo <elegir> <recorrido>|largo N|pierde trio|uno|terminar`. Panel: pestaña Pruebas, "★ Encadenados de tres".

## Estilo del complejo (bloques propios)
Todo el complejo se está pasando a **bloques nuevos del mod** (nada de bloques de Minecraft): una vieja institución abandonada, mitad prisión y mitad manicomio, con las salas del Coleccionista elegantes pero podridas. Texturas de 32×32, sucias y oxidadas, hechas por código (`herramientas/deco/`: `materiales.py` para los bloques enteros, `props_celdas.py` para muebles 3D, `build.py` lo genera todo y el mod lo registra desde `data/coleccionista/deco.json`).
- **Hecho:** el **Ala de celdas** completa (azulejo de hospital, yeso descarapelado, concreto, piso de cuadros, alfombra roja, barrotes oxidados, puertas de celda, catre que sirve de cama, retrete, lavabo, espejo, baúl de las pistas, rayones y placas con texto, lámparas fluorescentes que fallan y zumban, focos de jaula, tuberías, vigas, rejilla de pasarela, cámaras, bocinas, reloj, telarañas, manchas, grilletes, rayitas contando días, camilla, silla de ruedas, escenario con cortinas de terciopelo, candelabros y vitrinas con muñecas de porcelana) y la **Galería de los postes**.
- **Ambiente:** ruidos del edificio a lo lejos (metal, cadenas, puertas, goteras), lámparas que parpadean solas, sin coordenadas en F3 y sin avisos del juego en la esquina durante el evento.
- **Falta:** pasar el estilo a Cloacas, Salón, Archivo, Capilla, Laberintos, Sala de las puertas y la Cámara final.

## Antes de cada prueba
Fundido → presentación de cine ("PRUEBA N · TÍTULO") → **tarjeta de reglas** (3-4 puntos: qué hacer, cómo se gana, qué pasa si pierdes; también queda en el chat) → discurso del Coleccionista → la prueba.

## Voz (Simple Voice Chat, opcional)
Si el servidor tiene Simple Voice Chat, el mod se conecta solo:
- **Vivos:** voz por cercanía normal. No pueden crear ni entrar a grupos (nadie habla en secreto desde lejos). `/evento voz grupos si` lo permite.
- **Fantasmas:** lo que dicen **no lo oyen los vivos**; va por el **canal de los fantasmas** a todos los fantasmas (en todo el mapa) y al organizador. Ellos sí oyen a los vivos que están cerca.
- **Altavoz del organizador** (botón *Altavoz* del panel o `/evento voz altavoz si|no`): su voz le llega a todos, estén donde estén.
- **Voz por salas** durante A ciegas, El traidor y El sacrificio: cada equipo solo oye a los suyos (el organizador y los fantasmas oyen a todos).
- `/evento voz` muestra el estado; `/evento voz probar <habla> <oye>` dice si alguien oye a otro.

## Guion de voces
`claude/evento-coleccionista-guion-voces.md`: 57 líneas (≈5 min) para grabar, un archivo por línea (`<guion>_<clave>.ogg`), con el tiempo de cada una. Los subtítulos ya tienen tiempo para leerse (≈12 letras por segundo).

## Pruebas automáticas
- **Servidor (CI, bots):** despertar, Encadenados ×2, Compartir (bots sin elegir solos para que sea exacto), Las puertas, Las copas, El sacrificio, El traidor ×2, A ciegas (guía elegido, caminantes que salen, el guía del último cae) y las reglas de voz.
- **Pruebas sin Minecraft** (`tests/run.sh`): la cadena que se enreda en los postes.
- **Cliente con fotos** (`cliente-core.yml`, `mod=coleccionista`, `escenario=coleccionista`): HUD, eliminación, fantasma, panel, sala de espera, cinemática, tele, zoom, candado, cuerpos, disfraz y las 7 pruebas (presentación, mapa, pantallas de elegir/votar/teclado, venda y vista del guía, finales).

## Pendiente
- Pasar el estilo nuevo a las demás zonas (después de que Axelis apruebe el piloto del Ala de celdas).
- Grabar las voces (Axelis) y meterlas al mod.
- El Coleccionista como personaje para las cinemáticas.
- Ensayo completo con bots y prueba en la instancia `test` (con Simple Voice Chat).
- Launcher para los amigos.
