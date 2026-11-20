package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.relic.PetKinds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Comportamientos de {@link Mascota}: seguirte, pasear, flotar, pelear (según su ficha), recoger cosas y asustarse. */
final class MascotaGoals {
    private MascotaGoals() {
    }

    // ------------------------------------------------------------------ seguir al dueño (anda o vuela; se teletransporta si te alejas)

    static final class Follow extends Goal {
        private final Mascota pet;
        private LivingEntity owner;
        private int recalc;

        Follow(Mascota pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private double startDist() {
            return pet.flier() ? 4.0 : 5.0;
        }

        @Override
        public boolean canUse() {
            LivingEntity o = pet.getOwner();
            if (o == null || o.isSpectator() || pet.isOrderedToSit() || pet.isLeashed() || pet.leaving()) {
                return false;
            }
            double d = pet.distanceToSqr(o);
            if (pet.getTarget() != null && d < 16 * 16) {
                return false; // peleando cerca de ti: sigue peleando
            }
            if (d < startDist() * startDist()) {
                return false;
            }
            owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (owner == null || pet.isOrderedToSit() || pet.getNavigation().isDone() || pet.leaving()) {
                return false;
            }
            return pet.distanceToSqr(owner) > 2.4 * 2.4;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            owner = null;
            pet.getNavigation().stop();
        }

        @Override
        public void tick() {
            pet.getLookControl().setLookAt(owner, 10.0F, (float) pet.getMaxHeadXRot());
            if (--recalc > 0) {
                return;
            }
            recalc = adjustedTickDelay(10);
            double d = pet.distanceToSqr(owner);
            if (d >= 12 * 12 && owner instanceof ServerPlayer sp) {
                pet.teleportNear(sp);
                return;
            }
            double speed = d > 8 * 8 ? 1.4 : 1.05;
            if (pet.flier()) {
                // a tu lado y un poco detrás, a la altura de tu cabeza (nunca delante de tu cara)
                Vec3 v = besideOwner(pet, owner, 1.7, 1.3);
                pet.getNavigation().moveTo(v.x, v.y, v.z, speed);
            } else {
                pet.getNavigation().moveTo(owner, speed);
            }
        }
    }

    /** Punto a un lado y un poco detrás del dueño (cada mascota elige su lado), a cierta altura. */
    static Vec3 besideOwner(Mascota pet, LivingEntity owner, double side, double up) {
        double yaw = Math.toRadians(owner.getYRot());
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        double s = (pet.getId() % 2 == 0 ? 1 : -1) * side;
        return owner.position().add(right.scale(s)).add(fwd.scale(-1.1)).add(0, up, 0);
    }

    // ------------------------------------------------------------------ pasear (las que andan) y flotar cerca de ti (las que vuelan)

    static final class Wander extends WaterAvoidingRandomStrollGoal {
        private final Mascota pet;

        Wander(Mascota pet) {
            super(pet, 0.8);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return !pet.flier() && !pet.isOrderedToSit() && !pet.leaving() && super.canUse();
        }
    }

    static final class Hover extends Goal {
        private final Mascota pet;
        private int wait;

        Hover(Mascota pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!pet.flier() || pet.isOrderedToSit() || pet.leaving() || pet.getTarget() != null) {
                return false;
            }
            if (--wait > 0) {
                return false;
            }
            wait = 50 + pet.getRandom().nextInt(70);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !pet.getNavigation().isDone() && !pet.isOrderedToSit();
        }

        @Override
        public void start() {
            LivingEntity o = pet.getOwner();
            boolean near = o != null && o.distanceToSqr(pet) < 12 * 12;
            Vec3 c = near ? o.position() : pet.position();
            for (int i = 0; i < 8; i++) {
                double x, z;
                if (near) {
                    // alrededor de su dueño pero fuera de lo que tiene delante de los ojos (a los lados o detrás)
                    double a = Math.toRadians(o.getYRot()) + Math.PI + (pet.getRandom().nextDouble() - 0.5) * Math.toRadians(220);
                    double r = 2.0 + pet.getRandom().nextDouble() * 1.8;
                    x = c.x - Math.sin(a) * r;
                    z = c.z + Math.cos(a) * r;
                } else {
                    x = c.x + (pet.getRandom().nextDouble() - 0.5) * 7.0;
                    z = c.z + (pet.getRandom().nextDouble() - 0.5) * 7.0;
                }
                double gy = ground(x, c.y + 2, z);
                double y = gy + 1.1 + pet.getRandom().nextDouble() * 1.1;
                Vec3 v = new Vec3(x, y, z);
                if (pet.level().noCollision(pet, pet.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(v))) {
                    pet.getNavigation().moveTo(x, y, z, 0.8);
                    return;
                }
            }
        }

