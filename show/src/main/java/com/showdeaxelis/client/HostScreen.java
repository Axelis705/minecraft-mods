package com.showdeaxelis.client;

import com.showdeaxelis.game.Format;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/**
 * El panel del presentador (tecla Y): elegir si presenta o juega, armar equipos, lanzar segmentos, mover el marcador, efectos y camaras.
 * Se queda a la derecha para que se siga viendo el programa. Los botones cambian segun lo que este pasando.
 */
public final class HostScreen extends Screen {
    private static HostScreen current;
    private static int tab;
    private static final String[] TABS = {"Juego", "Efectos", "Cámara", "Puntos", "Más"};
    public static final int PW = 196;

    private final List<int[]> notes = new ArrayList<>();
    private final List<net.minecraft.util.FormattedCharSequence> noteText = new ArrayList<>();
    private int cx;
    private int cy;
    private int col;
    private String sig = "";

    public HostScreen() {
        super(Component.literal("Show de Axelis"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new HostScreen());
    }

    public static void stateChanged() {
        // se reconstruye en el siguiente tick si cambia lo que muestra
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        current = this;
        sig = signature();
        notes.clear();
        noteText.clear();
        cx = width - PW - 6;
        cy = 12;
        col = 0;
        CompoundTag st = ShowClient.state;
        if (!st.getBoolean("active")) {
            buildRole();
            return;
        }
        if (!st.getCompound("you").getBoolean("host")) {
            note("El show ya esta en marcha.");
            note("Lo controla " + st.getString("hostName") + ".");
            return;
        }
        String phase = st.getString("phase");
        if (phase.equals("LOBBY")) {
            buildLobby(st);
            return;
        }
        int tx = cx;
        int totalW = 0;
        for (String t : TABS) {
            totalW += font.width(t) + 10;
        }
        int gap = Math.max(0, Math.min(2, (PW - totalW) / (TABS.length - 1)));
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            int w = font.width(TABS[i]) + 10;
            Component lab = i == tab ? Component.literal(TABS[i]).withStyle(net.minecraft.ChatFormatting.YELLOW) : Component.literal(TABS[i]);
            addRenderableWidget(Button.builder(lab, x -> {
                tab = t;
                rebuildWidgets();
            }).bounds(tx, cy, w, 16).build());
            tx += w + gap;
        }
        cy += 20;
        switch (tab) {
            case 0 -> buildGame(st, phase);
            case 1 -> buildFx();
            case 2 -> buildCams(st);
            case 3 -> buildPoints(st);
            default -> buildMore(st);
        }
    }

    private String signature() {
        CompoundTag st = ShowClient.state;
        StringBuilder s = new StringBuilder();
        s.append(st.getBoolean("active")).append(st.getString("phase")).append(st.getInt("format")).append(st.getBoolean("hostPlays")).append(tab);
        s.append(st.getBoolean("autoWrong")).append(st.getBoolean("voice")).append(st.getBoolean("hideSecrets")).append(st.getBoolean("manual"));
        if (st.contains("r", 10)) {
            CompoundTag r = st.getCompound("r");
            s.append("r").append(r.getBoolean("qs")).append(r.getInt("fw")).append(r.getInt("stage")).append(r.getString("pending")).append(r.getString("ans")).append(r.getInt("strikes"));
            ListTag a = r.getList("a", 10);
            for (int i = 0; i < a.size(); i++) {
                s.append(a.getCompound(i).getBoolean("rev") ? '1' : '0');
            }
        }
        if (st.contains("t", 10)) {
            s.append("t").append(st.getCompound("t").getBoolean("rev"));
        }
        if (st.contains("imp", 10)) {
            s.append("i").append(st.getCompound("imp").getString("stage"));
        }
        if (st.contains("tb", 10)) {
            s.append("b").append(st.getCompound("tb").getBoolean("over"));
        }
        if (st.contains("dr", 10)) {
            s.append("d").append(st.getCompound("dr").getBoolean("over")).append(st.getCompound("dr").getInt("round"));
        }
        s.append(st.getList("sides", 10).size());
        ListTag pl = st.getList("players", 10);
        for (int i = 0; i < pl.size(); i++) {
            s.append(pl.getCompound(i).getString("n")).append(pl.getCompound(i).getInt("side"));
        }
        if (st.getString("phase").equals("LOBBY") && Minecraft.getInstance().getConnection() != null) {
            s.append(Minecraft.getInstance().getConnection().getOnlinePlayers().size());
        }
        return s.toString();
    }

