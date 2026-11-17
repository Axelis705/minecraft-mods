package com.celular.client;

import com.celular.CelularReg;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Notas y alarmas de este celular (se guardan en .minecraft/celular/notas.json): una lista de pendientes, alarmas con la hora del
 * juego (suenan cuando el reloj de Minecraft llega a esa hora) y un temporizador de minutos reales.
 */
public final class Notes {
    public static final class Note {
        public String text;
        public boolean done;
    }

    public static final class Alarm {
        public int hour, minute;
        public boolean on = true;
    }

    public static final List<Note> notes = new ArrayList<>();
    public static final List<Alarm> alarms = new ArrayList<>();
    /** Cuándo termina el temporizador (0 = parado) y cuánto duraba. */
    public static long timerEnd;
    public static int timerLen;
    private static boolean loaded;
    private static int lastMinute = -1;
    private static long ringUntil, nextRing;

    private Notes() {
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve("celular").resolve("notas.json");
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("notas")) {
                JsonObject n = e.getAsJsonObject();
                Note x = new Note();
                x.text = n.get("texto").getAsString();
                x.done = n.get("hecha").getAsBoolean();
                notes.add(x);
            }
            for (JsonElement e : o.getAsJsonArray("alarmas")) {
                JsonObject n = e.getAsJsonObject();
                Alarm a = new Alarm();
                a.hour = n.get("hora").getAsInt();
                a.minute = n.get("minuto").getAsInt();
                a.on = n.get("activa").getAsBoolean();
                alarms.add(a);
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        JsonObject o = new JsonObject();
        JsonArray ns = new JsonArray();
        for (Note n : notes) {
            JsonObject x = new JsonObject();
            x.addProperty("texto", n.text);
            x.addProperty("hecha", n.done);
            ns.add(x);
        }
        o.add("notas", ns);
        JsonArray as = new JsonArray();
        for (Alarm a : alarms) {
            JsonObject x = new JsonObject();
            x.addProperty("hora", a.hour);
            x.addProperty("minuto", a.minute);
            x.addProperty("activa", a.on);
            as.add(x);
        }
        o.add("alarmas", as);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), o.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    /** Minuto del día en el reloj de Minecraft (0 = 0:00, el amanecer es 6:00). -1 si no hay mundo. */
    public static int gameMinute() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return -1;
        }
        long t = Math.floorMod(com.celular.plat.Plat.dayTime(mc.level) + 6000L, 24000L);
        return (int) (t * 1440L / 24000L);
    }

    public static String clock(int minuteOfDay) {
        return String.format(java.util.Locale.ROOT, "%d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }

    private static void ring(Component what) {
        PhoneHud.toast(D.APP_NOTES, "celular.app.notes", what);
        ringUntil = System.currentTimeMillis() + 5000;
        nextRing = 0;
    }

    /** Cada cuadro: revisa alarmas y temporizador, y hace sonar el timbre unos segundos. */
    static void frame(Minecraft mc) {
        load();
        long now = System.currentTimeMillis();
        int m = gameMinute();
        if (m >= 0 && lastMinute >= 0 && m != lastMinute) {
            // pasó del minuto anterior al actual (con salto de medianoche o si el reloj saltó un poco)
            int span = Math.floorMod(m - lastMinute, 1440);
            if (span <= 60) {
                for (Alarm a : alarms) {
                    int am = a.hour * 60 + a.minute;
                    if (a.on && Math.floorMod(am - lastMinute, 1440) > 0 && Math.floorMod(am - lastMinute, 1440) <= span) {
                        ring(Component.translatable("celular.notes.alarm_ring", clock(am)));
                    }
                }
            }
        }
        lastMinute = m;
        if (timerEnd > 0 && now >= timerEnd) {
            timerEnd = 0;
            ring(Component.translatable("celular.notes.timer_done"));
        }
        if (now < ringUntil && now >= nextRing && mc.player != null) {
            nextRing = now + 2500;
            ClientHooks.play(CelularReg.RINGTONE.get(), 1.15F, 0.6F);
        }
    }

    static void reset() {
        lastMinute = -1;
        ringUntil = 0;
    }
}
