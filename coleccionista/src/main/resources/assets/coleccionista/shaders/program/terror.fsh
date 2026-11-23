#version 150

// Filtro de terror de Los Juegos del Coleccionista:
// - lo oscuro más oscuro y lo iluminado más brillante (curva por luz),
// - un resplandor suave alrededor de las luces fuertes (focos, velas, pantallas),
// - un poco menos de color y tono frío en las sombras,
// - viñeta (orillas oscuras) y grano de película.

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float azar(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float luz(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 brillante(vec2 uv) {
    vec3 c = texture(DiffuseSampler, uv).rgb;
    return c * smoothstep(0.62, 0.95, luz(c));
}

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = luz(c);

    // curva: sombras hacia abajo, luces hacia arriba
    float k = smoothstep(0.05, 0.85, l);
    c *= mix(0.55, 1.22, k);

    // resplandor de las luces fuertes (8 muestras en dos anillos)
    vec3 g = vec3(0.0);
    vec2 t = oneTexel * 3.0;
    g += brillante(texCoord + vec2(t.x, 0.0)) + brillante(texCoord - vec2(t.x, 0.0));
    g += brillante(texCoord + vec2(0.0, t.y)) + brillante(texCoord - vec2(0.0, t.y));
    g += brillante(texCoord + t * 2.0) + brillante(texCoord - t * 2.0);
    g += brillante(texCoord + vec2(t.x, -t.y) * 2.0) + brillante(texCoord + vec2(-t.x, t.y) * 2.0);
    c += g * 0.06;

    // menos color y frío en las sombras
    float l2 = luz(c);
    c = mix(c, vec3(l2), 0.28 * (1.0 - k * 0.6));
    c *= mix(vec3(0.90, 0.95, 1.07), vec3(1.03, 1.0, 0.96), k);

    // viñeta
    vec2 d = texCoord - 0.5;
    d.x *= OutSize.x / max(1.0, OutSize.y);
    c *= mix(0.42, 1.0, smoothstep(0.95, 0.32, length(d)));

    // grano
    float n = azar(floor(texCoord * OutSize / 2.0) + floor(Time * 400.0));
    c += (n - 0.5) * 0.045;

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
