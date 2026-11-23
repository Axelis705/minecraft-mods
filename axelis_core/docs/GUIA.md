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

Versión actual: **1.4.0 (API 5)**. Un mod que necesita algo nuevo lo pide en su `mods.toml` (`versionRange="[1.4.0,)"`).

| API | Qué se agregó |
|---|---|
| 1 | Personajes, animación, interfaz, efectos, red, datos |
| 2 | Cuadros de información (`InfoCard`) y temas con marco, `CoreItem`, `ItemTag`, `Plat.give`, `onLogin`, `onBreak`, `onServerTick`, condiciones de modelos de objetos (`ItemModelProps`), bloques con datos y dibujo propio (`CoreBlock`, `CoreBlockEntity`, `CoreBER`), capas del HUD (`Hud`), `Ui.fit`, `ClientTest.waitFor` |
| 3 | Combate (`combat/`): estados (`Estados`, `Estado`), barras de vida y números de daño, ataques con aviso (`Aviso`), proyectiles (`Proyectil`, `ProyectilTipo`), jefes por fases (`Jefe`, `Fase`, `Ataque`, `JefeRenderer`), música de jefe, ayudas (`Combate`), renderizador sencillo (`CoreEntityRenderer`) |
| 4 | NPC (`npc/`): `NpcDef`, `Npc`, misiones (`Mision`, `Objetivo`, `Misiones`), tiendas (`Tienda`), amistad (`Amistad`), ventana del NPC, diario (tecla N) y rastreador; teclas propias (`Teclas`) |
| 5 | Estudio: `.bbmodel` directo (`BbModel`), carpeta `axelis_estudio` que recarga sola, editor de animaciones (`/axeliscore estudio`), curvas Bézier y `easingArgs`, mezcla por valor (`Layer.mezcla`), máscaras suaves, `Animator.query`, `anim.mc.Procedural` (resortes, mirar, ruido), `Huesos.posicion`; efectos por capas (`fx/Efectos`, `EfectosCliente`); interfaz pixel art (`Theme.fondo`, `Ui.boton`, `Ui.nueve`, `Ui.ranura`, `Ui.fondo`, `Ui.relieve`); `Ejemplos` |

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

### Al milímetro (API 5)

- **Proyectos de Blockbench tal cual** (`BbModel`): un `.bbmodel` (Blockbench 4 o 5) se lee sin exportar: grupos, cubos girados,
  UV de caja o por cara, animaciones con **Bézier** (manijas), catmullrom, paso y expresiones, sonidos, partículas y línea de
  tiempo, y las texturas que lleva dentro. Se comprobó contra el exportador del propio Blockbench (las pruebas sin Minecraft lo
  revisan en cada cambio).
- **Carpeta del estudio** (`.minecraft/axelis_estudio/`): `nombre.bbmodel`, `nombre.geo.json`, `nombre.animation.json`,
  `nombre.png` o `nombre.efecto.json` reemplazan al del mod con ese nombre y **se recargan solos al guardar** (sin reiniciar ni
  F3+T); los errores salen en el chat. Borrar el archivo vuelve a lo del mod.
- **Editor dentro del juego** (`/axeliscore estudio` mirando a una criatura del Core): huesos (se ven encima de la criatura),
  animaciones (nueva, en bucle, duración), línea de tiempo con los cuadros clave (giro dorado, posición celeste, escala verde),
  cuadro por cuadro, velocidad, poner y quitar cuadros, curva de llegada, ver con o sin el código del personaje y **mover los
  huesos con el mouse** (clic derecho y arrastrar; con Shift gira en z). Guarda `nombre.animation.json` en el estudio (se abre en
  Blockbench con Importar animaciones). Teclas: espacio, flechas, Supr, S.
- **Curvas**: las de GeckoLib/Blockbench (`easeInOutSine`, `easeOutBack`...) con `easingArgs` (cuánto se pasa o rebota, escalones);
  Bézier con manijas; `Ease.bezier(x1, y1, x2, y2)` para usarlas en código.
