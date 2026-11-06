package com.showdeaxelis.data;

import com.showdeaxelis.game.Answer100;
import com.showdeaxelis.game.ImpostorGame;
import com.showdeaxelis.game.Participant;
import com.showdeaxelis.game.Phase;
import com.showdeaxelis.game.Round100;
import com.showdeaxelis.game.Show;
import com.showdeaxelis.game.Side;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/** Arma lo que cada jugador puede ver del show. Lo escondido (respuestas, palabra secreta, impostor) solo va a quien le toca saberlo. */
public final class ShowSync {
    private ShowSync() {
    }

    private static ListTag strings(Iterable<String> it) {
        ListTag l = new ListTag();
        for (String s : it) {
            l.add(StringTag.valueOf(s));
        }
        return l;
    }

    private static String name(Show s, UUID id) {
        if (id == null) {
            return "";
        }
        Participant p = s.byId(id);
        return p == null ? "" : p.name;
    }

    public static CompoundTag empty() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", false);
        return t;
    }

    public static CompoundTag build(Show s, UUID viewer, int now, StageData.Info stage) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", true);
        boolean host = viewer.equals(s.host);
        Participant me = s.byId(viewer);
        // el presentador que tambien juega no ve secretos (si lo prefiere asi): ve lo mismo que un jugador
        boolean sees = host && !(s.hostPlays && s.hideSecrets);
        CompoundTag you = new CompoundTag();
        you.putBoolean("host", host);
        you.putBoolean("player", me != null);
        you.putInt("side", me == null ? -1 : s.sideOf(me));
        t.put("you", you);
        t.putString("phase", s.phase.name());
        t.putInt("format", s.format.ordinal());
        t.putString("hostName", s.hostName);
        t.putBoolean("hostPlays", s.hostPlays);
        t.putBoolean("autoWrong", s.autoWrong);
        t.putBoolean("voice", s.voice);
        t.putBoolean("manual", s.manual);
        t.putBoolean("hideSecrets", s.hideSecrets);
        t.putBoolean("sees", sees);
        t.putBoolean("canStart", s.canStart());
        t.putInt("round", s.roundNo);
        if (stage != null) {
            CompoundTag st = new CompoundTag();
            st.putInt("x", stage.x());
            st.putInt("y", stage.y());
            st.putInt("z", stage.z());
            st.putInt("d", stage.dir());
            t.put("stage", st);
        }
        ListTag sides = new ListTag();
        for (Side sd : s.sides) {
            CompoundTag c = new CompoundTag();
            c.putString("n", sd.name);
            c.putInt("c", sd.color);
            c.putInt("s", sd.score);
            ListTag ms = new ListTag();
            for (Participant p : sd.members) {
                ms.add(StringTag.valueOf(p.name));
            }
            c.put("m", ms);
            Participant rep = sd.rep();
            c.putString("rep", rep == null ? "" : rep.name);
            sides.add(c);
        }
        t.put("sides", sides);
        ListTag pl = new ListTag();
        for (Participant p : s.players) {
            CompoundTag c = new CompoundTag();
            c.putString("n", p.name);
            c.putInt("side", s.sideOf(p));
            c.putInt("pts", p.points);
            pl.add(c);
        }
        t.put("players", pl);

        if (s.phase == Phase.R100 && s.r != null) {
            Round100 r = s.r;
            CompoundTag c = new CompoundTag();
            c.putString("q", host || r.qShown ? r.q.text() : "");
            c.putBoolean("qs", r.qShown);
            c.putBoolean("open", r.open);
            c.putInt("fw", r.faceWinner);
            c.putInt("stage", r.stage);
            c.putInt("bank", r.bank);
            c.putInt("mult", r.mult);
            c.putInt("strikes", r.strikes);
            c.putInt("control", r.control);
            c.putString("ans", name(s, r.answerer));
            c.putString("pending", r.pending == null ? "" : r.pending);
            c.putInt("faceTry", r.faceTry);
            ListTag as = new ListTag();
            for (int i = 0; i < r.revealed.length; i++) {
                Answer100 a = r.q.answers().get(i);
                CompoundTag ac = new CompoundTag();
                boolean show = r.revealed[i];
                ac.putBoolean("rev", show);
                if (show || sees) {
                    ac.putString("t", a.text());
                    ac.putInt("p", a.points());
                }
                as.add(ac);
            }
            c.put("a", as);
            t.put("r", c);
        }
        if (s.phase == Phase.TRIVIA && s.t != null) {
            CompoundTag c = new CompoundTag();
            c.putString("q", s.t.q.text());
            c.putString("cat", s.t.q.category());
            c.put("op", strings(s.t.q.options()));
            c.putInt("left", Math.max(0, s.t.deadlineTick - now));
            c.putInt("total", s.t.deadlineTick - s.t.startTick);
            c.putBoolean("rev", s.t.revealed);
            if (s.t.revealed || sees) {
                c.putInt("ok", s.t.q.correct());
                c.putString("note", s.t.q.note());
            }
            ListTag done = new ListTag();
            for (UUID id : s.t.answers.keySet()) {
                done.add(StringTag.valueOf(name(s, id)));
            }
            c.put("done", done);
            int[] mine = s.t.answers.get(viewer);
            c.putInt("mine", mine == null ? -1 : mine[0]);
            if (s.t.revealed) {
                ListTag g = new ListTag();
                for (var e : s.t.gained.entrySet()) {
                    g.add(StringTag.valueOf(name(s, e.getKey()) + "|" + e.getValue()));
                }
                c.put("gained", g);
                ListTag ch = new ListTag();
                for (var e : s.t.answers.entrySet()) {
                    ch.add(StringTag.valueOf(name(s, e.getKey()) + "|" + e.getValue()[0]));
                }
                c.put("choices", ch);
            }
            t.put("t", c);
        }
        if (s.phase == Phase.IMPOSTOR && s.imp != null) {
            ImpostorGame g = s.imp;
            boolean done = g.stage == ImpostorGame.Stage.DONE;
            CompoundTag c = new CompoundTag();
            c.putString("stage", g.stage.name());
            c.putString("cat", g.category);
            c.putString("turn", name(s, g.current()));
            c.putInt("step", g.step);
            c.putInt("total", g.totalClues());
            ListTag cl = new ListTag();
            for (String[] x : g.clues) {
                cl.add(StringTag.valueOf(x[0] + "|" + x[1]));
            }
            c.put("clues", cl);
            c.putInt("votes", g.votes.size());
            c.putInt("n", s.players.size());
            c.putBoolean("iVoted", g.votes.containsKey(viewer));
            c.putString("acc", name(s, g.accused));
            c.putBoolean("caught", g.impostorCaught);
            boolean isImp = viewer.equals(g.impostor);
            c.putBoolean("isImp", isImp);
            if (!isImp || done) {
                if (me != null || sees || done) {
                    c.putString("word", g.word);
                }
            }
            if (sees || done) {
                c.putString("imp", name(s, g.impostor));
            }
            c.putString("guess", g.impostorGuess);
            c.putBoolean("guessed", g.impostorGuessedWord);
            ListTag names = new ListTag();
            for (java.util.UUID id : g.order) {
                names.add(StringTag.valueOf(name(s, id)));
            }
            c.put("order", names);
            t.put("imp", c);
        }
        if (s.phase == Phase.TABOO && s.taboo != null) {
            var g = s.taboo;
            CompoundTag c = new CompoundTag();
            c.putString("desc", name(s, g.describer));
            c.putInt("side", g.side);
            c.putInt("left", Math.max(0, g.deadlineTick - now));
            c.putInt("total", g.deadlineTick - g.startTick);
            c.putInt("strikes", g.strikes);
            c.putBoolean("over", g.over);
            c.putBoolean("solved", g.solved);
            c.putBoolean("iGuess", s.isGuesser(viewer));
            c.putInt("gained", g.gained);
            c.putString("solver", name(s, g.solver));
            boolean sees2 = sees || viewer.equals(g.describer) || g.over;
            if (sees2) {
                c.putString("word", g.word);
                c.put("no", strings(g.forbidden));
            }
            c.put("lines", strings(g.lines.size() > 6 ? g.lines.subList(g.lines.size() - 6, g.lines.size()) : g.lines));
            t.put("tb", c);
        }
        if (s.phase == Phase.DRAW && s.draw != null) {
            var g = s.draw;
            CompoundTag c = new CompoundTag();
            c.putString("drawer", name(s, g.drawer));
            c.putInt("side", Math.max(0, s.sideOf(s.byId(g.drawer))));
            c.putInt("left", Math.max(0, g.deadlineTick - now));
            c.putInt("total", g.deadlineTick - g.startTick);
            c.putBoolean("over", g.over);
            c.putBoolean("iDraw", viewer.equals(g.drawer));
            c.putString("mask", g.mask(now));
            c.putInt("round", s.roundNo);
            ListTag solved = new ListTag();
            for (UUID id : g.solved) {
                solved.add(StringTag.valueOf(name(s, id)));
            }
            c.put("solved", solved);
            c.putBoolean("iSolved", g.solved.contains(viewer));
            if (sees || viewer.equals(g.drawer) || g.over) {
                c.putString("word", g.word);
            }
            t.put("dr", c);
        }
        return t;
    }
}
