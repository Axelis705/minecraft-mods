"""La parte del launcher que hace el trabajo (sin ventanas, así se puede probar sola):

1. lee el manifiesto (qué versión de Forge, qué mods, a qué servidor) desde GitHub
2. busca Java (el que trae el launcher de Minecraft; si no hay, baja uno)
3. instala Forge 1.20.1 en la carpeta de Minecraft (con el instalador oficial de Forge, sin ventanas)
4. baja los mods a una carpeta propia del evento (%APPDATA%\\.coleccionista) y escribe la configuración
5. crea o actualiza el perfil "El Coleccionista" en el launcher oficial de Minecraft (con la RAM elegida)
6. abre el launcher oficial (ahí cada quien entra con su cuenta de Microsoft y presiona Jugar)

Todo lo que descarga lo revisa (tamaño y sha1 si el manifiesto lo trae). Para las pruebas, COLECCIONISTA_APPDATA cambia la carpeta.
"""
import base64
import ctypes
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile

REPO = 'https://raw.githubusercontent.com/Axelis705/minecraft-mods/main/launcher/'
URL_MANIFIESTO = REPO + 'manifiesto.json'
URL_USUARIOS = REPO + 'usuarios.json'
JAVA_URL = 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jre/hotspot/normal/eclipse'
PERFIL_ID = 'el_coleccionista'
AGENTE = 'ElColeccionista-Launcher/1.0'


def base_datos():
    return os.environ.get('COLECCIONISTA_APPDATA') or os.environ.get('APPDATA') or os.path.expanduser('~')


def carpeta_minecraft():
    return os.path.join(base_datos(), '.minecraft')


def carpeta_juego():
    return os.path.join(base_datos(), '.coleccionista')


def recurso(nombre):
    """Un archivo que va dentro del .exe (PyInstaller lo pone en sys._MEIPASS) o junto al script."""
    base = getattr(sys, '_MEIPASS', os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, nombre)


class Fallo(Exception):
    """Un error con un mensaje para la persona (en español sencillo)."""


# ---------------------------------------------------------------------------------------------------- red

def bajar_texto(url, respaldo=None, intentos=3):
    ultimo = None
    for _ in range(intentos):
        try:
            req = urllib.request.Request(url + ('&' if '?' in url else '?') + 't=' + str(int(datetime.datetime.now().timestamp())),
                                         headers={'User-Agent': AGENTE, 'Cache-Control': 'no-cache'})
            with urllib.request.urlopen(req, timeout=20) as r:
                return r.read().decode('utf-8')
        except Exception as e:  # noqa: BLE001
            ultimo = e
    if respaldo and os.path.exists(respaldo):
        with open(respaldo, encoding='utf-8') as f:
            return f.read()
    raise Fallo('No pude conectarme a internet para revisar las actualizaciones (%s).' % ultimo)


def sha1_de(ruta):
    h = hashlib.sha1()
    with open(ruta, 'rb') as f:
        for b in iter(lambda: f.read(1 << 16), b''):
            h.update(b)
    return h.hexdigest()


def bajar(url, destino, sha1=None, avance=None, intentos=3):
    """Descarga a un archivo temporal y lo mueve al final (si se corta, no queda un archivo a medias)."""
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    ultimo = None
    for _ in range(intentos):
        tmp = destino + '.parte'
        try:
            req = urllib.request.Request(url, headers={'User-Agent': AGENTE})
            with urllib.request.urlopen(req, timeout=60) as r, open(tmp, 'wb') as f:
                total = int(r.headers.get('Content-Length') or 0)
                hecho = 0
                while True:
                    b = r.read(1 << 16)
                    if not b:
                        break
                    f.write(b)
                    hecho += len(b)
                    if avance and total:
                        avance(hecho / total)
            if sha1 and sha1_de(tmp) != sha1.lower():
                raise Fallo('El archivo %s llegó dañado.' % os.path.basename(destino))
            os.replace(tmp, destino)
            return
        except Exception as e:  # noqa: BLE001
            ultimo = e
            if os.path.exists(tmp):
                os.remove(tmp)
    raise Fallo('No pude descargar %s (%s).' % (os.path.basename(destino), ultimo))


# ---------------------------------------------------------------------------------------------------- manifiesto y usuarios

def manifiesto():
    return json.loads(bajar_texto(URL_MANIFIESTO, respaldo=recurso('manifiesto.json')))


def usuarios():
    ruta = os.environ.get('COLECCIONISTA_USUARIOS')
    if ruta:
        with open(ruta, encoding='utf-8') as f:
            return json.load(f)
    return json.loads(bajar_texto(URL_USUARIOS, respaldo=recurso('usuarios.json')))


