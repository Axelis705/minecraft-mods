#!/usr/bin/env python3
"""Textos de la ronda 5 (pantallas de récords y anfitrión de la ruleta de la suerte) en todos los idiomas.

    python3 casino/herramientas/textos_ronda5.py
"""
import json
import os

L = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "lang")

T = {}


def k(key, es, en, de, fr, it, pt, ru, ja, zh):
    T[key] = {"es": es, "en_us": en, "de_de": de, "fr_fr": fr, "it_it": it, "pt_br": pt, "ru_ru": ru, "ja_jp": ja, "zh_cn": zh}


k("block.casino.record_screen", "Pantalla de récords", "Record Screen", "Rekordbildschirm", "Écran des records", "Schermo dei record",
  "Tela de recordes", "Экран рекордов", "記録スクリーン", "纪录屏幕")
k("casino.records.ch_0", "MÁS GANADORES", "TOP WINNERS", "TOP-GEWINNER", "MEILLEURS GAGNANTS", "I PIÙ VINCENTI", "MAIORES VENCEDORES",
  "ГЛАВНЫЕ ПОБЕДИТЕЛИ", "トップ勝者", "最大赢家")
k("casino.records.ch_1", "MÁS APOSTADO", "MOST WAGERED", "HÖCHSTE EINSÄTZE", "PLUS GROS PARIEURS", "PIÙ PUNTATO", "MAIS APOSTADO",
  "БОЛЬШЕ ВСЕГО СТАВОК", "最多賭け額", "下注最多")
k("casino.records.ch_2", "MÁS APUESTAS", "MOST BETS", "MEISTE WETTEN", "LE PLUS DE PARIS", "PIÙ SCOMMESSE", "MAIS APOSTAS",
  "ЧАЩЕ ВСЕХ ИГРАЮТ", "最多ベット回数", "下注次数最多")
k("casino.records.ch_3", "LOS QUE MÁS PIERDEN", "BIGGEST LOSERS", "GRÖSSTE VERLIERER", "PLUS GROS PERDANTS", "I PIÙ SFORTUNATI",
  "OS QUE MAIS PERDEM", "БОЛЬШЕ ВСЕГО ПРОИГРАЛИ", "最大敗者", "输得最多")
k("casino.records.ch_4", "PREMIOS MÁS GRANDES", "BIGGEST PRIZES", "GRÖSSTE GEWINNE", "PLUS GROS GAINS", "PREMI PIÙ GRANDI",
  "MAIORES PRÊMIOS", "САМЫЕ КРУПНЫЕ ВЫИГРЫШИ", "最大の賞金", "最大奖金")
k("casino.records.ch_5", "RÉCORDS DE HOY", "TODAY'S RECORDS", "REKORDE VON HEUTE", "RECORDS DU JOUR", "RECORD DI OGGI",
  "RECORDES DE HOJE", "РЕКОРДЫ ДНЯ", "今日の記録", "今日纪录")
k("casino.records.ch_6", "EL CASINO EN NÚMEROS", "THE CASINO IN NUMBERS", "DAS CASINO IN ZAHLEN", "LE CASINO EN CHIFFRES",
  "IL CASINÒ IN NUMERI", "O CASSINO EM NÚMEROS", "КАЗИНО В ЦИФРАХ", "数字で見るカジノ", "赌场数据")
k("casino.records.ch_7", "ÚLTIMOS PREMIOS", "LATEST WINS", "LETZTE GEWINNE", "DERNIERS GAINS", "ULTIME VINCITE", "ÚLTIMOS PRÊMIOS",
  "ПОСЛЕДНИЕ ВЫИГРЫШИ", "最新の当たり", "最新中奖")
k("casino.records.today_net", "Ganancia de hoy", "Won today", "Heute gewonnen", "Gagné aujourd'hui", "Vinto oggi", "Ganho hoje",
  "Выиграно сегодня", "今日の勝ち", "今日赢得")
k("casino.records.today_wagered", "Apostado hoy", "Wagered today", "Heute gesetzt", "Misé aujourd'hui", "Puntato oggi", "Apostado hoje",
  "Поставлено сегодня", "今日の賭け額", "今日下注")
k("casino.records.total_wagered", "Apostado", "Wagered", "Gesetzt", "Misé", "Puntato", "Apostado", "Поставлено", "賭け額", "下注总额")
k("casino.records.total_paid", "Pagado", "Paid out", "Ausgezahlt", "Payé", "Pagato", "Pago", "Выплачено", "払い戻し", "派彩")
k("casino.records.house", "Ganancia de la casa", "House profit", "Gewinn des Hauses", "Gain de la maison", "Guadagno del banco",
  "Lucro da casa", "Прибыль казино", "ハウスの利益", "庄家盈利")
k("casino.records.bets", "Apuestas", "Bets", "Wetten", "Paris", "Scommesse", "Apostas", "Ставки", "ベット数", "下注次数")
k("casino.records.players", "Jugadores", "Players", "Spieler", "Joueurs", "Giocatori", "Jogadores", "Игроки", "プレイヤー", "玩家")
k("casino.records.today", "Apostado hoy", "Wagered today", "Heute gesetzt", "Misé aujourd'hui", "Puntato oggi", "Apostado hoje",
  "Поставлено сегодня", "今日の賭け額", "今日下注")
for i, names in enumerate([
        ("Blackjack", "Blackjack", "Blackjack", "Blackjack", "Blackjack", "Blackjack", "Блэкджек", "ブラックジャック", "二十一点"),
        ("Ruleta", "Roulette", "Roulette", "Roulette", "Roulette", "Roleta", "Рулетка", "ルーレット", "轮盘"),
        ("Póker", "Poker", "Poker", "Poker", "Poker", "Pôquer", "Покер", "ポーカー", "扑克"),
        ("Tragamonedas", "Slots", "Spielautomat", "Machine à sous", "Slot", "Caça-níqueis", "Слоты", "スロット", "老虎机"),
        ("Plinko", "Plinko", "Plinko", "Plinko", "Plinko", "Plinko", "Плинко", "プリンコ", "弹珠台"),
        ("Rasca y gana", "Scratch cards", "Rubbellose", "Grattage", "Gratta e vinci", "Raspadinha", "Лотерея", "スクラッチ", "刮刮乐"),
        ("Carreras", "Horse races", "Pferderennen", "Courses", "Corse", "Corridas", "Скачки", "競馬", "赛马")]):
    k(f"casino.records.game_{i}", *names)
