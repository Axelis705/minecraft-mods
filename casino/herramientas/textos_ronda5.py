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


# ---- ronda 8: mini tarjeta de socio, cofre de nivel, trofeos y fichas de colección
LV8 = (("bronze", ("Bronce", "Bronze", "Bronze", "Bronze", "Bronzo", "Bronze", "Бронза", "ブロンズ", "青铜")),
       ("silver", ("Plata", "Silver", "Silber", "Argent", "Argento", "Prata", "Серебро", "シルバー", "白银")),
       ("gold", ("Oro", "Gold", "Gold", "Or", "Oro", "Ouro", "Золото", "ゴールド", "黄金")),
       ("platinum", ("Platino", "Platinum", "Platin", "Platine", "Platino", "Platina", "Платина", "プラチナ", "铂金")),
       ("diamond", ("Diamante", "Diamond", "Diamant", "Diamant", "Diamante", "Diamante", "Бриллиант", "ダイヤモンド", "钻石")))
for lv, (es, en, de, fr, it, pt, ru, ja, zh) in LV8:
    k("block.casino.trophy_" + lv, f"Trofeo de socio {es}", f"{en} Member Trophy", f"Mitgliedstrophäe {de}", f"Trophée de membre {fr}",
      f"Trofeo socio {it}", f"Troféu de sócio {pt}", f"Трофей участника ({ru})", f"{ja}会員トロフィー", f"{zh}会员奖杯")
    k("item.casino.member_chip_" + lv, f"Ficha de colección {es}", f"{en} Collector Chip", f"Sammlerjeton {de}", f"Jeton de collection {fr}",
      f"Fiche da collezione {it}", f"Ficha de coleção {pt}", f"Коллекционная фишка ({ru})", f"{ja}記念チップ", f"{zh}纪念筹码")
k("casino.member.chip_tooltip", "Recuerdo de cuando llegaste a %s (no sirve para apostar)", "A keepsake from reaching %s (can't be bet)",
  "Andenken an das Erreichen von %s (nicht setzbar)", "Souvenir de ton passage au niveau %s (ne se mise pas)", "Ricordo del livello %s (non si punta)",
  "Lembrança de quando chegou a %s (não serve para apostar)", "Память о достижении уровня %s (нельзя ставить)", "%s到達の記念品(賭けには使えません)",
  "达到%s的纪念品(不能下注)")
k("casino.member.biggest", "Premio más grande", "Biggest prize", "Größter Gewinn", "Plus gros gain", "Vincita più grande", "Maior prêmio",
  "Самый большой выигрыш", "最大の賞金", "最大奖金")
k("casino.hud.join", "Haz tu primera apuesta para recibir tu tarjeta de socio", "Place your first bet to get your member card",
  "Platziere deine erste Wette, um deine Mitgliedskarte zu erhalten", "Fais ton premier pari pour recevoir ta carte de membre",
  "Fai la tua prima puntata per ricevere la tessera socio", "Faça sua primeira aposta para receber seu cartão de sócio",
  "Сделай первую ставку, чтобы получить карту участника", "最初のベットで会員カードがもらえます", "下第一注即可获得会员卡")
k("casino.hud.left", "Faltan %s para %s", "%s to go for %s", "Noch %s bis %s", "Encore %s pour %s", "Mancano %s per %s", "Faltam %s para %s",
  "До уровня %2$s: %1$s", "%2$sまであと%1$s", "距离%2$s还差%1$s")
k("casino.hud.reward", "Al subir: cofre + %s fichas", "Next up: chest + %s chips", "Aufstieg: Truhe + %s Jetons", "Au prochain : coffre + %s jetons",
  "Al prossimo: forziere + %s fiche", "Ao subir: baú + %s fichas", "Награда: сундук + %s фишек", "昇格で:宝箱+%sチップ", "升级奖励:宝箱+%s筹码")
k("casino.hud.total", "Total apostado: %s", "Total wagered: %s", "Insgesamt gesetzt: %s", "Total misé : %s", "Totale puntato: %s",
  "Total apostado: %s", "Всего поставлено: %s", "累計ベット:%s", "累计下注:%s")
k("casino.hud.ach", "Logros %s/%s", "Achievements %s/%s", "Erfolge %s/%s", "Succès %s/%s", "Obiettivi %s/%s", "Conquistas %s/%s",
  "Достижения %s/%s", "実績 %s/%s", "成就 %s/%s")
k("casino.hud.bonus", "¡Bono listo!", "Bonus ready!", "Bonus bereit!", "Bonus prêt !", "Bonus pronto!", "Bônus pronto!", "Бонус готов!",
  "ボーナス受取可!", "奖励可领取!")
k("casino.hud.hide", "%s: ocultar", "%s: hide", "%s: ausblenden", "%s : masquer", "%s: nascondi", "%s: ocultar", "%s: скрыть", "%s:隠す", "%s:隐藏")
k("key.casino.member_hud", "Mostrar/ocultar tarjeta de socio", "Show/hide member card", "Mitgliedskarte ein-/ausblenden",
  "Afficher/masquer la carte de membre", "Mostra/nascondi tessera socio", "Mostrar/ocultar cartão de sócio", "Показать/скрыть карту участника",
  "会員カードの表示切替", "显示/隐藏会员卡")
k("casino.chest.title", "Cofre de socio", "Member chest", "Mitgliedstruhe", "Coffre de membre", "Forziere socio", "Baú de sócio", "Сундук участника",
  "会員の宝箱", "会员宝箱")
k("casino.chest.level", "¡NIVEL %s!", "%s LEVEL!", "STUFE %s!", "NIVEAU %s !", "LIVELLO %s!", "NÍVEL %s!", "УРОВЕНЬ %s!", "%sレベル!", "%s等级!")
k("casino.chest.sub", "Tu premio por subir de nivel (ya está en tu inventario)", "Your level-up reward (already in your inventory)",
  "Deine Belohnung für den Aufstieg (schon im Inventar)", "Ta récompense de niveau (déjà dans ton inventaire)",
  "Il tuo premio di livello (già nell'inventario)", "Seu prêmio por subir de nível (já está no inventário)",
  "Награда за новый уровень (уже в инвентаре)", "レベルアップの報酬(すでにインベントリにあります)", "升级奖励(已放入背包)")
k("casino.chest.close", "Haz clic para cerrar", "Click to close", "Klicken zum Schließen", "Clique pour fermer", "Clicca per chiudere",
  "Clique para fechar", "Нажми, чтобы закрыть", "クリックで閉じる", "点击关闭")


