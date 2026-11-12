#!/usr/bin/env python3
"""Textos de la Guía del Casino (español e inglés). Cada página: título y cuerpo (\\n = nuevo párrafo)."""
import json
import os

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "lang")

ES = [
    ("Guía del Casino", "Todo lo que necesitas para jugar, ganar (o perder con estilo) y decorar tu propio casino.\nPasa las páginas con las flechas o con las teclas ← y →. En la página 2 está el índice: haz clic en un tema para ir directo."),
    ("Índice", ""),
    ("Fichas y cajero", "En el casino se juega con fichas de 1, 5, 25, 100 y 500.\nEn el CAJERO compras fichas: 1 esmeralda = 10 fichas, 1 diamante = 80 fichas. También puedes canjear tus fichas de vuelta por esmeraldas o diamantes.\nLas fichas se cambian solas: si apuestas 30 y solo tienes una de 100, el juego te da el cambio."),
    ("Ruleta de la suerte", "Un giro GRATIS cada día (se reinicia a medianoche, hora del servidor).\nHaz clic en la rueda: puede tocarte fichas, objetos, experiencia, un premio misterioso o el PREMIO GORDO que está en el podio.\nLos operadores cambian el premio gordo agachados con el objeto en la mano."),
    ("Tragamonedas", "Elige tu apuesta y tira de la palanca. Hay 5 líneas: tres filas y dos diagonales.\nTres iguales pagan: cereza x6, limón x12, campana x32, gema x80, estrella x200, siete x800. Dos cerezas desde la izquierda pagan x2.\nTres CORONAS pagan x120 y además el PREMIO GORDO acumulado, que crece con cada tirada."),
    ("Plinko", "Elige apuesta y riesgo, y suelta la bola. Rebota entre 10 filas de clavijas y cae en un casillero.\nRiesgo bajo: pagos parejos (hasta x8). Riesgo medio: hasta x22. Riesgo alto: casi todo paga poco, pero las orillas pagan x55.\nEn promedio el plinko devuelve entre 95% y 97% de lo apostado."),
    ("Blackjack", "Hasta 5 jugadores contra el crupier. Gana quien se acerque más a 21 sin pasarse.\nLas figuras valen 10 y el as vale 1 u 11. Pide carta, plántate, DOBLA (una carta más y apuesta doble) o SEPARA un par en dos manos.\nBlackjack (as + 10 en las primeras dos) paga 3 a 2. Si el crupier muestra un as puedes tomar SEGURO. El crupier pide hasta 17."),
    ("Ruleta europea", "37 números (0 al 36). Pon fichas en el paño y espera la bola.\nPagos: número solo 35:1, caballo (2) 17:1, calle (3) 11:1, cuadro (4) 8:1, línea (6) 5:1, docena o columna 2:1, rojo/negro, par/impar o 1-18/19-36 1:1.\nTambién hay apuestas de pista: vecinos del cero, tercio y huérfanos. Shift + clic pone 5 fichas de golpe."),
    ("Casino Hold'em", "Póker contra el crupier. Pon tu ANTE: recibes 2 cartas y salen 3 comunes.\nDecide: RETIRARTE (pierdes el ante) o IGUALAR pagando el doble del ante. Salen 2 cartas más y se comparan las mejores manos de 5.\nEl crupier califica con par de 4 o mejor. Si no califica, el ante paga 1:1. Si ganas, la igualación paga 1:1 y el ante paga por tu mano: escalera 1:1, color 2:1, full 3:1, póker 10:1, escalera de color 20:1, escalera real 100:1."),
    ("Rasca y gana", "Compra boletos en la máquina (10 fichas cada uno, de 1, 5 o 10 a la vez).\nRasca los 6 círculos con el mouse. Si salen 3 símbolos iguales ganas su premio: desde 10 hasta 5000 fichas.\nNo hace falta rascar todo: el boleto sabe si ganaste desde que lo compras."),
    ("El edificio del casino", "En llanuras, bosques, desiertos, sabanas, taigas y nieve puede aparecer un casino completo con todos los juegos y sus crupieres.\nLos operadores pueden construir uno con /casino build <x y z> [giro], regalar fichas con /casino chips <jugadores> <cantidad> y reiniciar la ruleta diaria con /casino resetwheel."),
    ("Muebles", "Sillas de casino, taburetes y sofás en 4 colores: haz clic derecho para SENTARTE (agachado colocas bloques encima).\nLos sofás se unen solos con los de al lado. La barra hace esquinas hacia adentro y hacia afuera como las escaleras.\nPon BEBIDAS en mesas y barras: con más en la mano se agregan hasta 4 vasos."),
    ("Luces y letreros", "LETRAS DE NEÓN: pon varias juntas y haz clic derecho para escribir con el teclado (salta sola a la siguiente letra). Elige entre 6 colores; el color se aplica a todo el letrero.\nLETREROS y PANTALLAS: clic derecho cambia el dibujo (agachado, hacia atrás).\nPOSTES CON CORDÓN: el cordón se tiende solo entre postes vecinos y nadie puede cruzarlo."),
    ("Ambiente", "La BOCINA DE JAZZ toca música de casino; clic derecho la prende o apaga. Aunque pongas varias, suena una sola pista que se oye más fuerte cerca.\nEl CANDELABRO de cristal ilumina como una lámpara de glowstone.\nPisos de alfombra en 4 diseños, mármol blanco y negro (bloque, escaleras y losas), panel de caoba, moldura dorada y paredes de terciopelo."),
    ("Juega con cabeza", "Las fichas son de mentira, pero las esmeraldas y los diamantes no.\nEn todos los juegos la casa gana un poquito a la larga: si vas perdiendo, levántate y ve a sentarte al bar.\n¡Buena suerte!"),
]

