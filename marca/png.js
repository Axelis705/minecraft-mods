const {chromium}=require('playwright');const fs=require('fs');
(async()=>{const b=await chromium.launch();const p=await b.newPage();
const out=process.argv[2];
const items=[['axelis_studio_blanco',2400],['axelis_studio_negro',2400],['axelis_blanco',2400],['axelis_negro',2400],['axelis_A_blanco',1024],['axelis_A_negro',1024]];
for(const [n,w] of items){const svg=fs.readFileSync(`${out}/${n}.svg`,'utf8');const m=svg.match(/viewBox="0 0 ([\d.]+) ([\d.]+)"/);const h=Math.round(w*m[2]/m[1]);
await p.setViewportSize({width:w,height:h});await p.setContent(`<html><body style="margin:0;background:transparent">${svg.replace(/width="\d+" height="\d+"/,`width="${w}" height="${h}"`)}</body></html>`);
await p.screenshot({path:`${out}/png/${n}.png`,omitBackground:true,clip:{x:0,y:0,width:w,height:h}});}
// foto de perfil: cuadro negro con la A blanca al centro (Discord lo recorta redondo)
const a=fs.readFileSync(`${out}/axelis_A_blanco.svg`,'utf8').replace(/width="\d+" height="\d+"/,'width="440" height="431"');
await p.setViewportSize({width:1024,height:1024});await p.setContent(`<html><body style="margin:0;width:1024px;height:1024px;background:#000;display:flex;align-items:center;justify-content:center">${a}</body></html>`);
await p.screenshot({path:`${out}/png/foto_de_perfil.png`});await b.close();})();