# ---- ronda 9: robo a la bóveda
H = "casino.heist."
for name, es, en, de, fr, it, pt, ru, ja, zh in (
    ("security_camera", "Cámara de seguridad", "Security Camera", "Überwachungskamera", "Caméra de sécurité", "Telecamera di sicurezza", "Câmera de segurança", "Камера наблюдения", "監視カメラ", "监控摄像头"),
    ("laser_emitter", "Emisor láser", "Laser Emitter", "Laseremitter", "Émetteur laser", "Emettitore laser", "Emissor laser", "Лазерный излучатель", "レーザー発射器", "激光发射器"),
    ("card_reader", "Lector de tarjeta", "Card Reader", "Kartenleser", "Lecteur de carte", "Lettore di tessere", "Leitor de cartão", "Считыватель карт", "カードリーダー", "读卡器"),
    ("wall_safe", "Caja fuerte de pared", "Wall Safe", "Wandtresor", "Coffre-fort mural", "Cassaforte a muro", "Cofre de parede", "Настенный сейф", "壁金庫", "壁式保险箱"),
    ("clue_note", "Nota del personal", "Staff Note", "Personalnotiz", "Note du personnel", "Biglietto del personale", "Bilhete dos funcionários", "Записка персонала", "スタッフのメモ", "员工便条"),
    ("camera_console", "Consola de cámaras", "Camera Console", "Kamerakonsole", "Console des caméras", "Console telecamere", "Console de câmeras", "Пульт камер", "カメラ制御盤", "监控控制台"),
    ("cctv_monitor", "Monitor de cámaras", "CCTV Monitor", "Überwachungsmonitor", "Moniteur de surveillance", "Monitor di sorveglianza", "Monitor de câmeras", "Монитор наблюдения", "監視モニター", "监控显示器"),
    ("deposit_boxes", "Cajas de seguridad", "Safe Deposit Boxes", "Schließfächer", "Coffres de dépôt", "Cassette di sicurezza", "Cofres de depósito", "Банковские ячейки", "貸金庫", "保险柜格"),
    ("vault_door", "Puerta de la bóveda", "Vault Door", "Tresortür", "Porte de la chambre forte", "Porta del caveau", "Porta do cofre", "Дверь хранилища", "金庫の扉", "金库大门"),
    ("vault_seal", "Sello de la bóveda", "Vault Seal", "Tresorsiegel", "Scellé de la chambre forte", "Sigillo del caveau", "Selo do cofre", "Затвор хранилища", "金庫の封鎖", "金库封闭"),
    ("loot_cart", "Carrito de botín", "Loot Cart", "Beutewagen", "Chariot de butin", "Carrello del bottino", "Carrinho de saque", "Тележка с добычей", "戦利品カート", "战利品推车"),
    ("gas_vent", "Ducto de gas", "Gas Vent", "Gasdüse", "Bouche de gaz", "Bocchetta del gas", "Duto de gás", "Газовый клапан", "ガス噴出口", "毒气通风口")):
    k("block.casino." + name, es, en, de, fr, it, pt, ru, ja, zh)
k("item.casino.manager_keycard", "Tarjeta del gerente", "Manager's Keycard", "Schlüsselkarte des Managers", "Carte du directeur", "Tessera del direttore", "Cartão do gerente", "Карта управляющего", "支配人のカードキー", "经理门卡")
k("casino.npc.security", "Seguridad", "Security", "Sicherheit", "Sécurité", "Sicurezza", "Segurança", "Охрана", "警備員", "保安")
k("casino.npc.fixer", "El Contacto", "The Contact", "Der Kontakt", "Le Contact", "Il Contatto", "O Contato", "Связной", "連絡係", "联络人")
k("key.casino.heist_help", "Ayudar a seguridad (robo)", "Help security (heist)", "Sicherheit helfen (Raub)", "Aider la sécurité (braquage)", "Aiutare la sicurezza (colpo)", "Ajudar a segurança (assalto)", "Помочь охране (ограбление)", "警備を手伝う(強盗)", "协助保安(抢劫)")
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Psst... a los socios Diamante les tengo un trabajo especial.", "Psst... I've got a special job for Diamond members.", "Psst... für Diamant-Mitglieder hab ich einen Spezialjob.", "Psst... j'ai un boulot spécial pour les membres Diamant.", "Psst... ho un lavoretto speciale per i soci Diamante.", "Psiu... tenho um trabalho especial para sócios Diamante.", "Пс-с... для участников Бриллиант есть особое дельце.", "シッ…ダイヤモンド会員にだけ特別な仕事がある。", "嘘……钻石会员才有的特别活儿。"),
    ("Dicen que la bóveda está justo debajo de nosotros...", "They say the vault is right beneath us...", "Man sagt, der Tresor liegt direkt unter uns...", "On dit que la chambre forte est juste sous nos pieds...", "Dicono che il caveau sia proprio sotto di noi...", "Dizem que o cofre fica bem embaixo de nós...", "Говорят, хранилище прямо под нами...", "金庫はこの真下にあるらしい…", "听说金库就在我们脚下……"),
    ("El gerente siempre olvida su combinación. Por eso la apunta.", "The manager always forgets his combination. That's why he writes it down.", "Der Manager vergisst immer seine Kombination. Deshalb schreibt er sie auf.", "Le directeur oublie toujours sa combinaison. Alors il la note.", "Il direttore dimentica sempre la combinazione. Per questo la scrive.", "O gerente sempre esquece a combinação. Por isso anota.", "Управляющий вечно забывает код. Поэтому записывает.", "支配人は暗証番号をいつも忘れる。だからメモしてるのさ。", "经理总记不住密码,所以他会写下来。"),
    ("Agachado, las cámaras te ven solo de cerca.", "Crouch: cameras only spot you up close.", "Geduckt sehen dich Kameras nur aus der Nähe.", "Accroupi, les caméras ne te voient que de près.", "Accovacciato, le telecamere ti vedono solo da vicino.", "Agachado, as câmeras só te veem de perto.", "Пригнись: камеры видят тебя только вблизи.", "しゃがめばカメラは近くでしか気づかない。", "蹲下走,摄像头只能在近处发现你。"),
    ("Los láseres matan: brinca los de abajo, agáchate en los de arriba.", "Lasers kill: jump the low ones, crouch under the high ones.", "Über die unteren Laser springen, unter den oberen ducken.", "Saute les lasers bas, passe accroupi sous les hauts.", "Salta i laser bassi, passa accovacciato sotto quelli alti.", "Pule os lasers de baixo; passe agachado pelos de cima.", "Нижние лазеры перепрыгивай, под верхними — пригнись.", "低いレーザーは跳び越え、高いのはしゃがんでくぐれ。", "低处激光跳过去,高处激光蹲着过。"))):
    k("casino.tip.fixer." + str(i), es, en, de, fr, it, pt, ru, ja, zh)
k("casino.tip.fixer.say_0", "Tengo un plan. Escúchame bien.", "I've got a plan. Listen carefully.", "Ich hab einen Plan. Hör gut zu.", "J'ai un plan. Écoute bien.", "Ho un piano. Ascoltami bene.", "Tenho um plano. Escute bem.", "У меня есть план. Слушай внимательно.", "計画がある。よく聞け。", "我有个计划,仔细听好。")
k("casino.tip.fixer.say_1", "Todavía no es tu momento.", "It's not your time yet.", "Noch ist nicht deine Zeit.", "Ce n'est pas encore ton heure.", "Non è ancora il tuo momento.", "Ainda não é a sua hora.", "Твоё время ещё не пришло.", "まだお前の出番じゃない。", "还没到你出手的时候。")
k("casino.tip.security.say_0", "Circule, por favor.", "Move along, please.", "Bitte weitergehen.", "Circulez, s'il vous plaît.", "Circolare, prego.", "Circulando, por favor.", "Проходите, пожалуйста.", "立ち止まらないでください。", "请继续前进。")
k("casino.tip.security.say_1", "Zona restringida más adelante.", "Restricted area ahead.", "Sperrbereich voraus.", "Zone réglementée plus loin.", "Zona riservata più avanti.", "Área restrita adiante.", "Дальше закрытая зона.", "この先は立入禁止です。", "前方为限制区域。")
k("casino.tip.security.say_2", "¡Alto ahí! ¡Intruso!", "Freeze! Intruder!", "Stehen bleiben! Eindringling!", "Halte ! Intrus !", "Fermo! Intruso!", "Parado aí! Intruso!", "Стоять! Нарушитель!", "止まれ!侵入者だ!", "站住!有入侵者!")

def h(key, es, en, de, fr, it, pt, ru, ja, zh):
    k(H + key, es, en, de, fr, it, pt, ru, ja, zh)

