package com.showdeaxelis.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.showdeaxelis.game.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * La pantalla gigante del estudio y los letreros de marcador sobre cada podio. Son entidades de texto de vanilla (text_display):
 * se ven desde cualquier lugar del estudio, no necesitan mod instalado para dibujarse y todos ven lo mismo. Solo muestran lo
 * publico (lo mismo que ve alguien del publico), asi que nunca delatan respuestas ni al impostor.
 *
 * Disposicion de la pared (alturas del mundo local): titulo [17,19), pregunta [15,17), 4 filas de 2 columnas [7,15), pie [5,7).
 */
public final class Displays {
    private static final String TAG = "show_disp_";
    private static final UUID PUBLIC = new UUID(0L, 0L);
    private static final int[] OPT_COLORS = {0xFF5555, 0x5599FF, 0xFFE055, 0x55DD66};
    private static final String[] LETTERS = {"A", "B", "C", "D"};
    private static final int GOLD = 0xFFC107;
    private static final int WHITE = 0xFFFFFF;
    private static final int GRAY = 0x9AA0B8;
    private static final int GREEN = 0x55EE66;
    private static final int RED = 0xFF5555;
    private static final Map<String, UUID> ents = new HashMap<>();
    private static final Map<String, String> sigs = new HashMap<>();
    /** Si el texto de la pantalla sale de espaldas, /show pantalla girar lo voltea. */
    public static boolean flip;
    /** Se sube cada vez que se cambia algo global (para forzar el redibujado). */
    private static int epoch;

    private Displays() {
    }

    // ------------------------------------------------------------ texto con colores

    private record Seg(String t, int rgb, boolean bold) {
    }

    private static Seg s(String t, int rgb) {
        return new Seg(t, rgb, false);
    }

    private static Seg b(String t, int rgb) {
        return new Seg(t, rgb, true);
    }

    private static List<Seg> line(Seg... segs) {
        return new ArrayList<>(List.of(segs));
    }