k("casino.records.empty", "¡Todavía nadie! Juega y aparece aquí", "Nobody yet! Play and show up here", "Noch niemand! Spiel und erscheine hier",
  "Personne encore ! Jouez et apparaissez ici", "Ancora nessuno! Gioca e appari qui", "Ninguém ainda! Jogue e apareça aqui",
  "Пока никого! Играй и попади сюда", "まだ誰もいません!遊んでここに載ろう", "还没有人!快来上榜吧")
k("casino.records.connecting", "Sintonizando...", "Tuning in...", "Empfang...", "Réglage...", "Sintonizzazione...", "Sintonizando...",
  "Настройка...", "受信中...", "调频中...")
k("casino.records.live", "EN VIVO", "LIVE", "LIVE", "EN DIRECT", "IN DIRETTA", "AO VIVO", "ЭФИР", "ライブ", "直播")
k("casino.records.won_in", "ganó %s en %s", "won %s at %s", "gewann %s bei %s", "a gagné %s au %s", "ha vinto %s a %s", "ganhou %s em %s",
  "выиграл %s в игре «%s»", "が%sを獲得(%s)", "在%2$s赢得%1$s")

k("casino.npc.wheel", "Anfitrión de la Ruleta", "Wheel Host", "Glücksrad-Moderator", "Animateur de la roue", "Presentatore della ruota",
  "Apresentador da roleta", "Ведущий колеса", "ルーレットの司会", "幸运轮主持人")
TIPS = [
    ("¡La Ruleta de la Suerte da un giro gratis cada día! Háblame y yo la giro por ti.",
     "The Wheel of Fortune gives one free spin a day! Talk to me and I'll spin it for you.",
     "Das Glücksrad schenkt dir jeden Tag einen Dreh! Sprich mich an und ich drehe es für dich.",
     "La roue de la fortune offre un tour gratuit par jour ! Parle-moi et je la lance pour toi.",
     "La ruota della fortuna regala un giro al giorno! Parlami e la giro io per te.",
     "A roleta da sorte dá um giro grátis por dia! Fale comigo e eu giro para você.",
     "Колесо удачи дарит один бесплатный поворот в день! Поговори со мной, и я его раскручу.",
     "幸運のルーレットは1日1回無料!話しかけてくれたら回してあげるよ。", "幸运轮每天免费转一次!和我说话,我来帮你转。"),
    ("Dicen que la rebanada dorada trae suerte... ¿te animas a probar?", "They say the golden slice brings luck... feeling brave?",
     "Man sagt, das goldene Feld bringt Glück ... traust du dich?", "On dit que la part dorée porte bonheur... tu tentes ta chance ?",
     "Dicono che lo spicchio d'oro porti fortuna... ci provi?", "Dizem que a fatia dourada dá sorte... vai tentar?",
     "Говорят, золотой сектор приносит удачу... рискнёшь?", "金色のマスは幸運を呼ぶらしい…試してみる?", "听说金色格子会带来好运……要试试吗?"),
    ("Cada medianoche la ruleta se reinicia: ¡un giro nuevo para todos!", "Every midnight the wheel resets: a fresh spin for everyone!",
     "Um Mitternacht wird das Rad zurückgesetzt: ein neuer Dreh für alle!", "À minuit, la roue se réinitialise : un nouveau tour pour tous !",
     "A mezzanotte la ruota si azzera: un giro nuovo per tutti!", "Toda meia-noite a roleta reinicia: um giro novo para todos!",
     "В полночь колесо обновляется: новый поворот для всех!", "毎晩0時にリセット:みんなに新しいチャンス!", "每天午夜重置:每个人都有新机会!"),
    ("Si no estás cuando cae tu premio, te lo guardo hasta que vuelvas.", "If you're gone when your prize lands, I'll keep it until you're back.",
     "Bist du weg, wenn dein Preis fällt, hebe ich ihn für dich auf.", "Si tu es parti quand ton lot tombe, je te le garde.",
     "Se non ci sei quando esce il premio, te lo conservo io.", "Se você não estiver quando sair o prêmio, eu guardo para você.",
     "Если тебя не будет, когда выпадет приз, я его сохраню.", "賞品が出た時にいなくても、戻るまで預かっておくよ。", "如果奖品出来时你不在,我会替你保管。"),
    ("¿Ya viste el premio mayor de hoy? Está en el podio, junto a la ruleta.", "Seen today's grand prize? It's on the podium by the wheel.",
     "Schon den Hauptpreis gesehen? Er steht auf dem Podest beim Rad.", "Tu as vu le gros lot du jour ? Il est sur le podium près de la roue.",
     "Hai visto il premio di oggi? È sul podio accanto alla ruota.", "Já viu o prêmio principal de hoje? Está no pódio ao lado da roleta.",
     "Видел главный приз дня? Он на подиуме у колеса.", "今日の大賞は見た?ルーレットの横の台にあるよ。", "看到今天的大奖了吗?就在轮盘旁边的台子上。"),
    ("Las pantallas de récords muestran a los grandes ganadores. ¡Tu nombre podría estar ahí!",
     "The record screens show the big winners. Your name could be up there!",
     "Die Rekordbildschirme zeigen die großen Gewinner. Dein Name könnte dort stehen!",
     "Les écrans des records montrent les grands gagnants. Ton nom pourrait y être !",
     "Gli schermi dei record mostrano i grandi vincitori. Il tuo nome potrebbe esserci!",
     "As telas de recordes mostram os grandes vencedores. Seu nome pode estar lá!",
     "На экранах рекордов — главные победители. Там может быть и твоё имя!", "記録スクリーンには大勝者が映るよ。君の名前も載るかも!",
     "纪录屏幕上有大赢家。你的名字也可能出现!"),
    ("¡Damas y caballeros, la suerte está girando!", "Ladies and gentlemen, luck is spinning!", "Meine Damen und Herren, das Glück dreht sich!",
     "Mesdames et messieurs, la chance tourne !", "Signore e signori, la fortuna gira!", "Senhoras e senhores, a sorte está girando!",
     "Дамы и господа, удача вращается!", "紳士淑女の皆さま、運命が回ります!", "女士们先生们,好运转起来啦!"),
    ("Un consejo: gira con una sonrisa, a la ruleta le gusta.", "A tip: spin with a smile, the wheel likes that.",
     "Ein Tipp: Dreh mit einem Lächeln, das mag das Rad.", "Un conseil : tourne avec le sourire, la roue adore ça.",
     "Un consiglio: gira con un sorriso, alla ruota piace.", "Uma dica: gire sorrindo, a roleta gosta.",
     "Совет: крути с улыбкой, колесу это нравится.", "アドバイス:笑顔で回そう、ルーレットはそれが好き。", "小建议:笑着转,轮盘喜欢这样。"),
]
for i, t in enumerate(TIPS):
    k(f"casino.tip.wheel.{i}", *t)
