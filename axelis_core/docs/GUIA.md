# Axelis Core — guía

Axelis Core (`axelis_core`) es la base de todos los mods **nuevos** de Axelis. Los mods anteriores (Mundos de Axelis, casino,
celular, cartas…) no lo usan.

- Versiones: **1.20.1** (Forge 47), **1.21.1** (Forge 52) y **26.3** (Forge 66). Un solo código: las diferencias van con marcas
  `//#if MC >= x` (ver `tools/mcver.py`) y los proyectos de 1.21.1 y 26.3 se generan con `tools/mkproject.py`.
- Nadie instala el Core aparte: cada mod lo lleva **dentro** (Jar-in-Jar, `tools/jarjar.py`). Si dos mods traen versiones
  distintas, Forge carga la más nueva. Por eso **nunca se quita ni se cambia** algo público: solo se agrega, y se sube
  `AxelisCore.API`.

## Hacer un mod nuevo (la Fábrica)

```
python3 tools/mkmod.py pesca "Pesca de colección" "Peces, récords y un libro de colección" Pesca
```

Crea la carpeta `pesca/` lista para compilar y probar: depende del Core, clase principal con su canal de red, registros, un
comando de prueba, el cliente con un escenario de fotos, idiomas (es_es, es_mx, en_us), `ci_commands.txt`, `real_commands.txt` y
la anota en `tools/mods_core.txt`. Al subirla, `mods-core.yml` la compila y la prueba en las tres versiones (servidor de
desarrollo, Jar-in-Jar y servidor Forge real) y deja los jars en las ramas `jar-<mod>-<versión>`.

Fotos del juego de verdad: `cliente-core.yml` con `mod=<mod>` (quedan en la rama `fotos-<mod>-<versión>`).

En el constructor del mod:

```java
AxelisCore.use(MODID);                 // el Core sabe quién lo usa (/axeliscore lo muestra)
AxelisCore.commands(d -> d.register(...));   // comandos, registrados bien en todas las versiones
AxelisCore.onLogin(jugador -> ...);          // algo al entrar un jugador (servidor)
AxelisCore.onBreak((jugador, mundo, pos, bloque) -> ...);  // un jugador rompe un bloque (solo si de verdad se rompe)
AxelisCore.onServerTick(servidor -> ...);    // al final de cada tick del servidor (20 por segundo)
```

Versión actual: **1.1.0 (API 2)**. Un mod que necesita algo nuevo lo pide en su `mods.toml` (`versionRange="[1.1.0,)"`).

| API | Qué se agregó |
|---|---|
| 1 | Personajes, animación, interfaz, efectos, red, datos |
| 2 | Cuadros de información (`InfoCard`) y temas con marco, `CoreItem`, `ItemTag`, `Plat.give`, `onLogin`, `onBreak`, `onServerTick`, condiciones de modelos de objetos (`ItemModelProps`), bloques con datos y dibujo propio (`CoreBlock`, `CoreBlockEntity`, `CoreBER`), capas del HUD (`Hud`), `Ui.fit`, `ClientTest.waitFor` |

## Personajes

| Clase | Qué es | Animaciones |
|---|---|---|
| `Persona` | Humano "Minecraft mejorado": proporciones del jugador (sirve cualquier piel 64x64), codos, rodillas, parpadeo, boca, mira con cabeza y pecho, objetos en las manos | idle, walk, run, talk, wave, point, laugh, nod, shake_head, think, sad, cheer, sit, dance, look_around, attack, hurt, death, fish_cast, fish_hold, fish_reel |
| `Bestia` | Cuadrúpedo (lobo de ceniza) | idle, walk, run, sit, sniff, howl, happy, attack, hurt, death |
| `Alado` | Ave que vuela y planea | idle, fly, glide, attack, hurt, death |
| `Gelatina` | Cuerpo blando que se estira y aplasta | idle, hop, jump, land, attack, hurt, death |
| `Coloso` | Jefe grande con barra de jefe y ataques sincronizados con la animación | idle, walk, roar, slam, sweep, stomp, phase, hurt, death |
| `Chispa` | Mascota pequeña | idle, walk, run, sit, beg, play, happy, sleep, hurt, death |

Todas heredan de `CoreMob`:

- `playAction("wave")` / `playAction("talk", 80)`: el servidor pide un gesto y todos los que miran lo ven.
- `showcase(clip, ticks)`: se queda quieta repitiendo un gesto (galerías y pruebas).
- `variant()` / `setVariant(n)`: piel o forma, guardada y sincronizada.
- Ayudas del cliente para el controlador de animación: `axMove` (caminar/correr a la velocidad real, sin pies que resbalan),
  `axLook`, `axActions`, `axHurt`, `axSwing`, `axDeath`.

Un personaje de otro mod con su propia piel:

```java
public class Pescador extends Persona { ... }                       // comportamiento propio
event.registerEntityRenderer(PESCADOR.get(), PersonaRenderer.with(Ids.rl("pesca", "textures/entity/nilo.png")));
```

## Motor de animación (`anim/`)

