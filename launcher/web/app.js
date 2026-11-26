// El launcher del Coleccionista: el escenario en 3D (él en su sillón, el reflector, el polvo), sus sustos y la parte de entrar y jugar.
// Habla con Python por window.pywebview.api (ver coleccionista.py). Abierto en un navegador con ?demo, usa una API de mentira.
'use strict';

const $ = (id) => document.getElementById(id);
const r = (d) => d * Math.PI / 180;
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
const azar = (a, b) => a + Math.random() * (b - a);

// ------------------------------------------------------------------------------------------------ sonidos

const SONIDOS = {};
function sonido(nombre, vol = 1, bucle = false) {
  if (!SONIDOS[nombre]) {
    const a = new Audio('sonidos/' + nombre + '.ogg');
    a.loop = bucle;
    SONIDOS[nombre] = a;
  }
  const a = SONIDOS[nombre];
  a.volume = vol;
  if (!bucle) a.currentTime = 0;
  const p = a.play();
  if (p && p.catch) p.catch(() => pendientes.add(nombre));
  return a;
}
const pendientes = new Set();
// si el sistema no dejó sonar nada antes de un clic, suena al primer clic o tecla
['pointerdown', 'keydown'].forEach((ev) => window.addEventListener(ev, () => {
  for (const n of pendientes) {
    const a = SONIDOS[n];
    if (a && (a.loop || n.startsWith('voz_'))) a.play().catch(() => {});
  }
  pendientes.clear();
}, { once: false }));

let musicaOn = true;
function musica(si) {
  musicaOn = si;
  $('b-musica').classList.toggle('apagada', !si);
  if (si) {
    sonido('musica', 0.3, true);
    sonido('ambiente', 0.5, true);
  } else {
    for (const n of ['musica', 'ambiente', 'latido']) if (SONIDOS[n]) SONIDOS[n].pause();
  }
}

// lo que dice: la voz, el subtítulo y la animación de hablar mientras suena
const LINEAS = {
  que_tenemos: 'Vaya, vaya… ¿Qué tenemos aquí?',
  presento: 'Permítanme presentarme. Soy… el Coleccionista.',
  coleccion: 'Y ustedes son… mi nueva colección.',
  hola: 'Hola…',
};
let hablando = null;
function decir(clave) {
  const a = sonido('voz_' + clave, 1);
  hablando = { clave, hasta: performance.now() + 1500 };
  $('subtitulo').querySelector('span').textContent = LINEAS[clave];
  $('subtitulo').style.opacity = 1;
  const fin = () => {
    if (hablando && hablando.clave === clave) {
      hablando = null;
      $('subtitulo').style.opacity = 0;
    }
  };
  a.onended = fin;
  setTimeout(fin, 7000);
}
function habla() {
  if (!hablando) return false;
  const a = SONIDOS['voz_' + hablando.clave];
  return performance.now() < hablando.hasta || (a && !a.paused && !a.ended);
}

// ------------------------------------------------------------------------------------------------ el modelo (formato de Minecraft Bedrock)