SAYS = [
    ("¿Quieres girar la ruleta y ver tu suerte?", "Want to spin the wheel and test your luck?", "Willst du das Rad drehen und dein Glück testen?",
     "Tu veux tourner la roue et tenter ta chance ?", "Vuoi girare la ruota e tentare la fortuna?", "Quer girar a roleta e testar sua sorte?",
     "Хочешь крутануть колесо и испытать удачу?", "ルーレットを回して運試しする?", "想转动轮盘试试运气吗?"),
    ("¡Allá va! ¡Mucha suerte!", "Here it goes! Good luck!", "Los geht's! Viel Glück!", "C'est parti ! Bonne chance !", "Si parte! Buona fortuna!",
     "Lá vai! Boa sorte!", "Поехали! Удачи!", "それっ!幸運を!", "开转啦!祝你好运!"),
    ("Ya giraste hoy. ¡Vuelve mañana por otra oportunidad!", "You already spun today. Come back tomorrow for another try!",
     "Du hast heute schon gedreht. Komm morgen wieder!", "Tu as déjà tourné aujourd'hui. Reviens demain !", "Hai già girato oggi. Torna domani!",
     "Você já girou hoje. Volte amanhã!", "Ты уже крутил сегодня. Возвращайся завтра!", "今日はもう回したね。また明日!", "你今天已经转过了。明天再来!"),
    ("¡¡INCREÍBLE!! ¡Qué premiazo!", "INCREDIBLE!! What a prize!", "UNGLAUBLICH!! Was für ein Preis!", "INCROYABLE !! Quel lot !",
     "INCREDIBILE!! Che premio!", "INCRÍVEL!! Que prêmio!", "НЕВЕРОЯТНО!! Вот это приз!", "すごい!!大当たりだ!", "太不可思议了!!大奖!"),
    ("¡Felicidades! ¡Disfruta tu premio!", "Congratulations! Enjoy your prize!", "Glückwunsch! Viel Spaß mit dem Preis!",
     "Félicitations ! Profite de ton lot !", "Congratulazioni! Goditi il premio!", "Parabéns! Aproveite o prêmio!",
     "Поздравляю! Наслаждайся призом!", "おめでとう!賞品を楽しんで!", "恭喜!好好享受奖品!"),
    ("¡Bienvenido! ¿Te animas a girar la Ruleta de la Suerte?", "Welcome! Care to spin the Wheel of Fortune?",
     "Willkommen! Lust, das Glücksrad zu drehen?", "Bienvenue ! Tu veux tourner la roue de la fortune ?",
     "Benvenuto! Ti va di girare la ruota della fortuna?", "Bem-vindo! Vai girar a roleta da sorte?",
     "Добро пожаловать! Крутанёшь колесо удачи?", "ようこそ!幸運のルーレットを回してみる?", "欢迎!要转一下幸运轮吗?"),
    ("¡Un momento! La ruleta está girando.", "One moment! The wheel is spinning.", "Einen Moment! Das Rad dreht sich.",
     "Un instant ! La roue tourne.", "Un attimo! La ruota sta girando.", "Um momento! A roleta está girando.", "Минутку! Колесо крутится.",
     "ちょっと待って!今回っているよ。", "稍等!轮盘正在转。"),
    ("¡Por aquí! Háblame a mí y yo la giro por ti.", "Over here! Talk to me and I'll spin it for you.",
     "Hier drüben! Sprich mit mir, ich drehe es für dich.", "Par ici ! Parle-moi et je la lance pour toi.",
     "Da questa parte! Parla con me e la giro io.", "Aqui! Fale comigo que eu giro para você.",
     "Сюда! Поговори со мной, и я раскручу его.", "こっちだよ!話しかけてくれたら回すよ。", "这边!跟我说话,我来帮你转。"),
]
for i, t in enumerate(SAYS):
    k(f"casino.tip.wheel.say_{i}", *t)
k("casino.wheel.ask_host", "Habla con el anfitrión para girar la ruleta.", "Talk to the host to spin the wheel.",
  "Sprich mit dem Moderator, um das Rad zu drehen.", "Parle à l'animateur pour tourner la roue.", "Parla con il presentatore per girare la ruota.",
  "Fale com o apresentador para girar a roleta.", "Поговори с ведущим, чтобы крутить колесо.", "司会に話しかけてルーレットを回そう。", "和主持人说话来转轮盘。")
k("casino.wheel.dialog.ask",
  "¡Bienvenido a la Ruleta de la Suerte! Hoy tienes un giro gratis. ¿Quieres girar la ruleta y ver tu suerte? Premio mayor: %s",
  "Welcome to the Wheel of Fortune! You have a free spin today. Want to spin the wheel and test your luck? Grand prize: %s",
  "Willkommen beim Glücksrad! Du hast heute einen Gratis-Dreh. Willst du dein Glück testen? Hauptpreis: %s",
  "Bienvenue à la roue de la fortune ! Tu as un tour gratuit aujourd'hui. Tu veux tenter ta chance ? Gros lot : %s",
  "Benvenuto alla ruota della fortuna! Oggi hai un giro gratis. Vuoi tentare la fortuna? Primo premio: %s",
  "Bem-vindo à roleta da sorte! Hoje você tem um giro grátis. Quer testar sua sorte? Prêmio principal: %s",
  "Добро пожаловать к колесу удачи! Сегодня у тебя бесплатный поворот. Испытаешь удачу? Главный приз: %s",
  "幸運のルーレットへようこそ!今日は無料で1回回せるよ。運試しする?大賞:%s", "欢迎来到幸运轮!你今天有一次免费机会。要试试运气吗?大奖:%s")
k("casino.wheel.dialog.ask_plain",
  "¡Bienvenido a la Ruleta de la Suerte! Hoy tienes un giro gratis. ¿Quieres girar la ruleta y ver tu suerte?",
  "Welcome to the Wheel of Fortune! You have a free spin today. Want to spin the wheel and test your luck?",
  "Willkommen beim Glücksrad! Du hast heute einen Gratis-Dreh. Willst du dein Glück testen?",
  "Bienvenue à la roue de la fortune ! Tu as un tour gratuit aujourd'hui. Tu veux tenter ta chance ?",
  "Benvenuto alla ruota della fortuna! Oggi hai un giro gratis. Vuoi tentare la fortuna?",
  "Bem-vindo à roleta da sorte! Hoje você tem um giro grátis. Quer testar sua sorte?",
  "Добро пожаловать к колесу удачи! Сегодня у тебя бесплатный поворот. Испытаешь удачу?",
  "幸運のルーレットへようこそ!今日は無料で1回回せるよ。運試しする?", "欢迎来到幸运轮!你今天有一次免费机会。要试试运气吗?")
