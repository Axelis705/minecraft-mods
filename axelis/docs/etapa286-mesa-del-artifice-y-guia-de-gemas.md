# Etapa 286 — Mesa del Artífice: crear de verdad, todos los reparadores, y la Guía del Gemista

Pedido de Axelis: la Mesa del Artífice con más cariño y detalle; mantener el remache ardiente y el dorado y añadir todos los demás
objetos que reparan armaduras, que se puedan **crear** (no solo comprar) una vez descubiertos; precios más altos (hecho en la 282).
Y un libro guía para la Mesa de Gemas.

## Pestaña «Crear» por categorías (`table/TableRecipes.java`, `client/TableScreen.java`)
- Tres sub-pestañas con contador de lo que conoces: **Reparar**, **Útiles** y **Base**.
- **Reparar** (16 recetas):
  - el **Remache Ardiente** y el **Remache Dorado**, siempre;
  - los demás reparadores de cada tier (bellota y bellota dorada, escamas de escarcha, ceniza templada y corazón de ceniza, runas de
    reparación, escamas de dragón, plumas tejidas y astillas del vacío) **se aprenden al tenerlos una vez**. Mientras tanto salen
    como «??? (aún no lo conoces)».
  - Cada uno necesita Esencia, materiales y nivel de Mesa.
- **Útiles**: anillo del herrero, bayas del santuario, asado de la forja y gemas vacías.
- **Base**: Gran Yunque, Mesa de Gemas, Guía del Gemista y Tablón de Encargos.
- Al aprender una receta nueva sale un aviso: «Nueva receta en la Mesa del Artífice».

## Guía del Gemista (`gema/GuiaGemasItem.java`, `client/GuiaGemasLibro.java`)
- Un libro de verdad (se abre como los libros del juego) con 7 páginas: qué son las gemas, gemas vacías, esencias, infundir,
  combinar, engarzar y la pureza.
- **La primera vez que usas la Mesa de Gemas te la regala.** También se crea en la Mesa del Artífice (pestaña Base).
- Textura propia (libro morado con una gema).

## Pruebas
- **Servidor** (`mundos prueba etapa286`): todas las recetas están en una pestaña que existe; están los 16 reparadores; la bellota se
  aprende al tenerla y el remache ardiente está siempre; la guía se regala una sola vez; caben todas las recetas en los botones.
- **Cliente** (fotos 190–193): la pestaña Crear en Reparar y en Base, y la guía abierta en dos páginas.