EN = [
    ("Casino Guide", "Everything you need to play, win (or lose in style) and decorate your own casino.\nTurn pages with the arrows or the ← and → keys. Page 2 has the contents: click a topic to jump there."),
    ("Contents", ""),
    ("Chips and Cashier", "Casino games use chips worth 1, 5, 25, 100 and 500.\nBuy chips at the CASHIER: 1 emerald = 10 chips, 1 diamond = 80 chips. You can also cash your chips back into emeralds or diamonds.\nChips make change by themselves: bet 30 with a single 100 chip and the game gives you the change."),
    ("Lucky Wheel", "One FREE spin every day (resets at midnight, server time).\nClick the wheel: you can win chips, items, experience, a mystery prize or the JACKPOT shown on the podium.\nOperators change the jackpot by sneaking with the item in hand."),
    ("Slot Machine", "Pick your bet and pull the lever. There are 5 lines: three rows and two diagonals.\nThree of a kind pays: cherry x6, lemon x12, bell x32, gem x80, star x200, seven x800. Two cherries from the left pay x2.\nThree CROWNS pay x120 plus the progressive JACKPOT, which grows with every spin."),
    ("Plinko", "Pick a bet and a risk level, then drop the ball. It bounces through 10 rows of pegs into a slot.\nLow risk: even payouts (up to x8). Medium risk: up to x22. High risk: most slots pay little, but the edges pay x55.\nOn average Plinko returns 95% to 97% of what you bet."),
    ("Blackjack", "Up to 5 players against the dealer. Get closer to 21 than the dealer without going over.\nFace cards are worth 10 and aces 1 or 11. HIT, STAND, DOUBLE (one more card, double bet) or SPLIT a pair into two hands.\nBlackjack (ace + 10 on the first two cards) pays 3 to 2. If the dealer shows an ace you can take INSURANCE. The dealer draws to 17."),
    ("European Roulette", "37 numbers (0 to 36). Place chips on the felt and wait for the ball.\nPayouts: straight 35:1, split 17:1, street 11:1, corner 8:1, six line 5:1, dozen or column 2:1, red/black, even/odd or 1-18/19-36 1:1.\nThere are also racetrack bets: neighbours of zero, tier and orphans. Shift + click places 5 chips at once."),
    ("Casino Hold'em", "Poker against the dealer. Place your ANTE: you get 2 cards and 3 community cards are dealt.\nChoose: FOLD (lose the ante) or CALL by paying twice the ante. Two more cards come and the best 5-card hands are compared.\nThe dealer qualifies with a pair of 4s or better. If not, the ante pays 1:1. If you win, the call pays 1:1 and the ante pays by your hand: straight 1:1, flush 2:1, full house 3:1, four of a kind 10:1, straight flush 20:1, royal flush 100:1."),
    ("Scratch Cards", "Buy tickets at the machine (10 chips each, 1, 5 or 10 at a time).\nScratch the 6 circles with your mouse. Three matching symbols win their prize: from 10 up to 5000 chips.\nNo need to scratch everything: the ticket knows if you won from the moment you buy it."),
    ("The Casino Building", "In plains, forests, deserts, savannas, taigas and snowy plains a full casino can appear, with every game and its dealers.\nOperators can build one with /casino build <x y z> [rotation], give chips with /casino chips <players> <amount> and reset the daily wheel with /casino resetwheel."),
    ("Furniture", "Casino chairs, bar stools and sofas in 4 colors: right-click to SIT (sneak to place blocks on them).\nSofas join with the ones next to them. The bar counter makes inner and outer corners just like stairs.\nPut DRINKS on tables and counters: use more on them to add up to 4 glasses."),
    ("Lights and Signs", "NEON LETTERS: place several in a row and right-click to type with your keyboard (it jumps to the next letter by itself). Pick from 6 colors; the color applies to the whole sign.\nSIGNS and SCREENS: right-click changes the design (sneak to go back).\nROPE POSTS: the velvet rope stretches between neighbouring posts by itself and nobody can cross it."),
    ("Atmosphere", "The JAZZ SPEAKER plays casino music; right-click turns it on or off. Even with several speakers you hear a single track, louder when you are close.\nThe crystal CHANDELIER lights up like a glowstone lamp.\nCarpet floors in 4 designs, white and black marble (block, stairs and slabs), mahogany panel, gold trim and velvet walls."),
    ("Play Smart", "Chips are pretend money, but emeralds and diamonds are not.\nIn every game the house wins a little in the long run: if you are losing, get up and go sit at the bar.\nGood luck!"),
]

