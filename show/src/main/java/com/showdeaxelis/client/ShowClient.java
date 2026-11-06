package com.showdeaxelis.client;

import com.showdeaxelis.net.ShowNet;
import com.showdeaxelis.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;

/** Lo que el cliente sabe del show: el ultimo estado recibido y los efectos en marcha (texto del locutor, rotulos, confeti...). */
public final class ShowClient {
    public static CompoundTag state = new CompoundTag();
    private static final Random RND = new Random();

    // locutor
    public static String sayText = "";
    public static int sayTick;
    public static int sayLife;
    // rotulo grande
    public static String title = "";
    public static int titleTick;
    public static String titleSub = "";
    // X grande
    public static int strikeCount;
    public static int strikeTick;
    // cuenta de "+40"
    public record Popup(String text, int side, int born) {
    }
    public static final List<Popup> popups = new ArrayList<>();
    // confeti
    public static final class Confetti {
        public float x, y, vx, vy, rot, vr;
        public int color;
    }
    public static final List<Confetti> confetti = new ArrayList<>();
    public static int glitchTick;
    public static int clientTick;
    private static int lastTickBeep = -1;

    private ShowClient() {
    }

    public static boolean active() {
        return state.getBoolean("active");
    }

    public static void onState(CompoundTag t) {
        state = t == null ? new CompoundTag() : t;
        stateStamp = clientTick;
        if (!active()) {
            TvCamera.release();
        }
        HostScreen.stateChanged();
        // al empezar su ronda de dibujo, al dibujante se le abre solo el lienzo
        if (active() && state.getString("phase").equals("DRAW") && state.getCompound("dr").getBoolean("iDraw")
                && !state.getCompound("dr").getBoolean("over") && state.getCompound("dr").getInt("round") != drawOpenedRound) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (mc.screen == null || mc.screen instanceof HostScreen)) {
                drawOpenedRound = state.getCompound("dr").getInt("round");
                DrawScreen.open();
            }
        }
        // en modo voz, a quien le toca votar se le abre la ventana de votos
        CompoundTag im = state.getCompound("imp");
        boolean voting = active() && state.getString("phase").equals("IMPOSTOR") && im.getString("stage").equals("VOTE");
        if (!voting) {
            voteOpened = false;
        } else if (!voteOpened && state.getBoolean("voice") && state.getCompound("you").getBoolean("player") && !im.getBoolean("iVoted")) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (mc.screen == null || mc.screen instanceof HostScreen)) {
                voteOpened = true;
                VoteScreen.open();
            }
        }
    }

    private static int drawOpenedRound = -1;
    private static boolean voteOpened;
    /** Como se dibuja el HUD: 0 automatico (compacto si estas dentro del estudio), 1 compacto, 2 completo. */
    public static int hudMode;

    public static void play(String name, float pitch) {
        SoundEvent se = ModSounds.get(name);
        if (se != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(se, pitch, 1.0f));
        }
    }

    public static void onCue(ShowNet.Cue c) {
        switch (c.type()) {
            case "snd" -> play(c.text(), 1.0f);
            case "say" -> {
                sayText = c.text();
                sayTick = 0;
                sayLife = c.text().length() * 2 + 140;
            }
            case "title" -> {
                title = c.text();
                titleTick = 0;
                titleSub = c.a() > 1 ? "x" + c.a() + " PUNTOS" : "";
            }
            case "strike" -> {
                strikeCount = Math.max(1, Math.min(3, c.a()));
                strikeTick = 0;
            }
            case "popup" -> {
                popups.add(new Popup(c.text(), c.a(), clientTick));
            }
            case "fx" -> {
                if (c.text().equals("confetti")) {
                    spawnConfetti();
                } else if (c.text().equals("glitch")) {
                    glitchTick = 60;
                }
            }
            case "cam" -> TvCamera.shot(c.text(), c.b() == 1);
            case "tv" -> TvCamera.forced(c.a() == 1);
            default -> {
            }
        }
    }

    private static void spawnConfetti() {
        int[] cols = {0xFFE53935, 0xFF1E88E5, 0xFFFDD835, 0xFF43A047, 0xFF8E24AA, 0xFFFB8C00, 0xFFFFFFFF};
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        for (int i = 0; i < 220; i++) {
            Confetti f = new Confetti();
            f.x = RND.nextFloat() * w;
            f.y = -RND.nextFloat() * 120f;
            f.vx = (RND.nextFloat() - 0.5f) * 1.6f;
            f.vy = 1.2f + RND.nextFloat() * 2.2f;
            f.rot = RND.nextFloat() * 6.28f;
            f.vr = (RND.nextFloat() - 0.5f) * 0.4f;
            f.color = cols[RND.nextInt(cols.length)];
            confetti.add(f);
        }
    }

    /** Un paso del reloj del cliente (20 por segundo). */
    public static void tick() {
        clientTick++;
        Minecraft mc = Minecraft.getInstance();
        if (!sayText.isEmpty()) {
            sayTick++;
            if (sayTick <= sayText.length() * 2 && sayTick % 2 == 0) {
                char ch = sayText.charAt(Math.min(sayText.length() - 1, sayTick / 2));
                if (ch != ' ') {
                    play("blip", 0.8f + RND.nextFloat() * 0.6f);
                }
            }
            if (sayTick > sayLife) {
                sayText = "";
            }
        }
        if (!title.isEmpty() && ++titleTick > 90) {
            title = "";
        }
        if (strikeCount > 0 && ++strikeTick > 40) {
            strikeCount = 0;
        }
        if (glitchTick > 0) {
            glitchTick--;
        }
        popups.removeIf(p -> clientTick - p.born() > 50);
        int h = mc.getWindow().getGuiScaledHeight();
        confetti.removeIf(f -> {
            f.x += f.vx + (float) Math.sin(f.rot) * 0.5f;
            f.y += f.vy;
            f.rot += f.vr;
            return f.y > h + 10;
        });
        // pitidos en los ultimos segundos de una ronda con tiempo
        int left = timeLeftTicks();
        if (left > 0 && left <= 100 && left % 20 == 0 && left != lastTickBeep) {
            lastTickBeep = left;
            play("tick", 1.0f);
        }
        if (left == 0) {
            lastTickBeep = -1;
        }
    }

    /** Ticks que faltan de la ronda con tiempo en curso (trivia o adivina la palabra), 0 si no hay. */
    public static int timeLeftTicks() {
        long age = clientTick - stateStamp;
        if (state.contains("t", 10) && !state.getCompound("t").getBoolean("rev")) {
            return (int) Math.max(0, state.getCompound("t").getInt("left") - age);
        }
        if (state.contains("dr", 10) && state.getString("phase").equals("DRAW") && !state.getCompound("dr").getBoolean("over")) {
            return (int) Math.max(0, state.getCompound("dr").getInt("left") - age);
        }
        if (state.contains("tb", 10) && !state.getCompound("tb").getBoolean("over")) {
            return (int) Math.max(0, state.getCompound("tb").getInt("left") - age);
        }
        return 0;
    }

    public static int stateStamp;

    public static void send(String act, String s, int n) {
        ShowNet.CHANNEL.sendToServer(new ShowNet.Action(act, s, n));
    }

    public static void send(String act) {
        send(act, "", 0);
    }
}
