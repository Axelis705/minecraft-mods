#!/usr/bin/env python3
"""Textos del casino del Viejo Oeste (español e inglés). Luego: lang_variants.py y lang_build.py.

    python3 casino/herramientas/west_lang.py
"""
import json
import os

L = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "lang")

ES = {
    "block.casino.saloon_door": "Puerta de saloon",
    "block.casino.vip_gate": "Puertita VIP del saloon",
    "block.casino.player_piano": "Pianola",
    "block.casino.painted_letter": "Letra pintada",
    "block.casino.wanted_poster": "Cartel de \"Se busca\"",
    "subtitles.casino.music_piano": "La pianola toca ragtime",
    "subtitles.casino.saloon_door": "Rechina la puerta del saloon",
    "casino.west.gate_set": "La puertita ahora pide: %s",
    "casino.west.gate_ok": "Adelante, socio VIP.",
    "casino.west.gate_need_silver": "Solo con tarjeta VIP Plata en la mano o en el inventario (se compra en la caja).",
    "casino.west.gate_need_gold": "Solo con tarjeta VIP Oro en la mano o en el inventario (se compra en la caja).",
    "casino.npc.guard_west": "Sheriff",
    "casino.npc.bartender_west": "Cantinero",
    "casino.npc.blackjack_west": "Crupier del saloon",
    "casino.npc.wheel_west": "Anfitrión",
    "casino.npc.patron_64": "Vaquero Jed",
    "casino.npc.patron_65": "Billy el Pelirrubio",
    "casino.npc.patron_66": "Forastero de negro",
    "casino.npc.patron_67": "Viejo Hank",
    "casino.npc.patron_68": "Vaquera Rosie",
    "casino.npc.patron_69": "Jugador de cartas",
    "casino.npc.patron_70": "Señorita Lola",
    "casino.npc.patron_71": "Gambusino Pete",
    # sheriff
    "casino.tip.guard_west.0": "Arriba se juega fuerte, forastero. Solo con tarjeta VIP.",
    "casino.tip.guard_west.1": "Con la tarjeta Plata subes a la sala de cartas del primer piso.",
    "casino.tip.guard_west.2": "Con la tarjeta Oro también entras al palco del dueño.",
    "casino.tip.guard_west.3": "Las tarjetas se compran en la caja, junto a la entrada.",
    "casino.tip.guard_west.4": "Nada de armas en la mesa. En mi pueblo se apuesta limpio.",
    "casino.tip.guard_west.5": "Si oyes la campana al mediodía, ¡alguien se va a batir en duelo en la calle!",
    "casino.tip.guard_west.say_0": "Alto ahí. Arriba solo pasa quien trae tarjeta VIP: Plata 5.000, Oro 25.000.",
    "casino.tip.guard_west.say_1": "Adelante, socio. El palco del dueño es solo para tarjeta Oro.",
    "casino.tip.guard_west.say_2": "Bienvenido, patrón. El saloon entero es suyo.",
    # cantinero
    "casino.tip.bartender_west.0": "¿Un whisky para el polvo del camino?",
    "casino.tip.bartender_west.1": "Aquí la zarzaparrilla es tan buena como el whisky. Bueno, casi.",
    "casino.tip.bartender_west.2": "La pianola no descansa nunca. Ojalá yo tampoco.",
    "casino.tip.bartender_west.3": "Si se arma una pelea, escóndete detrás de la barra.",
    "casino.tip.bartender_west.4": "El crupier del 21 nunca ha perdido una camisa. Ni una.",
    "casino.tip.bartender_west.5": "Háblame y te sirvo el especial de la casa: trae suerte.",
    "casino.tip.bartender_west.6": "Los carteles de \"Se busca\" son de verdad... creo.",
    "casino.tip.bartender_west.7": "Arriba juegan los que traen el bolsillo lleno.",
    "casino.tip.bartender_west.8": "Una vez un vaquero ganó un rancho en una sola mano.",
    "casino.tip.bartender_west.9": "Las puertas del saloon rechinan desde 1880.",
    "casino.tip.bartender_west.say_0": "¡Ahí va un whisky de la casa! Con suerte de minero.",
    "casino.tip.bartender_west.say_1": "Ahí tienes, vaquero. ¡Que te sonría la baraja!",
    "casino.tip.bartender_west.say_2": "Este va por la casa. ¡Salud, forastero!",
    "casino.tip.bartender_west.say_3": "Despacio, vaquero. El próximo en un minuto.",
    # crupier
    "casino.tip.blackjack_west.0": "Acérquese, socio. Aquí el 21 se juega como en Tombstone.",
    "casino.tip.blackjack_west.1": "Haga su apuesta antes de que salga la primera carta.",
    "casino.tip.blackjack_west.2": "Con 21 en dos cartas cobra tres a dos.",
    "casino.tip.blackjack_west.3": "Yo me planto en 17. Siempre. Es la ley del saloon.",
    "casino.tip.blackjack_west.4": "Doblar con 11 es de vaqueros valientes.",
    "casino.tip.blackjack_west.5": "Las mangas arriba: aquí nadie esconde ases.",
    "casino.tip.blackjack_west.6": "Si te pasas de 21, te quedas sin caballo.",
    "casino.tip.blackjack_west.7": "Dividir dos ases es buena idea. Dos ochos, también.",
    "casino.tip.blackjack_west.8": "La casa paga en fichas. El oro, en la caja.",
    "casino.tip.blackjack_west.9": "¿Otra mano? La noche es joven y la baraja también.",
    # clientes
    "casino.tip.patron_west.0": "Dicen que el sheriff fue el mejor pistolero del territorio.",
    "casino.tip.patron_west.1": "Vine por un trago y llevo tres horas en el 21.",
    "casino.tip.patron_west.2": "Esa pianola sabe más canciones que yo.",
    "casino.tip.patron_west.3": "No mires mucho al forastero de negro...",
    "casino.tip.patron_west.4": "Mi caballo está afuera, en el bebedero. Espero.",
    "casino.tip.patron_west.5": "Gané una pepita de oro en la ruleta. ¡Una!",
    "casino.tip.patron_west.6": "¿Has visto el cartel de \"Se busca\"? Se parece a mi primo.",
    "casino.tip.patron_west.7": "Arriba, en la sala de cartas, se apuesta en serio.",
    "casino.tip.patron_west.8": "Aquí las puertas se abren solas. ¡Qué modernidad!",
    "casino.tip.patron_west.9": "El polvo del desierto se te mete hasta en las botas.",
    "casino.tip.patron_west.10": "Un día voy a construir un saloon más grande que este.",
    "casino.tip.patron_west.11": "¡Otra ronda para todos! ...Bueno, para mí.",
    "casino.tip.patron_west.say_0": "¡Buenas, forastero! ¿Vienes a probar suerte?",
    "casino.tip.patron_west.say_1": "¡Suerte en las cartas, socio!",
    "casino.tip.patron_west.say_2": "¡Qué saloon tan animado!",
    "casino.tip.patron_west.say_3": "Shh... estoy contando cartas. ¡Es broma, sheriff!",
}

