package com.casino.entity;

import com.casino.decor.SeatBlock;
import com.casino.decor.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Lo que hacen los clientes del casino (crupieres con GAME_PATRON): pasean cerca de donde aparecieron, se sientan un rato en sillas,
 * taburetes y sofás, se paran a ver una mesa (y aplauden o festejan de vez en cuando), toman de su copa y miran a quien se acerca.
 * Nunca se alejan más de {@link #RANGE} bloques de su lugar, así no se salen del casino.
 */
public final class PatronAI {
    static final int RANGE = 11;
    private static final double SPEED = 0.38;

    private PatronAI() {
    }

    static void register(DealerEntity e, GoalSelector goals) {
        goals.addGoal(0, new FloatGoal(e));
        goals.addGoal(1, new Sit(e));
        goals.addGoal(2, new Watch(e));
        goals.addGoal(3, new Stroll(e));
        goals.addGoal(4, new LookAtPlayerGoal(e, Player.class, 6.0F));
        goals.addGoal(5, new RandomLookAroundGoal(e));
    }

    private static boolean standable(Level level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty() && level.getFluidState(p).isEmpty();
    }

    /** Un lugar donde pararse cerca de (x, z), buscando el piso de arriba hacia abajo. */
    private static BlockPos ground(Level level, double x, double z, int y0) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos p = BlockPos.containing(x, y0 + dy, z);
            if (standable(level, p)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Caminar hasta un punto. Va escrito ". moveTo" con un espacio a propósito: el preprocesador cambia ".moveTo(" por ".snapTo(" en 26.x
     * (eso es para teletransportar entidades) y aquí es el camino del navegador, que en 26.x sigue llamándose moveTo.
     */
    private static boolean walk(DealerEntity e, double x, double y, double z) {
        return e.getNavigation(). moveTo(x, y, z, SPEED);
    }

    private static boolean nearHome(DealerEntity e, BlockPos p) {
        BlockPos h = e.home();
        return h == null || p.distSqr(h) <= RANGE * RANGE && Math.abs(p.getY() - h.getY()) <= 3;
    }

    // ------------------------------------------------------------------ pasear

    static final class Stroll extends Goal {
        private final DealerEntity e;

        Stroll(DealerEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!e.isPatron() || e.isPassenger() || e.getRandom().nextInt(140) != 0) {
                return false;
            }
            BlockPos h = e.home() != null ? e.home() : e.blockPosition();
            for (int i = 0; i < 10; i++) {
                double x = h.getX() + 0.5 + (e.getRandom().nextDouble() * 2 - 1) * (RANGE - 2);
                double z = h.getZ() + 0.5 + (e.getRandom().nextDouble() * 2 - 1) * (RANGE - 2);
                BlockPos p = ground(e.level(), x, z, h.getY());
                if (p != null && nearHome(e, p) && walk(e, p.getX() + 0.5, p.getY(), p.getZ() + 0.5)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return !e.getNavigation().isDone() && !e.isPassenger();
        }

        @Override
        public void stop() {
            e.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ sentarse

    static final class Sit extends Goal {
        private final DealerEntity e;
        private BlockPos seat;
        private int left;
        private int walk;

        Sit(DealerEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!e.isPatron() || e.isPassenger() || e.getRandom().nextInt(320) != 0) {
                return false;
            }
            Level level = e.level();
            BlockPos c = e.blockPosition();
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-7, -2, -7), c.offset(7, 2, 7))) {
                BlockState st = level.getBlockState(p);
                if (st.getBlock() instanceof SeatBlock && nearHome(e, p)
                        && level.getEntitiesOfClass(SeatEntity.class, new AABB(p).inflate(0.2)).isEmpty()
                        && level.getEntitiesOfClass(Player.class, new AABB(p).inflate(1.0)).isEmpty()) {
                    found.add(p.immutable());
                }
            }
            if (found.isEmpty()) {
                return false;
            }
            seat = found.get(e.getRandom().nextInt(found.size()));
            walk = 200;
            left = 0;
            return walk(e, seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
        }

        @Override
        public boolean canContinueToUse() {
            return seat != null && (e.isPassenger() ? left > 0 : walk > 0);
        }

        @Override
        public void tick() {
            if (seat == null) {
                return;
            }
            if (e.isPassenger()) {
                left--;
                return;
            }
            walk--;
            Vec3 c = Vec3.atBottomCenterOf(seat);
            if (e.position().distanceToSqr(c.x, e.getY(), c.z) < 1.6 * 1.6) {
                BlockState st = e.level().getBlockState(seat);
                if (st.getBlock() instanceof SeatBlock s && SeatEntity.sit(e.level(), seat, s.seatHeight(), s.front(st), e)) {
                    e.getNavigation().stop();
                    left = 600 + e.getRandom().nextInt(1400);
                } else {
                    seat = null;
                }
            } else if (e.getNavigation().isDone()) {
                walk(e, c.x, c.y, c.z);
            }
        }

        @Override
        public void stop() {
            if (e.isPassenger()) {
                e.stopRiding();
            }
            seat = null;
            e.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ ver una mesa

    static final class Watch extends Goal {
        private final DealerEntity e;
        private Vec3 look;
        private int left;

        Watch(DealerEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private static boolean isGame(BlockEntity be) {
            return be instanceof com.casino.block.BlackjackBE || be instanceof com.casino.block.PokerBE || be instanceof com.casino.block.RouletteBE
                    || be instanceof com.casino.block.LuckyWheelBE || be instanceof com.casino.block.PlinkoBE || be instanceof com.casino.block.SlotBE;
        }

        @Override
        public boolean canUse() {
            if (!e.isPatron() || e.isPassenger() || e.getRandom().nextInt(260) != 0) {
                return false;
            }
            Level level = e.level();
            BlockPos c = e.blockPosition();
            List<BlockPos> games = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-10, -2, -10), c.offset(10, 2, 10))) {
                if (isGame(level.getBlockEntity(p))) {
                    games.add(p.immutable());
                }
            }
            if (games.isEmpty()) {
                return false;
            }
            BlockPos g = games.get(e.getRandom().nextInt(games.size()));
            // se para a unos 3 bloques del juego, del lado del público
            for (int i = 0; i < 8; i++) {
                double a = e.getRandom().nextDouble() * Math.PI * 2;
                double r = 2.6 + e.getRandom().nextDouble();
                BlockPos p = ground(level, g.getX() + 0.5 + Math.cos(a) * r, g.getZ() + 0.5 + Math.sin(a) * r, g.getY());
                if (p != null && nearHome(e, p) && walk(e, p.getX() + 0.5, p.getY(), p.getZ() + 0.5)) {
                    look = Vec3.atCenterOf(g).add(0, 0.4, 0);
                    left = 300 + e.getRandom().nextInt(500);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return look != null && left > 0 && !e.isPassenger();
        }

        @Override
        public void tick() {
            if (!e.getNavigation().isDone()) {
                return;
            }
            left--;
            e.getLookControl().setLookAt(look.x, look.y, look.z);
            // de vez en cuando, emoción
            if (!e.animBusy() && e.getRandom().nextInt(220) == 0) {
                int r = e.getRandom().nextInt(4);
                e.anim(r == 0 ? DealerAnim.CHEER : r == 1 ? DealerAnim.CLAP : r == 2 ? DealerAnim.NOD : DealerAnim.SIP);
            }
        }

        @Override
        public void stop() {
            look = null;
            e.getNavigation().stop();
        }
    }
}
