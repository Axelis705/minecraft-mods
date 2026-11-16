# Mod Celular (modid `celular`)

Un celular de verdad en Minecraft (inspirado en Kirri's Phone, pero con más cosas). Carpeta del repo: `celular/`, paquete `com.celular`.
Versiones: **1.20.1** (se compila desde la carpeta), **1.21.1, 1.21.4 y 26.3** (generadas con `tools/mkproject.py celular <ver>` en el CI).

## Objetos
- 8 celulares: `celular:phone` (negro) y `phone_white/pink/blue/green/purple/gold/red`.
- Receta: `IGI / IRI / ICI` (hierro, panel de vidrio, redstone, cobre). Se tiñe en la mesa: celular (cualquiera, tag `celular:phones`) + tinte (lingote de oro para el dorado).
- Clic derecho o tecla **K** abre el celular (hay que tenerlo en el inventario). **Y** contesta y **U** cuelga.

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
| Ajustes | 8 fondos (atardecer, noche, océano, aurora, pastel, bosque, synthwave, galaxia) o una foto propia, modo oscuro, pantalla de bloqueo, notificaciones, sonido de cámara, cuadrícula, callar la música de Minecraft, aviso de la canción. Se guardan en `.minecraft/celular/ajustes.properties`. |

Extras: la "isla" de arriba muestra la llamada en curso o la canción (portada y barritas); insignias rojas en los iconos; notificaciones que bajan arriba a la derecha; fuera del celular se ve el aviso de llamada entrante, una pastilla con la llamada en curso y "Ahora suena" al cambiar de canción.

Inicio (desde el 16-nov): pantalla de bloqueo (reloj grande, fecha, llamadas perdidas, me gusta, la canción con botones; se quita con clic, tecla o deslizando hacia arriba; se puede apagar), reloj, widget de la canción (con pausa y siguiente) o del clima, cuadrícula de 4 columnas (Galería, Clima, Brújula, Calculadora, Patreon, Ajustes) y dock de vidrio (Teléfono, Instacraft, Cámara, Música). El icono de Música late mientras suena.

## Música (cómo funciona)
- `music/MusicPlayer` (sin nada de Minecraft): biblioteca, cola, aleatorio, repetir y un hilo "Celular-Musica" que decodifica y toca con `javax.sound` (`SourceDataLine`, ~0.25 s de búfer). Así funciona igual en las 4 versiones, la pausa es instantánea y la posición es exacta. Se detiene sola cuando el juego está en pausa; se calla al salir del mundo.
- Se probó primero con el motor de sonido de Minecraft (`SoundInstance.getStream`), pero en 26.3 Forge ya no llama a ese método: por eso se usa el audio de Java.
- `music/Pcm`: MP3 con JavaMP3 (de delthas, MIT, en `music/mp3`, licencia en `META-INF/licenses`), con un arreglo para empezar a media canción; salto rápido por cuadros (`Meta.mp3Seek`). OGG con el decodificador de Minecraft (`OggAudioStream` en 1.20.1, `JOrbisAudioStream` desde 1.20.2). WAV con `AudioSystem`.
- `music/Meta`: título y artista (ID3v2/ID3v1, comentarios Vorbis o "Artista - Título.mp3"), duración (cuenta cuadros del MP3 o usa la cabecera Xing; última página del OGG). Solo MP3 MPEG-1 (los MPEG-2 se marcan "no se puede tocar").
- `music/Spectrum`: FFT de 1024 cuadros → 16 bandas (50 Hz a 15 kHz) guardadas con su número de cuadro para mostrar las del momento.
- Volumen: el de la app × Volumen general × Discos de Minecraft. Teclas sin asignar: pausar/seguir, siguiente y anterior (en Controles).
- **Modo bocina** (17-nov): si el celular no está en tu inventario (ni agarrado con el ratón) pero está tirado en el suelo, la música sale desde ahí: volumen según la distancia (normal hasta 2 bloques, silencio a 24) y más fuerte del lado en que queda (`MusicPlayer.spatialL/R`, el cambio se reparte dentro de cada trozo para que no chasquee). Si el celular desaparece cerca de ti (lo recogió otro, se quemó, lo guardaste en un cofre) la música se pausa con el aviso `celular.music.phone_gone`. Si te alejas mucho (o mueres y reapareces lejos) solo queda en silencio hasta que vuelvas. Solo lo escucha quien tiene las canciones (son archivos de su PC).
- `client/Music`: une el reproductor con el juego (carpeta, volumen, pausa, callar la música de Minecraft, avisos, abrir la carpeta con explorer/open/xdg-open).
- Prueba `celular/tests/MusicTest.java` (en el CI): etiquetas, duración, decodificar, salto, barras y WAV.

## Llamadas y voz
- `CallManager` (servidor): DIAL → el que recibe ve "Llamada entrante" y su celular **suena de verdad** (lo oyen los de cerca) cada 2.6 s; 30 s sin contestar = llamada perdida. Ocupado, sin celular, desconectado, rechazada: cada una con su mensaje.
- Voz: `voice/VoicePlugin` es un complemento de **Simple Voice Chat** (`@ForgeVoicechatPlugin`, API 2.5.0 compileOnly desde `maven.maxhenkel.de`). Toma el audio del micrófono de quien habla, lo decodifica, le pone el **filtro de teléfono** (`PhoneFilter`: pasa 320–3300 Hz, un poco de saturación y siseo) y lo manda por un canal directo al otro, a cualquier distancia. Si SVC no está, la clase nunca se carga y la llamada funciona sin voz (la app lo dice).
- Prueba del filtro: `celular/tests/PhoneFilterTest.java` (en el CI).

## Instacraft en el servidor
`world/celular/instacraft/publicaciones.json` + una foto `<id>.jpg` por publicación (máximo 400; se borran las más viejas). Las fotos suben en pedazos de 28 KB (máx 96 KB). 15 s entre publicaciones (los operadores no esperan). Comandos: `/celular instacraft borrar <jugador>` y `/celular instacraft total`.

## Recursos
- `celular/herramientas/assets.py`: texturas del objeto, iconos de apps (`gui/apps.png`, 10 de 64 px), iconos de la interfaz (`gui/ui.png`, 48 de 24 px, blancos y teñidos en el juego) y fondos (`gui/wallpapers.png`, 8). Las de la interfaz llevan `.mcmeta` con `blur`.
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

## Herramientas de diagnóstico (para versiones nuevas)
- Flujo manual `.github/workflows/diag.yml`: `gh api -X POST .../actions/workflows/diag.yml/dispatches -f ref=main -f inputs[mod]=celular -f "inputs[clases]=a.B c.D%Interna" [-f inputs[privados]=Clase:texto] [-f inputs[metodo]=Clase#metodo]` compila en 1.21.1, 1.21.4 y 26.3 y muestra las clases (usa `DIAG_MC` para tomar el jar de la versión correcta).
- `tools/javap_diag.sh <proyecto> <clases...>`: busca el jar de Minecraft que bajó Gradle y muestra los métodos públicos. Con `DIAG_PRIVADOS="Clase:texto"` muestra miembros privados y con `DIAG_METODO="Clase#metodo"` el orden de llamadas dentro de un método.
- `tools/find_symbol.py <jar> <texto>...`: qué clases mencionan un nombre (para encontrar a dónde se movió algo).
- Se usan poniendo un paso temporal en el CI (la nube no puede bajar Minecraft).

## Pendiente
- Nombre y estilo definitivos del celular (los decide Axelis).
- Probarlo en el juego (sobre todo la cámara en 26.3 y la voz con SVC).
