# Etapa 282 — Arreglos rápidos (lo que salió al probar)

Primera tanda de la lista de cosas que Axelis encontró jugando. Lo grande (animaciones de todo el cuerpo, dos manos, IA de los
jefes, tiendas de los NPC, Mesa del Artífice y los reworks de estructuras) va en las etapas 283 a 287.

## Qué cambió

### Cofres volteados
- **Causa:** el Cofre del santuario (y la Tumba) no giraban al construir una estructura girada. La estructura se gira según desde
  dónde se pide (3 de cada 4 veces), así que en la mayoría de los casos el cofre quedaba mirando a la pared.
- **Arreglo:** `VaultChestBlock` y `GraveBlock` ya tienen `rotate` y `mirror`, como el resto de bloques con dirección.
- Además había 4 cofres puestos al revés en el plano (santuario, forja, fragua y fortín): ahora miran a la sala.

### Revisión de planos (nueva, también en el CI)
- `axelis/herramientas/RevisarPlanos.java` construye cada plano fuera del juego y comprueba:
  - que el frente de cada cofre dé a un hueco;
  - que cada puerta de hierro tenga sus dos mitades, suelo, paso delante y detrás, y muro al menos a un lado;
  - que cada Altar Maldito tenga suelo, 3 bloques de aire encima y un claro de radio 2 para la oleada;
  - que cada mímico esté junto a un cofre de verdad, sobre suelo y sin nada dentro.
- Si un altar está mal, propone el sitio bueno más cercano.
- Se ejecuta en el CI (trabajo «estructuras»), después de la prueba de conectividad. Si algo falla, el CI falla.
- **Arreglado con la revisión:**
  - el altar del santuario estaba en la puerta de la nave: pasa a la nave (1, 0, 22);
  - el de la torre estaba torcido sobre la alfombra: pasa al centro (0, 3, 29);
  - el mímico del santuario estaba dentro de una maceta y el del hielo en otra sala sin cofre: ahora están junto a su cofre.

### Puertas de hierro
- En el gran portal de la Ciudadela había dos puertas de hierro sueltas, sin marco, y una no se podía abrir. Se quitaron.
- El resto de puertas (celdas) pasan la revisión.

### Fragua
- El «palo» del centro era el mango del martillo gigante, que colgaba hasta media sala. Ahora el mango va arriba, hacia las
  cadenas, y la sala queda libre.

### Volcán sin netherita (y en general)
- Los cofres del volcán daban restos antiguos. Ahora dan ceniza templada (el remache de la armadura del volcán).
- **Cajas, cofres de tiers altos y cajas misteriosas:** la netherita se cambió por cosas útiles con el equipo del mod: experiencia,
  bloques de diamante, tótems y manzanas encantadas. El pico de netherita encantado se queda porque es una herramienta.

### Arrojadizos → «estallido» de las armas
- Las bombas ya no salen en cofres, barriles, élites, Saqueadores, encargos ni en la pestaña del mod.
- Siguen registradas: las que ya tengas siguen funcionando.
- **Ahora son un efecto de las armas.** Un golpe cargado tiene un 20 % de «estallar» con el arrojadizo de su tier:
  - bosque: esporas;
  - forja: metralla;
  - hielo: escarcha;
  - volcán: erupción;
  - del tier 5 en adelante: pulso arcano.
- Entre un estallido y el siguiente hay 5 s de espera.
- La ficha del arma lo explica (línea con ✸).
- El encargo «Lanza N arrojadizos» pasa a ser «Provoca N estallidos con tus armas».
- **Detalle técnico:** el estilo del arma (pasivas y elemento) ahora solo se aplica a golpes cuerpo a cuerpo (`PLAYER_ATTACK`). Así
  el daño del estallido no vuelve a disparar el estilo.