EN = {
    "block.casino.saloon_door": "Saloon Door",
    "block.casino.vip_gate": "Saloon VIP Gate",
    "block.casino.player_piano": "Player Piano",
    "block.casino.painted_letter": "Painted Letter",
    "block.casino.wanted_poster": "Wanted Poster",
    "subtitles.casino.music_piano": "Player piano plays ragtime",
    "subtitles.casino.saloon_door": "Saloon door creaks",
    "casino.west.gate_set": "The gate now requires: %s",
    "casino.west.gate_ok": "Come on in, VIP partner.",
    "casino.west.gate_need_silver": "Only with a Silver VIP card in your hand or inventory (sold at the cashier).",
    "casino.west.gate_need_gold": "Only with a Gold VIP card in your hand or inventory (sold at the cashier).",
    "casino.npc.guard_west": "Sheriff",
    "casino.npc.bartender_west": "Barkeep",
    "casino.npc.blackjack_west": "Saloon Dealer",
    "casino.npc.wheel_west": "Host",
    "casino.npc.patron_64": "Cowboy Jed",
    "casino.npc.patron_65": "Blond Billy",
    "casino.npc.patron_66": "Stranger in Black",
    "casino.npc.patron_67": "Old Hank",
    "casino.npc.patron_68": "Cowgirl Rosie",
    "casino.npc.patron_69": "Card Sharp",
    "casino.npc.patron_70": "Miss Lola",
    "casino.npc.patron_71": "Prospector Pete",
    "casino.tip.guard_west.0": "Upstairs they play for keeps, stranger. VIP card only.",
    "casino.tip.guard_west.1": "A Silver card lets you up to the card room on the first floor.",
    "casino.tip.guard_west.2": "A Gold card also opens the owner's box.",
    "casino.tip.guard_west.3": "Cards are sold at the cashier, by the entrance.",
    "casino.tip.guard_west.4": "No guns at the table. In my town we bet clean.",
    "casino.tip.guard_west.5": "If you hear the bell at noon, someone's dueling in the street!",
    "casino.tip.guard_west.say_0": "Hold it. Upstairs is VIP only: Silver 5,000, Gold 25,000.",
    "casino.tip.guard_west.say_1": "Go on up, partner. The owner's box is Gold only.",
    "casino.tip.guard_west.say_2": "Welcome, boss. The whole saloon is yours.",
    "casino.tip.bartender_west.0": "A whiskey for the dust of the trail?",
    "casino.tip.bartender_west.1": "Our sarsaparilla's as good as the whiskey. Well, almost.",
    "casino.tip.bartender_west.2": "That piano never rests. Wish I could say the same.",
    "casino.tip.bartender_west.3": "If a brawl breaks out, duck behind the bar.",
    "casino.tip.bartender_west.4": "The blackjack dealer never lost his shirt. Not once.",
    "casino.tip.bartender_west.5": "Talk to me and I'll pour the house special: it brings luck.",
    "casino.tip.bartender_west.6": "Those wanted posters are real... I think.",
    "casino.tip.bartender_west.7": "Upstairs is for folks with full pockets.",
    "casino.tip.bartender_west.8": "A cowboy once won a whole ranch in a single hand.",
    "casino.tip.bartender_west.9": "Those saloon doors have creaked since 1880.",
    "casino.tip.bartender_west.say_0": "One whiskey on the house! With a miner's luck.",
    "casino.tip.bartender_west.say_1": "There you go, cowboy. May the deck smile on you!",
    "casino.tip.bartender_west.say_2": "This one's on the house. Cheers, stranger!",
    "casino.tip.bartender_west.say_3": "Easy, cowboy. Next one in a minute.",
    "casino.tip.blackjack_west.0": "Step right up, partner. We play twenty-one like in Tombstone.",
    "casino.tip.blackjack_west.1": "Place your bet before the first card falls.",
    "casino.tip.blackjack_west.2": "Blackjack pays three to two.",
    "casino.tip.blackjack_west.3": "I stand on 17. Always. Saloon law.",
    "casino.tip.blackjack_west.4": "Doubling on 11 is for brave cowboys.",
    "casino.tip.blackjack_west.5": "Sleeves rolled up: nobody hides aces here.",
    "casino.tip.blackjack_west.6": "Go over 21 and you'll walk home without a horse.",
    "casino.tip.blackjack_west.7": "Splitting two aces is smart. Two eights, too.",
    "casino.tip.blackjack_west.8": "The house pays in chips. Gold at the cashier.",
    "casino.tip.blackjack_west.9": "Another hand? The night's young and so's the deck.",
    "casino.tip.patron_west.0": "They say the sheriff was the fastest gun in the territory.",
    "casino.tip.patron_west.1": "Came in for one drink, been at the table three hours.",
    "casino.tip.patron_west.2": "That piano knows more songs than I do.",
    "casino.tip.patron_west.3": "Don't stare at the stranger in black...",
    "casino.tip.patron_west.4": "My horse is outside at the trough. I hope.",
    "casino.tip.patron_west.5": "Won a gold nugget on the wheel. One!",
    "casino.tip.patron_west.6": "Seen that wanted poster? Looks like my cousin.",
    "casino.tip.patron_west.7": "Upstairs in the card room they bet for real.",
    "casino.tip.patron_west.8": "Doors that open by themselves. How modern!",
    "casino.tip.patron_west.9": "Desert dust gets even in your boots.",
    "casino.tip.patron_west.10": "Someday I'll build a saloon bigger than this one.",
    "casino.tip.patron_west.11": "Another round for everybody! ...Well, for me.",
    "casino.tip.patron_west.say_0": "Howdy, stranger! Here to try your luck?",
    "casino.tip.patron_west.say_1": "Good luck at the cards, partner!",
    "casino.tip.patron_west.say_2": "What a lively saloon!",
    "casino.tip.patron_west.say_3": "Shh... I'm counting cards. Just kidding, sheriff!",
}


def merge(code, extra):
    p = os.path.join(L, code + ".json")
    with open(p, encoding="utf-8") as fh:
        d = json.load(fh)
    d.update(extra)
    with open(p, "w", encoding="utf-8") as fh:
        json.dump(d, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    extra_es, extra_en = {}, {}
    try:
        import west_saloon_lang as more  # textos de los juegos del Oeste (siguientes etapas)
        extra_es, extra_en = more.ES, more.EN
    except ImportError:
        pass
    import cajero_lang as caja  # caja nueva (Cambio / VIP), monedas arcade y boletos
    extra_es, extra_en = {**extra_es, **caja.ES}, {**extra_en, **caja.EN}
    merge("es_es", {**ES, **extra_es})
    merge("en_us", {**EN, **extra_en})
    print("textos del Oeste listos")
