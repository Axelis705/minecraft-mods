# Kensei

Mod de Minecraft Forge 1.20.1 (Forge 47.4.10, Java 17): combate con katana, estilo "Combat Warriors".

- mod_id: `kensei`  ·  paquete: `com.kensei`  ·  clase principal: `Kensei.java`
- Para compilar: `gradlew build` (el .jar queda en `build/libs`). Para probar en desarrollo: `gradlew runClient`.
- No usa librerías externas: modelo 3D, texturas, sonidos y animaciones se generan con código y JSON.

## El arma
**Katana Kensei** (`kensei:katana`): única arma por ahora (rareza épica, 3200 usos, daño de netherita+).
Receta (3×3): fila 1 `_ _ N`, fila 2 `_ N S`, fila 3 `B _ _` (N = lingote de netherita, S = estrella del Nether, B = vara de blaze).
Reparable con lingotes de netherita. Modelo 3D propio (hoja con línea de temple y runas celestes, guardia dorada, mango con cinta roja) y una versión guardada en su vaina que se ve en la espalda cuando la llevas en el inventario y no en la mano (solo se ve en uno mismo).

## Cómo se juega
Con la katana en la mano:
- **Clic izquierdo**: cadena de 4 tajos, cada uno con su animación (horizontal, de vuelta, diagonal y final ascendente), en primera y tercera persona. Das un paso hacia el enemigo más cercano y el golpe alcanza a varios en un abanico delante de ti (no hace falta apuntar exacto). Mantener el clic repite los golpes. El 4.º levanta al enemigo; si lo golpeas en el aire sigue flotando y el último golpe aéreo lo manda al suelo con un remate que daña en área.
- **Clic izquierdo corriendo**: si no hay cadena en curso, en vez del tajo haces una **estocada** (avance largo, ×1.3 de daño, abanico estrecho).
- **R**: **ataque crítico**. El arma sube sobre la cabeza y cae con mucho daño y empuje. **Rompe la guardia** y no se puede parrear: solo esquivar. Tiene espera (barrita dorada bajo las barras de esquive).
- **Alt izquierdo**: esquivar hacia donde apretes (sin dirección = salto atrás). Gasta 1 dash, da unos ticks de invulnerabilidad y corta lo que estés preparando; el cuerpo se inclina hacia donde esquivas.
- **Clic central**: fijar objetivo (el personaje se queda mirando al enemigo; arriba se ve su nombre y vida). Otro clic central lo suelta.
- **Clic derecho sostenido**: guardia. Si el golpe llega en los primeros ticks es un **parry perfecto**: el golpe se anula, el atacante sale empujado y aturdido, ganas energía y se abre un **contraataque** (×1.75). Bloquear tarde solo reduce el daño. No se puede atacar mientras bloqueas.
- **Dashes** (3, arriba a la izquierda junto a tu vida): cada esquive gasta uno y se recargan solos cuando llevas un momento sin atacar ni esquivar (un parry o esquive perfecto devuelve energía extra).
- **Clic central** fija al enemigo (aparece una mira sobre él) y lo suelta; **X** cambia al siguiente; con un objetivo fijado, **un movimiento rápido del ratón a la izquierda o a la derecha** pasa al enemigo más cercano de ese lado (como Dark Souls); si el fijado muere pasa solo al más cercano.
- **Alt izquierdo** esquiva (dash), **R** ataque crítico. Todas las teclas se pueden cambiar en Controles > Kensei.

## Estamina (ya no se puede spamear)

Una barra amarilla bajo tu vida. Cada tajo gasta (ligero 10, final 16, estocada 14, crítico 30) y **fallar el golpe al aire gasta extra**. Al golpear te frenas un poco (más con el crítico). Bloquear gasta según el daño; si no alcanza, **se te rompe la guardia**. Un parry perfecto devuelve estamina. La estamina se recupera sola tras una pausa (más lento mientras bloqueas). Si llegas a 0 quedas **AGOTADO** un momento (lento y sin poder atacar). El rival de práctica sigue las mismas reglas. Ajustes en el config, sección `estamina` (`activada`, máximo, recuperación, costos...).

## Menú de duelo

