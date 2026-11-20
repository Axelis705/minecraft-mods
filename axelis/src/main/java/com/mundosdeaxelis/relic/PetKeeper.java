package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Mascota;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Etapa 254: las mascotas equipadas son criaturas de verdad ({@link Mascota}). Este administrador decide cuándo existe cada una:
 * <ul>
 *   <li>Equipada → aparece junto a ti y te sigue; quitada del menú → se despide y desaparece.</li>
 *   <li>Sentada → se queda en su sitio aunque te vayas. Si te alejas mucho (o cambias de mundo) «se queda en casa»: la criatura
 *   se quita sin hacer ruido y vuelve a aparecer sentada en el mismo sitio cuando regresas. El sitio se guarda con el jugador.</li>
 *   <li>Si se desmaya en una pelea vuelve a su figurita y regresa sola al minuto.</li>
 * </ul>
 * Las criaturas nunca se guardan con el mundo: las vuelve a crear este administrador (así no hay copias ni mascotas perdidas).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class PetKeeper {
    private PetKeeper() {
    }

    private static final String SIT_KEY = "mdc_pet_sit";
    /** Ticks que descansa en su figurita tras desmayarse. */
    public static final int FAINT_TICKS = 1200;
    /** Distancia a la que una mascota sentada «se queda en casa» (y vuelve a aparecer al regresar). */
    private static final double HOME_RANGE = 44.0;

    private static final class Rec {
        final Companions.Spec spec;
        Mascota pet;
        long faintUntil;
        ResourceKey<Level> sitDim;
        Vec3 sitPos;
        float sitYaw;

        Rec(Companions.Spec spec) {
            this.spec = spec;
        }
    }

    private static final Map<UUID, Map<Companions.Spec, Rec>> OWNERS = new HashMap<>();

    /** La figurita equipada de esa mascota (donde se guardan sus recuerdos), o vacío. */
    public static ItemStack figurine(ServerPlayer sp, Companions.Spec spec) {
        Equipment.Data d = Equipment.get(sp);
        int n = Math.min(Equipment.MAX_PETS, Equipment.petSlots(sp));
        for (int i = 0; i < n; i++) {
            if (d.pets[i].getItem() instanceof CompanionItem ci && ci.spec() == spec) {
                return d.pets[i];
            }
        }
        return ItemStack.EMPTY;
    }

    /** Guarda los cambios de la figurita (y avisa al cliente para que su descripción esté al día). */
    public static void saved(ServerPlayer sp, boolean sync) {
        Equipment.save(sp);
        if (sync) {
            Equipment.sync(sp);
        }
    }

    /**
     * Se llama varias veces por segundo desde el equipo del jugador: crea, mueve o quita las criaturas para que coincidan con
     * las figuritas equipadas.
     */
    public static void follow(ServerPlayer sp, List<Companions.Spec> wanted) {
        Map<Companions.Spec, Rec> m = OWNERS.computeIfAbsent(sp.getUUID(), k -> new LinkedHashMap<>());
        Iterator<Map.Entry<Companions.Spec, Rec>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Rec r = it.next().getValue();
            if (!wanted.contains(r.spec)) {
                if (r.pet != null && !r.pet.isRemoved()) {
                    r.pet.leave(true);
                }
                clearSit(sp, r.spec);
                it.remove();
            }
        }
        long now = sp.serverLevel().getGameTime();
        for (Companions.Spec w : wanted) {
            Rec r = m.get(w);
            if (r == null) {
                r = new Rec(w);
                loadSit(sp, r);
                m.put(w, r);
                ItemStack fig = figurine(sp, w);
                if (r.sitPos == null && !fig.isEmpty() && PetData.read(fig).contains("Home")) {
                    PetData.edit(fig).remove("Home"); // se había quedado de otra vez
                    saved(sp, true);
                }
            }
            try {
                tend(sp, r, now);
            } catch (Exception e) {
                MundosDeAxelis.LOGGER.error("Error con la mascota " + w.id(), e);
            }
        }
    }

    private static void tend(ServerPlayer sp, Rec r, long now) {
        if (r.faintUntil > now) {
            return;
        }
        // el descanso va guardado en la figurita: quitarla y ponerla no lo salta
        long rest = PetData.read(figurine(sp, r.spec)).getLong("RestUntil");
        if (rest > now) {
            r.faintUntil = rest;
            return;
        }
        Mascota p = r.pet;
        if (p != null && (p.isRemoved() || !p.isAlive())) {
            p = null;
            r.pet = null;
        }
        if (r.sitPos != null) {
            boolean near = sp.level().dimension().equals(r.sitDim) && sp.position().distanceTo(r.sitPos) < HOME_RANGE;
            if (!near) {
                if (p != null) {
                    p.leave(false); // se queda en casa: sin ruido ni chispas
                    r.pet = null;
                }
                return;
            }
            if (p == null || p.level() != sp.level()) {
                if (p != null) {
                    p.leave(false);
                }
                r.pet = spawn(sp, r, r.sitPos, r.sitYaw, true);
            } else if (p.isOrderedToSit()) {
                r.sitPos = p.position(); // si alguien la empujó, recuerda dónde quedó
            }
            return;
        }
        if (p == null || p.level() != sp.level()) {
            if (sp.isSpectator()) {
                return;
            }
            if (p != null) {
                p.leave(false);
            }
            r.pet = spawn(sp, r, null, sp.getYRot(), false);
            return;
        }
        if (p.distanceToSqr(sp) > 34 * 34 && !sp.isSpectator()) {
            p.teleportNear(sp);
        }
    }

    private static Mascota spawn(ServerPlayer sp, Rec r, Vec3 at, float yaw, boolean sitting) {
        ServerLevel sl = sp.serverLevel();
        Mascota m = ModEntities.MASCOTA.get().create(sl);
        if (m == null) {
            return null;
        }
        m.setup(sp, r.spec, figurine(sp, r.spec));
        Vec3 pos = at != null ? at : m.spotNear(sp);
        m.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        m.setYHeadRot(yaw);
        m.yBodyRot = yaw;
        if (sitting) {
            m.setOrderedToSit(true);
            m.setInSittingPose(true);
        }
        sl.addFreshEntity(m);
        if (!sitting) {
            m.arriveFx();
        }
        return m;
    }

    private static Rec rec(Mascota m) {
        UUID o = m.getOwnerUUID();
        if (o == null || m.spec() == null) {
            return null;
        }
        Map<Companions.Spec, Rec> map = OWNERS.get(o);
        return map == null ? null : map.get(m.spec());
    }

    /** ¿Es esta criatura la mascota vigente de su dueño? (si no, sobra y se quita). */
    public static boolean isCurrent(Mascota m) {
        Rec r = rec(m);
        return r != null && r.pet == m;
    }

    /** Se cansó en una pelea: vuelve a su figurita y regresa sola al minuto. */
    public static void fainted(Mascota m, ServerPlayer owner) {
        Rec r = rec(m);
        if (r == null) {
            return;
        }
        r.pet = null;
        r.faintUntil = m.level().getGameTime() + FAINT_TICKS;
        if (owner != null) {
            r.sitPos = null;
            clearSit(owner, r.spec);
            ItemStack fig = figurine(owner, r.spec);
            if (!fig.isEmpty()) {
                PetData.edit(fig).putLong("RestUntil", r.faintUntil);
                PetData.edit(fig).remove("Home");
                saved(owner, true);
            }
            owner.displayClientMessage(Component.translatable("msg.mundos_de_axelis.pet.fainted", m.getDisplayName(), FAINT_TICKS / 20), true);
        }
    }

    /** Ticks que le quedan de descanso tras desmayarse (0 si está bien). */
    public static int restTicks(ServerPlayer sp, Companions.Spec spec) {
        Map<Companions.Spec, Rec> map = OWNERS.get(sp.getUUID());
        Rec r = map == null ? null : map.get(spec);
        return r == null ? 0 : (int) Math.max(0, r.faintUntil - sp.level().getGameTime());
    }

    /** La mascota se sentó o se levantó: se recuerda el sitio (también al salir del juego). */
    public static void sitChanged(Mascota m, ServerPlayer owner, boolean sit) {
        Rec r = rec(m);
        if (r == null || owner == null) {
            return;
        }
        if (sit) {
            r.sitPos = m.position();
            r.sitDim = m.level().dimension();
            r.sitYaw = m.getYRot();
            CompoundTag all = owner.getPersistentData().getCompound(SIT_KEY);
            CompoundTag t = new CompoundTag();
            t.putString("Dim", r.sitDim.location().toString());
            t.putDouble("X", r.sitPos.x);
            t.putDouble("Y", r.sitPos.y);
            t.putDouble("Z", r.sitPos.z);
            t.putFloat("Yaw", r.sitYaw);
            all.put(r.spec.id(), t);
            owner.getPersistentData().put(SIT_KEY, all);
        } else {
            r.sitPos = null;
            clearSit(owner, r.spec);
        }
        // la figurita recuerda dónde te espera (se ve en su descripción)
        ItemStack fig = figurine(owner, r.spec);
        if (!fig.isEmpty()) {
            if (sit) {
                CompoundTag h = new CompoundTag();
                h.putString("Dim", r.sitDim.location().toString());
                h.putInt("X", (int) Math.floor(r.sitPos.x));
                h.putInt("Y", (int) Math.floor(r.sitPos.y));
                h.putInt("Z", (int) Math.floor(r.sitPos.z));
                PetData.edit(fig).put("Home", h);
            } else {
                PetData.edit(fig).remove("Home");
            }
            saved(owner, true);
        }
    }

    private static void clearSit(ServerPlayer sp, Companions.Spec spec) {
        CompoundTag all = sp.getPersistentData().getCompound(SIT_KEY);
        if (all.contains(spec.id())) {
            all.remove(spec.id());
            sp.getPersistentData().put(SIT_KEY, all);
        }
    }

    private static void loadSit(ServerPlayer sp, Rec r) {
        CompoundTag all = sp.getPersistentData().getCompound(SIT_KEY);
        if (!all.contains(r.spec.id())) {
            return;
        }
        CompoundTag t = all.getCompound(r.spec.id());
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("Dim"));
        if (dim == null) {
            return;
        }
        r.sitDim = ResourceKey.create(Registries.DIMENSION, dim);
        r.sitPos = new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"));
        r.sitYaw = t.getFloat("Yaw");
    }

    /** Quita (sin ruido) todas las mascotas de un jugador: al salir del juego. Los sitios donde estaban sentadas se recuerdan. */
    private static void dropAll(UUID owner) {
        Map<Companions.Spec, Rec> m = OWNERS.remove(owner);
        if (m == null) {
            return;
        }
        for (Rec r : m.values()) {
            if (r.pet != null && !r.pet.isRemoved()) {
                r.pet.leave(false);
            }
        }
    }

    /** Tus flechas (y lo que lances) atraviesan a tus mascotas en vez de rebotar en ellas. */
    @SubscribeEvent
    public static void onProjectile(net.minecraftforge.event.entity.ProjectileImpactEvent e) {
        if (e.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult eh && eh.getEntity() instanceof Mascota m
                && e.getProjectile().getOwner() instanceof net.minecraft.world.entity.LivingEntity shooter && m.isOwnedBy(shooter)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        dropAll(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(SIT_KEY)) {
            e.getEntity().getPersistentData().put(SIT_KEY, old.getCompound(SIT_KEY).copy());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        OWNERS.clear();
    }
}
