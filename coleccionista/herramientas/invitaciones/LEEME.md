# Invitaciones del Coleccionista

Una imagen por amigo (1080x1350, para Discord): su skin en 3D de pie dentro de una vitrina con su placa de latón, el número de
invitación, "Ya tengo un lugar para ti en mi colección.", la fecha del evento y la A de Axelis como firma. En lo oscuro, a la
derecha de la vitrina, unos ojos rojos.

Uso:
```
node hacer.js lista.json salida/
```
`lista.json`: `[{"nombre": "Scardax", "skin": "/ruta/Scardax.png", "num": "07"}]`. La skin es el PNG de 64x64 (o 64x32 de las
viejas); los brazos delgados se detectan solos. Usa `../trailer/web3d/three.min.js` y Chromium de Playwright con SwiftShader.
Las skins de los amigos no se guardan en el repo.
