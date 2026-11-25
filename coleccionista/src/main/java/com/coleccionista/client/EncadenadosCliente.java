package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Encadenados;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * (Cliente) Encadenados: lo que llega del servidor (cadenas, grilletes, gas, placas), la cadena que sujeta a mi jugador (se hace aquí
 * para que se sienta suave, sin tirones) y lo que se ve en el HUD: cuántas parejas se salvaron, el aviso del grillete con quién tiene tu
 * llave, y el aire que te queda en el gas.
 */
public final class EncadenadosCliente {
    private static final String MOD = Coleccionista.MODID;
    /** Una cadena entre dos piezas. */
    public record Cadena(UUID a, UUID b) {
    }

    /** Un grillete en la pared: dónde está atado y la argolla. */
    public record Grillete(UUID id, Vec3 punto, Vec3 argolla, int tiene) {
    }

    public record Placa(int x, int y, int z, boolean on, boolean abierta) {
    }

    static String fase = "NADA";
    static double largo = Encadenados.LARGO, radio = Encadenados.RADIO;
    static int cupo, salvados, equipos;
    static final List<Cadena> cadenas = new ArrayList<>();
    static final Map<UUID, Grillete> grilletes = new HashMap<>();
    static final List<Placa> placas = new ArrayList<>();
    static final Set<UUID> aSalvo = new HashSet<>();
    static CompoundTag gas = new CompoundTag();
    static int aire = 100;
    /** Las entidades de las piezas encadenadas (se busca cada tick). */
    static final Map<UUID, Entity> ents = new HashMap<>();

    private EncadenadosCliente() {
    }

    public static void init() {
        ClientData.listen(Encadenados.KEY, EncadenadosCliente::recibir);
        ClientData.listen(Encadenados.KEY_AIRE, t -> aire = Nbt.i(t, "aire"));
        MinecraftForge.EVENT_BUS.addListener(EncadenadosCliente::tickJugador);
        MinecraftForge.EVENT_BUS.addListener(EncadenadosCliente::tickCliente);
        com.axeliscore.fx.client.Hud.add(EncadenadosCliente::hud);
    }

    public static boolean activa() {
        return !"NADA".equals(fase);
    }

    public static boolean enJuego() {
        return "LISTA".equals(fase) || "HABLANDO".equals(fase) || "JUEGO".equals(fase) || "CIERRE".equals(fase);
    }

