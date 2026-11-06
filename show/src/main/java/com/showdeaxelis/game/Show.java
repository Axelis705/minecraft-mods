package com.showdeaxelis.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Las reglas del show, sin nada de Minecraft: quien juega, los equipos, las rondas de 100 mexicanos dijeron y de trivia.
 * El servidor llama a estos metodos y despues vacia la lista de eventos para anunciarlos (sonido, texto, efectos).
 */
public final class Show {
    public static final int[] PALETTE = {0xE53935, 0x1E88E5, 0xFDD835, 0x43A047, 0x8E24AA, 0xFB8C00};
    public static final String[] TEAM_NAMES = {"Equipo Rojo", "Equipo Azul", "Equipo Amarillo"};
    /** Puntos de trivia segun el orden de llegada de los que aciertan. */
    public static final int[] RANK_POINTS = {10, 8, 6, 5, 4, 3};

    public Format format = Format.TWO_TEAMS;
    public Phase phase = Phase.LOBBY;
    public UUID host;
    public String hostName = "";
    public boolean hostPlays;
    public boolean autoWrong;
    public int roundNo;
    public final List<Participant> players = new ArrayList<>();
    public final List<Side> sides = new ArrayList<>();
    public Round100 r;
    public TriviaRound t;
    public ImpostorGame imp;
    public TabooGame taboo;
    public DrawGame draw;
    private int tabooTurn;
    private int drawTurn;
    public final List<Ev> events = new ArrayList<>();

    public Show(UUID host, String hostName, boolean hostPlays) {
        this.host = host;
        this.hostName = hostName;
        this.hostPlays = hostPlays;
        rebuildSides();
    }

    // ------------------------------------------------------------ equipos

