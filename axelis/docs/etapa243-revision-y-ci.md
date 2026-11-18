# Etapa 243 – Revisión a fondo del mod principal + CI con servidor real (18-nov-2026)

Axelis pidió seguir mejorando el mod principal y eligió las 4 opciones (revisar el código, terminar la Torre, quinto jefe, pruebas en el juego real). Orden acordado: 1) revisión, 2) Torre, 3) quinto jefe, 4) pruebas reales. La PC estaba encendida, así que se adelantó parte de las pruebas reales (CI).

## Arreglos de la revisión (compila 0 errores, entregado a `Desktop\Mod\1`, 73 archivos)

### Estado que pasaba de un mundo a otro (un jugador: el juego sigue abierto al salir al menú)
- Nuevo `world/ServerReset`. Al cerrar el servidor limpia:
  - las tareas programadas (`Scheduler`), que antes seguían corriendo en el mundo nuevo contra el mundo ya cerrado (riesgo de que se trabe);
  - el candado de entrada (`BossTravel.lockOn`), que con el reloj del servidor nuevo podía bloquear las dungeons ~40 min;
  - la bandera de "construyendo" de Bosque/Forja (dejaba la copia sin funcionar);
  - los estados de dungeon/forja/hielo/torre, las intros, `Props`, cinemáticas, zonas de calor, frío, daño de jefes, resultados, deshacer de estructuras, portales, cofres de cerradura, asientos, hilo rojo, ancla, `GearEvents`;
  - `DungeonReset`: el número de época ahora se renueva en cada servidor (antes solo al abrir el juego).
- Nuevo `client/ClientReset` (al salir de un mundo o servidor). Quita la pantalla negra de entrada (podía durar 5 min en el siguiente mundo), la cámara de cinemática, el jugador congelado sin poder moverse, la barra de frío, el mapa, avisos, botín, lápidas, radar y daño de jefe.

### Errores de juego
- `Props.clear(level)` borraba las piezas de **todas** las dimensiones. Una intro en una copia rompía la de otra, el hielo de un jugador congelado o los pinchos del lobo. Ahora solo borra las de su nivel; `IceIntro.MONO` y `TowerIntro.BOOKS` toman solo las de su nivel (`Props.take`).
- **Lápidas**: si el servidor se cerraba mientras se reconstruía una dungeon, las lápidas apartadas (con los objetos del jugador) se perdían. Ahora se devuelven antes de guardar (`GraveEvents.onServerStopping`, `detach(level, returnTo)`).
- **Lago de lava** de la intro de la Forja: si el servidor se cerraba a media escena, quedaba lava en la arena. Ahora vuelve a ser suelo antes de guardar (`BossIntro.onServerStopping`).
- **Escena abandonada**: si todos los que veían una intro salían, nunca se ejecutaba el final (sala sin jefe, lava puesta). Ahora el final se hace igual (`CinematicManager.dropEmpty`). El recorrido de escenas ya no puede romperse si una escena empieza o corta otra; los jugadores desconectados no se añaden.
- **Obra de dungeon**: si un paso fallaba, la construcción se quedaba a medias para siempre. Ahora se registra el error y se sigue con el resto (`run()` en Bosque/Forja).
- **Entrada vigilada** (las 4 dungeons): `BossTravel.guard` y `watchdog`. Si algo falla o tarda más de 3600 ticks, se suelta el candado y se quita la pantalla negra con un aviso.
- **Red**: cada paquete solo vale en un sentido (`ModNetwork`: 5 al servidor, 23 al cliente). Forge rechaza los demás antes de leerlos.

### Encontrado por el servidor real (CI)
- 5 tablas de botín huérfanas: `decor_bonsai`, `decor_confetti_block`, `decor_teddy`, `decor_heart_statue` y `decor_heart_lamp`. Sus bloques ya no existen en el mod principal (se fueron al regalo en la separación de la etapa 226).
- En cada carga de mundo salía en el registro `Couldn't parse element loot_tables:...` (también en el proyecto real: están en el jar del 15-nov).
- En la PC se reemplazaron por tablas vacías válidas (se pueden borrar). En el CI se quitaron.

## Cartas: las copias se habían separado
- El jar que Axelis compiló el 15-nov (`Downloads\mods_para_servidor\mundos_de_axelis-1.0.0.jar`) tiene las cartas integradas: `Cartas.init` y el sobre seguro de cada jefe.
- `Desktop\Mod\1` (y la copia de la nube) **no** las tenía. El `BossRewards.java` entregado en la etapa 242 quitaba el sobre de los jefes.
- **Arreglo:**
  - paquete `cartas` copiado del repo (firmas comparadas con el jar: iguales);
  - `Cartas.init(modBus)` en `MundosDeAxelis`;
  - `cardPack(stage)` de vuelta en la ficha y en el premio;
  - 45 textos de cartas añadidos a los 3 lang.
- Las texturas y modelos de las cartas siguen solo en el proyecto real (están demasiado hondo para el puente).

## CI: el mod principal ya corre en un servidor Forge real
- Módulo `axelis/` en `Axelis705/minecraft-mods` (repo **público**, como antes la rama `axelis-compile`):
  - código + `data/` (dimensiones, botín, recetas, `est_decor.json`);
  - sin texturas, modelos ni sonidos.
  - Se compila y arranca con `tools/boot_server.sh`.
- `tools/boot_server.sh`: nueva línea `#wait TEXTO SEGUNDOS` en `ci_commands.txt`, que espera a que algo salga en el registro.
- Nuevo comando `/mundos prueba construir <1-4>`:
  - construye arena + dungeon en la copia 1 sin jugadores (sirve desde la consola);
  - escribe `PRUEBA_CONSTRUIR_n_OK vez k en X s`.
- `axelis/ci_commands.txt`:
  - `cartas probar`;
  - construir torre, hielo, bosque, bosque otra vez (prueba el borrado de la dungeon anterior) y forja;
  - empieza otra forja y apaga el servidor a media obra.
- Resultado (run 44308384255, verde):
  - compila con Gradle y Forge 47.4.10;
  - el servidor arranca sin errores;
  - `PRUEBA_CARTAS_OK 32 cartas, 3 sobres`;
  - tiempos de construcción en el servidor del CI: Torre 1,1 s, Hielo 0,7 s, Bosque 1,8 s, Bosque otra vez (borra la anterior) 1,1 s, Forja 1,8 s;
  - se apaga limpio, con la limpieza nueva al cerrar.
- Lo que falló en los primeros intentos y se arregló:
  - a `axelis/build.gradle` le faltaba `standardInput = System.in`, así que el servidor no leía los comandos;
  - el mundo plano no tenía capas;
  - las 5 tablas de botín huérfanas.

## Pendiente
- Texturas del Archimago (`archimago.png`, `archimago_glow.png`) y modelo del huevo: están en la PC a 8 carpetas de profundidad. El puente no llega, así que no se pudieron subir al CI.
- Prueba con cliente (fotos/video) del mod principal: hace falta subir texturas/modelos (83 MB) o leerlos del jar.
- Terminar la Torre (salas antes de la arena, mascotas de jefes 3 y 4, sonidos del Archimago, música del Palacio de hielo).
- Quinto jefe (Corazón de Magma).