- Modelos y animaciones de **Blockbench** (formato Bedrock: `geo/*.geo.json`, `animations/*.animation.json`).
- Capas (`base`, `upper` con máscara desde un hueso, `full`, aditivas) con mezcla suave entre animaciones.
- Curvas de suavizado, expresiones tipo Molang sencillas (`math.sin(q.anim_time*90)*5`), eventos en tiempos de la animación (para
  sincronizar golpes y sonidos).
- Huesos que se doblan suave (codos, rodillas): `axelis_bends` en el geo. Partes que brillan en la oscuridad: `axelis_glow`.
- Renderer propio que funciona igual en las tres versiones (`GeoEntityRenderer`).
- Pruebas sin Minecraft: `axelis_core/tests/AnimTest.java` (corren en CI). Vista previa sin el juego:
  `axelis_core/herramientas/visor`.
- Los modelos y pieles de muestra se generan con `axelis_core/herramientas/*.py`.

## Interfaz (`ui/`)

- `UiScreen`: pantalla con panel animado y tema. Se agregan `Button`, `Tabs`, `ListView` (con desplazamiento), barras
  (`Ui.bar`), texto con saltos de línea (`Ui.text`), ayuda al pasar el ratón (`Button.help`).
- Temas: `Theme.NOCHE`, `Theme.PERGAMINO`, `Theme.MAR` o `Theme.of(fondo, acento)`.
- `Toasts`: avisos que entran por la derecha con el dibujo de un objeto.
- `Dialogs.show(jugador, quien, piel, texto, respuestas, tema, respuesta -> ...)`: diálogo con retrato y botones; la respuesta
  vuelve al servidor.
- Pantallas que abre el servidor: en el cliente `ScreenRegistry.register("pesca:libro", LibroScreen::new)`; en el servidor
  `CoreScreens.open(jugador, "pesca:libro")`.
- `Screens.open/close/current` (en `plat/client`): abrir y cerrar pantallas igual en todas las versiones.
- Más piezas: `CardGrid` (cuadrícula de tarjetas, con `CardGrid.slot` para dibujar la casilla), `IconButton`, `Toggle`,
  `Slider`. `Ui.fit(texto, ancho)` recorta con "…" lo que no cabe.
- Marcos con textura por tema: `Theme.MADERA`, `Theme.PIEDRA`, `Theme.PERGAMINO`, `Theme.MAR`, `Theme.NOCHE` o
  `tema.withFrame(...)`. Rarezas comunes a todos los mods: `Rareza.COMUN` … `Rareza.LEGENDARIO` (color, nombre y brillo).

### Cuadros de información (`InfoCard`)

Un cuadro con título, rareza, ícono o dibujo, subtítulo, texto, barras, pares "dato: valor", estrellas, separadores y pistas:

```java
new InfoCard(stack.getHoverName()).rareza(Rareza.RARO).icon(stack).subtitle(...)
        .bar(texto, 0.7F, color, "70%").pair(texto, valor).stars(texto, 2, 3).divider().text(...).hint(...);
```

- **Objetos**: si el objeto extiende `CoreItem` y devuelve su cuadro en `card(stack, detalle)`, sale al pasar el ratón en
  cualquier pantalla (con Shift, el detalle). `tooltip(stack, add)` agrega líneas al texto normal y `onUse` maneja el clic
  derecho, igual en todas las versiones.
- **Al mirar**: los personajes del Core (y lo que implemente `InfoSource`) muestran su tarjeta al mirarlos un momento.
- **En pantallas del Core**: cada widget puede tener su cuadro (`Button.card(...)`, `ListView.cards`, `CardGrid`).
- Para otros objetos: `Tooltips.register(stack -> cuadro)`.

### Modelos de objetos que cambian (`item/client/ItemModelProps`)

Una condición propia (por ejemplo "la caña está lanzada") que funciona en las tres versiones:

```java
Net.onClient(() -> ItemModelProps.bool(MODID, "lanzada", (stack, quien) -> ...));   // en el constructor del mod
```

En 1.20.1 y 1.21.1 se usa en `"overrides"` del modelo (`"predicate": {"mod:lanzada": 1}`); en 1.21.4+ en
`assets/<mod>/items/<objeto>.json` con `"type": "minecraft:condition", "property": "mod:lanzada"`. Los dos archivos pueden
vivir juntos en el mod (cada versión lee el suyo).

## Bloques (`block/`)

- `CoreBlock`: clic derecho (`onUse`), vecinos (`onNeighbor`) y "un jugador lo rompe" (`onPlayerBreak`, para soltar lo que guarda)
  iguales en todas las versiones; gira bien con las plantillas.
- `CoreBlockEntity`: solo `writeData`/`readData` con un CompoundTag; guarda y manda a los clientes en cada versión. Después de
  cambiar algo, `changed()`. Objetos adentro: `Ser.item(stack)` / `Ser.item(tag)`.
- `CoreBER` (cliente): dibujar encima del bloque con `Draw` (cuadros con textura `cutout`/`translucent`/`glow`, texto, objetos);
  se registra con `e.registerBlockEntityRenderer(TIPO.get(), MiRenderer::new)`.
