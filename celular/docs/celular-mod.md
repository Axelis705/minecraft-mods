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
| Ajustes | 6 fondos (atardecer, noche, océano, aurora, pastel, bosque) o una foto propia, modo oscuro, notificaciones, sonido de cámara, cuadrícula. Se guardan en `.minecraft/celular/ajustes.properties`. |

Extras: la "isla" de arriba muestra la llamada en curso; insignias rojas en los iconos; notificaciones que bajan arriba a la derecha; fuera del celular se ve el aviso de llamada entrante y una pastilla con la llamada en curso.

## Llamadas y voz
- `CallManager` (servidor): DIAL → el que recibe ve "Llamada entrante" y su celular **suena de verdad** (lo oyen los de cerca) cada 2.6 s; 30 s sin contestar = llamada perdida. Ocupado, sin celular, desconectado, rechazada: cada una con su mensaje.
- Voz: `voice/VoicePlugin` es un complemento de **Simple Voice Chat** (`@ForgeVoicechatPlugin`, API 2.5.0 compileOnly desde `maven.maxhenkel.de`). Toma el audio del micrófono de quien habla, lo decodifica, le pone el **filtro de teléfono** (`PhoneFilter`: pasa 320–3300 Hz, un poco de saturación y siseo) y lo manda por un canal directo al otro, a cualquier distancia. Si SVC no está, la clase nunca se carga y la llamada funciona sin voz (la app lo dice).
- Prueba del filtro: `celular/tests/PhoneFilterTest.java` (en el CI).

## Instacraft en el servidor
`world/celular/instacraft/publicaciones.json` + una foto `<id>.jpg` por publicación (máximo 400; se borran las más viejas). Las fotos suben en pedazos de 28 KB (máx 96 KB). 15 s entre publicaciones (los operadores no esperan). Comandos: `/celular instacraft borrar <jugador>` y `/celular instacraft total`.

## Recursos
- `celular/herramientas/assets.py`: texturas del objeto, iconos de apps (`gui/apps.png`), iconos de la interfaz (`gui/ui.png`, 24 px, blancos y teñidos en el juego) y fondos (`gui/wallpapers.png`). Las de la interfaz llevan `.mcmeta` con `blur`.
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

## Herramientas de diagnóstico (para versiones nuevas)
- `tools/javap_diag.sh <proyecto> <clases...>`: busca el jar de Minecraft que bajó Gradle y muestra los métodos públicos. Con `DIAG_PRIVADOS="Clase:texto"` muestra miembros privados y con `DIAG_METODO="Clase#metodo"` el orden de llamadas dentro de un método.
- `tools/find_symbol.py <jar> <texto>...`: qué clases mencionan un nombre (para encontrar a dónde se movió algo).
- Se usan poniendo un paso temporal en el CI (la nube no puede bajar Minecraft).

## Pendiente
- Nombre y estilo definitivos del celular (los decide Axelis).
- Probarlo en el juego (sobre todo la cámara en 26.3 y la voz con SVC).
