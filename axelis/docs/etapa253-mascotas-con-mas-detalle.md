# Etapa 253 — Mascotas con mucho más detalle

Pedido de Axelis: "quiero que las mascotas las hagas mejor… que les pongas mucho más detalle a todas y que salgan mejor, con
modelos, animaciones y modelos mejores". Además, en el catálogo seguían marcadas **Cachorro Rey** y **Cabrita Montañera**.

## 1. Mejoras del motor (se notan en las 60 mascotas a la vez)

Archivo principal: `vfx/Creature3D.java`.

- **Ojos nuevos**: la textura `textures/vfx/creature3d.png` pasó de 128 a **256 px** (mismas celdas, el código no cambia de UV).
  - Iris con degradado (más claro abajo), fibras suaves, anillo y sombra del párpado.
  - Perla negra más brillante, ojo cerrado más fino, boca y rubor más suaves.
  - Los **brillos blancos** del ojo grande ahora los dibuja el motor encima (no se tiñen del color del iris) y siempre apuntan hacia la luz.
  - Generador: `herramientas/gen_atlas_creature3d.py`.
  - En el juego esa textura se filtra suave (`setFilter(true, false)`), así los ojos no se ven con dientes de sierra de cerca.
- **Sombreado con más volumen** (solo mascotas; la utilería y los jefes siguen igual):
  - Luz principal: mitad fija en el mundo, mitad desde arriba a la izquierda de la cámara.
  - Cielo arriba y suelo abajo, banda cálida entre luz y sombra, brillo suave y sombra de contacto cerca de los pies.
- **Luz del mundo**: de noche o en una cueva la mascota se ve más oscura, como los demás bichos. Nunca baja de 42%, y lo que brilla sigue brillando.
- **Sombrita en el suelo** bajo las que andan. Las herramientas la apagan para los iconos.
- **Más caras de cerca** (siluetas más redondas), con un tope para que ninguna se pase.
  - `MAX_QUADS` sube de 36.000 a 60.000 por si hay muchas juntas.
- `Pets21.sbox` + `Pets24.boxSurf` se siguen usando para cajas redondeadas.

## 2. Animaciones nuevas para todas

- **Te miran**: quietas y despiertas, de vez en cuando giran la cabeza hacia la cámara y levantan la vista.
  - Cuánto giran depende de cada especie.
  - Las de cara "pegada" al cuerpo no giran: Tostadora, Nubecita, Corazoncito de Magma.
- **Se duermen**: si te quedas quieto 45 s, cierran los ojos, respiran lento y hondo, bajan la cabeza y les salen **zetas**.
  - Al moverte otra vez se despiertan con un respingo.
- **Pasitos al girar**: antes giraban en el sitio deslizándose.
- Campos nuevos en `VfxData`: `mIdle`, `mTurn`, `pmTurn`, `mWake` y `light`. El cliente (`VfxClient`) calcula la luz.

## 3. Diseños nuevos

- **Cachorro Rey** (`Pets25.lionCub`): leoncito regordete con manos grandes (deditos y almohadillas rosas) y manchitas de bebé.
  - Melena esponjosa de tres capas de mechones redondeados más la nuca.
  - Orejas con dorso oscuro, naricita con brillo, puntitos y bigotes.
  - Manto real rojo con ribete de armiño que ondea, cordón dorado con broche de rubí y corona ladeada con rubí y zafiros.
  - Cola con borla que se mece con retraso y un mini rugido de vez en cuando.
- **Cabrita Montañera** (`Pets25.mountainGoat`): cabrita alpina color caramelo con raya oscura en el lomo y calcetines de chocolate.
  - Hocico alargado que rumia, perilla, orejas horizontales y cuernitos de pizarra con anillos.
  - Collar de cuero con costuras y cencerro con badajo, edelweiss tras la oreja y mantita tejida a rayas con flecos.
  - Trota a saltitos y hace topetazos de juego.

## 4. Rediseños para que sean 100% propias

Se parecían demasiado a personajes conocidos, así que ahora son diseños originales (`Pets26` y `Creatures5`). Las habilidades no cambian.

