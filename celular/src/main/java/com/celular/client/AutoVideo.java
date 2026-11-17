package com.celular.client;

import com.celular.CelularReg;
import com.celular.PhoneUse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * Video de las animaciones (solo en el CI: {@code -Dcelular.autotest=true -Dcelular.autotest.video=true}; el flujo graba la pantalla
 * con ffmpeg y corta los clips). Escenas: la cámara da la vuelta alrededor de otro jugador que cambia de pose (escribir, foto,
 * llamada moviendo la cabeza, linterna, audífonos); el celular en primera persona (sacarlo, desbloquear, abrir apps, pestañas,
 * interruptores, linterna); la linterna de noche (la de otro y la mía moviéndose) y las bocinas. Cada escena escribe en el registro
 * "AUTOTEST CLIP nombre inicio|fin t=milisegundos" para saber dónde cortar.
 */
final class AutoVideo {
    static final boolean ON = Boolean.getBoolean("celular.autotest.video");

    private record Step(long at, String name, Runnable r) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static long t0;
    private static int idx;
    /** Lo que se mueve en cada vuelta del reloj (la cámara, la cabeza del otro...), con los milisegundos desde que empezó. */
    private static LongConsumer motion;
    private static long motionAt;

    private AutoVideo() {
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (t0 == 0) {
            t0 = now + 10000;               // que carguen los chunks
            build();
            AutoTest.log("video pasos=" + STEPS.size());
            return;
        }
        while (idx < STEPS.size() && now - t0 >= STEPS.get(idx).at) {
            Step s = STEPS.get(idx++);
            try {
                s.r.run();
            } catch (Throwable e) {
                AutoTest.fails++;
                AutoTest.log("FALLO en " + s.name + ": " + e);
                com.celular.Celular.LOGGER.error("AUTOTEST", e);
            }
        }
        if (motion != null) {
            try {
                motion.accept(now - motionAt);
            } catch (Throwable e) {
                motion = null;
                AutoTest.log("FALLO en movimiento: " + e);
            }
        }
    }

    private static void at(double sec, String name, Runnable r) {
        STEPS.add(new Step((long) (sec * 1000), name, r));
    }

    private static void clip(String name, boolean start) {
        AutoTest.log("CLIP " + name + (start ? " inicio" : " fin") + " t=" + System.currentTimeMillis());
    }

    private static void move(LongConsumer m) {
        motion = m;
        motionAt = System.currentTimeMillis();
    }

    /** Pone la cámara (yo) en (x, z) mirando al punto, desde el cliente (más suave que /tp). */
    private static void cam(double x, double z, double tx, double ty, double tz) {
        LocalPlayer p = Minecraft.getInstance().player;
        double ey = AutoTest.GROUND + 1.62;
        double dx = tx - x, dy = ty - ey, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        p.setPos(x, AutoTest.GROUND, z);
        p.setDeltaMovement(Vec3.ZERO);
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.setYHeadRot(yaw);
    }

    /** Vuelta alrededor del otro jugador: {@code turns} vueltas en {@code ms} milisegundos, empezando de frente a la derecha. */
    private static void orbit(long t, long ms, double turns, double r) {
        double a = Math.toRadians(-45) + 2 * Math.PI * turns * Math.min(1, t / (double) ms);
        // ángulo 0 = delante del otro (al norte), sentido de las agujas del reloj visto desde arriba
        double x = AutoTest.FX + Math.sin(a) * r, z = AutoTest.FZ - Math.cos(a) * r;
        cam(x, z, AutoTest.FX, AutoTest.GROUND + 1.1, AutoTest.FZ);
    }

    private static PhoneScreen phone() {
        return ClientHooks.screen() instanceof PhoneScreen ps ? ps : null;
    }