h("banned", "Estás vetado del casino por el robo. Vuelve en %s min.", "You're banned from the casino for the heist. Come back in %s min.", "Du hast Casinoverbot wegen des Raubs. Komm in %s Min. wieder.", "Tu es banni du casino pour le braquage. Reviens dans %s min.", "Sei bandito dal casinò per il colpo. Torna tra %s min.", "Você está banido do cassino pelo assalto. Volte em %s min.", "Тебе запрещён вход в казино из-за ограбления. Возвращайся через %s мин.", "強盗のため出入り禁止。%s分後に戻ってきて。", "因抢劫被禁止进入赌场,%s分钟后再来。")
h("start", "¡Empieza el robo! Equipo de %s. Busca la puerta de servicio en el callejón de atrás.", "The heist begins! Crew of %s. Get in through the service door in the back yard.", "Der Coup beginnt! Team aus %s. Rein durch die Servicetür im Hinterhof.", "Le braquage commence ! Équipe de %s. Entre par la porte de service dans la cour arrière.", "Il colpo ha inizio! Squadra di %s. Entra dalla porta di servizio nel cortile.", "O assalto começou! Equipe de %s. Entre pela porta de serviço no pátio dos fundos.", "Ограбление началось! Команда из %s. Входите через служебную дверь на заднем дворе.", "強盗開始!%s人のチーム。裏庭の業務用ドアから入れ。", "抢劫开始!%s人小队。从后院的员工门进去。")
h("obj.door_hint", "Pista: la caseta gris junto al casino, en el callejón de atrás. Agáchate cerca de las cámaras.", "Hint: the gray shed next to the casino. Crouch near cameras.", "Tipp: der graue Schuppen am Casino. Bei Kameras ducken.", "Indice : la cabane grise collée au casino. Accroupis-toi près des caméras.", "Indizio: il casotto grigio accanto al casinò. Accovacciati vicino alle telecamere.", "Dica: a cabine cinza ao lado do cassino. Agache perto das câmeras.", "Подсказка: серая будка у казино. Пригибайся возле камер.", "ヒント:カジノ横の灰色の小屋。カメラの近くではしゃがめ。", "提示:赌场旁边的灰色小屋。靠近摄像头时蹲下。")
h("alarm.seen", "¡Te vieron! ¡Suena la alarma!", "You've been spotted! The alarm is ringing!", "Du wurdest gesehen! Alarm!", "On t'a repéré ! L'alarme sonne !", "Ti hanno visto! Scatta l'allarme!", "Te viram! O alarme disparou!", "Тебя заметили! Тревога!", "見つかった!警報が鳴った!", "你被发现了!警报响了!")
h("alarm.laser", "¡Cruzaste un láser! ¡Alarma!", "You tripped a laser! Alarm!", "Laser ausgelöst! Alarm!", "Tu as coupé un laser ! Alarme !", "Hai attraversato un laser! Allarme!", "Você cruzou um laser! Alarme!", "Ты задел лазер! Тревога!", "レーザーに触れた!警報!", "你触碰了激光!警报!")
h("alarm.caught", "¡Un guardia te atrapó! ¡Alarma!", "A guard caught you! Alarm!", "Eine Wache hat dich erwischt! Alarm!", "Un garde t'a attrapé ! Alarme !", "Una guardia ti ha beccato! Allarme!", "Um guarda te pegou! Alarme!", "Охранник тебя поймал! Тревога!", "警備員に見つかった!警報!", "被保安抓住了!警报!")
h("staff_only", "Solo personal autorizado.", "Authorized staff only.", "Nur für befugtes Personal.", "Personnel autorisé uniquement.", "Solo personale autorizzato.", "Somente pessoal autorizado.", "Только для персонала.", "関係者以外立入禁止。", "仅限授权员工。")
h("need_card", "Necesitas la tarjeta del gerente.", "You need the manager's keycard.", "Du brauchst die Schlüsselkarte des Managers.", "Il te faut la carte du directeur.", "Ti serve la tessera del direttore.", "Você precisa do cartão do gerente.", "Нужна карта управляющего.", "支配人のカードキーが必要だ。", "你需要经理的门卡。")
h("note_idle", "Una lista de turnos del personal.", "A staff shift schedule.", "Ein Dienstplan des Personals.", "Un planning des équipes.", "Un elenco dei turni del personale.", "Uma escala de turnos dos funcionários.", "График смен персонала.", "スタッフのシフト表だ。", "员工排班表。")
h("clue", "Pista %s: en la nota dice \"%s\". ¡Es parte de la combinación!", "Clue %s: the note says \"%s\". It's part of the combination!", "Hinweis %s: Auf dem Zettel steht \"%s\". Teil der Kombination!", "Indice %s : la note dit \"%s\". C'est une partie de la combinaison !", "Indizio %s: il biglietto dice \"%s\". È parte della combinazione!", "Pista %s: o bilhete diz \"%s\". É parte da combinação!", "Подсказка %s: в записке «%s». Это часть кода!", "手がかり%s:メモには「%s」。組み合わせの一部だ!", "线索%s:便条上写着“%s”,是密码的一部分!")
h("safe_empty", "La caja fuerte está vacía.", "The safe is empty.", "Der Tresor ist leer.", "Le coffre est vide.", "La cassaforte è vuota.", "O cofre está vazio.", "Сейф пуст.", "金庫は空だ。", "保险箱是空的。")
h("safe_locked", "Una caja fuerte bien cerrada.", "A tightly locked safe.", "Ein gut verschlossener Tresor.", "Un coffre bien verrouillé.", "Una cassaforte ben chiusa.", "Um cofre bem trancado.", "Надёжно запертый сейф.", "しっかり施錠された金庫。", "锁得很紧的保险箱。")
h("console_idle", "Las cámaras del casino.", "The casino's cameras.", "Die Kameras des Casinos.", "Les caméras du casino.", "Le telecamere del casinò.", "As câmeras do cassino.", "Камеры казино.", "カジノの監視カメラ。", "赌场的摄像头。")
h("cams_already", "Las cámaras ya están apagadas.", "The cameras are already off.", "Die Kameras sind schon aus.", "Les caméras sont déjà coupées.", "Le telecamere sono già spente.", "As câmeras já estão desligadas.", "Камеры уже выключены.", "カメラはもう止まっている。", "摄像头已经关了。")
h("reader_wait", "Tarjeta lista... ¡alguien más debe pasar la suya en el otro lector ya!", "Card in... someone else must swipe the other reader now!", "Karte drin... jemand anderes muss jetzt den anderen Leser nutzen!", "Carte passée... quelqu'un doit passer l'autre lecteur maintenant !", "Tessera inserita... qualcun altro deve usare l'altro lettore ora!", "Cartão passado... outra pessoa deve passar no outro leitor agora!", "Карта считана... кто-то должен сейчас приложить карту ко второму считывателю!", "カードOK…今すぐ別の誰かがもう一方のリーダーに通せ!", "已刷卡……另一个人必须马上刷另一台读卡器!")
h("vault_hint", "Pasa la tarjeta en los lectores para abrirla.", "Swipe the keycard on the readers to open it.", "Zieh die Karte durch die Leser, um sie zu öffnen.", "Passe la carte dans les lecteurs pour l'ouvrir.", "Passa la tessera nei lettori per aprirla.", "Passe o cartão nos leitores para abrir.", "Приложи карту к считывателям, чтобы открыть.", "リーダーにカードを通して開けろ。", "在读卡器上刷卡来打开它。")
h("vault_idle", "La bóveda del casino. Ni lo pienses.", "The casino vault. Don't even think about it.", "Der Casinotresor. Nicht mal dran denken.", "La chambre forte du casino. N'y pense même pas.", "Il caveau del casinò. Non pensarci nemmeno.", "O cofre do cassino. Nem pense nisso.", "Хранилище казино. Даже не думай.", "カジノの金庫。考えるだけ無駄だ。", "赌场金库。想都别想。")
h("cart_idle", "Un carrito lleno de dinero...", "A cart full of money...", "Ein Wagen voller Geld...", "Un chariot plein d'argent...", "Un carrello pieno di soldi...", "Um carrinho cheio de dinheiro...", "Тележка, полная денег...", "お金でいっぱいのカート…", "装满钱的推车……")
h("cart_empty", "Este carrito ya está vacío.", "This cart is already empty.", "Dieser Wagen ist schon leer.", "Ce chariot est déjà vide.", "Questo carrello è già vuoto.", "Este carrinho já está vazio.", "Эта тележка уже пуста.", "このカートはもう空だ。", "这辆推车已经空了。")
h("bag_full", "¡Tu mochila está llena! Vámonos.", "Your bag is full! Let's go.", "Deine Tasche ist voll! Los geht's.", "Ton sac est plein ! On y va.", "La tua borsa è piena! Andiamo.", "Sua mochila está cheia! Vamos.", "Сумка полна! Уходим.", "バッグがいっぱいだ!行くぞ。", "你的包满了!走吧。")
h("door_open", "¡Puerta abierta!", "Door open!", "Tür offen!", "Porte ouverte !", "Porta aperta!", "Porta aberta!", "Дверь открыта!", "ドアが開いた!", "门开了!")
h("obj.clues_hint", "Busca 3 notas con la combinación (oficina, vestidores, comedor) y abre la caja fuerte del gerente.", "Find 3 notes with the combination (office, lockers, break room) and open the manager's safe.", "Finde 3 Zettel mit der Kombination (Büro, Umkleide, Pausenraum) und öffne den Tresor des Managers.", "Trouve 3 notes avec la combinaison (bureau, vestiaires, salle de pause) et ouvre le coffre du directeur.", "Trova 3 biglietti con la combinazione (ufficio, spogliatoi, sala pausa) e apri la cassaforte del direttore.", "Encontre 3 bilhetes com a combinação (escritório, vestiários, copa) e abra o cofre do gerente.", "Найди 3 записки с кодом (кабинет, раздевалка, комната отдыха) и открой сейф управляющего.", "組み合わせのメモ3枚(事務所・更衣室・休憩室)を探し、支配人の金庫を開けろ。", "找到3张写有密码的便条(办公室、更衣室、休息室),打开经理的保险箱。")
h("lockpick_fail", "¡Se rompió la ganzúa! Hiciste ruido.", "The lockpick broke! You made noise.", "Der Dietrich ist abgebrochen! Das war laut.", "Le crochet a cassé ! Tu as fait du bruit.", "Il grimaldello si è rotto! Hai fatto rumore.", "A gazua quebrou! Você fez barulho.", "Отмычка сломалась! Ты нашумел.", "ピッキングツールが折れた!音を立てた。", "撬锁工具断了!你弄出了声响。")
h("safe_open", "¡%s abrió la caja fuerte! Tiene las tarjetas del gerente: a la escalera.", "%s opened the safe! They have the manager's keycards: to the stairs.", "%s hat den Tresor geöffnet! Mit den Karten zur Treppe.", "%s a ouvert le coffre ! Avec les cartes du directeur : à l'escalier.", "%s ha aperto la cassaforte! Con le tessere del direttore: alle scale.", "%s abriu o cofre! Com os cartões do gerente: para a escada.", "%s открыл сейф! Карты управляющего у него: к лестнице.", "%sが金庫を開けた!支配人のカードを持って階段へ。", "%s打开了保险箱!拿着经理门卡去楼梯。")
h("safe_wrong", "Combinación incorrecta.", "Wrong combination.", "Falsche Kombination.", "Mauvaise combinaison.", "Combinazione sbagliata.", "Combinação errada.", "Неверный код.", "組み合わせが違う。", "密码错误。")
h("cams_off", "¡Cámaras apagadas por %s segundos!", "Cameras off for %s seconds!", "Kameras für %s Sekunden aus!", "Caméras coupées pendant %s secondes !", "Telecamere spente per %s secondi!", "Câmeras desligadas por %s segundos!", "Камеры выключены на %s секунд!", "カメラを%s秒間停止!", "摄像头关闭%s秒!")
h("hack_fail", "El hackeo falló. Alguien lo notó.", "The hack failed. Someone noticed.", "Der Hack ist fehlgeschlagen. Jemand hat es bemerkt.", "Le piratage a échoué. Quelqu'un l'a remarqué.", "L'hackeraggio è fallito. Qualcuno se n'è accorto.", "O hack falhou. Alguém percebeu.", "Взлом не удался. Кто-то заметил.", "ハッキング失敗。誰かが気づいた。", "黑客入侵失败,有人察觉了。")
h("grabbed", "+%s (mochila %s/%s)", "+%s (bag %s/%s)", "+%s (Tasche %s/%s)", "+%s (sac %s/%s)", "+%s (borsa %s/%s)", "+%s (mochila %s/%s)", "+%s (сумка %s/%s)", "+%s(バッグ %s/%s)", "+%s(包 %s/%s)")
h("vault_opening", "¡Los lectores aceptaron! La bóveda se está abriendo...", "The readers accepted! The vault is opening...", "Die Leser haben akzeptiert! Der Tresor öffnet sich...", "Les lecteurs ont accepté ! La chambre forte s'ouvre...", "I lettori hanno accettato! Il caveau si sta aprendo...", "Os leitores aceitaram! O cofre está abrindo...", "Считыватели приняли карты! Хранилище открывается...", "リーダーが認証!金庫が開いていく…", "读卡器通过了!金库正在打开……")
h("timeout", "Tardaron demasiado: el robo se canceló.", "You took too long: the heist was called off.", "Zu lange gebraucht: Der Coup wurde abgeblasen.", "Trop lent : le braquage est annulé.", "Ci avete messo troppo: il colpo è annullato.", "Demoraram demais: o assalto foi cancelado.", "Слишком долго: ограбление отменено.", "時間切れ:強盗は中止だ。", "太慢了:抢劫取消。")
h("vault_open", "¡La bóveda está abierta! Tienen %s segundos para saquear.", "The vault is open! You have %s seconds to loot.", "Der Tresor ist offen! Ihr habt %s Sekunden zum Plündern.", "La chambre forte est ouverte ! Vous avez %s secondes pour piller.", "Il caveau è aperto! Avete %s secondi per saccheggiare.", "O cofre está aberto! Vocês têm %s segundos para saquear.", "Хранилище открыто! У вас %s секунд на добычу.", "金庫が開いた!略奪できるのは%s秒!", "金库开了!你们有%s秒抢东西!")
h("vault_closing", "¡La bóveda se cierra en %s segundos!", "The vault closes in %s seconds!", "Der Tresor schließt in %s Sekunden!", "La chambre forte se ferme dans %s secondes !", "Il caveau si chiude tra %s secondi!", "O cofre fecha em %s segundos!", "Хранилище закроется через %s секунд!", "あと%s秒で金庫が閉まる!", "金库将在%s秒后关闭!")
h("escape", "¡La bóveda se cerró y sonó la alarma! ¡Escapen del casino!", "The vault closed and the alarm went off! Get out of the casino!", "Der Tresor ist zu und der Alarm läuft! Raus aus dem Casino!", "La chambre forte s'est fermée et l'alarme sonne ! Sortez du casino !", "Il caveau si è chiuso ed è scattato l'allarme! Fuggite dal casinò!", "O cofre fechou e o alarme tocou! Fujam do cassino!", "Хранилище закрылось, сработала тревога! Бегите из казино!", "金庫が閉じて警報が鳴った!カジノから脱出しろ!", "金库关闭,警报响起!逃出赌场!")
h("gas", "¡Te quedaste adentro! El gas...", "You got locked in! The gas...", "Du bist eingesperrt! Das Gas...", "Tu es resté enfermé ! Le gaz...", "Sei rimasto chiuso dentro! Il gas...", "Você ficou preso! O gás...", "Ты остался внутри! Газ...", "閉じ込められた!ガスが…", "你被关在里面了!毒气……")
h("prize", "¡Robo a la bóveda!", "Vault heist!", "Tresorcoup!", "Braquage de la chambre forte !", "Colpo al caveau!", "Assalto ao cofre!", "Ограбление хранилища!", "金庫強盗!", "金库大劫案!")
h("escaped", "¡Escapaste con el botín! %s fichas.", "You escaped with the loot! %s chips.", "Mit der Beute entkommen! %s Jetons.", "Tu t'es échappé avec le butin ! %s jetons.", "Sei scappato col bottino! %s fiche.", "Você fugiu com o saque! %s fichas.", "Ты сбежал с добычей! %s фишек.", "戦利品を持って逃げ切った!%sチップ。", "你带着战利品逃脱了!%s筹码。")
h("crew_escaped", "%s escapó con su parte.", "%s escaped with their share.", "%s ist mit seinem Anteil entkommen.", "%s s'est échappé avec sa part.", "%s è scappato con la sua parte.", "%s fugiu com a parte dele.", "%s ушёл со своей долей.", "%sは分け前を持って逃げた。", "%s带着他的份额逃走了。")
h("caught", "Te atraparon. Perdiste el botín y estás vetado del casino por un día.", "You got caught. You lost the loot and are banned from the casino for a day.", "Du wurdest geschnappt. Beute weg, ein Tag Casinoverbot.", "Tu t'es fait prendre. Butin perdu et banni du casino pour une journée.", "Ti hanno preso. Bottino perso e bandito dal casinò per un giorno.", "Você foi pego. Perdeu o saque e está banido do cassino por um dia.", "Тебя поймали. Добыча потеряна, вход в казино закрыт на день.", "捕まった。戦利品を失い、1日カジノ出入り禁止だ。", "你被抓了。战利品没了,禁止进入赌场一天。")
h("crew_down", "¡Cayó %s!", "%s is down!", "%s ist erledigt!", "%s est tombé !", "%s è caduto!", "%s caiu!", "%s выбыл!", "%sがやられた!", "%s倒下了!")
h("cop_joined", "¡Ahora eres de seguridad! Atrapa a los ladrones que brillan.", "You're with security now! Catch the glowing robbers.", "Du gehörst jetzt zur Sicherheit! Schnapp die leuchtenden Räuber.", "Tu fais partie de la sécurité ! Attrape les voleurs qui brillent.", "Ora sei della sicurezza! Prendi i ladri che brillano.", "Agora você é da segurança! Pegue os ladrões brilhando.", "Теперь ты в охране! Лови светящихся грабителей.", "警備員になった!光っている強盗を捕まえろ。", "你现在是保安了!抓住发光的劫匪。")
h("cops_over", "El robo terminó. ¡Gracias por ayudar!", "The heist is over. Thanks for helping!", "Der Raub ist vorbei. Danke für die Hilfe!", "Le braquage est terminé. Merci pour ton aide !", "Il colpo è finito. Grazie dell'aiuto!", "O assalto acabou. Obrigado pela ajuda!", "Ограбление окончено. Спасибо за помощь!", "強盗は終わった。協力ありがとう!", "抢劫结束了。谢谢帮忙!")
h("cop_reward", "¡Atrapaste a %s! Recompensa: %s fichas.", "You caught %s! Reward: %s chips.", "Du hast %s geschnappt! Belohnung: %s Jetons.", "Tu as attrapé %s ! Récompense : %s jetons.", "Hai preso %s! Ricompensa: %s fiche.", "Você pegou %s! Recompensa: %s fichas.", "Ты поймал %s! Награда: %s фишек.", "%sを捕まえた!報酬:%sチップ。", "你抓住了%s!奖励:%s筹码。")
h("cop_assist", "Seguridad atrapó a %s. Tu parte: %s fichas.", "Security caught %s. Your share: %s chips.", "Die Sicherheit hat %s geschnappt. Dein Anteil: %s Jetons.", "La sécurité a attrapé %s. Ta part : %s jetons.", "La sicurezza ha preso %s. La tua parte: %s fiche.", "A segurança pegou %s. Sua parte: %s fichas.", "Охрана поймала %s. Твоя доля: %s фишек.", "警備が%sを捕まえた。分け前:%sチップ。", "保安抓住了%s。你的份额:%s筹码。")
h("hud.offer", "¡Robo en curso! %s ladrones escapan", "Heist in progress! %s robbers escaping", "Raub im Gange! %s Räuber fliehen", "Braquage en cours ! %s voleurs s'enfuient", "Colpo in corso! %s ladri in fuga", "Assalto em andamento! %s ladrões fugindo", "Идёт ограбление! Убегают грабители: %s", "強盗発生!%s人が逃走中", "抢劫进行中!%s名劫匪在逃")
h("hud.offer_key", "Pulsa %s para ayudar a seguridad", "Press %s to help security", "Drücke %s, um der Sicherheit zu helfen", "Appuie sur %s pour aider la sécurité", "Premi %s per aiutare la sicurezza", "Aperte %s para ajudar a segurança", "Нажми %s, чтобы помочь охране", "%sで警備を手伝う", "按%s协助保安")
h("hud.title", "ROBO A LA BÓVEDA", "VAULT HEIST", "TRESORCOUP", "BRAQUAGE", "COLPO AL CAVEAU", "ASSALTO AO COFRE", "ОГРАБЛЕНИЕ", "金庫強盗", "金库劫案")
h("hud.cop_title", "SEGURIDAD", "SECURITY", "SICHERHEIT", "SÉCURITÉ", "SICUREZZA", "SEGURANÇA", "ОХРАНА", "警備", "保安")
h("hud.alarm", "¡ALARMA!", "ALARM!", "ALARM!", "ALARME !", "ALLARME!", "ALARME!", "ТРЕВОГА!", "警報!", "警报!")
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Entra por la puerta de servicio (patio trasero)", "Enter through the service door (back yard)", "Durch die Servicetür rein (Hinterhof)", "Entre par la porte de service (cour arrière)", "Entra dalla porta di servizio (cortile)", "Entre pela porta de serviço (fundos)", "Войди через служебную дверь (задний двор)", "業務用ドアから侵入(裏庭)", "从员工门进入(后院)"),
    ("Busca las 3 notas con la combinación", "Find the 3 notes with the combination", "Finde die 3 Zettel mit der Kombination", "Trouve les 3 notes avec la combinaison", "Trova i 3 biglietti con la combinazione", "Encontre os 3 bilhetes com a combinação", "Найди 3 записки с кодом", "組み合わせのメモを3枚探せ", "找到3张写有密码的便条"),
    ("Abre la caja fuerte de la oficina del gerente", "Open the safe in the manager's office", "Öffne den Tresor im Büro des Managers", "Ouvre le coffre du bureau du directeur", "Apri la cassaforte nell'ufficio del direttore", "Abra o cofre do escritório do gerente", "Открой сейф в кабинете управляющего", "支配人室の金庫を開けろ", "打开经理办公室的保险箱"),
    ("Usa la tarjeta en el lector de la escalera", "Use the keycard on the stairway reader", "Benutze die Karte am Leser der Treppe", "Utilise la carte sur le lecteur de l'escalier", "Usa la tessera sul lettore delle scale", "Use o cartão no leitor da escada", "Приложи карту к считывателю у лестницы", "階段のリーダーにカードを通せ", "在楼梯读卡器上刷卡"),
    ("Hackea la consola de la compuerta (sala de guardias)", "Hack the gate console (guard room)", "Hacke die Torkonsole (Wachraum)", "Pirate la console de la grille (salle des gardes)", "Hackera la console del cancello (sala guardie)", "Hackeie o console do portão (sala dos guardas)", "Взломай пульт ворот (комната охраны)", "ゲートの制御盤をハッキング(警備室)", "黑入闸门控制台(警卫室)"),
    ("Baja, cruza los láseres (¡matan!) y pasa las tarjetas en los 2 lectores", "Go down, cross the lasers (deadly!) and swipe both readers", "Runter, Laser überwinden (tödlich!), beide Leser benutzen", "Descends, passe les lasers (mortels !) et les 2 lecteurs", "Scendi, supera i laser (letali!) e usa i 2 lettori", "Desça, passe os lasers (mortais!) e use os 2 leitores", "Спустись, пройди лазеры (смертельно!) и приложи карты к 2 считывателям", "下へ、レーザー(即死!)を越え、2台のリーダーに通せ", "下去,穿过激光(致命!),在两台读卡器上刷卡"),
    ("La bóveda se está abriendo...", "The vault is opening...", "Der Tresor öffnet sich...", "La chambre forte s'ouvre...", "Il caveau si sta aprendo...", "O cofre está abrindo...", "Хранилище открывается...", "金庫が開いていく…", "金库正在打开……"),
    ("¡Saquea los carritos antes de que se cierre!", "Loot the carts before it closes!", "Plündere die Wagen, bevor er schließt!", "Pille les chariots avant la fermeture !", "Saccheggia i carrelli prima che si chiuda!", "Saqueie os carrinhos antes que feche!", "Обчисти тележки, пока не закрылось!", "閉まる前にカートを略奪しろ!", "在关门前洗劫推车!"),
    ("¡Escapa! Sal del casino con vida", "Escape! Get out of the casino alive", "Flieh! Verlass das Casino lebend", "Fuis ! Sors du casino vivant", "Scappa! Esci vivo dal casinò", "Fuja! Saia do cassino com vida", "Беги! Выберись из казино живым", "逃げろ!生きてカジノを出ろ", "快逃!活着离开赌场"),
    ("Atrapa a los ladrones que brillan (%s quedan)", "Catch the glowing robbers (%s left)", "Fang die leuchtenden Räuber (%s übrig)", "Attrape les voleurs qui brillent (%s restants)", "Prendi i ladri che brillano (ne restano %s)", "Pegue os ladrões brilhando (%s restantes)", "Поймай светящихся грабителей (осталось %s)", "光る強盗を捕まえろ(残り%s人)", "抓住发光的劫匪(剩余%s人)"))):
    h("hud.obj_" + str(i), es, en, de, fr, it, pt, ru, ja, zh)
