import com.showdeaxelis.game.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pruebas de las reglas del show (sin Minecraft). */
public class ShowTest {
    static int fails = 0;

    static void ok(boolean c, String msg) {
        if (!c) {
            fails++;
            System.out.println("FALLO: " + msg);
        }
    }

    static UUID u(int i) {
        return new UUID(0, i);
    }

    static Question100 q() {
        List<Answer100> a = new ArrayList<>();
        a.add(new Answer100("Tacos", 40, List.of("taco")));
        a.add(new Answer100("Pizza", 25, List.of()));
        a.add(new Answer100("Hamburguesa", 15, List.of("hamburguesas", "burger")));
        a.add(new Answer100("Sushi", 10, List.of()));
        a.add(new Answer100("Pozole", 6, List.of()));
        return new Question100("q1", "Comida favorita", a);
    }

    static Show twoTeams() {
        Show s = new Show(u(99), "Host", false);
        s.setFormat(Format.TWO_TEAMS);
        for (int i = 0; i < 4; i++) {
            ok(s.assign(u(i), "J" + i, i % 2), "asignar " + i);
        }
        return s;
    }

    public static void main(String[] args) {
        // ---- capacidad por formato
        Show s = new Show(u(99), "Host", true);
        s.setFormat(Format.TWO_TEAMS);
        for (int i = 0; i < 3; i++) {
            ok(s.assign(u(i), "A" + i, 0), "equipo 0 cabe " + i);
        }
        ok(!s.assign(u(3), "A3", 0), "equipo de 3 lleno");
        ok(s.assign(u(3), "A3", 1), "otro equipo");
        s.setFormat(Format.THREE_TEAMS);
        int in0 = 0;
        for (Participant p : s.players) {
            if (p.team == 0) {
                in0++;
            }
        }
        ok(in0 <= 2, "al pasar a 3 equipos de 2 nadie sobra en el 0");
        s.setFormat(Format.FFA);
        for (int i = 10; i < 20; i++) {
            s.assign(u(i), "F" + i, 0);
        }
        ok(s.players.size() == 6, "maximo 6 en todos contra todos, hay " + s.players.size());
        ok(s.sides.size() == 6, "cada quien es su lado");

        // ---- Matcher
        Question100 q = q();
        boolean[] rev = new boolean[5];
        ok(Matcher.find("  TACOS!! ", q, rev) == 0, "mayusculas y signos");
        ok(Matcher.find("los tacos", q, rev) == 0, "articulo");
        ok(Matcher.find("taco", q, rev) == 0, "alias");
        ok(Matcher.find("hamburgesa", q, rev) == 2, "error de ortografia");
        ok(Matcher.find("sushi", q, rev) == 3, "sushi");
        ok(Matcher.find("pastel", q, rev) == -1, "no existe");
        rev[0] = true;
        ok(Matcher.find("tacos", q, rev) == -2, "repetida");
        ok(Matcher.same("Canción", "cancion"), "same acentos");
        ok(Matcher.containsWord("creo que es pizza jaja", "Pizza"), "contiene palabra");
        ok(!Matcher.containsWord("pizzas", "pizza"), "palabra completa");

        // ---- 100 mexicanos con 2 equipos: cara a cara gana el control quien pone la mas alta
        s = twoTeams();
        ok(s.canStart() && s.start(), "empezar");
        ok(s.start100(q(), 1), "empezar ronda");
        ok(!s.buzz(u(2)), "J2 no es el representante (es J0 del equipo 0)");
        ok(s.buzz(u(0)), "J0 toca el timbre");
        ok(s.answer(u(0), "pizza"), "J0 contesta");
        ok(s.r.stage == Round100.FACE, "sigue el cara a cara");
        ok(s.buzz(u(1)), "J1 (equipo 1) toca");
        ok(s.answer(u(1), "tacos"), "J1 contesta la mejor");
        ok(s.r.stage == Round100.PLAY && s.r.control == 1, "equipo 1 tiene el control");
        ok(s.r.bank == 65, "banco 65 = " + s.r.bank);
        // 3 errores y robo
        UUID a = s.r.answerer;
        ok(a != null, "alguien contesta");
        ok(s.answer(a, "paella"), "respuesta mala (pendiente)");
        ok(s.r.pending != null, "queda pendiente para el presentador");
        ok(s.hostRejectPending(), "el presentador la rechaza");
        ok(s.r.strikes == 1, "primer strike");
        s.autoWrong = true;
        s.answer(s.r.answerer, "lasana");
        s.answer(s.r.answerer, "ensalada");
        ok(s.r.stage == Round100.STEAL, "3 strikes -> robo, etapa " + s.r.stage);
        ok(s.r.answerer != null && s.sideOf(s.byId(s.r.answerer)) == 0, "roba el equipo 0");
        ok(s.answer(s.r.answerer, "burger"), "roba bien");
        ok(s.r.stage == Round100.END && s.r.winner == 0, "gana el que roba");
        ok(s.sides.get(0).score == 65 + 15, "puntos al ganador: " + s.sides.get(0).score);

        // multiplicador y robo fallido
        ok(s.start100(q(), 2), "segunda ronda doble");
        ok(s.r.stage == Round100.FACE, "otra vez cara a cara");
        ok(s.buzz(u(2)) || s.buzz(u(3)), "el siguiente representante toca"); // rotan los representantes
        s.finish100();
        ok(s.phase == Phase.IDLE, "vuelve al menu");

        // ---- trivia con puntos por rapidez
        s = twoTeams();
        s.start();
        TriviaQ tq = new TriviaQ("t1", "Mexico", "Capital?", List.of("CDMX", "Lima", "Quito", "Cali"), 0, "Obvio");
        ok(s.startTrivia(tq, 15, 100), "empezar trivia");
        ok(s.triviaAnswer(u(0), 0, 110), "J0 acierta primero");
        ok(s.triviaAnswer(u(1), 2, 112), "J1 falla");
        ok(!s.triviaAnswer(u(0), 1, 113), "no se puede cambiar");
        ok(s.triviaAnswer(u(2), 0, 120), "J2 acierta segundo");
        ok(s.t.revealed == false, "falta J3");
        s.tick(100 + 15 * 20);
        ok(s.t.revealed, "se acabo el tiempo");
        ok(s.byId(u(0)).points == 10 && s.byId(u(2)).points == 8, "10 y 8 puntos");
        ok(s.sides.get(0).score == 18 && s.sides.get(1).score == 0, "equipo 0 suma 18");
        ok(!s.triviaAnswer(u(3), 0, 400), "tarde");
        ok(s.segmentFree(), "se puede seguir");

        // ---- El Impostor
        s = twoTeams();
        s.start();
        List<UUID> order = List.of(u(0), u(1), u(2), u(3));
        ok(s.startImpostor("Comida", "Pizza", order, 2), "empezar impostor");
        ok(s.secretFor(u(0)).equals("Pizza") && s.secretFor(u(2)) == null, "el impostor no ve la palabra");
        ok(!s.impostorClue(u(1), "queso"), "no es su turno");
        for (int i = 0; i < 8; i++) {
            ok(s.impostorClue(s.imp.current(), "pista" + i), "pista " + i);
        }
        ok(s.imp.stage == ImpostorGame.Stage.DISCUSS, "a charlar");
        ok(s.impostorStartVote(), "votar");
        ok(s.impostorVote(u(0), u(2)) && s.impostorVote(u(1), u(2)) && s.impostorVote(u(3), u(2)), "3 votos");
        ok(!s.impostorVote(u(2), u(2)), "no se vota a si mismo");
        ok(s.impostorVote(u(2), u(0)), "el impostor vota");
        ok(s.imp.impostorCaught && s.imp.stage == ImpostorGame.Stage.GUESS, "lo descubrieron");
        ok(s.impostorGuess(u(2), "pizza"), "adivina");
        ok(s.imp.stage == ImpostorGame.Stage.DONE && s.byId(u(2)).points == 3, "impostor +3");
        // impostor se escapa por empate
        s.finishImpostor();
        ok(s.startImpostor("Animales", "Perro", order, 0), "otra");
        for (int i = 0; i < 8; i++) {
            s.impostorClue(s.imp.current(), "x");
        }
        s.impostorStartVote();
        s.impostorVote(u(0), u(1));
        s.impostorVote(u(1), u(2));
        s.impostorVote(u(2), u(1));
        s.impostorVote(u(3), u(2));
        ok(!s.imp.impostorCaught && s.imp.stage == ImpostorGame.Stage.DONE, "empate: se salva");
        ok(s.byId(u(0)).points == 5, "impostor +5");
        // impostor descubierto que falla: la tripulacion suma
        s.finishImpostor();
        s.startImpostor("Animales", "Perro", order, 1);
        s.impostorStartVote();
        s.impostorVote(u(0), u(1));
        s.impostorVote(u(2), u(1));
        s.impostorVote(u(3), u(1));
        s.impostorVote(u(1), u(0));
        ok(s.imp.impostorCaught, "descubierto");
        s.impostorGuess(u(1), "gato");
        ok(s.byId(u(0)).points == 5 + 3 && s.byId(u(3)).points == 3, "la tripulacion suma 3");

        // ---- Adivina la palabra
        s = twoTeams();
        s.start();
        ok(s.startTaboo("Playa", List.of("mar", "arena"), 60, 0), "empezar taboo");
        UUID d = s.taboo.describer;
        ok(s.tabooSay(d, "es donde hay ARENA", 5) == 2, "palabra prohibida");
        ok(s.tabooSay(d, "vas en verano a mojarte", 10) == 1, "pista buena");
        UUID mate = s.sides.get(s.taboo.side).members.stream().filter(m -> !m.id.equals(d)).findFirst().get().id;
        ok(s.tabooSay(mate, "alberca", 20) == 1, "intento fallido");
        ok(s.tabooSay(mate, "la playa", 30) == 3, "acierta");
        ok(s.taboo.over && s.taboo.gained >= 1, "ronda cerrada con puntos " + s.taboo.gained);
        ok(s.sides.get(s.taboo.side).score >= 1, "suma al lado");
        ok(s.startTaboo("Luna", List.of("noche"), 30, 40), "siguiente ronda");
        ok(!s.taboo.describer.equals(d) || s.sides.size() == 1, "cambia quien describe");
        s.tick(40 + 30 * 20);
        ok(s.taboo.over, "se acabo el tiempo");


        // ---- Adivina el dibujo
        s = twoTeams();
        s.start();
        ok(s.startDraw("Casa grande", 60, 0, null), "empezar dibujo");
        UUID dr = s.draw.drawer;
        UUID g1 = u(0).equals(dr) ? u(1) : u(0);
        UUID g2 = u(2).equals(dr) ? u(3) : u(2);
        ok(s.draw.mask(0).equals("_ _ _ _   _ _ _ _ _ _"), "mascara " + s.draw.mask(0));
        ok(s.draw.mask(60 * 20 * 3 / 4).chars().filter(Character::isLetter).count() == 2, "pista de letras");
        ok(s.addStroke(dr, 3, 2, new int[] {0, 0, 300, 300}), "trazo del dibujante");
        ok(s.draw.strokes.get(0).pts()[2] == DrawGame.W - 1, "recorta al lienzo");
        ok(!s.addStroke(g1, 3, 2, new int[] {1, 1, 2, 2}), "otro no dibuja");
        ok(!s.clearCanvas(g1) && s.clearCanvas(dr) && s.draw.strokes.isEmpty() && s.draw.clearCount == 1, "borrar lienzo");
        ok(s.drawSay(dr, "es una CASA", 5) == 2, "el dibujante no puede decir la palabra");
        ok(s.drawSay(g1, "perro", 10) == 1, "intento fallido");
        ok(s.drawSay(g1, "casa grande", 20) == 3, "acierta");
        ok(s.drawSay(g1, "casa grande", 21) == 0, "no acierta dos veces");
        ok(s.byId(g1).points == 10 && s.byId(dr).points == 3, "puntos del primero y del dibujante");
        ok(!s.draw.over, "faltan por adivinar");
        ok(s.drawSay(g2, "Casa Grande", 30) == 3 && s.byId(g2).points == 8, "segundo puntaje");
        s.tick(40);
        ok(!s.draw.over, "aun faltan");
        UUID g3 = null;
        for (Participant pp : s.players) {
            if (!pp.id.equals(dr) && !pp.id.equals(g1) && !pp.id.equals(g2)) {
                g3 = pp.id;
            }
        }
        ok(s.drawSay(g3, "casa grande", 50) == 3 && s.draw.over, "todos acertaron: termina");
        ok(s.startDraw("Luna", 30, 60, null), "siguiente ronda");
        ok(!s.draw.drawer.equals(dr), "cambia quien dibuja");
        s.tick(60 + 30 * 20);
        ok(s.draw.over, "tiempo agotado");
        ok(s.draw.mask(0).equals("L U N A"), "al terminar se ve la palabra");
        s.finishDraw();
        ok(s.phase == Phase.IDLE, "vuelve a esperar");

        // ---- final
        s.finishShow();
        ok(s.phase == Phase.FINAL, "final");
        ok(!s.drain().isEmpty(), "hay eventos");

        System.out.println(fails == 0 ? "SHOW_OK" : "FALLAS: " + fails);
        if (fails > 0) {
            System.exit(1);
        }
    }
}
