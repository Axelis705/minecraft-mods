# Casino — Etapa 7: suerte real del cóctel, objetos del Oeste y nombres de los clientes (16-nov-2026)

Pedidos de Axelis: "que dé efecto real de suerte, crea más objetos decorativos y después comenzamos con la última estructura";
y "en el casino de diamante había mujeres con nombre de hombre y viceversa, además de que se repetía mucho el mismo nombre Diego".

## Suerte del cóctel (ahora sí sirve en el casino)
- Mientras dura la Suerte que da el bartender, en TODOS los juegos (`CasinoStats.play` → `luck`):
  - si ganas, te dan un extra de la ganancia: +10 / 12 / 15 / 18 / 20 % (Bronce → Diamante);
  - si pierdes, hay 15 / 18 / 20 / 22 / 25 % de que te devuelvan la mitad de lo apostado;
  - tope de fichas de suerte por cóctel: 100 / 200 / 400 / 700 / 1000.
- Solo cuenta la Suerte del bartender y dentro de su tiempo (`Entry.luckUntil`, `luckLeft`, guardados como `lu`, `ll`); una poción
  de Suerte sola no da fichas.
- Avisos: al servir, una línea verde explica qué hace; cada vez que la suerte paga sale "☘ ¡Suerte! +N fichas (te quedan M)" o
  "☘ ¡La suerte te salvó!...", con sonido de amatista y partículas verdes. La tarjeta de socio muestra los números al pasar el ratón
  por la línea del cóctel.
- Constantes en `Membership.LUCK_BONUS_PCT / LUCK_SAVE_PCT / LUCK_CAP`.

## 12 objetos nuevos del Oeste (`herramientas/west_muebles.py`, "segunda tanda")
| Bloque | Qué es | Dónde quedó en el saloon |
|---|---|---|
| `spittoon` | escupidera de latón | al pie de la barra y en el porche junto a la puerta |
| `longhorn_skull` | cráneo de res con cuernos largos (de pared) | sobre el letrero WHISKEY, sobre las gradas del rodeo y sobre el sillón del jefe |
| `horseshoe` | herradura de la suerte (de pared) | a los dos lados de la puerta por dentro y sobre la entrada del rodeo |
| `wagon_wheel` | rueda de carreta recargada (de pared) | en la fachada (afuera) y en el rodeo |
| `oil_lamp` | lámpara de aceite con chimenea de vidrio transparente (luz 14) | en la barra, en el escritorio del jefe y en el cuarto del oeste |
| `whiskey_barrel` | barril acostado con llave de latón | rincón de la pianola |
| `saddle_stand` | silla de montar con cobija de rayas en caballete | entrada del rodeo |
| `water_trough` | abrevadero con agua animada | frente al saloon (antes calderos) |
| `hitching_post` | poste para amarrar caballos (en fila se ve como barandal) | frente al saloon (antes cercas) |
| `coat_rack` | perchero con sombrero vaquero y reata | entrada del saloon y del palco |
| `cash_register` | caja registradora antigua | sobre la barra (las teclas hacia el cantinero) |
| `rifle_rack` | armero con dos rifles (de pared) | junto a los sheriffs de la escalera, en el TIRO y sobre los sillones del palco |

- Los de pared se ponen apuntando a la pared y quedan pegados a ella (espalda en z=16 del modelo "north").
- Texturas propias nuevas en `textures/block/west/`: `brass_old`, `leather`, `blanket`, `lamp_glass` (transparente; el modelo de la
  lámpara usa `render_type: minecraft:cutout`) y `water` (animada).
- `decor_models.box(uv=...)` acepta un quinto número para girar la textura de una cara; `west_muebles.py` corre `zfix` al final.
- Recetas sin objetos que cambian de nombre en 26.x (nada de cadena).

## Nombres de los clientes del Diamond
- Antes el nombre salía del número de piel y estaban al revés: las pieles pares son señores y las nones señoras, pero los nombres
  pares eran de mujer. Y solo había 12 nombres.
- Ahora cada cliente guarda su número de nombre (`DealerEntity` dato sincronizado `NAME`, guardado como `PName`) y el nombre sale de
  la lista que va con su piel: 15 de señor, 15 de señora, 6 "Don" y 6 "Doña" (pieles 4 y 11, de pelo cano).
  `DealerEntity.patronNameKey(variante, nombre)`; claves `casino.npc.patron_m_N`, `_f_N`, `_om_N`, `_of_N`.
- `the_diamond.py` reparte los nombres en orden dentro de cada lista, así en un mismo casino no se repite ninguno
  (marcador `patron dir piel nombre`). Los clientes que ya existían en un mundo sacan su nombre de su UUID.
- Los del Oeste no cambian (su nombre va con su piel y ya coincidía).

## Otros
- `lang_build.py` toma los textos de `textos_ronda5.py` (que vienen en 9 idiomas) antes que el inglés: antes se perdían 286
  traducciones en alemán, francés, etc. (pantallas, torneo, clientes...).
- CI: `PRUEBA_SALOON_OBJETOS` (caja registradora, cráneo, poste, lámpara y perchero en su lugar) y `PRUEBA_DIAMOND_NOMBRES`
  (clientes con piel 5 y 4 llevan el nombre 0 de su lista).
- Un saloon o Diamond ya construido no cambia solo: hay que volver a construirlo (`/casino build x y z 0 saloon|diamond`).
