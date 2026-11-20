package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.entity.Mascota;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Acciones especiales del menú de cada mascota (etapa 254). Las que tienen espera la guardan en su figurita, así no se
 * reinicia al quitarla y ponerla. Ninguna da efectos de poder al jugador (las mascotas dan armadura, como se decidió antes):
 * son ayudas y detalles con su historia.
 */
public final class PetActions {
    private PetActions() {
    }

    /** Ticks de espera de cada acción. */
    public static int wait(PetKinds.Special s) {
        return switch (s) {
            case GIFT -> 24000; // una vez por «día» de juego (20 minutos)
            case HEAL -> 3600;
            case GROW -> 1200;
            case SPELL -> 2400;
            case ROAR -> 900;
            case GUIDE -> 200;
            default -> 0;
        };
    }

    /** Segundos que faltan para poder usar su acción (0: lista). */
    public static int secondsLeft(Mascota m, ItemStack fig) {
        if (m.kind() == null || fig.isEmpty()) {
            return 0;
        }
        long at = PetData.read(fig).getLong("SpecialAt");
        long now = m.level().getGameTime();
        return at > now ? (int) ((at - now + 19) / 20) : 0;
    }

    /** ¿Tiene encendida su acción de interruptor (luz o recoger)? */
    public static boolean toggled(Mascota m) {
        if (m.kind() == null) {
            return false;
        }
        return switch (m.kind().special) {
            case LIGHT -> m.hasFlag(Mascota.F_LIGHT);
            case FETCH -> m.hasFlag(Mascota.F_FETCH);
            default -> false;
        };
    }

