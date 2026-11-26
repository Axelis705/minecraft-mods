package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Desenlace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

/**
 * (Cliente) El desenlace ({@link Desenlace}): la cámara de cine que sigue al ganador por el pasillo (para los fantasmas), la pantalla que se
 * va poniendo blanca mientras se acerca a la luz, los créditos ({@link Creditos}) con la música de la caja, la cámara frente a la vitrina
 * después de los créditos y el FIN.
 */
public final class DesenlaceCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag();
    private static String fase = "NADA";
    /** Cuándo empezó la fase actual (ms del reloj de aquí). */
    private static long desde;
    private static ArmorStand cam;
    /** ¿La cámara de cine está puesta? (seguir al ganador o la vitrina) */
    private static boolean camara;
    /** El que se movió suelta la cámara (hasta la siguiente parte). */
    private static boolean soltado;
    private static Vec3 pos;
    private static Entity objetivo;
    private static long buscado, ultimoCuadro;
    /** Lo blanco de la luz (0 a 1) y lo negro (para entrar y salir de las escenas). */
    private static float blanco, negro;
    private static SimpleSoundInstance musica;
    /** ¿Se cerraron los créditos con Esc? (no se vuelven a abrir) */
    private static boolean sinCreditos;

    private DesenlaceCliente() {
    }

    public static void init() {
        ClientData.listen(Desenlace.KEY, DesenlaceCliente::recibir);
        MinecraftForge.EVENT_BUS.addListener(DesenlaceCliente::cuadro);
        MinecraftForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            if (camara) {
                e.setCanceled(true);
            }
        });
        // en las escenas (la cámara de cine, la luz, los créditos, la vitrina y el FIN) no se ve nada del juego encima: ni corazones, ni
        // barra, ni mira (el chat sí)
        MinecraftForge.EVENT_BUS.addListener((RenderGuiOverlayEvent.Pre e) -> {
            var id = e.getOverlay().id();
            if (escena() && "minecraft".equals(id.getNamespace()) && !id.equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
                e.setCanceled(true);
            }
        });
        com.axeliscore.fx.client.Hud.add(DesenlaceCliente::dibujar);
    }

    public static String fase() {
        return fase;
    }

    public static CompoundTag estado() {
        return estado;
    }

    /** ¿Está la escena de la vitrina (después de los créditos)? Ahí los subtítulos van grandes en la franja de abajo. */
    public static boolean cine() {
        return camara && ("POST".equals(fase) || "FIN".equals(fase));
    }

    /** ¿Hay una escena del desenlace en pantalla? (la cámara de cine, la luz, los créditos, la vitrina o el FIN): se esconde el HUD. */
    public static boolean escena() {
        return camara || "LUZ".equals(fase) || "CREDITOS".equals(fase) || "POST".equals(fase) || "FIN".equals(fase);
    }

    /** ¿La cámara de cine del desenlace está puesta? */
    public static boolean camara() {
        return camara;
    }

    private static Vec3 v(String k) {
        ListTag l = estado.getList(k, Tag.TAG_DOUBLE);
        return l.size() < 3 ? null : new Vec3(l.getDouble(0), l.getDouble(1), l.getDouble(2));
    }

    private static UUID ganador() {
        return estado.hasUUID("ganador") ? estado.getUUID("ganador") : null;
    }

    private static boolean soyGanador() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getUUID().equals(ganador());
    }

    private static void recibir(CompoundTag t) {
        String antes = fase;
        estado = t;
        fase = t.getString("fase").isEmpty() ? "NADA" : t.getString("fase");
        if (fase.equals(antes)) {
            return;
        }
        desde = System.currentTimeMillis();
        soltado = false;
        Minecraft mc = Minecraft.getInstance();
        switch (fase) {
            case "CAMINA" -> {
                blanco = 0;
                negro = 0;
                sinCreditos = false;
                objetivo = null;
                pos = null;
            }
            case "LUZ" -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.0F));
            case "CREDITOS" -> {
                soltar();
                mc.getMusicManager().stopPlaying();
                musica = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, "menu.musica")), 0.94F, 0.8F);
                mc.getSoundManager().play(musica);
                if (!sinCreditos) {
                    mc.setScreen(new Creditos(t.getList("piezas", Tag.TAG_COMPOUND), t.getInt("dura"), ganador()));
                }
            }
            case "POST" -> {
                if (mc.screen instanceof Creditos) {
                    mc.setScreen(null);
                }
                pararMusica();
                blanco = 0;
                negro = 1;
                Vec3 a = v("cam_a");
                if (a != null) {
                    pos = a;
                    ponerCamara();
                }
            }
            case "FIN" -> {
            }
            default -> {
                if (mc.screen instanceof Creditos) {
                    mc.setScreen(null);
                }
                pararMusica();
                soltar();
                blanco = 0;
                negro = 0;
            }
        }
    }

    private static void pararMusica() {
        if (musica != null) {
            Minecraft.getInstance().getSoundManager().stop(musica);
            musica = null;
        }
    }

    /** Los créditos se cerraron con Esc: queda negro hasta lo de después. */
    static void creditosCerrados() {
        sinCreditos = true;
    }

    private static void ponerCamara() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (cam == null || cam.level() != mc.level) {
            cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            cam.setInvisible(true);
            cam.setNoGravity(true);
        }
        camara = true;
    }

    private static void soltar() {
        Minecraft mc = Minecraft.getInstance();
        if (camara && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        camara = false;
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static void mirar(Vec3 p, Vec3 m) {
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

    /** El ganador (jugador o bot), si este cliente lo tiene cargado. */
    private static Entity buscarGanador(Minecraft mc) {
        UUID g = ganador();
        if (g == null || mc.level == null) {
            return null;
        }
        if (objetivo != null && objetivo.isAlive() && objetivo.level() == mc.level && g.equals(objetivo.getUUID())) {
            return objetivo;
        }
        long now = System.currentTimeMillis();
        if (now - buscado < 500) {
            return null;
        }
        buscado = now;
        objetivo = mc.level.getPlayerByUUID(g);
        if (objetivo == null) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (g.equals(e.getUUID())) {
                    objetivo = e;
                    break;
                }
            }
        }
        return objetivo;
    }

    private static boolean moviendose(Minecraft mc) {
        var o = mc.options;
        return mc.screen == null && (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown());
    }

    private static void cuadro(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        float dt = ultimoCuadro == 0 ? 0.016F : Math.min(0.1F, (now - ultimoCuadro) / 1000F);
        ultimoCuadro = now;
        float t = (now - desde) / 1000F;
        if (mc.level == null || mc.player == null) {
            camara = false;
            return;
        }
        Vec3 luz = v("luz"), puerta = v("puerta");
        switch (fase) {
            case "CAMINA" -> {
                Entity g = buscarGanador(mc);
                // lo blanco: crece mientras él (o yo) se acerca a la luz
                Vec3 donde = g != null ? g.position() : mc.player.position();
                if (luz != null) {
                    float k = Mth.clamp((float) ((donde.x - (luz.x - 28)) / 28.0), 0, 1);
                    blanco = 0.55F * k * k;
                }
                // los fantasmas lo ven irse con la cámara de cine (moverse la suelta)
                if (!soyGanador() && mc.player.isSpectator() && g != null && puerta != null && !soltado) {
                    if (moviendose(mc)) {
                        soltado = true;
                        soltar();
                        return;
                    }
                    Vec3 p = g.getPosition(e.renderTickTime);
                    boolean adentro = p.x > puerta.x + 0.5;
                    Vec3 dir = adentro ? new Vec3(1, 0, 0) : new Vec3(puerta.x - p.x, 0, puerta.z - p.z);
                    dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
                    Vec3 lado = new Vec3(-dir.z, 0, dir.x);
                    Vec3 quiero = p.subtract(dir.scale(4.2)).add(lado.scale(0.55 * Math.sin(t * 0.35))).add(0, 2.2, 0);
                    if (adentro) {
                        // dentro del pasillo: sin salirse de las paredes ni del techo
                        quiero = new Vec3(quiero.x, Math.min(quiero.y, p.y + 3.6), Mth.clamp(quiero.z, puerta.z - 1.2, puerta.z + 1.2));
                    }
                    if (!camara) {
                        ponerCamara();
                        pos = quiero;
                    }
                    pos = pos.lerp(quiero, 1 - Math.exp(-dt * 2.4));
                    mirar(pos, p.add(0, 1.45, 0).add(dir.scale(1.8)));
                    if (mc.getCameraEntity() != cam) {
                        mc.setCameraEntity(cam);
                    }
                } else if (camara) {
                    soltar();
                }
            }
            case "LUZ" -> blanco = Math.max(blanco, 0.55F + 0.45F * suave(t / 2.2F));
            case "CREDITOS" -> blanco = 1;
            case "POST", "FIN" -> {
                Vec3 a = v("cam_a"), b = v("cam_b"), m = v("cam_mira");
                if (a == null || b == null || m == null) {
                    return;
                }
                if (!camara) {
                    ponerCamara();
                }
                if ("POST".equals(fase)) {
                    negro = 1 - suave((t - 0.4F) / 2.2F);
                    float k = suave(t / 17F);
                    pos = a.lerp(b, k).add(0.02 * Math.sin(t * 1.1), 0.015 * Math.sin(t * 1.7), 0);
                } else {
                    negro = suave(t / 1.6F);
                }
                if (pos == null) {
                    pos = a;
                }
                mirar(pos, m);
                if (mc.getCameraEntity() != cam) {
                    mc.setCameraEntity(cam);
                }
            }
            default -> {
                if (camara) {
                    soltar();
                }
                negro = Math.max(0, negro - dt);
                blanco = Math.max(0, blanco - dt);
            }
        }
    }

    // ------------------------------------------------------------------ lo que se dibuja encima

    private static void dibujar(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        var f = Ui.font();
        if (camara) {
            // franjas de cine
            int barra = (int) (h * 0.1F);
            g.fill(0, 0, w, barra, 0xFF000000);
            g.fill(0, h - barra, w, h, 0xFF000000);
            if ("CAMINA".equals(fase)) {
                String s = Component.translatable(MOD + ".des.soltar").getString();
                g.drawString(f, s, w - 10 - f.width(s), barra / 2 - 4, 0xFF9A928A, false);
            }
            if ("POST".equals(fase) || "FIN".equals(fase)) {
                Espera.subtituloCine(g, w, h, barra);
            }
        }
        if (blanco > 0.003F) {
            g.fill(0, 0, w, h, Ui.alpha(0xFFFFFDF6, Math.min(1, blanco)));
        }
        if ("CREDITOS".equals(fase) && !(mc.screen instanceof Creditos)) {
            g.fill(0, 0, w, h, 0xFF000000);
        }
        if (negro > 0.003F) {
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, Math.min(1, negro)));
        }
        if ("FIN".equals(fase)) {
            float t = (System.currentTimeMillis() - desde) / 1000F;
            float a = suave((t - 1.6F) / 1.0F) * (1 - suave((t - 4.6F) / 0.8F));
            if (a > 0.01F) {
                String s = Component.translatable(MOD + ".des.fin").getString();
                g.push();
                g.translate(w / 2.0, h / 2.0 - 12, 0);
                g.scale(4F, 4F, 1F);
                g.drawString(f, s, -f.width(s) / 2, -4, Ui.alpha(0xFFEFE3CC, a), false);
                g.pop();
                int lw = (int) (70 * a);
                g.fill(w / 2 - lw, h / 2 + 14, w / 2 + lw, h / 2 + 15, Ui.alpha(0xFF8E1726, a));
            }
        }
    }
}
