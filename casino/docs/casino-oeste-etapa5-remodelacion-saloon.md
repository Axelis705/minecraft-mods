# Casino del Viejo Oeste — Etapa 5: remodelación del saloon (16-nov-2026)

Axelis mandó un video recorriendo el saloon construido (`/casino build ... saloon`): "hay muchos errores en las paredes, el suelo,
mesas, sillas, está todo mal". Errores encontrados en el video y en las vistas 3D:

| Problema | Causa | Arreglo |
|---|---|---|
| Por dentro del frente se veía la tabla blanca de afuera con los marcos rojos (formas de "T") | el muro del frente era de un solo bloque, sin forro | forro en z = 37 (los dos pisos) con huecos y marcos de tronco para puerta, ventanas y puerta del balcón |
| Rodeo y tiro con paredes blancas de tabla de abedul | los anexos no tenían forro | forro: lambrín de roble oscuro, riel de tronco, tablas de abeto, postes cada 4; vigas en el techo |
| Tierra en las puertas (umbrales) | bajo los muros quedaba el terreno | `floor_under`: madera bajo todos los muros de los tres edificios |
| Ruedo con cuadros amarillos | pacas de heno revueltas en la arena | arena con algo de tierra pisada, cerca de abeto más chica (queda pasillo), gradas con bancas |
| Mesas y sillas hechas de escaleras, trampillas y cercas | no había muebles del Oeste | muebles nuevos (abajo) en toda la planta baja, la galería, la sala de cartas, los cuartos y el palco |
| Desde arriba se veía el hueco del tejado entre las vigas | el techo del primer piso solo tenía vigas | techo cerrado de tablas con vigas cada 3 |
| La puerta del saloon al TIRO llevaba adentro de la caja con rejas | la caja ocupaba z 30..36 | la caja empieza en z 32 (cajeros en z 33 y 35); queda un pasillo libre a la galería |
| Candiles de losas y troncos flotando | candil armado con bloques | bloque nuevo `wheel_chandelier` colgado de cadenas |
| Aparador de losas y barriles en la sala de cartas | | barra (`bar_counter`) mirando al cuarto |
| Marcos blancos en los pasos al rodeo y al tiro | | troncos en los lados y el dintel, marcos en los forros |

## Muebles nuevos (decoración, `herramientas/west_muebles.py`, texturas de Minecraft)
- `saloon_chair` silla con respaldo de travesaños (asiento 0.55), `saloon_stool` taburete (0.85), `saloon_bench` banca con respaldo
  (0.5; en fila se ve como una sola). Son `SeatBlock`: clic derecho para sentarse; los clientes del casino también se sientan.
- `saloon_table` mesa redonda de un pie (cubierta hasta y=16: las bebidas quedan apoyadas), `poker_table` mesa con paño verde,
  fichas y cartas. `wheel_chandelier` rueda de carreta con 8 velas, casi dos bloques de ancho, luz 15.
- Recetas con madera, palos, lana verde, pepita de hierro y velas (la cadena se llama distinto en 26.x). Nombres ES/EN en `west_saloon_lang.py`.

## Otros cambios
- El letrero del récord del toro se dibuja 1 bloque más afuera (ya no queda encima de la cerca nueva).
- CI: `PRUEBA_SALOON_MESAS` revisa el cajero en (339, 101, 173).

## Vistas 3D sin abrir el juego
- `.github/workflows/texturas.yml` (manual) sube los modelos y texturas de bloques de 1.20.1 a la rama `mc-bloques-1.20.1`.
- `estructuras/herramientas/visor/bake.py` + `shoot.py` hornean el volcado de la estructura y toman fotos con Chromium.
  Uso: `EXTRA_ASSETS=casino/src/main/resources python3 bake.py volcado.txt salida --assets /tmp/mcassets --day`, luego
  `python3 shoot.py salida prefijo x,y,z,yaw,pitch ...`.
- Un saloon ya construido en un mundo no cambia solo: hay que volver a construirlo (`/casino build x y z 0 saloon`) o buscar uno nuevo.
