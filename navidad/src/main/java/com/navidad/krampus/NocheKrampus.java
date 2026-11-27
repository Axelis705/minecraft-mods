package com.navidad.krampus;

import com.axeliscore.AxelisCore;
import com.axeliscore.combat.Combate;
import com.axeliscore.data.WorldStore;
import com.navidad.ArbolSanta;
import com.navidad.Logros;
import com.navidad.Navidad;
import com.navidad.Temporada;
import com.navidad.entidad.HuellaKrampus;
import com.navidad.entidad.Krampus;
import com.navidad.entidad.Krampusillo;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La Noche del Krampus. En temporada, cada noche (al caer, hacia las 13 500) se decide para cada quien:
 * <ul>
 *     <li>El <b>5 de diciembre</b> (Krampusnacht, de verdad) visita a todos.</li>
 *     <li>Los otros días visita al que <b>se portó mal</b> desde la última vez (mató aldeanos, gatos, gólems, renos...).</li>
 *     <li>Si no, una noche de cada diez solo vienen <b>krampusillos</b> a robarse los regalos de tu árbol.</li>
 * </ul>
 * La visita: se oyen cadenas a lo lejos, sube la niebla, aparecen huellas de pezuña que se acercan a ti, llegan krampusillos y, al final,
 * sale él donde acaban las huellas. Mientras dura no se puede dormir. Si lo vences o llega el día, sobreviviste a la Noche del Krampus.
 * Se apaga con {@code /navidad krampus visitas no}; en pacífico (o sin aparición de criaturas) no viene.
 */
public final class NocheKrampus {
    /** Etapas de la visita (segundos desde que empieza). */
    static final int HUELLAS = 6, KRAMPUSILLOS = 22, LLEGA = 40;
    /** Para las pruebas: segundos por etapa más cortos. */
    public static float rapidez = 1F;
    public static int visitas, huellas;

    private static final Map<UUID, Visita> VISITAS = new HashMap<>();

    private static final class Visita {
        final ServerLevel l;
        Vec3 paso;
        Vec3 dir;
        int t, pasos;
        boolean izquierda;
        Krampus k;
        boolean llego;

        Visita(ServerLevel l, Vec3 desde) {
            this.l = l;
            this.paso = desde;
        }
    }

    private NocheKrampus() {
    }

    public static void init() {
        AxelisCore.onServerTick(NocheKrampus::tick);
        MinecraftForge.EVENT_BUS.addListener(NocheKrampus::alDormir);
    }

    public static boolean visitasActivas(MinecraftServer s) {
        return !"no".equals(WorldStore.get(s, Navidad.MODID).global().getString("krampusVisitas"));
    }

    public static void visitas(MinecraftServer s, boolean si) {
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        w.global().putString("krampusVisitas", si ? "si" : "no");
        w.dirty();
    }

    public static boolean enVisita(Player p) {
        return VISITAS.containsKey(p.getUUID());
    }

    static void tick(MinecraftServer s) {
        if (!VISITAS.isEmpty()) {
            for (UUID u : List.copyOf(VISITAS.keySet())) {
                ServerPlayer p = s.getPlayerList().getPlayer(u);
                Visita v = VISITAS.get(u);
                if (p == null || p.level() != v.l || !p.isAlive() && v.k == null) {
                    terminar(u, p, false);
                    continue;
                }
                avanzar(p, v);
            }
        }
        if (s.getTickCount() % 40 != 0 || !Temporada.activa(s) || !visitasActivas(s)) {
            return;
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            ServerLevel l = p.serverLevel();
            if (l.dimension() != Level.OVERWORLD || l.getDifficulty() == Difficulty.PEACEFUL || p.isCreative() || p.isSpectator()
                    || !l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
                continue;
            }
            long hora = l.getDayTime() % 24000L, dia = l.getDayTime() / 24000L;
            if (hora < 13500 || hora > 20000) {
                continue;
            }
            CompoundTag yo = WorldStore.get(s, Navidad.MODID).player(p.getUUID());
            if (yo.getLong("krampusNoche") == dia + 1) {
                continue;
            }
            yo.putLong("krampusNoche", dia + 1);
            int malos = com.navidad.Cartas.malosDe(p);
            boolean travieso = yo.contains("krampusMalos") && malos > yo.getInt("krampusMalos");
            yo.putInt("krampusMalos", malos);
            WorldStore.get(s, Navidad.MODID).dirty();
            if (Temporada.diaDiciembre() == 5 || travieso) {
                empezar(p);
            } else if (l.random.nextInt(10) == 0) {
                diablitosAlArbol(p);
            }
        }
    }