k("casino.wheel.dialog.wait", "Ya giraste la ruleta hoy. Tu próximo giro estará listo en %s. ¡Aquí te espero!",
  "You already spun the wheel today. Your next spin will be ready in %s. I'll be waiting!",
  "Du hast heute schon gedreht. Dein nächster Dreh ist in %s bereit. Ich warte auf dich!",
  "Tu as déjà tourné la roue aujourd'hui. Ton prochain tour sera prêt dans %s. Je t'attends !",
  "Hai già girato la ruota oggi. Il prossimo giro sarà pronto tra %s. Ti aspetto!",
  "Você já girou a roleta hoje. Seu próximo giro estará pronto em %s. Te espero!",
  "Ты уже крутил колесо сегодня. Следующий поворот будет через %s. Жду тебя!",
  "今日はもう回したね。次は%s後に回せるよ。待ってるね!", "你今天已经转过了。下次机会还有%s。我等你!")
k("casino.wheel.dialog.busy", "¡Un momento! La ruleta está girando ahora mismo. Espera a que se detenga y vuelve a hablarme.",
  "One moment! The wheel is spinning right now. Wait for it to stop and talk to me again.",
  "Einen Moment! Das Rad dreht sich gerade. Warte, bis es anhält, und sprich mich dann an.",
  "Un instant ! La roue tourne en ce moment. Attends qu'elle s'arrête et reparle-moi.",
  "Un attimo! La ruota sta girando. Aspetta che si fermi e parlami di nuovo.",
  "Um momento! A roleta está girando agora. Espere ela parar e fale comigo de novo.",
  "Минутку! Колесо сейчас крутится. Подожди, пока остановится, и подойди снова.",
  "ちょっと待って!今ルーレットが回っているよ。止まったらまた話しかけてね。", "稍等!轮盘正在转。等它停下再来找我。")
k("casino.wheel.dialog.yes", "¡Sí, gírala!", "Yes, spin it!", "Ja, dreh es!", "Oui, lance-la !", "Sì, girala!", "Sim, gire!", "Да, крути!",
  "回して!", "转吧!")
k("casino.wheel.dialog.no", "Ahora no", "Not now", "Jetzt nicht", "Pas maintenant", "Non ora", "Agora não", "Не сейчас", "今はいい", "以后再说")
k("casino.wheel.dialog.ok", "Entendido", "Got it", "Verstanden", "D'accord", "Capito", "Entendi", "Понятно", "わかった", "知道了")

# ---- ronda 6: clientes del casino
for i, n in enumerate(["Sofía", "Don Ramiro", "Valentina", "Mateo", "Lucía", "Carlos", "Isabela", "Fernando", "Camila", "Diego", "Elena", "Ricardo"]):
    k(f"casino.npc.patron_{i}", n, n, n, n, n, n, n, n, n)
k("casino.npc.patron", "Cliente", "Guest", "Gast", "Client", "Cliente", "Cliente", "Гость", "お客さん", "客人")
PATRON_TIPS = [
    ("¡Hoy siento que es mi noche de suerte!", "I feel like tonight is my lucky night!", "Heute ist meine Glücksnacht, das spüre ich!",
     "Je sens que c'est ma nuit de chance !", "Sento che stasera è la mia notte fortunata!", "Sinto que hoje é minha noite de sorte!",
     "Чувствую, сегодня моя счастливая ночь!", "今夜はツイてる気がする!", "我感觉今晚是我的幸运夜!"),
    ("¿Viste la ruleta? Cayó tres veces seguidas en rojo.", "Did you see the roulette? Red came up three times in a row.",
     "Hast du das Roulette gesehen? Dreimal hintereinander Rot.", "T'as vu la roulette ? Trois fois rouge d'affilée.",
     "Hai visto la roulette? Rosso tre volte di fila.", "Viu a roleta? Deu vermelho três vezes seguidas.",
     "Видел рулетку? Три раза подряд красное.", "ルーレット見た?3回連続で赤だよ。", "看到轮盘了吗?连续三次红色。"),
    ("Ese crupier de blackjack reparte rapidísimo.", "That blackjack dealer deals super fast.", "Der Blackjack-Croupier teilt blitzschnell aus.",
     "Ce croupier de blackjack distribue à toute vitesse.", "Quel croupier del blackjack distribuisce velocissimo.",
     "Aquele crupiê de blackjack distribui rapidíssimo.", "Этот крупье в блэкджек раздаёт молниеносно.", "あのブラックジャックのディーラー、配るのが速い!", "那个二十一点荷官发牌超快。"),
    ("Pedí el cóctel Diamante... ¡y gané dos manos seguidas!", "I ordered the Diamond cocktail... and won two hands in a row!",
     "Ich hab den Diamant-Cocktail bestellt ... und zwei Runden gewonnen!", "J'ai pris le cocktail Diamant... et gagné deux mains de suite !",
     "Ho preso il cocktail Diamante... e ho vinto due mani di fila!", "Pedi o coquetel Diamante... e ganhei duas mãos seguidas!",
     "Заказал коктейль «Бриллиант»... и выиграл две раздачи подряд!", "ダイヤモンドカクテルを頼んだら…2連勝!", "我点了钻石鸡尾酒……然后连赢两局!"),
    ("Dicen que en el sótano hay carreras de caballos.", "They say there are horse races in the basement.", "Im Keller soll es Pferderennen geben.",
     "Il paraît qu'il y a des courses de chevaux au sous-sol.", "Dicono che nel seminterrato ci siano le corse dei cavalli.",
     "Dizem que no porão tem corrida de cavalos.", "Говорят, в подвале проходят скачки.", "地下で競馬をやってるらしいよ。", "听说地下室有赛马。"),
    ("No le digas a nadie, pero ya perdí la mitad de mis fichas.", "Don't tell anyone, but I've already lost half my chips.",
     "Sag's keinem, aber ich hab schon die Hälfte meiner Chips verloren.", "Ne le dis à personne, mais j'ai déjà perdu la moitié de mes jetons.",
     "Non dirlo a nessuno, ma ho già perso metà delle fiches.", "Não conta pra ninguém, mas já perdi metade das fichas.",
     "Никому не говори, но я уже проиграл половину фишек.", "内緒だけど、もうチップの半分なくしちゃった。", "别告诉别人,我已经输掉一半筹码了。"),
    ("Me encanta la música de este lugar.", "I love the music in this place.", "Ich liebe die Musik hier.", "J'adore la musique ici.",
     "Adoro la musica di questo posto.", "Adoro a música deste lugar.", "Обожаю здешнюю музыку.", "ここの音楽、大好き。", "我超喜欢这里的音乐。"),
    ("Desde la terraza se ve toda la ciudad.", "You can see the whole city from the terrace.", "Von der Terrasse sieht man die ganze Stadt.",
     "De la terrasse, on voit toute la ville.", "Dalla terrazza si vede tutta la città.", "Da varanda dá pra ver a cidade inteira.",
     "С террасы видно весь город.", "テラスから街全体が見えるよ。", "从露台能看到整座城市。"),
    ("¡Mi nombre va a salir en la pantalla de récords!", "My name is going to be on the record screen!", "Mein Name kommt auf den Rekordbildschirm!",
     "Mon nom va apparaître sur l'écran des records !", "Il mio nome finirà sullo schermo dei record!", "Meu nome vai aparecer na tela de recordes!",
     "Моё имя появится на экране рекордов!", "記録スクリーンに名前を載せてやる!", "我的名字要上纪录屏幕了!"),
    ("Una vuelta más a la ruleta y me voy... bueno, dos.", "One more spin and I'm leaving... well, two.", "Noch eine Runde und ich geh ... na gut, zwei.",
     "Encore un tour et je m'en vais... bon, deux.", "Un altro giro e me ne vado... vabbè, due.", "Mais uma rodada e vou embora... tá, duas.",
     "Ещё один круг и ухожу... ну ладно, два.", "あと1回回したら帰る…やっぱり2回。", "再转一次我就走……好吧,两次。"),
    ("¿Ya probaste el rasca y gana? A mi amiga le salió el premio mayor.", "Tried the scratch cards? My friend hit the jackpot.",
     "Schon die Rubbellose probiert? Meine Freundin hat den Hauptgewinn!", "T'as essayé le grattage ? Mon amie a touché le gros lot.",
     "Hai provato il gratta e vinci? La mia amica ha vinto il premio grosso.", "Já tentou a raspadinha? Minha amiga ganhou o prêmio máximo.",
     "Пробовал лотерею? Моя подруга сорвала джекпот.", "スクラッチやった?友達が大当たり出したよ。", "你玩过刮刮乐吗?我朋友中了头奖。"),
    ("El penthouse es solo para VIP... algún día.", "The penthouse is VIP only... someday.", "Das Penthouse ist nur für VIPs ... eines Tages.",
     "Le penthouse est réservé aux VIP... un jour.", "L'attico è solo per i VIP... un giorno.", "A cobertura é só para VIP... um dia.",
     "Пентхаус только для VIP... когда-нибудь.", "ペントハウスはVIP専用…いつかきっと。", "顶层套房只限VIP……总有一天。"),
]
for i, t in enumerate(PATRON_TIPS):
    k(f"casino.tip.patron.{i}", *t)
