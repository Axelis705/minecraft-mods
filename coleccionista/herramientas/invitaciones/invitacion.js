/* Invitación del Coleccionista: el amigo (con su skin) de pie dentro de una vitrina, con su placa de latón.
   Parámetros: ?nombre=Scardax&skin=ruta/skin.png&num=07  (los brazos delgados se detectan solos). Al terminar: window.listo = true. */
'use strict';
const P = new URLSearchParams(location.search);
const NOMBRE = P.get('nombre') || 'Scardax';
const SKIN = P.get('skin') || 'skin.png';
const NUM = P.get('num') || '02';
const W = 1080, H = 1350;
const r = (d) => d * Math.PI / 180;
let semilla = 11;
const azar = () => { semilla = (semilla * 1664525 + 1013904223) >>> 0; return semilla / 4294967296; };

document.getElementById('nombre').textContent = NOMBRE;
document.getElementById('placaNombre').textContent = NOMBRE.toUpperCase();
document.getElementById('num').textContent = 'JUGADOR ' + NUM;

const ren = new THREE.WebGLRenderer({ canvas: document.getElementById('escena'), antialias: true, preserveDrawingBuffer: true });
ren.setSize(W, H, false);
ren.setPixelRatio(1);
ren.outputColorSpace = THREE.SRGBColorSpace;
ren.toneMapping = THREE.ACESFilmicToneMapping;
ren.toneMappingExposure = 1.2;
ren.shadowMap.enabled = true;
ren.shadowMap.type = THREE.PCFSoftShadowMap;
const escena = new THREE.Scene();
escena.background = new THREE.Color(0x030102);
escena.fog = new THREE.Fog(0x030102, 230, 420);
const cam = new THREE.PerspectiveCamera(26, W / H, 1, 1200);
cam.position.set(-44, 24, -262);
cam.lookAt(0, 9.5, 0);

// ---------- luces ----------
escena.add(new THREE.HemisphereLight(0x3a2a30, 0x050203, 0.55));
const foco = new THREE.SpotLight(0xffe2b8, 5.2, 0, r(17), 0.55, 0);
foco.position.set(0, 98, -24);
foco.target.position.set(0, 10, 0);
foco.castShadow = true;
foco.shadow.mapSize.set(2048, 2048);
foco.shadow.bias = -0.0004;
escena.add(foco, foco.target);
const rojo = new THREE.PointLight(0xff2418, 1.6, 0, 0);
rojo.position.set(10, 34, 46);
escena.add(rojo);
const frente = new THREE.DirectionalLight(0x8896b8, 0.3);
frente.position.set(-60, 40, -200);
escena.add(frente);
// luz cálida de frente, angosta, solo para la figura (para que se vea su ropa)
const clave = new THREE.SpotLight(0xffd6a8, 1.9, 0, r(7), 0.7, 0);
clave.position.set(-34, 46, -150);
clave.target.position.set(0, 17, 0);
escena.add(clave, clave.target);

// ---------- piso, pedestal y vitrina ----------
const piso = new THREE.Mesh(new THREE.PlaneGeometry(900, 900), new THREE.MeshStandardMaterial({ color: 0x120c0b, roughness: 0.85 }));
piso.rotation.x = -Math.PI / 2;
piso.position.y = -12;
piso.receiveShadow = true;
escena.add(piso);
const madera = new THREE.MeshStandardMaterial({ color: 0x2b1912, roughness: 0.55, metalness: 0.05 });
const laton = new THREE.MeshStandardMaterial({ color: 0xb8904f, roughness: 0.35, metalness: 0.85 });
function caja(w, h, d, x, y, z, mat, sombra = true) {
  const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat);
  m.position.set(x, y, z);
  m.castShadow = sombra;
  m.receiveShadow = true;
  escena.add(m);
  return m;
}
caja(28, 11, 21, 0, -6.5, 0, madera);            // pedestal
caja(29, 1, 22, 0, -0.5, 0, laton);              // moldura de arriba
caja(30, 1.2, 23, 0, -11.4, 0, laton);           // moldura de abajo
// vitrina: marco de latón y vidrios
const VW = 22, VH = 40, VD = 16, VY = 0;
for (const sx of [-1, 1]) for (const sz of [-1, 1]) caja(0.8, VH, 0.8, sx * VW / 2, VY + VH / 2, sz * VD / 2, laton, false);
for (const sz of [-1, 1]) { caja(VW + 0.8, 0.8, 0.8, 0, VY + VH, sz * VD / 2, laton, false); caja(VW + 0.8, 0.8, 0.8, 0, VY + 0.4, sz * VD / 2, laton, false); }
for (const sx of [-1, 1]) { caja(0.8, 0.8, VD, sx * VW / 2, VY + VH, 0, laton, false); caja(0.8, 0.8, VD, sx * VW / 2, VY + 0.4, 0, laton, false); }
caja(VW + 2.4, 1.6, VD + 2.4, 0, VY + VH + 1.2, 0, madera, false);   // tapa
function texturaBrillo() {
  const c = document.createElement('canvas');
  c.width = 256; c.height = 512;
  const x = c.getContext('2d');
  x.clearRect(0, 0, 256, 512);
  x.translate(128, 256);
  x.rotate(r(-28));
  for (const [dx, w, a] of [[-60, 38, 0.22], [-12, 12, 0.16], [70, 22, 0.1]]) {
    const g = x.createLinearGradient(dx - w, 0, dx + w, 0);
    g.addColorStop(0, 'rgba(255,255,255,0)');
    g.addColorStop(0.5, `rgba(255,248,235,${a})`);
    g.addColorStop(1, 'rgba(255,255,255,0)');
    x.fillStyle = g;
    x.fillRect(dx - w, -500, w * 2, 1000);
  }
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}
const vidrio = new THREE.MeshPhysicalMaterial({ color: 0xffffff, transparent: true, opacity: 0.07, roughness: 0.05, metalness: 0,
  side: THREE.DoubleSide, depthWrite: false });