    public Participant byId(UUID id) {
        for (Participant p : players) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    public int sideOf(Participant p) {
        for (Side s : sides) {
            if (s.members.contains(p)) {
                return s.index;
            }
        }
        return -1;
    }

    public void rebuildSides() {
        List<int[]> keep = new ArrayList<>();
        for (Side s : sides) {
            keep.add(new int[] {s.score, s.repCursor});
        }
        sides.clear();
        if (format.teams == 0) {
            int i = 0;
            for (Participant p : players) {
                Side s = new Side(i, p.name, PALETTE[i % PALETTE.length]);
                s.members.add(p);
                sides.add(s);
                i++;
            }
        } else {
            for (int i = 0; i < format.teams; i++) {
                sides.add(new Side(i, TEAM_NAMES[i], PALETTE[i]));
            }
            for (Participant p : players) {
                if (p.team >= 0 && p.team < sides.size()) {
                    sides.get(p.team).members.add(p);
                }
            }
        }
        for (int i = 0; i < sides.size() && i < keep.size(); i++) {
            sides.get(i).score = keep.get(i)[0];
            sides.get(i).repCursor = keep.get(i)[1];
        }
    }

    public boolean setFormat(Format f) {
        if (phase != Phase.LOBBY) {
            return false;
        }
        format = f;
        // si ya no caben, se recolocan en el primer equipo con lugar
        for (Participant p : new ArrayList<>(players)) {
            if (f.teams > 0 && (p.team < 0 || p.team >= f.teams)) {
                p.team = 0;
            }
        }
        if (f.teams > 0) {
            int[] n = new int[f.teams];
            List<Participant> out = new ArrayList<>();
            for (Participant p : players) {
                if (n[p.team] < f.perTeam) {
                    n[p.team]++;
                    continue;
                }
                int t = freeTeam(n, f);
                if (t < 0) {
                    out.add(p);
                } else {
                    p.team = t;
                    n[t]++;
                }
            }
            players.removeAll(out);
        } else {
            while (players.size() > f.maxPlayers()) {
                players.remove(players.size() - 1);
            }
        }
        rebuildSides();
        return true;
    }

    private static int freeTeam(int[] n, Format f) {
        for (int i = 0; i < f.teams; i++) {
            if (n[i] < f.perTeam) {
                return i;
            }
        }
        return -1;
    }

    private int countTeam(int team) {
        int c = 0;
        for (Participant p : players) {
            if (p.team == team) {
                c++;
            }
        }
        return c;
    }

    /** Pone o mueve a un jugador a un equipo (en todos contra todos el equipo se ignora). Devuelve false si no cabe. */
    public boolean assign(UUID id, String name, int team) {
        if (phase != Phase.LOBBY) {
            return false;
        }
        Participant p = byId(id);
        if (format.teams == 0) {
            if (p == null) {
                if (players.size() >= format.maxPlayers()) {
                    return false;
                }
                players.add(new Participant(id, name, 0));
            }
            rebuildSides();
            return true;
        }
        if (team < 0 || team >= format.teams) {
            return false;
        }
        if (p != null && p.team == team) {
            return true;
        }
        if (countTeam(team) >= format.perTeam) {
            return false;
        }
        if (p == null) {
            players.add(new Participant(id, name, team));
        } else {
            p.team = team;
        }
        rebuildSides();
        return true;
    }

    public boolean remove(UUID id) {
        Participant p = byId(id);
        if (p == null) {
            return false;
        }
        players.remove(p);
        rebuildSides();
        return true;
    }

    /** Reparte al azar a los candidatos (en orden dado ya mezclado). */
    public void autoAssign(List<UUID> ids, List<String> names) {
        players.clear();
        for (int i = 0; i < ids.size() && i < format.maxPlayers(); i++) {
            int team = 0;
            if (format.teams > 0) {
                team = i % format.teams;
            }
            players.add(new Participant(ids.get(i), names.get(i), team));
        }
        rebuildSides();
    }

    public boolean canStart() {
        if (phase != Phase.LOBBY) {
            return false;
        }
        int filled = 0;
        for (Side s : sides) {
            if (!s.members.isEmpty()) {
                filled++;
            }
        }
        if (format.teams == 0) {
            return players.size() >= 2;
        }
        return filled >= 2;
    }

    public boolean start() {
        if (!canStart()) {
            return false;
        }
        phase = Phase.IDLE;
        events.add(Ev.of("show_start"));
        return true;
    }

    // ------------------------------------------------------------ 100 mexicanos

    /** Se puede empezar otro segmento cuando no hay ninguno en curso o el anterior ya termino. */
    public boolean segmentFree() {
        return switch (phase) {
            case IDLE -> true;
            case R100 -> r != null && r.stage == Round100.END;
            case TRIVIA -> t != null && t.revealed;
            case IMPOSTOR -> imp != null && imp.stage == ImpostorGame.Stage.DONE;
            case TABOO -> taboo != null && taboo.over;
            case DRAW -> draw != null && draw.over;
            default -> false;
        };
    }

    private void clearSegments() {
        if (phase == Phase.R100) {
            advanceReps();
        }
        r = null;
        t = null;
        imp = null;
        taboo = null;
        draw = null;
    }

    public boolean start100(Question100 q, int mult) {
        if (!segmentFree()) {
            return false;
        }
        clearSegments();
        r = new Round100(q, mult);
        phase = Phase.R100;
        roundNo++;
        events.add(new Ev("round_start", -1, "", q.text(), mult, q.answers().size()));
        return true;
    }

    private void advanceReps() {
        for (Side s : sides) {
            s.repCursor++;
        }
    }

    private boolean isStealCandidate(int side) {
        return r != null && r.stage == Round100.STEAL && side != r.control && sides.get(side).rep() != null;
    }

    /** Un jugador toca el timbre. Solo cuenta si es quien da la cara de su lado en ese momento. */
    public boolean buzz(UUID id) {
        if (phase != Phase.R100 || r == null || r.answerer != null) {
            return false;
        }
        Participant p = byId(id);
        if (p == null) {
            return false;
        }
        int side = sideOf(p);
        if (side < 0) {
            return false;
        }
        if (r.stage == Round100.FACE) {
            if (r.faceDone.contains(side) || sides.get(side).rep() != p) {
                return false;
            }
        } else if (r.stage == Round100.STEAL) {
            if (!isStealCandidate(side) || sides.get(side).rep() != p) {
                return false;
            }
        } else {
            return false;
        }
        r.answerer = id;
        events.add(new Ev("buzz", side, p.name, "", 0, 0));
        return true;
    }

    public boolean isAnswerer(UUID id) {
        return phase == Phase.R100 && r != null && id.equals(r.answerer) && r.pending == null;
    }

    /** El jugador que contesta escribio algo. Devuelve true si se consumio como respuesta. */
    public boolean answer(UUID id, String text) {
        if (!isAnswerer(id)) {
            return false;
        }
        Participant p = byId(id);
        events.add(new Ev("said", sideOf(p), p.name, text, 0, 0));
        int idx = Matcher.find(text, r.q, r.revealed);
        if (idx >= 0) {
            resolve(idx);
        } else if (idx == -2) {
            events.add(new Ev("dup", sideOf(p), p.name, text, 0, 0));
        } else if (autoWrong) {
            resolve(-1);
        } else {
            r.pending = text;
            events.add(new Ev("pending", sideOf(p), p.name, text, 0, 0));
        }
        return true;
    }

    public boolean hostAcceptPending(int idx) {
        if (phase != Phase.R100 || r == null || r.pending == null || idx < 0 || idx >= r.revealed.length || r.revealed[idx]) {
            return false;
        }
        r.pending = null;
        resolve(idx);
        return true;
    }

    public boolean hostRejectPending() {
        if (phase != Phase.R100 || r == null || r.pending == null) {
            return false;
        }
        r.pending = null;
        resolve(-1);
        return true;
    }

    private int reveal(int idx, int side) {
        r.revealed[idx] = true;
        int pts = r.q.answers().get(idx).points();
        r.bank += pts;
        events.add(new Ev("reveal", side, "", r.q.answers().get(idx).text(), idx, pts));
        return pts;
    }

    private void resolve(int idx) {
        Participant ans = byId(r.answerer);
        int side = ans == null ? -1 : sideOf(ans);
        r.answerer = null;
        switch (r.stage) {
            case Round100.FACE -> {
                r.faceDone.add(side);
                int pts = idx >= 0 ? reveal(idx, side) : 0;
                if (idx < 0) {
                    events.add(new Ev("wrong", side, ans == null ? "" : ans.name, "", 0, 0));
                }
                r.faceGuess.put(side, pts);
                if (idx == 0) {
                    takeControl(side);
                } else if (r.faceDone.size() >= sides.size()) {
                    closeFace();
                }
            }
            case Round100.PLAY -> {
                if (idx >= 0) {
                    reveal(idx, side);
                    if (r.allRevealed()) {
                        endRound(r.control);
                    } else {
                        nextTurn();
                    }
                } else {
                    r.strikes++;
                    events.add(new Ev("strike", side, ans == null ? "" : ans.name, "", r.strikes, 0));
                    if (r.strikes >= 3) {
                        beginSteal();
                    } else {
                        nextTurn();
                    }
                }
            }
            case Round100.STEAL -> {
                if (idx >= 0) {
                    reveal(idx, side);
                    events.add(new Ev("steal_ok", side, ans == null ? "" : ans.name, "", 0, 0));
                    endRound(side);
                } else {
                    events.add(new Ev("steal_fail", side, ans == null ? "" : ans.name, "", 0, 0));
                    endRound(r.control);
                }
            }
            default -> {
            }
        }
    }

    /** Cierra el cara a cara: gana el control quien dio la respuesta mas alta. Si nadie acerto, se repite. */
    public void closeFace() {
        if (phase != Phase.R100 || r == null || r.stage != Round100.FACE) {
            return;
        }
        int best = -1;
        int bp = 0;
        for (Map.Entry<Integer, Integer> e : r.faceGuess.entrySet()) {
            if (e.getValue() > bp) {
                bp = e.getValue();
                best = e.getKey();
            }
        }
        if (best >= 0) {
            takeControl(best);
        } else {
            r.faceDone.clear();
            r.faceGuess.clear();
            r.faceTry++;
            r.answerer = null;
            events.add(new Ev("face_none", -1, "", "", r.faceTry, 0));
        }
    }

    public void takeControl(int side) {
        if (phase != Phase.R100 || r == null || side < 0 || side >= sides.size()) {
            return;
        }
        r.control = side;
        r.stage = Round100.PLAY;
        r.strikes = 0;
        Side s = sides.get(side);
        r.turn = s.members.size() > 1 ? Math.floorMod(s.repCursor + 1, s.members.size()) : 0;
        r.answerer = s.members.isEmpty() ? null : s.members.get(r.turn).id;
        r.pending = null;
        events.add(new Ev("control", side, "", "", 0, 0));
        if (r.allRevealed()) {
            endRound(side);
        }
    }

    private void nextTurn() {
        Side s = sides.get(r.control);
        if (s.members.isEmpty()) {
            r.answerer = null;
            return;
        }
        r.turn = Math.floorMod(r.turn + 1, s.members.size());
        r.answerer = s.members.get(r.turn).id;
    }

    private void beginSteal() {
        r.stage = Round100.STEAL;
        r.answerer = null;
        List<Integer> cand = new ArrayList<>();
        for (Side s : sides) {
            if (isStealCandidate(s.index)) {
                cand.add(s.index);
            }
        }
        events.add(new Ev("steal", cand.isEmpty() ? -1 : cand.get(0), "", "", cand.size(), 0));
        if (cand.size() == 1) {
            r.answerer = sides.get(cand.get(0)).rep().id;
        } else if (cand.isEmpty()) {
            endRound(r.control);
        }
    }

    public void endRound(int side) {
        if (phase != Phase.R100 || r == null || r.stage == Round100.END) {
            return;
        }
        r.stage = Round100.END;
        r.winner = side;
        r.answerer = null;
        r.pending = null;
        int pts = r.bank * r.mult;
        if (side >= 0 && side < sides.size()) {
            sides.get(side).score += pts;
        }
        events.add(new Ev("round_win", side, "", "", pts, r.mult));
    }

    /** El presentador destapa lo que falta (sin dar puntos). */
    public void revealRest() {
        if (r == null) {
            return;
        }
        for (int i = 0; i < r.revealed.length; i++) {
            if (!r.revealed[i]) {
                r.revealed[i] = true;
                events.add(new Ev("reveal_rest", -1, "", r.q.answers().get(i).text(), i, r.q.answers().get(i).points()));
            }
        }
    }

    /** El presentador destapa una respuesta a mano: suma al banco y cuenta como acierto del lado en turno. */
    public boolean hostReveal(int idx) {
        if (phase != Phase.R100 || r == null || idx < 0 || idx >= r.revealed.length || r.revealed[idx] || r.stage == Round100.END) {
            return false;
        }
        Participant ans = r.answerer == null ? null : byId(r.answerer);
        int side = ans != null ? sideOf(ans) : r.control;
        r.pending = null;
        if (ans != null) {
            r.answerer = ans.id;
            resolve(idx);
        } else {
            reveal(idx, side);
            if (r.stage == Round100.PLAY && r.allRevealed()) {
                endRound(r.control);
            }
        }
        return true;
    }

    public boolean hostStrike() {
        if (phase != Phase.R100 || r == null || r.stage == Round100.END) {
            return false;
        }
        if (r.answerer != null) {
            r.pending = null;
            resolve(-1);
        } else if (r.stage == Round100.PLAY) {
            r.strikes++;
            events.add(new Ev("strike", r.control, "", "", r.strikes, 0));
            if (r.strikes >= 3) {
                beginSteal();
            }
        }
        return true;
    }

    public boolean hostSetAnswerer(UUID id) {
        if (phase != Phase.R100 || r == null || r.stage == Round100.END || byId(id) == null) {
            return false;
        }
        r.answerer = id;
        r.pending = null;
        return true;
    }

    /** Cierra la ronda de 100 mexicanos y vuelve al menu del show. */
    public void finish100() {
        if (phase == Phase.R100) {
            if (r != null && r.stage != Round100.END) {
                r.stage = Round100.END;
            }
            advanceReps();
            phase = Phase.IDLE;
        }
    }

    // ------------------------------------------------------------ trivia

    public boolean startTrivia(TriviaQ q, int seconds, int nowTick) {
        if (!segmentFree()) {
            return false;
        }
        clearSegments();
        t = new TriviaRound(q, nowTick, nowTick + Math.max(3, seconds) * 20);
        phase = Phase.TRIVIA;
        roundNo++;
        events.add(new Ev("trivia_start", -1, "", q.text(), seconds, 0));
        return true;
    }

    public boolean triviaAnswer(UUID id, int choice, int nowTick) {
        if (phase != Phase.TRIVIA || t == null || t.revealed || nowTick > t.deadlineTick) {
            return false;
        }
        if (byId(id) == null || t.answers.containsKey(id) || choice < 0 || choice >= t.q.options().size()) {
            return false;
        }
        t.answers.put(id, new int[] {choice, nowTick});
        events.add(new Ev("trivia_answer", sideOf(byId(id)), byId(id).name, "", t.answers.size(), players.size()));
        if (t.answers.size() >= players.size()) {
            revealTrivia();
        }
        return true;
    }

    /** Llamar cada tick: cierra la ronda al acabarse el tiempo. */
    public void tick(int nowTick) {
        tickTaboo(nowTick);
        tickDraw(nowTick);
        if (phase == Phase.TRIVIA && t != null && !t.revealed && nowTick >= t.deadlineTick) {
            revealTrivia();
        }
    }

    public void revealTrivia() {
        if (phase != Phase.TRIVIA || t == null || t.revealed) {
            return;
        }
        t.revealed = true;
        List<Map.Entry<UUID, int[]>> right = new ArrayList<>();
        for (Map.Entry<UUID, int[]> e : t.answers.entrySet()) {
            if (e.getValue()[0] == t.q.correct()) {
                right.add(e);
            }
        }
        right.sort((a, b) -> Integer.compare(a.getValue()[1], b.getValue()[1]));
        int rank = 0;
        for (Map.Entry<UUID, int[]> e : right) {
            int pts = RANK_POINTS[Math.min(rank, RANK_POINTS.length - 1)];
            rank++;
            Participant p = byId(e.getKey());
            if (p == null) {
                continue;
            }
            p.points += pts;
            int s = sideOf(p);
            if (s >= 0) {
                sides.get(s).score += pts;
            }
            t.gained.put(e.getKey(), pts);
        }
        events.add(new Ev("trivia_reveal", -1, "", t.q.note(), t.q.correct(), right.size()));
    }

    /** Cierra la trivia y vuelve al menu del show. */
    public void finishTrivia() {
        if (phase == Phase.TRIVIA) {
            if (t != null && !t.revealed) {
                revealTrivia();
            }
            phase = Phase.IDLE;
        }
    }


    // ------------------------------------------------------------ el impostor

    private void givePoints(Participant p, int pts) {
        p.points += pts;
        int s = sideOf(p);
        if (s >= 0) {
            sides.get(s).score += pts;
        }
    }

    /** Empieza El Impostor. {@code shuffled} son los jugadores ya en orden al azar; el primero de {@code pick} es el impostor. */
    public boolean startImpostor(String category, String word, List<UUID> shuffled, int impostorIdx) {
        if (!segmentFree() || players.size() < 3) {
            return false;
        }
        clearSegments();
        List<UUID> order = new ArrayList<>();
        for (UUID id : shuffled) {
            if (byId(id) != null) {
                order.add(id);
            }
        }
        if (order.size() < 3) {
            return false;
        }
        UUID who = order.get(Math.floorMod(impostorIdx, order.size()));
        imp = new ImpostorGame(category, word, who, order);
        phase = Phase.IMPOSTOR;
        roundNo++;
        events.add(new Ev("imp_start", -1, "", category, order.size(), 0));
        return true;
    }

    /** Lo que ve cada quien: la palabra, o null si es el impostor. */
    public String secretFor(UUID id) {
        return imp == null || id.equals(imp.impostor) ? null : imp.word;
    }

    public boolean impostorClue(UUID id, String text) {
        if (phase != Phase.IMPOSTOR || imp == null || !id.equals(imp.current()) || text.isBlank()) {
            return false;
        }
        imp.clues.add(new String[] {byId(id).name, text.trim()});
        imp.step++;
        events.add(new Ev("imp_clue", sideOf(byId(id)), byId(id).name, text.trim(), imp.step, imp.totalClues()));
        if (imp.step >= imp.totalClues()) {
            imp.stage = ImpostorGame.Stage.DISCUSS;
            events.add(Ev.of("imp_discuss"));
        }
        return true;
    }

    /** El presentador salta el turno de quien no contesta. */
    public boolean impostorSkip() {
        if (phase != Phase.IMPOSTOR || imp == null || imp.current() == null) {
            return false;
        }
        imp.clues.add(new String[] {byId(imp.current()).name, "..."});
        imp.step++;
        if (imp.step >= imp.totalClues()) {
            imp.stage = ImpostorGame.Stage.DISCUSS;
            events.add(Ev.of("imp_discuss"));
        }
        return true;
    }

    public boolean impostorStartVote() {
        if (phase != Phase.IMPOSTOR || imp == null || (imp.stage != ImpostorGame.Stage.DISCUSS && imp.stage != ImpostorGame.Stage.CLUES)) {
            return false;
        }
        imp.stage = ImpostorGame.Stage.VOTE;
        imp.votes.clear();
        events.add(Ev.of("imp_vote"));
        return true;
    }

    public boolean impostorVote(UUID voter, UUID target) {
        if (phase != Phase.IMPOSTOR || imp == null || imp.stage != ImpostorGame.Stage.VOTE
                || byId(voter) == null || byId(target) == null || voter.equals(target)) {
            return false;
        }
        imp.votes.put(voter, target);
        events.add(new Ev("imp_voted", sideOf(byId(voter)), byId(voter).name, "", imp.votes.size(), players.size()));
        if (imp.votes.size() >= players.size()) {
            impostorTally();
        }
        return true;
    }

    /** Cuenta los votos. Gana quien tenga mas; si hay empate nadie es acusado y el impostor se salva. */
    public void impostorTally() {
        if (phase != Phase.IMPOSTOR || imp == null || imp.stage != ImpostorGame.Stage.VOTE) {
            return;
        }
        Map<UUID, Integer> count = new java.util.LinkedHashMap<>();
        for (UUID tgt : imp.votes.values()) {
            count.merge(tgt, 1, Integer::sum);
        }
        int best = 0;
        UUID top = null;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> e : count.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                top = e.getKey();
                tie = false;
            } else if (e.getValue() == best) {
                tie = true;
            }
        }
        imp.accused = tie ? null : top;
        imp.impostorCaught = imp.accused != null && imp.accused.equals(imp.impostor);
        if (imp.impostorCaught) {
            imp.stage = ImpostorGame.Stage.GUESS;
            events.add(new Ev("imp_caught", -1, byId(imp.impostor).name, "", best, 0));
        } else {
            events.add(new Ev("imp_escaped", -1, byId(imp.impostor).name, imp.accused == null ? "" : byId(imp.accused).name, best, 0));
            impostorFinish();
        }
    }

    public boolean impostorGuess(UUID id, String text) {
        if (phase != Phase.IMPOSTOR || imp == null || imp.stage != ImpostorGame.Stage.GUESS || !id.equals(imp.impostor)) {
            return false;
        }
        imp.impostorGuess = text.trim();
        imp.impostorGuessedWord = Matcher.same(text, imp.word) || Matcher.containsWord(text, imp.word);
        impostorFinish();
        return true;
    }

    /** El presentador cierra la fase de adivinar si el impostor no contesta. */
    public void impostorGiveUp() {
        if (phase == Phase.IMPOSTOR && imp != null && imp.stage == ImpostorGame.Stage.GUESS) {
            impostorFinish();
        }
    }

    private void impostorFinish() {
        imp.stage = ImpostorGame.Stage.DONE;
        Participant ip = byId(imp.impostor);
        boolean impWins = !imp.impostorCaught || imp.impostorGuessedWord;
        if (!imp.impostorCaught) {
            givePoints(ip, 5);
        } else if (imp.impostorGuessedWord) {
            givePoints(ip, 3);
        } else {
            for (Participant p : players) {
                if (!p.id.equals(imp.impostor)) {
                    givePoints(p, 3);
                }
            }
        }
        events.add(new Ev("imp_end", sideOf(ip), ip.name, imp.word, impWins ? 1 : 0, imp.impostorGuessedWord ? 1 : 0));
    }

    public void finishImpostor() {
        if (phase == Phase.IMPOSTOR) {
            if (imp != null && imp.stage != ImpostorGame.Stage.DONE) {
                imp.stage = ImpostorGame.Stage.DONE;
            }
            phase = Phase.IDLE;
        }
    }

    // ------------------------------------------------------------ adivina la palabra

    /** Empieza una ronda: describe un jugador (se turnan entre los lados) y su lado adivina. */
    public boolean startTaboo(String word, List<String> forbidden, int seconds, int nowTick) {
        if (!segmentFree() || players.size() < 2) {
            return false;
        }
        clearSegments();
        List<Side> live = new ArrayList<>();
        for (Side s : sides) {
            if (!s.members.isEmpty()) {
                live.add(s);
            }
        }
        Side s = live.get(tabooTurn % live.size());
        Participant d = s.members.get((tabooTurn / live.size()) % s.members.size());
        tabooTurn++;
        taboo = new TabooGame(word, forbidden, d.id, s.index, nowTick, nowTick + Math.max(10, seconds) * 20);
        phase = Phase.TABOO;
        roundNo++;
        events.add(new Ev("taboo_start", s.index, d.name, "", seconds, 0));
        return true;
    }

    public boolean isGuesser(UUID id) {
        if (phase != Phase.TABOO || taboo == null || taboo.over || id.equals(taboo.describer) || byId(id) == null) {
            return false;
        }
        Side s = sides.get(taboo.side);
        return s.members.size() > 1 ? s.members.contains(byId(id)) : true;
    }

    /** Resultado de lo que escribio alguien: 0 no es del juego, 1 pista valida, 2 palabra prohibida (ocultar), 3 acerto. */
    public int tabooSay(UUID id, String text, int nowTick) {
        if (phase != Phase.TABOO || taboo == null || taboo.over || text.isBlank()) {
            return 0;
        }
        Participant p = byId(id);
        if (p == null) {
            return 0;
        }
        if (id.equals(taboo.describer)) {
            boolean bad = Matcher.containsWord(text, taboo.word);
            for (String f : taboo.forbidden) {
                bad |= Matcher.containsWord(text, f);
            }
            if (bad) {
                taboo.strikes++;
                events.add(new Ev("taboo_bad", taboo.side, p.name, text, taboo.strikes, 0));
                if (taboo.strikes >= 3) {
                    taboo.over = true;
                    events.add(new Ev("taboo_fail", taboo.side, p.name, taboo.word, 0, 0));
                }
                return 2;
            }
            taboo.lines.add(text.trim());
            events.add(new Ev("taboo_line", taboo.side, p.name, text.trim(), 0, 0));
            return 1;
        }
        if (!isGuesser(id)) {
            return 0;
        }
        if (Matcher.same(text, taboo.word) || Matcher.containsWord(text, taboo.word)) {
            taboo.solved = true;
            taboo.over = true;
            taboo.solver = id;
            int left = Math.max(0, (taboo.deadlineTick - nowTick) / 20);
            taboo.gained = 4 + left / 10 - taboo.strikes;
            taboo.gained = Math.max(1, taboo.gained);
            givePoints(p, taboo.gained);
            Participant d = byId(taboo.describer);
            if (d != null && sideOf(d) != sideOf(p)) {
                givePoints(d, taboo.gained);
            } else if (d != null) {
                d.points += taboo.gained;
            }
            events.add(new Ev("taboo_solved", sideOf(p), p.name, taboo.word, taboo.gained, left));
            return 3;
        }
        events.add(new Ev("taboo_guess", sideOf(p), p.name, text.trim(), 0, 0));
        return 1;
    }

    public void tickTaboo(int nowTick) {
        if (phase == Phase.TABOO && taboo != null && !taboo.over && nowTick >= taboo.deadlineTick) {
            taboo.over = true;
            events.add(new Ev("taboo_timeout", taboo.side, "", taboo.word, 0, 0));
        }
    }

    public void finishTaboo() {
        if (phase == Phase.TABOO) {
            if (taboo != null) {
                taboo.over = true;
            }
            phase = Phase.IDLE;
        }
    }


    // ------------------------------------------------------------ adivina el dibujo

    /** Empieza una ronda de dibujo. {@code drawerId} puede ser null: entonces dibuja el siguiente en turno. */
    public boolean startDraw(String word, int seconds, int nowTick, UUID drawerId) {
        if (!segmentFree() || players.size() < 2) {
            return false;
        }
        Participant d = drawerId == null ? null : byId(drawerId);
        if (d == null) {
            d = players.get(drawTurn % players.size());
            drawTurn++;
        }
        clearSegments();
        draw = new DrawGame(word, d.id, nowTick, nowTick + Math.max(15, seconds) * 20);
        phase = Phase.DRAW;
        roundNo++;
        events.add(new Ev("draw_start", Math.max(0, sideOf(d)), d.name, "", seconds, 0));
        return true;
    }

    public boolean isDrawer(UUID id) {
        return phase == Phase.DRAW && draw != null && !draw.over && id.equals(draw.drawer);
    }

    /** Agrega un trazo del dibujante. Devuelve false si no vale (no es quien dibuja o ya acabo). */
    public boolean addStroke(UUID id, int color, int size, int[] pts) {
        if (!isDrawer(id) || pts.length < 2 || pts.length % 2 != 0 || draw.strokes.size() >= DrawGame.MAX_STROKES) {
            return false;
        }
        int[] c = new int[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            c[i] = Math.max(0, Math.min(DrawGame.W - 1, pts[i]));
            c[i + 1] = Math.max(0, Math.min(DrawGame.H - 1, pts[i + 1]));
        }
        draw.strokes.add(new DrawGame.Stroke(Math.max(0, Math.min(15, color)), Math.max(1, Math.min(6, size)), c));
        return true;
    }

    public boolean clearCanvas(UUID id) {
        if (!isDrawer(id)) {
            return false;
        }
        draw.strokes.clear();
        draw.clearCount++;
        return true;
    }

    /** Puede adivinar: juega, no es quien dibuja, no ha acertado ya. */
    public boolean isDrawGuesser(UUID id) {
        return phase == Phase.DRAW && draw != null && !draw.over && !id.equals(draw.drawer) && byId(id) != null && !draw.solved.contains(id);
    }

    /** Resultado de lo que escribio alguien: 0 no es del juego, 1 intento fallido, 2 ocultar (el dibujante soltando la palabra), 3 acerto. */
    public int drawSay(UUID id, String text, int nowTick) {
        if (phase != Phase.DRAW || draw == null || draw.over || text == null || text.isBlank()) {
            return 0;
        }
        Participant p = byId(id);
        if (p == null) {
            return 0;
        }
        if (id.equals(draw.drawer)) {
            boolean bad = Matcher.same(text, draw.word) || Matcher.containsWord(text, draw.word);
            for (String part : draw.word.split("\\s+")) {
                bad |= part.length() >= 4 && Matcher.containsWord(text, part);
            }
            if (bad) {
                events.add(new Ev("draw_bad", sideOf(p), p.name, text, 0, 0));
                return 2;
            }
            return 0;
        }
        if (!isDrawGuesser(id)) {
            return 0;
        }
        if (Matcher.same(text, draw.word) || Matcher.containsWord(text, draw.word)) {
            int rank = draw.solved.size();
            int pts = RANK_POINTS[Math.min(rank, RANK_POINTS.length - 1)];
            draw.solved.add(id);
            givePoints(p, pts);
            Participant d = byId(draw.drawer);
            if (d != null) {
                givePoints(d, 3);
            }
            events.add(new Ev("draw_solved", sideOf(p), p.name, draw.word, pts, rank + 1));
            int guessers = players.size() - 1;
            if (draw.solved.size() >= guessers) {
                draw.over = true;
                events.add(new Ev("draw_over", sideOf(d), d == null ? "" : d.name, draw.word, draw.solved.size(), 1));
            }
            return 3;
        }
        events.add(new Ev("draw_guess", sideOf(p), p.name, text.trim(), 0, 0));
        return 1;
    }

    public void tickDraw(int nowTick) {
        if (phase == Phase.DRAW && draw != null && !draw.over && nowTick >= draw.deadlineTick) {
            draw.over = true;
            Participant d = byId(draw.drawer);
            events.add(new Ev("draw_over", d == null ? -1 : sideOf(d), d == null ? "" : d.name, draw.word, draw.solved.size(), 0));
        }
    }

    public void finishDraw() {
        if (phase == Phase.DRAW) {
            if (draw != null) {
                draw.over = true;
            }
            phase = Phase.IDLE;
        }
    }

    // ------------------------------------------------------------ puntos y final

    public boolean addScore(int side, int delta) {
        if (side < 0 || side >= sides.size()) {
            return false;
        }
        sides.get(side).score += delta;
        events.add(new Ev("score", side, "", "", delta, sides.get(side).score));
        return true;
    }

    /** Lados con mas puntos (puede haber empate). */
    public List<Side> leaders() {
        int max = Integer.MIN_VALUE;
        for (Side s : sides) {
            if (!s.members.isEmpty()) {
                max = Math.max(max, s.score);
            }
        }
        List<Side> out = new ArrayList<>();
        for (Side s : sides) {
            if (!s.members.isEmpty() && s.score == max) {
                out.add(s);
            }
        }
        return out;
    }

    public void finishShow() {
        if (phase == Phase.LOBBY) {
            return;
        }
        phase = Phase.FINAL;
        List<Side> w = leaders();
        events.add(new Ev("final", w.size() == 1 ? w.get(0).index : -1, "", "", w.size(), 0));
    }

    public List<Ev> drain() {
        List<Ev> out = new ArrayList<>(events);
        events.clear();
        return out;
    }
}