async function cargarModelo() {
  const geo = await (await fetch('coleccionista.geo.json')).json();
  const g = geo['minecraft:geometry'][0];
  const TW = g.description.texture_width, TH = g.description.texture_height;
  const glow = new Set(g.axelis_glow || []);
  const img = new Image();
  img.src = 'coleccionista.png';
  await img.decode();
  const tex = new THREE.Texture(img);
  tex.magFilter = THREE.NearestFilter;
  tex.minFilter = THREE.NearestFilter;
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.needsUpdate = true;
  const matN = new THREE.MeshLambertMaterial({ map: tex, alphaTest: 0.5, side: THREE.DoubleSide });
  const matG = new THREE.MeshBasicMaterial({ map: tex, alphaTest: 0.5, side: THREE.DoubleSide, color: 0xffffff });
  function caras(u, v, w, h, d) {
    return {
      up: [u + d, v, w, d], down: [u + d + w, v, w, d], east: [u, v + d, d, h], north: [u + d, v + d, w, h],
      west: [u + d + w, v + d, d, h], south: [u + d + w + d, v + d, w, h],
    };
  }
  function caja(c, mat) {
    const inf = c.inflate || 0;
    const s = c.size.map((x) => x + 2 * inf);
    const bg = new THREE.BoxGeometry(Math.max(s[0], 0.01), Math.max(s[1], 0.01), Math.max(s[2], 0.01));
    const w = Math.max(1, Math.ceil(c.size[0])), h = Math.max(1, Math.ceil(c.size[1])), d = Math.max(1, Math.ceil(c.size[2]));
    const cs = caras(c.uv[0], c.uv[1], w, h, d);
    const orden = ['east', 'west', 'up', 'down', 'south', 'north'];
    const uv = bg.attributes.uv;
    for (let f = 0; f < 6; f++) {
      const [x, y, cw, ch] = cs[orden[f]];
      const u0 = x / TW, u1 = (x + cw) / TW, v0 = 1 - y / TH, v1 = 1 - (y + ch) / TH;
      const q = [[u0, v0], [u1, v0], [u0, v1], [u1, v1]];
      for (let k = 0; k < 4; k++) uv.setXY(f * 4 + k, q[k][0], q[k][1]);
    }
    uv.needsUpdate = true;
    return new THREE.Mesh(bg, mat);
  }
  const raiz = new THREE.Group();
  const huesos = {}, piv = {}, base = {};
  for (const b of g.bones) piv[b.name] = b.pivot || [0, 0, 0];
  for (const b of g.bones) {
    const gr = new THREE.Group();
    const p = piv[b.name], pp = b.parent ? piv[b.parent] : [0, 0, 0];
    gr.position.set(p[0] - pp[0], p[1] - pp[1], p[2] - pp[2]);
    gr.rotation.order = 'ZYX';
    const rot = b.rotation || [0, 0, 0];
    gr.rotation.set(-r(rot[0]), r(rot[1]), -r(rot[2]));
    base[b.name] = { pos: gr.position.clone(), rot: [-r(rot[0]), r(rot[1]), -r(rot[2])] };
    huesos[b.name] = gr;
    const mat = glow.has(b.name) ? matG : matN;
    for (const c of b.cubes || []) {
      const m = caja(c, mat);
      const centro = [c.origin[0] + c.size[0] / 2 - p[0], c.origin[1] + c.size[1] / 2 - p[1], c.origin[2] + c.size[2] / 2 - p[2]];
      if (c.rotation) {
        const cp = c.pivot;
        const cg = new THREE.Group();
        cg.position.set(cp[0] - p[0], cp[1] - p[1], cp[2] - p[2]);
        cg.rotation.order = 'ZYX';
        cg.rotation.set(-r(c.rotation[0]), r(c.rotation[1]), -r(c.rotation[2]));
        m.position.set(centro[0] - (cp[0] - p[0]), centro[1] - (cp[1] - p[1]), centro[2] - (cp[2] - p[2]));
        cg.add(m);
        gr.add(cg);
      } else {
        m.position.set(...centro);
        gr.add(m);
      }
    }
  }
  for (const b of g.bones) (b.parent ? huesos[b.parent] : raiz).add(huesos[b.name]);
  return { raiz, huesos, base, matG };
}

// ------------------------------------------------------------------------------------------------ la escena

const lienzo = $('escena');
const render = new THREE.WebGLRenderer({ canvas: lienzo, alpha: true, antialias: false });
render.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
const escena = new THREE.Scene();
const camara = new THREE.PerspectiveCamera(24, 16 / 9, 1, 2000);
const MIRA = new THREE.Vector3(0, 32, 0);

const ambiente = new THREE.AmbientLight(0xffffff, 0.55);
escena.add(ambiente);
const foco = new THREE.SpotLight(0xffe2b0, 3.2, 0, 0.42, 0.65, 0);
foco.position.set(0, 190, -70);
foco.target.position.set(0, 25, 0);
escena.add(foco, foco.target);
const contra = new THREE.PointLight(0xc01020, 1.4, 0, 0);
contra.position.set(0, 70, 70);
escena.add(contra);
const frente = new THREE.DirectionalLight(0x9a8cff, 0.25);
frente.position.set(-60, 40, -120);
escena.add(frente);

