# Etapa 284 — La cabeza de los jefes: a quién pegar, sin kiteo y sin huir de las mascotas

Pedido de Axelis: los jefes tenían que elegir mejor a quién atacar, no dejarse «kitear» y no salir corriendo detrás del jugador
mientras las mascotas les muerden.

## Cómo piensa un jefe (`world/JefeIA.java`)
Cada jefe lleva la cuenta de la **amenaza** de cada jugador y cada mascota:
- el daño que le hacen suma amenaza (y se va olvidando poco a poco);
- estar cerca también cuenta;
- las **mascotas** que le muerden son objetivo: si el jugador se aleja y su mascota sigue pegando, el jefe se gira hacia ella;
- no cambia de objetivo a cada rato: el actual vale un poco más y hay un tiempo mínimo entre cambios (2,5 s);
- si no puede llegar a alguien (subido muy arriba o fuera de su alcance), ese casi no cuenta.

**Contra el kiteo**: si su objetivo se queda lejos (a más de 9 bloques) unos 6 s, su amenaza baja y el jefe va a por otro que tenga a
mano. Si no hay otro, da un **salto de persecución** hacia él (como mucho cada 8 s).

**Manotazo**: las mascotas que le muerden de cerca se llevan un golpe cada 1,2 s, aunque el jefe esté atacando a otro.

## Jefes que lo usan
- Rey Brote, Gran Herrero, Lobo Ártico y Archimago (con salto de persecución los tres primeros).
- Coloso de Hierro, Guardián del Musgo, Capataz y Gran Salamandra (cambian su forma de elegir objetivo por esta).

## Pruebas
- **Servidor** (`mundos prueba etapa284`), con un jefe y muñecos de prueba:
  - al principio va a por el que tiene cerca;
  - no cambia de objetivo sin esperar el tiempo mínimo;
  - cambia al que más daño le hace;
  - deja al que no puede alcanzar;
  - se da cuenta de que le kitean y salta hacia quien lo hace.
