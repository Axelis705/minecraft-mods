#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float AddMode;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    // AddMode = 0: pinta encima normalmente (alfa premultiplicado).
    // AddMode = 1: suma luz. Con los gráficos "Fabulosos" el juego descarta los píxeles de alfa 0 al mezclar las capas,
    //   así que la luz aditiva deja un alfa mínimo (1/255) donde haya algo de luz; apenas oscurece lo de atrás.
    // AddMode = 2: pasada que solo escribe profundidad (se ignora el color): solo donde el dibujo es visible.
    if (AddMode > 1.5) {
        if (color.a < 0.12) {
            discard;
        }
        fragColor = vec4(0.0);
        return;
    }
    vec3 rgb = color.rgb * color.a;
    if (AddMode > 0.5) {
        float lum = max(rgb.r, max(rgb.g, rgb.b));
        fragColor = vec4(rgb, lum > 0.004 ? 0.0045 : 0.0);
    } else {
        fragColor = vec4(rgb, color.a);
    }
}
