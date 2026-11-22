# Etapa 287 — El volcán: bajada limpia, oleadas que abren el paso y el gran despertar; celdas de la Fortaleza

Lo que Axelis encontró jugando en el volcán: el cartel en mitad del camino, un «pasillo» donde todo estorba, que la sala de las
oleadas se pudiera saltar y que el volcán estuviera quieto. Y en la Fortaleza: celdas a medio hacer y salas secretas que se notaban.

## La bajada al lago (`DesignVolcan.jumpPier`)
- El camino en espiral ya no termina en cualquier sitio: al llegar arriba sigue **llano por el borde del cráter** hasta el lado este,
  donde todo queda recto con los bloques.
- Desde allí baja una **escalera recta de 3 de ancho** (con hombros de 1 a cada lado) hasta un rellano y el **muelle rojo**, que ahora
  también es de 3 de ancho y entra 4 bloques en el lago.
- **Nada en medio**: los faroles de almas van sobre postecitos a 3 bloques del centro; el **arco de los cultistas** deja libre el paso
  (postes a los lados y viga a 5 de altura, con su farol colgado).
- El **cartel** «Salto de fe» cuelga del poste izquierdo del arco, a la altura de los ojos, mirando a quien baja. Los estandartes
  cuelgan de los postes.
- Las fogatas de señal se movieron para que ninguna quede junto a la bajada.
- **Revisión automática** (`DesignVolcan.revisarBajada`, también en la prueba del servidor): por los 3 carriles, desde el final del
  camino hasta la punta del muelle, hay 2 bloques libres sobre el suelo, el suelo baja como mucho 1 por paso y con escalón, y no hay
  fogatas ni faroles a menos de 2 del paso.

## Las oleadas abren el paso
- La salida de la sala de erupciones (hacia la Gran Salamandra) tiene una **reja nueva (canal 3)**. Solo se abre al **vencer las tres
  oleadas del Altar Maldito** de esa misma sala.
- Al tocar la reja: «La reja no cede. La guarda el Altar Maldito de esta sala: vence sus tres oleadas.» En la sala, con la reja cerrada,
  sale también una pista.
- Al vencer el altar: «¡Con el altar vencido, la reja de la sala se abre!» (cualquier altar abre las rejas del canal 3 que tenga a
  menos de 30 bloques).
- **Al reiniciar el volcán** (10 minutos sin nadie) la reja se vuelve a cerrar y el altar queda listo otra vez, aunque estuviera
  descansando: nunca se queda nadie encerrado esperando un día.

## El gran despertar
- La primera vez que cada jugador **ve** un volcán (lo tiene delante, a menos de 230 bloques y sin nada que tape la cima del cráter),
  el volcán despierta: **tres erupciones seguidas** y el título «¡El volcán despierta! — Algo late bajo el cráter...».
- Se apunta por jugador (sobrevive a la muerte). Si varios lo ven a la vez, no se encadenan más erupciones.
- Las erupciones normales siguen como siempre (cada uno o dos minutos con alguien cerca).

## Fortaleza del Archimago
- **Celdas de la cripta cerradas del todo**: había un hueco en la esquina de los barrotes de la celda de Teo y la celda del norte no
  tenía frente ni puerta. Ahora tienen barrotes al norte y al sur hasta el techo, y la entrada de la del norte está tapiada con
  barrotes (es una celda vacía, de adorno).
- **Salas secretas**: el tapiz que esconde cada pasadizo colgaba al fondo de un hueco en la pared y se notaba que detrás había algo.
  Ahora va pegado a la pared del salón, igual que los demás tapices.
- `RevisarPlanos` comprueba que **la celda de cada preso está cerrada** (sin cruzar puertas de hierro ni barrotes, tampoco por un hueco
  en diagonal). Revisadas todas las estructuras.

## Pruebas
- **Servidor** (`mundos prueba etapa287`, sobre el volcán construido en el CI): la bajada del plano; la reja cerrada en su sitio; no se
  abre durante las oleadas y sí al terminarlas; al reiniciar el volcán se cierra y el altar queda listo; mirando al cráter se ve, de
  espaldas no y con una piedra delante tampoco; se apunta que lo vio y despierta.
- Comprobaciones nuevas del CI: la reja del altar cerrada, el arco y el cartel en su sitio nuevo.
- **Cliente** (`solo=volcan`): la primera vista (fotos 30b y 30c), la bajada libre con el arco y el cartel (foto 33b) y la reja de la
  sala de erupciones cerrada. También se arregló la prueba de la Piedra de regreso (ahora pide confirmar).
