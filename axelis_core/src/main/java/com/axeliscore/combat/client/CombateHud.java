package com.axeliscore.combat.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.combat.Estado;
import com.axeliscore.combat.Estados;
import com.axeliscore.data.Config;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * (Cliente) Lo que se ve del combate en la pantalla de juego, para cualquier criatura (también las de Minecraft y otros mods):
 * <ul>
 *     <li>Barra de vida sobre las criaturas heridas (la parte blanca baja despacio para ver cuánto quitó el golpe).</li>
 *     <li>Números de daño que saltan (los golpes grandes, más grandes y dorados; las curaciones, en verde).</li>
 *     <li>Íconos de los estados sobre la cabeza, estrellas si está aturdido y partículas (fuego, escarcha, gotas).</li>
 *     <li>Los estados del propio jugador, con su tiempo, arriba de los corazones.</li>
 * </ul>
 * Se apagan en {@code config/axelis_core.json} ({@code barras_vida}, {@code numeros_dano}). Los jefes usan su barra grande.
 */
public final class CombateHud {
    private static final class Seguido {
        float vida = -1, max = 1, lag = -1;
        long herido, ultimo;
        boolean visible = true;
        double dist;
    }

    private static final class Numero {
        final double x, y, z;
        final String txt;
        final int color;
        final float tam, dx;
        final long nace = System.currentTimeMillis();