h("laser_kill", "¡Un láser te desintegró!", "A laser disintegrated you!", "Ein Laser hat dich aufgelöst!", "Un laser t'a désintégré !", "Un laser ti ha disintegrato!", "Um laser te desintegrou!", "Лазер испепелил тебя!", "レーザーで消し飛んだ!", "你被激光汽化了!")
h("gate_idle", "La consola de la compuerta. Está bloqueada.", "The gate console. It's locked.", "Die Torkonsole. Gesperrt.", "La console de la grille. Verrouillée.", "La console del cancello. È bloccata.", "O console do portão. Está travado.", "Пульт ворот. Заблокирован.", "ゲートの制御盤。ロックされている。", "闸门控制台,已锁定。")
h("gate_already", "La compuerta ya está abierta.", "The gate is already open.", "Das Tor ist schon offen.", "La grille est déjà ouverte.", "Il cancello è già aperto.", "O portão já está aberto.", "Ворота уже открыты.", "ゲートはもう開いている。", "闸门已经开了。")
h("gate_open", "¡Compuerta abierta! Abajo están los láseres y la bóveda.", "Gate open! The lasers and the vault are below.", "Tor offen! Unten warten Laser und Tresor.", "Grille ouverte ! En bas : lasers et chambre forte.", "Cancello aperto! Sotto ci sono i laser e il caveau.", "Portão aberto! Lá embaixo: lasers e o cofre.", "Ворота открыты! Внизу лазеры и хранилище.", "ゲートが開いた!下にレーザーと金庫がある。", "闸门开了!下面是激光和金库。")
h("hud.suspicion", "Sospecha", "Suspicion", "Verdacht", "Soupçon", "Sospetto", "Suspeita", "Подозрение", "疑念", "怀疑")
h("hud.combo", "Combinación: %s", "Combination: %s", "Kombination: %s", "Combinaison : %s", "Combinazione: %s", "Combinação: %s", "Код: %s", "組み合わせ:%s", "密码:%s")
h("hud.vault_time", "Bóveda: %s", "Vault: %s", "Tresor: %s", "Chambre forte : %s", "Caveau: %s", "Cofre: %s", "Хранилище: %s", "金庫:%s", "金库:%s")
h("hud.cams_off", "Cámaras apagadas: %s", "Cameras off: %s", "Kameras aus: %s", "Caméras coupées : %s", "Telecamere spente: %s", "Câmeras desligadas: %s", "Камеры выкл.: %s", "カメラ停止:%s", "摄像头关闭:%s")
h("hud.bag", "Mochila: %s", "Bag: %s", "Tasche: %s", "Sac : %s", "Borsa: %s", "Mochila: %s", "Сумка: %s", "バッグ:%s", "包:%s")
h("plan.title", "Robo a la bóveda", "The vault heist", "Der Tresorcoup", "Le braquage de la chambre forte", "Il colpo al caveau", "O assalto ao cofre", "Ограбление хранилища", "金庫強盗の計画", "金库劫案计划")
h("plan.go", "¡Hagámoslo!", "Let's do it!", "Machen wir's!", "Allons-y !", "Facciamolo!", "Vamos nessa!", "Начинаем!", "やるぞ!", "动手吧!")
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Entra por la puerta de servicio con una ganzúa.", "Pick the lock of the service door.", "Knacke das Schloss der Servicetür.", "Crochète la porte de service.", "Scassina la porta di servizio.", "Arrombe a porta de serviço.", "Вскрой служебную дверь отмычкой.", "業務用ドアをピッキング。", "撬开员工门的锁。"),
    ("Evita cámaras y guardias. Agáchate.", "Avoid cameras and guards. Crouch.", "Meide Kameras und Wachen. Ducken.", "Évite caméras et gardes. Accroupis-toi.", "Evita telecamere e guardie. Accovacciati.", "Evite câmeras e guardas. Agache.", "Избегай камер и охраны. Пригибайся.", "カメラと警備を避けろ。しゃがめ。", "避开摄像头和保安。蹲下。"),
    ("Junta 3 notas y abre la caja del gerente.", "Collect 3 notes, open the manager's safe.", "3 Zettel finden, Managertresor öffnen.", "3 notes, puis le coffre du directeur.", "3 biglietti, poi la cassaforte del direttore.", "3 bilhetes e abra o cofre do gerente.", "Собери 3 записки, открой сейф.", "メモ3枚で支配人の金庫を開けろ。", "集齐3张便条,打开经理保险箱。"),
    ("Baja con su tarjeta y hackea la compuerta.", "Go down with the keycard, hack the gate.", "Mit der Karte runter. Laser!", "Avec la carte, descends. Lasers !", "Con la tessera scendi. Laser!", "Com o cartão, desça. Lasers!", "С картой вниз. Лазеры!", "カードで地下へ。レーザーに注意!", "用门卡下去。小心激光!"),
    ("2 lectores a la vez (solo, basta uno).", "2 readers at once (solo: just one).", "2 Leser gleichzeitig (allein: einer).", "2 lecteurs en même temps (seul : un).", "2 lettori insieme (da solo: uno).", "2 leitores juntos (sozinho: um).", "2 считывателя сразу (одному — один).", "2台同時に(1人なら1台)。", "两台读卡器同时刷(单人刷一台)。"),
    ("Saquea y escapa. ¡Ojo con el gas!", "Loot and escape. Watch the gas!", "Plündern und fliehen. Vorsicht Gas!", "Pille et fuis. Attention au gaz !", "Saccheggia e scappa. Occhio al gas!", "Saqueie e fuja. Cuidado com o gás!", "Грабь и беги. Осторожно, газ!", "略奪して逃げろ。ガスに注意!", "抢完就跑。小心毒气!"))):
    h("plan.step_" + str(i), es, en, de, fr, it, pt, ru, ja, zh)