Al pulsar **G** sobre alguien sale un menú: elige **1 vs 1** o **2 vs 2**, y quién es tu aliado y los rivales (jugadores cercanos o el *Rival de práctica*). Los jugadores reciben el aviso para aceptar; el rival de práctica entra solo.

## Aliados y rivales en duelos por equipos

En un duelo 2v2 los aliados llevan el nombre en **verde** con la etiqueta [ALIADO] y una **flecha verde** sobre la cabeza que se ve a cualquier distancia (también tras paredes); los rivales llevan el nombre en rojo. A los aliados **no se les puede fijar** (clic central / X) ni los elige la fijación automática, y tus golpes no los dañan. Código: `client/TeamMarks.java`, `ClientDuel.relation`.

## Animaciones de la katana (tercera persona)

Las posturas se calculan: cada una dice hacia dónde apunta la mano y la hoja, y el mod resuelve los ángulos del brazo; la otra mano sujeta la empuñadura. Hay guardia con las dos manos, espera con respiración, carrera con la hoja atrás, 4 tajos distintos (horizontal, vuelta, diagonal, ascendente), estocada y crítico sobre la cabeza. Además hay **postura de combate de cuerpo entero**: pies abiertos y uno adelantado, el cuerpo baja un poco (`client/StanceEvents.java`), las caderas siguen al torso en cada tajo y el paso es más largo; al caminar o correr las piernas vuelven a su paso normal. Código en `client/KatanaStance.java` (matemática) y `client/KatanaPoses.java`; el rival usa las mismas. La inclinación de la katana en la mano está en `models/item/katana.json` (`thirdperson_*hand`).
- **Cámara de combate (F5, vista de espaldas)**: con la katana la cámara se acerca, se corre sobre el hombro y se abre al esquivar/correr/crítico (config `camara`).
- **Efectos visuales** (config `efectos`, se pueden apagar uno por uno): tajos de luz en forma de media luna para cada golpe del combo (cian; dorado en el final; rojo en el crítico), círculo mágico y chispas girando durante la carga del crítico, ondas en el suelo, estrellas y destellos al golpear (más grandes al matar), siluetas brillantes ("imágenes residuales") al esquivar y en la estocada, estrellitas sobre el enemigo aturdido tras un parry, líneas de velocidad, marca de impacto en la mira, números de daño y el nombre del movimiento (CRÍTICO, PARRY PERFECTO...), y un contador de golpes seguidos a la derecha.
- Sensación: estela brillante del filo, sacudida y zoom de cámara, chispas y sonidos propios (tajo, golpe, parry, crítico, esquive, guardia rota, desenvainar). Si haces clic justo antes de poder atacar, el golpe se guarda y sale solo; en servidores con algo de ping el servidor da 2 ticks de margen para que no se pierdan golpes.
- Funciona contra mobs y jugadores (PvP): los bonos se reducen contra jugadores (configurable).
- Con la katana en la mano el clic izquierdo ya no usa el ataque normal del juego.

## Rival de práctica (para probar sin amigos)

Un espadachín con katana controlado por el mod. Usa las mismas reglas que tú: ataca con cadenas de tajos, hace un **crítico anunciado** (levanta la katana con chispas rojas; no se puede parrear, hay que esquivarlo), se cubre, esquiva y se aturde si lo parreas o le rompes la guardia con tu crítico.

- **Cómo sacarlo:** huevo de aparición en la pestaña creativa de Kensei, o `/kensei rival` (pelea) / `/kensei rival dummy` (maniquí). `/kensei rival clear` (administradores) los quita.
- **Cambiar de modo:** agáchate y haz clic derecho sobre él (pelea ↔ maniquí; el maniquí no ataca y se cura solo).
- **Duelo de práctica:** apúntale y pulsa **G**, o `/kensei duel bot` (1v1) / `/kensei duel bot 2v2` (tú + un rival aliado contra dos rivales). Usa la misma arena, rondas, K.O. y HUD que un duelo normal; los rivales aparecen solos y desaparecen al terminar.
- **Ajustes** (config, sección `rival`): `danoTajo`, `danoCritico`, `agresividad`, `seDefiende`.
- Para que te pueda hacer daño y probar parry/esquive tienes que estar en **supervivencia o aventura** (en creativo no te toca).
- Código: `entity/RivalEntity.java` (cerebro), `entity/RivalCommands.java`, `registry/ModEntities.java`, `client/RivalRenderer.java` y `RivalModel.java`; la parte de duelo está en `duel/Duel.java` (`enemyFor`, peleadores `bot`) y `DuelManager.startBotDuel`.

