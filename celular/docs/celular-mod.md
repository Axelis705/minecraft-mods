# Mod Celular (modid `celular`)

Un celular de verdad en Minecraft (inspirado en Kirri's Phone, pero con más cosas). Carpeta del repo: `celular/`, paquete `com.celular`.
Versiones: **1.20.1** (se compila desde la carpeta), **1.21.1, 1.21.4 y 26.3** (generadas con `tools/mkproject.py celular <ver>` en el CI).

## Objetos
- 8 celulares: `celular:phone` (negro) y `phone_white/pink/blue/green/purple/gold/red`.
- Receta: `IGI / IRI / ICI` (hierro, panel de vidrio, redstone, cobre). Se tiñe en la mesa: celular (cualquiera, tag `celular:phones`) + tinte (lingote de oro para el dorado).
- Clic derecho o tecla **K** abre el celular (hay que tenerlo en el inventario). Con la K, si el celular está en la barra rápida pero no en la mano, se elige esa casilla (así los demás ven la pose) y al cerrar se vuelve a la de antes (salvo con la linterna encendida). **Y** contesta y **U** cuelga. Tecla opcional (sin asignar) para la linterna.
- Bloque `celular:flash_light` (sin objeto): la luz invisible de la linterna.
- **Audífonos** `celular:headphones` (hierro, redstone y lana): se ponen en la cabeza con clic derecho (`Equipable` hasta 1.21.1, componente `equippable` desde 1.21.2) y se ven en el jugador (modelo 3D con `display.head`). Con ellos puestos tu música solo la oyes tú: no sale por el altavoz ni por la bocina, y no oyes los altavoces de otros (el Jam sí).
- **Bocina Bluetooth** `celular:speaker` (hierro, redstone y bloque musical): clic derecho para conectar tu celular (otra vez para soltarla; una bocina es de una persona). Tu música sale desde la bocina (se oye hasta 40 bloques; tú debes estar a menos de 96) y las canciones del mundo las oyen todos los cercanos desde ahí. Mientras suena: estado `on=true` (luz 7, barra de luz, aro de color y bajo que late con la textura animada `speaker_woofer_on`) y notas musicales. Se apaga sola con un tick aleatorio si nadie la usa, al cerrar el servidor y al desconectarse el dueño.

## El celular en 3D (17-nov)
- Modelo `models/item/phone3d/base.json`: cuerpo de 7 × 14 × 1.5 con marco, pantalla (plano encima del frente), cámara trasera con dos lentes y flash, botones a los lados y un logo propio (estrellita). Carcasas de 64 px por color (`textures/item/phone3d/<color>.png`).
- Pantallas de 32 × 64 (`textures/item/phone3d/screen/`): `lock` (la normal, celular guardado), las 17 apps y `ear` (apagada, pegada a la oreja). Dibujadas a 4x y achicadas, con hora de píxeles, isla y esquinas redondeadas.
- Un modelo por carcasa × pantalla (`phone3d/<color>_<app>.json`); la de la llamada usa `base_ear.json` (otra forma de sostenerlo).
- Cómo se sostiene: `thirdperson_righthand` rotación [0, -50, 90], traslación [-0.5, -1, -1.2], escala 0.45 (la parte de arriba hacia los dedos, la pantalla hacia la palma, inclinado 40°); primera persona abajo a la derecha mirando a la cámara. Todo calculado con `herramientas/sim` (ver su LEEME): simula la cadena del juego (brazo → mano → `display`) y dibuja un jugador de cajas con cada pose.
- Poses (`PhonePoses.aim`: el brazo apunta a un lado con la palma hacia otro y de ahí salen los ángulos Z·Y·X): escribir (las dos manos delante del pecho, vaivén y pulgares), foto (las dos manos arriba delante de la cara; pantalla de la cámara), llamada (`EAR` = 18, el codo afuera y el celular entre la cabeza y el brazo), linterna (el brazo hacia donde mira). En la simulación la pantalla queda mirando a los ojos al escribir y al tomar foto (0.99 de 1).
- Haz de la linterna: motitas de luz (`END_ROD`) delante de cualquier jugador cercano que alumbra.

## Animaciones de la interfaz (17-nov)
`client/Anim`: valores que se acercan suave a su objetivo. Interruptores que se deslizan (Ajustes, Notas, Mapa), pestañas con la pastilla que se mueve (Música, Notas), brillo al tocar cualquier botón (`App.drawPress`), burbujas nuevas que suben y aparecen, el saldo de la billetera que cuenta, y el botón de la linterna que se hunde con un resplandor que crece.

## Apps
| App | Qué hace |
|---|---|
| Instacraft | Muro (120 más nuevas), doble clic = me gusta con corazón grande, perfil de cada jugador (publicaciones y me gusta), publicar con 8 filtros (Normal, Vívido, Cálido, Frío, B/N, Sepia, Retro, Noche) y texto de 150 letras. Avisos de like y de publicaciones nuevas. |
| Cámara | Pantalla aparte (`CameraScreen`): visor cuadrado, cuadrícula de tercios, foto/selfie (cámara en tercera persona de frente), arrastrar para mirar, Espacio/Enter/clic en el botón para tomar foto, Tab cambia a selfie. Las fotos se guardan cuadradas de 288 px en `.minecraft/celular/fotos/*.jpg`. |
| Galería | Fotos locales; ver en grande, publicar, poner de fondo, borrar (con confirmación). |
| Teléfono | Contactos (jugadores conectados, dice quién no tiene celular), recientes (sesión), pantalla de llamada (llamando, entrante con anillos, en curso con reloj y ondas de voz). |
| Patreon | Lista con brillo dorado. Archivo `config/celular-patreon.txt` (empieza con "Odette") + `/celular patreon agregar|quitar <nombre>` y `lista`. |
| Música | Canciones de `.minecraft/celular/musica` (MP3, OGG, WAV; también subcarpetas). Botón para abrir la carpeta en la PC y Actualizar. Lista con portadas de colores (por título), reproductor con disco que gira, barra de avance arrastrable, aleatorio, repetir (lista / canción), volumen (rueda del ratón también) y ecualizador de 16 barras con la canción real. Espacio = pausa, flechas = anterior/siguiente. |
| Clima | Tiempo de Minecraft donde estás (sol, luna, nubes, lluvia, nieve, tormenta con relámpagos), "temperatura" del bioma, reloj del mundo (disco día/noche), día, fase de la luna, minutos para que salgan los monstruos o amanezca, hora real. Nether y End con su mensaje. |
| Brújula | Rosa de los vientos que gira contigo, rumbo y coordenadas, lugares guardados con distancia y flechita (la flecha grande apunta al elegido), "donde moriste". Se guardan por mundo/servidor en `.minecraft/celular/lugares.properties`. |
| Calculadora | Como la de los celulares (AC/C, ±, %, ÷ × − +), también con el teclado. |
| Mensajes (17-nov) | Chats privados con cualquier jugador que haya estado en el mundo y el grupo **Todos**. Lista con el último mensaje, hora y sin leer (globito verde en el icono y aviso en la pantalla de bloqueo); burbujas (las tuyas azules a la derecha; en Todos se ve quién habla), caja para escribir y Enter para enviar. Llegan aunque el otro esté desconectado. Botón rojo con alfiler: manda tu ubicación (texto `[[loc]]x|y|z|dim`, se ve como tarjeta; al tocar la de otro se guarda en la Brújula y la flecha apunta hacia ella). En un chat privado, el teléfono verde de arriba llama a esa persona. |
| Notas (17-nov) | Tres pestañas: **Notas** (pendientes: Enter para agregar, tocar para tachar, basura con doble toque), **Alarmas** con la hora de Minecraft (tocar la hora = +1, los minutos = +15; interruptor) y **Timer** de minutos reales (1, 3, 5, 10, 15, 30). Suenan con el timbre y un aviso aunque el celular esté cerrado. Se guardan en `.minecraft/celular/notas.json`. |
| Mapa (17-nov) | Mapa real alrededor (128 × 128, colores de los bloques como los mapas del juego, con relieve y agua más oscura si es profunda; x1, x2 o x4 bloques por píxel). Tu flecha y las caras de los amigos que comparten su ubicación (si están fuera del mapa, en el borde con marco naranja); abajo la lista con distancia y hacia dónde. Interruptor para no compartir (se recuerda en ajustes). En el Nether busca el suelo debajo de ti. Los lugares guardados de la Brújula salen como alfileres rojos (al pasar el ratón, su nombre). Tocar a un amigo de la lista abre su chat. |
| Billetera (17-nov) | Las fichas del mod Casino que llevas (total y de qué valor, con su dibujo), enviar fichas a un jugador conectado (+1/+5/+25/+100/+500, Todo, Borrar) y los últimos 30 movimientos. Si el Casino no está instalado lo dice. No depende del Casino: reconoce las fichas por su nombre (`casino:chip_N`). |
| Linterna (17-nov) | Botón grande. Con el celular en la mano, la luz sale hacia donde miras (también con el celular cerrado) y el brazo se estira hacia adelante. Si sueltas el celular se apaga. |
| Ajustes | 8 fondos (atardecer, noche, océano, aurora, pastel, bosque, synthwave, galaxia) o una foto propia y (con desplazamiento) modo oscuro, pantalla de bloqueo, notificaciones, avisos de mensajes, sonido de cámara, cuadrícula, callar la música de Minecraft, aviso de la canción, altavoz, oír la música de otros, notas de fiesta y compartir ubicación. Se guardan en `.minecraft/celular/ajustes.properties`. |

Extras: la "isla" de arriba muestra la llamada en curso o la canción (portada y barritas); insignias rojas en los iconos; notificaciones que bajan arriba a la derecha; fuera del celular se ve el aviso de llamada entrante, una pastilla con la llamada en curso y "Ahora suena" al cambiar de canción.

Inicio (desde el 16-nov): pantalla de bloqueo (reloj grande, fecha, llamadas perdidas, me gusta, la canción con botones; se quita con clic, tecla o deslizando hacia arriba; se puede apagar), reloj, widget de la canción (con pausa y siguiente) o del clima, cuadrícula de 4 columnas y dock de vidrio. Desde el 17-nov son 15 apps: dock = Teléfono, Mensajes, Instacraft, Música; cuadrícula = Cámara, Galería, Clima, Mapa, Notas, Billetera, Brújula, Calculadora, Linterna, Patreon, Ajustes. Si no cabe el reloj grande (pantallas chicas) se dibuja más pequeño o no se dibuja. El icono de Música late mientras suena.

## Lo que ven los demás (17-nov)
- `PhoneUse` (servidor): el celular dice qué pantalla muestra (`CelularNet.UseScreen`: 0 apagado, 1 inicio, 2 Instacraft … 16 linterna, 17 Jam; el mismo orden que `SCREENS` en `assets.py`) y el servidor lo escribe en el **custom_model_data** del celular que tienes en la mano (`CustomModelData` NBT en 1.20.1, componente con un número en 1.21.1 y con `floats` desde 1.21.4). Así todos lo ven sin paquetes extra y cada versión lo dibuja con su modelo: `overrides` en `models/item/phone*.json` (hasta 1.21.3) y `range_dispatch` en `items/phone*.json` (1.21.4+). Los otros celulares del inventario se apagan solos (revisa 10 veces por segundo); al entrar o salir del mundo se apagan todos. `shouldCauseReequipAnimation` evita que la mano "vuelva a sacar" el celular al cambiar de app.
- Texturas: `textures/item/screen/<app>.png` (solo la pantalla de 6 × 10, encima del celular) y modelos `models/item/screen/<carcasa>_<app>.json` (8 × 17), todo generado por `assets.py`. `tools/mcver.py` solo crea definiciones de ítem para los modelos que están directo en `models/item/` (no en subcarpetas).
- Pose en tercera persona (`client/PhonePoses`, por `IClientItemExtensions.getArmPose` + `ArmPose.create`): con cualquier app, las dos manos delante del pecho mirando la pantalla; con la linterna, el brazo estirado hacia donde mira. En 26.x `ArmPose.create` lleva un booleano más.
- Linterna: el servidor busca con `ClipContext` hasta 28 bloques lo que miras y pone `celular:flash_light` (luz 15, sin choque, reemplazable, sin botín) en el aire justo delante; la mueve al cambiar de lugar. Con ticks aleatorios se borra sola si ninguna linterna la usa (por si el servidor se cierra de golpe). `noCollission` se llama `noCollision` en 26.x (lo cambia `mcver.py`).

## Música (cómo funciona)
- `music/MusicPlayer` (sin nada de Minecraft): biblioteca, cola, aleatorio, repetir y un hilo "Celular-Musica" que decodifica y toca con `javax.sound` (`SourceDataLine`, ~0.25 s de búfer). Así funciona igual en las 4 versiones, la pausa es instantánea y la posición es exacta. Se detiene sola cuando el juego está en pausa; se calla al salir del mundo.
- Se probó primero con el motor de sonido de Minecraft (`SoundInstance.getStream`), pero en 26.3 Forge ya no llama a ese método: por eso se usa el audio de Java.
- `music/Pcm`: MP3 con JavaMP3 (de delthas, MIT, en `music/mp3`, licencia en `META-INF/licenses`), con un arreglo para empezar a media canción; salto rápido por cuadros (`Meta.mp3Seek`). OGG con el decodificador de Minecraft (`OggAudioStream` en 1.20.1, `JOrbisAudioStream` desde 1.20.2). WAV con `AudioSystem`.
- `music/Meta`: título y artista (ID3v2/ID3v1, comentarios Vorbis o "Artista - Título.mp3"), duración (cuenta cuadros del MP3 o usa la cabecera Xing; última página del OGG). Solo MP3 MPEG-1 (los MPEG-2 se marcan "no se puede tocar").
- `music/Spectrum`: FFT de 1024 cuadros → 16 bandas (50 Hz a 15 kHz) guardadas con su número de cuadro para mostrar las del momento.
- Volumen: el de la app × Volumen general × Discos de Minecraft. Teclas sin asignar: pausar/seguir, siguiente y anterior (en Controles).
- **Modo bocina** (17-nov): si el celular no está en tu inventario (ni agarrado con el ratón) pero está tirado en el suelo, la música sale desde ahí: volumen según la distancia (normal hasta 2 bloques, silencio a 24) y más fuerte del lado en que queda (`MusicPlayer.spatialL/R`, el cambio se reparte dentro de cada trozo para que no chasquee). Si el celular desaparece cerca de ti (lo recogió otro, se quemó, lo guardaste en un cofre) la música se pausa con el aviso `celular.music.phone_gone`. Si te alejas mucho (o mueres y reapareces lejos) solo queda en silencio hasta que vuelvas. Solo lo escucha quien tiene las canciones (son archivos de su PC).
- **Música del mundo** (17-nov): pestañas **Mías** (la carpeta; flechita para subir la canción al mundo, globo verde si ya está), **Mundo** y **Jam**.
  - Subir: pedazos de 28 KB con confirmación (6 en camino), máximo 15 MB por canción, 50 por jugador y 500 MB por mundo (`config/celular-musica.properties`). Se guardan en `world/celular/musica` con `canciones.json`; las repetidas (mismo SHA-1) no se duplican. Borra el que la subió o un operador (doble toque en la basura). `/celular musica borrar <jugador>` y `/celular musica total`.
  - Bajar: cada celular guarda las del mundo en `.minecraft/celular/cache_musica/<sha1>.<ext>` y solo las baja cuando las va a tocar o a oír (pedazos de 60 KB, 6 por pedido, se verifica el SHA-1).
  - **Altavoz** (interruptor en Mundo, encendido por defecto): si tocas una canción del mundo (o una tuya que ya está en el mundo), el servidor avisa a los de tu dimensión qué canción, en qué segundo y desde qué entidad (tú o tu celular tirado). Cada uno la oye desde ahí con distancia y lado (hasta 24 bloques, los 3 más cercanos), en un hilo aparte (`music/RemoteStream`).
  - **Botonera de DJ** (en el Jam): Bocina, Scratch, Aplausos y Bajo (sintetizados en `sonidos.py`, que ahora puede generar solo algunos: `python3 sonidos.py dj_horn ...`). Cualquiera del Jam la toca (máximo uno cada 1.2 s por persona) y la oyen todos los del Jam (`Notify.DJ`).
  - **Notas de fiesta**: notas musicales flotan sobre quien suena con altavoz y sobre los del Jam que están cerca (se apagan en Ajustes).
  - **Jam** (como Spotify): crear, unirse (lista o invitación con aviso), cola compartida (se agrega desde Mundo con +), el anfitrión pausa/salta/quita, los demás votan para saltar (con la mitad basta) y quitan lo que ellos pusieron. El servidor lleva el reloj y cada celular se sincroniza con `MusicPlayer.playExternal` (corrige si se desfasa más de 1.5 s y baja la siguiente antes). Las teclas de música y el widget del inicio pasan por el Jam.
- `client/Music`: une el reproductor con el juego (carpeta, volumen, pausa, callar la música de Minecraft, avisos, abrir la carpeta con explorer/open/xdg-open).
- Prueba `celular/tests/MusicTest.java` (en el CI): etiquetas, duración, decodificar, salto, barras y WAV.

## Letras de las canciones (22-nov)

Axelis pidió que, al poner una canción, salgan las letras flotando alrededor del jugador. Eligió buscarlas en un archivo y, si no hay, en internet. También pidió un ajuste para ver o no las letras de otros, que solo se ven si estás a distancia de oír su canción.

- **De dónde sale la letra** (`music/Letras`, en el hilo "Celular-Letras"), en este orden:
  1. un `.lrc` o `.txt` con el mismo nombre que la canción, junto a ella o en una carpeta `letras` (o `lyrics`) de al lado. Acepta UTF-8 con o sin BOM, UTF-16 y Latin-1;
  2. la letra guardada en la canción: MP3 (marcos `SYLT` con tiempos en ms y `USLT`) u OGG (comentarios `SYNCEDLYRICS`, `LYRICS` y `UNSYNCEDLYRICS`);
  3. la caché `.minecraft/celular/letras/`. Si no se encontró, queda un `.none` y se vuelve a buscar a los 3 días;
  4. internet: **LRCLIB** (lrclib.net, gratis). Primero `/api/get` con título, artista y duración; después `/api/search`. Elige la versión con tiempos cuya duración cuadre (±8 s) y no la instrumental.
  - El ajuste «Buscar letras en internet» lo apaga. Solo se manda el título, el artista y la duración.
  - Las canciones del mundo de otros se buscan por su archivo de la caché (la letra de dentro viaja con él) y por internet. El `.lrc` de quien la subió no viaja.
- **Formato** (`music/Letra`):
  - LRC con varias marcas por línea y `[offset:±ms]` (positivo = sale antes);
  - palabras con tiempo `<mm:ss.xx>` (LRC mejorado); si no las trae, las palabras se reparten según su largo;
  - letra sin tiempos: las líneas se reparten entre el 7 % y el 93 % de la canción;
  - una línea dura como mucho 7 s.
- **Cómo se ve** (`client/LetrasHud`, en el HUD):
  - Cada punto del mundo se pasa a la pantalla con la posición y el giro de la cámara. Así funciona igual en las 4 versiones; desde 1.21 el giro de la cámara viene con media vuelta (`FWD`).
  - Tus canciones en primera persona: las palabras salen en un arco delante de ti, de izquierda a derecha, y se quedan quietas en el mundo aunque gires (el arco se fija al empezar cada línea).
  - En tercera persona, con la bocina o con el celular tirado: giran alrededor de quien canta (o de la bocina).
  - Cada palabra entra con un salto, sube despacio, tiene contorno oscuro y su color (según la portada); la que se canta en ese momento va en blanco y más grande. Al acabar la línea se desvanecen.
  - Las de otros solo se ven si su altavoz o bocina te suena (24 o 40 bloques) y se desvanecen al alejarte.
  - **Subtítulo tipo karaoke** abajo (encima de la barra de acción): las palabras ya cantadas en blanco y la siguiente línea más pequeña. Con canción de otro sale «♪ Nombre:».
- **Ajustes nuevos**: Letras de mis canciones, Letras de los demás, Subtítulo de la letra y Buscar letras en internet (`letras_*` en `ajustes.properties`).
- **App de Música**: la línea que se canta ahora, sobre el ecualizador (o «Buscando la letra…» / «Sin letra…»).
- **Pruebas**:
  - `celular/tests/LetrasTest.java` (en el CI): LRC, offset, palabras con tiempo, letra sin tiempos, `.lrc` de al lado con BOM, caché, USLT y SYLT de un MP3 armado en la prueba, y la elección entre resultados de LRCLIB (JSON con escapes). Las letras de prueba son inventadas.
  - Con `--internet` busca una canción de dominio público; si LRCLIB no responde no es un fallo.
  - Prueba con el cliente: `tests/fixtures/tono.lrc` junto a `tono.mp3`. Comprueba que se lee el `.lrc`, que salen palabras y el subtítulo correcto, y que un punto delante de la cámara cae en el centro de la pantalla. Fotos en primera persona, en tercera (de espaldas y de frente) y alrededor de otro jugador (`LetrasHud.debugOtro`).

## Llamadas y voz
- `CallManager` (servidor): DIAL → el que recibe ve "Llamada entrante" y su celular **suena de verdad** (lo oyen los de cerca) cada 2.6 s; 30 s sin contestar = llamada perdida. Ocupado, sin celular, desconectado, rechazada: cada una con su mensaje.
- Voz: `voice/VoicePlugin` es un complemento de **Simple Voice Chat** (`@ForgeVoicechatPlugin`, API 2.5.0 compileOnly desde `maven.maxhenkel.de`). Toma el audio del micrófono de quien habla, lo decodifica, le pone el **filtro de teléfono** (`PhoneFilter`: pasa 320–3300 Hz, un poco de saturación y siseo) y lo manda por un canal directo al otro, a cualquier distancia. Si SVC no está, la clase nunca se carga y la llamada funciona sin voz (la app lo dice).
- Prueba del filtro: `celular/tests/PhoneFilterTest.java` (en el CI).

## Instacraft en el servidor
`world/celular/instacraft/publicaciones.json` + una foto `<id>.jpg` por publicación (máximo 400; se borran las más viejas). Las fotos suben en pedazos de 28 KB (máx 96 KB). 15 s entre publicaciones (los operadores no esperan). Comandos: `/celular instacraft borrar <jugador>` y `/celular instacraft total`.

## Recursos
- `celular/herramientas/assets.py`: el celular 3D (carcasas, 19 pantallas y modelos), audífonos, bocina (con la textura animada), iconos de apps (`gui/apps.png`, 15 de 64 px), iconos de la interfaz (`gui/ui.png`, 64 de 24 px, blancos y teñidos en el juego) y fondos (`gui/wallpapers.png`, 8). Las de la interfaz llevan `.mcmeta` con `blur`.
- `celular/herramientas/sonidos.py`: sonidos sintetizados (obturador, tono de llamada, tono de espera, notificación, like, colgar, toque, desbloqueo).
- `celular/herramientas/idiomas.py`: 15 idiomas (español en todas sus variantes, inglés, portugués, francés, alemán, italiano, ruso, japonés y chino).

## Diferencias entre versiones (marcas `//#if MC`)
- Píxeles de `NativeImage`: `getPixelRGBA/setPixelRGBA` (ABGR) hasta 1.21.1; `getPixel/setPixel` (ARGB) desde 1.21.2.
- `DynamicTexture(Supplier, img)` y `Screenshot.takeScreenshot(target, callback)` desde 1.21.5.
- Ocultar el HUD: `options.hideGui` hasta 1.21.x. En 26.3 ya no existe: el HUD está en `mc.gui.hud` (`Hud.isHidden`, sin "set"), así que la cámara lo cambia con `ObfuscationReflectionHelper`.
- Orden de dibujo en 26.3 (visto con `tools/javap_diag.sh` en el CI): primero se "extrae" todo (`GameRenderer.extract` → `Gui.extractRenderState` → HUD y pantalla) y después se dibuja (`GameRenderer.render` → borra, mundo, GUI). Por eso, al extraer la pantalla, la imagen principal todavía es la del cuadro anterior: la cámara deja un cuadro sin visor y toma la foto en el siguiente.
- Pantallas en 26.2+: `mc.gui.setScreen(...)` / `mc.gui.screen()` (en `ClientHooks.open/screen`). Imagen principal en 26.3: `mc.gameRenderer.mainRenderTarget()`.
- Caras de jugadores: `getSkinLocation()` (1.20.1) / `getSkin().texture()` / `getSkin().body().texturePath()` (26.x).
- Categoría de teclas: `KeyMapping.Category.register` en 26.x.
- **Teclado en 26.3 (SDL)**: los códigos de tecla son *scancodes* de SDL (K = 14, Esc = 41, Enter = 40) y "sin asignar" ya no es -1 (un -1 se guardaba como `key.keyboard.-1` y el juego caía al entrar al mundo en `KeyMapping.setAll`). Las teclas se crean con `InputConstants.KEY_*` / `InputConstants.UNKNOWN` (constantes que cada versión compila con su valor) y `SafeKey` ignora códigos negativos guardados. En las pantallas, `plat/client/Keys.glfw` traduce el scancode a los códigos de siempre (GLFW) para que las apps sigan comparando con 256/257/259/flechas. Lo mismo en el casino (`CasinoScreen`, teclas B/J/H). La 26.2 sigue con GLFW.

## Pruebas en el servidor (`/celular prueba`)
`SelfTest` + `net/NetTest`: pone y quita cada pantalla en un celular (y que quede igual que uno nuevo), coloca la luz de la linterna (luz 15) y la borra con un tick aleatorio, claves y archivo de Mensajes (guardar y volver a leer), billetera, y escribe/lee de vuelta todos los mensajes de red (canción, Jam, pedidos, altavoz, pedazos, mensajes, amigos, billetera, pantalla). El CI la corre al arrancar el servidor en las 4 versiones (`ci_commands.txt` exige `PRUEBA_CELULAR TOTAL fallos=0 fin`).

## Pulido (17-nov, tarde)
- **Reloj de respaldo** (`client/Ticker`): cada 50 ms, con `Minecraft.execute` (igual en todas las versiones), corre la parte del HUD que no dibuja (`PhoneHud.logic`: teclas, música, linterna, alarmas, bocina) si el HUD no lo hizo hace 150 ms. Antes, con F1 (sin HUD) no servían las teclas ni sonaban las alarmas. Se arranca en `ClientModEvents.onKeys`.
- **Inicio que se adapta** (`PhoneScreen.header`): con 15 apps el widget no cabía en las pantallas comunes (240–270 de alto). Ahora: ≥92 px libres → reloj grande + widget; ≥63 → reloj mediano con fecha + widget; ≥34 → el widget si suena música, si no el reloj mediano.
- **Mapa**: dibuja filas hasta gastar ~1.5 ms por cuadro (antes 12 filas fijas; en el Nether cada columna cuesta hasta 52 bloques).
- **Bocinas y dimensiones**: al cruzar un portal se olvidan los altavoces de la dimensión anterior (una bocina en las mismas coordenadas del Nether ya no se oye) y mi altavoz se vuelve a avisar (el servidor lo apaga al cambiar). Si me alejo o acerco a mi bocina (96 bloques) también se reavisa.
- **K con el celular en la mochila**: el servidor lo pasa a una casilla vacía de la barra rápida (o a la de la mano, cambiando de lugar lo que había) y avisa la casilla (`Req.TAKE_OUT` / `Notify.TAKE_OUT`).
- **En llamada** el celular se queda en la mano (en la oreja) aunque cierres la pantalla; al colgar vuelve la casilla de antes.
- Acerca de (Ajustes) con consejos: K, bocina, audífonos, carpetas y `/celular prueba`.
- **Crash arreglado (lo encontró la prueba con el cliente)**: las poses (`HumanoidModel.ArmPose.create`) se creaban la primera vez que alguien usaba el celular; el juego arma una tabla de poses la primera vez que dibuja a cualquier jugador, y una pose creada después hacía caer el juego (`Index 10 out of bounds for length 10` en `HumanoidModel.poseRightArm`) a todos los que veían a otro usar el celular. Ahora se crean al iniciar (campos `static final` de `PhonePoses`, que se carga al registrar el celular, y `PhonePoses.init()` en `onKeys`). Kensei ya lo hacía así.
- **Textos que caben** (`D.fit`): se achican hasta un mínimo y luego se cortan con "…". Nombres de apps del inicio, ajustes, hora del clima, reloj y fecha del bloqueo, "Aleatorio"/"N canciones" (y "1 canción"), texto vacío de la brújula (`App.centered(..., bottom)`), título del widget de música.
- **Bloqueo**: se dibuja más adelante (z +120) que el inicio; los textos con sombra del inicio se veían a través. La barra de estado y las esquinas van encima (z +130). Lo mismo con la animación de abrir una app (z +60).
- **Pose de llamada nueva**: la mano sube hasta junto a la boca (por fuera) y el celular va de la boca a la oreja pegado a la cabeza (`base_ear`: rotación [-180,-100,155], traslación [-2.21,2.07,5.18], escala 0.45; `sim/ear2.py`). Antes el brazo subía derecho y parecía que saludaba. La mano sigue a la cabeza (arriba/abajo y a los lados).
- Tu propia linterna en primera persona ya no echa chispas delante de la cara.

## Prueba con el cliente real (fotos)
- **Crash de 26.3 arreglado (lo encontró esta prueba)**: `getArmPose` devolvía `null` con el celular apagado (antes significaba "la pose normal"). En 26.3 `AvatarRenderer.getArmPose` lo usa directo (`mainHandPose.isTwoHanded()`) y el juego se cerraba apenas alguien tenía el celular en la mano, incluso tú en primera persona (`LevelExtractor.extractPlayerState` extrae también al jugador local). Ahora devuelve `ArmPose.ITEM` (lo mismo que hace el juego con cualquier objeto).
- Ahora sirve en las 4 versiones: entrada `versiones` del flujo (por defecto `1.20.1 1.21.1 1.21.4 26.3`), una rama `fotos-celular-<versión>` por versión. Las capturas se toman con `Tex.screenshot` (igual en todas) y se guardan con `ImageIO`; el otro jugador se agrega con `addPlayer` (1.20.1) / `addEntity` (1.20.2+) y se coloca con `moveTo` / `snapTo` (1.21.5+). Los otros tamaños de pantalla solo en 1.20.1. En 26.3 con Xvfb no hay GLX para OpenGL y el juego usa Vulkan (por eso se instala `mesa-vulkan-drivers`).
- **Video de las animaciones**: entrada `video=true` del flujo → `client/AutoVideo` (con `-Dcelular.autotest.video=true`) sigue un guion de escenas (la cámara da la vuelta al otro jugador que cambia de pose y mueve la cabeza en la llamada; el celular en primera persona: sacar, desbloquear, abrir apps, pestañas, modo claro/oscuro, linterna; la linterna de noche; las bocinas). El flujo graba la pantalla de Xvfb con `ffmpeg -f x11grab` y corta un clip por escena con las marcas `AUTOTEST CLIP nombre inicio|fin t=ms` del registro. Rama `videos-celular-<versión>`. Los videos se ven en la página "Animaciones del Celular" (artifact con cámara lenta y cuadro por cuadro).
- **Subida de canciones arreglada (lo encontró la prueba de dos jugadores)**: el reloj de espera de la subida (`upAt`) solo se ponía después de leer el archivo; en el primer cuadro valía 0 (o el de la subida anterior) y `Radio.frame` la cancelaba "porque el servidor no contestó" antes de empezar. Ahora se pone al llamar a `upload`.
- Cámara: el visor se achica en pantallas chicas (arriba 24 para la etiqueta, abajo 50 para botones y ayuda). Ajustes y avisos del Clima: `D.label` escribe en una línea si se lee bien (y nunca parte una palabra sola) o en dos líneas.
- **Dos jugadores** (`.github/workflows/dos.yml` + `client/AutoTestMulti`): servidor de prueba con los dos como operadores (`ops.json` con los UUID sin conexión) y dos clientes de verdad con Forge instalado por `portablemc` y el jar que se entrega (`-Dcelular.autotest.rol=A|B`). Guion por tiempo desde que se ven: B comprueba la pantalla del celular de A (inicio, música), llamada (entrante, en curso, en la oreja, colgar), mensajes de ida y vuelta, A sube una canción al mundo y la pone con altavoz (B la debe oír y bajar), Jam (A crea, B se une, cola) y el mapa. Cada comprobación escribe `AUTOTEST CHECK nombre OK|FALLO`. Rama `fotos-celular-dos`. Arrancan juntos con una señal: A pone un bloque de oro en (0, -50, 4) 8 s después de ver a B y los dos empiezan el guion al verlo (antes B iba ~6 s atrás). Resultado el 17-nov (9ea0d3d): 19 comprobaciones, 0 fallos.

Flujo manual `.github/workflows/cliente.yml` (`gh api -X POST repos/Axelis705/minecraft-mods/actions/workflows/cliente.yml/dispatches -f ref=main`): el servidor de prueba crea un mundo plano, se copia a `run/saves/prueba` y se abre el juego 1.20.1 de verdad sin pantalla (Xvfb + Mesa por software) con `--quickPlaySingleplayer prueba` y `-Dcelular.autotest=true`. `client/AutoTest` (solo hace algo con esa opción) toma capturas: bloqueo, inicio, inicio con canción, las 15 apps, el celular en la mano (apagado, inicio, música), otro jugador de mentira (`RemotePlayer` agregado solo en el cliente) con cada pose desde 3–4 lados (texto, oreja, foto, linterna, con audífonos), las bocinas encendida/apagada, la linterna de noche y el inicio con la interfaz a 360 y 270 de alto (1080p). Las fotos, `latest.log` y el final del registro se suben a la rama `fotos-celular` (bajar con `git fetch origin fotos-celular`).

## Herramientas de diagnóstico (para versiones nuevas)
- Flujo manual `.github/workflows/diag.yml`: `gh api -X POST .../actions/workflows/diag.yml/dispatches -f ref=main -f inputs[mod]=celular -f "inputs[clases]=a.B c.D%Interna" [-f inputs[privados]=Clase:texto] [-f inputs[metodo]=Clase#metodo]` compila en 1.21.1, 1.21.4 y 26.3 y muestra las clases (usa `DIAG_MC` para tomar el jar de la versión correcta).
- `tools/javap_diag.sh <proyecto> <clases...>`: busca el jar de Minecraft que bajó Gradle y muestra los métodos públicos. Con `DIAG_PRIVADOS="Clase:texto"` muestra miembros privados y con `DIAG_METODO="Clase#metodo"` el orden de llamadas dentro de un método.
- `tools/find_symbol.py <jar> <texto>...`: qué clases mencionan un nombre (para encontrar a dónde se movió algo).
- Se usan poniendo un paso temporal en el CI (la nube no puede bajar Minecraft).

## Pendiente
- Nombre y estilo definitivos del celular (los decide Axelis).
- Probarlo en el juego a mano: la voz con Simple Voice Chat y el sonido de verdad (en el CI no hay tarjeta de sonido). Lo demás ya lo prueban los flujos `cliente.yml` (4 versiones) y `dos.yml` (dos jugadores).
