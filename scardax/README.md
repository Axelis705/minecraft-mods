# Scardax

Mod de Minecraft Forge 1.20.1 (Forge 47.4.10, Java 17) para controlar el spawneo de mobs desde dentro del juego.

- mod_id: `scardax`  ·  paquete: `com.scardax`  ·  clase principal: `Scardax.java`
- Proyecto independiente de "Corazones": no comparten código ni identificador.
- Para compilar: `gradlew build` (el .jar queda en `build/libs`).

## Uso
`/scardax` (o `/quitarmobs`, solo operadores) abre la pantalla. Cada mob de **cualquier mod** aparece con su dibujo, su mod y su categoría:
- barra de spawneo de 0 a 300 % (100 % = normal, menos = aparece menos, más = aparecen extra en apariciones naturales);
- botón **Quitar** (pone 0 %: no vuelve a aparecer);
- filtros por texto, categoría, mod y estado; botones para quitar o restaurar todos los visibles;
- **Limpiar ya cargados**: borra los mobs de 0 % que ya están en el mundo (no toca a los que tienen nombre, mascotas, atados ni montados);
- **Modo estricto**: además del evento normal, controla a cualquier mob que otro mod meta al mundo por su cuenta (también afecta a huevos, comandos y crías de los mobs por debajo de 100 %).

Las reglas se guardan con el mundo. Funciona aunque solo el servidor tenga el mod; quien abre la pantalla lo necesita en su juego.
Solo afectan apariciones que dependen del mundo (natural, generación de terreno, estructuras, spawners, patrullas, refuerzos, jinetes); lo que invoca un jugador no se toca (salvo modo estricto).

## Estructura
- `rules/` reglas guardadas (`SpawnRules`), evento de spawneo (`SpawnHandler`), limpieza (`Purge`)
- `net/` canal y paquetes · `command/` el comando · `client/` la pantalla y la lista de mobs