## Duelos 1v1 y 2v2
- **G** (cambiable en Controles > Kensei): mira o fija con el clic central a otro jugador y aprieta G para **retarlo**. El retado recibe un aviso en el chat con botones [ACEPTAR] / [RECHAZAR] y también puede apretar **G** para aceptar. Los dos necesitan una katana en la mano (config `requiereKatana`).
- Al aceptar se construye una **arena en el cielo** sobre el retador (suelo redondo, pilares con luces, muro invisible). Los peleadores aparecen en lados opuestos con vida de duelo (`vida`, 40 por defecto), cuenta atrás **RONDA / 3-2-1 / ¡PELEA!**, tiempo por ronda (`segundosPorRonda`, 99) y mejor de `rondas` (3). Al caer a 0 de vida no mueres: hay **K.O.**, se acaba la ronda y todos vuelven a su sitio con la vida llena.
- **Marcador estilo juego de pelea**: tu vida, dashes, espera del crítico, parries/esquives/golpes arriba a la izquierda; los del rival, en espejo, a la derecha; temporizador y rondas ganadas al centro. Los espectadores ven los dos lados.
- **Espectadores**: el aviso del duelo en el chat tiene un botón [VER] (o `/kensei duel watch <jugador>`). Miran en modo espectador desde la orilla; `/kensei duel leave` para salir.
- **2v2** por comando: `/kensei duel 2v2 <aliado> <rival1> <rival2>`. Si un compañero cae, mira desde el aire hasta que acabe la ronda; si cae todo el equipo, pierde la ronda.
- Al terminar (o si el servidor se apaga a mitad) la arena se desmonta y cada jugador recupera su posición, modo de juego y vida. Abandonar o desconectarse = perder el duelo.
- Comandos: `/kensei duel challenge <jugador>`, `accept`, `decline`, `leave`, `watch <jugador>`, y `end` (solo operadores) para terminar todos los duelos.
- **El servidor debe tener `pvp=true`** en server.properties; con pvp apagado el juego bloquea el daño entre jugadores.
- Config `duelo` en `config/kensei-common.toml`: activado, rondas, segundosPorRonda, vida, requiereKatana, bloquearObjetos (sin comida/pociones/perlas dentro), segundosParaAceptar, anunciar, restaurarVida.

## Ajustes
Todos los números (energía, tiempos, daño, alcance, ángulos, efectos, PvP) están en `config/kensei-common.toml` después de abrir el mundo una vez.

## Agregar más armas después
1. Registrar el arma en `registry/ModItems.java` con `weapon("nombre", () -> new ...)` (aparece sola en la pestaña creativa).
2. Su modelo va en `assets/kensei/models/item/`, con textura en `textures/item/`. Los generadores de la katana (modelo, textura y sonidos) sirven de plantilla.
3. Los movimientos (tajos, estocada, crítico) están en `combat/Moves.java` y las animaciones en `client/KatanaPoses.java` y `client/FirstPersonAnim.java`; un arma nueva puede reutilizarlos o tener los suyos.

## Estructura
- `combat/` reglas del servidor (esquive, parry, golpes `Moves`, combos, energía)
- `item/` la katana · `registry/` objetos, material, partículas, sonidos y pestaña creativa
- `duel/` duelos (arena, rondas, K.O., comandos, respaldo del jugador) · `net/` red (protocolo "4"; `FxPacket` manda al cliente los efectos de golpe, parry, esquive perfecto y bloqueo)
- `client/` teclas, barras en pantalla, posturas y animaciones (3.ª y 1.ª persona), estela, sacudida, inclinación, fijar objetivo, katana en la espalda, `Vfx` (efectos dibujados en el mundo; texturas blancas en `textures/vfx/`, el color lo pone el código), `DamageNumbers` (números, cartel de movimiento y contador de golpes) `ScreenFx` (líneas de velocidad, destellos y marca de impacto), `ClientDuel` y `DuelHud` (marcador del duelo)
