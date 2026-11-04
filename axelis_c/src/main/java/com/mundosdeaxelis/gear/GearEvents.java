package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Eventos del equipo: golpes, almas, minado en área / veta / tala, experiencia, tesoros, fundición e imán de objetos.
 * Todo sale de las capacidades ({@link Perk}) de la herramienta que lleva el jugador en la mano.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class GearEvents {
    private GearEvents() {
    }

    private static final Set<UUID> BREAKING = new HashSet<>();

    /** Dónde acaban de caer objetos de este jugador (para fundirlos o mandarlos a su inventario). */
    private static final class Rec {
        boolean smelt;
        boolean magnet;
        final Map<BlockPos, Long> pos = new HashMap<>();
    }

    private static final Map<UUID, Rec> RECS = new HashMap<>();

    private static void mark(ServerPlayer sp, BlockPos p, long tick, boolean smelt, boolean magnet) {
        Rec r = RECS.computeIfAbsent(sp.getUUID(), k -> new Rec());
        r.smelt = smelt;
        r.magnet = magnet;
        if (r.pos.size() > 3000) {
            r.pos.values().removeIf(t -> t < tick - 40);
        }
        r.pos.put(p.immutable(), tick);
    }

    // ------------------------------------------------------------------ golpes y almas

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (Gear.busy) {
            return;
        }
        if (e.getSource().getEntity() instanceof ServerPlayer sp && e.getSource().getDirectEntity() == sp
                && sp.level() instanceof ServerLevel sl) {
            if (sp.getMainHandItem().getItem() instanceof GearSword gs) {
                e.setAmount(Gear.onHit(gs.ability(), sp, sl, e.getEntity(), e.getAmount()));
            } else if (sp.getMainHandItem().getItem() instanceof LadderSword ls) {
                e.setAmount(Gear.onHitDef(ls.def(), sp, sl, e.getEntity(), e.getAmount()));
            }
        }
    }

    private static final float[] SOUL_MUL = {1.0F, 1.0F, 1.5F, 2.0F, 3.0F};

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player || dead instanceof ArmorStand) {
            return;
        }
        ItemStack st = sp.getMainHandItem();
        if (!(st.getItem() instanceof SwordItem) || !(st.getItem() instanceof GearTool gt)) {
            return;
        }
        int base = Math.max(1, Math.min(20, Math.round(dead.getMaxHealth() / 6.0F)));
        int lvl = Math.min(4, gt.lvl(Perk.SOULS));
        int gain = Math.max(1, Math.round(base * SOUL_MUL[lvl]));
        int got = Souls.add(sp, gain);
        Vec3 c = dead.getBoundingBox().getCenter();
        sl.sendParticles(Fx.spark(0x7FE8FF, 0.7F), c.x, c.y, c.z, 10, 0.3, 0.4, 0.3, 0.04);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.35F, 1.5F);
        if (got <= 0) {
            sl.sendParticles(Fx.spark(0x667788, 0.5F), c.x, c.y, c.z, 4, 0.2, 0.3, 0.2, 0.02);
        }
    }

    /** Botín de la espada: suma niveles de Botín (vale para los objetos de Minecraft y para la probabilidad de materiales). */
    @SubscribeEvent
    public static void onLooting(net.minecraftforge.event.entity.living.LootingLevelEvent e) {
        if (e.getDamageSource() == null || !(e.getDamageSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ItemStack st = sp.getMainHandItem();
        if (st.getItem() instanceof SwordItem && st.getItem() instanceof GearTool gt) {
            e.setLootingLevel(e.getLootingLevel() + gt.lvl(Perk.LOOT));
        }
    }

    private static final Map<UUID, Integer> LAST_SOUL_MAX = new HashMap<>();

    /** Si cambia la capacidad de almas (te pones o quitas armadura de set), se avisa al cliente para que el contador se ajuste. */
    @SubscribeEvent
    public static void onSoulCapacity(net.minecraftforge.event.TickEvent.PlayerTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || sp.tickCount % 20 != 0) {
            return;
        }
        int max = Souls.max(sp);
        Integer old = LAST_SOUL_MAX.put(sp.getUUID(), max);
        if (old == null || old != max) {
            Souls.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Souls.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Souls.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Souls.sync(sp);
        }
    }

    /** Las almas no se pierden al morir. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        e.getEntity().getPersistentData().putInt("mdc_souls", e.getOriginal().getPersistentData().getInt("mdc_souls"));
    }

    // ------------------------------------------------------------------ minado

    /** Troncos que rompe el hacha de cada set (puesto 1..16), contando el que se golpea: el primero rompe 2 y el último 500. */
    static final int[] FELL_LOGS = {2, 4, 7, 11, 16, 24, 35, 50, 70, 95, 125, 165, 215, 280, 370, 500};
    /** Bloques de mena conectados que rompe el pico de cada set (puesto 1..16), contando el que se golpea. */
    static final int[] VEIN_CAP = {2, 3, 4, 6, 8, 10, 13, 16, 20, 26, 33, 42, 54, 70, 92, 128};
    /** Primer puesto con minado en área: pico (puesto 4) y pala (puesto 3). Desde ahí crece en cada set. */
    static final int PICK_AREA_FROM = 4;
    static final int SHOVEL_AREA_FROM = 3;
    /** Radio y profundidad del área (3x3x1 = radio 1, profundidad 1) para el nivel 1, 2, 3... */
    static final int[] AREA_R = {1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5};
    static final int[] AREA_D = {1, 2, 3, 2, 3, 4, 3, 4, 5, 4, 5, 6, 5, 6};

    static int fellLogs(int level) {
        return FELL_LOGS[Math.max(0, Math.min(FELL_LOGS.length, level) - 1)];
    }

    static int veinCap(int level) {
        return VEIN_CAP[Math.max(0, Math.min(VEIN_CAP.length, level) - 1)];
    }

    static int areaR(int level) {
        return AREA_R[Math.max(0, Math.min(AREA_R.length, level) - 1)];
    }

    static int areaD(int level) {
        return AREA_D[Math.max(0, Math.min(AREA_D.length, level) - 1)];
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        ItemStack st = sp.getMainHandItem();
        if (!(st.getItem() instanceof GearTool gt)) {
            return;
        }
        Map<Perk, Integer> pk = gt.perks();
        if (pk.isEmpty()) {
            return;
        }
        BlockPos pos = e.getPos();
        BlockState state = e.getState();
        long now = sl.getGameTime();
        boolean smelt = pk.containsKey(Perk.SMELT), magnet = pk.containsKey(Perk.MAGNET);
        if (smelt || magnet) {
            mark(sp, pos, now, smelt, magnet);
        }
        // experiencia extra en menas (también en las que rompe la veta o el área)
        int xp = pk.getOrDefault(Perk.XP, 0);
        if (xp > 0 && state.is(Tags.Blocks.ORES)) {
            int extra = switch (Math.min(3, xp)) {
                case 1 -> 2 + sl.random.nextInt(3);
                case 2 -> 4 + sl.random.nextInt(5);
                default -> 8 + sl.random.nextInt(7);
            };
            e.setExpToDrop(e.getExpToDrop() + extra);
            RelicFx.burst(sl, Fx.spark(0xFFD966, 0.8F), Vec3.atCenterOf(pos), 8, 0.25);
            sl.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45F, 1.4F);
        }
        // tesoros al excavar
        int tr = pk.getOrDefault(Perk.TREASURE, 0);
        if (tr > 0 && state.is(BlockTags.MINEABLE_WITH_SHOVEL) && sl.random.nextFloat() < 0.04F + 0.04F * tr) {
            treasure(sl, pos, tr, sp, now, smelt, magnet);
        }
        if (BREAKING.contains(sp.getUUID()) || sp.isShiftKeyDown()) {
            return;
        }
        int fell = pk.getOrDefault(Perk.FELL, 0);
        if (fell > 0 && state.is(BlockTags.LOGS)) {
            fell(sp, sl, st, pos, fell, pk.containsKey(Perk.LEAVES), pk.containsKey(Perk.REPLANT), smelt, magnet);
            return;
        }
        int vein = pk.getOrDefault(Perk.VEIN, 0);
        if (vein > 0 && state.is(Tags.Blocks.ORES)) {
            vein(sp, sl, st, pos, state, veinCap(vein) - 1);
            return;
        }
        int area = pk.getOrDefault(Perk.AREA, 0);
        if (area > 0) {
            if (st.getItem() instanceof PickaxeItem && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                area(sp, sl, st, pos, BlockTags.MINEABLE_WITH_PICKAXE, area);
            } else if (st.getItem() instanceof ShovelItem && state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                area(sp, sl, st, pos, BlockTags.MINEABLE_WITH_SHOVEL, area);
            }
        }
    }

    private static final java.util.function.Supplier<List<net.minecraft.world.item.Item>> T1 = () -> List.of(Items.BONE, Items.FLINT,
            Items.CLAY_BALL, Items.COAL, Items.STICK, Items.BONE_MEAL);
    private static final java.util.function.Supplier<List<net.minecraft.world.item.Item>> T2 = () -> List.of(Items.BONE, Items.FLINT,
            Items.CLAY_BALL, Items.COAL, Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.RAW_COPPER, Items.LAPIS_LAZULI);
    private static final java.util.function.Supplier<List<net.minecraft.world.item.Item>> T3 = () -> List.of(Items.IRON_NUGGET,
            Items.GOLD_NUGGET, Items.LAPIS_LAZULI, Items.EMERALD, Items.AMETHYST_SHARD, Items.GOLD_INGOT, Items.RAW_IRON, Items.RAW_GOLD);

    private static void treasure(ServerLevel sl, BlockPos pos, int lvl, ServerPlayer sp, long now, boolean smelt, boolean magnet) {
        List<net.minecraft.world.item.Item> t = lvl <= 1 ? T1.get() : lvl == 2 ? T2.get() : T3.get();
        ItemStack out = new ItemStack(t.get(sl.random.nextInt(t.size())));
        mark(sp, pos, now, smelt, magnet);
        Block.popResource(sl, pos, out);
        RelicFx.burst(sl, Fx.spark(0xFFE070, 0.8F), Vec3.atCenterOf(pos), 10, 0.25);
        sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private static boolean usable(ServerPlayer sp, ItemStack st) {
        return sp.isAlive() && !st.isEmpty() && sp.getMainHandItem() == st;
    }

    /** Rompe una lista de bloques (en un solo tick o repartida si es muy grande). Se gasta solo la mitad de durabilidad extra. */
    private static void breakAll(ServerPlayer sp, ServerLevel sl, ItemStack st, List<BlockPos> list, int perTick) {
        if (list.size() <= perTick) {
            breakNow(sp, st, list);
            return;
        }
        for (int i = 0; i < list.size(); i += perTick) {
            final List<BlockPos> sub = new ArrayList<>(list.subList(i, Math.min(list.size(), i + perTick)));
            Scheduler.runLater(i / perTick, () -> breakNow(sp, st, sub));
        }
    }

    private static void breakNow(ServerPlayer sp, ItemStack st, List<BlockPos> list) {
        if (!usable(sp, st)) {
            return;
        }
        int before = st.getDamageValue();
        BREAKING.add(sp.getUUID());
        try {
            for (BlockPos p : list) {
                if (!usable(sp, st)) {
                    break;
                }
                sp.gameMode.destroyBlock(p);
            }
        } finally {
            BREAKING.remove(sp.getUUID());
        }
        if (!st.isEmpty() && st.isDamageableItem()) {
            int used = st.getDamageValue() - before;
            if (used > 1) {
                st.setDamageValue(before + 1 + used / 2);
            }
        }
    }

    private static void area(ServerPlayer sp, ServerLevel sl, ItemStack st, BlockPos pos, net.minecraft.tags.TagKey<Block> tag, int level) {
        Direction face = null;
        HitResult hr = sp.pick(6.0, 1.0F, false);
        if (hr instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos)) {
            face = bhr.getDirection();
        }
        if (face == null) {
            face = Direction.getNearest(sp.getLookAngle().x, sp.getLookAngle().y, sp.getLookAngle().z);
        }
        int r = areaR(level);
        int depth = areaD(level);
        Direction.Axis ax = face.getAxis();
        List<BlockPos> list = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            BlockPos base = pos.relative(face.getOpposite(), d);
            for (int i = -r; i <= r; i++) {
                for (int j = -r; j <= r; j++) {
                    if (d == 0 && i == 0 && j == 0) {
                        continue;
                    }
                    BlockPos p = switch (ax) {
                        case Y -> base.offset(i, 0, j);
                        case X -> base.offset(0, i, j);
                        default -> base.offset(i, j, 0);
                    };
                    BlockState s = sl.getBlockState(p);
                    float h = s.getDestroySpeed(sl, p);
                    if (!s.isAir() && h >= 0 && h <= 50 && s.is(tag) && !s.hasBlockEntity() && st.isCorrectToolForDrops(s)) {
                        list.add(p);
                    }
                }
            }
        }
        breakAll(sp, sl, st, list, list.size() > 120 ? 60 : 30);
    }

    private static void vein(ServerPlayer sp, ServerLevel sl, ItemStack st, BlockPos origin, BlockState state, int cap) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        seen.add(origin);
        q.add(origin);
        outer:
        while (!q.isEmpty()) {
            BlockPos c = q.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = c.offset(dx, dy, dz);
                        if (!seen.add(n)) {
                            continue;
                        }
                        if (sl.getBlockState(n).is(state.getBlock())) {
                            found.add(n);
                            q.add(n);
                            if (found.size() >= cap) {
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        for (BlockPos p : found) {
            if (sl.random.nextInt(3) == 0) {
                RelicFx.burst(sl, Fx.spark(0xC060FF, 0.6F), Vec3.atCenterOf(p), 2, 0.1);
            }
        }
        breakAll(sp, sl, st, found, 24);
    }

    // ------------------------------------------------------------------ tala

    private static void fell(ServerPlayer sp, ServerLevel sl, ItemStack st, BlockPos origin, int level, boolean leavesPerk,
                             boolean replant, boolean smelt, boolean magnet) {
        // el tope cuenta el tronco golpeado (que rompe el propio jugador), así que aquí se restan; así el primer hacha tira 2 en total
        int cap = fellLogs(level) - 1;
        // se explora un poco más de lo que se va a romper para saber si de verdad es un árbol (aunque sea el hacha más chica)
        int scan = Math.max(cap, 60);
        // se baja hasta la base del tronco para talar el árbol entero aunque se golpee por arriba
        BlockPos base = origin;
        while (sl.getBlockState(base.below()).is(BlockTags.LOGS) && origin.getY() - base.getY() < 40) {
            base = base.below();
        }
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        seen.add(base);
        q.add(base);
        if (!base.equals(origin)) {
            logs.add(base);
        }
        boolean leaves = false;
        int minY = base.getY();
        while (!q.isEmpty() && logs.size() < scan) {
            BlockPos c = q.poll();
            for (int dx = -1; dx <= 1 && logs.size() < scan; dx++) {
                for (int dy = -1; dy <= 1 && logs.size() < scan; dy++) {
                    for (int dz = -1; dz <= 1 && logs.size() < scan; dz++) {
                        BlockPos n = c.offset(dx, dy, dz);
                        if (n.getY() < minY || !seen.add(n)) {
                            continue;
                        }
                        if (sl.getBlockState(n).is(BlockTags.LOGS)) {
                            if (!n.equals(origin)) {
                                logs.add(n);
                            }
                            q.add(n);
                        }
                    }
                }
            }
            if (!leaves) {
                leaves = hasLeavesNear(sl, c);
            }
        }
        // solo se tala si de verdad es un árbol (tiene hojas cerca); así no desarma casas de madera
        if (!leaves) {
            return;
        }
        if (logs.size() > cap) {
            logs = new ArrayList<>(logs.subList(0, cap));
        }
        // el sentido del derribo: de arriba hacia abajo
        logs.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        int per = cap > 64 ? 40 : 24;
        long now = sl.getGameTime();
        for (BlockPos p : logs) {
            mark(sp, p, now, smelt, magnet);
        }
        breakAll(sp, sl, st, logs, per);
        int batches = (logs.size() + per - 1) / per;
        // el tronco que ya estaba golpeado se rompe aparte; las hojas y el retoño van al final
        if (leavesPerk) {
            List<BlockPos> all = new ArrayList<>(logs);
            all.add(origin);
            List<BlockPos> lv = collectLeaves(sl, all, 900);
            final int delay = batches + 2;
            for (int i = 0; i < lv.size(); i += 60) {
                final List<BlockPos> sub = new ArrayList<>(lv.subList(i, Math.min(lv.size(), i + 60)));
                Scheduler.runLater(delay + i / 60, () -> {
                    if (!sp.isAlive()) {
                        return;
                    }
                    long t = sl.getGameTime();
                    for (BlockPos p : sub) {
                        if (sl.getBlockState(p).is(BlockTags.LEAVES)) {
                            mark(sp, p, t, smelt, magnet);
                            sl.destroyBlock(p, true, sp);
                        }
                    }
                });
            }
        }
        if (replant) {
            final BlockPos b = base;
            final BlockState logState = sl.getBlockState(origin);
            Scheduler.runLater(batches + 4 + (leavesPerk ? 14 : 0), () -> replant(sl, b, logState));
        }
    }

    private static boolean hasLeavesNear(ServerLevel sl, BlockPos c) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (sl.getBlockState(m).is(BlockTags.LEAVES)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Las hojas que sostienen estos troncos (siguiendo la cadena de "distancia" de las hojas). */
    private static List<BlockPos> collectLeaves(ServerLevel sl, List<BlockPos> logs, int cap) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Map<BlockPos, Integer> dist = new HashMap<>();
        for (BlockPos l : logs) {
            for (Direction d : Direction.values()) {
                BlockPos n = l.relative(d);
                if (seen.add(n) && sl.getBlockState(n).is(BlockTags.LEAVES)) {
                    out.add(n);
                    dist.put(n, 1);
                    q.add(n);
                }
            }
        }
        while (!q.isEmpty() && out.size() < cap) {
            BlockPos c = q.poll();
            int dc = dist.get(c);
            if (dc >= 7) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (!seen.add(n)) {
                    continue;
                }
                BlockState s = sl.getBlockState(n);
                if (s.is(BlockTags.LEAVES) && out.size() < cap) {
                    out.add(n);
                    dist.put(n, dc + 1);
                    q.add(n);
                }
            }
        }
        return out;
    }

    private static void replant(ServerLevel sl, BlockPos base, BlockState logState) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(logState.getBlock());
        if (key == null || !sl.getBlockState(base).isAir() || !sl.getBlockState(base.below()).is(BlockTags.DIRT)) {
            return;
        }
        String n = key.getPath();
        if (n.startsWith("stripped_")) {
            n = n.substring(9);
        }
        for (String suf : new String[]{"_log", "_wood", "_stem", "_hyphae"}) {
            if (n.endsWith(suf)) {
                n = n.substring(0, n.length() - suf.length());
                break;
            }
        }
        ResourceLocation sap = new ResourceLocation(key.getNamespace(), n + "_sapling");
        if (!ForgeRegistries.BLOCKS.containsKey(sap)) {
            return;
        }
        Block b = ForgeRegistries.BLOCKS.getValue(sap);
        if (b != null && b.defaultBlockState().canSurvive(sl, base)) {
            sl.setBlock(base, b.defaultBlockState(), 3);
            sl.sendParticles(Fx.spark(0x7BD66B, 0.7F), base.getX() + 0.5, base.getY() + 0.4, base.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.02);
        }
    }

    // ------------------------------------------------------------------ fundición e imán de objetos

    private static ItemStack smeltOf(ServerLevel sl, ItemStack in) {
        if (!(in.is(Tags.Items.RAW_MATERIALS) || in.is(Tags.Items.ORES) || in.is(ItemTags.LOGS_THAT_BURN)
                || in.is(Tags.Items.SAND) || in.is(Items.CLAY_BALL))) {
            return ItemStack.EMPTY;
        }
        Optional<SmeltingRecipe> rec = sl.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(in), sl);
        if (rec.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = rec.get().getResultItem(sl.registryAccess()).copy();
        out.setCount(Math.max(1, out.getCount()) * in.getCount());
        return out;
    }

    @SubscribeEvent
    public static void onItemJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof ItemEntity ie) || !(e.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        if (RECS.isEmpty()) {
            return;
        }
        long now = sl.getGameTime();
        BlockPos bp = ie.blockPosition();
        for (Map.Entry<UUID, Rec> en : RECS.entrySet()) {
            Rec r = en.getValue();
            Long t = r.pos.get(bp);
            if (t == null || Math.abs(now - t) > 2) {
                continue;
            }
            if (r.smelt) {
                ItemStack out = smeltOf(sl, ie.getItem());
                if (!out.isEmpty()) {
                    ie.setItem(out);
                    Vec3 c = ie.position();
                    sl.sendParticles(Fx.ember(0xFF9A3C, 0.7F), c.x, c.y + 0.2, c.z, 4, 0.15, 0.1, 0.15, 0.02);
                }
            }
            if (r.magnet) {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(en.getKey());
                if (sp != null && sp.level() == sl && sp.distanceToSqr(ie) < 48 * 48) {
                    ItemStack copy = ie.getItem().copy();
                    sp.getInventory().add(copy);
                    if (copy.isEmpty()) {
                        e.setCanceled(true);
                        if (sp.tickCount % 3 == 0) {
                            sl.playSound(null, sp.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.15F, 1.6F);
                        }
                    } else {
                        ie.setItem(copy);
                    }
                }
            }
            return;
        }
    }
}
