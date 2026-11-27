package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Año Nuevo, como en México: a la medianoche del 31 de diciembre (hora del centro de México) suenan las <b>12 campanadas</b> y con cada
 * una te comes una uva y pides un deseo. Antes, una cuenta regresiva de 10 en la pantalla de todos; después, "¡Feliz Año Nuevo!" y
 * fuegos artificiales alrededor de cada quien. Quien se come las 12 uvas a tiempo tiene suerte mucho rato y un regalo de los grandes.
 * <p>Se puede hacer cuando sea con {@code /navidad anio_nuevo} (para una fiesta o para probar). El 31 de diciembre, al entrar, a cada
 * quien le tocan sus 12 uvas.
 */
public final class AnioNuevo {
    /** Ticks entre campanadas (1.5 segundos: lo que tarda comerse una uva). */
    public static final int ENTRE = 30;
    public static final int CUENTA = 10;

    private static Show show;
    /** Cuántas uvas se comió cada quien en el último Año Nuevo (para la prueba con fotos). */
    public static final Map<UUID, Integer> ultimo = new HashMap<>();

    private AnioNuevo() {
    }

    public static void init() {
        AxelisCore.onServerTick(AnioNuevo::tick);
        AxelisCore.onLogin(AnioNuevo::alEntrar);
    }

    /** El show que está pasando: cuenta regresiva, campanadas y fuegos. Cuenta en ticks desde que empezó. */
    static final class Show {
        final int cuenta;
        final int anio;
        int t;
        final Map<UUID, Integer> uvas = new HashMap<>();

        Show(int cuenta, int anio) {
            this.cuenta = cuenta;
            this.anio = anio;
        }

        int inicioCampanadas() {
            return cuenta * 20;
        }

        int finCampanadas() {
            return inicioCampanadas() + 12 * ENTRE;
        }
    }

    public static boolean pasando() {
        return show != null;
    }

    /** Empieza el show con una cuenta regresiva de tantos segundos. */
    public static void empezar(MinecraftServer s, int segundos) {
        LocalDateTime h = LocalDateTime.now(Temporada.ZONA);
        // el año que empieza: en la segunda mitad del año es el que sigue; en enero (pasada la medianoche) es este
        int anio = h.getMonthValue() >= 7 ? h.getYear() + 1 : h.getYear();
        show = new Show(Math.max(0, segundos), anio);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("navidad.anio.prepara").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        Navidad.LOGGER.info("NAVIDAD_ANIO empieza cuenta={} anio={}", segundos, anio);
    }

