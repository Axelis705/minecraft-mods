# Etapa 288 — La Dríade del Roble, mini-jefa del Santuario del Bosque

Pedido de Axelis: «el Santuario necesita un mini-jefe» y «la decoración no tiene sentido».

## La Dríade del Roble (`entity/DriadeRoble.java`)
El espíritu que cuidaba el santuario, al que la corrupción del Rey Brote volvió contra todos. Encerró a Ruda y guarda la llave.
- **Dormida**: arrodillada en el centro del santuario, entre raíces y pétalos, con la cabeza gacha. No se la puede herir (un golpe de
  un jugador la despierta, pero no le hace daño).
- **Despierta** cuando alguien (no espectador ni creativo) se acerca a 9 bloques: polvo de raíces, pétalos, un rugido y su barra
  verde (solo se ve dentro del santuario).
- **Ataques** (vida y daño de tier 1, con más jugadores pegan más):
  - **latigazo** cuerpo a cuerpo que frena;
  - **enredadera**: estira el brazo (0,6 s de aviso) y un látigo de hojas atrae a su objetivo hacia ella;
  - **flores venenosas**: levanta los brazos y salen anillos rosas bajo quienes pelean (y alguno más alrededor); a 1,2 s revientan
    en esporas que hieren y envenenan;
  - **raíces del roble**: de cerca, levanta los brazos (anillo verde) y al bajarlos salen raíces que hieren y atrapan.
- **A media vida florece**: la barra se vuelve rosa, llama a dos Brotes y se vuelve más rápida.
- Elige a quién pegar con la «cabeza» de los jefes (amenaza, mascotas, contra el kiteo).
- **Al morir**: se deshace en pétalos y suelta la llave de la celda de Ruda.
- Aspecto: cuerpo humano con piel de corteza, vestido de hojas con cinturón de raíz, corona de flores, pelo de enredaderas y ojos de
  savia que brillan en la oscuridad (`gen/gen_driade288.py`). Flota un poco, como un espíritu.
- Huevo generador en la pestaña del mod.

## El santuario por dentro
- Fuera los bancos sueltos (estaban torcidos y sin sentido).
- En el centro, un nudo de raíces donde duerme la Dríade, dentro de un anillo de musgo con pétalos rosas.
- Raíces que cuelgan de las vigas, raíces de mangle saliendo de los rincones, azaleas en flor y velas.
- El Altar Maldito pasa a un lado (-5, 0, 22), fuera del anillo.
- El Raizal ya no lleva la llave: pasea por el patio.

## Ruda y los textos
- Ruda habla de la Dríade («cuando despierte, aléjate de sus flores rosas»). Si se pierde la llave, «la Dríade vuelve a brotar
  entre las raíces» con ella.
- La crónica del bosque («La herbolaria y la Dríade») y la descripción de la llave también cambian.

## Pruebas
- **Servidor** (`mundos prueba etapa288`):
  - en el plano está la Dríade con la llave, el Raizal en el patio y ya no hay bancos;
  - dormida no recibe daño ni despierta sola, y el golpe de un jugador sí la despierta;
  - sus raíces y sus flores hieren a un cerdo y lo envenenan;
  - al florecer llama a dos Brotes;
  - al morir suelta la llave.
- **CI**: en el santuario construido, la Dríade con la llave y dormida; al vencerla cae la llave y Ruda sale. También en el santuario
  natural.
- **Cliente** (`solo=santuario`): fotos de la Dríade dormida (48b) y despierta (48c); la llave cae al vencerla y se rescata a Ruda.