PATRON_SAYS = [
    ("¡Hola! ¿Vienes a probar suerte?", "Hi! Here to try your luck?", "Hallo! Willst du dein Glück versuchen?", "Salut ! Tu viens tenter ta chance ?",
     "Ciao! Vieni a tentare la fortuna?", "Oi! Veio testar a sorte?", "Привет! Пришёл испытать удачу?", "やあ!運試しに来たの?", "嗨!来试试运气吗?"),
    ("¡Suerte en las mesas!", "Good luck at the tables!", "Viel Glück an den Tischen!", "Bonne chance aux tables !", "Buona fortuna ai tavoli!",
     "Boa sorte nas mesas!", "Удачи за столами!", "テーブルで幸運を!", "祝你牌桌好运!"),
    ("¡Qué ambiente tan increíble hay hoy!", "What an amazing vibe tonight!", "Was für eine tolle Stimmung heute!", "Quelle ambiance incroyable ce soir !",
     "Che atmosfera incredibile stasera!", "Que clima incrível hoje!", "Какая сегодня потрясающая атмосфера!", "今日はすごく盛り上がってるね!", "今晚气氛太棒了!"),
    ("Shh... estoy contando cartas. ¡Es broma!", "Shh... I'm counting cards. Just kidding!", "Psst ... ich zähle Karten. Nur Spaß!",
     "Chut... je compte les cartes. Je rigole !", "Shh... sto contando le carte. Scherzo!", "Psiu... tô contando cartas. Brincadeira!",
     "Тсс... я считаю карты. Шучу!", "シーッ…カードを数えてるんだ。冗談だよ!", "嘘……我在算牌。开玩笑的!"),
]
for i, t in enumerate(PATRON_SAYS):
    k(f"casino.tip.patron.say_{i}", *t)


# ---- ronda 7: tarjeta de socio y logros
k("item.casino.member_card", "Tarjeta de socio", "Member Card", "Mitgliedskarte", "Carte de membre", "Tessera socio", "Cartão de sócio",
  "Карта участника", "会員カード", "会员卡")
k("casino.member.tooltip", "Clic derecho: tu nivel, tus logros y tu bono diario", "Right-click: your level, achievements and daily bonus",
  "Rechtsklick: Stufe, Erfolge und Tagesbonus", "Clic droit : ton niveau, tes succès et ton bonus du jour", "Clic destro: livello, obiettivi e bonus giornaliero",
  "Clique direito: seu nível, conquistas e bônus diário", "ПКМ: уровень, достижения и ежедневный бонус", "右クリック:レベル・実績・デイリーボーナス", "右键:等级、成就和每日奖励")
k("casino.member.welcome", "¡Bienvenido al club! Te dimos tu Tarjeta de socio: úsala para ver tu nivel, tus logros y cobrar tu bono diario.",
  "Welcome to the club! Here's your Member Card: use it to see your level, achievements and claim your daily bonus.",
  "Willkommen im Club! Hier ist deine Mitgliedskarte: Stufe, Erfolge und Tagesbonus.", "Bienvenue au club ! Voici ta carte de membre : niveau, succès et bonus du jour.",
  "Benvenuto nel club! Ecco la tua tessera socio: livello, obiettivi e bonus giornaliero.", "Bem-vindo ao clube! Aqui está seu cartão de sócio: nível, conquistas e bônus diário.",
  "Добро пожаловать в клуб! Вот твоя карта участника: уровень, достижения и ежедневный бонус.", "クラブへようこそ!会員カードをどうぞ。レベル・実績・デイリーボーナスを確認できます。",
  "欢迎加入俱乐部!这是你的会员卡:查看等级、成就并领取每日奖励。")
