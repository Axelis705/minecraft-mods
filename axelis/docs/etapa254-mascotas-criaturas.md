# Etapa 254 — Las mascotas son criaturas de verdad

Pedido de Axelis: «no me gustan las mascotas, les falta algo comparadas con los lobos o los gatos de Minecraft… ¿podemos hacer que sean una entidad?».

Lo que eligió:
- **Aspecto**: el mismo diseño 3D de siempre.
- **Combate**: según la mascota.
- **Cómo se usan**: desde el menú de equipo.
- **Extras**: sentarse con clic derecho, acariciarlas y darles comida, y un menú propio con agachado + clic derecho donde cambiarles el nombre y hacer cosas distintas según la mascota.

## 1. Cómo funciona ahora

Antes las mascotas eran dibujos que el servidor movía a mano. Ahora cada mascota equipada es una criatura del juego (`entity/Mascota`, que hereda de `TamableAnimal`, como el lobo y el gato).

**Moverse**
- Anda con su propio camino, salta bloques y nada.
- Las voladoras vuelan, flotan cerca de ti y, si les dices que se sienten, se posan en el suelo.
- Si te alejas más de 12 bloques se teletransporta a tu lado.

**Verse como las demás criaturas**
- Choca con las cosas y se puede empujar.
- Recibe la luz del sitio donde está: de noche o en una cueva se oscurece, pero lo que brilla sigue brillando.
- Tiene la sombra redonda del juego y parpadea en rojo cuando le pegan.
- Su nombre se ve encima cuando la miras.
- Se puede atar con una cuerda y renombrar con una etiqueta de nombre.

**Por dentro**
- Las criaturas nunca se guardan con el mundo. Las crea y las quita `relic/PetKeeper` según las figuritas equipadas, así nunca hay copias ni mascotas perdidas.
- Lo que recuerda cada mascota se guarda en su figurita (`relic/PetData`), así viaja con ella: nombre, cariño, modo, luz y recoger, esperas, regalo y mochila.

## 2. Lo que hace con el jugador

- **Equiparla / quitarla** (menú de equipo, G): aparece con un destello a tu lado; al quitarla se despide y se va.
- **Clic derecho**: se sienta o te vuelve a seguir.
  - Sentada se queda en su sitio aunque te vayas.
  - Si te alejas más de 44 bloques o cambias de mundo, «se queda en casa»: la criatura se quita sin ruido y vuelve a aparecer sentada en el mismo sitio cuando regresas.
  - El sitio se guarda con el jugador, así que también sirve al salir y entrar del juego.
- **Comida**: cada una tiene su comida favorita (en `relic/PetKinds`; por ejemplo el Gato de la Fortuna, bacalao y la Tostadora, pan).
  - Con su favorita se cura 4 corazones y sube 8 de cariño.
  - Con otra comida se cura menos y sube 3.
  - Al comer le salen corazones y migas, y suena contenta.
- **Cariño** (0 a 100, 5 corazones).
  - Sube al darle de comer, al acariciarla (como mucho cada 20 s) y al usar su acción.
  - Con 50 se cura más rápido.
  - Con 100 es tu **mejor amigo**: aguanta un 20 % más y a veces le salen corazones cuando estás cerca.
- **Reacciones** que ya tenía el modelo, ahora con la criatura:
  - te mira (gira la cabeza hacia quien está mirando);
  - salta contigo;
  - se agacha si te agachas;
  - se duerme si está quieta 45 s o si tú duermes en una cama;
  - da un respingo si le pegan.
- **Sonidos**: usa sonidos del juego, cada mascota con su tono (gato, lobo, ajolote, zorro, etc.). El Gato de la Fortuna ronronea si está sentado.

## 3. El menú de la mascota (agachado + clic derecho)

Pantalla `client/PetScreen` (menú `relic/PetMenu`, paquete `network/PetNamePacket` para el nombre).

**Lo que muestra**
- Vista 3D de la mascota, que sigue al ratón con la mirada.
- Casilla para el **nombre**. Se guarda con «Guardar» o Intro; si la dejas vacía vuelve a llamarse como su especie.
- Su cariño, su vida, la armadura que te da, su comida favorita (con su icono) y cómo pelea.

**Botones**
- Siéntate / Sígueme.
- Acariciar.
- Truco: da una vuelta y un saltito.
- **Modo**, solo en las que pelean:
  - Defender: como el lobo, ataca a lo que te pega y a lo que tú golpeas.
  - Atacar: además busca a los monstruos cercanos, nunca a los creepers.
  - Tranquilo: no pelea con nadie.
- **Acción propia**: una por mascota (ver la tabla). Las que tienen espera muestran el tiempo que falta.
- **Mochila**: las que la tienen muestran sus casillas. Lo que guardas se queda en la figurita.