h("plan.crew", "Equipo (a 8 bloques)", "Crew (within 8 blocks)", "Team (innerhalb 8 Blöcken)", "Équipe (à moins de 8 blocs)", "Squadra (entro 8 blocchi)", "Equipe (a até 8 blocos)", "Команда (в 8 блоках)", "チーム(8ブロック以内)", "队伍(8格以内)")
h("plan.rules", "100 s en la bóveda. Mochila de 8 espacios (el oro ocupa 2). Si te atrapan pierdes el botín y te vetan 1 día. Otros jugadores pueden ayudar a seguridad.", "100 s in the vault. 8-slot bag (gold takes 2). Caught: you lose the loot and get a 1-day ban. Other players can help security.", "100 Sekunden im Tresor. Tasche mit 8 Plätzen (Gold zählt doppelt). Erwischt: Beute weg, ein Tag Verbot. Andere können der Sicherheit helfen.", "100 secondes dans la chambre forte. Sac de 8 places (l'or compte double). Pris : butin perdu, banni un jour. Les autres peuvent aider la sécurité.", "100 secondi nel caveau. Borsa da 8 posti (l'oro pesa doppio). Se ti prendono perdi il bottino e sei bandito un giorno. Gli altri possono aiutare la sicurezza.", "100 segundos no cofre. Mochila de 8 espaços (ouro pesa o dobro). Se for pego, perde o saque e fica banido por um dia. Outros podem ajudar a segurança.", "100 секунд в хранилище. Сумка на 8 мест (золото весит вдвое). Поймают — потеряешь добычу и бан на день. Другие могут помочь охране.", "金庫内は100秒。バッグは8枠(金は2枠)。捕まると戦利品を失い1日出入り禁止。他のプレイヤーは警備に加われる。", "金库内100秒。包有8格(金条占2格)。被抓就失去战利品并被禁一天。其他玩家可以协助保安。")
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Solo para socios Diamante.", "Diamond members only.", "Nur für Diamant-Mitglieder.", "Réservé aux membres Diamant.", "Solo per soci Diamante.", "Só para sócios Diamante.", "Только для участников Бриллиант.", "ダイヤモンド会員限定。", "仅限钻石会员。"),
    ("Estás vetado del casino.", "You're banned from the casino.", "Du hast Casinoverbot.", "Tu es banni du casino.", "Sei bandito dal casinò.", "Você está banido do cassino.", "Тебе запрещён вход в казино.", "カジノ出入り禁止中。", "你被禁止进入赌场。"),
    ("Ya hay un robo en marcha.", "A heist is already underway.", "Es läuft schon ein Coup.", "Un braquage est déjà en cours.", "C'è già un colpo in corso.", "Já há um assalto em andamento.", "Ограбление уже идёт.", "すでに強盗が進行中だ。", "已经有抢劫在进行了。"),
    ("La bóveda ya fue robada hoy. Vuelve mañana.", "The vault was already hit today. Come back tomorrow.", "Der Tresor wurde heute schon ausgeraubt. Komm morgen wieder.", "La chambre forte a déjà été braquée aujourd'hui. Reviens demain.", "Il caveau è già stato colpito oggi. Torna domani.", "O cofre já foi roubado hoje. Volte amanhã.", "Хранилище сегодня уже грабили. Приходи завтра.", "今日はもう襲われた。明日また来い。", "金库今天已经被抢过了,明天再来。"),
    ("Este casino no tiene bóveda.", "This casino has no vault.", "Dieses Casino hat keinen Tresor.", "Ce casino n'a pas de chambre forte.", "Questo casinò non ha un caveau.", "Este cassino não tem cofre.", "В этом казино нет хранилища.", "このカジノに金庫はない。", "这个赌场没有金库。"))):
    h("plan.no_" + str(i + 1), es, en, de, fr, it, pt, ru, ja, zh)
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Ganzúa", "Lockpick", "Dietrich", "Crochetage", "Grimaldello", "Gazua", "Отмычка", "ピッキング", "撬锁"),
    ("Caja fuerte", "Safe", "Tresor", "Coffre-fort", "Cassaforte", "Cofre", "Сейф", "金庫", "保险箱"),
    ("Hackeo de cámaras", "Camera hack", "Kamera-Hack", "Piratage des caméras", "Hack telecamere", "Hack das câmeras", "Взлом камер", "カメラハッキング", "黑入摄像头"),
    ("Saqueo", "Looting", "Plündern", "Pillage", "Saccheggio", "Saque", "Добыча", "略奪", "抢夺"))):
    h("game." + str(i), es, en, de, fr, it, pt, ru, ja, zh)
