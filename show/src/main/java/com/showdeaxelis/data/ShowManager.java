package com.showdeaxelis.data;

import com.showdeaxelis.game.DrawGame;
import com.showdeaxelis.game.Ev;
import com.showdeaxelis.game.Format;
import com.showdeaxelis.game.ImpostorGame;
import com.showdeaxelis.game.Matcher;
import com.showdeaxelis.game.Participant;
import com.showdeaxelis.game.Phase;
import com.showdeaxelis.game.Question100;
import com.showdeaxelis.game.Round100;
import com.showdeaxelis.game.Show;
import com.showdeaxelis.game.Side;
import com.showdeaxelis.game.TriviaQ;
import com.showdeaxelis.net.ShowNet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** El director del show en el servidor: recibe lo que hacen el presentador y los jugadores, aplica las reglas y avisa a todos. */
public final class ShowManager {
    public static Show show;
    public static MinecraftServer server;
    public static boolean announcerOn = true;
    private static boolean dirty;
    private static final Random RND = new Random();
    private static final Set<String> used = new HashSet<>();
    private static int now;

    private ShowManager() {
    }

    // ------------------------------------------------------------ ayudas

    public static void msg(ServerPlayer sp, String text) {
        sp.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
    }

    private static ServerPlayer find(String name) {
        return server == null ? null : server.getPlayerList().getPlayerByName(name);
    }

    public static boolean isHost(ServerPlayer sp) {
        return show != null && sp.getUUID().equals(show.host);
    }

    private static void cue(String type, String text, int a, int b) {
        ShowNet.toAll(new ShowNet.Cue(type, text, a, b));
    }

    private static void snd(String name) {
        cue("snd", name, 0, 0);
    }

    private static void say(String text) {
        if (announcerOn && !text.isEmpty()) {
            cue("say", text, 0, 0);
        }
    }

    private static String sideName(int i) {
        return show != null && i >= 0 && i < show.sides.size() ? show.sides.get(i).name : "";
    }

    private static Map<String, String> vars(Ev e) {
        Map<String, String> m = new HashMap<>();
        m.put("side", sideName(e.side()));
        m.put("player", e.player());
        m.put("text", e.text());
        m.put("a", String.valueOf(e.a()));
        m.put("b", String.valueOf(e.b()));
        m.put("pts", String.valueOf(e.type().equals("round_win") ? e.a() : e.b()));
        return m;
    }

    public static void markDirty() {
        dirty = true;
    }

    // ------------------------------------------------------------ envio del estado

