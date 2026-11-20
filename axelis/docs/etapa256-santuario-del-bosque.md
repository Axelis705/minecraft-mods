# Etapa 256 — Santuario del Bosque (la primera estructura) y estructuras que no se pueden picar

Pedido (20 nov 2026): antes del siguiente jefe, pulir lo que ya hay. Faltan las estructuras del bosque y del hielo; empezar por la del bosque.
- Una estructura **sencilla** (es la inicial) donde salgan enemigos y al final esté el bloque para ir a la dungeon.
- Una **celda** para el NPC que pondremos.
- Ni las dungeons ni las estructuras deben poder picarse.

## Santuario del Bosque (`/mundos estructura bosque`)

Unos 27 × 40 bloques. Se entra por un camino de tierra con faroles en postes y un tronco caído.
- **Muro en ruinas** con musgo alrededor de un patio, con un arco a la entrada y faroles en las esquinas.
- **Patio**: flores, helechos, un estanque con nenúfares y una columna caída.
- **Santuario** de piedra con musgo:
  - pilares y vigas de roble;
  - tejado bajo de abeto con alero oscuro, con musgo y hojas creciendo encima y algún hueco;
  - bancos, faroles colgados, enredaderas.
- **Altar** al fondo con la **Piedra de Portal: Naturaleza** (lleva a la dungeon del Rey Brote, como siempre), azaleas en flor y musgo en la pared.
- **Cofre de provisiones** del santuario (etapa 1) junto al altar.
- **Celda** al lado del salón:
  - barrotes de hierro que dan al salón y una puerta de hierro sin manivela (no se abre: el NPC se rescatará más adelante);
  - dentro, cama de paja, farol de almas, caldero, vela y telarañas.
  - El sitio del NPC está apuntado en el diseño (`DesignSantuario.NPC_X/NPC_Z`).
- **Un roble enorme** en la esquina de atrás, con la copa sobre el tejado.
- **Enemigos** (aparecen una vez): 3 Brotes en el patio, un Espinoso y un Raizal que guarda el altar.
- **El claro**: bajo la obra el terreno queda plano. Alrededor hay una rampa suave de 7 a 10 bloques hasta el terreno real, sin ceniza (el pasto de siempre); se quitan los árboles que estorben.
- El templo grande de antes sigue disponible como `/mundos estructura templo_bosque`.

## Aparece solo en el mundo

- Sistema común nuevo (`NaturalGen`) para el bosque y el volcán. Antes la aparición natural estaba solo dentro del volcán: ahora es compartida; las regiones ya vistas del volcán se pasan solas.
- **Santuario**:
  - regiones de 640 bloques, más de la mitad con santuario;
  - la región del punto de aparición **siempre** tiene uno (es la primera estructura), a partir de 200 bloques;
  - en bosques, taigas, llanuras o praderas;
  - se construye cuando alguien pasa a unos 220 bloques.
- **Volcán**: igual que en la etapa 255.
- Comandos:

  | Comando | Qué hace |
  |---|---|
  | `/mundos natural cerca` | Qué hay en la región donde estás, de cada tipo. |
  | `/mundos natural generar <bosque\|volcan> [aqui]` | Construye uno "natural". |
  | `/mundos natural activar <tipo> <true\|false>` | Activa o quita la aparición sola. |

  Los de `/mundos volcan ...` siguen igual.

## No se pueden picar

- `StructGuard` (nuevo, guardado con el mundo): cada estructura que se construye se apunta, sea con el comando o sola.
  - En supervivencia nadie puede romper sus bloques (paredes, suelos, adornos, la Piedra de Portal, la celda...). Sale el aviso «Esta estructura no se puede romper».
  - El terreno de alrededor sí se puede picar.
  - Las explosiones tampoco rompen la obra.
  - En creativo se puede todo, para editar y probar.
  - `deshacer` la quita de la lista.
- Dungeons y arenas: ya no se podían romper ni colocar bloques. Ahora además ni siquiera empieza a picarse: no salen grietas que luego desaparecen.
- Los volcanes y estructuras que ya existían antes de esta versión no estaban apuntados. Habría que volver a construirlos.

## Comprobado

- Compila (740 clases).
- Plano revisado: conectividad del santuario con 1 sala sin acceso tolerada (la celda, cerrada a propósito), 0 trampas, entrada transitable. Las otras 5 estructuras siguen en verde.
- Vistas del visor: fuera, patio, salón, altar y celda.
- **Servidor de verdad (CI)**: todo en verde.
  - Piedra de Portal en el altar, puerta y barrotes de la celda, cofre con sus datos (provisiones, etapa 1).
  - Los Brotes, el Espinoso y el Raizal.
  - `deshacer` lo quita todo, enemigos incluidos.
  - Un santuario "natural" construido por pasos (`/mundos natural generar bosque aqui`).
  - El volcán natural sigue funcionando con el sistema común.
  - Detalle de la prueba: la zona de prueba se mantiene cargada (`forceload`); si no, el servidor la descargaba antes de comprobarla.
- **Juego real (cliente)**: en el altar está la Piedra de Portal: Naturaleza; se ven los enemigos (3 Brotes y el Raizal).
  - **No se puede picar** en supervivencia: se intentó romper una pared y la Piedra de Portal y las dos siguen ahí.
  - Fotos de frente, de lado, del salón con el Raizal junto al altar y de la celda.

## Pendiente

- El NPC de la celda (cuando se haga).
- La estructura del hielo.