def revisar_clave(usuario, clave, datos=None):
    """¿El usuario existe y la contraseña es la suya? (los nombres no distinguen mayúsculas)."""
    datos = datos or usuarios()
    u = datos.get('usuarios', {}).get(usuario.strip().lower())
    if not u:
        return False
    it = int(datos.get('iteraciones', 200000))
    h = hashlib.pbkdf2_hmac('sha256', clave.encode('utf-8'), bytes.fromhex(u['sal']), it).hex()
    return h == u['hash']


# ---------------------------------------------------------------------------------------------------- memoria

def ram_total_gb():
    """La memoria de la computadora en GB (en Windows; en otro sistema, 16 para poder probar)."""
    if os.name != 'nt':
        return float(os.environ.get('COLECCIONISTA_RAM', '16'))

    class Estado(ctypes.Structure):
        _fields_ = [('dwLength', ctypes.c_ulong), ('dwMemoryLoad', ctypes.c_ulong), ('ullTotalPhys', ctypes.c_ulonglong),
                    ('ullAvailPhys', ctypes.c_ulonglong), ('ullTotalPageFile', ctypes.c_ulonglong),
                    ('ullAvailPageFile', ctypes.c_ulonglong), ('ullTotalVirtual', ctypes.c_ulonglong),
                    ('ullAvailVirtual', ctypes.c_ulonglong), ('sullAvailExtendedVirtual', ctypes.c_ulonglong)]

    e = Estado()
    e.dwLength = ctypes.sizeof(Estado)
    ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(e))
    return e.ullTotalPhys / (1024 ** 3)


def ram_posibles():
    """Las opciones (4, 6, 8, 10 GB) y si cada una se puede: hay que dejarle al menos 2 GB a Windows."""
    total = ram_total_gb()
    return [(g, g <= total - 2 + 0.25) for g in (4, 6, 8, 10)]


# ---------------------------------------------------------------------------------------------------- java

def _version_java(exe):
    try:
        r = subprocess.run([exe, '-version'], capture_output=True, text=True, timeout=20, **_sin_ventana())
        txt = r.stderr + r.stdout
        import re
        m = re.search(r'version "(\d+)(?:\.(\d+))?', txt)
        if not m:
            return 0
        v = int(m.group(1))
        return int(m.group(2) or 0) if v == 1 else v
    except Exception:  # noqa: BLE001
        return 0


def _sin_ventana():
    if os.name == 'nt':
        return {'creationflags': 0x08000000}  # CREATE_NO_WINDOW
    return {}


def buscar_java():
    """Un Java 17 o más nuevo: el que bajó el launcher de Minecraft, el que ya bajamos nosotros, o el de la computadora."""
    exe = 'java.exe' if os.name == 'nt' else 'java'
    candidatos = []
    raices = [os.path.join(carpeta_minecraft(), 'runtime')]
    local = os.environ.get('LOCALAPPDATA')
    if local:
        raices.append(os.path.join(local, 'Packages', 'Microsoft.4297127D64EC6_8wekyb3d8bbwe', 'LocalCache', 'Local', 'runtime'))
    for raiz in raices:
        for nombre in ('java-runtime-gamma', 'java-runtime-delta', 'java-runtime-beta'):
            d = os.path.join(raiz, nombre)
            if os.path.isdir(d):
                for base, _dirs, archivos in os.walk(d):
                    if exe in archivos and base.endswith('bin'):
                        candidatos.append(os.path.join(base, exe))
    propio = os.path.join(carpeta_juego(), 'java')
    if os.path.isdir(propio):
        for base, _dirs, archivos in os.walk(propio):
            if exe in archivos and base.endswith('bin'):
                candidatos.append(os.path.join(base, exe))
    if os.environ.get('JAVA_HOME'):
        candidatos.append(os.path.join(os.environ['JAVA_HOME'], 'bin', exe))
    w = shutil.which('java')
    if w:
        candidatos.append(w)
    for c in candidatos:
        if os.path.exists(c) and _version_java(c) >= 17:
            return c
    return None


def bajar_java(avance=None):
    if os.name != 'nt':
        raise Fallo('No encontré Java 17.')
    destino = os.path.join(carpeta_juego(), 'java')
    zipf = os.path.join(carpeta_juego(), 'java.zip')
    bajar(JAVA_URL, zipf, avance=avance)
    if os.path.isdir(destino):
        shutil.rmtree(destino, ignore_errors=True)
    with zipfile.ZipFile(zipf) as z:
        z.extractall(destino)
    os.remove(zipf)
    j = buscar_java()
    if not j:
        raise Fallo('Bajé Java pero no funciona.')
    return j