### La llave de la celda siempre la lleva alguien (adiós «forzar con 4 de hierro»)
- Se quitó el botón «Forzar».
- Al hablar con el preso, la barra de acción dice quién lleva la llave y hacia dónde está, desde ti (por ejemplo: «al noreste, más
  arriba, a 23 bloques»). Si la llave está tirada en el suelo, también lo dice.
- **Si no hay guardián ni llave por ningún lado** (se cayó a la lava, se fue lejos o es una estructura vieja), llega otro guardián
  con la llave a su puesto. Como mucho llega uno por minuto, y el preso lo comenta con una frase suya.
- El guardián de la llave tiene un nombre dorado siempre visible («Raizal con la llave»…) y no sale como élite.
- La llave tirada ya no desaparece del suelo.
- **Al construir la estructura,** al preso se le apunta el sitio, el tipo y el UUID de su guardián. En estructuras viejas, el sitio
  sale de la puerta de la celda: al otro lado, a 6–12 bloques.

### Los enemigos se quedan dentro («correa», `world/Correa.java`)
- Cada enemigo que pone una estructura recuerda su sitio.
- **Persecución:** persigue a quien entra, pero no fija como objetivo a alguien que está lejos de su zona. Si el jugador se aleja,
  lo suelta y vuelve. Si acaba muy lejos o en la lava, reaparece en su sitio entre humo.
- **Radios:**
  - normal: 26 bloques (persigue hasta 34);
  - guardián de la llave: 12 bloques (persigue hasta 16). Así ya no se sale del fortín.
- Los guardianes de estructuras viejas toman como sitio el lugar donde están al cargarse.

### Piedra de regreso con confirmación
- Al tocarla se abre una ventanita: «¿Salir del volcán?», con Cancelar y Confirmar.
- Confirmar se va llenando y se puede pulsar a los 3 s.
- El servidor no acepta la confirmación antes de tiempo, pasados 2 minutos ni lejos de la piedra.
- Paquetes genéricos `ConfirmarPacket` / `ConfirmadoPacket` (sirven para otras confirmaciones). La versión del protocolo pasa
  a 23.

### Todas las semillas de casa de una
- Comando: `/mundos vecinos semillas`.
- Da la semilla de cada habitante que aún no tiene casa. Quien siga preso pasa a «libre», para poder plantar su casa (la de la
  celda desaparece sola).

### Precios más altos
| Qué | Antes → ahora |
|---|---|
| Llaves en la Mesa | +35 % (bosque 30 → 40 … abismo 400 → 540) |
| Subir de nivel un objeto | +30 % |
| Ligar un objeto | +25 % |
| Mejorar la Mesa | +20 % |
| Recetas de «Crear» | +25–35 % |
| Tienda de los habitantes | +40 % |

Lo que te devuelve desmontar no cambia.

## Pruebas
- **Servidor** (`mundos prueba etapa282`, `#expect PRUEBA_ETAPA282_OK`):
  - el cofre gira con la estructura (bloque y plano);
  - no se puede confirmar al momento ni otra cosa, y sí a los 3 s;
  - el estallido de esporas envenena a dos muñecos;
  - en una celda de prueba sin guardián llega uno nuevo con marca, correa y nombre, al otro lado de la puerta; no llega otro sin
    esperar;
  - un enemigo de estructura llevado a 80 bloques vuelve a su sitio;
  - 1.280 cofres sin netherita;
  - el comando de semillas da una por habitante sin casa y los deja libres (luego la prueba lo deja como estaba).
- **Arrojadizos** (`PRUEBA_ARROJADIZOS_OK`): ahora comprueba que ningún cofre los trae.
- **Saqueador:** el saco lleva experiencia en lugar de bombas.
- **Planos:** `RevisarPlanos` → `PLANOS_OK` (también en el CI).
- **Cliente:**
  - fotos 140 y 141: la ventanita esperando y lista;
  - la ficha del arma tiene la línea del estallido;
  - las bombas no salen en la pestaña.
