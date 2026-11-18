# Etapa 246 — Orden de etapas, menú de opciones y dificultad

Respuesta a la prueba del usuario con 3 videos (18 nov 2026).

## Lo que pidió y lo que se hizo

| Pedido | Qué se hizo |
|---|---|
| El jefe que sigue al Hielo es el de **fuego**, no el de magia (orden de las cajas) | El **Archimago pasa a etapa 5**. La etapa 4 queda reservada para el jefe de fuego (pendiente). Piedra de Portal: Torre pide ahora nivel del mundo 4. Núcleo de Magma = material de etapa 4; Fragmento Arcano = etapa 5. Comandos de prueba: `entrar5`, `jefe5`, `arena5`, `limpiar5`, `construir 5`. Entrar a la etapa 4 avisa "todavía no existe". |
| Un comando para no tener que vencer a todos los jefes antes | `/mundos desbloquear <etapa>` (1–8, nivel de permisos 2). Sube el nivel del mundo a `etapa-1` y apunta el avance en el Archivo de los jugadores conectados. Ej.: `/mundos desbloquear 5`. |
| El jefe nuevo mató de un golpe; que el mundo normal **no** se vuelva más difícil | Se quitó el aumento de vida/daño de los monstruos del mundo (`WorldTierEvents.OVERWORLD_SCALING = false`). Los monstruos ya escalados pierden el modificador al cargar. El nivel del mundo solo sirve para abrir la siguiente Piedra de Portal. Nuevo `BossHitCap`: en dungeons y arenas, ningún golpe quita más del **40 % de la vida máxima** (no afecta `/kill` ni el vacío). |
| Mucho texto en pantalla al pasar rápido la dungeon | Opinión: es en parte por ir rápido, pero había exceso real (tarjetas de 10 s, hasta 5 apiladas, "la puerta de atrás se cerrará" en cada sala, consejos de enemigos repetidos). Ahora: aviso de la puerta solo en la primera sala, cada consejo de enemigo una vez por partida, avisos de 5,5 s, y opción "Pocos avisos" (uno a la vez, máximo 3,5 s). |
| Un cuadrito propio en el menú de Minecraft para cambiar cosas (p. ej. ocultar la barra de almas) | Botón 20×20 con el icono de la Piedra de Portal arriba a la izquierda del **menú de pausa** → pantalla "Opciones de Mundos de Axelis": Barra de almas, Mapa de la dungeon, Pocos avisos en pantalla, Música de dungeons y jefes. Se guarda en `config/mundos_de_axelis-client.toml`. También se abre desde la lista de Mods → Configurar. |
| Bug al ponerse en espectador en dungeons 1 y 2 | Causa encontrada: si no quedaba ningún jugador activo (el espectador no cuenta), a los 2 s la dungeon se reiniciaba y se reconstruía. Ahora, si hay alguien en espectador, la dungeon o la arena se queda **en pausa** y sigue igual al volver a supervivencia (`BossTravel.hasSpectator`, aplicado en DungeonManager, ForgeManager, IceTravel y TowerTravel). |

## Archivos

- Nuevos: `client/MundosOptionsScreen.java`, `client/PauseMenuButton.java`, `world/BossHitCap.java`.
- Cambiados: `ClientConfig` (sección `pantalla`), `MundosDeAxelis` (ConfigScreenFactory), `SoulsHud`, `DungeonMapHud`, `NoticeHud`, `AmbientMusic`, `BossTravel`, `ModDimensions`, `BossRewards`, `BossLore`, `BossDamage`, `Archimago`, `Mats`, `ModBlocks`, `TestCommands`, `OwnCommands`, `WorldTierEvents`, `DungeonManager`, `ForgeManager`, `Titles`, `TowerTravel`, `BossResultScreen`, lang (es_es, es_mx, en_us).

## Comprobado

- Compila (628 clases).
- CI en servidor Forge real: `construir 5` (Torre) OK, `construir 4` falla con aviso limpio, `desbloquear 5` deja el mundo en nivel 4, demás etapas y enemigos de la Torre OK.

## Pendiente

- Jefe y dungeon de **fuego (etapa 4)**.
- Mascotas exclusivas de jefes (Lobo Ártico y Archimago).
- Los Brotes del bosque siguen apareciendo en el mundo tras vencer al Rey Brote (son enemigos del mod, no un aumento de dificultad); preguntar si se quieren quitar.
