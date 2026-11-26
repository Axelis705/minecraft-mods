/* El Coleccionista en 3D para el tráiler: lo mismo que el launcher, pero cuadro por cuadro (window.dibujar(toma, t)). */
'use strict';
const $ = (id) => document.getElementById(id);
const r = (d) => d * Math.PI / 180;
const lerp = (a, b, k) => a + (b - a) * k;
const suave = (x) => { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); };
// azar con semilla (así cada cuadro sale igual cada vez)
let semilla = 7;
const azar = () => { semilla = (semilla * 1664525 + 1013904223) >>> 0; return semilla / 4294967296; };

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

const lienzo = $('escena');
const render = new THREE.WebGLRenderer({ canvas: lienzo, alpha: true, antialias: true, preserveDrawingBuffer: true });
render.setPixelRatio(1);
const VERT = location.search.indexOf('v=1') >= 0;
render.setSize(VERT ? 1080 : 1920, VERT ? 1920 : 1080, false);
const escena = new THREE.Scene();
const camara = new THREE.PerspectiveCamera(VERT ? 36 : 24, VERT ? 9 / 16 : 16 / 9, 1, 2000);
const ambiente = new THREE.AmbientLight(0xffffff, 0.55);
const foco = new THREE.SpotLight(0xffe2b0, 3.2, 0, 0.42, 0.65, 0);
foco.position.set(0, 190, -70);
foco.target.position.set(0, 25, 0);
const contra = new THREE.PointLight(0xc01020, 1.4, 0, 0);
contra.position.set(0, 70, 70);
const frente = new THREE.DirectionalLight(0x9a8cff, 0.25);
frente.position.set(-60, 40, -120);
escena.add(ambiente, foco, foco.target, contra, frente);

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

const POLVO = 260;
const polvoBase = [];
for (let i = 0; i < POLVO; i++) {
  const a = azar() * Math.PI * 2, rr = Math.sqrt(azar()) * 42, y = azar() * 190 - 10;
  polvoBase.push([Math.cos(a) * rr, y, Math.sin(a) * rr - 10, 2 + azar() * 4, azar() * 6.28, 0.4 + azar() * 0.8]);
}
const polvoPos = new Float32Array(POLVO * 3);
const polvoGeo = new THREE.BufferGeometry();
polvoGeo.setAttribute('position', new THREE.BufferAttribute(polvoPos, 3));
const polvo = new THREE.Points(polvoGeo, new THREE.PointsMaterial({ color: 0xffe8c0, size: 1.1, transparent: true, opacity: 0.55,
  depthWrite: false, blending: THREE.AdditiveBlending }));
escena.add(polvo);

let modelo = null;
const SIGNO = [-1, 1, -1];
function poner(nombre, x, y, z, dy = 0) {
  const h = modelo.huesos[nombre], b = modelo.base[nombre];
  if (!h) return;
  h.rotation.set(b.rot[0] + SIGNO[0] * x, b.rot[1] + SIGNO[1] * y, b.rot[2] + SIGNO[2] * z);
  h.position.set(b.pos.x, b.pos.y - dy, b.pos.z);
}

// las posturas (cabeza kx ky kz; brazos)
const MANOS_JUNTAS = [-0.55, 0.12, -0.35, -1.9, -0.55, -0.12, -0.35, 1.9];
const HABLA = [
  [[0.02, 0.22, -0.08], [-0.7, 0.4, -0.25, -2.15, -0.55, -0.12, -0.35, 1.9]],
  [[0, -0.1, 0.05], [-0.3, 0.45, -0.5, -0.75, -0.3, -0.45, -0.5, 0.75]],
  [[0.02, -0.22, 0.08], [-0.55, 0.12, -0.35, -1.9, -0.7, -0.4, -0.25, 2.15]],
];
const RISA = [[-0.38, 0, 0.1], [-0.45, 0.25, -0.3, -1.5, -0.45, -0.25, -0.3, 1.5]];