// el haz del reflector (un cono que brilla un poquito) y el polvo que flota dentro
function texturaHaz() {
  const c = document.createElement('canvas');
  c.width = 4; c.height = 128;
  const x = c.getContext('2d');
  const gr = x.createLinearGradient(0, 0, 0, 128);
  gr.addColorStop(0, 'rgba(255,230,180,0.0)');
  gr.addColorStop(0.15, 'rgba(255,230,180,0.55)');
  gr.addColorStop(1, 'rgba(255,230,180,0.08)');
  x.fillStyle = gr;
  x.fillRect(0, 0, 4, 128);
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}
const haz = new THREE.Mesh(new THREE.ConeGeometry(58, 230, 40, 1, true),
  new THREE.MeshBasicMaterial({ map: texturaHaz(), transparent: true, opacity: 0.16, depthWrite: false, blending: THREE.AdditiveBlending,
    side: THREE.DoubleSide }));
haz.position.set(0, 95, -10);
escena.add(haz);

const POLVO = 220;
const polvoPos = new Float32Array(POLVO * 3), polvoVel = [];
for (let i = 0; i < POLVO; i++) {
  const a = Math.random() * Math.PI * 2, rr = Math.sqrt(Math.random()) * 42, y = Math.random() * 190 - 10;
  polvoPos.set([Math.cos(a) * rr, y, Math.sin(a) * rr - 10], i * 3);
  polvoVel.push([azar(2, 6), azar(0, 6.28), azar(0.4, 1.2)]);
}
const polvoGeo = new THREE.BufferGeometry();
polvoGeo.setAttribute('position', new THREE.BufferAttribute(polvoPos, 3));
const polvo = new THREE.Points(polvoGeo, new THREE.PointsMaterial({ color: 0xffe8c0, size: 1.1, transparent: true, opacity: 0.55,
  depthWrite: false, blending: THREE.AdditiveBlending }));
escena.add(polvo);

let modelo = null;

function acomodar() {
  const w = window.innerWidth, h = window.innerHeight;
  render.setSize(w, h, false);
  camara.aspect = w / h;
  // él a la derecha (en la luz del fondo): se corre el centro de la vista
  camara.setViewOffset(w, h, -w * 0.17, h * 0.02, w, h);
  camara.updateProjectionMatrix();
}
window.addEventListener('resize', acomodar);
acomodar();

// ------------------------------------------------------------------------------------------------ cómo se mueve (como en el juego)

const CX = 0, KX = 1, KY = 2, KZ = 3, BDX = 4, BDZ = 5, ADX = 6, ADZ = 7, BIX = 8, BIZ = 9, AIX = 10, AIZ = 11, N = 12;
const ACT = new Array(N).fill(0), OBJ = new Array(N).fill(0);
let empezado = false;
const raton = { x: 0, y: 0 };
window.addEventListener('mousemove', (e) => {
  raton.x = e.clientX / window.innerWidth * 2 - 1;
  raton.y = e.clientY / window.innerHeight * 2 - 1;
});
let risa = false, miraFijo = 0, ladoAntes = 0, cerca = 0;

function set3(i, x, y, z) { OBJ[i] = x; OBJ[i + 1] = y; OBJ[i + 2] = z; }
function brazos(a, b, c, d, e, f, g, h) {
  OBJ[BDX] = a; OBJ[BDZ] = b; OBJ[ADX] = c; OBJ[ADZ] = d; OBJ[BIX] = e; OBJ[BIZ] = f; OBJ[AIX] = g; OBJ[AIZ] = h;
}