- **Hámster Glotón** (antes "Motita de Hollín"): hámster dorado con mofletes llenos que mordisquea una pipa de girasol.
- **Ratón Relámpago**: ratoncito de tormenta color plata con orejas redondas que brillan, gafas de cobre y cola con bombilla.
- **Droide Minero** (antes "Droide Astromecánico"): robot naranja con orugas, taladro, pinza, visor con ojos de luz, casco con lámpara y batería.
- **Espíritu del Bosque**: cervatillo de musgo con flores, cuernitos de rama con hojas y setita, cola de hoja y luciérnagas.
- **Dragón de la Noche**: azul medianoche, alas de constelación, cuernos y aleta de luna creciente, ojos dorados.
- Nombres y frases cambiados en `es_es`, `es_mx` y `en_us`.

## 5. Detalles nuevos por mascota (además de lo del motor)

- **Tiburoncito**: hoyito de tierra con pasto y piedritas, rayitas, puntas blancas, muesca en la aleta y cejas.
- **Nubecita**: solecito que asoma y nubecitas que le dan vueltas.
- **Tostadora**: borde cromado, perillas, cable con enchufe, calorcito y vapor, mantequilla y carita de mermelada.
- **Murcielaguito**: medallón con rubí y garritas en las alas.
- **Hongo**: faldita en el tallo, pequitas y una mariquita que pasea por el sombrero.
- **Caracol Casita**: jardincito con flores y rastro de baba al andar.
- **Patito Pirata**: cinturón con hebilla, mapa del tesoro y plumón despeinado.
- **Cangrejo Tetera**: bolsita de té con etiqueta.
- **Gato de la Fortuna**: lazo con cascabel.
- **Conejo de la Luna**: pechera de pelusa.
- **Calamar Pintor**: paleta con pegotes y salpicaduras en la boina.
- **Farolillo**: ramita de cerezo pintada y una polilla curiosa.
- **Tortuga Isla**: manchitas y una gaviota que planea.
- **Pingüino**: brújula al cuello y vaho.
- **Mamut**: uñitas.
- **Narval**: moteado.
- **Ballena**: surcos en la garganta y pajaritos.
- **Zorrillo Mago**: libro de hechizos flotante que pasa las páginas.
- **Foca**: manchitas.
- **Tricerito**: placas en el lomo y manchas.
- **Yetito**: nieve en la cabeza, vaho y copitos.
- **Brotecito Rey**: burbujas en la gelatina y rocío.
- **Corazoncito de Magma**: gotas de lava.
- **Pulpo Astronauta**: planetita con anillo.
- **Dragoncito de Cristal**: escamas de barriga y destellos.
- **Sapo Brote**: manchitas y saco de la garganta que se infla.
- **Perrito Ninja**: shuriken en la bufanda.
- **Quetzal**: plumas en escamas en el pecho.
- **Cerdito Alcancía**: brillo de cerámica y corazón dorado.

## 6. Herramientas

- `PetSheet`: el ancho de las vistas se elige con `-Dw=...`, y `pose(..., idle)` permite dibujar una mascota dormida.
- `PetClip`: ahora graba 110 cuadros: quieta, andando y dormida.
- `PetBake`:
  - apaga la sombra para iconos y figuritas;
  - detecta la geometría sólida por la UV (sirve para cualquier sitio del blanco);
  - lee la tira entera de las zetas.
- `PetCatalog` / `allsheet.sh` / `grid.sh` (scratchpad): retratos para el catálogo y hojas de revisión.

## 7. Comprobado

- Compilación local sin errores y CI "Compilar mods".
- Prueba con el cliente: la tanda de mascotas usa León, Cabrita, Hámster y Droide, y quietas más de 45 s se duermen.
- Iconos, figuritas y retratos rehorneados de las 58 figuritas.
- Catálogo con botón **Comparar con antes**; las marcas nuevas se guardan en `seleccion/axelis253`.
- Página de animaciones con 10 videos nuevos; las marcas nuevas se guardan en `revision/animaciones253`.

