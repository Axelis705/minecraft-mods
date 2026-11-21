# Etapa 262 — El bosque por tiers: Gran Templo con el bloque y Santuario con Ruda

Pedido (21 nov 2026): dos estructuras por tier. En el **bosque**, la grande tiene el bloque importante (la Piedra de Portal) y la pequeña tiene al habitante (Ruda). Cada estructura tiene un inicio (la puerta) y un final (el bloque, la celda o el botín).

## Gran Templo del Bosque (la grande: el bloque)

- `/mundos estructura templo`.
- Es el templo grande que ya existía, ahora con su final:
  - la **Piedra de Portal: Naturaleza** sobre el altar, junto al árbol del mundo (arriba del todo);
  - un cofre de provisiones de la etapa 1 al lado.
- Enemigos por el camino:
  - brotes en la entrada;
  - espinosos en los balcones;
  - brotes y un espinoso junto al altar;
  - un **Raizal** que guarda el altar.
- Suelo y alrededores nuevos: se funde con el terreno (hierba, musgo y podsol) y tiene un camino hasta la puerta.
- **Aparece sola en el mundo**:
  - en bosques, llanuras y praderas;
  - en regiones de 896 bloques, más o menos en la mitad.
  - `/mundos natural generar bosque [aqui]`.
- Las brújulas y las pistas de los habitantes («bosque») llevan aquí.

## Santuario del Bosque (la pequeña: Ruda)

- `/mundos estructura santuario`.
- Se quitó la Piedra de Portal y su altar.
- El final es la **celda de Ruda**, al fondo:
  - muro con barrotes y una puerta de hierro;
  - dentro, paja, un caldero, una vela, un helecho en maceta y telarañas.
- El Raizal con la llave está justo delante de la celda.
- El cofre de provisiones sigue al lado.
- **Aparece sola**, también en bosques: `/mundos natural generar santuario [aqui]`.

## Otros cambios

- Las dos estructuras del bosque no salen pegadas: si ya hay otra estructura cerca, se busca otro sitio.
- En mundos ya empezados, el santuario que ya estaba cerca del inicio se conserva (se pasa al nombre nuevo).
- Las frases de Ruda, Bruno, Hilda y Teo ahora hablan del «gran templo del bosque».

## Comprobado

- Compila.
- **Plano revisado fuera del juego**:
  - el templo tiene 2 salas sin acceso toleradas y ninguna trampa;
  - el santuario tiene 1 (la celda, cerrada a propósito).
- **Servidor de verdad (CI)**, todo en verde:
  - el templo: la Piedra de Portal, el cofre, el Raizal, los brotes y los espinosos, y que no tenga habitante;
  - el santuario: sin Piedra de Portal, con la puerta y los barrotes de la celda;
  - Ruda presa, rescatada y vecina;
  - templo y santuario «naturales» construidos por pasos;
  - deshacer las dos.
- **Juego real (cliente)**: fotos del templo por fuera, la entrada y el altar, y el rescate de Ruda en la celda del fondo.

## Pendiente

- Los demás tiers: volcán (etapa 263), hielo, forja y magia.