- **Capas**: `layer.peso(w)`, `tiempo(t)`, `velocidad(v)`; **máscara suave** con pesos por hueso
  (`mascara(geo, Map.of("chest", 0.5F, "right_arm", 1F))`); **mezcla por valor** con el paso sincronizado
  (`layer("base").mezcla(new Clip[]{walk, run}, new float[]{1.3F, 3.7F}).valor(velocidad)`); `animator.pausar`, `escala`
  (cámara lenta).
- **Lo que las animaciones pueden leer** (`q.X` en Blockbench): `ground_speed`, `vertical_speed`, `is_on_ground`, `is_in_water`,
  `is_sneaking`, `is_sprinting`, `health`, `max_health`, `hurt_time`, `yaw_speed`, `head_x_rotation`, `head_y_rotation`, además de
  `anim_time`, `life_time`, `anim_length` y las `v.X` que ponga el personaje (`animator.var`).
- **Movimiento por código** (`anim.mc.Procedural`, en `axPose`): `resorte(h, pose, "tail", entidad, rigidez, freno, fuerza)` y
  `cadena(...)` para colas, pelo, orejas y capas; `mirar(...)` con límites y suavizado repartido entre huesos; `ruido(...)` suave.
- **Dónde está un hueso en el mundo**: `Huesos.posicion(entidad, "right_hand", pt)` (cliente).

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
- **Estilo "Minecraft pulido"** (API 5): cada tema trae un **fondo de pixel art** que se repite al doble como el fondo de tierra de
  Minecraft (`fondo_madera`: tablones; `fondo_piedra`: ladrillos con musgo; `fondo_pergamino`; `fondo_noche`: con **estrellas que
  titilan**; `fondo_mar`: **el agua se mueve**). Propio: `tema.withFondo(textura128, velo, animación)`. Los botones usan una textura
  de Minecraft teñida del color del tema (`Ui.boton`): al pasar el mouse sale el filo blanco y cruza un brillo, al apretar se
  hunden. Las pestañas tienen una marca que se desliza, los interruptores y deslizadores son de pixel art, las barras tienen
  marcas cada 10 %, las listas entran deslizándose renglón por renglón, los componentes suben al aparecer
  (`Widget.retraso` para escalonarlos), cada clic hace una onda y los avisos entran con rebote y un brillo. Piezas:
  `Ui.nueve` (textura en nueve partes), `Ui.ranura` (ranura de inventario), `Ui.fondo`, `Ui.relieve`. Las texturas se hacen con
  `herramientas/interfaz.py`.
- `UiScreen.panelPrincipal = false`: sin el panel del centro ni el fondo oscuro (para pantallas que dibujan sus propios paneles y
  dejan ver el mundo, como el editor de animaciones).

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

### Efectos de partículas por capas (`fx/Efectos`)

Un efecto es un JSON en `assets/<mod>/efectos/<nombre>.json` (o `axelis_estudio/<nombre>.efecto.json`, que se recarga solo) con
capas: partícula (`polvo` con `color` y `color2`, `brillo`, `chispa`, `fuego`, `magia`, `portal`, `totem`, `hoja`, `nieve`... o
cualquier `minecraft:...` sencilla), forma (`punto`, `esfera`, `anillo`, `disco`, `linea`, `cono`, `espiral`, `columna`), hacia dónde
salen (`afuera`, `adentro`, `arriba`, `abajo`, `dir`, `azar`), cuántas, tiempos (`retraso`, `duracion`, `repetir`, `cada`) y un
sonido. Servidor: `Efectos.lanzar(level, "mod:nombre", pos[, dir, escala])` o `Efectos.enEntidad(e, "mod:nombre", "hueso")`;
cliente: `EfectosCliente.lanzar(...)`. En Blockbench, un cuadro de **partícula** con efecto `axelis_core:chispas` y localizador
(hueso) sale de ese hueso al animar. Muestras: `chispas`, `explosion_magica`, `curacion`, `aparecer`, `fuego_circulo`, `hojas`,
`nevada`, `subir_nivel`, `golpe`, `portal` (`/axeliscore particulas <nombre>` para verlas).

## Ejemplos (`Ejemplos`)

