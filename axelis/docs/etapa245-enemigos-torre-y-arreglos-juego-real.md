# Etapa 245 – Enemigos de la Torre + arreglos encontrados en el juego real (18-nov-2026)

Axelis empezó a probar el mod en CurseForge (instancia `test`, 1.20.1, Forge 47.4.28) y eligió seguir con: 1) enemigos de la Torre, 2) mascotas exclusivas de los jefes 3 y 4 (pendiente).

## Enemigos de la Torre (los que eligió Axelis en la etapa 237)
| Enemigo | Comportamiento | Vida / daño | Modelo y sonidos |
|---|---|---|---|
| **Esbirro del Archimago** (`esbirro`) | Cuerpo a cuerpo con espada de hierro. Si te alejas más de 7 bloques, cada 6–10 s avisa con humo y se teletransporta a tu lado. | 22 / 4 | Humano con textura propia (túnica añil, capucha con la cara en sombra, ojos morados). Sonidos del vindicador y el teletransporte del Archimago. |
| **Libro embrujado** (`libro_embrujado`) | Vuela en círculos a 5–7 bloques. Antes de disparar pasa las hojas rápido (1 s) y lanza una página-orbe morada (orbe virtual con estela, 3 de daño). | 10 | Libro de la mesa de encantamientos teñido de morado, abre y cierra las tapas. |
| **Armadura encantada** (`armadura_encantada`) | Quieta como adorno hasta que alguien está a menos de 5 bloques y la ve, o la golpea. Entonces suena metal, le brillan los ojos (chispas azules) y ataca, lenta y difícil de empujar. | 30 / 6 | Cuerpo invisible: solo se ve la armadura de hierro completa. |

- Botín modesto (`loot_tables/entities/*`): papel, pepitas de hierro, lapislázuli; raro: esmeralda, libro, lingote, casco de malla. Nada que supere lo del Rey Brote.
- Huevos de aparición en la pestaña del mod. Nombres en ES/MX/EN.
- **Dónde salen:**
  - **Estructura de la Torre** (`/mundos estructura torre`): se ponen una sola vez al colocarla, no reaparecen. `TorreInterior.mobs` busca celdas libres con suelo firme:
    - 3 esbirros en el Gran Salón y 2 en la cripta;
    - 2 libros en la biblioteca y 2 en la sala de encantamiento;
    - 1 armadura en la armería secreta.
  - `deshacer` también los quita.
  - **Dimensión del Archimago:** al pisar el puente salen 2 libros (+1 por jugador extra, máximo 4) que cortan el paso. Cuando empieza la intro del jefe, o se reinicia la torre, los libros que queden se van.
- CI (servidor real): los 3 aparecen y viven, la torre se coloca con 10 enemigos y `deshacer` los quita.

## Arreglos que salieron del registro del juego real de Axelis
- **Capas**: `CapeLayer` buscaba el campo `layers` por su nombre de desarrollo. En el juego normal los nombres están ofuscados, así que fallaba en cada carga y la capa del juego no se ocultaba con una capa del mod puesta. Ahora busca el único campo `List` de `LivingEntityRenderer`.
- **Pila de libros** (`est_pila_libros`): su modelo tenía una rotación de 10° (Minecraft solo admite ±45/22,5/0). El modelo no cargaba y el bloque salía morado y negro. Corregido a 0°.
- **Botas de Magma**: no tenían modelo ni icono (salían moradas y negras). Icono animado nuevo (`gen_botas_magma.py`).

## Pendiente
- Mascotas exclusivas del Lobo y del Archimago: hay que hacer el modelo 3D por código (`vfx/PetsNN`), la figurita 3D y el icono animado de 12 cuadros. La herramienta que generaba esas figuritas no está en la nube; hay que rehacerla.
- Las texturas del Archimago están en el jar (regeneradas con `gen_archimago.py`, mismo resultado), pero en la PC están a 8 carpetas de profundidad.
