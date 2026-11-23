package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.coleccionista.evento.Despertar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * (Cliente) Las cinemáticas: una cámara aparte (sin mover al jugador) que recorre un camino mirando a un punto.
 * <ul>
 *     <li>Despertar: arriba en la esquina de la celda, viéndose acostado; se acerca despacio a la cama y pasa a sus ojos con un
 *     parpadeo.</li>
 *     <li>Acercarse a la pantalla (tecla R): la cámara va hasta quedar frente a la pantalla de la celda; moverse la regresa.</li>
 * </ul>
 * La cámara es una entidad invisible que no está en el mundo; Minecraft dibuja desde ella (y así se ve el propio cuerpo).
 */
public final class Cine {
    private static ArmorStand cam;
    private static boolean activo;
    private static String tipo = "";
    private static Vec3 a, b, mira;
    private static long inicio;
    private static float dur;
    /** Momento en que empezó el parpadeo (0 = no hay). */
    private static long parpadeo;
    private static long ultimoLatido;

    private Cine() {
    }

    public static void init() {
        ClientData.listen(Despertar.KEY_CINE, Cine::recibir);
        MinecraftForge.EVENT_BUS.addListener(Cine::cuadro);
        MinecraftForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            if (activo) {
                e.setCanceled(true);
            }
        });
        // la ventana de "Salir de la cama": no en la sala de espera ni durante la cinemática
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean pieza = mc.player != null && EventoCliente.activo() && EventoCliente.vivo(EventoCliente.yo());
            if (e.getNewScreen() instanceof InBedChatScreen && (activo || EventoCliente.sala() || pieza)) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre e) -> {
            if (activo && e.getOverlay().id().equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id())) {
                e.setCanceled(true);
            }
        });
        com.axeliscore.fx.client.Hud.add(Cine::dibujar);
    }

    public static boolean activo() {
        return activo;
    }

    /** ¿Estoy viendo la pantalla de cerca? */
    public static boolean enPantalla() {
        return activo && "pantalla".equals(tipo);
    }

    private static Vec3 v(CompoundTag t, String k) {
        ListTag l = t.getList(k, Tag.TAG_DOUBLE);
        return l.size() < 3 ? null : new Vec3(l.getDouble(0), l.getDouble(1), l.getDouble(2));
    }

    private static void recibir(CompoundTag t) {
        if ("despertar".equals(t.getString("tipo"))) {
            Vec3 pa = v(t, "a"), pb = v(t, "b"), pm = v(t, "mira");
            if (pa != null && pb != null && pm != null) {
                empezar("despertar", pa, pb, pm, 4.6F);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.8F, 1F));
            }
        }
    }

    private static void empezar(String t, Vec3 desde, Vec3 hasta, Vec3 punto, float segundos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (cam == null || cam.level() != mc.level) {
            cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            cam.setInvisible(true);
            cam.setNoGravity(true);
        }
        tipo = t;
        a = desde;
        b = hasta;
        mira = punto;
        dur = segundos;
        inicio = System.currentTimeMillis();
        activo = true;
        poner(a, mira);
        mc.setCameraEntity(cam);
    }

    /** Tecla R: la cámara va hasta quedar frente a la pantalla (o regresa si ya estaba). */
    public static void acercarse(Vec3 frente, Vec3 centro) {
        Minecraft mc = Minecraft.getInstance();
        if (activo && "pantalla".equals(tipo)) {
            terminar(false);
            return;
        }
        if (activo || mc.player == null) {
            return;
        }
        empezar("pantalla", mc.player.getEyePosition(), frente, centro, 1.8F);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.25F));
    }

    private static void terminar(boolean conParpadeo) {
        Minecraft mc = Minecraft.getInstance();
        activo = false;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        if (conParpadeo) {
            parpadeo = System.currentTimeMillis();
        }
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static void poner(Vec3 p, Vec3 m) {
        Vec3 d = m.subtract(p);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z)));
        double y = p.y - cam.getEyeHeight();
        cam.setPos(p.x, y, p.z);
        cam.xo = p.x;
        cam.yo = y;
        cam.zo = p.z;
        cam.xOld = p.x;
        cam.yOld = y;
        cam.zOld = p.z;
        cam.setYRot(yaw);
        cam.yRotO = yaw;
        cam.setXRot(pitch);
        cam.xRotO = pitch;
        cam.yHeadRot = yaw;
        cam.yHeadRotO = yaw;
    }

    private static void cuadro(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !activo) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || cam.level() != mc.level) {
            activo = false;
            return;
        }
        float t = (System.currentTimeMillis() - inicio) / 1000F;
        if ("pantalla".equals(tipo)) {
            var o = mc.options;
            if (mc.screen == null && (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown())) {
                terminar(false);
                return;
            }
            // acercamiento de cine: arranca y frena suave, con un vaivén mínimo al final
            float k = suave(t / dur);
            double vaiven = t > dur ? 0.006 * Math.sin(t * 1.3) : 0;
            poner(a.lerp(b, k).add(0, vaiven, 0), mira);
        } else {
            if (t >= dur) {
                terminar(true);
                return;
            }
            float k = suave(t / dur);
            // como cámara en mano: un vaivén chiquito
            double s = 0.03 * Math.sin(t * 1.7) + 0.015 * Math.sin(t * 3.1);
            Vec3 p = a.lerp(b, k).add(s, 0.02 * Math.sin(t * 2.3), -s);
            poner(p, mira.add(0, -0.2 * k, 0));
            if (t > 2.4F && ultimoLatido < inicio) {
                ultimoLatido = System.currentTimeMillis();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 1F));
            }
        }
        if (mc.getCameraEntity() != cam) {
            mc.setCameraEntity(cam);
        }
    }

    /** El negro del principio del despertar, las franjas de cine, los subtítulos de cerca y el parpadeo. */
    private static void dibujar(G g, int w, int h) {
        long now = System.currentTimeMillis();
        if (enPantalla()) {
            float t = (now - inicio) / 1000F;
            int barra = (int) (h * 0.12F * suave(t / 0.7F));
            g.fill(0, 0, w, barra, 0xFF000000);
            g.fill(0, h - barra, w, h, 0xFF000000);
            Espera.subtituloCine(g, w, h, barra);
            if (t > 2.5F && t < 6F) {
                float a = Math.min(1, (t - 2.5F) / 0.5F) * Math.min(1, (6F - t) / 0.6F);
                String s = net.minecraft.network.chat.Component.translatable(com.coleccionista.Coleccionista.MODID + ".cine.salir").getString();
                g.drawString(com.axeliscore.ui.Ui.font(), s, w - 10 - com.axeliscore.ui.Ui.font().width(s), barra / 2 - 4,
                        com.axeliscore.ui.Ui.alpha(0xFF8A8078, a), false);
            }
        }
        if (activo && "despertar".equals(tipo)) {
            float t = (now - inicio) / 1000F;
            if (t < 1.4F) {
                g.fill(0, 0, w, h, com.axeliscore.ui.Ui.alpha(0xFF000000, 1 - suave(t / 1.4F)));
            }
            // franjas de cine
            int barra = (int) (h * 0.1F);
            g.fill(0, 0, w, barra, 0xFF000000);
            g.fill(0, h - barra, w, h, 0xFF000000);
        }
        if (parpadeo > 0) {
            float t = (now - parpadeo) / 1000F;
            if (t > 2.6F) {
                parpadeo = 0;
                return;
            }
            // qué tan abiertos están los ojos: se abren un poco, se cierran, se abren más, del todo
            float abierto;
            if (t < 0.5F) {
                abierto = 0.35F * suave(t / 0.5F);
            } else if (t < 0.8F) {
                abierto = 0.35F * (1 - suave((t - 0.5F) / 0.3F));
            } else if (t < 1.5F) {
                abierto = 0.7F * suave((t - 0.8F) / 0.7F);
            } else if (t < 1.75F) {
                abierto = 0.7F - 0.25F * suave((t - 1.5F) / 0.25F);
            } else {
                abierto = 0.45F + 0.55F * suave((t - 1.75F) / 0.85F);
            }
            int cierre = (int) ((1 - abierto) * h / 2F);
            int borde = Math.max(2, h / 14);
            g.fill(0, 0, w, cierre, 0xFF000000);
            g.fillGradient(0, cierre, w, cierre + borde, 0xFF000000, 0x00000000);
            g.fill(0, h - cierre, w, h, 0xFF000000);
            g.fillGradient(0, h - cierre - borde, w, h - cierre, 0x00000000, 0xFF000000);
        }
    }
}