function animar(t, dt) {
  const hb = habla();
  const quieto = performance.now() < miraFijo;
  OBJ[CX] = -0.04;
  if (risa) {
    OBJ[CX] = 0.06;
    set3(KX, -0.38, 0, 0.1);
    brazos(-0.45, 0.25, -0.3, -1.5, -0.45, -0.25, -0.3, 1.5);
  } else if (hb) {
    const g = [...hablando.clave].reduce((a, c) => a + c.charCodeAt(0), 0) % 3;
    if (g === 0) { set3(KX, 0.02, 0.22, -0.08); brazos(-0.7, 0.4, -0.25, -2.15, -0.55, -0.12, -0.35, 1.9); }
    else if (g === 1) { set3(KX, 0, -0.1, 0.05); brazos(-0.3, 0.45, -0.5, -0.75, -0.3, -0.45, -0.5, 0.75); }
    else { set3(KX, 0.02, -0.22, 0.08); brazos(-0.55, 0.12, -0.35, -1.9, -0.7, -0.4, -0.25, 2.15); }
  } else {
    // esperando: manos juntas y la cabeza ladeada; cada tanto la ladea al otro lado de golpe, como muñeco (y cruje)
    const lado = Math.floor(t / 6.5) % 2 === 0 ? 0.2 : -0.12;
    if (lado !== ladoAntes && t > 2) sonido('crujido', 0.35);
    ladoAntes = lado;
    set3(KX, 0.08, 0, quieto ? 0 : lado);
    brazos(-0.55, 0.12, -0.35, -1.9, -0.55, -0.12, -0.35, 1.9);
  }
  // sigue al ratón con la mirada (después de un apagón, te mira fijo a ti)
  if (quieto) {
    OBJ[KX] = 0.02; OBJ[KY] = 0;
  } else {
    OBJ[KY] += clamp(-(raton.x - 0.3) * 0.55, -0.6, 0.6);
    OBJ[KX] += clamp(raton.y * 0.28, -0.25, 0.3);
  }
  if (!empezado) { for (let i = 0; i < N; i++) ACT[i] = OBJ[i]; empezado = true; }
  const k = 1 - Math.exp(-dt * 5), kr = 1 - Math.exp(-dt * (quieto ? 25 : 11));
  for (let i = 0; i < N; i++) ACT[i] += (OBJ[i] - ACT[i]) * ((i === KZ || (quieto && i <= KZ)) ? kr : k);
  // encima, lo que no se suaviza: respirar, asentir al hablar, mover la mano, reír
  const resp = Math.sin(t * 1.7);
  let kx = ACT[KX], ky = ACT[KY], kz = ACT[KZ], cy = -resp * 0.35;
  if (risa) {
    cy += Math.sin(t * 40) * 0.6;
    kz += Math.sin(t * 23) * 0.04;
  } else if (hb) {
    kx += Math.sin(t * 7) * 0.05 + Math.sin(t * 11.3) * 0.02;
    ky += Math.sin(t * 1.9) * 0.05;
  } else if (!quieto) {
    kx += Math.sin(t * 0.53) * 0.04;
    ky += Math.sin(t * 0.37) * 0.14;
  }
  const mano = hb && !risa ? Math.sin(t * 3.1) * 0.12 : 0;
  const dedos = !hb ? Math.sin(t * 9) * 0.025 * Math.max(0, Math.sin(t * 0.8)) : 0;
  poner('cuerpo', ACT[CX] + resp * 0.012, 0, 0, cy);
  poner('cabeza', kx, ky, kz);
  poner('brazo_der', ACT[BDX], 0, ACT[BDZ]);
  poner('antebrazo_der', ACT[ADX], 0, ACT[ADZ] + mano + dedos);
  poner('brazo_izq', ACT[BIX], 0, ACT[BIZ]);
  poner('antebrazo_izq', ACT[AIX], 0, ACT[AIZ] - mano * 0.4 - dedos);
}

// una rotación como Pose.setRot del juego (x y z al revés, por la y hacia abajo); dy como Pose.addPos (positivo = hacia abajo)
const SIGNO = (() => {
  const q = new URLSearchParams(location.search);
  return [+(q.get('sx') || -1), +(q.get('sy') || 1), +(q.get('sz') || -1)];
})();
function poner(nombre, x, y, z, dy = 0) {
  const h = modelo.huesos[nombre], b = modelo.base[nombre];
  if (!h) return;
  h.rotation.set(b.rot[0] + SIGNO[0] * x, b.rot[1] + SIGNO[1] * y, b.rot[2] + SIGNO[2] * z);
  h.position.set(b.pos.x, b.pos.y - dy, b.pos.z);
}

// ------------------------------------------------------------------------------------------------ los sustos

let proximoGlitch = 9 + Math.random() * 8, proximoApagon = 26 + Math.random() * 14, proximoSusurro = 40 + Math.random() * 30;
let apagonHasta = 0, glitchHasta = 0;

function glitch(t) {
  document.body.classList.add('glitch');
  glitchHasta = t + azar(0.12, 0.32);
  sonido('estatica', 0.22);
}