    /** Cuando alguien se come una uva (desde {@link com.navidad.objeto.Uvas}). */
    public static void comioUva(ServerPlayer p) {
        if (show == null || show.t < show.inicioCampanadas() - 20 || show.t > show.finCampanadas() + 40) {
            return;
        }
        int n = show.uvas.merge(p.getUUID(), 1, Integer::sum);
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.anio.uva", Math.min(n, 12))
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    // ------------------------------------------------------------------ cada tick

    static void tick(MinecraftServer s) {
        if (show == null) {
            if (s.getTickCount() % 20 == 0) {
                revisarReloj(s);
            }
            return;
        }
        Show sh = show;
        int t = sh.t++;
        // la cuenta regresiva: 10, 9, 8...
        if (t < sh.inicioCampanadas() && t % 20 == 0) {
            int n = sh.cuenta - t / 20;
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                titulo(p, Component.literal(String.valueOf(n)).withStyle(n <= 3 ? ChatFormatting.RED : ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable("navidad.anio.falta"), 0, 22, 0);
                p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.MASTER, 0.8F, n <= 3 ? 1.6F : 1.2F);
            }
        }
        // las doce campanadas
        int dc = t - sh.inicioCampanadas();
        if (dc >= 0 && dc < 12 * ENTRE && dc % ENTRE == 0) {
            int k = dc / ENTRE + 1;
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                p.playNotifySound(ModRegistry.CAMPANADA.get(), SoundSource.MASTER, 1.2F, 1F);
                titulo(p, Component.literal("♪ " + k + " ♪").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                        Component.translatable(k == 1 ? "navidad.anio.come" : "navidad.anio.campanada", k), 0, ENTRE, 4);
            }
            Navidad.LOGGER.info("NAVIDAD_ANIO campanada {}", k);
        }
        // ¡feliz año nuevo!
        if (t == sh.finCampanadas() + 20) {
            felicidades(s, sh);
        }
        int df = t - (sh.finCampanadas() + 20);
        if (df >= 0 && df < 600 && df % 6 == 0) {
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                fuego(p);
            }
        }
        if (df >= 600) {
            show = null;
            Navidad.LOGGER.info("NAVIDAD_ANIO fin");
        }
    }

    private static void felicidades(MinecraftServer s, Show sh) {
        ultimo.clear();
        ultimo.putAll(sh.uvas);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            titulo(p, Component.translatable("navidad.anio.feliz").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("navidad.anio.feliz2", sh.anio).withStyle(ChatFormatting.YELLOW), 10, 100, 30);
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1F, 0.8F);
            p.playNotifySound(ModRegistry.CASCABELES.get(), SoundSource.MASTER, 1F, 1F);
            Premios.confeti(p.serverLevel(), p.getX(), p.getY() + 3.4, p.getZ(), true);
            int n = Math.min(12, sh.uvas.getOrDefault(p.getUUID(), 0));
            if (n >= 12) {
                p.sendSystemMessage(Component.translatable("navidad.anio.doce").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                p.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60 * 60, 1));
                p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 30, 0));
                Fiestas.darCaja(p, Premios.sacar(Premios.Rareza.EPICO, p.getRandom()), "Año Nuevo");
            } else if (n > 0) {
                p.sendSystemMessage(Component.translatable("navidad.anio.algunas", n).withStyle(ChatFormatting.LIGHT_PURPLE));
                p.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60 * 5 * n / 6 + 1200, 0));
            } else {
                p.sendSystemMessage(Component.translatable("navidad.anio.ninguna").withStyle(ChatFormatting.GRAY));
            }
        }
        Navidad.LOGGER.info("NAVIDAD_ANIO feliz anio={} uvas={}", sh.anio, sh.uvas.values());
    }

    private static void titulo(ServerPlayer p, Component t, Component sub, int entra, int queda, int sale) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(entra, queda, sale));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(t));
    }

    /** Un cohete de colores cerca de alguien (a cielo abierto y nunca encima de nadie: si revienta cerca, lastima). */
    private static void fuego(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        RandomSource r = p.getRandom();
        for (int intento = 0; intento < 4; intento++) {
            double a = r.nextDouble() * Math.PI * 2, d = 9 + r.nextDouble() * 14;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (Math.abs(y - p.getY()) > 24 || y < p.getY() - 12) {
                continue;
            }
            boolean libre = true;
            for (ServerPlayer o : l.players()) {
                double dx = o.getX() - x, dz = o.getZ() - z;
                if (dx * dx + dz * dz < 7 * 7) {
                    libre = false;
                    break;
                }
            }
            if (!libre) {
                continue;
            }
            FireworkRocketEntity f = new FireworkRocketEntity(l, x + 0.5, y + 0.2, z + 0.5, cohete(r));
            l.addFreshEntity(f);
            return;
        }
    }

    private static final int[] COLORES = {0xFF3B3B, 0xFFD23A, 0x3BE070, 0x4AA0FF, 0xFF70D0, 0xFFFFFF, 0xFF8A1C, 0xB070FF};

    public static ItemStack cohete(RandomSource r) {
        ItemStack f = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fw = f.getOrCreateTagElement("Fireworks");
        fw.putByte("Flight", (byte) (1 + r.nextInt(2)));
        ListTag ex = new ListTag();
        int n = 1 + (r.nextInt(4) == 0 ? 1 : 0);
        for (int i = 0; i < n; i++) {
            CompoundTag e = new CompoundTag();
            int[] tipos = {0, 1, 1, 2, 4};
            e.putByte("Type", (byte) tipos[r.nextInt(tipos.length)]);
            e.putIntArray("Colors", new int[]{COLORES[r.nextInt(COLORES.length)], COLORES[r.nextInt(COLORES.length)]});
            e.putIntArray("FadeColors", new int[]{COLORES[r.nextInt(COLORES.length)]});
            e.putBoolean("Flicker", r.nextBoolean());
            e.putBoolean("Trail", r.nextInt(3) == 0);
            ex.add(e);
        }
        fw.put("Explosions", ex);
        return f;
    }

    // ------------------------------------------------------------------ el reloj de verdad y las uvas del 31

    /** A las 23:59:50 del 31 de diciembre empieza solo (una vez por año). */
    private static void revisarReloj(MinecraftServer s) {
        LocalDateTime h = LocalDateTime.now(Temporada.ZONA);
        if (h.getMonth() != Month.DECEMBER || h.getDayOfMonth() != 31 || h.getHour() != 23 || h.getMinute() != 59 || h.getSecond() < 50) {
            return;
        }
        if ("no".equals(Temporada.modo(s)) || s.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        if (w.global().getInt("anioNuevo") == h.getYear()) {
            return;
        }
        w.global().putInt("anioNuevo", h.getYear());
        w.dirty();
        empezar(s, 60 - h.getSecond());
    }

    static void alEntrar(ServerPlayer p) {
        LocalDateTime h = LocalDateTime.now(Temporada.ZONA);
        if (h.getMonth() != Month.DECEMBER || h.getDayOfMonth() != 31 || "no".equals(Temporada.modo(p.server))) {
            return;
        }
        WorldStore w = WorldStore.get(p.server, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        if (yo.getInt("uvas") == h.getYear()) {
            return;
        }
        yo.putInt("uvas", h.getYear());
        w.dirty();
        p.sendSystemMessage(Component.translatable("navidad.anio.nochevieja").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        ItemStack u = new ItemStack(ModRegistry.UVAS.get(), 12);
        if (!p.getInventory().add(u)) {
            p.drop(u, false);
        }
    }
}