EXTRA_ES = {"item.casino.casino_guide": "Guía del Casino", "casino.guide.page": "Página %s de %s", "casino.guide.prev": "Anterior",
            "casino.guide.next": "Siguiente", "casino.guide.given": "Te regalaron la Guía del Casino. ¡Léela para aprender a jugar!",
            "casino.guide.tooltip": "Clic derecho para leer las reglas de todos los juegos."}
EXTRA_EN = {"item.casino.casino_guide": "Casino Guide", "casino.guide.page": "Page %s of %s", "casino.guide.prev": "Previous",
            "casino.guide.next": "Next", "casino.guide.given": "You got the Casino Guide. Read it to learn how to play!",
            "casino.guide.tooltip": "Right-click to read the rules of every game."}


def entries(pages, extra):
    out = dict(extra)
    for i, (t, b) in enumerate(pages):
        out[f"casino.guide.p{i}.title"] = t
        out[f"casino.guide.p{i}.body"] = b
    return out


def main():
    assert len(ES) == len(EN)
    for lang, data in (("es_es", entries(ES, EXTRA_ES)), ("en_us", entries(EN, EXTRA_EN))):
        p = os.path.join(A, lang + ".json")
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update(data)
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print("guía:", len(ES), "páginas")


if __name__ == "__main__":
    main()