    public static void sync() {
        dirty = false;
        if (server == null) {
            return;
        }
        StageData.Info st = StageData.get(server).info();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            ShowNet.toPlayer(sp, new ShowNet.State(show == null ? ShowSync.empty() : ShowSync.build(show, sp.getUUID(), now, st)));
        }
    }

    public static void onLogin(ServerPlayer sp) {
        if (server == null) {
            return;
        }
        StageData.Info st = StageData.get(server).info();
        ShowNet.toPlayer(sp, new ShowNet.State(show == null ? ShowSync.empty() : ShowSync.build(show, sp.getUUID(), now, st)));
        replayDraw(sp);
    }

    public static void tick(MinecraftServer srv) {
        server = srv;
        now = srv.getTickCount();
        if (show == null) {
            return;
        }
        show.tick(now);
        Bots.tick(show, now);
        boolean timed = show.phase == Phase.TRIVIA && show.t != null && !show.t.revealed
                || show.phase == Phase.TABOO && show.taboo != null && !show.taboo.over
                || show.phase == Phase.DRAW && show.draw != null && !show.draw.over;
        List<Ev> evs = show.drain();
        for (Ev e : evs) {
            onEvent(e);
        }
        if (!evs.isEmpty() || dirty || timed && now % 10 == 0) {
            sync();
        }
    }

    // ------------------------------------------------------------ eventos -> sonidos, texto, efectos

    private static void line(String type, Ev e) {
        say(Announcer.line(type, vars(e)));
    }

    private static void onEvent(Ev e) {
        switch (e.type()) {
            case "show_start" -> {
                snd("intro");
                cue("title", "SHOW DE AXELIS", 0, 0);
                cue("fx", "confetti", 0, 0);
                cue("cam", "wide", 0, 0);
                Studio.crowd(server, 2, 100);
                line("show_start", e);
            }
            case "round_start" -> {
                snd("round_start");
                cue("title", "100 MEXICANOS DIJERON", e.b(), 0);
                cue("cam", "wide", 0, 0);
                line("round_start", e);
            }
            case "buzz" -> {
                snd("buzzer");
                cue("cam", "pod" + e.side(), 0, 0);
                line("buzz", e);
            }
            case "dup" -> {
                snd("whistle_down");
                line("dup", e);
            }
            case "pending" -> snd("tick");
            case "reveal" -> {
                snd("reveal");
                cue("popup", "+" + e.b(), e.side(), 0);
                Studio.crowd(server, 1, 30);
                if (RND.nextInt(3) == 0) {
                    line("reveal", e);
                }
            }
            case "reveal_rest" -> snd("reveal");
            case "wrong" -> {
                snd("strike");
                line("wrong", e);
            }
            case "strike" -> {
                snd("strike");
                cue("strike", "", e.a(), 0);
                Studio.crowd(server, 3, 40);
                line("strike", e);
            }
            case "steal" -> {
                snd("drumroll");
                cue("cam", "pod" + e.side(), 0, 0);
                line("steal", e);
            }
            case "steal_ok" -> {
                snd("cheer");
                Studio.crowd(server, 2, 60);
                line("steal_ok", e);
            }
            case "steal_fail" -> {
                snd("trombone");
                Studio.crowd(server, 4, 60);
                line("steal_fail", e);
            }
            case "control" -> {
                snd("whistle_up");
                cue("cam", "pod" + e.side(), 0, 0);
                line("control", e);
            }
            case "face_none" -> {
                snd("crickets");
                Studio.crowd(server, 4, 60);
                line("face_none", e);
            }
            case "round_win" -> {
                snd("fanfare");
                snd("applause_big");
                cue("fx", "confetti", 0, 0);
                cue("cam", "wide", 0, 0);
                Studio.crowd(server, 2, 120);
                line(e.side() >= 0 ? "round_win" : "round_none", e);
            }
            case "trivia_start" -> {
                snd("round_start");
                cue("title", e.text().isEmpty() ? "TRIVIA" : triviaLabel(), 0, 0);
                cue("cam", "wide", 0, 0);
                line("trivia_start", e);
            }
            case "trivia_answer" -> snd("pop");
            case "trivia_reveal" -> {
                if (e.b() == 0) {
                    snd("trombone");
                    Studio.crowd(server, 4, 60);
                    line("trivia_none", e);
                } else {
                    snd("correct");
                    snd("applause");
                    Studio.crowd(server, 1, 60);
                    line("trivia_reveal", e);
                }
            }
            case "score" -> snd("pop");
            case "imp_start" -> {
                snd("suspense");
                cue("title", "EL IMPOSTOR", 0, 0);
                cue("cam", "wide", 0, 0);
                line("imp_start", e);
            }
            case "imp_clue" -> snd("pop");
            case "imp_discuss" -> {
                snd("whistle_up");
                line("imp_discuss", e);
            }
            case "imp_vote" -> {
                snd("drumroll");
                line("imp_vote", e);
            }
            case "imp_voted" -> snd("vote");
            case "imp_caught" -> {
                snd("airhorn");
                snd("boo");
                line("imp_caught", e);
            }
            case "imp_escaped" -> {
                snd("laugh");
                Studio.crowd(server, 4, 80);
                line("imp_escaped", e);
            }
            case "imp_end" -> {
                if (e.a() == 1) {
                    snd("trombone");
                } else {
                    snd("fanfare");
                    cue("fx", "confetti", 0, 0);
                }
                line("imp_end", e);
            }
            case "taboo_start" -> {
                snd("round_start");
                cue("title", "ADIVINA LA PALABRA", 0, 0);
                cue("cam", "pod" + e.side(), 0, 0);
                line("taboo_start", e);
            }
            case "taboo_bad" -> {
                snd("buzzer");
                cue("strike", "", (int) e.a(), 0);
                line("taboo_bad", e);
            }
            case "taboo_fail", "taboo_timeout" -> {
                snd(e.type().equals("taboo_fail") ? "trombone" : "time_up");
                Studio.crowd(server, 4, 60);
                line("taboo_fail", e);
            }
            case "taboo_solved" -> {
                snd("correct");
                snd("applause_big");
                cue("fx", "confetti", 0, 0);
                Studio.crowd(server, 2, 100);
                line("taboo_solved", e);
            }
            case "draw_start" -> {
                snd("round_start");
                cue("title", "ADIVINA EL DIBUJO", 0, 0);
                cue("cam", "board", 0, 0);
                ShowNet.toAll(new ShowNet.Draw(1, 0, 0, new int[0]));
                line("draw_start", e);
            }
            case "draw_guess" -> snd("pop");
            case "draw_bad" -> {
                snd("buzzer");
                line("draw_bad", e);
            }
            case "draw_solved" -> {
                snd("correct");
                cue("popup", "+" + e.a(), e.side(), 0);
                Studio.crowd(server, 1, 40);
                line("draw_solved", e);
            }
            case "draw_over" -> {
                if (e.b() == 1) {
                    snd("applause_big");
                    cue("fx", "confetti", 0, 0);
                    Studio.crowd(server, 2, 100);
                    line("draw_all", e);
                } else if (e.a() == 0) {
                    snd("trombone");
                    Studio.crowd(server, 4, 60);
                    line("draw_none", e);
                } else {
                    snd("time_up");
                }
            }
            case "final" -> {
                snd("drumroll");
                snd("fanfare");
                snd("applause_big");
                cue("title", "FINAL", 0, 0);
                cue("fx", "confetti", 0, 0);
                cue("cam", "wide", 0, 0);
                Studio.crowd(server, 2, 200);
                line(e.side() >= 0 ? "final" : "final_tie", e);
                if (e.side() >= 0) {
                    say("Su premio: " + Announcer.prize() + ".");
                }
            }
            default -> {
            }
        }
    }

    private static String triviaLabel() {
        return show != null && show.t != null ? show.t.q.category().toUpperCase(java.util.Locale.ROOT) : "TRIVIA";
    }

    // ------------------------------------------------------------ chat

    private static int letter(String raw, int options) {
        String s = Matcher.norm(raw);
        switch (s) {
            case "a", "1" -> {
                return 0;
            }
            case "b", "2" -> {
                return 1;
            }
            case "c", "3" -> {
                return options > 2 ? 2 : -1;
            }
            case "d", "4" -> {
                return options > 3 ? 3 : -1;
            }
            case "v", "verdadero", "true", "cierto", "si" -> {
                return 0;
            }
            case "f", "falso", "fake", "no" -> {
                return 1;
            }
            default -> {
                return -1;
            }
        }
    }

    /** Devuelve true si el mensaje se uso como jugada y no debe salir en el chat. */
    public static boolean onChat(ServerPlayer sp, String text) {
        if (show == null || text == null) {
            return false;
        }
        UUID id = sp.getUUID();
        boolean hide = false;
        switch (show.phase) {
            case R100 -> show.answer(id, text);
            case TRIVIA -> {
                if (show.t != null && !show.t.revealed && show.byId(id) != null) {
                    int c = letter(text, show.t.q.options().size());
                    if (c >= 0 && show.triviaAnswer(id, c, now)) {
                        hide = true;
                    }
                }
            }
            case IMPOSTOR -> {
                ImpostorGame g = show.imp;
                if (g == null) {
                    break;
                }
                if (g.stage == ImpostorGame.Stage.CLUES) {
                    show.impostorClue(id, text);
                } else if (g.stage == ImpostorGame.Stage.VOTE && show.byId(id) != null) {
                    UUID target = findTarget(text);
                    if (target != null && show.impostorVote(id, target)) {
                        hide = true;
                    }
                } else if (g.stage == ImpostorGame.Stage.GUESS) {
                    show.impostorGuess(id, text);
                }
            }
            case TABOO -> hide = show.tabooSay(id, text, now) == 2;
            case DRAW -> {
                int r = show.drawSay(id, text, now);
                hide = r == 2 || r == 3;
            }
            default -> {
            }
        }
        if (show != null) {
            markDirty();
        }
        return hide;
    }

    private static UUID findTarget(String text) {
        String t = Matcher.norm(text);
        for (int i = 0; i < show.players.size(); i++) {
            Participant p = show.players.get(i);
            if (t.equals(String.valueOf(i + 1)) || Matcher.same(t, p.name) || Matcher.norm(p.name).equals(t)) {
                return p.id;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ acciones

    private static final String[] SHOTS = {"wide", "host", "board", "audience", "pod0", "pod1", "pod2", "pod3", "pod4", "pod5"};

    public static void act(ServerPlayer sp, String act, String s, int n) {
        server = sp.getServer();
        now = server.getTickCount();
        UUID id = sp.getUUID();
        // acciones de jugador
        switch (act) {
            case "buzz" -> {
                if (show != null && show.buzz(id)) {
                    markDirty();
                }
                return;
            }
            case "ans" -> {
                if (show != null && show.triviaAnswer(id, n, now)) {
                    markDirty();
                }
                return;
            }
            case "vote" -> {
                if (show != null) {
                    UUID tg = findTarget(s);
                    if (tg != null) {
                        show.impostorVote(id, tg);
                        markDirty();
                    }
                }
                return;
            }
            case "refresh" -> {
                onLogin(sp);
                return;
            }
            default -> {
            }
        }
        if (act.equals("role")) {
            if (!sp.hasPermissions(2)) {
                msg(sp, "Solo un operador puede organizar el show.");
                return;
            }
            Banks.load();
            show = new Show(id, sp.getName().getString(), s.equals("player"));
            used.clear();
            Bots.reset();
            if (show.hostPlays) {
                show.assign(id, show.hostName, 0);
            }
            markDirty();
            sync();
            return;
        }
        if (show == null || !isHost(sp)) {
            msg(sp, "Solo el presentador puede hacer eso.");
            return;
        }
        hostAct(sp, act, s, n);
        if (show != null) {
            markDirty();
        }
    }

    private static Question100 pick100(String idOrBlank) {
        for (Question100 q : Banks.Q100) {
            if (q.id().equals(idOrBlank)) {
                return q;
            }
        }
        List<Question100> free = new ArrayList<>();
        for (Question100 q : Banks.Q100) {
            if (!used.contains(q.id())) {
                free.add(q);
            }
        }
        if (free.isEmpty()) {
            for (Question100 q : Banks.Q100) {
                used.remove(q.id());
            }
            free.addAll(Banks.Q100);
        }
        return free.isEmpty() ? null : free.get(RND.nextInt(free.size()));
    }

    private static TriviaQ pickTrivia(String cat) {
        List<TriviaQ> free = new ArrayList<>();
        for (int pass = 0; pass < 2 && free.isEmpty(); pass++) {
            for (TriviaQ q : Banks.TRIVIA) {
                boolean ok = cat.isEmpty() || q.category().equalsIgnoreCase(cat);
                if (ok && (pass == 1 || !used.contains(q.id()))) {
                    free.add(q);
                }
            }
        }
        return free.isEmpty() ? null : free.get(RND.nextInt(free.size()));
    }

    private static void hostAct(ServerPlayer sp, String act, String s, int n) {
        Show sh = show;
        switch (act) {
            case "format" -> {
                if (!sh.setFormat(Format.byOrdinal(n))) {
                    msg(sp, "El formato solo se cambia antes de empezar.");
                }
            }
            case "assign" -> {
                ServerPlayer p = find(s);
                if (p == null) {
                    msg(sp, "No encuentro al jugador " + s + ".");
                } else if (!sh.assign(p.getUUID(), p.getName().getString(), n)) {
                    msg(sp, "No cabe en ese equipo.");
                }
            }
            case "unassign" -> {
                ServerPlayer p = find(s);
                if (p != null) {
                    sh.remove(p.getUUID());
                } else {
                    for (Participant pp : new ArrayList<>(sh.players)) {
                        if (pp.name.equalsIgnoreCase(s)) {
                            sh.remove(pp.id);
                        }
                    }
                }
            }
            case "auto" -> {
                List<ServerPlayer> c = new ArrayList<>();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (!p.getUUID().equals(sh.host) || sh.hostPlays) {
                        c.add(p);
                    }
                }
                Collections.shuffle(c, RND);
                List<UUID> ids = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (ServerPlayer p : c) {
                    ids.add(p.getUUID());
                    names.add(p.getName().getString());
                }
                sh.autoAssign(ids, names);
                if (c.size() > sh.format.maxPlayers()) {
                    msg(sp, "Hay mas gente conectada que lugares; entraron " + sh.format.maxPlayers() + " al azar.");
                }
            }
            case "hostplays" -> {
                sh.hostPlays = !sh.hostPlays;
                if (sh.hostPlays) {
                    sh.assign(sh.host, sh.hostName, 0);
                } else {
                    sh.remove(sh.host);
                }
            }
            case "start" -> {
                if (!sh.start()) {
                    msg(sp, "Faltan jugadores: hacen falta al menos 2 (o 2 equipos con gente).");
                }
            }
            case "reset" -> {
                show = null;
                Bots.reset();
                Studio.crowd(server, 0, 1);
            }
            case "end" -> sh.finishShow();
            case "autowrong" -> sh.autoWrong = !sh.autoWrong;
            case "announcer" -> announcerOn = !announcerOn;
            case "reload" -> msg(sp, "Bancos recargados: " + Banks.load());
            case "tp" -> Studio.teleportToPodiums(server, sh);
            // --- 100 mexicanos
            case "r100" -> {
                Question100 q = pick100(s);
                if (q == null) {
                    msg(sp, "No hay preguntas de 100 mexicanos.");
                } else if (sh.start100(q, Math.max(1, n))) {
                    used.add(q.id());
                } else {
                    msg(sp, "Termina primero lo que esta en curso.");
                }
            }
            case "reveal" -> sh.hostReveal(n);
            case "strike" -> sh.hostStrike();
            case "control" -> sh.takeControl(n);
            case "closeface" -> sh.closeFace();
            case "accept" -> sh.hostAcceptPending(n);
            case "reject" -> sh.hostRejectPending();
            case "revealrest" -> sh.revealRest();
            case "who" -> {
                ServerPlayer p = find(s);
                if (p != null) {
                    sh.hostSetAnswerer(p.getUUID());
                }
            }
            case "award" -> {
                if (sh.r != null) {
                    sh.endRound(n);
                }
            }
            case "finish" -> {
                switch (sh.phase) {
                    case R100 -> sh.finish100();
                    case TRIVIA -> sh.finishTrivia();
                    case IMPOSTOR -> sh.finishImpostor();
                    case TABOO -> sh.finishTaboo();
                    case DRAW -> sh.finishDraw();
                    default -> {
                    }
                }
            }
            // --- trivia
            case "trivia" -> {
                TriviaQ q = pickTrivia(s);
                if (q == null) {
                    msg(sp, "No hay preguntas de esa categoria.");
                } else if (sh.startTrivia(q, n > 0 ? n : 20, now)) {
                    used.add(q.id());
                } else {
                    msg(sp, "Termina primero lo que esta en curso.");
                }
            }
            case "treveal" -> sh.revealTrivia();
            case "reto" -> {
                if (Banks.RETOS.isEmpty() || sh.players.isEmpty()) {
                    msg(sp, "No hay retos o jugadores.");
                } else {
                    Participant who = sh.players.get(RND.nextInt(sh.players.size()));
                    String reto = Banks.RETOS.get(RND.nextInt(Banks.RETOS.size()));
                    snd("round_start");
                    cue("title", "RETO ABSURDO", 0, 0);
                    cue("cam", "pod" + Math.max(0, sh.sideOf(who)), 0, 0);
                    say(who.name + ", tu reto: " + reto);
                }
            }
            // --- impostor
            case "imp" -> startImpostor(sp, s);
            case "impskip" -> sh.impostorSkip();
            case "impvote" -> sh.impostorStartVote();
            case "imptally" -> sh.impostorTally();
            case "impgiveup" -> sh.impostorGiveUp();
            // --- adivina la palabra
            case "taboo" -> {
                if (Banks.TABOO.isEmpty()) {
                    msg(sp, "No hay palabras prohibidas.");
                } else {
                    Banks.Taboo t = Banks.TABOO.get(RND.nextInt(Banks.TABOO.size()));
                    if (!sh.startTaboo(t.word(), t.forbidden(), n > 0 ? n : 60, now)) {
                        msg(sp, "Termina primero lo que esta en curso (hacen falta 2 jugadores).");
                    }
                }
            }
            // --- adivina el dibujo
            case "draw" -> startDraw(sp, s, n);
            // --- jugadores de mentira para probar solo
            case "bot" -> {
                if (!Bots.add(sh, n)) {
                    msg(sp, "No cabe otro bot (o el show ya empezo).");
                }
            }
            case "botsfill" -> msg(sp, "Se agregaron " + Bots.fill(sh) + " bots.");
            case "botsclear" -> Bots.removeAll(sh);
            // --- puntos, efectos y camaras
            case "score" -> sh.addScore(n, parseInt(s, 0));
            case "cue" -> hostCue(s);
            case "cam" -> cue("cam", s, 0, 0);
            case "tv" -> cue("tv", "", n, 0);
            default -> msg(sp, "Accion desconocida: " + act);
        }
        // el secreto del impostor llega por mensaje privado ademas de la pantalla
        if (act.equals("imp") && sh.phase == Phase.IMPOSTOR && sh.imp != null) {
            for (Participant p : sh.players) {
                ServerPlayer pl = server.getPlayerList().getPlayer(p.id);
                if (pl != null) {
                    String w = sh.secretFor(p.id);
                    msg(pl, w == null ? "ERES EL IMPOSTOR. No sabes la palabra: disimula. Categoria: " + sh.imp.category
                            : "La palabra secreta es: " + w + ". Da pistas sin decirla.");
                }
            }
        }
    }

    private static void startDraw(ServerPlayer sp, String drawerName, int seconds) {
        List<String> free = new ArrayList<>();
        for (String w : Banks.DIBUJOS) {
            if (!used.contains("d:" + w)) {
                free.add(w);
            }
        }
        if (free.isEmpty()) {
            for (String w : Banks.DIBUJOS) {
                used.remove("d:" + w);
            }
            free.addAll(Banks.DIBUJOS);
        }
        if (free.isEmpty()) {
            msg(sp, "No hay palabras para dibujar.");
            return;
        }
        UUID who = null;
        if (!drawerName.isBlank()) {
            for (Participant p : show.players) {
                if (p.name.equalsIgnoreCase(drawerName.trim())) {
                    who = p.id;
                }
            }
        }
        String word = free.get(RND.nextInt(free.size()));
        if (!show.startDraw(word, seconds > 0 ? seconds : 90, now, who)) {
            msg(sp, "Termina primero lo que esta en curso (hacen falta 2 jugadores).");
            return;
        }
        used.add("d:" + word);
        ServerPlayer d = server.getPlayerList().getPlayer(show.draw.drawer);
        if (d != null) {
            msg(d, "Te toca dibujar: " + word + ". Abre el lienzo con la tecla J (o se abre solo). No digas la palabra.");
        }
    }

    // ------------------------------------------------------------ dibujo y bots

    /** Un trazo o un borrado que llega del dibujante: se guarda y se reenvia a todos los demas. */
    public static void onDraw(ServerPlayer sp, int op, int color, int size, int[] pts) {
        if (show == null || show.phase != Phase.DRAW || show.draw == null) {
            return;
        }
        UUID id = sp.getUUID();
        if (op == 1) {
            if (show.clearCanvas(id)) {
                for (ServerPlayer o : server.getPlayerList().getPlayers()) {
                    if (o != sp) {
                        ShowNet.toPlayer(o, new ShowNet.Draw(1, 0, 0, new int[0]));
                    }
                }
            }
        } else if (op == 0 && pts.length <= ShowNet.MAX_PTS * 2 && show.addStroke(id, color, size, pts)) {
            broadcastStroke(color, size, pts, sp);
        }
    }

    public static void broadcastStroke(int color, int size, int[] pts, ServerPlayer except) {
        if (server == null) {
            return;
        }
        ShowNet.Draw d = new ShowNet.Draw(0, color, size, pts);
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o != except) {
                ShowNet.toPlayer(o, d);
            }
        }
    }

    /** Un jugador que entra tarde recibe el dibujo que ya se hizo. */
    private static void replayDraw(ServerPlayer sp) {
        if (show == null || show.phase != Phase.DRAW || show.draw == null) {
            return;
        }
        ShowNet.toPlayer(sp, new ShowNet.Draw(1, 0, 0, new int[0]));
        for (DrawGame.Stroke st : show.draw.strokes) {
            ShowNet.toPlayer(sp, new ShowNet.Draw(0, st.color(), st.size(), st.pts()));
        }
    }

    /** Lo que dice un bot, como si lo escribiera en el chat. */
    public static void botChat(String name, String text) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("<" + name + "> " + text), false);
        }
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return d;
        }
    }

    private static void startImpostor(ServerPlayer sp, String cat) {
        if (Banks.WORDS.isEmpty()) {
            msg(sp, "No hay palabras para el impostor.");
            return;
        }
        List<String> cats = new ArrayList<>(Banks.WORDS.keySet());
        String c = cat.isEmpty() || !Banks.WORDS.containsKey(cat) ? cats.get(RND.nextInt(cats.size())) : cat;
        List<String> ws = Banks.WORDS.get(c);
        if (ws.isEmpty()) {
            msg(sp, "Esa categoria no tiene palabras.");
            return;
        }
        List<UUID> order = new ArrayList<>();
        for (Participant p : show.players) {
            order.add(p.id);
        }
        Collections.shuffle(order, RND);
        if (!show.startImpostor(c, ws.get(RND.nextInt(ws.size())), order, RND.nextInt(Math.max(1, order.size())))) {
            msg(sp, "No se puede empezar (hacen falta 3 jugadores y nada en curso).");
        }
    }

    private static void hostCue(String name) {
        switch (name) {
            case "applause" -> {
                snd("applause");
                Studio.crowd(server, 1, 80);
            }
            case "cheer" -> {
                snd("cheer");
                Studio.crowd(server, 2, 80);
            }
            case "boo" -> {
                snd("boo");
                Studio.crowd(server, 3, 80);
            }
            case "laugh" -> {
                snd("laugh");
                Studio.crowd(server, 4, 80);
            }
            case "confetti" -> {
                cue("fx", "confetti", 0, 0);
                snd("fanfare");
            }
            case "techdiff" -> {
                snd("glitch");
                cue("fx", "glitch", 0, 0);
                say(Announcer.line("techdiff", new HashMap<>()));
            }
            case "joke" -> say(Announcer.line("chiste", new HashMap<>()));
            case "prize" -> say("Se lleva " + Announcer.prize() + ".");
            default -> snd(name);
        }
    }

    public static String[] shots() {
        return SHOTS;
    }

    // ------------------------------------------------------------ jugadores que se van

    public static void onLogout(ServerPlayer sp) {
        if (show != null && sp.getServer() != null) {
            markDirty();
        }
    }

    public static Side sideOfName(String name) {
        if (show == null) {
            return null;
        }
        for (Side sd : show.sides) {
            for (Participant p : sd.members) {
                if (p.name.equalsIgnoreCase(name)) {
                    return sd;
                }
            }
        }
        return null;
    }

    public static int round100Stage() {
        return show != null && show.r != null ? show.r.stage : -1;
    }

    public static boolean inFace() {
        return round100Stage() == Round100.FACE;
    }
}