h("game.open", "Abrir", "Open", "Öffnen", "Ouvrir", "Apri", "Abrir", "Открыть", "開ける", "打开")
h("game.hint_press", "ESPACIO o clic cuando la aguja esté en verde", "SPACE or click when the needle is in the green", "LEERTASTE oder Klick, wenn die Nadel im Grünen ist", "ESPACE ou clic quand l'aiguille est dans le vert", "SPAZIO o clic quando l'ago è nel verde", "ESPAÇO ou clique quando a agulha estiver no verde", "ПРОБЕЛ или клик, когда стрелка в зелёной зоне", "針が緑のときにスペースかクリック", "指针在绿区时按空格或点击")
h("game.hint_hack", "Sincroniza los 4 canales (ESPACIO en verde)", "Sync all 4 channels (SPACE in the green)", "Synchronisiere alle 4 Kanäle (LEERTASTE im Grünen)", "Synchronise les 4 canaux (ESPACE dans le vert)", "Sincronizza i 4 canali (SPAZIO nel verde)", "Sincronize os 4 canais (ESPAÇO no verde)", "Синхронизируй 4 канала (ПРОБЕЛ в зелёной зоне)", "4チャンネルを同期(緑でスペース)", "同步全部4个频道(绿区按空格)")
h("game.hint_loot", "Dorado: todo · Verde: casi todo · Fuera: poco", "Gold: all · Green: most · Outside: a little", "Gold: alles · Grün: fast alles · Außen: wenig", "Or : tout · Vert : presque tout · Dehors : peu", "Oro: tutto · Verde: quasi tutto · Fuori: poco", "Dourado: tudo · Verde: quase tudo · Fora: pouco", "Золото: всё · Зелёный: почти всё · Мимо: немного", "金:全部・緑:ほぼ全部・外:少し", "金色:全部·绿色:大部分·外面:一点")
h("game.hint_safe", "Rueda del ratón o clic arriba/abajo (Mayús: de 10 en 10)", "Mouse wheel or click top/bottom (Shift: by 10)", "Mausrad oder oben/unten klicken (Umschalt: in 10ern)", "Molette ou clic haut/bas (Maj : par 10)", "Rotella o clic sopra/sotto (Maiusc: di 10)", "Roda do mouse ou clique em cima/baixo (Shift: de 10)", "Колесо мыши или клик сверху/снизу (Shift: по 10)", "ホイールか上下クリック(Shiftで10ずつ)", "滚轮或点击上/下(Shift:每次10)")
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Fajos de billetes", "Cash bundles", "Geldbündel", "Liasses de billets", "Mazzette di banconote", "Maços de dinheiro", "Пачки денег", "札束", "成捆现金"),
    ("Lingotes de oro", "Gold bars", "Goldbarren", "Lingots d'or", "Lingotti d'oro", "Barras de ouro", "Золотые слитки", "金塊", "金条"),
    ("Diamantes", "Diamonds", "Diamanten", "Diamants", "Diamanti", "Diamantes", "Бриллианты", "ダイヤモンド", "钻石"),
    ("Fichas del casino", "Casino chips", "Casino-Jetons", "Jetons du casino", "Fiche del casinò", "Fichas do cassino", "Фишки казино", "カジノチップ", "赌场筹码"))):
    h("game.loot_" + str(i), es, en, de, fr, it, pt, ru, ja, zh)
