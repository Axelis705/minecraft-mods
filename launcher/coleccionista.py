"""El launcher del Coleccionista (Windows): una ventana sin bordes con el escenario en 3D (web/, three.js dentro de WebView2) donde se
entra con el usuario y la contraseña que da Axelis, se elige la memoria (4, 6, 8 o 10 GB) y Jugar prepara todo (instalador.py) y abre
el launcher oficial de Minecraft con el perfil del evento.

Uso: ElColeccionista.exe
     ElColeccionista.exe --prueba <carpeta>   entra con el usuario de prueba, prepara todo (sin abrir Minecraft), saca fotos y se cierra
"""
import json
import os
import sys
import threading
import time

import instalador as ins

VERSION = '1.0.0'
W, H = 1000, 580
PREFS = os.path.join(ins.carpeta_juego(), 'launcher.json')


def leer_prefs():
    try:
        with open(PREFS, encoding='utf-8') as f:
            return json.load(f)
    except Exception:  # noqa: BLE001
        return {}


def guardar_prefs(p):
    try:
        os.makedirs(os.path.dirname(PREFS), exist_ok=True)
        with open(PREFS, 'w', encoding='utf-8') as f:
            json.dump(p, f)
    except Exception:  # noqa: BLE001
        pass


class Api:
    """Lo que la página puede pedirle a Python (window.pywebview.api.*)."""

    def __init__(self, prueba=False):
        self._prefs = leer_prefs()
        self._prueba = prueba
        self._ventana = None
        self._trabajando = False

    def iniciar(self):
        return {'version': VERSION, 'usuario': self._prefs.get('usuario', ''), 'musica': self._prefs.get('musica', True)}

    def entrar(self, usuario, clave):
        try:
            if ins.revisar_clave(usuario, clave):
                self._prefs['usuario'] = usuario.strip()
                guardar_prefs(self._prefs)
                return {'ok': True, 'usuario': usuario.strip()}
            return {'ok': False, 'error': 'Usuario o contraseña incorrectos.'}
        except Exception as e:  # noqa: BLE001
            return {'ok': False, 'error': str(e)}

    def ram(self):
        posibles = ins.ram_posibles()
        elegida = self._prefs.get('ram')
        buenos = [g for g, si in posibles if si]
        if elegida not in buenos:
            elegida = 6 if 6 in buenos else (buenos[-1] if buenos else 4)
        return {'total': ins.ram_total_gb(), 'opciones': posibles, 'elegida': elegida}

    def elegir_ram(self, g):
        self._prefs['ram'] = int(g)
        guardar_prefs(self._prefs)
        return True

    def musica(self, si):
        self._prefs['musica'] = bool(si)
        guardar_prefs(self._prefs)
        return True

    def salir_sesion(self):
        self._prefs.pop('usuario', None)
        guardar_prefs(self._prefs)
        return True

    def minimizar(self):
        self._ventana.minimize()

    def cerrar(self):
        if not self._trabajando:
            self._ventana.destroy()

    def jugar(self, ram):
        if self._trabajando:
            return False
        self._trabajando = True

        def js(f, *a):
            try:
                self._ventana.evaluate_js('%s(%s)' % (f, ', '.join(json.dumps(x, ensure_ascii=False) for x in a)))
            except Exception:  # noqa: BLE001
                pass

        def trabajo():
            t0 = time.time()
            try:
                res = ins.preparar(int(ram), lambda k, txt: js('avance', k, txt), abrir=not self._prueba)
                js('listo', {'abierto': res.get('abierto', False)})
                if res.get('abierto'):
                    threading.Thread(target=self._esperar_juego, args=(t0,), daemon=True).start()
            except ins.Fallo as e:
                js('fallo', str(e))
            except Exception as e:  # noqa: BLE001
                js('fallo', 'Algo salió mal: %s' % e)
            finally:
                self._trabajando = False
        threading.Thread(target=trabajo, daemon=True).start()
        return True


    def _esperar_juego(self, t0):
        """Cuando el juego del evento ya abrió, este launcher se despide (fundido a negro) y se cierra solo."""
        fin = time.time() + 3 * 3600
        while time.time() < fin:
            if ins.juego_abierto_desde(t0):
                try:
                    self._ventana.evaluate_js('adios()')
                except Exception:  # noqa: BLE001
                    pass
                time.sleep(1.4)
                try:
                    self._ventana.destroy()
                except Exception:  # noqa: BLE001
                    pass
                return
            time.sleep(2)


def sin_webview2():
    """Si la computadora no tiene WebView2 (el motor de Edge que dibuja la ventana), se dice cómo conseguirlo."""
    import ctypes
    import webbrowser
    msg = ('Para ver el launcher hace falta "Microsoft Edge WebView2" (es gratis y de Microsoft).\n\n'
           '¿Abro la página para instalarlo? Luego vuelve a abrir El Coleccionista.')
    if ctypes.windll.user32.MessageBoxW(0, msg, 'El Coleccionista', 0x04 | 0x30) == 6:
        webbrowser.open('https://go.microsoft.com/fwlink/p/?LinkId=2124703')


