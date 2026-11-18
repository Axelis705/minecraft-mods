# Etapa 247 — Prueba con el juego real (fotos) y más opciones

Pedido (18 nov 2026): "sigue con el botón del menú y los errores"; los enemigos que salen por el mundo tras vencer jefes (Brotes) **se quedan**.

## Prueba automática con el cliente real
- Workflow nuevo `.github/workflows/cliente-axelis.yml` (se lanza a mano: `gh workflow run cliente-axelis.yml`).
  - Abre Minecraft 1.20.1 de verdad (Xvfb + dibujo por software) con el mod, en un mundo plano creado por el servidor de prueba.
  - Las texturas, modelos, sonidos e idiomas salen de la rama **`recursos-axelis`** (el módulo `axelis/` solo tiene código y datos). Para actualizarla: `scratchpad/push_recursos.sh` (jar del usuario del 15 nov + recursos de la nube encima).
  - Fotos y registro van a la rama **`fotos-axelis`**.
- `client/AutoTest.java` (solo actúa con `-Dmundos.autotest=true`; en el juego normal no hace nada):
  - pulsa "Continuar" en el aviso de Forge de "ajustes experimentales" (sale una vez por mundo por las dimensiones del mod);
  - fotos: HUD, menú de pausa con el cuadrito, opciones, explicación al pasar el ratón, opciones con la barra apagada, HUD sin barra de almas, inventario, pila de libros en 4 direcciones, enemigos de la Torre y el Archimago;
  - comprobaciones: cuadrito en (6,6) 20×20, abre `MundosOptionsScreen`, 6 interruptores, la opción se guarda en `mundos_de_axelis-client.toml`, "Aceptar" vuelve al menú de pausa, la pila de libros se pone, 4 de 4 enemigos se ven.
- Resultado (run 44334833677): **10 comprobaciones, 0 fallos, 9 fotos, 0 errores de modelos o texturas.**
- Primer intento colgado 30 min: el mundo creado por el servidor mostraba el aviso de ajustes experimentales y nadie lo pulsaba.

## Errores del registro del usuario
- `est_pila_libros` (rotación) y `magma_boots` (modelo): en el registro del usuario con el jar nuevo (08:36) ya **no salen**. Confirmado también por la prueba con cliente.
- Las líneas "Incorrect key pantalla..." del primer arranque son normales: Forge añade las opciones nuevas al archivo de configuración.

## Error encontrado por Axelis: "Todas las copias de esta dungeon están ocupadas"
- Al usar la Piedra de la Torre tras `/mundos desbloquear 5` salía ese aviso siempre.
- Causa: en el cambio de etapa (246) quedó `TowerTravel.enter` buscando las copias de la **etapa 4** (`pickCopy(server, 4)`), que ahora es la de fuego y no tiene copias. Ahora usa `Archimago.STAGE` (5).
- El CI de servidor no lo vio porque `construir 5` no pasa por la piedra. Por eso la prueba con cliente ahora hace lo mismo que un jugador: pone la Piedra de la Torre, clic derecho, abre la ficha, "Comenzar" a los 5 s y comprueba que llega a `boss_tower` (run 44336967679: **12 comprobaciones, 0 fallos**).

## Opciones (menú de pausa → cuadrito)
- Ahora 6 interruptores en dos columnas, cada uno con explicación al pasar el ratón:
  Barra de almas, Mapa de dungeon, Pocos avisos, Música de jefes, **Sacudidas de cámara** (nuevo, `screen_shake`), **Destellos** (nuevo, `flashes`: apaga los destellos de color `ScreenFxPacket.FLASH`).
- `/mundos desbloquear <etapa>`: un solo aviso en el chat (antes salía uno por etapa).

## Pendiente
- Jefe y dungeon de fuego (etapa 4).
- Mascotas exclusivas del Lobo Ártico y del Archimago.