for i, (es, en, de, fr, it, pt, ru, ja, zh) in enumerate((
    ("Se te cayó casi todo", "You dropped most of it", "Das meiste fallen gelassen", "Tu as presque tout fait tomber", "Ti è caduto quasi tutto", "Deixou cair quase tudo", "Почти всё уронил", "ほとんど落とした", "大部分掉了"),
    ("¡Buen agarre!", "Nice grab!", "Guter Griff!", "Belle prise !", "Bella presa!", "Boa pegada!", "Хороший захват!", "いい掴みだ!", "抓得好!"),
    ("¡PERFECTO!", "PERFECT!", "PERFEKT!", "PARFAIT !", "PERFETTO!", "PERFEITO!", "ИДЕАЛЬНО!", "パーフェクト!", "完美!"))):
    h("game.grab_" + str(i), es, en, de, fr, it, pt, ru, ja, zh)
h("game.clue", "Pista: %s", "Clue: %s", "Hinweis: %s", "Indice : %s", "Indizio: %s", "Pista: %s", "Подсказка: %s", "手がかり:%s", "线索:%s")


# ---- ronda 9: torneo de cada noche
def tn(key, es, en, de, fr, it, pt, ru, ja, zh):
    k("casino.tournament." + key, es, en, de, fr, it, pt, ru, ja, zh)