function cuerpo(t, cab, br, extra = {}) {
  const resp = Math.sin(t * 1.7);
  let [kx, ky, kz] = cab;
  let cy = -resp * 0.35;
  if (extra.risa) {
    cy += Math.sin(t * 40) * 0.6;
    kz += Math.sin(t * 23) * 0.04;
  } else if (extra.habla) {
    kx += Math.sin(t * 7) * 0.05 + Math.sin(t * 11.3) * 0.02;
    ky += Math.sin(t * 1.9) * 0.05;
  } else if (!extra.fijo) {
    kx += Math.sin(t * 0.53) * 0.04;
    ky += Math.sin(t * 0.37) * 0.14;
  }
  const mano = extra.habla && !extra.risa ? Math.sin(t * 3.1) * 0.12 : 0;
  const dedos = !extra.habla ? Math.sin(t * 9) * 0.025 * Math.max(0, Math.sin(t * 0.8)) : 0;
  poner('cuerpo', (extra.risa ? 0.06 : -0.04) + resp * 0.012, 0, 0, cy);
  poner('cabeza', kx, ky, kz);
  poner('brazo_der', br[0], 0, br[1]);
  poner('antebrazo_der', br[2], 0, br[3] + mano + dedos);
  poner('brazo_izq', br[4], 0, br[5]);
  poner('antebrazo_izq', br[6], 0, br[7] - mano * 0.4 - dedos);
}

function mezcla(a, b, k) { return a.map((v, i) => lerp(v, b[i], k)); }

function luces(luz, apagado) {
  foco.intensity = 3.2 * luz;
  ambiente.intensity = apagado ? 0.004 : 0.5 * (0.7 + 0.3 * luz);
  contra.intensity = apagado ? 0.06 : 1.4;
  frente.intensity = apagado ? 0 : 0.25;
  haz.material.opacity = 0.16 * luz;
  polvo.material.opacity = 0.55 * luz;
  document.body.style.setProperty('--lo', (0.9 * luz).toFixed(3));
  document.body.style.setProperty('--fb', apagado ? '0.05' : (0.55 + 0.25 * luz).toFixed(3));
}

function polvoEn(t) {
  for (let i = 0; i < POLVO; i++) {
    const b = polvoBase[i];
    let y = b[1] + b[3] * t * 0.6;
    y = ((y + 10) % 195) - 10;
    polvoPos[i * 3] = b[0] + Math.sin(t * b[5] + b[4]) * 1.2;
    polvoPos[i * 3 + 1] = y;
    polvoPos[i * 3 + 2] = b[2];
  }
  polvoGeo.attributes.position.needsUpdate = true;
}

const grano = $('grano').getContext('2d');
const granoImg = grano.createImageData(480, 270);
function ruido(fuerte) {
  const d = granoImg.data;
  for (let i = 0; i < d.length; i += 4) {
    const v = azar() * 255;
    d[i] = d[i + 1] = d[i + 2] = v;
    d[i + 3] = 255;
  }
  grano.putImageData(granoImg, 0, 0);
  $('grano').style.opacity = fuerte ? '0.35' : '0.08';
}

function camaraEn(px, py, pz, mx, my, mz, ox = 0, oy = 0) {
  camara.position.set(px + ox, py + oy, pz);
  camara.lookAt(mx + ox * 0.3, my + oy * 0.3, mz);
}

// parpadeo del reflector (determinista)
function parpadeo(t) {
  const f = Math.floor(t * 11.1);
  return (f % 61 < 2 || Math.floor(t * 14.3) % 97 < 1) ? 0.45 : 1;
}