# ---------------------------------------------------------------------------------------------------- forge

def forge_instalado(version):
    return os.path.exists(os.path.join(carpeta_minecraft(), 'versions', version, version + '.json'))


def instalar_forge(man, java, avance=None):
    version = man['forge']['version']
    if forge_instalado(version):
        return False
    mc = carpeta_minecraft()
    if not os.path.exists(os.path.join(mc, 'launcher_profiles.json')):
        raise Fallo('Primero abre una vez el launcher de Minecraft (el oficial) e inicia sesión con tu cuenta; luego vuelve aquí.')
    inst = os.path.join(tempfile.gettempdir(), 'forge-installer-%s.jar' % version)
    bajar(man['forge']['instalador'], inst, sha1=man['forge'].get('sha1'), avance=avance)
    r = subprocess.run([java, '-jar', inst, '--installClient', mc], capture_output=True, text=True, timeout=1800,
                       cwd=tempfile.gettempdir(), **_sin_ventana())
    registro = os.path.join(carpeta_juego(), 'forge-instalador.log')
    os.makedirs(carpeta_juego(), exist_ok=True)
    with open(registro, 'w', encoding='utf-8') as f:
        f.write(r.stdout + '\n' + r.stderr)
    if not forge_instalado(version):
        raise Fallo('No se pudo instalar Forge (el detalle quedó en %s).' % registro)
    return True


# ---------------------------------------------------------------------------------------------------- mods y configuración

def instalar_mods(man, avance=None):
    """Baja los mods que faltan o cambiaron y quita los que el evento ya no usa (solo los que puso este launcher)."""
    mods = os.path.join(carpeta_juego(), 'mods')
    os.makedirs(mods, exist_ok=True)
    estado_f = os.path.join(carpeta_juego(), 'launcher-mods.json')
    antes = []
    if os.path.exists(estado_f):
        with open(estado_f, encoding='utf-8') as f:
            antes = json.load(f)
    lista = man.get('mods', [])
    nuevos = [m['archivo'] for m in lista]
    for viejo in antes:
        if viejo not in nuevos and os.path.exists(os.path.join(mods, viejo)):
            os.remove(os.path.join(mods, viejo))
    bajados = 0
    for i, m in enumerate(lista):
        ruta = os.path.join(mods, m['archivo'])
        bien = os.path.exists(ruta) and (not m.get('sha1') or sha1_de(ruta) == m['sha1'].lower())
        if not bien:
            def parte(k, i=i):
                if avance:
                    avance((i + k) / max(1, len(lista)), m['archivo'])
            bajar(m['url'], ruta, sha1=m.get('sha1'), avance=parte)
            bajados += 1
        if avance:
            avance((i + 1) / max(1, len(lista)), m['archivo'])
    with open(estado_f, 'w', encoding='utf-8') as f:
        json.dump(nuevos, f)
    return bajados


def escribir_config(man):
    juego = carpeta_juego()
    os.makedirs(os.path.join(juego, 'config'), exist_ok=True)
    # a dónde entra "Jugar" en la pantalla del Coleccionista
    with open(os.path.join(juego, 'config', 'coleccionista-inicio.properties'), 'w', encoding='utf-8') as f:
        f.write('# Lo escribe el launcher del Coleccionista\n')
        f.write('servidor=%s\n' % man.get('servidor', ''))
        f.write('nombre=%s\n' % man.get('nombre', 'El Coleccionista'))
        f.write('activo=true\n')
    # las opciones de Minecraft solo la primera vez (luego cada quien las cambia en Configurar)
    opciones = os.path.join(juego, 'options.txt')
    if not os.path.exists(opciones):
        with open(opciones, 'w', encoding='utf-8') as f:
            for k, v in man.get('opciones', {}).items():
                f.write('%s:%s\n' % (k, v))


# ---------------------------------------------------------------------------------------------------- perfil del launcher oficial

def _icono_perfil():
    ruta = recurso(os.path.join('arte', 'perfil.png'))
    if not os.path.exists(ruta):
        return 'Furnace'
    with open(ruta, 'rb') as f:
        return 'data:image/png;base64,' + base64.b64encode(f.read()).decode('ascii')


