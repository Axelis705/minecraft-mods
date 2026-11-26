"""Los letreros del tráiler (PNG transparentes a 1920x1080) hechos con HTML y Playwright."""
import os
import sys

from playwright.sync_api import sync_playwright

AQUI = os.path.dirname(os.path.abspath(__file__))
SAL = os.path.join(AQUI, 'cartas')

ESTILO = """
<style>
  html, body { margin: 0; width: 1920px; height: 1080px; background: transparent; overflow: hidden; }
  .c { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
  .abajo { justify-content: flex-end; padding-bottom: 158px; box-sizing: border-box; }
  .bajo { padding-top: 330px; box-sizing: border-box; }
  .serif { font-family: 'Latin Modern Roman', 'GFS Baskerville', 'DejaVu Serif', serif; }
  .chico { font: 600 34px 'Latin Modern Roman Caps', 'Latin Modern Roman', serif; letter-spacing: 18px; color: #c9a25a;
    text-shadow: 0 0 18px rgba(0,0,0,.9); }
  .grande { font: 700 150px 'Latin Modern Roman', serif; letter-spacing: 14px; color: #efe3cc;
    text-shadow: 6px 6px 0 #3a0008, 0 0 40px rgba(142, 23, 38, .7), 0 0 4px #000; }
  .rojo { color: #b3192b; }
  .linea { height: 3px; width: 760px; margin: 26px 0 22px; background: linear-gradient(90deg, transparent, #8e1726 20%, #d9a93a 50%, #8e1726 80%, transparent); }
  .medio { font: 700 104px 'Latin Modern Roman', serif; letter-spacing: 22px; color: #efe3cc;
    text-shadow: 5px 5px 0 #3a0008, 0 0 36px rgba(142, 23, 38, .75), 0 0 4px #000; }
  .lema { font: italic 600 44px 'Latin Modern Roman', serif; color: #cdbb9c; letter-spacing: 2px; text-shadow: 0 0 14px #000; }
  .enlace { font: 700 64px 'Latin Modern Mono', 'DejaVu Sans Mono', monospace; color: #efe3cc; letter-spacing: 4px; margin-top: 18px;
    padding: 18px 46px; border: 3px solid #8e1726; background: rgba(20, 4, 8, .82); box-shadow: 0 0 40px rgba(142,23,38,.55); }
  .el { font: 600 58px 'Latin Modern Roman Caps', serif; letter-spacing: 30px; color: #b8a888; margin-bottom: -6px; }
  .titulo { font: 700 158px 'Latin Modern Roman', serif; letter-spacing: 10px; color: #efe3cc;
    text-shadow: 8px 8px 0 #3a0008, 0 0 50px rgba(142, 23, 38, .8), 0 0 6px #000; }
</style>
"""

CARTAS = {
    'evento': '<div class="c abajo"><div class="chico">UN EVENTO DE AXELIS</div></div>',
    'capitulos': '<div class="c"><div class="medio">7 CAPÍTULOS</div><div class="linea"></div>'
                 '<div class="lema">una prueba en cada sala</div></div>',
    'vida': '<div class="c"><div class="medio">UNA SOLA <span class="rojo">VIDA</span></div></div>',
    'sale': '<div class="c"><div class="medio">SOLO <span class="rojo">UNO</span> SALE</div></div>',
    'titulo': '<div class="c bajo"><div class="el">EL</div><div class="titulo">COLEC<span class="rojo">C</span>IONISTA</div>'
              '<div class="linea"></div><div class="lema">Un evento de Axelis</div></div>',
    'discord': '<div class="c"><div class="chico">ENTRA A LA COLECCIÓN</div><div class="enlace">discord.gg/sttQcQsc7m</div></div>',
    'fecha': '<div class="c"><div class="chico">JUEVES</div><div class="medio">10 DE DICIEMBRE</div><div class="linea"></div>'
             '<div class="lema">7:00 PM · hora del centro de México</div></div>',
    'pronto': '<div class="c"><div class="medio" style="font-size:84px;letter-spacing:30px">PRÓXIMAMENTE</div></div>',
}


VERTICAL = """
<style>
  html, body { width: 1080px; height: 1920px; }
  .chico { font-size: 30px; letter-spacing: 10px; }
  .medio { font-size: 66px; letter-spacing: 12px; text-align: center; }
  .lema { font-size: 36px; }
  .linea { width: 560px; }
  .el { font-size: 44px; letter-spacing: 22px; }
  .titulo { font-size: 86px; letter-spacing: 6px; }
  .enlace { font-size: 44px; padding: 14px 30px; }
  .abajo { padding-bottom: 250px; }
  .bajo { padding-top: 520px; }
</style>
"""


def main():
    os.makedirs(SAL, exist_ok=True)
    solo = sys.argv[1:]
    with sync_playwright() as p:
        b = p.chromium.launch()
        for suf, (w, h), extra in (('', (1920, 1080), ''), ('_v', (1080, 1920), VERTICAL)):
            pg = b.new_page(viewport={'width': w, 'height': h})
            for n, html in CARTAS.items():
                if solo and n not in solo:
                    continue
                pg.set_content('<!doctype html><html><head><meta charset="utf-8">' + ESTILO + extra + '</head><body>' + html
                               + '</body></html>')
                pg.wait_for_timeout(150)
                pg.screenshot(path=os.path.join(SAL, n + suf + '.png'), omit_background=True)
                print('ok', n + suf)
            pg.close()
        b.close()


if __name__ == '__main__':
    main()
