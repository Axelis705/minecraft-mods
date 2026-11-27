#!/usr/bin/env python3
"""Textos de la tanda del 27-nov (pedidos de Axelis después de probar): reinicio, caja de música, adornos en criaturas, Ramiro...
Uso: python3 herramientas/lang_extras.py"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lang_cosas import aplicar  # noqa: E402

T = {
    'navidad.reinicio.libro': ('Tu Libro de Navidad volvió a empezar: todas las tareas desde cero.',
                               'Your Christmas Book started over: every task from scratch.'),
    'navidad.reinicio.santa': ('Santa vuelve a pasar hoy: sal a verlo. Tus calcetines se llenan otra vez y puedes mandar otra carta.',
                               'Santa will fly by again today: go outside. Your stockings fill up again and you can send another letter.'),
    'navidad.reinicio.listo': ('Reiniciado (%s) para %s jugador(es)', 'Reset (%s) for %s player(s)'),
    'navidad.regalo.ahora_caja': ('Ahora los regalos son cajas: se convierte en una caja de regalo envuelta.',
                                  'Gifts are boxes now: this turns into a wrapped gift box.'),
    'navidad.sub.lanzanieves_impacto': ('Revienta la bola gigante', 'Giant snowball bursts'),
    'navidad.sub.lanzanieves_aspira': ('El lanzanieves aspira nieve', 'Snow launcher vacuums snow'),
    'navidad.sub.lanzanieves_vacio': ('El lanzanieves está vacío', 'Snow launcher is empty'),
    'block.navidad.caja_musica': ('Caja de música navideña', 'Christmas music box'),
    'navidad.caja_musica.ayuda': ('Clic derecho: toca un villancico (otro clic: el siguiente). Agachado: la cierras.',
                                  'Right-click: plays a carol (click again: the next one). Sneak: close it.'),
    'navidad.caja_musica.suena': ('♪ Suena: %s', '♪ Now playing: %s'),
    'navidad.cancion.0': ('Cascabeles (Jingle Bells)', 'Jingle Bells'),
    'navidad.cancion.1': ('Te deseamos feliz Navidad', 'We Wish You a Merry Christmas'),
    'navidad.cancion.2': ('Noche de paz', 'Silent Night'),
    'navidad.cancion.3': ('Adornen los salones', 'Deck the Halls'),
    'navidad.sub.caja_musica': ('Toca una caja de música', 'Music box plays'),
}

if __name__ == '__main__':
    aplicar(T)
    print(len(T), 'textos')
