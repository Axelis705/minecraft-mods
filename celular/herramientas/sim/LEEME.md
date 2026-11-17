# Simulación de cómo se sostiene el celular

Minecraft pone el ítem en la mano con una cadena de transformaciones (brazo → mano → `display` del modelo). Como la nube no puede
abrir el juego, estas herramientas la repiten en Python para elegir los números del modelo y de las poses:

- `hand.py`: la cadena de tercera persona (`chain`) y hacia dónde quedan la parte de arriba y la pantalla del celular.
- `java_check.py`: `aim_java`, la misma matemática de `PhonePoses.aim` (brazo apuntando a un lado con la palma hacia otro).
- `tilt.py`: busca la inclinación del celular en la mano para que la pantalla mire a los ojos al escribir y al tomar foto.
- `ear_model.py`: calcula la forma de sostenerlo en una llamada (`models/item/phone3d/base_ear.json`).
- `render_pose.py salida.png`: dibuja un jugador de cajas con cada pose y el celular 3D (de frente y de espaldas).

Uso: `cd celular/herramientas/sim && python3 render_pose.py /tmp/poses.png`
