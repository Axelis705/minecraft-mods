package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.CrateForge;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Etapa 269: el progreso es de cada jugador. Las estructuras de un tier más alto que el suyo (si aún no venció al jefe del tier
 * anterior) quedan selladas para él: no abre sus cofres, barriles ni celdas, sale un aviso de qué jefe vencer y, mientras está
 * dentro, un poder antiguo lo frena (lentitud), como las zonas bloqueadas de Twilight Forest.
 * <p>El orden es el de las cajas: bosque (1), forja (2), hielo (3), fuego (4, el volcán), magia (5) y luego 6 a 8. Etapa del jugador
 * = el jefe más alto que ya venció (los sellos de su Archivo). Puede lootear hasta su etapa + 1. En creativo no hay sellos.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Sellos {
    private Sellos() {
    }

    /** Jefe de cada tier (1 a 5); de la 6 en adelante aún no existen. */
    private static final String[] BOSS = {"rey_brote", "gran_herrero", "lobo_artico", "corazon_magma", "archimago"};

    /** Etapa del jugador: el jefe más alto que ya venció (0 = ninguno). */
    public static int playerStage(ServerPlayer sp) {
        return CrateForge.stage(Essence.archive(sp));
    }

    /** ¿Puede lootear algo de ese tier? (hasta su etapa + 1; el tier 0 o menos no tiene sello). */
    public static boolean canLoot(ServerPlayer sp, int tier) {
        return tier <= 1 || sp.isCreative() || sp.isSpectator() || tier <= playerStage(sp) + 1;
    }

    /** Tier de cada diseño de estructura (las casas de los habitantes no tienen sello). */
    public static int designTier(String design) {
        return switch (design) {
            case "bosque", "santuario" -> 1;
            case "forja", "fragua" -> 2;
            case "hielo", "fortin" -> 3;
            case "volcan" -> 4;
            case "torre", "torre_arcana" -> 5;
            default -> 0;
        };
    }

    /** Tier de cada habitante (el de la estructura donde está preso). */
    public static int npcTier(String npc) {
        return switch (npc) {
            case "ruda" -> 1;
            case "hilda" -> 2;
            case "bruno" -> 3;
            case "ignacio" -> 4;
            case "teo" -> 5;
            default -> 0;
        };
    }

    /** Tier del lugar donde está ese punto: la estructura del mod más alta que lo contiene, o el volcán. */
    public static int tierAt(ServerLevel level, BlockPos pos) {
        int t = StructGuard.tierAt(level, pos);
        if (t < 4 && Volcanes.near(level, pos, 72)) {
            t = 4;
        }
        return t;
    }

    /** El jefe que hay que vencer para lootear ese tier (el del tier anterior). */
    public static Component bossToBeat(int tier) {
        int b = tier - 1;
        if (b >= 1 && b <= BOSS.length) {
            return Component.translatable("entity.mundos_de_axelis." + BOSS[b - 1]).withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("message.mundos_de_axelis.sello.jefe_etapa", b).withStyle(ChatFormatting.GOLD);
    }

    /** Aviso, sonido y humo de alma: «Un sello antiguo protege esto. Vence a X para abrirlo». */
    public static void deny(ServerPlayer sp, BlockPos pos, int tier, String what) {
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.sello." + what, bossToBeat(tier)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        ServerLevel l = sp.serverLevel();
        l.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.9F, 0.7F);
        l.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 1.6F);
        l.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
        l.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.01);
    }

    // ------------------------------------------------------------------ barriles y otros contenedores dentro de una estructura sellada

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        BlockEntity be = sl.getBlockEntity(e.getPos());
        // los cofres del santuario se sellan solos (tienen su etapa); aquí, los barriles y demás contenedores de las estructuras
        if (!(be instanceof Container) || be instanceof com.mundosdeaxelis.block.VaultChestBlockEntity) {
            return;
        }
        int tier = tierAt(sl, e.getPos());
        if (!canLoot(sp, tier)) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            deny(sp, e.getPos(), tier, "barril");
        }
    }

    // ------------------------------------------------------------------ dentro de una estructura que aún no te toca: lentitud y aviso

    private static final Map<UUID, Long> WARNED = new HashMap<>();
    private static final long WARN_EVERY = 20L * 12;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 != 11) {
            return;
        }
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (sp.isCreative() || sp.isSpectator()) {
                continue;
            }
            int tier = tierAt(sp.serverLevel(), sp.blockPosition());
            if (canLoot(sp, tier)) {
                continue;
            }
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, true, true, true));
            long now = server.getTickCount();
            Long last = WARNED.get(sp.getUUID());
            if (last == null || now - last >= WARN_EVERY) {
                WARNED.put(sp.getUUID(), now);
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.sello.zona", bossToBeat(tier)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.35F, 1.4F);
            }
        }
    }

    /** Al cerrar el servidor se olvidan los avisos. */
    public static void clearRuntime() {
        WARNED.clear();
    }
}