Mara e Iker, la Persona de muestra, las criaturas, el Coloso, la galería y los comandos que los usan son **solo para probar la
librería**: en los mods que usan el Core no aparecen. Se activan solos en desarrollo (runClient, runServer), en las pruebas
automáticas o con `-Daxcore.ejemplos=true` (en el servidor y en el cliente a la vez).

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

## Combate (`combat/`)

**Estados** (servidor): `Estados.aplicar(objetivo, Estados.QUEMADO, ticks[, nivel, fuente])`, `tiene`, `nivel`, `quitar`, `limpiar`,
`aturdido`. Los del Core: `ATURDIDO` (no camina ni ataca), `QUEMADO` (daño cada segundo, el agua lo apaga), `CONGELADO` (lento y con
escarcha), `MARCADO` (+25 % de daño por nivel), `SANGRADO` (pierde vida, más si se mueve). Uno propio:
`Estados.registrar(new Estado("mimod", "electrizado", 0x9FE8FF).icono(6).cada(20, (e, a) -> ...).lento(0.1F))` (íconos del Core: 0
aturdido, 1 fuego, 2 hielo, 3 marca, 4 gota, 5 escudo, 6 rayo; o `icono(textura16x16)`). Nombre: `estado.<mod>.<nombre>` en los lang.
Una criatura que los resiste implementa `Estados.Resiste`.

**Lo que se ve** (cliente, para todas las criaturas): barra de vida sobre las heridas (con la parte blanca que baja despacio), números
de daño (dorados y grandes en golpes fuertes, verdes al curarse, del color del estado si lo tiene), íconos de estados sobre la cabeza,
estrellas al estar aturdido, partículas y, en el propio jugador, sus estados con el tiempo arriba de los corazones. Se apagan en
`config/axelis_core.json` (`barras_vida`, `numeros_dano`). Se dibuja en el HUD pasando puntos del mundo a la pantalla (`Proyeccion`).

**Ataques con aviso**: `Aviso.circulo / anillo / linea / cono / cuadro(level, ...)` + `.color .dano(quien, n) .empuje .estado .sonido
.particulas .temblor .alto .seguir(entidad, fijarAntes) .soloA .alPegar .alTerminar` + `.lanzar()`. Se llena en el suelo y pega al
terminar (no a los aliados del que ataca; ver `Combate.aliados`).

**Proyectiles**: `ProyectilTipo.registrar(new ProyectilTipo(mod, nombre).orbe(color, tam) | .objeto("minecraft:...") | .modelo(geo, tex,
tam)` + `.estela .gravedad .arrastre .persigue .vida .dano .area .estado .impacto .alLanzar)`; se lanzan con `Proyectil.lanzar(...)` o
`Proyectil.hacia(level, tipo, dueño, desde, objetivo, rapidez)` (+ `.persiguiendo(e)`). Los escudos los paran. Muestras en
`CoreProyectiles` (FUEGO, ESCARCHA, FRAGMENTO, CRISTAL).

**Jefes**: `class MiJefe extends Jefe` con `fases()` (lista `static final` de `Fase`: desde qué vida, `.ataque(...)`, `.color`, `.titulo`,
`.entrada(clip, ticks)`, `.dano`, `.rapidez`, `.caminar`, `.aspecto`, `.musica("mod:musica.x")`, `.alEntrar`). Cada `Ataque`: `.clip .dura
.golpe .distancia .peso .espera .despues .alEmpezar .alGolpe .mientras`. El jefe trae barra, elige ataques por distancia y peso, no
recibe daño mientras entra a una fase, resiste estados y pone música a los que estén cerca (sounds.json con `"stream": true`).
Renderizador: `new JefeRenderer<>(ctx, mod, nombre, sombra, escala, aspectos)` (texturas `nombre.png`, `nombre_f2.png`...).
Ejemplo completo: `character/Coloso.java` (música hecha con `herramientas/musica.py`).

## NPC, misiones y tiendas (`npc/`)