    private static String json(List<List<Seg>> lines) {
        JsonArray arr = new JsonArray();
        arr.add("");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                JsonObject nl = new JsonObject();
                nl.addProperty("text", "\n");
                arr.add(nl);
            }
            for (Seg sg : lines.get(i)) {
                JsonObject o = new JsonObject();
                o.addProperty("text", sg.t());
                o.addProperty("color", String.format(Locale.ROOT, "#%06X", sg.rgb() & 0xFFFFFF));
                if (sg.bold()) {
                    o.addProperty("bold", true);
                }
                arr.add(o);
            }
        }
        return arr.toString();
    }

    private static String cut(String t, int max) {
        return t.length() <= max ? t : t.substring(0, Math.max(0, max - 1)) + ".";
    }

    /** Parte un texto largo en renglones de ancho maximo. */
    private static List<String> wrap(String text, int max) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && cur.length() + 1 + w.length() > max) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) {
                cur.append(' ');
            }
            cur.append(w);
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    // ------------------------------------------------------------ las piezas

    private record Spec(String key, double x, double y, double z, boolean fixed, double scale, double widthBlocks, List<List<Seg>> lines) {
    }

    /** Un solo renglon en un hueco de la tabla; el tamano baja si el texto es largo. */
    private static void slot(List<Spec> out, int row, int col, List<Seg> text, int chars) {
        double scale = chars <= 19 ? 5.0 : chars <= 25 ? 3.9 : chars <= 33 ? 3.0 : 2.4;
        double x = col == 0 ? -9.5 : 9.5;
        double y = 14.0 - 2.0 * row;
        out.add(new Spec("s" + (row + 4 * col), x, y, Studio.BACK_Z - 0.55, true, scale, 17.5, List.of(text)));
    }

    private static void band(List<Spec> out, String key, double y, double scale, List<List<Seg>> lines) {
        out.add(new Spec(key, 0, y, Studio.BACK_Z - 0.55, true, scale, 37, lines));
    }

    /** La pregunta: en un renglon grande si cabe, si no en dos mas chicos. */
    private static void questionBand(List<Spec> out, String q) {
        if (q.length() <= 44) {
            band(out, "q", 16.0, 4.4, List.of(line(b(q, WHITE))));
            return;
        }
        List<List<Seg>> ql = new ArrayList<>();
        for (String w : wrap(q, 58)) {
            ql.add(line(b(w, WHITE)));
        }
        band(out, "q", 16.0, 3.4, ql);
    }

    private static CompoundTag sideTag(CompoundTag pub, int i) {
        return pub.getList("sides", 10).getCompound(i);
    }

    private static String names(CompoundTag side) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < side.getList("m", 8).size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(side.getList("m", 8).getString(i));
        }
        return sb.toString();
    }

    private static String clock(int ticks) {
        return Math.max(0, (ticks + 19) / 20) + " s";
    }

    private static List<Spec> screen(CompoundTag pub) {
        List<Spec> out = new ArrayList<>();
        if (pub == null || !pub.getBoolean("active")) {
            band(out, "title", 18.0, 6.0, List.of(line(b("SHOW DE AXELIS", GOLD))));
            band(out, "q", 16.0, 4.5, List.of(line(s("Bienvenidos al programa", WHITE))));
            band(out, "foot", 6.0, 3.2, List.of(line(s("El presentador pulsa Y para abrir el panel y armar el show", GRAY))));
            return out;
        }
        String phase = pub.getString("phase");
        int round = pub.getInt("round");
        ListTag sides = pub.getList("sides", 10);
        switch (phase) {
            case "R100" -> r100(out, pub, round);
            case "TRIVIA" -> trivia(out, pub, round);
            case "IMPOSTOR" -> impostor(out, pub, round);
            case "TABOO" -> taboo(out, pub, round);
            case "DRAW" -> draw(out, pub, round);
            case "FINAL" -> ranking(out, pub, "FIN DEL SHOW", true);
            case "LOBBY" -> {
                band(out, "title", 18.0, 6.0, List.of(line(b("SHOW DE AXELIS", GOLD))));
                band(out, "q", 16.0, 4.2, List.of(line(s("Armando equipos... ya casi empezamos", WHITE))));
                int row = 0;
                int col = 0;
                for (int i = 0; i < sides.size() && row < 4; i++) {
                    CompoundTag sd = sides.getCompound(i);
                    if (sd.getList("m", 8).isEmpty()) {
                        continue;
                    }
                    String t = cut(sd.getString("n"), 12) + ": " + cut(names(sd), 22);
                    slot(out, row, col, line(b(t, sd.getInt("c"))), t.length());
                    if (++row == 4 && col == 0) {
                        row = 0;
                        col = 1;
                    }
                }
                band(out, "foot", 6.0, 3.2, List.of(line(s("Jugadores: " + pub.getList("players", 10).size() + "   |   " + Format.byOrdinal(pub.getInt("format")).label, GRAY))));
            }
            default -> ranking(out, pub, "SHOW DE AXELIS", false);
        }
        return out;
    }

    private static void ranking(List<Spec> out, CompoundTag pub, String title, boolean fin) {
        ListTag sides = pub.getList("sides", 10);
        List<CompoundTag> list = new ArrayList<>();
        for (int i = 0; i < sides.size(); i++) {
            if (!sides.getCompound(i).getList("m", 8).isEmpty()) {
                list.add(sides.getCompound(i));
            }
        }
        list.sort((x, y) -> Integer.compare(y.getInt("s"), x.getInt("s")));
        band(out, "title", 18.0, 6.0, List.of(line(b(title, GOLD))));
        String q = "Marcador";
        if (fin && !list.isEmpty()) {
            boolean tie = list.size() > 1 && list.get(1).getInt("s") == list.get(0).getInt("s");
            q = tie ? "Empate en el primer lugar" : "Ganador: " + list.get(0).getString("n");
        } else if (!fin) {
            q = "Un momento, viene el siguiente juego";
        }
        band(out, "q", 16.0, 4.5, List.of(line(b(q, WHITE))));
        int row = 0;
        int col = 0;
        for (int i = 0; i < list.size() && i < 6; i++) {
            CompoundTag sd = list.get(i);
            String t = (i + 1) + ". " + cut(sd.getString("n"), 14) + "  " + sd.getInt("s");
            slot(out, row, col, line(b(t, sd.getInt("c"))), t.length());
            if (++row == 3) {
                row = 0;
                col = 1;
            }
        }
        band(out, "foot", 6.0, 3.2, List.of(line(s(fin ? "Gracias por jugar" : "Ronda " + pub.getInt("round") + " terminada", GRAY))));
    }

    private static void r100(List<Spec> out, CompoundTag pub, int round) {
        CompoundTag r = pub.getCompound("r");
        band(out, "title", 18.0, 6.0, List.of(line(b("100 MEXICANOS DIJERON", GOLD), s("   Ronda " + round, GRAY))));
        String qtxt = r.getString("q");
        if (qtxt.isEmpty()) {
            qtxt = "¡Que pasen los representantes!";
        }
        questionBand(out, qtxt);
        ListTag as = r.getList("a", 10);
        for (int i = 0; i < as.size() && i < 8; i++) {
            CompoundTag a = as.getCompound(i);
            int row = i % 4;
            int col = i / 4;
            if (a.getBoolean("rev")) {
                String t = cut(a.getString("t"), 18);
                slot(out, row, col, line(b((i + 1) + "  ", GOLD), b(t.toUpperCase(Locale.ROOT), WHITE), b("   " + a.getInt("p"), 0xFFEB3B)), t.length() + 8);
            } else {
                slot(out, row, col, line(b((i + 1) + "  ", GOLD), s("_ _ _ _ _", GRAY)), 14);
            }
        }
        int stage = r.getInt("stage");
        int control = r.getInt("control");
        String ctl = control >= 0 && control < pub.getList("sides", 10).size() ? sideTag(pub, control).getString("n") : "";
        String who = switch (stage) {
            case 4 -> "Pasan los representantes al frente";
            case 5 -> "Ganó " + (r.getInt("fw") >= 0 && r.getInt("fw") < pub.getList("sides", 10).size() ? sideTag(pub, r.getInt("fw")).getString("n") : "?") + ": ¿juega o pasa?";
            case 0 -> r.getBoolean("qs") ? "¡A la campana! El más rápido contesta" : "Los representantes se preparan";
            case 1 -> "Juega: " + ctl + (r.getString("ans").isEmpty() ? "" : "  (contesta " + r.getString("ans") + ")");
            case 2 -> "¡Robo! Contesta " + (r.getString("ans").isEmpty() ? "el otro equipo" : r.getString("ans"));
            default -> "Ronda terminada";
        };
        int strikes = r.getInt("strikes");
        List<Seg> foot = line(s("Banco " + r.getInt("bank") + (r.getInt("mult") > 1 ? " (x" + r.getInt("mult") + ")" : ""), GOLD), s("    " + who, WHITE));
        if (strikes > 0) {
            foot.add(b("    " + "X ".repeat(strikes).trim(), RED));
        }
        band(out, "foot", 6.0, 3.2, List.of(foot));
    }

    private static void trivia(List<Spec> out, CompoundTag pub, int round) {
        CompoundTag t = pub.getCompound("t");
        band(out, "title", 18.0, 6.0, List.of(line(b("TRIVIA", GOLD), s("   " + t.getString("cat"), GRAY))));
        questionBand(out, t.getString("q"));
        boolean rev = t.getBoolean("rev");
        int ok = t.getInt("ok");
        ListTag op = t.getList("op", 8);
        for (int i = 0; i < op.size() && i < 4; i++) {
            String txt = cut(op.getString(i), 21);
            int row = i / 2;
            int col = i % 2;
            int color = OPT_COLORS[i];
            if (rev) {
                boolean good = i == ok;
                slot(out, row, col, line(b(LETTERS[i] + "  ", good ? GREEN : GRAY), good ? b(txt, GREEN) : s(txt, GRAY)), txt.length() + 3);
            } else {
                slot(out, row, col, line(b(LETTERS[i] + "  ", color), b(txt, WHITE)), txt.length() + 3);
            }
        }
        int total = pub.getList("players", 10).size();
        int done = t.getList("done", 8).size();
        if (!rev) {
            slot(out, 2, 0, line(b("Tiempo  " + clock(t.getInt("left")), t.getInt("left") < 200 ? RED : WHITE)), 14);
            slot(out, 2, 1, line(s("Contestaron  " + done + "/" + total, WHITE)), 16);
            band(out, "foot", 6.0, 3.2, List.of(line(s("Jugadores: elijan A, B, C o D con su teclado", GRAY))));
        } else {
            String note = t.getString("note");
            ListTag g = t.getList("gained", 8);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < g.size() && i < 4; i++) {
                String[] p = g.getString(i).split("\\|");
                sb.append(sb.length() > 0 ? "   " : "").append(p[0]).append(" +").append(p.length > 1 ? p[1] : "");
            }
            slot(out, 2, 0, line(b(sb.length() == 0 ? "Nadie acerto" : cut(sb.toString(), 34), sb.length() == 0 ? RED : GREEN)), 30);
            band(out, "foot", 6.0, 3.2, List.of(line(s(cut(note, 90), GRAY))));
        }
    }

    private static void impostor(List<Spec> out, CompoundTag pub, int round) {
        CompoundTag c = pub.getCompound("imp");
        String stage = c.getString("stage");
        band(out, "title", 18.0, 6.0, List.of(line(b("EL IMPOSTOR", GOLD), s("   " + c.getString("cat"), GRAY))));
        String q;
        int color = WHITE;
        switch (stage) {
            case "CLUES" -> q = "Turno de " + c.getString("turn") + ": di tu pista";
            case "DISCUSS" -> q = "¡A discutir! ¿Quien es el impostor?";
            case "VOTE" -> q = "¡A votar!   Votos: " + c.getInt("votes") + "/" + c.getInt("n");
            case "GUESS" -> q = "El impostor fue descubierto... ¡adivina la palabra!";
            default -> {
                q = c.getBoolean("caught") ? "Impostor: " + c.getString("imp") + "   Palabra: " + c.getString("word").toUpperCase(Locale.ROOT)
                        : "El impostor era " + c.getString("imp") + "  (la palabra: " + c.getString("word").toUpperCase(Locale.ROOT) + ")";
                color = c.getBoolean("caught") ? GREEN : RED;
            }
        }
        band(out, "q", 16.0, q.length() > 40 ? 3.4 : 4.4, List.of(line(b(q, color))));
        ListTag cl = c.getList("clues", 8);
        int start = Math.max(0, cl.size() - 8);
        for (int i = start; i < cl.size(); i++) {
            String[] p = cl.getString(i).split("\\|", 2);
            String t = cut(p[0], 9) + ": " + cut(p.length > 1 ? p[1] : "", 16);
            int k = i - start;
            slot(out, k % 4, k / 4, line(b(cut(p[0], 9) + ": ", GOLD), b(cut(p.length > 1 ? p[1] : "", 16), WHITE)), t.length());
        }
        band(out, "foot", 6.0, 3.2, List.of(line(s("Pista " + Math.min(c.getInt("step") + 1, c.getInt("total")) + " de " + c.getInt("total"), GRAY))));
    }

    private static void taboo(List<Spec> out, CompoundTag pub, int round) {
        CompoundTag c = pub.getCompound("tb");
        band(out, "title", 18.0, 6.0, List.of(line(b("ADIVINA LA PALABRA", GOLD), s("   Ronda " + round, GRAY))));
        boolean over = c.getBoolean("over");
        String q;
        int color = WHITE;
        if (over) {
            q = (c.getBoolean("solved") ? "¡Acertaron! " : "Se acabo. ") + "La palabra era: " + c.getString("word").toUpperCase(Locale.ROOT);
            color = c.getBoolean("solved") ? GREEN : RED;
        } else {
            q = c.getString("desc") + " describe la palabra";
        }
        band(out, "q", 16.0, q.length() > 40 ? 3.4 : 4.4, List.of(line(b(q, color))));
        ListTag lines = c.getList("lines", 8);
        for (int i = 0; i < lines.size() && i < 6; i++) {
            String t = cut(lines.getString(i), 34);
            slot(out, i % 4, i / 4, line(s(t, WHITE)), t.length());
        }
        List<Seg> foot = line(s("Tiempo " + clock(c.getInt("left")), WHITE));
        if (c.getInt("strikes") > 0) {
            foot.add(b("    Prohibidas: " + "X ".repeat(c.getInt("strikes")).trim(), RED));
        }
        band(out, "foot", 6.0, 3.2, List.of(foot));
    }

    private static void draw(List<Spec> out, CompoundTag pub, int round) {
        CompoundTag c = pub.getCompound("dr");
        band(out, "title", 18.0, 6.0, List.of(line(b("ADIVINA EL DIBUJO", GOLD), s("   Ronda " + round, GRAY))));
        boolean over = c.getBoolean("over");
        String mask = c.getString("mask");
        String q = over ? "Era: " + c.getString("word").toUpperCase(Locale.ROOT) : mask;
        band(out, "q", 16.0, 4.6, List.of(line(b(q, over ? GREEN : WHITE))));
        slot(out, 0, 0, line(b("Dibuja: ", GOLD), b(cut(c.getString("drawer"), 12), WHITE)), 20);
        slot(out, 1, 0, line(b("Tiempo  " + clock(c.getInt("left")), c.getInt("left") < 200 ? RED : WHITE)), 14);
        ListTag solved = c.getList("solved", 8);
        slot(out, 0, 1, line(b("Acertaron: " + solved.size(), GREEN)), 14);
        for (int i = 0; i < solved.size() && i < 3; i++) {
            String t = (i + 1) + ". " + cut(solved.getString(i), 14);
            slot(out, 1 + i, 1, line(s(t, WHITE)), t.length());
        }
        band(out, "foot", 6.0, 3.2, List.of(line(s("Dibuja con J; los demas escriben su respuesta", GRAY))));
    }

    /** Letreros sobre los podios: nombre y puntos de cada lado que juega ahi. */
    private static List<Spec> signs(CompoundTag pub) {
        List<Spec> out = new ArrayList<>();
        if (pub == null || !pub.getBoolean("active")) {
            return out;
        }
        Format fmt = Format.byOrdinal(pub.getInt("format"));
        String phase = pub.getString("phase");
        boolean scores = !phase.equals("LOBBY");
        int turnSide = -1;
        if (phase.equals("R100")) {
            turnSide = pub.getCompound("r").getInt("control");
        } else if (phase.equals("TABOO")) {
            turnSide = pub.getCompound("tb").getInt("side");
        } else if (phase.equals("DRAW")) {
            turnSide = pub.getCompound("dr").getInt("side");
        }
        ListTag sides = pub.getList("sides", 10);
        List<List<Seg>>[] perPod = new List[3];
        for (int i = 0; i < 3; i++) {
            perPod[i] = new ArrayList<>();
        }
        for (int i = 0; i < sides.size(); i++) {
            CompoundTag sd = sides.getCompound(i);
            if (sd.getList("m", 8).isEmpty()) {
                continue;
            }
            int pod = Studio.podiumOfSide(fmt, i)[0];
            boolean turn = i == turnSide;
            String name = (turn ? "> " : "") + cut(sd.getString("n"), 14);
            perPod[pod].add(line(b(name, sd.getInt("c"))));
            if (scores) {
                perPod[pod].add(line(b(String.valueOf(sd.getInt("s")), turn ? GOLD : WHITE)));
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!perPod[i].isEmpty()) {
                out.add(new Spec("pod" + i, Studio.podiumCenter(i), 6.4, Studio.PODIUM_Z + 0.5, false, 3.2, 9, perPod[i]));
            }
        }
        return out;
    }

    // ------------------------------------------------------------ entidades

    private static ListTag fl(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    private static ListTag db(double... v) {
        ListTag l = new ListTag();
        for (double d : v) {
            l.add(DoubleTag.valueOf(d));
        }
        return l;
    }

    private static CompoundTag tagFor(StageData.Info st, Spec sp, UUID id) {
        double[] p = Studio.atD(st, sp.x(), sp.y(), sp.z());
        double lineH = 10 * 0.025 * sp.scale();
        double h = lineH * sp.lines().size();
        // el texto crece hacia arriba desde el punto de la entidad: se baja medio bloque de texto para que quede centrado en y
        double y = p[1] - h / 2.0;
        Direction f = Studio.dirOf(st.dir());
        float yaw;
        if (sp.fixed()) {
            // el texto se ve desde el lado hacia donde mira la entidad: mira a los jugadores (contra el sentido del estudio)
            yaw = (float) Math.toDegrees(Math.atan2(f.getStepX(), -f.getStepZ())) + (flip ? 180f : 0f);
        } else {
            yaw = 0f;
        }
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:text_display");
        t.put("Pos", db(p[0], y, p[2]));
        t.put("Motion", db(0, 0, 0));
        t.put("Rotation", fl(yaw, 0f));
        t.putUUID("UUID", id);
        t.putString("text", json(sp.lines()));
        t.putInt("line_width", (int) Math.round(sp.widthBlocks() * 1.6 / (0.025 * sp.scale())));
        t.putInt("background", 0);
        t.putString("alignment", "center");
        t.putString("billboard", sp.fixed() ? "fixed" : "vertical");
        CompoundTag br = new CompoundTag();
        br.putInt("sky", 15);
        br.putInt("block", 15);
        t.put("brightness", br);
        t.putFloat("view_range", 3.0f);
        t.putFloat("shadow_radius", 0f);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", fl(0f, 0f, 0f, 1f));
        tr.put("right_rotation", fl(0f, 0f, 0f, 1f));
        tr.put("scale", fl((float) sp.scale(), (float) sp.scale(), (float) sp.scale()));
        tr.put("translation", fl(0f, 0f, 0f));
        t.put("transformation", tr);
        return t;
    }

    private static List<Display.TextDisplay> existing(ServerLevel lv, StageData.Info st, String key) {
        String tag = TAG + key;
        return lv.getEntitiesOfClass(Display.TextDisplay.class, Studio.studioArea(st), e -> e.getTags().contains(tag));
    }

    private static void apply(ServerLevel lv, StageData.Info st, Spec sp) {
        double[] p = Studio.atD(st, sp.x(), sp.y(), sp.z());
        if (!lv.hasChunkAt(BlockPos.containing(p[0], p[1], p[2]))) {
            return;
        }
        String sig = epoch + "|" + st.x() + "," + st.y() + "," + st.z() + "," + st.dir() + "|" + sp.x() + "," + sp.y() + "|" + json(sp.lines()) + "|" + sp.scale();
        Entity e = null;
        UUID id = ents.get(sp.key());
        if (id != null) {
            e = lv.getEntity(id);
            if (e != null && e.isRemoved()) {
                e = null;
            }
        }
        if (e == null) {
            List<Display.TextDisplay> found = existing(lv, st, sp.key());
            for (int i = 0; i < found.size(); i++) {
                if (i == 0) {
                    e = found.get(0);
                } else {
                    found.get(i).discard();
                }
            }
            if (e != null) {
                sigs.remove(sp.key());
            }
        }
        if (e != null && sig.equals(sigs.get(sp.key()))) {
            return;
        }
        if (e == null) {
            e = EntityType.TEXT_DISPLAY.create(lv);
            if (e == null) {
                return;
            }
            e.addTag(TAG + sp.key());
            e.load(tagFor(st, sp, e.getUUID()));
            lv.addFreshEntity(e);
        } else {
            e.load(tagFor(st, sp, e.getUUID()));
        }
        ents.put(sp.key(), e.getUUID());
        sigs.put(sp.key(), sig);
    }

    /** Pone en las pantallas lo que corresponde. {@code pub} es null cuando no hay show. */
    public static void update(ServerLevel lv, StageData.Info st, CompoundTag pub) {
        List<Spec> specs = screen(pub);
        specs.addAll(signs(pub));
        Set<String> keep = new HashSet<>();
        for (Spec sp : specs) {
            keep.add(sp.key());
            apply(lv, st, sp);
        }
        // lo que ya no se usa (huecos vacios, letreros de podios sin nadie) se quita
        for (String key : new ArrayList<>(ents.keySet())) {
            if (!keep.contains(key)) {
                removeKey(lv, st, key);
            }
        }
        for (String key : new String[] {"s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "pod0", "pod1", "pod2"}) {
            if (!keep.contains(key) && !ents.containsKey(key)) {
                for (Display.TextDisplay d : existing(lv, st, key)) {
                    d.discard();
                }
            }
        }
    }

    private static void removeKey(ServerLevel lv, StageData.Info st, String key) {
        UUID id = ents.remove(key);
        sigs.remove(key);
        if (id != null) {
            Entity e = lv.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        for (Display.TextDisplay d : existing(lv, st, key)) {
            d.discard();
        }
    }

    /** Borra todas las pantallas del estudio (al reconstruirlo o moverlo). */
    public static void clearAll(ServerLevel lv, StageData.Info st) {
        if (st == null) {
            return;
        }
        for (Display.TextDisplay d : lv.getEntitiesOfClass(Display.TextDisplay.class, Studio.studioArea(st), e -> {
            for (String t : e.getTags()) {
                if (t.startsWith(TAG)) {
                    return true;
                }
            }
            return false;
        })) {
            d.discard();
        }
        ents.clear();
        sigs.clear();
        epoch++;
    }

    /** Fuerza que todo se redibuje (por ejemplo despues de /show pantalla girar). */
    public static void refresh() {
        epoch++;
    }
}
