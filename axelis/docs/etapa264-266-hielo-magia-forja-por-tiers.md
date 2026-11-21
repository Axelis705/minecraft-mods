# Etapas 264 a 266 — Hielo, magia y forja por tiers

Pedido (21 nov 2026): dos estructuras por tier. Cada una empieza en la puerta y termina en lo importante: el bloque (la Piedra de Portal), la celda del habitante o el botín.

| Tier | Grande | Chica |
|---|---|---|
| Hielo | **Ciudadela de cristal**: la Piedra de Portal | **Fortín de Escarcha**: Bruno |
| Magia | **Fortaleza del Archimago**: la Piedra de Portal y Teo | **Torre Arcana**: el tesoro |
| Forja | **Gran Fundición**: la Piedra de Portal e Hilda | **Fragua Olvidada**: el tesoro (y luego un minijefe nuevo) |

Las brújulas y las pistas de los habitantes llevan siempre a la estructura que tiene la Piedra de Portal.

## Hielo (etapa 264)

### Ciudadela de cristal (la grande)

- `/mundos estructura ciudadela`.
- **El final**: la cripta de hielo, al fondo de las mazmorras.
  - Sobre el altar está la **Piedra de Portal: Hielo**.
  - Al lado hay un cofre de provisiones (etapa 3) y un farol de almas.
- **Enemigos**:
  - lobos de escarcha en los patios y en la sala de los carceleros;
  - centinelas de escarcha en la catedral y en las mazmorras;
  - dos centinelas más guardan la cripta.
- El borde se mezcla con el terreno y tiene un camino de hielo y nieve hasta el puente.
- **Aparece sola** en llanuras nevadas, taigas nevadas y picos de hielo: `/mundos natural generar hielo [aqui]`.

### Fortín de Escarcha (la chica)

- Ahora se construye con `/mundos estructura fortin`.
- Se quitó su Piedra de Portal. En el estrado ahora está **la mesa de mapas de Bruno** (se la quitaron los centinelas), con unas velas.
- **El final** es la celda de Bruno.
- Aparece solo: `/mundos natural generar fortin [aqui]`.

## Magia (etapa 265)

### Fortaleza del Archimago (la grande)

- `/mundos estructura fortaleza`. Antes se llamaba «Fortaleza en ruinas».
- **Arriba del todo**, en el observatorio: la **Piedra de Portal: Torre** (el final del recorrido), ahora con un cofre de provisiones (etapa 4).
- **Abajo**, en la mazmorra de la cripta: la **celda de Teo**.
  - Puerta de hierro en el hueco de los barrotes.
  - Barrotes que la cierran.
  - Dentro, su jergón, una estantería con velas y los grilletes.
- **La llave** la lleva la **armadura encantada del observatorio**: hay que subir hasta arriba y volver a bajar.
- Más enemigos en el camino de subida: esbirros en el piso 5 y un libro embrujado en el piso 6.
- El borde se mezcla con el terreno.
- **Aparece sola** en bosques oscuros, de abedules, taigas antiguas y arboledas de cerezos: `/mundos natural generar torre [aqui]`.

### Torre Arcana (la chica: el botín)

- Sigue siendo `/mundos estructura torre`.
- **El final**: en el observatorio, sobre la columna, está **el tesoro del Archimago**. Es un cofre con la cerradura más difícil (nivel 5, etapa 4).
- La antigua celda ahora es la **despensa**:
  - tiene puerta de madera;
  - dentro hay barriles, sacos y un cofre de premio (etapa 4).
- La armadura del observatorio guarda el tesoro (ya no lleva llave).
- Aparece sola: `/mundos natural generar arcana [aqui]`.

## Forja (etapa 266)

### Gran Fundición (la grande)

- `/mundos estructura fundicion`.
- **El final**, abajo en los hornos profundos: en la isla del lago de lava, la **Piedra de Portal: Forja**, con un cofre de provisiones (etapa 2).
- En la pared del fondo de los hornos está la **celda de Hilda**:
  - barrotes que dan al lago y una puerta de hierro;
  - dentro, su jergón, un yunque roto, un caldero, una vela y un farol.
- **La llave** la lleva el **Fundidor que vigila la isla**.
- **Enemigos**: escorias en los patios, en la nave y en los hornos, chisperos, y otro Fundidor en la nave.
- El borde se mezcla con el terreno.
- **Aparece sola** en tierras baldías, colinas azotadas por el viento y mesetas de sabana: `/mundos natural generar forja [aqui]`.

### Fragua Olvidada (la chica: el botín)

- Ahora se construye con `/mundos estructura fragua`.
- **El final**: sobre el altar con forma de yunque está **el tesoro del Gran Herrero**, un cofre con cerradura difícil (nivel 4, etapa 2).
- La antigua celda ahora es el **almacén**: puerta de madera, barriles y un cofre de premio.
- El Fundidor guarda el altar.
- Aparece sola: `/mundos natural generar fragua [aqui]`.
- **Pendiente**: el minijefe nuevo (en diseño).

## Mundos ya empezados

- Las estructuras «chicas» que ya habían salido cerca (fortín, torre arcana, fragua) se conservan con su nombre nuevo.
- Las grandes empiezan a aparecer desde ahora.

## Comprobado

- Compila.
- **Plano revisado fuera del juego**: las 9 estructuras en verde.
  - Las únicas «salas sin acceso» son las celdas, cerradas a propósito.
  - Se arregló que la revisión automática abría una escalera hasta la celda de Hilda: ahora la celda se hace después.
- Vistas del visor:
  - la cripta de la ciudadela con la Piedra de Portal;
  - la celda de Teo en la cripta de la fortaleza;
  - el observatorio de la fortaleza con la piedra y el cofre.
- **Servidor de verdad (CI)**, para cada estructura:
  - la piedra o el tesoro en su sitio;
  - las celdas, los enemigos y la llave;
  - el rescate;
  - deshacer;
  - la versión «natural».