| Acción | Qué hace | Espera | Mascotas |
|---|---|---|---|
| Regalo del día | Te trae algo pequeño y con su historia (pipas y galleta, pan, monedita de oro, tesoro enterrado, plumas, polvo de estrellas…) | 20 min | Tiburoncito, Gato de la Fortuna, Conejo de la Luna, Pulpo Astronauta, Hámster, Cabrita, Tostadora, Babosa, Creepercito, Muñeco de Nieve, Golemcito, Calamar Pintor, Cangrejo Tetera, Quetzal, Pegaso, Dragón Celestial, Remendín, Foca, Yetito, Gran Herrerito, Dragoncito de Cristal |
| Luz | Una luz te sigue por donde va (cuevas, noches) | interruptor | Ratón Relámpago, Droide Minero, Corazoncito de Magma, Gatito Cometa, Fantasmita, Fosu, Espíritu Farolito |
| Hacer crecer | Hace crecer cultivos, brotes y flores alrededor | 1 min | Hongo Paseante, Sapo Brote, Espíritu del Bosque, Brotecito Rey, Nubecita, Abejita |
| Recoger cosas | Va por lo que hay tirado cerca y te lo trae (no toca lo que tira un jugador) | interruptor | Perrito Ninja, Cerbero, Cookie, Lobito Ártico, Grifo |
| Mochila | 9 u 18 casillas | — | Tortuga Isla, Caracol Casita, Patito Pirata, Cerdito Alcancía (9); Mamut, Cofre Mímico, Ballena de Cielo (18) |
| Cuidarte | Recuperas 3 corazones | 3 min | Ajolote, Capibara Zen |
| Hechizo sorpresa | Flor, galleta, «¡puf!», flotar un rato (el Archimaguito también suelta experiencia) | 2 min | Zorrillo Mago, Pilluelo Lunar, Archimaguito |
| Rugido | Aparta y frena a los monstruos cercanos | 45 s | Cachorro Rey |
| Camino a casa | Rastro de luz hacia tu cama (o el inicio del mundo) y cuántos bloques faltan | 10 s | Pingüino Explorador, Narval Estelar |

- Ninguna acción da efectos de poder al jugador: se mantiene la decisión de que las mascotas den armadura.
- «Cuidarte» es una curación pequeña con espera larga.

## 4. Combate (según la mascota)

**Las 25 que pelean**
- A mordiscos (saltan al enemigo): Tiburoncito, Perrito Ninja, Golemcito, Cerbero, Cachorro Rey, Cofre Mímico, Tricerito, Yetito, Gran Herrerito, Lobito Ártico, Murcielaguito y Grifo.
- A distancia (se quedan a media distancia y disparan cada ~2 s): Pulpo Astronauta, Tostadora, Zorro de Nueve Colas, Ratón Relámpago, Archimaguito, Corazoncito de Magma, Nubecita, Gatito Cometa, Dragón de la Noche, Fosu, Dragón Celestial, Brotecito Rey y Dragoncito de Cristal.

**El estilo de cada ataque**
- Fuego: quema.
- Hielo: frena.
- Rayos: llegan al instante.
- Estrellas: hacen brillar al enemigo.
- Tinta: ciega.
- Tostadas: van en parábola y queman alrededor.
- Cristal: esquirlas alrededor.
- Magia: empuja.
- Roba vida: te cura un poco a ti y a ella.
- Semillas: enraízan.
- Tierra: lanza hacia arriba.

**Fuerza y aguante**
- Daño: 2,5 + 0,9 × nivel de su caja.
- Vida: 16 + 4 × nivel en las que pelean y 18 + 2 × nivel en las demás.
- A los jefes les hacen el 40 % del daño, para no estropear esas peleas. Su daño cuenta para el reparto de premios del dueño.

**Las que no pelean**
- Te acompañan.
- Si les pegan, huyen asustadas y luego vuelven contigo.

**Reglas de daño**
- Nunca reciben daño de su dueño, de otras mascotas suyas, de caídas ni por quedarse atrapadas en un bloque (en ese caso se teletransportan).
- Los demás jugadores solo pueden hacerles daño si el servidor tiene el combate entre jugadores activado.
- Si una pelea las deja sin vida, **no mueren**: se cansan, vuelven a su figurita y regresan solas al minuto, con un aviso en la pantalla.
- Se curan solas poco a poco si no les están pegando.

## 5. Dibujo

`client/PetRenderer` usa el mismo `Creature3D` de siempre, con `VfxData.entity = true`:
- no dibuja su sombrita propia, porque usa la del juego;
- el giro del cuerpo y la cabeza vienen de la criatura;
- `eYawOff` hace la vuelta del truco.

Detalles:
- La textura se filtra suave (tipos de dibujo propios), así los ojos no tienen dientes de sierra.
- Las piezas que brillan (`Sink.quadE`, con el nuevo `emissive`) no se oscurecen de noche.
- La caja de choque de cada mascota se midió con su modelo (`PetBounds`). En las voladoras, el modelo se baja lo justo para que su parte de abajo quede en el suelo de la caja.

## 6. Comprobado

- Compilación local y del CI sin errores.
- Prueba sin el juego: las 58 mascotas se dibujan en modo criatura (cabeza girada, truco, andando), sin fallos.
- (Prueba con el cliente: ver abajo.)

## Pendiente / ideas siguientes

- Montar en las grandes (Pegaso, Grifo, dragones).
- Sonidos propios para las 14 que aún no los tienen.