    private static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("msg.mundos_de_axelis.pet." + key, args), true);
    }

    /** Pulsó el botón de su acción especial en el menú. */
    public static void special(Mascota m, ServerPlayer p, ItemStack fig) {
        PetKinds.Kind k = m.kind();
        if (k == null || fig.isEmpty() || !(m.level() instanceof ServerLevel sl)) {
            return;
        }
        switch (k.special) {
            case LIGHT -> {
                boolean on = !m.hasFlag(Mascota.F_LIGHT);
                m.setFlag(Mascota.F_LIGHT, on);
                PetData.edit(fig).putBoolean("Light", on);
                PetKeeper.saved(p, false);
                say(p, on ? "light_on" : "light_off", m.getDisplayName());
                m.voice(0.5F, on ? 1.3F : 0.9F);
                if (!on) {
                    m.clearLight();
                }
                return;
            }
            case FETCH -> {
                boolean on = !m.hasFlag(Mascota.F_FETCH);
                m.setFlag(Mascota.F_FETCH, on);
                PetData.edit(fig).putBoolean("Fetch", on);
                PetKeeper.saved(p, false);
                say(p, on ? "fetch_on" : "fetch_off", m.getDisplayName());
                m.voice(0.5F, on ? 1.3F : 0.9F);
                return;
            }
            case BAG, NONE -> {
                return;
            }
            default -> {
            }
        }
        if (secondsLeft(m, fig) > 0) {
            return;
        }
        boolean done = switch (k.special) {
            case GIFT -> gift(m, p, sl);
            case HEAL -> heal(m, p, sl);
            case GROW -> grow(m, p, sl);
            case SPELL -> spell(m, p, sl);
            case ROAR -> roar(m, p, sl);
            case GUIDE -> guide(m, p, sl);
            default -> false;
        };
        if (done) {
            PetData.edit(fig).putLong("SpecialAt", sl.getGameTime() + wait(k.special));
            PetKeeper.saved(p, false);
            m.addLove(1);
        }
    }

    /** Le da algo al jugador (a su inventario; si no cabe, a sus pies). */
    private static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
    }

    private static boolean gift(Mascota m, ServerPlayer p, ServerLevel sl) {
        List<ItemStack> list = PetKinds.gift(m.kind().species, sl.random);
        List<Component> names = new ArrayList<>();
        for (ItemStack st : list) {
            names.add(Component.literal(st.getCount() + "× ").append(st.getHoverName()));
            give(p, st.copy());
        }
        Vec3 c = m.getBoundingBox().getCenter();
        sl.sendParticles(Fx.spark(m.kind().rgb, 0.8F), c.x, c.y, c.z, 14, 0.3, 0.3, 0.3, 0.06);
        Fx.confetti(sl, c.x, c.y + 0.3, c.z, 12, 0.3);
        m.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.2F);
        m.voice(0.7F, 1.25F);
        m.level().broadcastEntityEvent(m, Mascota.EV_HAPPY);
        Component what = names.isEmpty() ? Component.empty() : names.get(0);
        for (int i = 1; i < names.size(); i++) {
            what = Component.empty().append(what).append(", ").append(names.get(i));
        }
        say(p, "gift", m.getDisplayName(), what);
        return true;
    }

    private static boolean heal(Mascota m, ServerPlayer p, ServerLevel sl) {
        if (p.distanceToSqr(m) > 10 * 10) {
            say(p, "too_far", m.getDisplayName());
            return false;
        }
        p.heal(6.0F);
        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2.0, p.getZ(), 6, 0.4, 0.2, 0.4, 0.02);
        Vec3 c = m.getBoundingBox().getCenter();
        sl.sendParticles(Fx.note(m.kind().rgb, 0.8F), c.x, c.y + 0.3, c.z, 6, 0.3, 0.2, 0.3, 0.02);
        m.voice(0.6F, 1.0F);
        m.level().broadcastEntityEvent(m, Mascota.EV_CAST);
        say(p, "healed", m.getDisplayName());
        return true;
    }

    private static boolean grow(Mascota m, ServerPlayer p, ServerLevel sl) {
        BlockPos c = m.blockPosition();
        List<BlockPos> plants = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
            BlockState st = sl.getBlockState(q);
            if (st.getBlock() instanceof BonemealableBlock b && !(st.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))
                    && b.isValidBonemealTarget(sl, q, st, false)) {
                plants.add(q.immutable());
            }
        }
        if (plants.isEmpty()) {
            say(p, "no_plants", m.getDisplayName());
            return false;
        }
        java.util.Collections.shuffle(plants, new java.util.Random(sl.random.nextLong()));
        int n = 0;
        for (BlockPos q : plants) {
            if (n >= 8) {
                break;
            }
            if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), sl, q, p)) {
                sl.levelEvent(1505, q, 0);
                n++;
            }
        }
        Vec3 mc = m.getBoundingBox().getCenter();
        sl.sendParticles(Fx.petal(m.kind().rgb, 0.8F), mc.x, mc.y, mc.z, 16, 1.2, 0.4, 1.2, 0.02);
        m.playSound(SoundEvents.BONE_MEAL_USE, 0.8F, 1.1F);
        m.voice(0.6F, 1.2F);
        m.level().broadcastEntityEvent(m, Mascota.EV_CAST);
        say(p, "grew", m.getDisplayName(), n);
        return n > 0;
    }

    private static boolean spell(Mascota m, ServerPlayer p, ServerLevel sl) {
        Vec3 c = m.getBoundingBox().getCenter();
        int rgb = m.kind().rgb;
        m.level().broadcastEntityEvent(m, Mascota.EV_CAST);
        int r = sl.random.nextInt(m.kind().species == 66 ? 5 : 4);
        switch (r) {
            case 0 -> {
                Item[] fl = {Items.POPPY, Items.DANDELION, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET, Items.OXEYE_DAISY, Items.CORNFLOWER,
                        Items.PINK_TULIP};
                give(p, new ItemStack(fl[sl.random.nextInt(fl.length)]));
                Fx.confetti(sl, c.x, c.y + 0.4, c.z, 20, 0.4);
                m.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
                say(p, "spell_flower", m.getDisplayName());
            }
            case 1 -> {
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.2, c.z, 6, 0.3, 0.2, 0.3, 0.01);
                sl.sendParticles(Fx.dust(0x8AE070, 1.2F), c.x, c.y, c.z, 14, 0.5, 0.3, 0.5, 0.0);
                m.playSound(SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.6F);
                say(p, "spell_poof", m.getDisplayName());
            }
            case 2 -> {
                give(p, new ItemStack(Items.COOKIE));
                sl.sendParticles(Fx.bubble(rgb, 0.8F), c.x, c.y + 0.3, c.z, 16, 0.4, 0.3, 0.4, 0.02);
                m.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 0.8F, 1.3F);
                say(p, "spell_cookie", m.getDisplayName());
            }
            case 3 -> {
                m.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, false, false));
                sl.sendParticles(Fx.spark(rgb, 0.9F), c.x, c.y, c.z, 20, 0.4, 0.5, 0.4, 0.05);
                m.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.6F, 1.8F);
                say(p, "spell_float", m.getDisplayName());
            }
            default -> {
                sl.addFreshEntity(new ExperienceOrb(sl, c.x, c.y, c.z, 5 + sl.random.nextInt(4)));
                sl.sendParticles(Fx.rune(rgb, 0.9F), c.x, c.y + 0.3, c.z, 16, 0.4, 0.4, 0.4, 0.03);
                m.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.6F, 1.5F);
                say(p, "spell_xp", m.getDisplayName());
            }
        }
        return true;
    }

    private static boolean roar(Mascota m, ServerPlayer p, ServerLevel sl) {
        m.level().broadcastEntityEvent(m, Mascota.EV_ROAR);
        m.playSound(SoundEvents.RAVAGER_ROAR, 0.55F, 1.9F);
        Vec3 c = m.getBoundingBox().getCenter();
        Vfx.ring(sl, m.position().add(0, 0.15, 0), m.kind().rgb, 6.5, 14);
        int n = 0;
        for (Mob e : m.monstersNear(7.0)) {
            RelicFx.knock(e, m.position(), 1.2, 0.35);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), m);
            n++;
        }
        sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 10, 0.4, 0.2, 0.4, 0.08);
        say(p, n > 0 ? "roar" : "roar_none", m.getDisplayName(), n);
        return true;
    }

    private static boolean guide(Mascota m, ServerPlayer p, ServerLevel sl) {
        BlockPos home = null;
        if (p.getRespawnPosition() != null && p.getRespawnDimension() == sl.dimension()) {
            home = p.getRespawnPosition();
        } else if (sl.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            home = sl.getSharedSpawnPos();
        }
        if (home == null) {
            say(p, "guide_none", m.getDisplayName());
            return false;
        }
        Vec3 from = m.getBoundingBox().getCenter();
        Vec3 to = Vec3.atCenterOf(home);
        Vec3 dir = new Vec3(to.x - from.x, 0, to.z - from.z);
        double dist = dir.length();
        if (dist < 3) {
            say(p, "guide_here", m.getDisplayName());
            return true;
        }
        Vec3 n = dir.normalize();
        int rgb = m.kind().rgb;
        for (int rep = 0; rep < 4; rep++) {
            Scheduler.runLater(rep * 15, () -> {
                for (int i = 1; i <= 24; i++) {
                    double s = i * 0.6;
                    if (s > dist) {
                        break;
                    }
                    Vec3 q = from.add(n.scale(s)).add(0, 0.15 * Math.sin(i * 0.6), 0);
                    sl.sendParticles(p, Fx.spark(rgb, 0.7F), true, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            });
        }
        m.getLookControl().setLookAt(to.x, to.y, to.z);
        m.voice(0.6F, 1.1F);
        String[] dirs = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        double yaw = Math.toDegrees(Math.atan2(-n.x, n.z));
        int idx = Math.floorMod((int) Math.round(yaw / 45.0), 8);
        say(p, "guide", m.getDisplayName(), (int) Math.round(dist), Component.translatable("msg.mundos_de_axelis.pet.dir." + dirs[idx]));
        return true;
    }

    /** Detalles mientras vive: con la luz encendida suelta brillitos de su color. */
    public static void tick(Mascota m) {
        if (m.hasFlag(Mascota.F_LIGHT) && m.tickCount % 12 == 0 && m.level() instanceof ServerLevel sl && m.kind() != null) {
            Vec3 c = m.getBoundingBox().getCenter();
            sl.sendParticles(Fx.spark(m.kind().rgb, 0.45F), c.x, c.y + 0.2, c.z, 1, 0.25, 0.2, 0.25, 0.01);
        }
    }
}
