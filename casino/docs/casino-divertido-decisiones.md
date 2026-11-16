# Casino Divertido (la sala recreativa) — decisiones de Axelis (16-nov-2026)

Es el tercer y último casino (ver `casino-ronda2-tres-casinos.md`): biomas normales (bosque, llanuras), muy raro, por fuera una
tragamonedas gigante y por dentro una sala recreativa (nunca "parque de diversiones").

## Decidido
- **Se juega con monedas arcade** (objeto nuevo). Las máquinas **sueltan boletos** cuando ganas.
- Los boletos se cambian en el **cajero** (por fichas o por dinero) o en el **mostrador de premios** del arcade.
- **Cajeros mejorados en todos los casinos**: una interfaz mejor, con apartados para comprar las tarjetas VIP y para cambiar
  dinero (esmeraldas, diamantes) por fichas, monedas arcade o boletos, y al revés.
- **Arreglar precios**: en la tienda de premios el diamante cuesta 100 fichas y en el cajero 80; tiene que encajar.
- **Juegos**: máquina de garra, empuja-monedas, carrera de patos, pachinko, golpea al topo, skee-ball y canasta de básquet.
- **Bolos, hockey de mesa y billar**: ya existen en el mod `pooltable`; Axelis eligió **copiarlos al casino** para que funcionen
  sin el otro mod (quedan repetidos si tiene los dos).
- **Estilo**: retro neón de los 80 (alfombra oscura con figuras de colores que brillan, letreros de neón, luces que parpadean).
- **Piso VIP** arriba (tarjeta Plata u Oro), con juegos de premios más grandes y un bar.
- Sin póker ni ruleta europea (como en el Oeste).

## Orden propuesto
1. Cajeros nuevos (apartados Fichas / Arcade / VIP), monedas arcade y boletos, precios que encajan.
2. El edificio: tragamonedas gigante por fuera, sala recreativa neón por dentro, mostrador de premios, piso VIP.
3. Juegos en tandas: garra, empuja-monedas, pachinko y carrera de patos; luego topo, skee-ball y básquet; al final copiar
   bolos, hockey de mesa y billar del mod `pooltable` (adaptándolos a las 5 versiones del casino).

## Hecho: etapa 1 — caja nueva, monedas arcade y boletos (16-nov-2026)
- `game/Exchange` (pura, prueba `tests/ExchangeTest` en el CI): valores en fichas — ficha 1, moneda arcade 5, boleto 1,
  esmeralda 10, diamante 80. Se cambia por lotes enteros y vale lo mismo de ida y de vuelta (8 esmeraldas = 1 diamante,
  5 fichas = 1 moneda, 1 diamante = 16 monedas...).
- Objetos nuevos `casino:arcade_coin` (Moneda arcade) y `casino:arcade_ticket` (Boleto), con su valor en el texto de abajo
  (`item/ArcadeItem`, texturas de `herramientas/arcade_items.py`).
- Caja (`client/CashierScreen`), dos apartados:
  - **Cambio**: arriba lo que llevas de cada cosa; eliges qué ENTREGAS y qué RECIBES (5 casillas cada uno; si eliges lo mismo
    se voltea), la cantidad (-10, -1, +1, +10, Máx) y abajo el cambio exacto antes de CONFIRMAR.
  - **Tarjetas VIP**: Plata y Oro con precio, qué abren y si ya la tienes. Si la tenías registrada y perdiste la tarjeta, al
    "comprarla" te dan una copia sin cobrar.
  - Paquete: `CashierPacket` con kind = 1000 + de × 10 + a y la cantidad en lotes (`Cashier.convert`).
- Tienda de premios: el diamante ahora vale 80 fichas (lo mismo que en la caja); las tiendas ya construidas con el precio viejo
  (100) se corrigen solas al cargar.
- Guía (página "Fichas y cajero") actualizada con los valores nuevos.