## 8. Segunda vuelta (mismo día): rendimiento y reacciones

Axelis: «luego las testeo, sigue mejorando el mod y puliendo cosas».

### Rendimiento: dibujar mascotas cuesta 2,5 veces menos
Medido sin el juego (de cerca, el caso más caro):
- **Antes:** entre 0,5 y 8,7 ms por mascota en cada cuadro (de media 1,6 ms).
- **Ahora:** de media unos 0,6 ms, y como máximo 2,7 ms (Remendín).

Cambios:
- `Ctx.grid`: en esferas, tubos, cajas redondeadas, casquetes, telas y parches cada vértice se transforma y se sombrea **una sola vez**. Antes se hacía 4 veces, una por cada cuadro que lo comparte. Se ve igual.
- Cuentas del sombreado más baratas: potencias por multiplicación y la banda cálida con un polinomio.
- Remendín: cuerpo y cabeza con menos caras (el dibujo de tela ya va en parches aparte).
- Cachorro Rey: mechones de 5 lados.

### Reacciones al jugador (solo en el cliente, mascotas a menos de 10 bloques)
- **Saltas** → dan un saltito contigo (cada una con su pequeño retraso).
- **Te agachas** → se sientan (más bajitas) y te miran.
- **Te pegan** → respingo, y si estaban dormidas se despiertan.
- Campos nuevos en `VfxData`: `mHop`, `mSit`, `pmSit`. El cliente (`VfxClient`) los calcula.

### Detalles para las que solo tenían las mejoras generales
- Cerbero: collares de cuero con púas y placa dorada en los tres cuellos.
- Cofre Mímico: cuatro gemas que brillan en la tapa.
- Fantasmita: lacito rosa.
- Abejita: florecita en la cabeza.
- Pilluelo Lunar: colgante de luna creciente.
- Pegaso: polvo dorado detrás.

### Catálogo
- Debajo de cada mascota va su frase (las equipadas dan armadura, no habilidades).
- El encabezado cuenta las reacciones nuevas.

### Comprobado
- CI «Compilar mods» en verde.
- Prueba con el cliente: 66 fotos, 63 comprobaciones, 0 fallos.

## 9. Tercera vuelta: pulido sin crear nada nuevo

Axelis: «continúa, con pulir más, y aún no crees nada».

- **Dragoncito del huevo** (`Dragon3D`) ahora usa el motor de las mascotas, con la misma forma de antes. Con eso tiene:
  - sombreado con volumen y luz del mundo;
  - ojos dorados nuevos con brillos;
  - dibujo por rejilla;
  - te mira cuando va despacio.
  - Ya no usa su propia textura `dragon3d.png`, que queda sin usar.
- **Criaturas de malla del volcán** (salamandra, diablillo, cultista, larva) y el **Corazón de Magma**: tienen el mismo sombreado con volumen (`BossMesh.ctx`).
- **Yetito de las Nieves**:
  - Antes tenía la cara en un disco plano que sobresalía de perfil y, según el orden de dibujo, tapaba los ojos.
  - Ahora es una máscara curva, con ojos, nariz con brillo, boca y rubor encima.
- **Barrigas curvas** en el Yetito y el Lobito Ártico (antes eran discos planos).
- **Pelaje más suave** en todas: los mechones van más tumbados, más anchos y con la punta menos brillante. Parecen pelusa, no púas.
- **Patito Pirata**: el cinturón ya se ve por fuera de la pelusa.
- **Cangrejo Tetera**: la bolsita de té cuelga por delante de la tetera (antes quedaba dentro).
- **Cofre Mímico**: las gemas ahora se ven sobre la tapa.
- **CI**: gson se baja siempre de Maven Central (`exclusiveContent` en `settings.gradle`). La caché del CI recordaba el portal de plugins, que daba 404.
- **Comprobado**:
  - Compilación del CI en verde.
  - Prueba con el cliente: 66 fotos, 63 comprobaciones, 0 fallos. Las mallas del volcán se ven bien en el juego.
