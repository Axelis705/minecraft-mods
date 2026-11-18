# Etapa 244 – Música del Palacio de hielo y sonidos propios del Archimago (18-nov-2026)

Axelis: "sigue mejorando y puliendo todo en general". Entregado a `Desktop\Mod\1` (20 archivos). Compila sin errores con javac y en el CI con Gradle + servidor Forge real.

## Música del Palacio de hielo (antes no tenía)
- `herramientas/gen_musica_hielo.py`: usa los instrumentos de `gen_musica_torre.py`; música original, sin muestras.
  - **`music_ice_dungeon`** (60 s): mi menor, 64 bpm, celesta como gotas, pad de vidrio con trémolo, viento.
  - **`music_ice_boss`** (55 s): 140 bpm, bajo galopante, timbales, coro, arpegios de celesta y aullidos lejanos.
- Volumen igual al de las otras pistas (−16 a −17 dB de media).
- `AmbientMusic` las pone en las 3 copias del hielo; cambia a la de pelea cuando hay un Lobo Ártico vivo cerca.

## Sonidos propios del Archimago (antes usaba sonidos del evocador)
- `herramientas/gen_sonidos_archimago.py` (mono, para que tengan posición): voz grave con formantes, soplos con filtro que se mueve (STFT) y sala difusa por convolución.
- Eventos (con subtítulos ES/MX/EN):

| Evento | Variantes | Dónde suena |
|---|---|---|
| `archimago_ambient` | 2 | Murmullo de ambiente |
| `archimago_hurt` | 2 | Cuando recibe daño |
| `archimago_death` | 1 | Al morir |
| `archimago_charge` | 1 | Prepara un ataque o una invocación, carga final |
| `archimago_cast` | 1 | Lanza un hechizo (3 sitios) |
| `archimago_blink` | 1 | Teletransporte: al irse y al llegar |
| `archimago_orb` | 1 | Lanza una esfera |
| `archimago_burst` | 1 | Impactos y estallido final |

- Se quedaron los sonidos de vanilla que encajan: faros (sellos y fases), vidrio, amatista.

## Otros
- Palacio de hielo: después de reiniciar el servidor, la primera entrada reconstruye la sala (`dirtyArena` empieza en true). Antes, si la última pelea dejó hielo derretido, se quedaba así.

## Sin probar
Cómo suenan en el juego: solo se revisaron niveles y espectrogramas. Si algún sonido no gusta, se ajusta el generador y se vuelve a crear.