    private static void build() {
        Minecraft mc = Minecraft.getInstance();
        at(0, "reglas", () -> {
            AutoTest.cmd("gamerule sendCommandFeedback false");
            AutoTest.cmd("gamerule logAdminCommands false");
        });
        at(0.6, "mundo", () -> {
            AutoTest.cmd("gamemode creative @a");
            AutoTest.cmd("time set 6000");
            AutoTest.cmd("gamerule doDaylightCycle false");
            AutoTest.cmd("weather clear 999999");
            AutoTest.cmd("gamerule doWeatherCycle false");
            AutoTest.cmd("gamerule randomTickSpeed 0");
            AutoTest.cmd("gamerule doMobSpawning false");
            AutoTest.cmd("kill @e[type=!player]");
            AutoTest.cmd("clear @a");
            AutoTest.cmd("item replace entity @a hotbar.0 with celular:phone");
            AutoTest.cmd("item replace entity @a hotbar.1 with celular:speaker");
            AutoTest.cmd("item replace entity @a hotbar.2 with celular:headphones");
            AutoTest.cmd("setblock 6 -60 -6 celular:speaker[facing=south,on=true]");
            AutoTest.cmd("setblock 8 -60 -6 celular:speaker[facing=south,on=false]");
            AutoTest.cmd("setblock 7 -60 -7 minecraft:oak_planks");
            AutoTest.cmd("tp @a 0.5 -60 6.5 180 0");
            ClientHooks.select(mc.player.getInventory(), 0);
            Prefs.load();
        });
        at(3, "otro jugador", () -> {
            AutoTest.ui(true);
            AutoTest.makeFake();
            AutoTest.fakeUses(PhoneUse.HOME, 15F);
        });

        // ---- 1) poses que ven los demás, dando la vuelta
        at(5, "poses", () -> {
            clip("1_poses", true);
            move(t -> {
                orbit(t, 30000, 1.0, 2.5);
                if (t < 6000) {
                    AutoTest.fakeUses(PhoneUse.HOME, 15F);
                } else if (t < 11000) {
                    AutoTest.fakeUses(PhoneUse.CAMERA, 0F);
                } else if (t < 19000) {
                    // en llamada mueve la cabeza: la mano y el celular la siguen
                    double k = (t - 11000) / 1000.0;
                    AutoTest.fakeUses(PhoneUse.EAR, (float) (18 * Math.sin(k * 1.3)));
                    AutoTest.fake.yHeadRot = AutoTest.fake.yHeadRotO = 180F + (float) (35 * Math.sin(k * 0.9));
                } else if (t < 24000) {
                    AutoTest.fake.yHeadRot = AutoTest.fake.yHeadRotO = 180F;
                    AutoTest.fakeUses(PhoneUse.FLASH, 10F);
                } else {
                    if (AutoTest.fake.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                        AutoTest.fake.setItemSlot(EquipmentSlot.HEAD, new ItemStack(CelularReg.HEADPHONES.get()));
                    }
                    AutoTest.fakeUses(PhoneUse.MUSIC, 15F);
                }
            });
        });
        at(35.5, "fin poses", () -> {
            move(null);
            clip("1_poses", false);
            AutoTest.fake.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            AutoTest.fakeUses(PhoneUse.OFF, 0F);
        });

        // ---- 2) el celular en primera persona: sacarlo, desbloquearlo, apps
        at(36.5, "primera persona", () -> {
            AutoTest.ui(false);
            cam(0.5, 7.5, 0.5, AutoTest.GROUND + 1.4, 0.5);
            Prefs.lockScreen = true;
            MusicApp.tab = 0;
        });
        at(38, "sacar", () -> {
            clip("2_celular", true);
            ClientHooks.openPhone(0);
        });
        at(40, "desbloquear", () -> {
            PhoneScreen ps = phone();
            if (ps != null) {
                ps.unlock();
            }
        });
        at(42, "abrir música", () -> phone().openApp("music"));
        at(44, "pestaña mundo", () -> MusicApp.tab = 1);
        at(45.5, "pestaña jam", () -> MusicApp.tab = 2);
        at(47, "pestaña mías", () -> MusicApp.tab = 0);
        at(48.5, "inicio", () -> phone().goHome());
        at(50, "abrir ajustes", () -> phone().openApp("settings"));
        at(51.5, "modo claro", () -> Prefs.dark = false);
        at(53, "modo oscuro", () -> Prefs.dark = true);
        at(54.5, "inicio", () -> phone().goHome());
        at(56, "abrir mensajes", () -> phone().openApp("messages"));
        at(58, "inicio", () -> phone().goHome());
        at(59.5, "abrir linterna", () -> phone().openApp("flash"));
        at(61, "prender", Flash::toggle);
        at(63, "apagar", Flash::toggle);
        at(64.5, "inicio", () -> phone().goHome());
        at(66, "guardar", () -> {
            AutoTest.close();
            Prefs.lockScreen = false;
        });
        at(67.5, "fin celular", () -> clip("2_celular", false));

        // ---- 3) de noche: la linterna del otro (dando media vuelta) y la mía moviéndose
        at(68, "noche", () -> {
            AutoTest.cmd("time set 18000");
            AutoTest.ui(true);
            AutoTest.fakeUses(PhoneUse.FLASH, 20F);
        });
        at(70, "linterna del otro", () -> {
            clip("3_noche_otro", true);
            move(t -> orbit(t, 9000, 0.5, 3.6));
        });
        at(79.5, "fin linterna del otro", () -> {
            move(null);
            clip("3_noche_otro", false);
            AutoTest.fakeUses(PhoneUse.OFF, 0F);
            AutoTest.ui(false);
            cam(0.5, 8.5, 0.5, AutoTest.GROUND, 4.5);
            AutoTest.myScreen(PhoneUse.FLASH);
        });
        at(81.5, "mi linterna", () -> {
            clip("3_noche_mia", true);
            move(t -> {
                double k = t / 1000.0;
                double tx = 0.5 + 5 * Math.sin(k * 0.8), tz = 3.5 - 2 * Math.cos(k * 0.55);
                cam(0.5, 8.5, tx, AutoTest.GROUND, tz);
            });
        });
        at(90.5, "fin mi linterna", () -> {
            move(null);
            clip("3_noche_mia", false);
            AutoTest.myScreen(PhoneUse.OFF);
            AutoTest.cmd("time set 6000");
        });

        // ---- 4) las bocinas (el bajo late y salen notas de la encendida)
        at(92, "bocinas", () -> {
            AutoTest.ui(true);
            clip("4_bocinas", true);
            move(t -> {
                double k = Math.min(1, t / 7000.0);
                cam(7.0 + 0.8 * Math.sin(k * 2), -2.2 - 2.2 * k, 7.0, AutoTest.GROUND + 0.5, -6.5);
            });
        });
        at(100, "fin bocinas", () -> {
            move(null);
            clip("4_bocinas", false);
        });

        at(101, "fin", () -> AutoTest.log("AUTOTEST_FIN video fallos=" + AutoTest.fails));
        at(102, "salir", () -> Minecraft.getInstance().stop());
    }
}