k("casino.member.card_title", "Socio del casino", "Casino member", "Casino-Mitglied", "Membre du casino", "Socio del casinò", "Sócio do cassino",
  "Участник казино", "カジノ会員", "赌场会员")
for lv, names in (("bronze", ("Bronce", "Bronze", "Bronze", "Bronze", "Bronzo", "Bronze", "Бронза", "ブロンズ", "青铜")),
                  ("silver", ("Plata", "Silver", "Silber", "Argent", "Argento", "Prata", "Серебро", "シルバー", "白银")),
                  ("gold", ("Oro", "Gold", "Gold", "Or", "Oro", "Ouro", "Золото", "ゴールド", "黄金")),
                  ("platinum", ("Platino", "Platinum", "Platin", "Platine", "Platino", "Platina", "Платина", "プラチナ", "铂金")),
                  ("diamond", ("Diamante", "Diamond", "Diamant", "Diamant", "Diamante", "Diamante", "Бриллиант", "ダイヤモンド", "钻石"))):
    k("casino.member.level_" + lv, *names)
k("casino.member.level_up", "¡Nuevo nivel: %s!", "New level: %s!", "Neue Stufe: %s!", "Nouveau niveau : %s !", "Nuovo livello: %s!", "Novo nível: %s!",
  "Новый уровень: %s!", "新レベル:%s!", "新等级:%s!")
k("casino.member.level_up_chat", "¡Subiste de nivel de socio! Ahora eres %s. Tu bono diario y tus premios crecen.",
  "Member level up! You are now %s. Your daily bonus and rewards grow.", "Mitgliedsstufe aufgestiegen! Du bist jetzt %s.",
  "Niveau de membre supérieur ! Tu es maintenant %s.", "Livello socio aumentato! Ora sei %s.", "Subiu de nível de sócio! Agora você é %s.",
  "Новый уровень участника! Теперь ты %s.", "会員レベルアップ!あなたは%sになりました。", "会员等级提升!你现在是%s。")
k("casino.member.next", "Siguiente nivel: %s", "Next level: %s", "Nächste Stufe: %s", "Niveau suivant : %s", "Prossimo livello: %s", "Próximo nível: %s",
  "Следующий уровень: %s", "次のレベル:%s", "下一等级:%s")
k("casino.member.max", "¡Nivel máximo alcanzado!", "Maximum level reached!", "Höchste Stufe erreicht!", "Niveau maximum atteint !", "Livello massimo raggiunto!",
  "Nível máximo alcançado!", "Достигнут максимальный уровень!", "最高レベル達成!", "已达最高等级!")
k("casino.member.won", "Ganado", "Won", "Gewonnen", "Gagné", "Vinto", "Ganho", "Выиграно", "獲得", "赢得")
k("casino.member.net", "Ganancia neta", "Net profit", "Nettogewinn", "Gain net", "Guadagno netto", "Lucro líquido", "Чистая прибыль", "純利益", "净收益")
k("casino.member.achievements", "Logros %s/%s", "Achievements %s/%s", "Erfolge %s/%s", "Succès %s/%s", "Obiettivi %s/%s", "Conquistas %s/%s",
  "Достижения %s/%s", "実績 %s/%s", "成就 %s/%s")
k("casino.member.reward", "Premio: %s fichas", "Reward: %s chips", "Belohnung: %s Chips", "Récompense : %s jetons", "Premio: %s fiches", "Prêmio: %s fichas",
  "Награда: %s фишек", "報酬:%sチップ", "奖励:%s筹码")
k("casino.member.done", "¡Conseguido!", "Unlocked!", "Freigeschaltet!", "Débloqué !", "Sbloccato!", "Desbloqueado!", "Получено!", "達成!", "已解锁!")
k("casino.member.locked", "Todavía no", "Not yet", "Noch nicht", "Pas encore", "Non ancora", "Ainda não", "Ещё нет", "未達成", "尚未达成")
k("casino.member.bonus", "Cobrar bono diario (+%s)", "Claim daily bonus (+%s)", "Tagesbonus holen (+%s)", "Bonus du jour (+%s)", "Ritira bonus (+%s)",
  "Pegar bônus diário (+%s)", "Забрать бонус (+%s)", "デイリーボーナス(+%s)", "领取每日奖励(+%s)")
k("casino.member.bonus_wait", "Bono cobrado: vuelve mañana", "Bonus claimed: come back tomorrow", "Bonus abgeholt: morgen wieder", "Bonus pris : reviens demain",
  "Bonus ritirato: torna domani", "Bônus pego: volte amanhã", "Бонус получен: приходи завтра", "受取済み:また明日", "已领取:明天再来")
k("casino.member.bonus_got", "Bono diario de socio: +%s fichas", "Member daily bonus: +%s chips", "Mitglieds-Tagesbonus: +%s Chips",
  "Bonus du jour : +%s jetons", "Bonus giornaliero: +%s fiches", "Bônus diário: +%s fichas", "Ежедневный бонус: +%s фишек", "デイリーボーナス:+%sチップ", "每日奖励:+%s筹码")
k("casino.member.bonus_done", "Ya cobraste tu bono de hoy.", "You already claimed today's bonus.", "Den heutigen Bonus hast du schon.",
  "Tu as déjà pris le bonus du jour.", "Hai già ritirato il bonus di oggi.", "Você já pegou o bônus de hoje.", "Сегодняшний бонус уже получен.", "今日のボーナスは受取済みです。", "今天的奖励已领取。")
k("casino.member.daily_info", "Tu bono diario: %s fichas", "Your daily bonus: %s chips", "Dein Tagesbonus: %s Chips", "Ton bonus du jour : %s jetons",
  "Il tuo bonus: %s fiches", "Seu bônus diário: %s fichas", "Твой бонус: %s фишек", "デイリー:%sチップ", "每日奖励:%s筹码")
k("casino.member.unlocked", "★ ¡Logro desbloqueado! %s (+%s fichas)", "★ Achievement unlocked! %s (+%s chips)", "★ Erfolg freigeschaltet! %s (+%s Chips)",
  "★ Succès débloqué ! %s (+%s jetons)", "★ Obiettivo sbloccato! %s (+%s fiches)", "★ Conquista desbloqueada! %s (+%s fichas)",
  "★ Достижение получено! %s (+%s фишек)", "★ 実績解除!%s(+%sチップ)", "★ 成就解锁!%s(+%s筹码)")