/**
 * Dibuja un cuadro de una toma.
 *  presenta: se acerca despacio mientras habla ("Permítanme presentarme. Soy… el Coleccionista").
 *  mira:     apagón (solo los ojos) y al volver la luz está más cerca, mirándote fijo.
 *  risa:     se ríe (la cabeza atrás, se sacude) con el reflector parpadeando.
 *  espera:   de lejos, manos juntas, la cabeza ladeada (para el fondo del título).
 *  ojos:     todo negro y solo los ojos (para cortes).
 */
window.dibujar = function (toma, t, dur) {
  if (!modelo) return false;
  const k = dur ? t / dur : 0;
  polvoEn(t + 10);
  let luz = (0.85 + 0.1 * Math.sin(t * 2.3) + 0.05 * Math.sin(t * 7.1)) * parpadeo(t);
  let apagado = false, ojos = 0.7, escala = 1, fuerte = false;
  $('negro').style.opacity = '0';
  $('escena').style.transform = '';
  if (toma === 'presenta') {
    const fase = Math.min(2, Math.floor(k * 3));
    const g = HABLA[fase];
    cuerpo(t, g[0], g[1], { habla: true });
    ojos = 0.85 + 0.15 * Math.abs(Math.sin(t * 7));
    const s = suave(k);
    camaraEn(lerp(0, 0, s), lerp(40, 47, s), lerp(-190, -92, s), 0, lerp(34, 44, s), 0, Math.sin(t * 0.4) * 2, Math.sin(t * 0.3));
    luz = Math.max(luz, 0.8);
  } else if (toma === 'mira') {
    const corte = 1.7;
    if (t < corte) {
      apagado = true;
      luz = 0;
      ojos = 1;
      cuerpo(t, [0.02, 0, 0], MANOS_JUNTAS, { fijo: true });
      camaraEn(0, 44, -120, 0, 43, 0);
      escala = 1.06;
    } else {
      const tt = t - corte;
      cuerpo(t, [0.02, 0, 0], MANOS_JUNTAS, { fijo: true });
      escala = 1.08;
      ojos = 1;
      const sac = Math.max(0, 1 - tt * 2.5);
      camaraEn(0, 45, lerp(-78, -70, suave(tt / 2)), 0, 44, 0, (azar() - 0.5) * 6 * sac, (azar() - 0.5) * 4 * sac);
      luz = Math.max(0.9, luz);
      if (tt < 0.12) { fuerte = true; $('escena').style.transform = `translateX(${(azar() * 30 - 15).toFixed(0)}px)`; }
    }
  } else if (toma === 'risa') {
    cuerpo(t, RISA[0], RISA[1], { risa: true });
    ojos = 1;
    camaraEn(0, 46, -120 + t * 6, 0, 44, 0, Math.sin(t * 31) * 1.5, Math.sin(t * 27));
    luz = (Math.floor(t * 9) % 3 === 0) ? 0.35 : 1;
  } else if (toma === 'espera') {
    const lado = Math.floor((t + 3) / 6.5) % 2 === 0 ? 0.2 : -0.12;
    cuerpo(t, [0.08, 0, lado], MANOS_JUNTAS, {});
    const s = suave(k);
    camaraEn(lerp(-8, 6, s), 40, lerp(-215, -200, s), 0, 34, 0);
  } else if (toma === 'ojos') {
    apagado = true; luz = 0;
    ojos = 0.78 + 0.22 * Math.abs(Math.sin(t * 1.3)) * (Math.floor(t * 8) % 23 === 0 ? 0.2 : 1);
    cuerpo(t, [0.02, 0, 0], MANOS_JUNTAS, { fijo: true });
    camaraEn(0, 40, lerp(-118, -100, suave(k)), 0, 33, 0);
    escala = 1.06;
  }
  modelo.raiz.scale.set(escala, escala, escala);
  modelo.matG.color.setScalar(Math.min(1, ojos));
  luces(luz, apagado);
  ruido(fuerte);
  render.render(escena, camara);
  return true;
};

cargarModelo().then((m) => {
  modelo = m;
  escena.add(m.raiz);
  window.listo3d = true;
});
