package com.showdeaxelis.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Dice en palabras simples "que esta pasando" y "que haces tu ahora", segun el momento del show y tu papel (presentador, jugador
 * o publico). Es lo que se muestra en el aviso de arriba del HUD.
 */
public final class Guide {
    /** {@code what}: que pasa. {@code todo}: que haces tu. {@code mine}: si te toca actuar a ti (se resalta). */
    public record Tip(String what, String todo, boolean mine) {
    }

    private Guide() {
    }

    private static String me() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? "" : mc.player.getName().getString();
    }

    private static String sideName(ListTag sides, int i) {
        return i >= 0 && i < sides.size() ? sides.getCompound(i).getString("n") : "";
    }

    private static boolean hasMembers(CompoundTag side) {
        return !side.getList("m", 8).isEmpty();
    }

    public static Tip tip(CompoundTag st) {
        CompoundTag you = st.getCompound("you");
        boolean host = you.getBoolean("host");
        boolean player = you.getBoolean("player");
        boolean voice = st.getBoolean("voice");
        int mySide = you.getInt("side");
        ListTag sides = st.getList("sides", 10);
        String phase = st.getString("phase");
        String talk = voice ? "Habla por el chat de voz" : "Escribelo en el chat";
        switch (phase) {
            case "LOBBY" -> {
                if (host) {
                    return new Tip("Armando equipos", "Reparte a los jugadores y pulsa EMPEZAR EL SHOW (Y)", true);
                }
                return new Tip("Se estan armando los equipos", player ? "Espera: el presentador va a empezar" : "Mira el show desde las gradas o con la camara (H)", false);
            }
            case "IDLE" -> {
                if (host) {
                    return new Tip("Entre juegos", "Elige el siguiente segmento en el panel (Y)", true);
                }
                return new Tip("Entre juegos", "Espera: el presentador elige el siguiente", false);
            }
            case "R100" -> {
                return r100(st, you, sides, host, player, mySide, talk, voice);
            }
            case "TRIVIA" -> {
                CompoundTag t = st.getCompound("t");
                boolean rev = t.getBoolean("rev");
                int total = st.getList("players", 10).size();
                int done = t.getList("done", 8).size();
                String what = "Trivia: " + t.getString("cat");
                if (rev) {
                    String good = "Correcta: " + (char) ('A' + t.getInt("ok"));
                    if (player && t.getInt("mine") >= 0) {
                        boolean ok = t.getInt("mine") == t.getInt("ok");
                        return new Tip(what, good + (ok ? "  -  ¡Acertaste!" : "  -  Fallaste"), false);
                    }
                    return new Tip(what, good, false);
                }
                if (player) {
                    if (t.getInt("mine") >= 0) {
                        return new Tip(what, "Elegiste " + (char) ('A' + t.getInt("mine")) + ". Esperando a los demas (" + done + "/" + total + ")", false);
                    }
                    return new Tip(what, "Elige con Z X C V (A B C D). Contestaron " + done + "/" + total, true);
                }
                return new Tip(what, host ? "Los jugadores eligen solos; puedes revelar antes en el panel (Y)" : "Contestaron " + done + " de " + total, false);
            }
            case "IMPOSTOR" -> {
                return impostor(st, host, player, talk, voice);
            }
            case "TABOO" -> {
                CompoundTag tb = st.getCompound("tb");
                String desc = tb.getString("desc");
                boolean mine = desc.equals(me());
                if (tb.getBoolean("over")) {
                    return new Tip("Adivina la palabra", tb.getBoolean("solved") ? tb.getString("solver") + " acerto: " + tb.getString("word") : "Se acabo: era " + tb.getString("word"), false);
                }
                if (mine) {
                    return new Tip("Te toca describir", (voice ? "Descríbela hablando" : "Descríbela en el chat") + " SIN decir la palabra ni las prohibidas", true);
                }
                boolean guesser = tb.getBoolean("iGuess");
                if (guesser) {
                    return new Tip(desc + " te describe una palabra", (voice ? "Adivina hablando" : "Adivina escribiendo en el chat") + " lo que crees que es", true);
                }
                if (host && voice) {
                    return new Tip(desc + " describe la palabra", "Cuando alguien acierte pulsa su nombre; si dice una prohibida pulsa Prohibida (Y)", true);
                }
                return new Tip(desc + " describe la palabra", player ? "Esta adivinando otro equipo: no digas nada" : "Escucha y mira", false);
            }
            case "DRAW" -> {
                CompoundTag d = st.getCompound("dr");
                if (d.getBoolean("over")) {
                    return new Tip("Adivina el dibujo", "Era: " + d.getString("word"), false);
                }
                if (d.getBoolean("iDraw")) {
                    return new Tip("Te toca dibujar: " + d.getString("word").toUpperCase(Locale.ROOT), "Pulsa J para abrir el lienzo. No escribas ni digas la palabra", true);
                }
                if (d.getBoolean("iSolved")) {
                    return new Tip("¡Ya acertaste!", "Espera a que acierten los demas", false);
                }
                if (player) {
                    return new Tip(d.getString("drawer") + " esta dibujando", (voice ? "Adivina hablando" : "Escribe en el chat") + " lo que ves", true);
                }
                if (host && voice) {
                    return new Tip(d.getString("drawer") + " dibuja", "Cuando alguien acierte pulsa su nombre en el panel (Y)", true);
                }
                return new Tip(d.getString("drawer") + " esta dibujando", "Mira el dibujo", false);
            }
            case "FINAL" -> {
                return new Tip("¡Fin del show!", host ? "Cierra y empieza de nuevo en el panel (Y)" : "Gracias por jugar", false);
            }
            default -> {
                return new Tip("Show de Axelis", "", false);
            }
        }
    }

    private static Tip r100(CompoundTag st, CompoundTag you, ListTag sides, boolean host, boolean player, int mySide, String talk, boolean voice) {
        CompoundTag r = st.getCompound("r");
        int stage = r.getInt("stage");
        String ans = r.getString("ans");
        String pending = r.getString("pending");
        int control = r.getInt("control");
        boolean iAnswer = player && ans.equals(me());
        String what;
        String todo;
        boolean mine = false;
        switch (stage) {
            case 0 -> {
                StringBuilder sb = new StringBuilder("Cara a cara: ");
                int n = 0;
                for (int i = 0; i < sides.size(); i++) {
                    CompoundTag sd = sides.getCompound(i);
                    if (!hasMembers(sd)) {
                        continue;
                    }
                    if (n++ > 0) {
                        sb.append("  vs  ");
                    }
                    sb.append(sd.getString("rep")).append(" (").append(sd.getString("n")).append(")");
                }
                what = sb.toString();
                String myRep = mySide >= 0 && mySide < sides.size() ? sides.getCompound(mySide).getString("rep") : "";
                if (iAnswer) {
                    todo = "¡Te toca! " + talk + (voice ? " (el presentador la valida)" : "");
                    mine = true;
                } else if (player && myRep.equals(me())) {
                    todo = "Tu das la cara: pulsa G para tocar el timbre";
                    mine = true;
                } else if (player) {
                    todo = "Da la cara " + myRep + ": espera tu turno y animalo";
                } else if (host) {
                    todo = ans.isEmpty() ? "Esperando que toquen el timbre (G)" : "Contesta " + ans + ": " + (voice ? "valida con los botones (Y)" : "mira lo que escribe");
                    mine = !ans.isEmpty();
                } else {
                    todo = "Mira el cara a cara";
                }
            }
            case 1 -> {
                what = "Juega " + sideName(sides, control) + (ans.isEmpty() ? "" : "  -  contesta " + ans);
                if (iAnswer) {
                    todo = "¡Tu turno! " + talk;
                    mine = true;
                } else if (player && mySide == control) {
                    todo = "Tu equipo juega: ayuda a " + (ans.isEmpty() ? "tu companero" : ans);
                } else if (player) {
                    todo = "Espera: juega " + sideName(sides, control) + ". Si fallan 3 veces puedes robar";
                } else if (host) {
                    todo = voice ? "Valida lo que dice " + ans + " con los botones (Y)" : "Revisa lo que escribe " + ans;
                    mine = !ans.isEmpty();
                } else {
                    todo = "Mira la ronda";
                }
            }
            case 2 -> {
                what = "¡Robo! " + (ans.isEmpty() ? "El otro equipo" : ans) + " puede robar el banco";
                if (iAnswer) {
                    todo = "Una sola oportunidad: " + talk;
                    mine = true;
                } else if (player && ans.isEmpty() && mySide != control) {
                    todo = "Quien da la cara pulsa G para robar";
                    mine = true;
                } else {
                    todo = host ? "Valida el robo (Y)" : "Espera el resultado del robo";
                }
            }
            default -> {
                what = "Ronda terminada";
                todo = host ? "Elige otra pregunta o vuelve al menu (Y)" : "Espera el siguiente juego";
            }
        }
        if (host && !pending.isEmpty()) {
            what = what + "  |  Escribio: " + pending;
            todo = "Acepta con el # que corresponde o rechaza con la X (Y)";
            mine = true;
        }
        return new Tip(what, todo, mine);
    }

    private static Tip impostor(CompoundTag st, boolean host, boolean player, String talk, boolean voice) {
        CompoundTag im = st.getCompound("imp");
        String stage = im.getString("stage");
        String cat = im.getString("cat");
        boolean isImp = im.getBoolean("isImp");
        switch (stage) {
            case "CLUES" -> {
                String turn = im.getString("turn");
                if (turn.equals(me()) && player) {
                    return new Tip("Tu turno de dar pista", (voice ? "Di UNA palabra que se relacione" : "Escribe UNA palabra en el chat") + (isImp ? " (disimula: no sabes la palabra)" : " (sin decir la palabra)"), true);
                }
                if (host && voice) {
                    return new Tip("Pista de " + turn + " (" + im.getInt("step") + "/" + im.getInt("total") + ")", "Cuando termine de hablar pulsa Siguiente turno (Y)", true);
                }
                return new Tip("Pista de " + turn + " (" + im.getInt("step") + "/" + im.getInt("total") + ")", player ? "Escucha la pista y sospecha. Categoria: " + cat : "Categoria: " + cat, false);
            }
            case "DISCUSS" -> {
                return new Tip("A discutir", (voice ? "Hablen" : "Charlen en el chat") + ": ¿quien es el impostor?", false);
            }
            case "VOTE" -> {
                int v = im.getInt("votes");
                int n = im.getInt("n");
                if (player && !im.getBoolean("iVoted")) {
                    return new Tip("Votacion (" + v + "/" + n + ")", voice ? "Elige a quien acusas en la ventana de votos (N la abre de nuevo)" : "Escribe en el chat el numero o nombre de a quien acusas", true);
                }
                return new Tip("Votacion (" + v + "/" + n + ")", player ? "Ya votaste. Esperando a los demas" : host ? "Puedes contar los votos ya en el panel (Y)" : "Mira la votacion", false);
            }
            case "GUESS" -> {
                String acc = im.getString("acc");
                if (acc.equals(me())) {
                    return new Tip("¡Te descubrieron!", "Adivina la palabra secreta para salvarte: " + talk.toLowerCase(Locale.ROOT), true);
                }
                return new Tip("Impostor descubierto", acc + " intenta adivinar la palabra", false);
            }
            default -> {
                return new Tip(im.getBoolean("caught") ? "Descubrieron al impostor" : "El impostor se salvo", "Era " + im.getString("imp") + " y la palabra: " + im.getString("word"), false);
            }
        }
    }
}