    @Override
    public void tick() {
        super.tick();
        if (!signature().equals(sig)) {
            rebuildWidgets();
        }
    }

    // ------------------------------------------------------------ ayudas de diseno

    private void nextRow() {
        if (col != 0) {
            col = 0;
            cy += 22;
        }
    }

    private void note(String s) {
        nextRow();
        if (s.isEmpty()) {
            cy += 6;
            return;
        }
        for (net.minecraft.util.FormattedCharSequence l : font.split(Component.literal(s), PW)) {
            noteText.add(l);
            notes.add(new int[] {cx, cy});
            cy += 11;
        }
        cy += 1;
    }

    private void full(String label, Runnable r) {
        nextRow();
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run()).bounds(cx, cy, PW, 20).build());
        cy += 22;
    }

    private void half(String label, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run()).bounds(cx + col * (PW / 2 + 2), cy, PW / 2 - 2, 20).build());
        col++;
        if (col == 2) {
            col = 0;
            cy += 22;
        }
    }

    private static void act(String a) {
        ShowClient.send(a);
    }

    private static void act(String a, String s, int n) {
        ShowClient.send(a, s, n);
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n - 1) + "." : s;
    }

    // ------------------------------------------------------------ pantallas

    private void buildRole() {
        note("Que quieres hacer en el show?");
        nextRow();
        full("Solo presentar", () -> act("role", "presenter", 0));
        full("Presentar y tambien jugar", () -> act("role", "player", 0));
        note("Tendras el control de todo igual.");
        note("Hace falta ser operador.");
    }

    private void buildLobby(CompoundTag st) {
        Format fmt = Format.byOrdinal(st.getInt("format"));
        note("PREPARACION  (max 6 jugadores)");
        full("Formato: " + fmt.label, () -> act("format", "", (fmt.ordinal() + 1) % Format.values().length));
        full("Yo juego: " + (st.getBoolean("hostPlays") ? "SI" : "NO"), () -> act("hostplays"));
        full("Modo voz (Discord): " + (st.getBoolean("voice") ? "SI" : "NO"), () -> act("voice"));
        full("Presentador manual: " + (st.getBoolean("manual") ? "SI" : "NO"), () -> act("manual"));
        if (st.getBoolean("hostPlays")) {
            full("Ocultarme los secretos: " + (st.getBoolean("hideSecrets") ? "SI" : "NO"), () -> act("hidesecrets"));
        }
        full("Repartir al azar a los conectados", () -> act("auto"));
        half("+ Bot (prueba)", () -> act("bot", "", -1));
        half("Llenar con bots", () -> act("botsfill"));
        full("Quitar los bots", () -> act("botsclear"));
        ListTag sides = st.getList("sides", 10);
        ListTag pl = st.getList("players", 10);
        List<String> online = new ArrayList<>();
        if (Minecraft.getInstance().getConnection() != null) {
            for (PlayerInfo pi : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                online.add(pi.getProfile().getName());
            }
        }
        java.util.Collections.sort(online);
        // los que ya estan en el show y no estan conectados (los bots) tambien se listan, para poder cambiarlos de equipo
        for (int i = 0; i < pl.size(); i++) {
            String n = pl.getCompound(i).getString("n");
            if (!online.contains(n)) {
                online.add(n);
            }
        }
        for (String name : online) {
            int side = -2;
            for (int i = 0; i < pl.size(); i++) {
                if (pl.getCompound(i).getString("n").equals(name)) {
                    side = pl.getCompound(i).getInt("side");
                }
            }
            nextRow();
            noteText.add(Component.literal(cut(name, 14) + (side >= -1 && side != -2 ? "  (dentro)" : "")).getVisualOrderText());
            notes.add(new int[] {cx, cy + 5});
            int bx = cx + 100;
            int teams = Math.max(1, fmt.teams);
            if (fmt.teams == 0) {
                final boolean in = side != -2;
                addRenderableWidget(Button.builder(Component.literal(in ? "Sacar" : "Meter"), b -> act(in ? "unassign" : "assign", name, 0))
                        .bounds(bx, cy, 90, 20).build());
            } else {
                int bw = 90 / (teams + 1);
                for (int t = 0; t < teams; t++) {
                    final int tt = t;
                    String lab = sides.size() > t ? String.valueOf(t + 1) : "?";
                    addRenderableWidget(Button.builder(Component.literal(side == t ? "[" + lab + "]" : lab), b -> act("assign", name, tt))
                            .bounds(bx + t * bw, cy, bw - 1, 20).build());
                }
                addRenderableWidget(Button.builder(Component.literal("X"), b -> act("unassign", name, 0)).bounds(bx + teams * bw, cy, bw - 1, 20).build());
            }
            cy += 22;
        }
        note("");
        full("EMPEZAR EL SHOW", () -> act("start"));
        full("Cancelar", () -> act("reset"));
    }

    private void buildGame(CompoundTag st, String phase) {
        switch (phase) {
            case "IDLE" -> buildMenu(st);
            case "R100" -> build100(st);
            case "TRIVIA" -> buildTrivia(st);
            case "IMPOSTOR" -> buildImpostor(st);
            case "TABOO" -> buildTaboo(st);
            case "DRAW" -> buildDraw(st);
            default -> {
                note("El show termino.");
                full("Cerrar y empezar de nuevo", () -> act("reset"));
            }
        }
    }

    private void buildMenu(CompoundTag st) {
        note("Elige el siguiente segmento:");
        half("100 mexicanos", () -> act("r100", "", 1));
        half("100 mex. x2", () -> act("r100", "", 2));
        if (st.getInt("format") != Format.TWO_TEAMS.ordinal()) {
            note("(100 mexicanos solo va con 2 equipos)");
        }
        half("Trivia", () -> act("trivia", "Trivia", 20));
        half("True o Fake", () -> act("trivia", "True o Fake", 12));
        half("Algo no cuadra", () -> act("trivia", "Algo no cuadra", 20));
        half("Quien habla?", () -> act("trivia", "¿Quién habla?", 20));
        half("Mayor o menor", () -> act("trivia", "Mayor o menor", 12));
        half("El Impostor", () -> act("imp", "", 0));
        half("Adivina palabra", () -> act("taboo", "", 60));
        half("Adivina el dibujo", () -> act("draw", "", 90));
        half("Reto absurdo", () -> act("reto"));
        nextRow();
        full("Llevar jugadores a sus podios", () -> act("tp"));
        full("Terminar el show (final)", () -> act("end"));
    }

    /** Lo que el presentador esta escribiendo (se conserva aunque el panel se reconstruya). */
    private static String draft = "";
    private EditBox box;
    private Runnable sendAction;

    private static String sideLabel(ListTag sides, int i) {
        return cut(sides.getCompound(i).getString("n"), 10);
    }

    private static boolean live(ListTag sides, int i) {
        return !sides.getCompound(i).getList("m", 8).isEmpty();
    }

    /** Caja para escribir lo que dijo el equipo; {@code send} recibe el texto. */
    private void answerBox(Runnable defaultSend) {
        nextRow();
        box = new EditBox(font, cx, cy, PW, 18, Component.literal("Lo que dicen"));
        box.setMaxLength(60);
        box.setValue(draft);
        box.setResponder(t -> draft = t);
        box.setHint(Component.literal("Escribe lo que dijeron...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        addRenderableWidget(box);
        setFocused(box);
        box.setFocused(true);
        sendAction = defaultSend;
        cy += 22;
    }

    private static void sendText(int side) {
        String t = draft.trim();
        if (!t.isEmpty()) {
            act("hans", t, side);
            draft = "";
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && box != null && box.isFocused() && sendAction != null) {
            sendAction.run();
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void build100(CompoundTag st) {
        CompoundTag r = st.getCompound("r");
        ListTag as = r.getList("a", 10);
        ListTag sides = st.getList("sides", 10);
        int stage = r.getInt("stage");
        String pending = r.getString("pending");
        String ans = r.getString("ans");
        boolean manual = st.getBoolean("manual");
        box = null;
        sendAction = null;
        if (stage == 3) {
            note("Ronda terminada.");
            half("Otra pregunta", () -> act("r100", "", 1));
            half("Doble", () -> act("r100", "", 2));
            nextRow();
            full("Descubrir lo que falta", () -> act("revealrest"));
            full("Volver al menu", () -> act("finish"));
            return;
        }
        boolean blind = !st.getBoolean("sees");
        note("Pregunta: " + r.getString("q"));
        if (!pending.isEmpty() && !blind) {
            note("Dijeron: " + cut(pending, 24) + "  (" + cut(ans, 14) + ")");
            for (int i = 0; i < as.size(); i++) {
                CompoundTag a = as.getCompound(i);
                if (!a.getBoolean("rev")) {
                    final int idx = i;
                    half("Es #" + (i + 1) + " " + cut(a.getString("t"), 9), () -> act("accept", "", idx));
                }
            }
            nextRow();
            full("No esta en el tablero (X)", () -> act("reject"));
            return;
        }
        switch (stage) {
            case 4 -> {
                note("1) Llama a los representantes al frente.");
                full("¡Que pasen los representantes!", () -> act("callreps"));
                full("Anunciar la pregunta (abre la campana)", () -> act("announce"));
                nextRow();
                full("Terminar segmento", () -> act("finish"));
                return;
            }
            case 5 -> {
                int fw = r.getInt("fw");
                note("Gano el cara a cara: " + (fw >= 0 && fw < sides.size() ? sides.getCompound(fw).getString("n") : "?"));
                note("¿Juegan o pasan?");
                full("JUGAR", () -> act("choose", "", 1));
                full("PASAR el turno al otro equipo", () -> act("choose", "", 0));
                nextRow();
                full("Terminar segmento", () -> act("finish"));
                return;
            }
            default -> {
            }
        }
        boolean face = stage == 0;
        if (face && !r.getBoolean("qs")) {
            note("Representantes al frente. Lee la pregunta y abre la campana.");
            full("Anunciar la pregunta (abre la campana)", () -> act("announce"));
            full("Llamar de nuevo a los representantes", () -> act("callreps"));
        } else if (face) {
            note(ans.isEmpty() ? "Campana abierta: espera al primero (o dala tu)." : "Contesta: " + cut(ans, 16));
            if (ans.isEmpty()) {
                for (int i = 0; i < sides.size(); i++) {
                    if (live(sides, i)) {
                        final int idx = i;
                        half("Campana: " + sideLabel(sides, i), () -> act("bell", "", idx));
                    }
                }
                nextRow();
            }
        } else {
            note(stage == 1 ? "Turno de " + cut(ans, 16) + " (equipo en juego)" : "ROBO: contesta " + cut(ans, 16));
        }
        if (manual) {
            // el presentador escribe lo que dice el equipo
            if (face && r.getBoolean("qs")) {
                answerBox(() -> {
                    // Enter: responde por quien tiene la campana (o el primer equipo)
                    int target = 0;
                    for (int i = 0; i < sides.size(); i++) {
                        if (live(sides, i) && sides.getCompound(i).getString("rep").equals(ans)) {
                            target = i;
                        }
                    }
                    sendText(target);
                });
                for (int i = 0; i < sides.size(); i++) {
                    if (live(sides, i)) {
                        final int idx = i;
                        half("Dice " + sideLabel(sides, i), () -> {
                            sendText(idx);
                            rebuildWidgets();
                        });
                    }
                }
                nextRow();
            } else if (!face) {
                answerBox(() -> sendText(0));
                full("Enviar lo que dice " + cut(ans, 12), () -> {
                    sendText(0);
                    rebuildWidgets();
                });
            }
        } else if (st.getBoolean("voice") && !blind) {
            note("Modo voz: oye la respuesta y destapa el # que corresponde.");
        }
        if (!ans.isEmpty()) {
            full("Que conteste el bot (prueba)", () -> act("botsay"));
        }
        for (int i = 0; i < as.size() && !blind; i++) {
            CompoundTag a = as.getCompound(i);
            if (!a.getBoolean("rev")) {
                final int idx = i;
                half("#" + (i + 1) + " " + cut(a.getString("t"), 9) + " " + a.getInt("p"), () -> act("reveal", "", idx));
            }
        }
        nextRow();
        full("Poner X (respuesta mala)", () -> act("strike"));
        if (face) {
            for (int i = 0; i < sides.size(); i++) {
                if (live(sides, i)) {
                    final int idx = i;
                    half("Gana: " + sideLabel(sides, i), () -> act("control", "", idx));
                }
            }
            nextRow();
            full("Cerrar cara a cara", () -> act("closeface"));
        } else {
            for (int i = 0; i < sides.size(); i++) {
                if (live(sides, i)) {
                    final int idx = i;
                    half("Banco a " + cut(sides.getCompound(i).getString("n"), 8), () -> act("award", "", idx));
                }
            }
            nextRow();
        }
        full("Terminar segmento", () -> act("finish"));
    }

    private void buildTrivia(CompoundTag st) {
        CompoundTag t = st.getCompound("t");
        boolean rev = t.getBoolean("rev");
        if (t.contains("ok")) {
            note("Correcta: " + (char) ('A' + t.getInt("ok")));
        }
        if (!rev) {
            full("Revelar ya", () -> act("treveal"));
        } else {
            final String cat = t.getString("cat");
            full("Otra de " + cut(cat, 16), () -> act("trivia", cat, 20));
        }
        full("Volver al menu", () -> act("finish"));
    }

    private void buildImpostor(CompoundTag st) {
        CompoundTag im = st.getCompound("imp");
        String stage = im.getString("stage");
        if (im.contains("imp")) {
            note("Impostor: " + im.getString("imp"));
        }
        if (im.contains("word")) {
            note("Palabra: " + im.getString("word"));
        }
        switch (stage) {
            case "CLUES" -> {
                if (st.getBoolean("voice")) {
                    full("Siguiente turno (ya hablo " + cut(im.getString("turn"), 8) + ")", () -> act("impskip"));
                } else {
                    full("Saltar el turno de " + cut(im.getString("turn"), 10), () -> act("impskip"));
                }
                full("Ir directo a votar", () -> act("impvote"));
            }
            case "DISCUSS" -> full("Abrir la votacion", () -> act("impvote"));
            case "VOTE" -> full("Contar votos ya", () -> act("imptally"));
            case "GUESS" -> full("El impostor se rinde", () -> act("impgiveup"));
            default -> {
                full("Otra partida de Impostor", () -> act("finish"));
            }
        }
        full("Volver al menu", () -> act("finish"));
    }

    private void buildTaboo(CompoundTag st) {
        CompoundTag tb = st.getCompound("tb");
        if (tb.contains("word")) {
            note("Palabra: " + tb.getString("word"));
        }
        if (st.getBoolean("voice") && !tb.getBoolean("over")) {
            note("Modo voz: marca quien acierta o si dijo una prohibida.");
            ListTag sides = st.getList("sides", 10);
            int ds = tb.getInt("side");
            boolean teamGuess = ds >= 0 && ds < sides.size() && sides.getCompound(ds).getList("m", 8).size() > 1;
            ListTag pl = st.getList("players", 10);
            for (int i = 0; i < pl.size(); i++) {
                String n = pl.getCompound(i).getString("n");
                if (n.equals(tb.getString("desc"))) {
                    continue;
                }
                if (teamGuess && pl.getCompound(i).getInt("side") != ds) {
                    continue;
                }
                half("Acerto " + cut(n, 9), () -> act("tsolve", n, 0));
            }
            nextRow();
            full("Dijo una prohibida (X)", () -> act("tstrike"));
        }
        if (tb.getBoolean("over")) {
            full("Otra palabra", () -> act("taboo", "", 60));
        }
        full("Volver al menu", () -> act("finish"));
    }

    private void buildDraw(CompoundTag st) {
        CompoundTag d = st.getCompound("dr");
        if (d.contains("word")) {
            note("Dibuja " + cut(d.getString("drawer"), 12) + ": " + cut(d.getString("word"), 20));
        } else {
            note("Dibuja " + cut(d.getString("drawer"), 12));
        }
        note("Acertaron: " + d.getList("solved", 8).size());
        if (st.getBoolean("voice") && !d.getBoolean("over")) {
            note("Modo voz: marca quien acierta.");
            ListTag pl = st.getList("players", 10);
            java.util.Set<String> done = new java.util.HashSet<>();
            for (int i = 0; i < d.getList("solved", 8).size(); i++) {
                done.add(d.getList("solved", 8).getString(i));
            }
            for (int i = 0; i < pl.size(); i++) {
                String n = pl.getCompound(i).getString("n");
                if (!n.equals(d.getString("drawer")) && !done.contains(n)) {
                    half("Acerto " + cut(n, 9), () -> act("dsolve", n, 0));
                }
            }
            nextRow();
        }
        if (d.getBoolean("over")) {
            full("Otro dibujo", () -> act("draw", "", 90));
        }
        full("Volver al menu", () -> act("finish"));
    }

    private void buildFx() {
        note("Efectos de programa:");
        half("Aplausos", () -> act("cue", "applause", 0));
        half("Ovacion", () -> act("cue", "cheer", 0));
        half("Abucheo", () -> act("cue", "boo", 0));
        half("Risas", () -> act("cue", "laugh", 0));
        half("Confeti", () -> act("cue", "confetti", 0));
        half("Redoble", () -> act("cue", "drumroll", 0));
        half("Trombon triste", () -> act("cue", "trombone", 0));
        half("Fanfarria", () -> act("cue", "fanfare", 0));
        half("Grillos", () -> act("cue", "crickets", 0));
        half("Bocina", () -> act("cue", "airhorn", 0));
        half("Suspenso", () -> act("cue", "suspense", 0));
        half("Rimshot", () -> act("cue", "rimshot", 0));
        nextRow();
        full("Dificultades tecnicas", () -> act("cue", "techdiff", 0));
        full("Chiste del locutor", () -> act("cue", "joke", 0));
        full("Premio absurdo", () -> act("cue", "prize", 0));
    }

    private void buildCams(CompoundTag st) {
        note("Plano de camara (para quien la tenga activa):");
        half("General", () -> act("cam", "wide", 0));
        half("Presentador", () -> act("cam", "host", 0));
        half("Pantalla", () -> act("cam", "board", 0));
        half("Publico", () -> act("cam", "audience", 0));
        ListTag sides = st.getList("sides", 10);
        for (int i = 0; i < sides.size(); i++) {
            if (!sides.getCompound(i).getList("m", 8).isEmpty()) {
                final int idx = i;
                half("Podio: " + cut(sides.getCompound(i).getString("n"), 9), () -> act("cam", "pod" + idx, 0));
            }
        }
        nextRow();
        full("Espectadores: ver por camara TV", () -> act("tv", "", 1));
        full("Espectadores: vista normal", () -> act("tv", "", 0));
        note("H: tu camara de TV (cada quien)");
    }

    private void buildPoints(CompoundTag st) {
        ListTag sides = st.getList("sides", 10);
        for (int i = 0; i < sides.size(); i++) {
            CompoundTag s = sides.getCompound(i);
            if (s.getList("m", 8).isEmpty()) {
                continue;
            }
            final int idx = i;
            note(cut(s.getString("n"), 20) + ": " + s.getInt("s"));
            nextRow();
            int w = PW / 4 - 1;
            int[] d = {-10, -1, 1, 10};
            for (int k = 0; k < 4; k++) {
                final int dd = d[k];
                addRenderableWidget(Button.builder(Component.literal(dd > 0 ? "+" + dd : String.valueOf(dd)), b -> act("score", String.valueOf(dd), idx))
                        .bounds(cx + k * (w + 1), cy, w, 18).build());
            }
            cy += 20;
        }
    }

    private void buildMore(CompoundTag st) {
        full("Locutor: " + "alternar", () -> act("announcer"));
        full("Modo voz (Discord): " + (st.getBoolean("voice") ? "SI" : "NO"), () -> act("voice"));
        full("Presentador manual: " + (st.getBoolean("manual") ? "SI" : "NO"), () -> act("manual"));
        if (st.getBoolean("hostPlays")) {
            full("Ocultarme los secretos: " + (st.getBoolean("hideSecrets") ? "SI" : "NO"), () -> act("hidesecrets"));
        }
        full("Respuesta mala sin preguntar: " + (st.getBoolean("autoWrong") ? "SI" : "NO"), () -> act("autowrong"));
        full("Recargar bancos de preguntas", () -> act("reload"));
        full("Llevar jugadores a sus podios", () -> act("tp"));
        full("Terminar el show (final)", () -> act("end"));
        full("Cerrar show y reiniciar", () -> act("reset"));
    }

    // ------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(cxLeft() - 4, 0, width, height, 0xC8101018);
        g.fill(cxLeft() - 4, 0, cxLeft() - 3, height, 0xFFFFC107);
        g.drawString(font, "PRESENTADOR - Show de Axelis", cxLeft(), 2, 0xFFFFC107, false);
        for (int i = 0; i < noteText.size(); i++) {
            g.drawString(font, noteText.get(i), notes.get(i)[0], notes.get(i)[1], 0xFFFFFFFF, true);
        }
        super.render(g, mx, my, pt);
    }

    private int cxLeft() {
        return width - PW - 6;
    }

    @Override
    public void removed() {
        if (current == this) {
            current = null;
        }
    }
}