- Los bloques que no son cubos enteros llevan `noOcclusion()`. Para transparencias, mejor dibujarlas en el `CoreBER` que con
  `render_type` en el modelo.

## Efectos (`fx/Fx`)

Desde el servidor: `Fx.shake` / `Fx.shakeNear` (temblor de cámara), `Fx.flash` (destello), `Fx.vignette` (bordes de color),
`Fx.title` (título grande con franja de cine), `Fx.toast` (aviso con objeto).

En el cliente, `Hud.add((g, ancho, alto) -> ...)` dibuja cosas propias en la pantalla de juego (marcadores, temporizadores).

## Datos (`data/`)

- `WorldStore.get(server, "pesca")`: datos guardados con el mundo, una parte general (`global()`) y una por jugador
  (`player(uuid)`); después de cambiar, `dirty()`.
- `PlayerStore.of(jugador, "pesca")` + `PlayerStore.save(jugador, "pesca")`: guarda y manda la copia al cliente.
  `PlayerStore.syncOnLogin("pesca")` la manda sola al entrar.
- En el cliente: `ClientData.player("pesca")`, `ClientData.get(clave)`, `ClientData.listen(clave, tag -> ...)` (se olvidan
  solos al cambiar de mundo).
- `Actions.on("pesca:reclamar", (jugador, datos) -> ...)` en el servidor y `ClientData.send("pesca:reclamar", datos)` en el
  cliente: botones que piden cosas al servidor sin hacer un mensaje de red propio. **El servidor siempre comprueba.**
- `Config.of("pesca")`: opciones en `config/pesca.json` con explicación al lado (`num`, `dec`, `bool`, `str`, `set`, `reload`).

## Red y plataforma (`plat/`, `net/`)

- `Net`: canal propio de cada mod (`register`, `toServer`, `toPlayer`, `toAll`, `toNear`), igual en las tres versiones.
- `Reg`: propiedades de bloques, objetos y entidades con su id (lo que pide 1.21.2+).
- `Ids` (identificadores), `Nbt` (leer datos sin los Optional de 1.21.5+), `Ser` (objetos y textos en mensajes), `Plat`
  (caminar con `walkTo`, empujar, dañar, mensajes, objetos por id…).
- Texto: `net/Txt` empaqueta textos traducibles para mandarlos al cliente.
- `Plat.give(jugador, objeto)` (al inventario o al suelo si no cabe), `Plat.server(entidad)`, `Plat.mainItems(inventario)` y
  `Plat.mainAndOffhand(inventario)` (el inventario cambió en 1.21.5). `ItemTag` lee y edita los datos propios de un objeto.

## Comandos de prueba

`/axeliscore` (versión y quién lo usa), `galeria`, `criaturas`, `probar <animación>`, `ir x y z [velocidad]`,
`efecto <temblor|destello|vineta|titulo|aviso|dialogo|demo>`, `datos`.

## Pruebas automáticas

- `core.yml`: marcas de versión, recursos, texturas, pruebas del motor, compilación, servidor de desarrollo con comandos,
  Jar-in-Jar y servidor Forge real, en las tres versiones.
- `cliente-core.yml`: abre el juego de verdad sin pantalla, corre el escenario (`test/ClientTest`, `test/CoreScenario`) y saca
  fotos. Termina con `AXCORE_AUTOTEST_FIN fotos=N fallos=M`. Pasos: `t.add` (esperar un tiempo), `t.waitFor` (esperar hasta que
  algo pase, con tiempo máximo), `t.photo`/`t.shot` (fotos), `t.check` (comprobar), `t.cmd` (comandos de consola) y
  `UiScreen.fakeMouse` (poner el ratón donde se quiera para fotografiar cuadros).
- `mods-core.yml`: lo mismo para cada mod de `tools/mods_core.txt`.

## Cosas a recordar de cada versión

- 26.x: eventos con `@SubscribeEvent(priority = Priority.LOWEST)` de `eventbus.api.listener`; romper bloques se decide con
  el resultado del evento (`DENY` = no se rompe). Los modelos de objetos usan `items/*.json` (desde 1.21.4).
- 26.x: `.moveTo(` se cambia solo a `.snapTo(`: escribir `this.moveTo(...)` (con el punto) y, para la navegación, que no debe
  cambiar, `getNavigation(). moveTo(` (con un espacio después del punto).
- 26.x: `ResourceLocation` → `Identifier`, `.moveTo(` → `.snapTo(` (por eso caminar va con `Plat.walkTo`), la pantalla vive en
  `Minecraft.gui` (26.2+), el golpe se lee con `getSwingAnimation` (26.3), no hay chunks de aparición (las pruebas del
  servidor usan `forceload`), la barra de jefe pide un UUID.
- 1.21.1: los comandos se registran a mano (en el mundo de un jugador el registro automático no llegaba).
- 1.20.1: es la versión principal para escribir y compilar rápido en la nube (`cac.sh`).
