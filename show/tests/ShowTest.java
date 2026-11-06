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

        // ---- 100 mexicanos (solo 2 equipos): llamar representantes, campana, cara a cara, jugar o pasar
        Show three = new Show(u(99), "Host", false);
        three.setFormat(Format.THREE_TEAMS);
        for (int i = 0; i < 6; i++) {
            three.assign(u(i), "T" + i, i % 3);
        }
        three.start();
        ok(!three.start100(q(), 1), "100 mexicanos no va con 3 equipos");
        s = twoTeams();
        ok(s.canStart() && s.start(), "empezar");
        ok(s.start100(q(), 1), "empezar ronda");
        ok(s.r.stage == Round100.CALL && !s.r.qShown, "primero se llama a los representantes");
        ok(!s.buzz(u(0)), "sin pregunta anunciada no suena la campana");
        ok(s.callReps() && s.r.stage == Round100.FACE, "pasan los representantes");
        ok(!s.buzz(u(0)), "la campana sigue cerrada");
        ok(s.announceQuestion() && s.r.qShown && s.r.open, "se anuncia la pregunta");
        ok(!s.buzz(u(2)), "J2 no es el representante (es J0 del equipo 0)");
        ok(s.buzz(u(0)), "J0 toca la campana");
        ok(!s.buzz(u(1)), "ya hay quien contesta");
        s.manual = true;
        ok(s.hostAnswer(0, "pizza"), "el presentador escribe lo que dijo el equipo 0");
        ok(s.r.stage == Round100.FACE, "sigue el cara a cara");
        ok(s.r.answerer != null && s.sideOf(s.byId(s.r.answerer)) == 1, "le toca al otro representante");
        ok(s.hostAnswer(1, "tacos"), "el equipo 1 dice la mejor");
        ok(s.r.stage == Round100.CHOOSE && s.r.faceWinner == 1, "gano el equipo 1, decide jugar o pasar");
        ok(s.r.bank == 65, "banco 65 = " + s.r.bank);
        ok(s.choosePlay(false) && s.r.control == 0 && s.r.stage == Round100.PLAY, "el equipo 1 pasa: juega el 0");
        // 3 errores y robo
        UUID a = s.r.answerer;
        ok(a != null && s.sideOf(s.byId(a)) == 0, "contesta alguien del equipo 0");
        ok(s.hostAnswer(0, "paella"), "respuesta mala (pendiente)");
        ok(s.r.pending != null, "queda pendiente para el presentador");
        ok(s.hostRejectPending(), "el presentador la rechaza");
        ok(s.r.strikes == 1, "primer strike");
        s.autoWrong = true;
        s.hostAnswer(0, "lasana");
        s.hostAnswer(0, "ensalada");
        ok(s.r.stage == Round100.STEAL, "3 strikes -> robo, etapa " + s.r.stage);
        ok(s.r.answerer != null && s.sideOf(s.byId(s.r.answerer)) == 1, "roba el equipo 1");
        ok(s.hostAnswer(1, "burger"), "roba bien");
        ok(s.r.stage == Round100.END && s.r.winner == 1, "gana el que roba");
        ok(s.sides.get(1).score == 65 + 15, "puntos al ganador: " + s.sides.get(1).score);

        // cara a cara sin aciertos se repite; con jugar
        ok(s.start100(q(), 2), "segunda ronda doble");
        ok(s.r.stage == Round100.CALL, "otra vez se llama a los representantes");
        s.autoWrong = true;
        s.announceQuestion();
        ok(s.hostGiveBell(1), "el presentador da la campana al equipo 1");
        s.hostAnswer(1, "nada");
        s.hostAnswer(0, "nada2");
        ok(s.r.stage == Round100.FACE && s.r.faceTry == 1 && s.r.open, "nadie acerto: se repite");
        s.hostAnswer(0, "sushi");
        s.hostAnswer(1, "pozole");
        ok(s.r.stage == Round100.CHOOSE && s.r.faceWinner == 0, "gana el que dijo la respuesta mas alta (sushi 10 > pozole 6)");
        ok(s.choosePlay(true) && s.r.control == 0, "juegan los ganadores");
        // el chat no contesta si es el presentador quien escribe (manual): answer() de jugador sigue disponible para el chat clasico
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
        s.manual = true;
        s.tick(100 + 15 * 20);
        ok(!s.t.revealed, "en modo presentador no se revela solo");
        s.manual = false;
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

        // ---- modo voz: el presentador marca aciertos con botones
        s = twoTeams();
        s.start();
        s.voice = true;
        ok(s.startDraw("Perro", 60, 0, null), "dibujo en modo voz");
        UUID vd = s.draw.drawer;
        UUID vg = u(0).equals(vd) ? u(1) : u(0);
        ok(!s.hostDrawSolve(vd), "el dibujante no puede ser marcado");
        ok(s.hostDrawSolve(vg) && s.draw.solved.contains(vg) && s.byId(vg).points == 10, "el presentador marca acierto");
        ok(!s.hostDrawSolve(vg), "no se marca dos veces");
        s.finishDraw();
        ok(s.startTaboo("Luna", List.of("noche"), 30, 100), "palabra en modo voz");
        UUID td = s.taboo.describer;
        ok(s.hostTabooStrike() && s.taboo.strikes == 1, "presentador marca prohibida");
        UUID tg = null;
        for (Participant pp : s.players) {
            if (s.isGuesser(pp.id)) {
                tg = pp.id;
            }
        }
        ok(tg != null && s.hostTabooSolve(tg, 200) && s.taboo.solved && s.taboo.over, "presentador marca que adivino");
        s.finishTaboo();
        // impostor: saltar turno en voz deja constancia
        List<UUID> ids = new java.util.ArrayList<>();
        for (Participant pp : s.players) {
            ids.add(pp.id);
        }
        ok(s.startImpostor("Animales", "Perro", ids, 0), "impostor en modo voz");
        ok(s.impostorSkip() && s.imp.clues.get(0)[1].contains("voz"), "turno de pista hablada");
        s.finishImpostor();

        // ---- el presentador que juega no responde por su cuenta si no ve las respuestas
        Show hs = new Show(u(0), "Host", true);
        ok(hs.hideSecrets && hs.hostPlays, "oculta secretos por defecto");

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