def poner_perfil(man, ram):
    """Crea o actualiza el perfil en launcher_profiles.json (y en el de la versión de la Tienda de Microsoft, si existe)."""
    mc = carpeta_minecraft()
    ahora = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.000Z')
    minimo = max(2, ram // 2)
    args = ('-Xmx%dG -Xms%dG -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 '
            '-XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M' % (ram, minimo))
    perfil = {
        'name': man.get('nombre', 'El Coleccionista'),
        'type': 'custom',
        'lastVersionId': man['forge']['version'],
        'gameDir': carpeta_juego(),
        'javaArgs': args,
        'icon': _icono_perfil(),
        'lastUsed': ahora,
        'created': ahora,
    }
    escritos = 0
    for nombre in ('launcher_profiles.json', 'launcher_profiles_microsoft_store.json'):
        ruta = os.path.join(mc, nombre)
        if not os.path.exists(ruta):
            continue
        with open(ruta, encoding='utf-8') as f:
            datos = json.load(f)
        perfiles = datos.setdefault('profiles', {})
        if PERFIL_ID in perfiles and 'created' in perfiles[PERFIL_ID]:
            perfil['created'] = perfiles[PERFIL_ID]['created']
        perfiles[PERFIL_ID] = dict(perfil)
        # el instalador de Forge agrega su propio perfil "forge" (con la carpeta normal de Minecraft, sin los mods del evento): se
        # quita para que nadie juegue en ese por error
        for k in [k for k, v in perfiles.items() if k != PERFIL_ID and v.get('lastVersionId') == man['forge']['version']
                  and not v.get('gameDir') and (k == 'forge' or v.get('name') == 'forge')]:
            del perfiles[k]
        tmp = ruta + '.tmp'
        with open(tmp, 'w', encoding='utf-8') as f:
            json.dump(datos, f, indent=2)
        os.replace(tmp, ruta)
        escritos += 1
    if not escritos:
        raise Fallo('No encontré el launcher de Minecraft. Ábrelo una vez e inicia sesión; luego vuelve aquí.')
    return escritos


# ---------------------------------------------------------------------------------------------------- abrir el launcher oficial

def abrir_carpeta():
    """Abre la carpeta del evento (ahí están los mods, la configuración y los registros del juego)."""
    os.makedirs(carpeta_juego(), exist_ok=True)
    if os.name == 'nt':
        os.startfile(carpeta_juego())  # noqa: S606


def abrir_launcher():
    if os.name != 'nt':
        return False
    candidatos = [
        os.path.join(os.environ.get('ProgramFiles(x86)', r'C:\Program Files (x86)'), 'Minecraft Launcher', 'MinecraftLauncher.exe'),
        os.path.join(os.environ.get('ProgramFiles', r'C:\Program Files'), 'Minecraft Launcher', 'MinecraftLauncher.exe'),
        r'C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe',
    ]
    for c in candidatos:
        if os.path.exists(c):
            subprocess.Popen([c], **_sin_ventana())
            return True
    # la versión de la Tienda de Microsoft (o la de Xbox)
    try:
        subprocess.Popen(['explorer.exe', r'shell:AppsFolder\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft'])
        return True
    except Exception:  # noqa: BLE001
        return False


def juego_abierto_desde(t0):
    """¿Ya arrancó el juego del evento después de {@code t0}? (el juego escribe su registro en su carpeta en cuanto abre)."""
    for nombre in ('latest.log', 'debug.log'):
        ruta = os.path.join(carpeta_juego(), 'logs', nombre)
        try:
            if os.path.getmtime(ruta) > t0:
                return True
        except OSError:
            pass
    return False


# ---------------------------------------------------------------------------------------------------- todo junto

def preparar(ram, avance=lambda k, txt: None, abrir=True):
    """Hace todo; {@code avance(fracción 0..1, texto)} para la barra. Devuelve un resumen."""
    avance(0.02, 'Revisando qué hay de nuevo…')
    man = manifiesto()
    avance(0.06, 'Buscando Java…')
    java = buscar_java()
    if not java:
        avance(0.08, 'Bajando Java (solo la primera vez)…')
        java = bajar_java(lambda k: avance(0.08 + 0.12 * k, 'Bajando Java (solo la primera vez)…'))
    nuevo_forge = False
    if not forge_instalado(man['forge']['version']):
        avance(0.2, 'Instalando Forge (solo la primera vez, tarda un poco)…')
        nuevo_forge = instalar_forge(man, java, lambda k: avance(0.2 + 0.1 * k, 'Bajando Forge…'))
    avance(0.45, 'Revisando los mods del evento…')
    bajados = instalar_mods(man, lambda k, nombre='': avance(0.45 + 0.45 * k, 'Mods: %s' % nombre))
    escribir_config(man)
    avance(0.93, 'Preparando tu perfil en el launcher de Minecraft…')
    poner_perfil(man, ram)
    abierto = abrir_launcher() if abrir else False
    avance(1.0, 'Listo')
    return {'forge': nuevo_forge, 'mods': bajados, 'abierto': abierto, 'java': java}
