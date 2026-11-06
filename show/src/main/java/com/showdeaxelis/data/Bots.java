package com.showdeaxelis.data;

import com.showdeaxelis.game.DrawGame;
import com.showdeaxelis.game.ImpostorGame;
import com.showdeaxelis.game.Participant;
import com.showdeaxelis.game.Phase;
import com.showdeaxelis.game.Round100;
import com.showdeaxelis.game.Show;
import com.showdeaxelis.game.Side;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Jugadores de mentira para probar el show sin amigos conectados. Contestan, dan pistas, votan y adivinan solos,
 * con tiempos y aciertos al azar. Solo existen dentro del show (no son jugadores reales del mundo).
 */
public final class Bots {
    public static final String[] NAMES = {"Bot Ana", "Bot Beto", "Bot Coco", "Bot Dora", "Bot Eli", "Bot Fer"};
    /** En las pruebas automaticas los bots no escriben en el chat ni mandan trazos. */
    public static boolean quiet;
    private static final Random RND = new Random();
    private static final Map<String, Integer> next = new HashMap<>();
    private static final Map<UUID, int[]> pen = new HashMap<>();
    private static final Map<String, Integer> strokes = new HashMap<>();
    private static final Set<UUID> IDS = new HashSet<>();

    private static final String[] CLUES = {
        "Es algo que casi todos conocen.", "Me recuerda a mi infancia.", "Lo vi ayer, creo.", "Mi primo tiene uno.", "Es más grande de lo que parece.",
        "Se puede encontrar en muchos lados.", "No es tan raro como suena.", "Siempre hay uno cerca cuando no lo necesitas.", "Mmm... difícil de explicar.",
        "Mucha gente lo usa los fines de semana."
    };
    private static final String[] HINTS = {
        "Es algo muy conocido.", "Seguro lo has visto.", "Piensa en lo primero que se te ocurra.", "No es tan difícil.", "Casi todos tenemos uno."
    };
    private static final String[] JUNK = {"pollo con papas", "mi tío Beto", "no sé, un sillón", "los tamales de mi abuela", "una nube"};

    static {
        for (String n : NAMES) {
            IDS.add(idOf(n));
        }
    }

    private Bots() {
    }