function apagon(t) {
  document.body.classList.add('apagon');
  apagonHasta = t + azar(1.0, 1.7);
  sonido('golpe', 0.75);
}

// ------------------------------------------------------------------------------------------------ el ruido de la tele

const grano = $('grano').getContext('2d');
const granoImg = grano.createImageData(320, 180);
function ruido() {
  const d = granoImg.data;
  for (let i = 0; i < d.length; i += 4) {
    const v = Math.random() * 255;
    d[i] = d[i + 1] = d[i + 2] = v;
    d[i + 3] = 255;
  }
  grano.putImageData(granoImg, 0, 0);
}

// ------------------------------------------------------------------------------------------------ el título que tiembla

const titulo = $('titulo');
titulo.innerHTML = [...'COLECCIONISTA'].map((c) => '<span>' + c + '</span>').join('');
const letras = [...titulo.children];
let letra = null;
setInterval(() => {
  if (letra) { letra.classList.remove('tiembla'); letra = null; }
  if (Math.random() < 0.6) {
    letra = letras[Math.floor(Math.random() * letras.length)];
    letra.classList.add('tiembla');
    let n = 0;
    const id = setInterval(() => {
      letra && letra.style.setProperty('--tx', azar(-1.5, 1.5) + 'px');
      letra && letra.style.setProperty('--ty', azar(-1.5, 1.5) + 'px');
      if (++n > 6) clearInterval(id);
    }, 30);
  }
}, 1700);

// ------------------------------------------------------------------------------------------------ cada cuadro

let antes = performance.now(), cuadro = 0;
const inicio = performance.now();
function paso(ahora) {
  const dt = Math.min(0.1, (ahora - antes) / 1000);
  antes = ahora;
  const t = (ahora - inicio) / 1000;
  cuadro++;
  if (modelo) {
    animar(t, dt);
    // se acerca un poquito cuando vuelve la luz (como si se hubiera movido en lo oscuro)
    cerca += ((performance.now() < miraFijo ? 1 : 0) - cerca) * (1 - Math.exp(-dt * 3));
    const s = 1 + 0.06 * cerca;
    modelo.raiz.scale.set(s, s, s);
    // los ojos: brillan más en lo oscuro y al hablar
    const ojos = t < apagonHasta ? 1.6 : (habla() ? 0.8 + 0.4 * Math.abs(Math.sin(t * 7)) : 0.65 + 0.12 * Math.sin(t * 1.1));
    modelo.matG.color.setScalar(Math.min(ojos, 1));
  }
  // el reflector parpadea de vez en cuando (y se apaga en los apagones)
  const parpadeo = (Math.floor(ahora / 90) % 61 < 2 || Math.floor(ahora / 70) % 97 < 1) ? 0.45 : 1;
  let luz = (0.85 + 0.1 * Math.sin(t * 2.3) + 0.05 * Math.sin(t * 7.1)) * parpadeo;
  if (t < apagonHasta) luz = 0;
  foco.intensity = 3.2 * luz;
  ambiente.intensity = t < apagonHasta ? 0.03 : 0.5 * (0.7 + 0.3 * luz);
  contra.intensity = t < apagonHasta ? 0.18 : 1.4;
  frente.intensity = t < apagonHasta ? 0.0 : 0.25;
  haz.material.opacity = 0.16 * luz;
  polvo.material.opacity = 0.55 * luz;
  $('luz').style.opacity = (0.9 * luz).toFixed(3);
  // el polvo sube despacio y se mece
  const p = polvoGeo.attributes.position.array;
  for (let i = 0; i < POLVO; i++) {
    const v = polvoVel[i];
    p[i * 3 + 1] += v[0] * dt * 0.6;
    p[i * 3] += Math.sin(t * v[2] + v[1]) * dt * 1.2;
    if (p[i * 3 + 1] > 185) p[i * 3 + 1] = -10;
  }
  polvoGeo.attributes.position.needsUpdate = true;
  // la cámara se mece un poquito y sigue al ratón (el fondo se mueve menos: profundidad)
  const ox = Math.sin(t * 0.21) * 4 + raton.x * 7, oy = Math.sin(t * 0.17) * 2 - raton.y * 4;
  camara.position.set(ox, 41 + oy, -240);
  camara.lookAt(MIRA);
  $('fondo').style.transform = `translate(${(-raton.x * 6).toFixed(1)}px, ${(-raton.y * 4).toFixed(1)}px)`;
  $('luz').style.transform = $('fondo').style.transform;
  // los sustos
  if (t > proximoGlitch) { glitch(t); proximoGlitch = t + azar(12, 26); }
  if (t > glitchHasta && document.body.classList.contains('glitch')) document.body.classList.remove('glitch');
  if (document.body.classList.contains('glitch')) {
    document.body.style.setProperty('--gx', azar(-6, 6).toFixed(0) + 'px');
    if (modelo) modelo.raiz.position.x = azar(-1.5, 1.5);
  } else if (modelo) modelo.raiz.position.x = 0;
  if (t > proximoApagon && !hablando) { apagon(t); proximoApagon = t + azar(32, 55); }
  if (apagonHasta && t > apagonHasta) {
    apagonHasta = 0;
    document.body.classList.remove('apagon');
    // vuelve la luz... y te está mirando
    miraFijo = performance.now() + 2600;
    sonido('estatica', 0.35);
  }
  if (t > proximoSusurro) { sonido('susurro', 0.35); proximoSusurro = t + azar(45, 80); }
  if (cuadro % 3 === 0) ruido();
  render.render(escena, camara);
  requestAnimationFrame(paso);
}

