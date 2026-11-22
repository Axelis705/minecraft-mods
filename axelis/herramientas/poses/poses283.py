import sys
sys.path.insert(0, __import__('os').path.join(__import__('os').path.dirname(__import__('os').path.abspath(__file__)), '..'))
from pose283 import *
D = 'agarre'
POSES['espada'] = [
 ('reposo', dict(brazo_der=[0.1,-0.95,0.3], arma_der=[0,0,1], brazo_izq=[-0.1,-1,0.05], arma_izq=[0,0,1])),
 ('prepara', dict(giro=0.55, brazo_der=[0.35,0.85,0.1], arma_der=[0.15,0.2,-1], brazo_izq=[-0.3,-0.4,0.86], arma_izq=[0,0,1], pierna_izq=-0.25, pierna_der=0.2, incl=-4)),
 ('golpe', dict(giro=-0.5, brazo_der=[-0.35,-0.15,0.92], arma_der=[-0.9,-0.3,-0.3], brazo_izq=[-0.2,-0.9,-0.3], arma_izq=[0,0,1], pierna_izq=-0.45, pierna_der=0.35, incl=10, baja=0.02, avance=0.12)),
 ('sigue', dict(giro=-0.62, brazo_der=[-0.6,-0.35,0.72], arma_der=[-0.55,-0.4,-0.73], brazo_izq=[-0.2,-0.9,-0.3], arma_izq=[0,0,1], pierna_izq=-0.4, pierna_der=0.3, incl=8, baja=0.02, avance=0.1)),
]
POSES['hacha'] = [
 ('reposo', dict(brazo_der=[0.15,-0.6,0.8], arma_der=[0,0.8,0.6], brazo_izq=D)),
 ('prepara', dict(giro=0.4, brazo_der=[0.3,0.9,-0.3], arma_der=[0,-0.3,-0.95], brazo_izq=D, pierna_izq=-0.3, pierna_der=0.25, incl=-8)),
 ('golpe', dict(giro=-0.35, brazo_der=[-0.1,0.1,0.99], arma_der=[0,-0.99,0.1], brazo_izq=D, pierna_izq=-0.55, pierna_der=0.45, incl=20, baja=0.08, avance=0.15)),
 ('sigue', dict(giro=-0.42, brazo_der=[-0.2,-0.25,0.95], arma_der=[0,-0.95,-0.25], brazo_izq=D, pierna_izq=-0.5, pierna_der=0.4, incl=16, baja=0.06, avance=0.12)),
]
POSES['martillo'] = [
 ('reposo', dict(brazo_der=[0.15,-0.6,0.8], arma_der=[0,0.8,0.6], brazo_izq=D)),
 ('prepara', dict(giro=0.18, brazo_der=[0.05,0.98,0.15], arma_der=[0,-0.15,-0.99], brazo_izq=D, pierna_izq=-0.35, pierna_der=0.3, incl=-12, avance=-0.02)),
 ('golpe', dict(giro=-0.08, brazo_der=[0,0.05,1], arma_der=[0,-1,0.05], brazo_izq=D, pierna_izq=-0.7, pierna_der=0.55, incl=26, baja=0.16, avance=0.1)),
 ('sigue', dict(giro=-0.1, brazo_der=[0,-0.15,0.99], arma_der=[0,-0.99,-0.15], brazo_izq=D, pierna_izq=-0.62, pierna_der=0.5, incl=22, baja=0.12, avance=0.08)),
]
POSES['lanza'] = [
 ('reposo', dict(brazo_der=[0.15,-0.6,0.8], arma_der=[0,0.8,0.6], brazo_izq=D)),
 ('prepara', dict(giro=0.45, brazo_der=[0.15,-0.95,-0.28], arma_der=[0,-0.28,0.96], brazo_izq=D, pierna_izq=-0.2, pierna_der=0.25, incl=-5, avance=-0.05)),
 ('golpe', dict(giro=-0.3, brazo_der=[0,-0.94,0.34], arma_der=[0,0.34,0.94], brazo_izq=D, pierna_izq=-0.75, pierna_der=0.55, incl=16, baja=0.05, avance=0.28)),
 ('sigue', dict(giro=-0.25, brazo_der=[0,-0.9,0.42], arma_der=[0,0.42,0.9], brazo_izq=D, pierna_izq=-0.65, pierna_der=0.5, incl=13, baja=0.04, avance=0.22)),
]
POSES['dagas'] = [
 ('reposo', dict(brazo_der=[0.1,-0.95,0.3], arma_der=[0,0,1], brazo_izq=[-0.1,-0.95,0.3], arma_izq=[0,0,1])),
 ('prepara', dict(giro=0.3, brazo_der=[0.1,0.6,0.79], arma_der=[0,-0.79,0.6], brazo_izq=[-0.2,-0.5,0.85], arma_izq=[0,0.85,0.5], pierna_izq=-0.2, pierna_der=0.2, incl=2)),
 ('golpe', dict(giro=-0.4, brazo_der=[0,-0.5,0.87], arma_der=[0,-0.87,-0.5], brazo_izq=[-0.2,-0.5,0.85], arma_izq=[0,0.85,0.5], pierna_izq=-0.4, pierna_der=0.3, incl=9, baja=0.02, avance=0.12)),
 ('sigue', dict(giro=-0.3, brazo_der=[0,-0.6,0.8], arma_der=[0,-0.8,-0.6], brazo_izq=[-0.2,-0.5,0.85], arma_izq=[0,0.85,0.5], pierna_izq=-0.35, pierna_der=0.25, incl=6, baja=0.02, avance=0.08)),
]
sheet(POSES, sys.argv[1])