    /** Empieza la visita del Krampus para {@code p} (también desde el comando). */
    public static boolean empezar(ServerPlayer p) {
        if (VISITAS.containsKey(p.getUUID())) {
            return false;
        }
        ServerLevel l = p.serverLevel();
        // las huellas empiezan a unos 26 bloques, por un lado al azar
        double a = l.random.nextDouble() * Math.PI * 2;
        Vec3 desde = alPiso(l, p.position().add(Math.cos(a) * 26, 0, Math.sin(a) * 26));
        Visita v = new Visita(l, desde);
        VISITAS.put(p.getUUID(), v);
        visitas++;
        p.sendSystemMessage(Component.translatable("navidad.noche_krampus.empieza").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        l.playSound(null, BlockPos.containing(desde), ModRegistry.NOCHE_KRAMPUS.get(), SoundSource.HOSTILE, 4F, 0.9F);
        KrampusMsg.niebla(p, 0.25F);
        Navidad.LOGGER.info("NAVIDAD_NOCHE_KRAMPUS empieza para {}", p.getGameProfile().getName());
        return true;
    }

    private static Vec3 alPiso(ServerLevel l, Vec3 p) {
        int x = (int) Math.floor(p.x), z = (int) Math.floor(p.z);
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return Combate.alSuelo(l, new Vec3(p.x, y + 0.5, p.z));
    }

    private static int seg(float s) {
        return Math.round(s * 20 / rapidez);
    }

    private static void avanzar(ServerPlayer p, Visita v) {
        v.t++;
        ServerLevel l = v.l;
        if (v.k != null) {
            // ya llegó: termina cuando ya no está (lo vencieron o se fue) o al amanecer
            long hora = l.getDayTime() % 24000L;
            if (!v.k.isAlive() || v.k.isRemoved() || hora < 12500 && hora > 300) {
                terminar(p.getUUID(), p, true);
            }
            return;
        }
        if (v.t >= seg(HUELLAS) && v.t % Math.max(1, seg(0.5F)) == 0) {
            // una huella más, acercándose
            Vec3 hacia = new Vec3(p.getX() - v.paso.x, 0, p.getZ() - v.paso.z);
            double d = hacia.length();
            if (d > 9) {
                v.dir = hacia.scale(1 / d);
                Vec3 lado = new Vec3(-v.dir.z, 0, v.dir.x).scale(v.izquierda ? 0.28 : -0.28);
                v.paso = alPiso(l, v.paso.add(v.dir.scale(0.95)));
                Vec3 h = v.paso.add(lado);
                float giro = (float) (Math.toDegrees(Math.atan2(-v.dir.x, v.dir.z)));
                HuellaKrampus.poner(l, h, giro, v.izquierda);
                v.izquierda = !v.izquierda;
                v.pasos++;
                huellas++;
                l.playSound(null, BlockPos.containing(h), ModRegistry.KRAMPUS_PEZUNA.get(), SoundSource.HOSTILE, 1.6F, 0.9F);
                if (v.pasos % 8 == 0) {
                    l.playSound(null, BlockPos.containing(h), ModRegistry.KRAMPUS_CADENAS.get(), SoundSource.HOSTILE, 2.2F, 0.8F);
                }
            }
            float k = Math.min(0.75F, 0.25F + v.t / (float) seg(LLEGA) * 0.5F);
            if (v.t % 20 == 0) {
                KrampusMsg.niebla(p, k);
            }
        }
        if (v.t == seg(KRAMPUSILLOS)) {
            BlockPos arbol = ArbolSanta.arbolConRegalos(p);
            List<BlockPos> cajas = arbol == null ? List.of() : ArbolSanta.cajasCerca(l, arbol);
            for (int i = 0; i < 2; i++) {
                Vec3 donde = v.paso.add(i * 1.5, 0, -i * 1.5);
                Krampusillo ki = Krampusillo.llamar(l, donde, cajas.isEmpty() ? p : null, null);
                if (ki != null && !cajas.isEmpty()) {
                    ki.robarA(cajas.get(i % cajas.size()), p);
                }
            }
            p.displayClientMessage(Component.translatable("navidad.noche_krampus.risitas").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        if (v.t >= seg(LLEGA)) {
            Vec3 donde = v.paso;
            if (donde.distanceToSqr(p.position()) > 20 * 20 || donde.distanceToSqr(p.position()) < 4 * 4) {
                Vec3 atras = Vec3.directionFromRotation(0, p.getYRot()).scale(-10);
                donde = alPiso(l, p.position().add(atras));
            }
            v.k = Krampus.llamar(l, donde, p, true);
            if (v.k == null) {
                terminar(p.getUUID(), p, false);
                return;
            }
            p.sendSystemMessage(Component.translatable("navidad.noche_krampus.llego", p.getDisplayName())
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            com.axeliscore.fx.Fx.shake(p, 0.7F, 30);
            KrampusMsg.niebla(p, 0.55F);
        }
    }

    private static void terminar(UUID u, ServerPlayer p, boolean sobrevivio) {
        VISITAS.remove(u);
        if (p != null) {
            KrampusMsg.niebla(p, 0F);
            if (sobrevivio && p.isAlive()) {
                Logros.dar(p, "noche_krampus");
                p.sendSystemMessage(Component.translatable("navidad.noche_krampus.sobreviviste").withStyle(ChatFormatting.GOLD));
            }
        }
        Navidad.LOGGER.info("NAVIDAD_NOCHE_KRAMPUS termina (sobrevivió={})", sobrevivio);
    }

    /** Una noche de krampusillos: vienen dos a robarse regalos de tu árbol (si tienes). */
    public static boolean diablitosAlArbol(ServerPlayer p) {
        BlockPos arbol = ArbolSanta.arbolConRegalos(p);
        if (arbol == null) {
            return false;
        }
        ServerLevel l = p.serverLevel();
        List<BlockPos> cajas = ArbolSanta.cajasCerca(l, arbol);
        for (int i = 0; i < Math.min(2, cajas.size()); i++) {
            double a = l.random.nextDouble() * Math.PI * 2;
            Vec3 donde = alPiso(l, Vec3.atCenterOf(arbol).add(Math.cos(a) * 12, 0, Math.sin(a) * 12));
            Krampusillo k = Krampusillo.llamar(l, donde, null, null);
            if (k != null) {
                k.robarA(cajas.get(i), p);
            }
        }
        p.displayClientMessage(Component.translatable("navidad.noche_krampus.risitas").withStyle(ChatFormatting.DARK_PURPLE), true);
        Navidad.LOGGER.info("NAVIDAD_NOCHE_KRAMPUS krampusillos al árbol de {}", p.getGameProfile().getName());
        return true;
    }

    static void alDormir(PlayerSleepInBedEvent e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) {
            return;
        }
        boolean cerca = !p.level().getEntitiesOfClass(Krampus.class, p.getBoundingBox().inflate(48)).isEmpty();
        if (enVisita(p) || cerca) {
            e.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            p.displayClientMessage(Component.translatable("navidad.noche_krampus.no_dormir").withStyle(ChatFormatting.DARK_RED), true);
        }
    }
}
