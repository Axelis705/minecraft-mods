package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.ColdPacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.mundosdeaxelis.block.EternalBrazierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Medidor de frío (0 a 100). Las zonas heladas y la ventisca lo suben ({@link #zone}); junto a un fuego (fogata, horno encendido,
 * lava...) baja rápido y con el tiempo se recupera solo. Con 40 te ralentiza, con 70 además te entumece las manos, cerca de 100 se
 * activa la escarcha del juego (pantalla blanca y daño) y al llegar a 100 quedas congelado en hielo ({@link Frozen}).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Cold {
    private Cold() {
    }

    private static final class St {
        float meter;
        float rate;
        long zoneUntil;
        int sent = -1;
        int heat;
    }

    private static final Map<UUID, St> MAP = new HashMap<>();

    /** El jugador está expuesto al frío ({@code perSecond} puntos por segundo) durante {@code ticks}. */
    public static void zone(ServerPlayer p, float perSecond, int ticks) {
        St s = MAP.computeIfAbsent(p.getUUID(), k -> new St());
        s.rate = perSecond;
        s.zoneUntil = p.serverLevel().getGameTime() + ticks;
    }

    public static float get(ServerPlayer p) {
        St s = MAP.get(p.getUUID());
        return s == null ? 0.0F : s.meter;
    }

    public static void set(ServerPlayer p, float value) {
        St s = MAP.computeIfAbsent(p.getUUID(), k -> new St());
        s.meter = Math.max(0.0F, Math.min(100.0F, value));
        sync(p, s);
    }

    public static void add(ServerPlayer p, float delta) {
        set(p, get(p) + delta);
    }

    private static void sync(ServerPlayer p, St s) {
        int v = Math.round(s.meter);
        if (v != s.sent) {
            s.sent = v;
            ModNetwork.sendTo(p, new ColdPacket(v));
        }
    }

    /** ¿Hay una fuente de calor cerca? */
    public static boolean nearHeat(ServerPlayer p) {
        return heat(p) > 0;
    }

    /**
     * Calor a tu alrededor: 0 nada, 1 fuego normal a 3 bloques (fogata, horno encendido, fuego, magma, lava), 2 Brasero Eterno
     * encendido a 5 bloques o zona de calor del Lobo Ártico (enfría el medidor más rápido y funde el hielo que te atrapa).
     */
    public static int heat(ServerPlayer p) {
        if (HeatZones.inside(p)) {
            return 2;
        }
        ServerLevel l = p.serverLevel();
        BlockPos c = p.blockPosition();
        int best = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int h2 = dx * dx + dz * dz;
                    if (h2 > 25) {
                        continue;
                    }
                    BlockState st = l.getBlockState(c.offset(dx, dy, dz));
                    if (st.isAir()) {
                        continue;
                    }
                    Block b = st.getBlock();
                    if (b instanceof EternalBrazierBlock) {
                        if (st.getValue(EternalBrazierBlock.LIT)) {
                            return 2;
                        }
                        continue;
                    }
                    if (h2 > 9 || Math.abs(dy) > 2 || best > 0) {
                        continue;
                    }
                    if ((b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE) && st.getValue(BlockStateProperties.LIT)) {
                        best = 1;
                    } else if ((b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) && st.getValue(BlockStateProperties.LIT)) {
                        best = 1;
                    } else if (b == Blocks.FIRE || b == Blocks.MAGMA_BLOCK || b == Blocks.LAVA || b == Blocks.LAVA_CAULDRON) {
                        best = 1;
                    }
                }
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || MAP.isEmpty()) {
            return;
        }
        St s = MAP.get(p.getUUID());
        if (s == null) {
            return;
        }
        long now = p.serverLevel().getGameTime();
        if (now % 10 == 0) {
            s.heat = s.meter > 0.0F ? heat(p) : 0;
        }
        float exposure = now < s.zoneUntil ? s.rate : 0.0F;
        boolean warm = s.heat > 0;
        float perSecond = exposure * (warm ? 0.1F : 1.0F) - (s.heat >= 2 ? 22.0F : warm ? 12.0F : 0.0F) - (exposure <= 0.0F ? 3.0F : 0.0F);
        s.meter = Math.max(0.0F, Math.min(100.0F, s.meter + perSecond / 20.0F));
        if (s.meter >= 100.0F) {
            // congelado de golpe; si está en su segundo de inmunidad, se queda al borde
            if (Frozen.freeze(p, Frozen.DEFAULT_HITS)) {
                s.meter = 65.0F;
            } else {
                s.meter = 99.0F;
            }
        }
        if (s.meter >= 50.0F) {
            // escarcha de vanilla: pantalla blanca; cerca de 100 también quita vida (las prendas de cuero protegen)
            p.setTicksFrozen(Math.max(p.getTicksFrozen(), (int) (s.meter * 1.45F)));
        }
        if (now % 10 == 0) {
            if (s.meter >= 40.0F) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, s.meter >= 70.0F ? 1 : 0, true, false, false));
            }
            if (s.meter >= 70.0F) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, true, false, false));
            }
        }
        sync(p, s);
        if (s.meter <= 0.0F && exposure <= 0.0F && !Frozen.isFrozen(p)) {
            MAP.remove(p.getUUID());
        }
    }

    private static void reset(ServerPlayer p) {
        if (MAP.remove(p.getUUID()) != null) {
            ModNetwork.sendTo(p, new ColdPacket(0));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            reset(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            MAP.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            reset(p);
        }
    }
}