k("casino.member.ach_prize", "Logro: %s", "Achievement: %s", "Erfolg: %s", "Succès : %s", "Obiettivo: %s", "Conquista: %s", "Достижение: %s", "実績:%s", "成就:%s")
ACH = [
    (("Primera apuesta", "First bet", "Erste Wette", "Premier pari", "Prima scommessa", "Primeira aposta", "Первая ставка", "初ベット", "第一次下注"),
     ("Haz tu primera apuesta en el casino.", "Place your first bet in the casino.", "Platziere deine erste Wette.", "Fais ton premier pari.", "Fai la tua prima puntata.",
      "Faça sua primeira aposta.", "Сделай первую ставку.", "カジノで初めて賭ける。", "在赌场第一次下注。")),
    (("Apostador de altura", "High roller", "High Roller", "Gros joueur", "Giocatore d'azzardo", "Apostador de elite", "Хайроллер", "ハイローラー", "豪赌客"),
     ("Apuesta 1,000 fichas o más de una sola vez.", "Bet 1,000 chips or more at once.", "Setze 1.000 Chips oder mehr auf einmal.", "Mise 1 000 jetons ou plus d'un coup.",
      "Punta 1.000 fiches o più in una volta.", "Aposte 1.000 fichas ou mais de uma vez.", "Поставь 1000 фишек или больше за раз.", "一度に1,000チップ以上賭ける。", "一次下注1000筹码以上。")),
    (("Gran ganador", "Big winner", "Großer Gewinner", "Grand gagnant", "Grande vincitore", "Grande vencedor", "Крупный выигрыш", "大勝利", "大赢家"),
     ("Gana 1,000 fichas o más en una jugada.", "Win 1,000 chips or more in one play.", "Gewinne 1.000 Chips oder mehr in einem Spiel.", "Gagne 1 000 jetons ou plus en un coup.",
      "Vinci 1.000 fiches o più in una giocata.", "Ganhe 1.000 fichas ou mais numa jogada.", "Выиграй 1000 фишек или больше за раз.", "一度に1,000チップ以上勝つ。", "一次赢得1000筹码以上。")),
    (("¡Premio gordo!", "Jackpot!", "Jackpot!", "Jackpot !", "Jackpot!", "Jackpot!", "Джекпот!", "ジャックポット!", "头奖!"),
     ("Gana 10,000 fichas o más en una jugada.", "Win 10,000 chips or more in one play.", "Gewinne 10.000 Chips oder mehr in einem Spiel.", "Gagne 10 000 jetons ou plus en un coup.",
      "Vinci 10.000 fiches o più in una giocata.", "Ganhe 10.000 fichas ou mais numa jogada.", "Выиграй 10 000 фишек за раз.", "一度に10,000チップ以上勝つ。", "一次赢得10000筹码以上。")),
    (("Conocedor", "Connoisseur", "Kenner", "Connaisseur", "Intenditore", "Conhecedor", "Знаток", "目利き", "行家"),
     ("Juega los 7 juegos del casino.", "Play all 7 casino games.", "Spiele alle 7 Casinospiele.", "Joue aux 7 jeux du casino.", "Gioca a tutti i 7 giochi.",
      "Jogue os 7 jogos do cassino.", "Сыграй во все 7 игр казино.", "カジノの7種類のゲームを遊ぶ。", "玩遍赌场的7种游戏。")),
    (("Cliente frecuente", "Regular", "Stammgast", "Habitué", "Cliente abituale", "Cliente frequente", "Завсегдатай", "常連", "常客"),
     ("Haz 100 apuestas.", "Place 100 bets.", "Platziere 100 Wetten.", "Fais 100 paris.", "Fai 100 puntate.", "Faça 100 apostas.", "Сделай 100 ставок.", "100回賭ける。", "下注100次。")),
    (("Veterano", "Veteran", "Veteran", "Vétéran", "Veterano", "Veterano", "Ветеран", "ベテラン", "老手"),
     ("Haz 1,000 apuestas.", "Place 1,000 bets.", "Platziere 1.000 Wetten.", "Fais 1 000 paris.", "Fai 1.000 puntate.", "Faça 1.000 apostas.", "Сделай 1000 ставок.", "1,000回賭ける。", "下注1000次。")),
    (("¡Blackjack!", "Natural Blackjack", "Natürlicher Blackjack", "Blackjack naturel", "Blackjack naturale", "Blackjack natural", "Натуральный блэкджек", "ナチュラル・ブラックジャック", "天生二十一点"),
     ("Saca 21 con tus dos primeras cartas.", "Get 21 with your first two cards.", "Erziele 21 mit den ersten zwei Karten.", "Fais 21 avec tes deux premières cartes.",
      "Fai 21 con le prime due carte.", "Faça 21 com as duas primeiras cartas.", "Набери 21 первыми двумя картами.", "最初の2枚で21を出す。", "用前两张牌拿到21点。")),
    (("Casa llena", "Full house", "Full House", "Full", "Full", "Full house", "Фулл-хаус", "フルハウス", "葫芦"),
     ("Gana al póker con full o mejor.", "Win at poker with a full house or better.", "Gewinne beim Poker mit Full House oder besser.", "Gagne au poker avec un full ou mieux.",
      "Vinci a poker con un full o meglio.", "Ganhe no pôquer com full house ou melhor.", "Выиграй в покер с фулл-хаусом или лучше.", "フルハウス以上でポーカーに勝つ。", "用葫芦或更好的牌赢得扑克。")),
    (("Escalera de color", "Straight flush", "Straight Flush", "Quinte flush", "Scala colore", "Straight flush", "Стрит-флеш", "ストレートフラッシュ", "同花顺"),
     ("Gana al póker con escalera de color o escalera real.", "Win at poker with a straight flush or royal flush.", "Gewinne mit Straight Flush oder Royal Flush.",
      "Gagne avec une quinte flush ou une quinte flush royale.", "Vinci con scala colore o scala reale.", "Ganhe com straight flush ou royal flush.",
      "Выиграй со стрит-флешем или роял-флешем.", "ストレートフラッシュ以上で勝つ。", "用同花顺或皇家同花顺获胜。")),
    (("Pleno", "Straight up", "Plein", "Plein", "Pieno", "Pleno", "Прямая ставка", "ストレートアップ", "单号命中"),
     ("Acierta un número exacto en la ruleta.", "Hit a single number at roulette.", "Triff eine einzelne Zahl beim Roulette.", "Touche un numéro plein à la roulette.",
      "Centra un numero secco alla roulette.", "Acerte um número exato na roleta.", "Угадай точное число в рулетке.", "ルーレットで1つの数字を当てる。", "在轮盘押中单个号码。")),
    (("Tres sietes", "Triple seven", "Dreifache Sieben", "Triple sept", "Tre sette", "Três setes", "Три семёрки", "トリプルセブン", "三个七"),
     ("Saca el premio mayor de una tragamonedas.", "Hit a slot machine jackpot.", "Knacke den Jackpot am Spielautomaten.", "Décroche le jackpot d'une machine à sous.",
      "Vinci il jackpot di una slot.", "Ganhe o jackpot de um caça-níqueis.", "Сорви джекпот на слоте.", "スロットでジャックポットを当てる。", "在老虎机中头奖。")),
    (("Al borde", "On the edge", "Am Rand", "Au bord", "Sul bordo", "Na beirada", "На краю", "ふちギリギリ", "边缘大奖"),
     ("Cae en un casillero de 22x o más en el plinko.", "Land a 22x or better slot in plinko.", "Lande beim Plinko auf 22x oder mehr.", "Tombe sur une case 22x ou plus au plinko.",
      "Atterra su 22x o più al plinko.", "Caia numa casa de 22x ou mais no plinko.", "Попади в ячейку 22x или выше в плинко.", "プリンコで22倍以上に入れる。", "弹珠台落入22倍以上。")),
    (("Ojo de jinete", "Horse sense", "Pferdeverstand", "Œil du turfiste", "Occhio da fantino", "Olho de jóquei", "Чутьё на лошадей", "馬を見る目", "慧眼识马"),
     ("Gana una apuesta en las carreras de caballos.", "Win a bet at the horse races.", "Gewinne eine Wette beim Pferderennen.", "Gagne un pari aux courses.",
      "Vinci una scommessa alle corse.", "Ganhe uma aposta nas corridas.", "Выиграй ставку на скачках.", "競馬で賭けに勝つ。", "在赛马中赢得一次下注。")),
    (("Rasca la suerte", "Lucky scratch", "Glückslos", "Grattage gagnant", "Gratta fortunato", "Raspadinha da sorte", "Счастливый билет", "ラッキースクラッチ", "幸运刮刮乐"),
     ("Gana 1,000 fichas o más con un rasca y gana.", "Win 1,000 chips or more on a scratch card.", "Gewinne 1.000 Chips oder mehr mit einem Rubbellos.",
      "Gagne 1 000 jetons ou plus au grattage.", "Vinci 1.000 fiches o più con un gratta e vinci.", "Ganhe 1.000 fichas ou mais numa raspadinha.",
      "Выиграй 1000 фишек или больше в лотерее.", "スクラッチで1,000チップ以上当てる。", "刮刮乐赢得1000筹码以上。")),
    (("Fiel a la ruleta", "Wheel devotee", "Glücksrad-Fan", "Fidèle de la roue", "Fedele alla ruota", "Fiel à roleta", "Поклонник колеса", "ルーレットの常連", "幸运轮粉丝"),
     ("Gira la Ruleta de la Suerte 7 veces (una por día).", "Spin the Wheel of Fortune 7 times (once a day).", "Drehe das Glücksrad 7-mal (einmal pro Tag).",
      "Tourne la roue de la fortune 7 fois (une par jour).", "Gira la ruota della fortuna 7 volte (una al giorno).", "Gire a roleta da sorte 7 vezes (uma por dia).",
      "Крути колесо удачи 7 раз (раз в день).", "幸運のルーレットを7回回す(1日1回)。", "转动幸运轮7次(每天一次)。")),
    (("Cliente del bar", "Bar regular", "Barstammgast", "Habitué du bar", "Cliente del bar", "Cliente do bar", "Завсегдатай бара", "バーの常連", "酒吧常客"),
     ("Tómate 10 cócteles Diamante.", "Drink 10 Diamond cocktails.", "Trinke 10 Diamant-Cocktails.", "Bois 10 cocktails Diamant.", "Bevi 10 cocktail Diamante.",
      "Beba 10 coquetéis Diamante.", "Выпей 10 коктейлей «Бриллиант».", "ダイヤモンドカクテルを10杯飲む。", "喝10杯钻石鸡尾酒。")),
    (("VIP de oro", "Gold VIP", "Gold-VIP", "VIP or", "VIP oro", "VIP ouro", "Золотой VIP", "ゴールドVIP", "金卡VIP"),
     ("Juega con la tarjeta VIP de oro.", "Play with the gold VIP card.", "Spiele mit der goldenen VIP-Karte.", "Joue avec la carte VIP or.", "Gioca con la tessera VIP oro.",
      "Jogue com o cartão VIP de ouro.", "Играй с золотой VIP-картой.", "ゴールドVIPカードで遊ぶ。", "持金卡VIP游玩。")),
    (("Socio Diamante", "Diamond member", "Diamant-Mitglied", "Membre Diamant", "Socio Diamante", "Sócio Diamante", "Бриллиантовый участник", "ダイヤモンド会員", "钻石会员"),
     ("Llega al nivel de socio Diamante.", "Reach Diamond member level.", "Erreiche die Diamant-Stufe.", "Atteins le niveau Diamant.", "Raggiungi il livello Diamante.",
      "Chegue ao nível Diamante.", "Достигни уровня «Бриллиант».", "ダイヤモンド会員になる。", "达到钻石会员等级。")),
    (("Rompe la banca", "Break the bank", "Die Bank sprengen", "Faire sauter la banque", "Sbanca il banco", "Quebre a banca", "Сорви банк", "胴元を破る", "赢垮庄家"),
     ("Lleva 10,000 fichas de ganancia neta.", "Reach 10,000 chips of net profit.", "Erreiche 10.000 Chips Nettogewinn.", "Atteins 10 000 jetons de gain net.",
      "Raggiungi 10.000 fiches di guadagno netto.", "Chegue a 10.000 fichas de lucro líquido.", "Набери 10 000 фишек чистой прибыли.", "純利益10,000チップを達成。", "净收益达到10000筹码。")),
]
for i, (name, desc) in enumerate(ACH):
    k(f"casino.ach.{i}", *name)
    k(f"casino.ach.{i}.desc", *desc)


def main():
    for fn in sorted(os.listdir(L)):
        if not fn.endswith(".json"):
            continue
        code = fn[:-5]
        p = os.path.join(L, fn)
        with open(p, encoding="utf-8") as fh:
            lang = json.load(fh)
        for key, tr in T.items():
            lang[key] = tr["es"] if code.startswith("es") else tr.get(code, tr["en_us"])
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(lang, fh, ensure_ascii=False, indent=2)
            fh.write("\n")
    print(len(T), "textos en", len(os.listdir(L)), "idiomas")


if __name__ == "__main__":
    main()
