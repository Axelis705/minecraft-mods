package com.celular.client;

//#if MC < 1.20.2
import com.celular.PhoneUse;
import com.celular.net.AppNet;
import com.celular.net.CelularNet;
import com.celular.plat.Net;
import com.celular.radio.SongInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
//#endif

/**
 * Prueba de DOS jugadores de verdad en un servidor (solo para el CI, workflow "Prueba de dos jugadores"): cada cliente se abre con
 * {@code -Dcelular.autotest.rol=A} o {@code B} y los dos entran al mismo servidor. Cuando se ven, siguen el mismo guion por tiempo:
 * A usa el celular y B le toma fotos (la pose y la pantalla llegan por la red), A llama a B y B contesta, se mandan mensajes, A sube
 * una canción al mundo y la pone con altavoz (B la debe recibir y bajar), Jam entre los dos y el mapa con la cara del otro. Cada
 * comprobación escribe "AUTOTEST CHECK nombre OK" o "FALLO" en el registro.
 */
final class AutoTestMulti {
    private AutoTestMulti() {
    }

    static void tick() {
        //#if MC < 1.20.2
        run();
        //#endif
    }

    //#if MC < 1.20.2
    private record Step(long at, String name, Runnable r) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static long t0;
    private static int idx, checks, bad;
    private static UUID other;
    private static String otherName = "";
    private static boolean a;

    private static void log(String s) {
        AutoTest.log(AutoTest.ROLE + " " + s);
    }

    private static void check(String name, boolean ok, String info) {
        checks++;
        if (!ok) {
            bad++;
        }
        log("CHECK " + name + (ok ? " OK" : " FALLO") + (info.isEmpty() ? "" : " (" + info + ")"));
    }

    private static void cmd(String c) {
        Minecraft.getInstance().player.connection.sendCommand(c);
    }

    private static void at(long sec, String name, Runnable r) {
        STEPS.add(new Step(sec * 1000L, name, r));
    }

    /** Me pongo en (x, z) mirando al punto (tx, ty, tz) (los dos son operadores del servidor de prueba). */
    private static void lookFrom(double x, double z, double tx, double ty, double tz) {
        double ey = -60 + 1.62;
        double dx = tx - x, dy = ty - ey, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        cmd(String.format(Locale.ROOT, "tp @s %.3f -60 %.3f %.2f %.2f", x, z, yaw, pitch));
    }