def prueba(carpeta, ventana):
    """Va solo: entra (mal y luego bien), presiona Jugar, espera a que termine y saca fotos de la ventana."""
    from PIL import ImageGrab
    os.makedirs(carpeta, exist_ok=True)
    log = open(os.path.join(carpeta, 'prueba.log'), 'a', encoding='utf-8')
    fallos = []

    def anota(*a):
        log.write(' '.join(str(x) for x in a) + '\n')
        log.flush()

    def foto(nombre):
        import ctypes
        from ctypes import wintypes
        try:
            h = ctypes.windll.user32.FindWindowW(None, 'El Coleccionista')
            rc = wintypes.RECT()
            ctypes.windll.user32.GetWindowRect(h, ctypes.byref(rc))
            ImageGrab.grab(bbox=(rc.left, rc.top, rc.right, rc.bottom), all_screens=True).save(os.path.join(carpeta, nombre + '.png'))
        except Exception as e:  # noqa: BLE001
            ImageGrab.grab(all_screens=True).save(os.path.join(carpeta, nombre + '.png'))
            anota('foto completa', nombre, e)
        anota('FOTO', nombre)

    def js(codigo):
        return ventana.evaluate_js(codigo)

    try:
        # espera a que la página esté lista (la primera vez el .exe tarda en desempacar y WebView2 en arrancar)
        listo = False
        for _ in range(120):
            try:
                listo = bool(js('window.paginaLista === true'))
            except Exception:  # noqa: BLE001
                listo = False
            if listo:
                break
            time.sleep(0.5)
        anota('pagina lista', listo)
        time.sleep(2.5)
        foto('1_inicio')
        anota('webgl', js("!!(window.THREE && document.getElementById('escena').getContext('webgl2'))"))
        js("document.getElementById('usuario').value='prueba'; document.getElementById('clave').value='mal'; "
           "document.getElementById('b-entrar').click()")
        time.sleep(3)
        foto('2_clave_mal')
        aviso = js("document.getElementById('aviso-entrar').textContent") or ''
        anota('aviso', aviso)
        if 'incorrect' not in aviso:
            fallos.append('no avisó de la clave mala')
        js("document.getElementById('usuario').value='prueba'; document.getElementById('clave').value='%s'; "
           "document.getElementById('b-entrar').click()" % os.environ.get('COLECCIONISTA_CLAVE', 'prueba123'))
        time.sleep(3.5)
        foto('3_jugar')
        if js("document.getElementById('p-jugar').hidden"):
            fallos.append('no entró con la clave buena')
        js("document.getElementById('b-jugar').click()")
        time.sleep(3)
        foto('4_preparando')
        fin = time.time() + 1500
        ultimo = ''
        while time.time() < fin:
            estado = js("document.getElementById('estado').textContent") or ''
            if estado != ultimo:
                anota('ESTADO', estado)
                ultimo = estado
            if estado.startswith('¡Listo') or (js("document.getElementById('estado').className") or '').endswith('mal'):
                break
            time.sleep(2)
        time.sleep(2)
        foto('5_listo')
        if not ultimo.startswith('¡Listo'):
            fallos.append('no terminó: ' + ultimo)
        # un apagón a la fuerza (para ver cómo se ve en lo oscuro) y cuando vuelve la luz
        js('proximoApagon = 0')
        time.sleep(0.7)
        foto('6_apagon')
        time.sleep(2.2)
        foto('7_te_mira')
    except Exception as e:  # noqa: BLE001
        fallos.append(repr(e))
    anota('PRUEBA_LAUNCHER_FIN fallos=%d %s' % (len(fallos), '; '.join(fallos)))
    log.close()
    with open(os.path.join(carpeta, 'resultado.txt'), 'w', encoding='utf-8') as f:
        f.write('ok' if not fallos else 'fallo')
    ventana.destroy()


def main():
    carpeta_prueba = sys.argv[2] if len(sys.argv) > 2 and sys.argv[1] == '--prueba' else None
    # que la música y la voz suenen sin tener que hacer clic primero
    os.environ['WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS'] = '--autoplay-policy=no-user-gesture-required'
    import webview
    api = Api(prueba=bool(carpeta_prueba))
    ventana = webview.create_window('El Coleccionista', url=ins.recurso(os.path.join('web', 'index.html')), js_api=api, width=W,
                                    height=H, resizable=False, frameless=True, easy_drag=False, background_color='#070304')
    api._ventana = ventana

    # la imagen de "cargando" que pone el .exe mientras se desempaca: se quita cuando la página ya cargó
    def sin_splash():
        try:
            import pyi_splash
            pyi_splash.close()
        except Exception:  # noqa: BLE001
            pass
    ventana.events.loaded += sin_splash
    opciones = dict(gui='edgechromium', http_server=True, private_mode=False, storage_path=os.path.join(ins.carpeta_juego(), 'webview'))
    try:
        if carpeta_prueba:
            webview.start(prueba, (carpeta_prueba, ventana), **opciones)
        else:
            webview.start(**opciones)
    except Exception:  # noqa: BLE001
        if os.name == 'nt':
            sin_webview2()
        raise


if __name__ == '__main__':
    main()