tn("announce", "Esta noche hay torneo de %s en los casinos. ¡Prepárate!", "Tonight there's a %s tournament at the casinos. Get ready!", "Heute Nacht gibt es ein %s-Turnier in den Casinos. Mach dich bereit!", "Ce soir, tournoi de %s dans les casinos. Prépare-toi !", "Stanotte torneo di %s nei casinò. Preparati!", "Hoje à noite tem torneio de %s nos cassinos. Prepare-se!", "Сегодня ночью в казино турнир по игре «%s». Готовься!", "今夜はカジノで%sトーナメント!準備しよう!", "今晚赌场有%s锦标赛,做好准备!")
tn("start", "¡Empieza el torneo de %s! Dura %s minutos: gana quien más fichas le saque.", "The %s tournament begins! It lasts %s minutes: whoever wins the most chips takes it.", "Das %s-Turnier beginnt! Es dauert %s Minuten: Wer die meisten Jetons gewinnt, siegt.", "Le tournoi de %s commence ! Il dure %s minutes : celui qui gagne le plus de jetons l'emporte.", "Inizia il torneo di %s! Dura %s minuti: vince chi guadagna più fiche.", "Começa o torneio de %s! Dura %s minutos: vence quem ganhar mais fichas.", "Турнир по игре «%s» начался! Длится %s мин.: побеждает тот, кто выиграет больше фишек.", "%sトーナメント開始!%s分間、一番多く稼いだ人の勝ち。", "%s锦标赛开始!持续%s分钟,赢得筹码最多者获胜。")
tn("end", "¡Terminó el torneo de %s!", "The %s tournament is over!", "Das %s-Turnier ist vorbei!", "Le tournoi de %s est terminé !", "Il torneo di %s è finito!", "O torneio de %s acabou!", "Турнир по игре «%s» окончен!", "%sトーナメント終了!", "%s锦标赛结束了!")
tn("prize", "Torneo: lugar %s en %s", "Tournament: place %s in %s", "Turnier: Platz %s in %s", "Tournoi : %se place au %s", "Torneo: %s° posto a %s", "Torneio: %sº lugar em %s", "Турнир: %s-е место в «%s»", "トーナメント:%sで%s位", "锦标赛:%s第%s名")
tn("place", "%s.º lugar: %s (+%s) gana %s fichas", "#%s: %s (+%s) wins %s chips", "Platz %s: %s (+%s) gewinnt %s Jetons", "%se place : %s (+%s) gagne %s jetons", "%s° posto: %s (+%s) vince %s fiche", "%sº lugar: %s (+%s) ganha %s fichas", "%s-е место: %s (+%s) получает %s фишек", "%s位:%s(+%s)に%sチップ", "第%s名:%s(+%s)获得%s筹码")
tn("thanks", "Gracias por jugar el torneo: +%s fichas.", "Thanks for playing the tournament: +%s chips.", "Danke fürs Mitspielen: +%s Jetons.", "Merci d'avoir joué le tournoi : +%s jetons.", "Grazie per aver giocato il torneo: +%s fiche.", "Obrigado por jogar o torneio: +%s fichas.", "Спасибо за участие: +%s фишек.", "参加ありがとう:+%sチップ。", "感谢参加锦标赛:+%s筹码。")
tn("nobody", "Nadie ganó fichas esta vez. ¡Mañana hay otro!", "Nobody won chips this time. There's another one tomorrow!", "Diesmal hat niemand Jetons gewonnen. Morgen gibt's ein neues!", "Personne n'a gagné de jetons cette fois. Il y en a un autre demain !", "Nessuno ha vinto fiche stavolta. Domani ce n'è un altro!", "Ninguém ganhou fichas desta vez. Amanhã tem outro!", "В этот раз никто не выиграл. Завтра будет новый!", "今回は誰も稼げなかった。明日また開催!", "这次没人赢得筹码。明天还有一场!")
tn("hud_title", "TORNEO: %s", "TOURNAMENT: %s", "TURNIER: %s", "TOURNOI : %s", "TORNEO: %s", "TORNEIO: %s", "ТУРНИР: %s", "トーナメント:%s", "锦标赛:%s")
tn("hud_left", "Termina en %s", "Ends in %s", "Endet in %s", "Fin dans %s", "Finisce tra %s", "Termina em %s", "До конца %s", "残り%s", "剩余%s")
tn("hud_me", "Tú: #%s (%s)", "You: #%s (%s)", "Du: #%s (%s)", "Toi : #%s (%s)", "Tu: #%s (%s)", "Você: #%s (%s)", "Ты: #%s (%s)", "あなた:%s位(%s)", "你:第%s名(%s)")
tn("hud_join", "¡Juega para entrar a la tabla!", "Play to get on the board!", "Spiel, um auf die Liste zu kommen!", "Joue pour entrer au classement !", "Gioca per entrare in classifica!", "Jogue para entrar na tabela!", "Играй, чтобы попасть в таблицу!", "プレイしてランクインしよう!", "参与游戏即可上榜!")


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
