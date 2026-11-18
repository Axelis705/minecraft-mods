# Etapa 248 — Mascotas exclusivas del Lobo Ártico y del Archimago

Pedido: "Mascotas exclusivas de jefes" (elegido junto con los enemigos de la Torre). Las de los jefes 1 y 2 ya existían (Brotecito Rey, Gran Herrerito).

## Las mascotas
| | Lobito Ártico (`comp_frost_pup`) | Archimaguito (`comp_little_archmage`) |
|---|---|---|
| Jefe | Lobo Ártico (etapa 3) | Archimago (etapa 5) |
| Especie 3D | 65 (`Creature3D.FROST_PUP`) | 66 (`Creature3D.LITTLE_ARCHMAGE`) |
| Aspecto | lobito blanco-azulado de peluche con capucha negra con orejas, capa rasgada, guadañita de escarcha a la espalda, cristales de hielo en los hombros, calaverita en el cinturón, cola esponjosa | magito con túnica añil y estrellas, ribete dorado, barba blanca en mechones, ojos que brillan, sombrero enorme con punta doblada, bastón con cristal, librito que flota y tres cristalitos que lo rodean (como el Archimago). Flota un poco: no tiene pies |
| Rareza | Legendaria | Mítica |
| Armadura equipada | 6 (estrella de la caja de Hielo) | 10 (estrella de la caja de Magia) |
| Probabilidad | 15 % (como las demás mascotas de jefe) | 15 % |

- Solo caen de su jefe (`Tiers.CRAFT_ONLY`: no salen en cajas ni cofres). Antes el Lobo daba el Yetito y el Archimago el Pilluelo Lunar (que siguen en sus cajas).
- Modelos en `vfx/Pets20.java`. Registradas en `Companions`, `ModItems`, pestaña creativa, `Tiers`, `CratePools.petArmor`, `BossRewards.spec(3)` y `spec(5)`, lang ×3 (nombre, 2 tooltips y lore).

## Herramienta nueva: `herramientas/PetBake.java`
- Dibuja una mascota de `Creature3D` sin Minecraft (rasterizador propio con profundidad, transparencias y brillos aditivos).
- Saca a la vez:
  - vistas grandes (frente, 3/4, lado, espalda) para revisar;
  - el icono animado 32×32 de 12 cuadros (`textures/item/<id>.png` + `.mcmeta`), igual que los demás;
  - la figurita 3D del objeto (`models/item/relic3d/<id>_3d.json` + paleta `textures/item/relic3d/<id>.png`, cajas de 0,5 con la profundidad del cuerpo) y el `models/item/<id>.json` (`forge:separate_transforms`).
- Probada contra el Gran Herrerito: icono y figurita casi iguales a los originales (450 cajas frente a 436).
- Uso: `javac -d out -cp <clases del mod> PetBake.java` y `java -cp <clases del mod>:out com.mundosdeaxelis.vfx.PetBake <especie> <tamaño> <id> <assets/mundos_de_axelis> <carpeta de vistas> <rgb>`; necesita `textures/vfx/creature3d.png` y `soft.png` en esa carpeta de assets.

## Comprobado
- Compila; CI de servidor verde.
- Prueba con el cliente real (run 44340637753): los iconos se ven en la barra y el inventario, y las dos mascotas invocadas aparecen a tu lado con su modelo 3D. 12 comprobaciones, 0 fallos, sin errores de modelos.
- La prueba ahora quita a los enemigos sin matarlos (si moría el Archimago contaba como victoria y llenaba la pantalla de premios).

## Pendiente
- Jefe y dungeon de fuego (etapa 4): falta decidir el diseño con Axelis.