        private double ground(double x, double y, double z) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
            for (int i = 0; i < 8; i++) {
                if (!pet.level().getBlockState(p).getCollisionShape(pet.level(), p).isEmpty()) {
                    return p.getY() + 1;
                }
                p.move(0, -1, 0);
            }
            return y - 2;
        }
    }

    // ------------------------------------------------------------------ pelear

    /** Salto al enemigo antes del mordisco (solo las de suelo que muerden). */
    static final class Leap extends LeapAtTargetGoal {
        private final Mascota pet;

        Leap(Mascota pet) {
            super(pet, 0.35F);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.kind() != null && pet.kind().attack == PetKinds.Attack.BITE && !pet.flier() && pet.canFight() && super.canUse();
        }
    }

    static final class Bite extends MeleeAttackGoal {
        private final Mascota pet;

        Bite(Mascota pet) {
            super(pet, 1.3, true);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.kind() != null && pet.kind().attack == PetKinds.Attack.BITE && pet.canFight() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return pet.canFight() && super.canContinueToUse();
        }
    }

    /** Las que disparan: se quedan a media distancia, miran al enemigo y lanzan su disparo cada poco. */
    static final class Bolt extends Goal {
        private final Mascota pet;
        private int seen;

        Bolt(Mascota pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = pet.getTarget();
            return pet.kind() != null && pet.kind().attack == PetKinds.Attack.BOLT && pet.canFight() && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            pet.getNavigation().stop();
            seen = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = pet.getTarget();
            if (t == null) {
                return;
            }
            double d = pet.distanceTo(t);
            boolean see = pet.getSensing().hasLineOfSight(t);
            seen = see ? seen + 1 : 0;
            pet.getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (d > 9.0 || !see) {
                if (pet.flier()) {
                    pet.getNavigation().moveTo(t.getX(), t.getY() + 2.0, t.getZ(), 1.2);
                } else {
                    pet.getNavigation().moveTo(t, 1.2);
                }
            } else if (d < 3.0 && !pet.flier()) {
                // demasiado cerca: retrocede un poco
                Vec3 away = pet.position().subtract(t.position()).normalize().scale(3.0).add(pet.position());
                pet.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            } else {
                pet.getNavigation().stop();
            }
            if (see && seen > 4 && d < 11.0 && pet.boltCd <= 0) {
                pet.boltCd = 34 + pet.getRandom().nextInt(10);
                pet.fireBolt(t);
            }
        }
    }

    /** Las que no pelean: si les pegan, corren asustadas (y luego vuelven contigo). */
    static final class Panic extends PanicGoal {
        private final Mascota pet;

        Panic(Mascota pet) {
            super(pet, 1.4);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.kind() != null && !pet.kind().combat() && !pet.flier() && !pet.isOrderedToSit() && super.canUse();
        }
    }

    static final class OwnerHurtBy extends OwnerHurtByTargetGoal {
        private final Mascota pet;

        OwnerHurtBy(Mascota pet) {
            super(pet);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.canFight() && super.canUse();
        }
    }

    static final class OwnerHurt extends OwnerHurtTargetGoal {
        private final Mascota pet;

        OwnerHurt(Mascota pet) {
            super(pet);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.canFight() && super.canUse();
        }
    }

    static final class Retaliate extends HurtByTargetGoal {
        private final Mascota pet;

        Retaliate(Mascota pet) {
            super(pet);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.canFight() && super.canUse();
        }
    }

    /** Modo «atacar»: busca monstruos cerca de su dueño (nunca creepers). */
    static final class Hunt extends NearestAttackableTargetGoal<Monster> {
        private final Mascota pet;

        Hunt(Mascota pet) {
            super(pet, Monster.class, 10, true, false, e -> !(e instanceof Creeper) && pet.getOwner() != null && e.distanceToSqr(pet.getOwner()) < 14 * 14);
            this.pet = pet;
        }

        @Override
        public boolean canUse() {
            return pet.canFight() && pet.mode() == Mascota.MODE_ATTACK && super.canUse();
        }
    }

    // ------------------------------------------------------------------ recoger lo que hay en el suelo (las que tienen «recoger»)

    static final class Fetch extends Goal {
        private final Mascota pet;
        private ItemEntity item;
        private int time;
        private final Map<UUID, Long> skip = new HashMap<>();

        Fetch(Mascota pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!pet.hasFlag(Mascota.F_FETCH) || pet.isOrderedToSit() || pet.getTarget() != null || pet.leaving() || pet.tickCount % 10 != 0) {
                return false;
            }
            LivingEntity o = pet.getOwner();
            if (o == null || o.distanceToSqr(pet) > 16 * 16) {
                return false;
            }
            long now = pet.level().getGameTime();
            skip.values().removeIf(t -> t < now);
            List<ItemEntity> list = pet.level().getEntitiesOfClass(ItemEntity.class, pet.getBoundingBox().inflate(8.0),
                    e -> e.isAlive() && !e.hasPickUpDelay() && e.getAge() > 20 && e.getOwner() == null && !skip.containsKey(e.getUUID()));
            ItemEntity best = null;
            double bd = Double.MAX_VALUE;
            for (ItemEntity e : list) {
                double d = e.distanceToSqr(pet);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            item = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive() && time > 0 && pet.hasFlag(Mascota.F_FETCH) && pet.getTarget() == null;
        }

        @Override
        public void start() {
            time = 120;
        }

        @Override
        public void stop() {
            if (item != null && item.isAlive() && time <= 0) {
                skip.put(item.getUUID(), pet.level().getGameTime() + 600); // no llega: lo deja en paz un rato
            }
            item = null;
            pet.getNavigation().stop();
        }

        @Override
        public void tick() {
            time--;
            pet.getLookControl().setLookAt(item, 30.0F, 30.0F);
            if (pet.distanceToSqr(item) < 1.6 * 1.6) {
                grab(item);
                item = null;
                return;
            }
            if (time % 10 == 0) {
                pet.getNavigation().moveTo(item.getX(), item.getY() + (pet.flier() ? 0.6 : 0), item.getZ(), 1.2);
            }
        }

        private void grab(ItemEntity e) {
            ServerPlayer o = pet.ownerPlayer();
            if (o == null) {
                return;
            }
            ItemStack st = e.getItem().copy();
            int n = st.getCount();
            pet.take(e, n); // el objeto vuela hacia la mascota
            if (!o.getInventory().add(st)) {
                o.drop(st, false);
            }
            e.discard();
            pet.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.4F, 1.4F + pet.getRandom().nextFloat() * 0.3F);
            pet.voice(0.4F, 1.25F);
        }
    }
}