    private static void run() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (t0 == 0) {
            for (Player p : mc.level.players()) {
                if (p != mc.player) {
                    other = p.getUUID();
                    otherName = p.getScoreboardName();
                }
            }
            if (other == null) {
                return;                 // todavía no llega el otro
            }
            a = "A".equals(AutoTest.ROLE);
            t0 = now + 4000;
            build();
            log("los dos están: yo=" + mc.player.getScoreboardName() + " otro=" + otherName + " pasos=" + STEPS.size());
            return;
        }
        while (idx < STEPS.size() && now - t0 >= STEPS.get(idx).at) {
            Step s = STEPS.get(idx++);
            try {
                s.r.run();
            } catch (Throwable e) {
                bad++;
                log("FALLO en " + s.name + ": " + e);
                com.celular.Celular.LOGGER.error("AUTOTEST", e);
            }
        }
    }

    private static SongInfo worldSong() {
        for (SongInfo i : Radio.songs) {
            if (i.title != null && i.title.toLowerCase(Locale.ROOT).contains("tono")) {
                return i;
            }
        }
        return Radio.songs.isEmpty() ? null : Radio.songs.get(0);
    }

    private static void build() {
        Minecraft mc = Minecraft.getInstance();
        // ---- preparar: A en el centro mirando al norte, B enfrente un poco a la derecha (para verle la mano del celular)
        at(0, "preparar", () -> {
            cmd("gamemode creative @s");
            cmd("item replace entity @s hotbar.0 with celular:phone" + (a ? "_pink" : ""));
            ClientHooks.select(mc.player.getInventory(), 0);
            Prefs.load();
            Prefs.lockScreen = false;
            mc.options.hideGui = !a;       // B solo mira y toma fotos: sin mano ni barra
            if (a) {
                cmd("time set 6000");
                cmd("weather clear");
                cmd("tp @s 0.5 -60 0.5 180 10");
            } else {
                lookFrom(2.06, -1.06, 0.5, -58.85, 0.5);
            }
        });
        // ---- 1) lo que ve B cuando A usa el celular (pose y pantalla, por la red de verdad)
        at(4, "A abre el celular", () -> {
            if (a) {
                ClientHooks.openPhone(0);
            }
        });
        at(8, "B ve a A con el celular", () -> {
            if (!a) {
                Player o = mc.level.getPlayerByUUID(other);
                int scr = o == null ? -1 : PhoneUse.get(o.getMainHandItem());
                check("ve_pantalla_del_otro", scr == PhoneUse.HOME, "pantalla=" + scr);
                AutoTest.shot("m_b_ve_a_inicio");
            }
        });
        at(10, "A abre Música", () -> {
            if (a) {
                ClientHooks.open(new PhoneScreen(0, "music"));
            }
        });
        at(14, "B ve la app", () -> {
            if (!a) {
                Player o = mc.level.getPlayerByUUID(other);
                int scr = o == null ? -1 : PhoneUse.get(o.getMainHandItem());
                check("ve_app_del_otro", scr == PhoneUse.MUSIC, "pantalla=" + scr);
                AutoTest.shot("m_b_ve_a_musica");
            }
        });
        // ---- 2) llamada: A llama, B contesta, A queda con el celular en la oreja
        at(16, "A llama", () -> {
            if (a) {
                ClientHooks.open(null);
                Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.DIAL, other));
            }
        });
        at(20, "B recibe la llamada", () -> {
            if (!a) {
                check("llamada_entrante", ClientHooks.call.state == CelularNet.CallState.INCOMING, "estado=" + ClientHooks.call.state);
                AutoTest.shot("m_b_llamada_entrante");
                Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.ACCEPT, null));
            }
        });
        at(24, "en llamada", () -> {
            check("en_llamada", ClientHooks.call.state == CelularNet.CallState.ACTIVE, "estado=" + ClientHooks.call.state);
            if (!a) {
                Player o = mc.level.getPlayerByUUID(other);
                int scr = o == null ? -1 : PhoneUse.get(o.getMainHandItem());
                check("ve_al_otro_en_la_oreja", scr == PhoneUse.EAR, "pantalla=" + scr);
                AutoTest.shot("m_b_ve_a_en_llamada");
            }
        });
        at(27, "A cuelga", () -> {
            if (a) {
                Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.HANGUP, null));
            }
        });
        at(30, "nadie en llamada", () -> check("colgaron", ClientHooks.call.state == CelularNet.CallState.IDLE, "estado=" + ClientHooks.call.state));
        // ---- 3) mensajes
        at(31, "mensaje", () -> Net.toServer(new AppNet.Req(AppNet.Req.MSG_SEND, other, 0, a ? "hola desde A" : "hola desde B")));
        at(35, "abrir mensajes", () -> {
            Net.toServer(new AppNet.Req(AppNet.Req.MSG_LIST));
            ClientHooks.open(new PhoneScreen(0, "messages"));
        });
        at(38, "mensaje recibido", () -> {
            boolean got = false;
            if (Chats.list != null) {
                for (AppNet.Conv c : Chats.list.convs) {
                    if (other.equals(c.id) && c.last != null && c.last.contains("hola desde")) {
                        got = true;
                    }
                }
            }
            check("mensaje_recibido", got, Chats.list == null ? "sin lista" : Chats.list.convs.size() + " chats");
            AutoTest.shot("m_" + (a ? "a" : "b") + "_mensajes");
            ClientHooks.open(null);
        });
        // ---- 4) música del mundo: A sube la canción y la pone con altavoz; B la recibe y la baja
        at(40, "A sube la canción", () -> {
            if (a) {
                com.celular.music.MusicPlayer.volume = 0F;
                com.celular.music.MusicPlayer.scan();
                log("canciones en la carpeta: " + com.celular.music.MusicPlayer.library.size());
                if (!com.celular.music.MusicPlayer.library.isEmpty()) {
                    Radio.upload(com.celular.music.MusicPlayer.library.get(0));
                }
            }
            Radio.requestLibrary();
        });
        at(44, "subiendo", () -> log("subiendo=" + (Radio.uploading != null) + " progreso=" + Radio.upProgress + " canciones=" + Radio.songs.size()));
        at(48, "lista del mundo", () -> {
            log("subiendo=" + (Radio.uploading != null) + " progreso=" + Radio.upProgress + " canciones=" + Radio.songs.size());
            Radio.requestLibrary();
        });
        at(50, "A pone la canción con altavoz", () -> {
            SongInfo s = worldSong();
            check("cancion_en_el_mundo", s != null, "canciones=" + Radio.songs.size());
            if (a && s != null) {
                Prefs.speaker = true;
                Radio.play(s);
            }
        });
        at(60, "B oye el altavoz de A", () -> {
            if (!a) {
                String r = Radio.remotesDebug();
                check("oye_altavoz_del_otro", r.contains(otherName), r);
                SongInfo s = worldSong();
                check("bajo_la_cancion", s != null && Radio.ready(s), s == null ? "sin canción" : "progreso=" + Radio.progress(s.id));
                AutoTest.shot("m_b_altavoz");
            } else {
                check("mi_altavoz_suena", Radio.broadcasting(), "");
            }
        });
        // ---- 5) Jam: A crea, B se une, A pone la canción
        at(62, "A crea el Jam", () -> {
            if (a) {
                com.celular.music.MusicPlayer.stop();
                Radio.jamCreate("Fiesta CI");
            }
        });
        at(65, "B busca Jams", () -> {
            if (!a) {
                Radio.jamListRequest();
            }
        });
        at(68, "B se une", () -> {
            if (!a) {
                if (Radio.jams != null && !Radio.jams.ids.isEmpty()) {
                    Radio.jamJoin(Radio.jams.ids.get(0));
                } else {
                    log("no veo ningún Jam");
                }
            }
        });
        at(71, "A pone la canción en el Jam", () -> {
            if (a) {
                SongInfo s = worldSong();
                if (s != null) {
                    Radio.jamAdd(s);
                }
            }
        });
        at(78, "los dos en el Jam", () -> {
            check("en_el_jam", Radio.inJam() && Radio.jam.members.size() == 2, "miembros=" + Radio.jam.members);
            check("jam_con_cancion", !Radio.jam.queue.isEmpty(), "cola=" + Radio.jam.queue.size());
            ClientHooks.open(new PhoneScreen(0, "music"));
        });
        at(81, "foto del Jam", () -> AutoTest.shot("m_" + (a ? "a" : "b") + "_jam"));
        at(83, "salir del Jam", () -> {
            ClientHooks.open(null);
            Radio.jamLeave();
        });
        // ---- 6) mapa con amigos: cada uno ve la cara del otro
        at(85, "mapa", () -> {
            Prefs.shareLocation = true;
            Net.toServer(new AppNet.Req(AppNet.Req.MAP));
            ClientHooks.open(new PhoneScreen(0, "map"));
        });
        at(90, "foto del mapa", () -> {
            AutoTest.shot("m_" + (a ? "a" : "b") + "_mapa");
            ClientHooks.open(null);
        });
        at(93, "fin", () -> log("AUTOTEST_FIN comprobaciones=" + checks + " fallos=" + bad + " fotos=" + AutoTest.shots));
        at(96, "salir", () -> Minecraft.getInstance().stop());
    }
    //#endif
}
