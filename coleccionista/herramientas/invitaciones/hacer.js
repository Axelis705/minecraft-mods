// Hace las invitaciones del Coleccionista (una por amigo, con su skin).
// Uso: node hacer.js <lista.json> <carpeta de salida>
// lista.json: [{"nombre": "Scardax", "skin": "/ruta/Scardax.png", "num": "07"}, ...]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  const lista = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const salida = process.argv[3];
  fs.mkdirSync(salida, { recursive: true });
  const b = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist',
    '--allow-file-access-from-files'] });
  const p = await b.newPage({ viewport: { width: 1080, height: 1350 } });
  p.on('pageerror', (e) => console.log('ERROR en la página:', e.message));
  const html = 'file://' + path.resolve(__dirname, 'invitacion.html');
  for (const a of lista) {
    const q = new URLSearchParams({ nombre: a.nombre, skin: 'file://' + path.resolve(a.skin), num: a.num });
    await p.goto(html + '?' + q.toString());
    await p.waitForFunction(() => window.listo === true, null, { timeout: 60000 });
    const t = await p.title();
    if (t.startsWith('ERROR')) console.log(a.nombre, t);
    const archivo = path.join(salida, `invitacion_${a.nombre.toLowerCase().replace(/[^a-z0-9]+/g, '_')}.png`);
    await p.screenshot({ path: archivo });
    console.log('listo:', archivo);
  }
  await b.close();
})();
