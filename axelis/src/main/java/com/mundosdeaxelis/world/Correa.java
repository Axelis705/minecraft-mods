package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Habitante;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Etapa 282: la «correa» de los enemigos de las estructuras. Cada uno recuerda su sitio y no se va lejos: persigue a quien entra,
 * pero si el jugador sale de la zona lo deja y vuelve (y si se perdió muy lejos, reaparece en su sitio). El guardián que lleva
 * la llave de la celda tiene la correa más corta: se queda dentro de la estructura. Además, al preso se le apunta quién y dónde
 * está su guardián, para que la llave nunca se pierda del todo (ver NpcService).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Correa {
    private Correa() {
    }

    public static final String KEY_TAG = "llave_celda";
    public static final String HOGAR = "mdc_hogar", RADIO = "mdc_radio";
    /** En el preso: sitio, tipo y UUID de su guardián, y cuándo puede volver a aparecer otro. */
    public static final String G_POS = "mdc_llave_pos", G_KIND = "mdc_llave_tipo", G_UUID = "mdc_llave_uuid", G_RENACE = "mdc_llave_renace";
    /** Radio de la correa: el normal y el del guardián de la llave. */
    public static final int RADIO_NORMAL = 26, RADIO_LLAVE = 12;

    public interface AlMundo {
        BlockPos world(int x, int y, int z);
    }

    /** Apunta el sitio del enemigo (al ponerlo la estructura). Los habitantes y los mímicos van por su cuenta. */
    public static void hogar(Mob mob, BlockPos p) {
        if (!(mob instanceof Enemy) || mob instanceof Habitante || mob instanceof com.mundosdeaxelis.entity.Mimico) {
            return;
        }
        CompoundTag d = mob.getPersistentData();
        d.putLong(HOGAR, p.asLong());
        d.putInt(RADIO, KEY_TAG.equals(d.getString(StructPlacer.TAG)) ? RADIO_LLAVE : RADIO_NORMAL);
    }

    /** El nombre del que lleva la llave (dorado y siempre a la vista: así se sabe a quién buscar). */
    public static void nombreLlave(Mob mob) {
        mob.setCustomName(Component.translatable("entity.mundos_de_axelis.con_llave", mob.getType().getDescription()).withStyle(ChatFormatting.GOLD));
        mob.setCustomNameVisible(true);
    }

    /** Tras poner los enemigos del plano: al preso se le apunta su guardián (sitio, tipo y UUID). */
    public static void enlazar(ServerLevel l, StructBp bp, List<UUID> mobs, AlMundo w) {
        Habitante preso = null;
        int llave = -1;
        for (int i = 0; i < bp.mobs.size() && i < mobs.size(); i++) {
            StructBp.Mob m = bp.mobs.get(i);
            if (KEY_TAG.equals(m.tag())) {
                llave = i;
            }
            UUID u = mobs.get(i);
            if (u != null && l.getEntity(u) instanceof Habitante h && h.state() == Habitante.PRESO) {
                preso = h;
            }
        }
        if (preso == null || llave < 0) {
            return;
        }
        StructBp.Mob m = bp.mobs.get(llave);
        CompoundTag d = preso.getPersistentData();
        d.putLong(G_POS, w.world(m.x(), m.y(), m.z()).asLong());
        d.putString(G_KIND, m.kind());
        if (mobs.get(llave) != null) {
            d.putUUID(G_UUID, mobs.get(llave));
        }
    }

    private static Vec3 centro(BlockPos p) {
        return new Vec3(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
    }

    private static int radio(CompoundTag d) {
        int r = d.getInt(RADIO);
        return r > 0 ? r : RADIO_NORMAL;
    }

    /** Hasta dónde persigue fuera de su zona: el de la llave casi no sale. */
    private static int persigue(CompoundTag d) {
        return radio(d) + (KEY_TAG.equals(d.getString(StructPlacer.TAG)) ? 4 : 8);
    }

    /** Los guardianes de la llave de estructuras de antes de la etapa 282: su sitio es donde estén al cargarse. */
    @SubscribeEvent
    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() == false || !(e.getEntity() instanceof Mob mob) || !llevaLlave(mob)
                || mob.getPersistentData().contains(HOGAR)) {
            return;
        }
        hogar(mob, mob.blockPosition());
        if (!mob.hasCustomName()) {
            nombreLlave(mob);
        }
    }

    /** No fija como objetivo a alguien que está fuera de su zona (así no lo sigue hasta el fin del mundo). */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide || e.getNewTarget() == null) {
            return;
        }
        CompoundTag d = mob.getPersistentData();
        if (!d.contains(HOGAR)) {
            return;
        }
        int r = persigue(d);
        if (e.getNewTarget().distanceToSqr(centro(BlockPos.of(d.getLong(HOGAR)))) > (double) r * r) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent e) {
        LivingEntity le = e.getEntity();
        if (!(le instanceof Mob mob) || !(le.level() instanceof ServerLevel sl) || (le.tickCount + le.getId()) % 20 != 0) {
            return;
        }
        CompoundTag d = mob.getPersistentData();
        if (!d.contains(HOGAR) || !mob.isAlive()) {
            return;
        }
        BlockPos home = BlockPos.of(d.getLong(HOGAR));
        Vec3 hc = centro(home);
        int r = radio(d);
        LivingEntity t = mob.getTarget();
        int rp = persigue(d);
        if (t != null && (!t.isAlive() || t.distanceToSqr(hc) > (double) rp * rp)) {
            mob.setTarget(null);
            t = null;
        }
        double d2 = mob.distanceToSqr(hc);
        if (t != null || d2 <= (double) r * r) {
            return;
        }
        if (d2 > (double) (r * 2 + 12) * (r * 2 + 12) || mob.isInLava() && d2 > 9) {
            // se perdió (cayó, lo empujaron muy lejos...): vuelve a su sitio entre humo
            if (sl.isLoaded(home)) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                mob.getNavigation().stop();
                mob.teleportTo(hc.x, hc.y, hc.z);
                mob.clearFire();
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, hc.x, hc.y + 1, hc.z, 10, 0.3, 0.5, 0.3, 0.02);
            }
            return;
        }
        mob.getNavigation().moveTo(hc.x, hc.y, hc.z, 1.1D);
    }

    /** ¿Este enemigo lleva la llave de la celda? */
    public static boolean llevaLlave(Entity e) {
        return KEY_TAG.equals(e.getPersistentData().getString(StructPlacer.TAG));
    }
}