// ------------------------------------------------------------------------------------------------ entrar y jugar (con Python)

const API = () => window.pywebview && window.pywebview.api;

function mostrar(id) {
  $('p-entrar').hidden = id !== 'p-entrar';
  $('p-jugar').hidden = id !== 'p-jugar';
}

function botonesConSonido() {
  document.querySelectorAll('button').forEach((b) => {
    b.addEventListener('mouseenter', () => { if (!b.disabled) sonido('encima', 0.3); });
    b.addEventListener('click', () => { if (!b.disabled) sonido('clic', 0.5); });
  });
}

async function entrar() {
  const u = $('usuario').value.trim(), k = $('clave').value;
  const aviso = $('aviso-entrar');
  if (!u || !k) { aviso.textContent = 'Escribe tu usuario y tu contraseña.'; return; }
  aviso.className = 'aviso bien';
  aviso.textContent = 'Revisando…';
  $('b-entrar').disabled = true;
  const res = await API().entrar(u, k);
  $('b-entrar').disabled = false;
  if (!res.ok) {
    aviso.className = 'aviso';
    aviso.textContent = res.error || 'Usuario o contraseña incorrectos.';
    sonido('golpe', 0.35);
    glitch((performance.now() - inicio) / 1000);
    return;
  }
  aviso.textContent = '';
  await pantallaJugar(res.usuario);
  setTimeout(() => decir('presento'), 500);
}

let ramElegida = 6;
async function pantallaJugar(usuario) {
  $('hola').textContent = 'Hola, ' + usuario;
  const info = await API().ram();
  const cont = $('rams');
  cont.innerHTML = '';
  for (const [g, si] of info.opciones) {
    const b = document.createElement('button');
    b.className = 'ram';
    b.textContent = g + ' GB';
    b.disabled = !si;
    b.onclick = () => elegirRam(g);
    b.dataset.g = g;
    cont.appendChild(b);
  }
  $('ram-info').textContent = 'Tu computadora tiene ' + Math.round(info.total) + ' GB (lo tachado no alcanza).';
  elegirRam(info.elegida);
  mostrar('p-jugar');
  botonesConSonido();
}

function elegirRam(g) {
  ramElegida = g;
  document.querySelectorAll('.ram').forEach((b) => b.classList.toggle('si', +b.dataset.g === g));
  API().elegir_ram(g);
}

function jugar() {
  const b = $('b-jugar');
  if (b.disabled) return;
  b.disabled = true;
  b.classList.add('latiendo');
  b.querySelector('span').textContent = 'PREPARANDO…';
  $('estado').className = 'estado';
  if (musicaOn) sonido('latido', 0.5, true);
  API().jugar(ramElegida);
}