    public static UUID idOf(String name) {
        return UUID.nameUUIDFromBytes(("show-bot:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isBot(UUID id) {
        return id != null && IDS.contains(id);
    }

    public static void reset() {
        next.clear();
        pen.clear();
        strokes.clear();
    }

    private static int bots(Show s) {
        int c = 0;
        for (Participant p : s.players) {
            if (isBot(p.id)) {
                c++;
            }
        }
        return c;
    }

    /** Agrega un bot (al equipo indicado, o al que tenga menos gente si team es negativo). Devuelve false si no cabe. */
    public static boolean add(Show s, int team) {
        if (s.phase != Phase.LOBBY) {
            return false;
        }
        for (String n : NAMES) {
            if (s.byId(idOf(n)) == null) {
                int t = team;
                if (s.format.teams > 0 && t < 0) {
                    int best = 0;
                    int bestN = 99;
                    for (int i = 0; i < s.format.teams; i++) {
                        int c = 0;
                        for (Participant p : s.players) {
                            if (p.team == i) {
                                c++;
                            }
                        }
                        if (c < bestN) {
                            bestN = c;
                            best = i;
                        }
                    }
                    t = best;
                }
                return s.assign(idOf(n), n, Math.max(0, t));
            }
        }
        return false;
    }

    /** Llena todos los lugares libres con bots. */
    public static int fill(Show s) {
        int n = 0;
        while (add(s, -1)) {
            n++;
        }
        return n;
    }

    public static void removeAll(Show s) {
        for (Participant p : new ArrayList<>(s.players)) {
            if (isBot(p.id)) {
                s.remove(p.id);
            }
        }
    }

    private static boolean due(UUID id, String key, Show s, int now, int lo, int hi) {
        String k = id + key + s.roundNo;
        Integer t = next.get(k);
        if (t == null) {
            next.put(k, now + lo + RND.nextInt(Math.max(1, hi - lo + 1)));
            return false;
        }
        if (now >= t) {
            next.remove(k);
            return true;
        }
        return false;
    }

    private static void chat(String name, String text) {
        if (!quiet) {
            ShowManager.botChat(name, text);
        }
    }

    private static String pickWrongAnswer() {
        return JUNK[RND.nextInt(JUNK.length)];
    }

    /** Un paso (cada tick del servidor). Hace actuar a los bots segun lo que este pasando en el show. */
    public static void tick(Show s, int now) {
        if (s == null || bots(s) == 0 || s.phase == Phase.LOBBY || s.phase == Phase.FINAL || s.phase == Phase.IDLE) {
            return;
        }
        switch (s.phase) {
            case R100 -> tick100(s, now);
            case TRIVIA -> tickTrivia(s, now);
            case IMPOSTOR -> tickImpostor(s, now);
            case TABOO -> tickTaboo(s, now);
            case DRAW -> tickDraw(s, now);
            default -> {
            }
        }
    }

    private static void tick100(Show s, int now) {
        Round100 r = s.r;
        if (r == null || r.stage == Round100.END) {
            return;
        }
        if (r.answerer == null) {
            // solo suena la campana cuando la pregunta ya se anuncio
            if (r.stage == Round100.FACE && r.open && r.qShown) {
                for (Side sd : s.sides) {
                    Participant rep = sd.rep();
                    if (rep != null && isBot(rep.id) && due(rep.id, "buzz" + r.stage + r.faceTry, s, now, 30, 110)) {
                        s.buzz(rep.id);
                    }
                }
            }
            return;
        }
        // en modo presentador los bots no hablan solos: el presentador los hace hablar (o escribe lo que dicen)
        if (!s.manual && isBot(r.answerer) && r.pending == null && due(r.answerer, "ans" + r.strikes + r.stage + countRevealed(r), s, now, 70, 160)) {
            say100(s);
        }
    }

    /** El presentador pide que el bot que tiene la palabra conteste ahora (modo presentador). */
    public static void sayNow(Show s, int now) {
        Round100 r = s.r;
        if (s.phase == Phase.R100 && r != null && r.answerer != null && isBot(r.answerer) && r.pending == null && r.stage != Round100.END) {
            say100(s);
        }
    }

    private static void say100(Show s) {
        Round100 r = s.r;
        List<String> hidden = new ArrayList<>();
        for (int i = 0; i < r.revealed.length; i++) {
            if (!r.revealed[i]) {
                hidden.add(r.q.answers().get(i).text());
            }
        }
        String say = !hidden.isEmpty() && RND.nextInt(100) < 65 ? hidden.get(RND.nextInt(hidden.size())) : pickWrongAnswer();
        String name = s.byId(r.answerer).name;
        chat(name, say);
        s.answer(r.answerer, say);
    }

    private static int countRevealed(Round100 r) {
        int c = 0;
        for (boolean b : r.revealed) {
            if (b) {
                c++;
            }
        }
        return c;
    }

    private static void tickTrivia(Show s, int now) {
        var t = s.t;
        if (t == null || t.revealed) {
            return;
        }
        int span = Math.max(60, (t.deadlineTick - t.startTick) * 6 / 10);
        for (Participant p : new ArrayList<>(s.players)) {
            if (isBot(p.id) && !t.answers.containsKey(p.id) && due(p.id, "tr", s, now, 30, span)) {
                int n = t.q.options().size();
                int c = RND.nextInt(100) < 60 ? t.q.correct() : RND.nextInt(n);
                s.triviaAnswer(p.id, c, now);
            }
        }
    }

    private static void tickImpostor(Show s, int now) {
        ImpostorGame g = s.imp;
        if (g == null) {
            return;
        }
        if (g.stage == ImpostorGame.Stage.CLUES) {
            UUID cur = g.current();
            if (cur != null && isBot(cur) && due(cur, "clue" + g.step, s, now, 40, 120)) {
                String line = CLUES[RND.nextInt(CLUES.length)];
                chat(s.byId(cur).name, line);
                s.impostorClue(cur, line);
            }
        } else if (g.stage == ImpostorGame.Stage.VOTE) {
            for (Participant p : new ArrayList<>(s.players)) {
                if (isBot(p.id) && !g.votes.containsKey(p.id) && due(p.id, "vote", s, now, 60, 220)) {
                    List<Participant> others = new ArrayList<>(s.players);
                    others.remove(p);
                    if (!others.isEmpty()) {
                        s.impostorVote(p.id, others.get(RND.nextInt(others.size())).id);
                    }
                }
            }
        } else if (g.stage == ImpostorGame.Stage.GUESS && isBot(g.impostor) && due(g.impostor, "guess", s, now, 60, 140)) {
            List<String> ws = Banks.WORDS.get(g.category);
            String w = ws == null || ws.isEmpty() ? "no sé" : ws.get(RND.nextInt(ws.size()));
            chat(s.byId(g.impostor).name, w);
            s.impostorGuess(g.impostor, w);
        }
    }

    private static void tickTaboo(Show s, int now) {
        var g = s.taboo;
        if (g == null || g.over) {
            return;
        }
        if (isBot(g.describer) && due(g.describer, "hint" + g.lines.size(), s, now, 60, 140)) {
            String line = HINTS[RND.nextInt(HINTS.length)];
            chat(s.byId(g.describer).name, line);
            s.tabooSay(g.describer, line, now);
            return;
        }
        double f = (now - g.startTick) / (double) Math.max(1, g.deadlineTick - g.startTick);
        for (Participant p : new ArrayList<>(s.players)) {
            if (isBot(p.id) && s.isGuesser(p.id) && due(p.id, "tg" + g.lines.size(), s, now, 80, 200)) {
                String w = RND.nextDouble() < 0.05 + 0.25 * f ? g.word : Banks.TABOO.isEmpty() ? "algo" : Banks.TABOO.get(RND.nextInt(Banks.TABOO.size())).word();
                chat(p.name, w);
                s.tabooSay(p.id, w, now);
            }
        }
    }

    private static void tickDraw(Show s, int now) {
        DrawGame g = s.draw;
        if (g == null || g.over) {
            return;
        }
        if (isBot(g.drawer) && now % 6 == 0) {
            String sk = g.drawer + "r" + s.roundNo;
            int n = strokes.getOrDefault(sk, 0);
            if (n < 45) {
                int[] at = pen.computeIfAbsent(g.drawer, k -> new int[] {40 + RND.nextInt(170), 30 + RND.nextInt(130)});
                int[] pts = new int[12];
                int x = at[0];
                int y = at[1];
                for (int i = 0; i < pts.length; i += 2) {
                    pts[i] = x;
                    pts[i + 1] = y;
                    x = Math.max(4, Math.min(DrawGame.W - 5, x + RND.nextInt(41) - 20));
                    y = Math.max(4, Math.min(DrawGame.H - 5, y + RND.nextInt(41) - 20));
                }
                at[0] = x;
                at[1] = y;
                int color = RND.nextInt(10) < 6 ? 0 : 2 + RND.nextInt(13);
                int size = 2 + RND.nextInt(3);
                if (s.addStroke(g.drawer, color, size, pts)) {
                    if (!quiet) {
                        ShowManager.broadcastStroke(color, size, pts, null);
                    }
                }
                strokes.put(sk, n + 1);
            }
        }
        double f = (now - g.startTick) / (double) Math.max(1, g.deadlineTick - g.startTick);
        for (Participant p : new ArrayList<>(s.players)) {
            if (isBot(p.id) && s.isDrawGuesser(p.id) && due(p.id, "dg" + g.solved.size(), s, now, 80, 260)) {
                boolean right = f > 0.3 && RND.nextInt(100) < 35;
                String w = right ? g.word : Banks.DIBUJOS.isEmpty() ? "algo" : Banks.DIBUJOS.get(RND.nextInt(Banks.DIBUJOS.size()));
                if (!right && w.equalsIgnoreCase(g.word)) {
                    w = "no sé";
                }
                if (!right) {
                    chat(p.name, w);
                }
                s.drawSay(p.id, w, now);
            }
        }
    }
}