const brillo = new THREE.MeshBasicMaterial({ map: texturaBrillo(), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending });
function panel(w, h, x, y, z, ry, conBrillo) {
  const g = new THREE.PlaneGeometry(w, h);
  const m = new THREE.Mesh(g, vidrio);
  m.position.set(x, y, z);
  m.rotation.y = ry;
  escena.add(m);
  if (conBrillo) {
    const b = new THREE.Mesh(g, brillo);
    b.position.set(x, y, z - 0.05);
    b.rotation.y = ry;
    b.renderOrder = 5;
    escena.add(b);
  }
}
panel(VW, VH, 0, VY + VH / 2, -VD / 2, 0, true);
panel(VW, VH, 0, VY + VH / 2, VD / 2, 0, false);
panel(VD, VH, -VW / 2, VY + VH / 2, 0, Math.PI / 2, false);
panel(VD, VH, VW / 2, VY + VH / 2, 0, Math.PI / 2, false);

// haz de luz del foco y polvo
function texturaHaz() {
  const c = document.createElement('canvas');
  c.width = 4; c.height = 128;
  const x = c.getContext('2d');
  const gr = x.createLinearGradient(0, 0, 0, 128);
  gr.addColorStop(0, 'rgba(255,230,180,0.0)');
  gr.addColorStop(0.2, 'rgba(255,230,180,0.5)');
  gr.addColorStop(1, 'rgba(255,230,180,0.06)');
  x.fillStyle = gr;
  x.fillRect(0, 0, 4, 128);
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}
const haz = new THREE.Mesh(new THREE.ConeGeometry(27, 108, 40, 1, true),
  new THREE.MeshBasicMaterial({ map: texturaHaz(), transparent: true, opacity: 0.13, depthWrite: false, blending: THREE.AdditiveBlending,
    side: THREE.DoubleSide, fog: false }));
haz.position.set(0, 44, -5);
escena.add(haz);
const POLVO = 170, pp = new Float32Array(POLVO * 3);
for (let i = 0; i < POLVO; i++) {
  const a = azar() * Math.PI * 2, rr = Math.sqrt(azar()) * 24, y = azar() * 90 - 8;
  pp.set([Math.cos(a) * rr * (0.35 + y / 140), y, Math.sin(a) * rr * (0.35 + y / 140) - 5], i * 3);
}
const pg = new THREE.BufferGeometry();
pg.setAttribute('position', new THREE.BufferAttribute(pp, 3));
escena.add(new THREE.Points(pg, new THREE.PointsMaterial({ color: 0xffe8c0, size: 0.55, transparent: true, opacity: 0.6, depthWrite: false,
  blending: THREE.AdditiveBlending })));

// ojos rojos en lo oscuro, al fondo (alguien mira)
function texturaHalo() {
  const c = document.createElement('canvas');
  c.width = c.height = 64;
  const x = c.getContext('2d');
  const g = x.createRadialGradient(32, 32, 0, 32, 32, 32);
  g.addColorStop(0, 'rgba(255,40,20,0.9)');
  g.addColorStop(0.25, 'rgba(255,20,10,0.35)');
  g.addColorStop(1, 'rgba(255,0,0,0)');
  x.fillStyle = g;
  x.fillRect(0, 0, 64, 64);
  return new THREE.CanvasTexture(c);
}
const halo = new THREE.SpriteMaterial({ map: texturaHalo(), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, fog: false,
  opacity: 0.55 });
const ojoMat = new THREE.MeshBasicMaterial({ color: 0xc81a10, fog: false });
for (const dx of [-1.4, 1.4]) {
  const o = new THREE.Mesh(new THREE.PlaneGeometry(1.1, 1.1), ojoMat);
  o.position.set(-31 + dx, 42, 90);
  o.rotation.y = Math.PI;
  escena.add(o);
  const h = new THREE.Sprite(halo);
  h.position.copy(o.position);
  h.scale.set(7, 7, 1);
  escena.add(h);
}