    public static boolean encadenado() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && grilletes.containsKey(mc.player.getUUID());
    }

    public static int aire() {
        return aire;
    }

    private static Vec3 v(CompoundTag t, String x, String y, String z) {
        return new Vec3(t.getDouble(x), t.getDouble(y), t.getDouble(z));
    }

    private static void recibir(CompoundTag t) {
        fase = Nbt.s(t, "fase");
        if (fase.isEmpty()) {
            fase = "NADA";
        }
        largo = t.contains("largo") ? t.getDouble("largo") : Encadenados.LARGO;
        radio = t.contains("radio") ? t.getDouble("radio") : Encadenados.RADIO;
        cupo = Nbt.i(t, "cupo");
        salvados = Nbt.i(t, "salvados");
        equipos = Nbt.i(t, "equipos");
        cadenas.clear();
        for (Tag x : t.getList("cadenas", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            cadenas.add(new Cadena(c.getUUID("a"), c.getUUID("b")));
        }
        grilletes.clear();
        for (Tag x : t.getList("grilletes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            UUID id = c.getUUID("id");
            Vec3 a = c.contains("ax") ? v(c, "ax", "ay", "az") : null;
            grilletes.put(id, new Grillete(id, v(c, "x", "y", "z"), a, Nbt.i(c, "tiene")));
        }
        placas.clear();
        for (Tag x : t.getList("placas", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            placas.add(new Placa(Nbt.i(c, "x"), Nbt.i(c, "y"), Nbt.i(c, "z"), Nbt.b(c, "on"), Nbt.b(c, "abierta")));
        }
        aSalvo.clear();
        for (Tag x : t.getList("a_salvo", Tag.TAG_COMPOUND)) {
            aSalvo.add(((CompoundTag) x).getUUID("id"));
        }
        gas = t.getCompound("gas");
        Gas.recibir(gas);
        if (!activa()) {
            aire = 100;
        }
    }

    // ------------------------------------------------------------------ la cadena de mi jugador

    private static void tickCliente(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) {
            return;
        }
        ents.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !activa() || cadenas.isEmpty() && grilletes.isEmpty()) {
            return;
        }
        Set<UUID> buscar = new HashSet<>();
        for (Cadena c : cadenas) {
            buscar.add(c.a());
            buscar.add(c.b());
        }
        buscar.addAll(grilletes.keySet());
        for (Entity en : mc.level.entitiesForRendering()) {
            if (buscar.contains(en.getUUID())) {
                ents.put(en.getUUID(), en);
            }
        }
    }

    /** Antes de moverse: no puede alejarse más que el largo de la cadena; después, si se pasó, se le regresa (chocando con paredes). */
    private static void tickJugador(TickEvent.PlayerTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.side != LogicalSide.CLIENT || e.player != mc.player || !enJuego() || mc.player.isSpectator()) {
            return;
        }
        LocalPlayer me = mc.player;
        UUID yo = me.getUUID();
        if (e.phase == TickEvent.Phase.START) {
            Vec3 v = me.getDeltaMovement();
            Grillete g = grilletes.get(yo);
            if (g != null) {
                v = restringir(me.position(), v, g.punto(), radio);
            }
            for (Cadena c : cadenas) {
                UUID otro = c.a().equals(yo) ? c.b() : c.b().equals(yo) ? c.a() : null;
                Entity o = otro == null ? null : ents.get(otro);
                if (o != null) {
                    v = restringir(me.position(), v, o.position(), largo);
                }
            }
            me.setDeltaMovement(v);
        } else {
            Grillete g = grilletes.get(yo);
            if (g != null) {
                regresar(me, g.punto(), radio + 0.35, 1.0);
            }
            for (Cadena c : cadenas) {
                UUID otro = c.a().equals(yo) ? c.b() : c.b().equals(yo) ? c.a() : null;
                Entity o = otro == null ? null : ents.get(otro);
                if (o != null) {
                    // si la otra pieza es un jugador, cada uno corrige la mitad
                    regresar(me, o.position(), largo + 0.5, o instanceof net.minecraft.world.entity.player.Player ? 0.5 : 1.0);
                }
            }
        }
    }

    /** Último sonido de la cadena (para no repetirlo) y cuánto se ha caminado desde el último tintineo. */
    private static long ultimoSonido;
    private static double caminado;
    private static boolean tensa;

    /**
     * La cadena se siente pesada: desde el 80% del largo, alejarse cuesta cada vez más (la velocidad que aleja se va frenando); pasando el
     * largo todavía se puede avanzar despacio (así se arrastra al compañero, cuyo propio juego lo jala hacia acá) y un resorte suave regresa
     * hacia el centro. Solo de lado; lo vertical lo hace la gravedad.
     */
    static Vec3 restringir(Vec3 pos, Vec3 v, Vec3 centro, double max) {
        double dx = centro.x - pos.x, dz = centro.z - pos.z, dy = centro.y - pos.y;
        double dist = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.25);
        double suave = max * 0.8;
        sentir(pos, v, dist / Math.max(0.5, max));
        if (dist <= suave || dist < 1e-4) {
            return v;
        }
        double h = Math.hypot(dx, dz);
        if (h < 1e-4) {
            return v;
        }
        double nx = dx / h, nz = dz / h;
        double vr = v.x * nx + v.z * nz;
        double x = v.x, z = v.z;
        if (vr < 0) {
            // la parte que aleja: frenada poco a poco (entre 80% y 100% del largo) y casi del todo pasando el largo
            double k = dist >= max ? 0.75 : (dist - suave) / (max - suave) * 0.6;
            x -= nx * vr * k;
            z -= nz * vr * k;
        }
        if (dist > max) {
            // el resorte: jala hacia el centro, más mientras más se pasa (sin tirones)
            double f = Math.min((dist - max) * 0.22, 0.32);
            x += nx * f;
            z += nz * f;
        }
        return new Vec3(x, v.y, z);
    }

    /** Si a pesar de todo quedó muy lejos (un salto, lag), se le regresa de golpe hasta el límite. */
    static void regresar(LocalPlayer me, Vec3 centro, double max, double parte) {
        double dx = centro.x - me.getX(), dz = centro.z - me.getZ();
        double h = Math.hypot(dx, dz);
        // con un compañero se puede pasar un poco (lo va arrastrando); con la argolla de la pared, no
        double limite = parte < 1 ? max + 0.9 : max;
        if (h <= limite || h < 1e-4) {
            return;
        }
        double k = (h - limite) * parte / h;
        me.move(MoverType.SELF, new Vec3(dx * k, 0, dz * k));
    }

    /** Lo que se oye: un tirón cuando se estira de golpe y un tintineo suave al caminar con la cadena floja. */
    private static void sentir(Vec3 pos, Vec3 v, double r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        long ms = System.currentTimeMillis();
        boolean ahora = r > 0.95;
        if (ahora && !tensa && ms - ultimoSonido > 350) {
            ultimoSonido = ms;
            mc.level.playLocalSound(pos.x, pos.y + 1, pos.z, net.minecraft.sounds.SoundEvents.CHAIN_HIT, net.minecraft.sounds.SoundSource.PLAYERS,
                    0.55F, 0.75F + mc.level.random.nextFloat() * 0.2F, false);
        }
        tensa = ahora ? true : r < 0.85 ? false : tensa;
        caminado += Math.hypot(v.x, v.z);
        if (caminado > 1.3) {
            caminado = 0;
            if (ms - ultimoSonido > 250) {
                ultimoSonido = ms;
                mc.level.playLocalSound(pos.x, pos.y + 0.6, pos.z, net.minecraft.sounds.SoundEvents.CHAIN_STEP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.18F + (float) Math.min(0.2, r * 0.15), 0.9F + mc.level.random.nextFloat() * 0.3F,
                        false);
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!activa() || !EventoHud.visible() || mc.player == null) {
            return;
        }
        Font f = Ui.font();
        // debajo de la placa: parejas a salvo y el gas
        if (enJuego() || "FIN".equals(fase)) {
            String s = Component.translatable(MOD + ".enc.hud.salvados", salvados, cupo).getString();
            String gs = "";
            if ("JUEGO".equals(fase) || "CIERRE".equals(fase)) {
                int gr = Nbt.i(gas, "gracia");
                gs = Nbt.b(gas, "on") ? Component.translatable(MOD + ".enc.hud.gas").getString()
                        : Component.translatable(MOD + ".enc.hud.gas_en", Math.max(0, gr - (int) ((System.currentTimeMillis() - Gas.recibido) / 1000)))
                                .getString();
            }
            String linea = gs.isEmpty() ? s : s + "   " + gs;
            EventoHud.lado(g, linea, Nbt.b(gas, "on") ? 0xFFB8D86A : 0xFFD8D0C8, true);
        }
        UUID yo = mc.player.getUUID();
        // el aviso del grillete
        Grillete gr = grilletes.get(yo);
        if (gr != null && enJuego() && !Presentacion.reglasVisibles()) {
            String l1 = Component.translatable(MOD + ".enc.hud.grillete").getString();
            String l2 = Component.translatable(MOD + ".enc.hud.tu_llave", String.format("%02d", gr.tiene())).getString();
            String l3 = Component.translatable(MOD + ".enc.hud.su_llave").getString();
            int bw = Math.max(f.width(l1), Math.max(f.width(l2), f.width(l3))) + 20;
            int x0 = w / 2 - bw / 2, y0 = h - 104;
            float p = (float) (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 380.0));
            g.fillGradient(x0, y0, x0 + bw, y0 + 38, 0xD0140C0E, 0xC0080506);
            g.fill(x0, y0, x0 + bw, y0 + 1, Ui.alpha(EventoHud.ROJO_CL, p));
            g.drawCenteredString(f, "⛓ " + l1, w / 2, y0 + 4, Ui.alpha(EventoHud.ROJO_CL, p));
            g.drawCenteredString(f, l2, w / 2, y0 + 15, 0xFFE8E2D8);
            g.drawCenteredString(f, l3, w / 2, y0 + 26, 0xFFA89E96);
        } else if (aSalvo.contains(yo)) {
            String s = Component.translatable(MOD + ".enc.hud.a_salvo").getString();
            g.drawCenteredString(f, s, w / 2, h - 72, 0xFF9FE08A);
        }
        // el aire en el gas
        if (aire < 100 && enJuego()) {
            float k = 1 - aire / 100F;
            // orillas verdes que se cierran
            int borde = (int) (Math.min(w, h) * 0.22F * k);
            if (borde > 0) {
                int c = Ui.alpha(0xFF3A5A10, 0.55F * k);
                g.fillGradient(0, 0, w, borde, c, 0x00000000);
                g.fillGradient(0, h - borde, w, h, 0x00000000, c);
            }
            int bw = 120, x0 = w / 2 - bw / 2, y0 = h - 64;
            g.fill(x0 - 1, y0 - 1, x0 + bw + 1, y0 + 6, 0xC0000000);
            int col = aire > 50 ? 0xFF9FD040 : aire > 25 ? 0xFFE0B040 : 0xFFE0404A;
            g.fill(x0, y0, x0 + (int) (bw * aire / 100F), y0 + 5, col);
            String s = Component.translatable(MOD + ".enc.hud.aire").getString();
            float p = aire < 30 ? (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 120.0)) : 1F;
            g.drawCenteredString(f, s, w / 2, y0 - 11, Ui.alpha(col, p));
        }
    }
}