**Un NPC** se define una vez al arrancar (igual en servidor y cliente) y se registra con `NpcDef.registrar(...)`:
`new NpcDef(mod, nombre).piel(textura64x64).tema(Theme.MADERA).saludos(claves...).tienda(...).mision(...).amistad(0, 10, 30, 60, 100)
.niveles(claves...).regalo(nivel, jugador -> ...).rutina(NORMAL | QUIETO | LIBRE)`. Se pone en el mundo con
`Npc.poner(level, def, pos)` (+ `.casa(pos)` y `.trabajo(pos)`), `/axeliscore npc poner mod:nombre` o
`/summon axelis_core:npc ~ ~ ~ {AxNpc:"mod:nombre"}`. No recibe daño (salvo en creativo), no desaparece y sigue su rutina: de día
cerca del trabajo, en la tarde a su casa y de noche se sienta. Nombre: `npc.<mod>.<nombre>`.

**Misiones**: `new Mision(mod, nombre).objetivo(...).premio(...)` + `.diaria() .repetible() .requiere(otraId) .nivel(amistad)
.amistad(puntos) .rareza(Rareza.RARO)`. Objetivos: `Objetivo.cazar("minecraft:zombie" | "*", n)`, `entregar(objeto, n)` (se los quedan),
`tener(objeto, n)`, `hablar(npcId)`, `llegar(x, z, radio)`, `evento("mod:clave", n)` (el mod avisa con `Misiones.evento(jugador, clave,
n)`), `.texto(clave)` para un texto propio. Premios: `Mision.Premio.objeto(id, n)`, `esmeraldas(n)`, `experiencia(n)`, `amistad(npc, n)`
o uno propio con `new Premio(icono, n, texto, jugador -> ...)`. Las de un NPC se aceptan y se entregan en su ventana; una suelta se
registra con `Misiones.registrar`, se da con `Misiones.aceptar(jugador, id)` y se entrega sola al completarse. Textos:
`mision.<mod>.<nombre>` y `.desc`. Servidor: `Misiones.estado / aceptar / entregar / abandonar / seguir / evento`.

**Tiendas**: `new Tienda().vende(objeto, n, precio[, existenciasPorDia, nivelAmistad]).compra(objeto, n, precio[, ...]).moneda(id)` (por
defecto esmeraldas). Las existencias son por jugador y se renuevan cada día del juego.

**Amistad**: sube al entregar misiones (5 por defecto) o con `Amistad.sumar(jugador, npc, n)`; al subir de nivel avisa y da el regalo
de ese nivel. Abre misiones (`.nivel(n)`) y cosas de la tienda.

**Lo que se ve**: "!" (misión nueva) o "?" (lista para entregar) sobre la cabeza y su nombre de cerca, la tarjeta al mirarlo (amistad),
su ventana (Hablar, Misiones, Tienda, Amistad, con su retrato y los colores de su tema), el diario de misiones (tecla **N**) y el
rastreador de la misión que se sigue a la derecha. Se apagan en `config/axelis_core.json` (`marcas_npc`, `rastreador_misiones`).
Todo se guarda con el jugador (`PlayerStore` del Core: `misiones`, `amistad`, `tiendas`). Ejemplos: `npc/CoreNpcs.java` (Mara e Iker).

**Teclas propias** (`ui/Teclas`): `Teclas.nueva(mod, "accion", 'J', () -> ...)` en el constructor del mod; salen en Controles
(`key.<mod>.accion`, categoría `key.categories.<mod>`) y funcionan igual en todas las versiones (en 26.3 cambiaron los códigos).

## Comandos de prueba

`/axeliscore` (versión y quién lo usa), `galeria`, `criaturas`, `probar <animación>`, `ir x y z [velocidad]`,
`efecto <temblor|destello|vineta|titulo|aviso|dialogo|demo>`, `datos`, `combate <estados|estado nombre [s] [nivel]|limpiar|avisos|proyectiles|jefe|fase n|ataque nombre|prueba|revisar>`,
`npc <poner mod:nombre|casa|trabajo|lista|mision id|evento clave [n]|amistad npc puntos|reiniciar|prueba|revisar>`,
`estudio` (editor de animaciones), `particulas <efecto>`. Los de ejemplos (galeria, criaturas, combate estados/jefe/prueba, npc
prueba) solo con los ejemplos activos.

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