// ---------- el jugador con su skin ----------
function jugador(img) {
  const tex = new THREE.Texture(img);
  tex.magFilter = THREE.NearestFilter;
  tex.minFilter = THREE.NearestFilter;
  tex.generateMipmaps = false;
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.needsUpdate = true;
  const mat = new THREE.MeshLambertMaterial({ map: tex, alphaTest: 0.5 });
  const mat2 = new THREE.MeshLambertMaterial({ map: tex, alphaTest: 0.5, side: THREE.DoubleSide });
  // ¿brazos delgados? (en las skins de 3 px esa columna queda vacía)
  const cv = document.createElement('canvas');
  cv.width = 64; cv.height = 64;
  const cx = cv.getContext('2d');
  cx.drawImage(img, 0, 0);
  const delgado = cx.getImageData(54, 20, 2, 12).data.every((v, i) => i % 4 !== 3 || v === 0);
  const AW = delgado ? 3 : 4;
  const TW = 64, TH = img.height === 32 ? 32 : 64;
  function pieza(w, h, d, u, v, inf, m) {
    const g = new THREE.BoxGeometry(w + 2 * inf, h + 2 * inf, d + 2 * inf);
    const cs = { east: [u, v + d, d, h], west: [u + d + w, v + d, d, h], up: [u + d, v, w, d], down: [u + d + w, v, w, d],
      north: [u + d, v + d, w, h], south: [u + d + w + d, v + d, w, h] };
    const orden = ['east', 'west', 'up', 'down', 'south', 'north'];
    const uv = g.attributes.uv;
    for (let f = 0; f < 6; f++) {
      const [x, y, cw, ch] = cs[orden[f]];
      const u0 = x / TW, u1 = (x + cw) / TW, v0 = 1 - y / TH, v1 = 1 - (y + ch) / TH;
      const q = [[u0, v0], [u1, v0], [u0, v1], [u1, v1]];
      for (let k = 0; k < 4; k++) uv.setXY(f * 4 + k, q[k][0], q[k][1]);
    }
    uv.needsUpdate = true;
    const mesh = new THREE.Mesh(g, m);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    return mesh;
  }
  // mira hacia -Z; su derecha es +X
  const raiz = new THREE.Group();
  function parte(px, py, pz, ox, oy, oz, base, capa) {
    const gr = new THREE.Group();
    gr.position.set(px, py, pz);
    base.position.set(ox, oy, oz);
    gr.add(base);
    if (capa && TH === 64) { capa.position.set(ox, oy, oz); gr.add(capa); }
    raiz.add(gr);
    return gr;
  }
  const cabeza = parte(0, 24, 0, 0, 4, 0, pieza(8, 8, 8, 0, 0, 0, mat), pieza(8, 8, 8, 32, 0, 0.5, mat2));
  parte(0, 18, 0, 0, 0, 0, pieza(8, 12, 4, 16, 16, 0, mat), pieza(8, 12, 4, 16, 32, 0.25, mat2));
  const bd = parte(4 + AW / 2 - 1, 22, 0, 1, -4, 0, pieza(AW, 12, 4, 40, 16, 0, mat), pieza(AW, 12, 4, 40, 32, 0.25, mat2));
  const bi = parte(-(4 + AW / 2 - 1), 22, 0, -1, -4, 0,
    pieza(AW, 12, 4, TH === 64 ? 32 : 40, TH === 64 ? 48 : 16, 0, mat), pieza(AW, 12, 4, 48, 48, 0.25, mat2));
  parte(2, 12, 0, 0, -6, 0, pieza(4, 12, 4, 0, 16, 0, mat), pieza(4, 12, 4, 0, 32, 0.25, mat2));
  parte(-2, 12, 0, 0, -6, 0, pieza(4, 12, 4, TH === 64 ? 16 : 0, TH === 64 ? 48 : 16, 0, mat), pieza(4, 12, 4, 0, 48, 0.25, mat2));
  // pose de muñeco en exhibición: derecho, brazos pegados y la cabeza un poco ladeada
  cabeza.rotation.set(r(4), r(-6), r(7));
  bd.rotation.z = r(4);
  bi.rotation.z = r(-4);
  raiz.rotation.y = r(9);
  raiz.position.y = 0.6;
  return raiz;
}

// grano de película
function grano() {
  const c = document.getElementById('grano');
  const x = c.getContext('2d');
  const d = x.createImageData(c.width, c.height);
  for (let i = 0; i < d.data.length; i += 4) {
    const v = azar() * 255;
    d.data[i] = d.data[i + 1] = d.data[i + 2] = v;
    d.data[i + 3] = 255;
  }
  x.putImageData(d, 0, 0);
}

(async () => {
  const img = new Image();
  img.src = SKIN;
  await img.decode();
  escena.add(jugador(img));
  grano();
  ren.render(escena, cam);
  // la placa va en el frente del pedestal
  const p = new THREE.Vector3(0, -6.2, -10.6).project(cam);
  const placa = document.getElementById('placa');
  placa.style.left = ((p.x + 1) / 2 * W) + 'px';
  placa.style.top = ((1 - p.y) / 2 * H) + 'px';
  await document.fonts.ready;
  window.listo = true;
})().catch((e) => { document.title = 'ERROR ' + e; window.listo = true; });