        Numero(double x, double y, double z, String txt, int color, float tam, float dx) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.txt = txt;
            this.color = color;
            this.tam = tam;
            this.dx = dx;
        }
    }

    private static final Map<Integer, Seguido> SEGUIDOS = new HashMap<>();
    private static final List<Numero> NUMEROS = new ArrayList<>();
    private static final Random R = new Random();
    private static final int VIDA_NUM = 950;
    private static long ultimoTick = Long.MIN_VALUE;
    private static Object ultimoMundo;
    private static boolean barras = true, numeros = true;
    private static int leer;

    private CombateHud() {
    }

    public static void init() {
        com.axeliscore.fx.client.Hud.add(CombateHud::render);
    }

    static void render(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.level != ultimoMundo) {
            ultimoMundo = mc.level;
            SEGUIDOS.clear();
            NUMEROS.clear();
            EstadosCliente.limpiar();
            ultimoTick = Long.MIN_VALUE;
        }
        long t = mc.level.getGameTime();
        if (t != ultimoTick) {
            ultimoTick = t;
            tick(mc);
        }
        float pt = Proyeccion.partial();
        Entity mira = mc.hitResult instanceof EntityHitResult ehr ? ehr.getEntity() : null;
        long now = System.currentTimeMillis();
        if (barras) {
            for (Map.Entry<Integer, Seguido> en : SEGUIDOS.entrySet()) {
                Entity ent = mc.level.getEntity(en.getKey());
                if (ent instanceof LivingEntity le) {
                    barra(g, w, h, le, en.getValue(), le == mira, pt, now);
                }
            }
        }
        if (numeros) {
            dibujarNumeros(g, w, h, now);
        }
        propios(g, w, h, mc.player);
    }

    // ------------------------------------------------------------------ cada tick del juego

    private static void tick(Minecraft mc) {
        if (--leer <= 0) {
            leer = 100;
            Config c = Config.of(AxelisCore.MODID);
            barras = c.bool("barras_vida", true, "Barra de vida sobre las criaturas heridas");
            numeros = c.bool("numeros_dano", true, "Números de daño que saltan al pegar");
        }
        long now = System.currentTimeMillis();
        Player yo = mc.player;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, yo.getBoundingBox().inflate(32))) {
            if (e == yo || !(e instanceof Mob || e instanceof Player)) {
                continue;
            }
            Seguido s = SEGUIDOS.computeIfAbsent(e.getId(), k -> new Seguido());
            float hp = Math.max(0, e.getHealth());
            s.max = Math.max(1, e.getMaxHealth());
            s.dist = Math.sqrt(e.distanceToSqr(yo));
            if (s.vida < 0) {
                s.vida = hp;
                s.lag = hp;
            } else if (hp < s.vida - 0.01F) {
                float d = s.vida - hp;
                s.herido = now;
                numero(e, d, false, s.max);
            } else if (hp > s.vida + 0.99F && e.isAlive()) {
                numero(e, hp - s.vida, true, s.max);
            }
            s.vida = hp;
            // la parte blanca espera un poco y luego baja
            if (s.lag < hp || now - s.herido > 450) {
                s.lag = s.lag < hp ? hp : Math.max(hp, s.lag - s.max * 0.035F);
            }
            s.ultimo = now;
            if ((e.tickCount + e.getId()) % 5 == 0) {
                s.visible = yo.hasLineOfSight(e);
            }
        }
        SEGUIDOS.values().removeIf(s -> now - s.ultimo > 2500);
        NUMEROS.removeIf(n -> now - n.nace > VIDA_NUM);
        particulas(mc);
    }

    private static void numero(LivingEntity e, float v, boolean cura, float max) {
        if (!numeros || v < 0.05F) {
            return;
        }
        String txt = Math.abs(v - Math.round(v)) < 0.05F ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
        int color;
        float tam;
        if (cura) {
            txt = "+" + txt;
            color = 0xFF6CFF7A;
            tam = 1F;
        } else if (v >= max * 0.25F || v >= 10) {
            color = 0xFFFFC23D;
            tam = 1.25F + Math.min(0.5F, v / max);
        } else {
            color = 0xFFFFFFFF;
            tam = 1F;
            int id = e.getId();
            if (EstadosCliente.tiene(id, Estados.QUEMADO)) {
                color = 0xFFFF9A4A;
            } else if (EstadosCliente.tiene(id, Estados.SANGRADO)) {
                color = 0xFFFF5866;
            } else if (EstadosCliente.tiene(id, Estados.CONGELADO)) {
                color = 0xFFA8ECFF;
            } else if (EstadosCliente.tiene(id, Estados.MARCADO)) {
                color = 0xFFFF86C0;
            }
        }
        double r = e.getBbWidth() * 0.6;
        NUMEROS.add(new Numero(e.getX() + (R.nextDouble() - 0.5) * r, e.getY() + e.getBbHeight() * 0.8, e.getZ() + (R.nextDouble() - 0.5) * r,
                txt, color, tam, (R.nextFloat() - 0.5F) * 0.5F));
        if (NUMEROS.size() > 60) {
            NUMEROS.remove(0);
        }
    }

    /** Partículas de los estados (fuego, escarcha, gotas, chispas). */
    private static void particulas(Minecraft mc) {
        for (Integer id : new ArrayList<>(EstadosCliente.conEstados())) {
            Entity ent = mc.level.getEntity(id);
            // al propio jugador no (en primera persona las llamas y copos le taparían la vista)
            if (!(ent instanceof LivingEntity e) || e == mc.player || e.distanceToSqr(mc.player) > 48 * 48 || e.isInvisible()) {
                continue;
            }
            for (EstadosCliente.Visto v : EstadosCliente.de(id)) {
                ParticleOptions p = null;
                int cada = 1;
                switch (v.estado.particula()) {
                    case "flame" -> p = ParticleTypes.FLAME;
                    case "snowflake" -> {
                        p = ParticleTypes.SNOWFLAKE;
                        cada = 3;
                    }
                    case "drip" -> {
                        p = ParticleTypes.DAMAGE_INDICATOR;
                        cada = 8;
                    }
                    case "spark" -> {
                        p = ParticleTypes.ENCHANTED_HIT;
                        cada = 6;
                    }
                    default -> {
                    }
                }
                if (p == null || (e.tickCount + id) % cada != 0) {
                    continue;
                }
                double bw = e.getBbWidth(), bh = e.getBbHeight();
                double x = e.getX() + (R.nextDouble() - 0.5) * bw, y = e.getY() + R.nextDouble() * bh, z = e.getZ() + (R.nextDouble() - 0.5) * bw;
                mc.level.addParticle(p, x, y, z, 0, p == ParticleTypes.FLAME ? 0.02 : -0.02, 0);
            }
        }
    }

    // ------------------------------------------------------------------ dibujo

    private static boolean esJefe(LivingEntity e) {
        String d = e.getType().getDescriptionId();
        return e instanceof com.axeliscore.combat.Jefe || d.equals("entity.minecraft.ender_dragon") || d.equals("entity.minecraft.wither");
    }

    private static void barra(G g, int w, int h, LivingEntity e, Seguido s, boolean mirando, float pt, long now) {
        if (e.isInvisible() || !s.visible || s.dist > 24 || esJefe(e)) {
            return;
        }
        List<EstadosCliente.Visto> est = EstadosCliente.de(e.getId());
        boolean herido = now - s.herido < 6000 && s.vida < s.max;
        boolean mostrarBarra = (herido || (mirando && s.vida < s.max) || !est.isEmpty()) && (e.isAlive() || now - s.herido < 1200);
        if (!mostrarBarra) {
            return;
        }
        double x = Mth.lerp(pt, e.xo, e.getX()), y = Mth.lerp(pt, e.yo, e.getY()) + e.getBbHeight() + 0.5, z = Mth.lerp(pt, e.zo, e.getZ());
        float[] p = Proyeccion.aPantalla(x, y, z, w, h);
        if (p == null || p[0] < -40 || p[0] > w + 40 || p[1] < -40 || p[1] > h + 40) {
            return;
        }
        float k = Mth.clamp(1.25F - p[2] / 24F, 0.6F, 1F);
        // se desvanece al final de los 6 s
        float a = herido || mirando || !est.isEmpty() ? Mth.clamp(1F - (now - s.herido - 5000) / 1000F, 0.25F, 1F) : 1F;
        if (mirando || !est.isEmpty()) {
            a = 1F;
        }
        g.push();
        g.translate(p[0], p[1], 0);
        g.scale(k, k, 1);
        int bw = 32, bh = 4;
        float frac = s.vida / s.max, lag = Mth.clamp(s.lag / s.max, frac, 1F);
        g.fill(-bw / 2 - 1, -bh / 2 - 1, bw / 2 + 1, bh / 2 + 1, Ui.alpha(0xFF0B0D12, 0.75F * a));
        g.fill(-bw / 2, -bh / 2, -bw / 2 + Math.round(bw * lag), bh / 2, Ui.alpha(0xFFF2E6D0, 0.85F * a));
        int c = frac > 0.6F ? Ui.mix(0xFFE8D04A, 0xFF56D86A, (frac - 0.6F) / 0.4F) : Ui.mix(0xFFE04A3E, 0xFFE8D04A, frac / 0.6F);
        g.fill(-bw / 2, -bh / 2, -bw / 2 + Math.round(bw * frac), bh / 2, Ui.alpha(c, a));
        g.fill(-bw / 2, -bh / 2, -bw / 2 + Math.round(bw * frac), -bh / 2 + 1, Ui.alpha(0xFFFFFFFF, 0.35F * a));
        if (mirando) {
            String n = (int) Math.ceil(s.vida) + "/" + Math.round(s.max);
            g.push();
            g.scale(0.5F, 0.5F, 1);
            g.drawString(Ui.font(), n, -Ui.font().width(n) / 2, 6, Ui.alpha(0xFFFFFFFF, a), true);
            g.pop();
        }
        // íconos de los estados encima de la barra
        int n = est.size(), iw = 9, gap = 1;
        int x0 = -(n * iw + (n - 1) * gap) / 2;
        for (int i = 0; i < n; i++) {
            EstadosCliente.Visto v = est.get(i);
            int ix = x0 + i * (iw + gap), iy = -bh / 2 - 2 - iw;
            icono(g, v.estado, ix, iy, iw, v.frac() < 0.2F ? 0.5F + 0.5F * (float) Math.abs(Math.sin(now / 120.0)) : 1F);
            if (v.nivel > 1) {
                g.push();
                g.translate(ix + iw - 2, iy + iw - 3, 0);
                g.scale(0.5F, 0.5F, 1);
                g.drawString(Ui.font(), String.valueOf(v.nivel), 0, 0, 0xFFFFFFFF, true);
                g.pop();
            }
        }
        g.pop();
        if (Estados.aturdido(e)) {
            estrellas(g, w, h, x, Mth.lerp(pt, e.yo, e.getY()) + e.getBbHeight() + 0.15, z, now);
        }
    }

    /** Tres estrellas que giran sobre la cabeza. */
    private static void estrellas(G g, int w, int h, double x, double y, double z, long now) {
        double t = now / 1000.0 * 4.0;
        for (int i = 0; i < 3; i++) {
            double ang = t + i * Math.PI * 2 / 3;
            float[] p = Proyeccion.aPantalla(x + Math.cos(ang) * 0.35, y + Math.sin(ang * 2) * 0.04, z + Math.sin(ang) * 0.35, w, h);
            if (p == null) {
                continue;
            }
            int sz = Math.max(4, Math.round(9 * Mth.clamp(1.3F - p[2] / 20F, 0.5F, 1F)));
            g.blit(Estado.ICONOS, Math.round(p[0] - sz / 2F), Math.round(p[1] - sz / 2F), sz, sz, 7 * 16, 0, 16, 16, Estado.ICONOS_W, 16);
        }
    }

    private static void icono(G g, Estado s, int x, int y, int size, float a) {
        Object[] ic = s.icono();
        ResourceLocation tex = (ResourceLocation) ic[0];
        int u = (Integer) ic[1];
        boolean core = tex == Estado.ICONOS;
        if (a >= 0.99F) {
            g.blit(tex, x, y, size, size, u, 0, 16, 16, core ? Estado.ICONOS_W : 16, 16);
        } else {
            g.blitTint(tex, x, y, size, size, u, 0, 16, 16, core ? Estado.ICONOS_W : 16, 16, Ui.alpha(0xFFFFFFFF, a));
        }
    }

    private static void dibujarNumeros(G g, int w, int h, long now) {
        for (Iterator<Numero> it = NUMEROS.iterator(); it.hasNext(); ) {
            Numero n = it.next();
            float age = (now - n.nace) / (float) VIDA_NUM;
            if (age >= 1) {
                continue;
            }
            float up = (float) (1 - Math.pow(1 - age, 3)) * 0.9F;
            float[] p = Proyeccion.aPantalla(n.x + n.dx * age, n.y + up, n.z, w, h);
            if (p == null || p[2] > 32) {
                continue;
            }
            float pop = age < 0.12F ? 0.6F + age / 0.12F * 0.6F : 1.2F - Math.min(0.2F, (age - 0.12F) * 0.6F);
            float k = n.tam * pop * Mth.clamp(1.4F - p[2] / 16F, 0.55F, 1.2F);
            float a = age > 0.7F ? 1 - (age - 0.7F) / 0.3F : 1F;
            g.push();
            g.translate(p[0], p[1], 0);
            g.scale(k, k, 1);
            g.drawString(Ui.font(), n.txt, -Ui.font().width(n.txt) / 2, -4, Ui.alpha(n.color, a), true);
            g.pop();
        }
    }

    /** Los estados del propio jugador: íconos con su tiempo arriba de los corazones. */
    private static void propios(G g, int w, int h, Player p) {
        List<EstadosCliente.Visto> l = EstadosCliente.de(p.getId());
        if (l.isEmpty()) {
            return;
        }
        int n = l.size(), sz = 16, gap = 4;
        int x0 = w / 2 - (n * sz + (n - 1) * gap) / 2, y = h - 66;
        long now = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            EstadosCliente.Visto v = l.get(i);
            int x = x0 + i * (sz + gap);
            g.fill(x - 2, y - 2, x + sz + 2, y + sz + 2, 0x900B0D12);
            int c = 0xFF000000 | v.estado.color;
            g.fill(x - 2, y + sz + 1, x - 2 + Math.round((sz + 4) * v.frac()), y + sz + 2, c);
            icono(g, v.estado, x, y, sz, v.frac() < 0.2F ? 0.5F + 0.5F * (float) Math.abs(Math.sin(now / 120.0)) : 1F);
            String t = Math.max(1, Math.round(v.resta() / 20F)) + "s";
            g.push();
            g.translate(x + sz / 2F, y + sz + 4, 0);
            g.scale(0.5F, 0.5F, 1);
            g.drawString(Ui.font(), t, -Ui.font().width(t) / 2, 0, 0xFFFFFFFF, true);
            g.pop();
            if (v.nivel > 1) {
                g.drawString(Ui.font(), String.valueOf(v.nivel), x + sz - 4, y + sz - 7, 0xFFFFFFFF, true);
            }
        }
    }
}
