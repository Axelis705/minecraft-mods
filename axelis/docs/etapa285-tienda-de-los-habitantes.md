# Etapa 285 — La tienda de cada habitante: el camino de uno en uno

Pedido de Axelis: cada NPC solo debe vender la ubicación hasta el siguiente NPC, y la tienda necesitaba una interfaz mejor.

## Qué vende cada uno (`npc/NpcShop.java`)
- **Ruda** (bosque): la brújula al sitio de **Hilda** (la Forja).
- **Hilda** (forja): la brújula al sitio de **Bruno** (el Fortín) y las del bosque (Santuario y Templo), para volver.
- **Bruno** (hielo): la de **Ignacio** (el Volcán) y las del bosque y la forja.
- **Ignacio** (volcán): la de **Teo** (la Torre) y las de antes.
- **Teo** (torre), el último: todas.
- Precio de la brújula según el tier del sitio: 55, 85, 110, 140 y 170 de Esencia.
- El resto de la tienda (provisiones y cosas especiales que se desbloquean con más vecinos) sigue igual.

## La ventana nueva (`client/NpcScreen.java`)
- Pestañas a la izquierda: **Destinos**, **Provisiones** y **Especiales**.
- A la derecha, la lista con iconos, precio y si te alcanza. La brújula al siguiente habitante sale marcada como «Siguiente».
- Debajo, la descripción de lo que tienes seleccionado y el botón **Comprar**.

## Pruebas
- **Servidor** (`mundos prueba etapa285`): lo que vende cada uno de los cinco (siguiente sí, el propio no, los anteriores sí).
- **Cliente** (fotos 180 y 181): la tienda de Hilda en Destinos y en Provisiones. En la primera prueba falló porque Hilda estaba dentro
  de su casa, a más de 6 bloques (el servidor no deja hablar desde tan lejos): ahora la prueba la hace salir a la puerta.
