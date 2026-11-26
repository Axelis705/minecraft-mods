# El launcher del Coleccionista

Un `.exe` para Windows que les das a tus amigos. Al abrirlo:

1. Aparece el escenario con **el Coleccionista en 3D** (respira, ladea la cabeza de golpe, te sigue con la mirada, se ríe si vas a
   cerrar, a veces se va la luz y solo se le ven los ojos…), con su vals de caja de música y sonidos de terror. Al abrir dice
   *"Vaya, vaya… ¿Qué tenemos aquí?"*.
2. Entran con el **usuario y contraseña** que tú les das.
3. Eligen la **memoria** (4, 6, 8 o 10 GB; lo que su computadora no aguanta sale tachado).
4. **Jugar**: instala lo que falte (Java si hace falta, Forge 1.20.1, el mod del evento y Simple Voice Chat) en su propia carpeta
   (`%APPDATA%\.coleccionista`, no toca su Minecraft normal), crea el perfil **"El Coleccionista"** en el launcher oficial de Minecraft
   y lo abre. Ahí entran con su cuenta de Microsoft, eligen el perfil y presionan Jugar. En el juego, la pantalla del Coleccionista los
   mete directo al servidor.

## Dónde está el .exe

En la rama `launcher-exe` del repositorio (`ElColeccionista.exe`), junto con las fotos de la última prueba.

## Lo que hace Claude (o tú) para mantenerlo

- **Amigos**: `python3 launcher/usuarios.py agregar <usuario> <contraseña>` (y `quitar`, `lista`, `probar`). Se sube `usuarios.json`.
  Solo se guarda la huella de la contraseña, nunca la contraseña. El repositorio es público: los nombres de usuario se ven.
- **Servidor y mods**: `python3 launcher/manifiesto.py --servidor <dirección>`; fija el jar del evento en la rama `evento-jars`
  (para que todos bajen justo el que tiene el servidor). Se sube `manifiesto.json`. El launcher lo lee de GitHub cada vez, así que no
  hay que mandar otro `.exe`.
- **Arte y sonidos**: `arte/hacer_arte.py` (ícono), `arte/sonidos_launcher.py` (sonidos de terror); la escena está en `web/`.
- **Prueba**: el flujo `.github/workflows/launcher.yml` arma el `.exe` en Windows, lo abre, entra, instala de verdad en una carpeta de
  prueba y saca fotos.