// Python llama a estas tres mientras prepara todo
window.avance = (k, txt) => {
  $('barra-avance').style.width = (clamp(k, 0, 1) * 100).toFixed(1) + '%';
  $('estado').textContent = txt;
};
window.listo = (res) => {
  terminado();
  $('barra-avance').style.width = '100%';
  $('estado').className = 'estado bien';
  $('estado').textContent = res.abierto
    ? '¡Listo! Se abrió el launcher de Minecraft: entra con tu cuenta, elige el perfil “El Coleccionista” y presiona JUGAR.'
    : '¡Listo! Abre el launcher de Minecraft, elige el perfil “El Coleccionista” y presiona JUGAR.';
  decir('coleccion');
  setTimeout(() => sonido('risa', 0.5), 3800);
};
window.fallo = (msg) => {
  terminado();
  $('b-jugar').querySelector('span').textContent = 'REINTENTAR';
  $('estado').className = 'estado mal';
  $('estado').textContent = msg;
  sonido('golpe', 0.4);
};
function terminado() {
  const b = $('b-jugar');
  b.disabled = false;
  b.classList.remove('latiendo');
  b.querySelector('span').textContent = 'JUGAR';
  if (SONIDOS.latido) SONIDOS.latido.pause();
}

async function arrancar() {
  const info = await API().iniciar();
  $('version').textContent = 'v' + info.version;
  $('usuario').value = info.usuario || '';
  (info.usuario ? $('clave') : $('usuario')).focus();
  musica(info.musica !== false);
  $('b-musica').onclick = () => { musica(!musicaOn); API().musica(musicaOn); };
  $('b-min').onclick = () => API().minimizar();
  $('b-cerrar').onclick = () => API().cerrar();
  // se ríe si piensas en irte
  $('b-cerrar').addEventListener('mouseenter', () => { risa = true; sonido('risa', 0.35); });
  $('b-cerrar').addEventListener('mouseleave', () => { risa = false; });
  $('b-entrar').onclick = entrar;
  $('clave').addEventListener('keydown', (e) => { if (e.key === 'Enter') entrar(); });
  $('usuario').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('clave').focus(); });
  $('b-jugar').onclick = jugar;
  $('b-salir').onclick = () => { API().salir_sesion(); $('clave').value = ''; mostrar('p-entrar'); };
  botonesConSonido();
  // se prende como una tele vieja: un destello blanco, la estática y luego él saluda
  sonido('estatica', 0.45);
  const d = $('destello');
  d.style.transition = 'none';
  d.style.opacity = 0.9; d.style.height = '40%'; d.style.top = '30%';
  requestAnimationFrame(() => {
    d.style.transition = 'all .35s ease-in';
    d.style.height = '2px'; d.style.top = '50%'; d.style.opacity = 0;
  });
  setTimeout(() => { $('negro').style.opacity = 0; }, 250);
  setTimeout(() => decir('que_tenemos'), 2000);
}

// en un navegador (para probar el diseño) no hay Python: una API de mentira
function apiDeMentira() {
  const espera = (v, ms = 300) => new Promise((ok) => setTimeout(() => ok(v), ms));
  window.pywebview = {
    api: {
      iniciar: () => espera({ version: '1.0.0', usuario: '', musica: true }, 10),
      entrar: (u, k) => espera(k === 'prueba123' ? { ok: true, usuario: u } : { ok: false, error: 'Usuario o contraseña incorrectos.' }),
      ram: () => espera({ total: 7.9, opciones: [[4, true], [6, true], [8, false], [10, false]], elegida: 4 }, 10),
      elegir_ram: () => espera(true, 1), jugar: () => {
        let k = 0;
        const id = setInterval(() => { k += 0.1; window.avance(k, 'Mods: coleccionista-1.0.0.jar'); if (k >= 1) { clearInterval(id); window.listo({ abierto: true }); } }, 300);
      },
      salir_sesion: () => espera(true, 1), musica: () => espera(true, 1), minimizar: () => {}, cerrar: () => {},
    },
  };
}

(async () => {
  try {
    modelo = await cargarModelo();
    escena.add(modelo.raiz);
  } catch (e) {
    console.log('sin modelo', e);
  }
  requestAnimationFrame(paso);
  if (location.search.includes('demo')) apiDeMentira();
  if (API()) arrancar();
  else window.addEventListener('pywebviewready', arrancar);
})();
