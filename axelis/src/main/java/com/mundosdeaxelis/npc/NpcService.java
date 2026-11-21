package com.mundosdeaxelis.npc;

import javax.annotation.Nullable;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Habitante;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.NpcOpenPacket;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import com.mundosdeaxelis.world.NaturalGen;
import com.mundosdeaxelis.world.StructBp;
import com.mundosdeaxelis.world.StructDesigns;
import com.mundosdeaxelis.world.StructPlacer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lo que pasa en el servidor con los habitantes (etapa 257): hablarles, rescatarlos, darles casa, la tienda, las pistas y mudarse.
 * El cliente solo pide cosas ({@link com.mundosdeaxelis.network.NpcActionPacket}); aquí se revisa todo (distancia, estado,
 * Esencia, vecinos) y se responde con {@link NpcOpenPacket}, que abre o actualiza la ventana de diálogo.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class NpcService {
    private NpcService() {
    }

    /** Marca del enemigo que lleva la llave de la celda (ver {@code DesignSantuario}). */
    public static final String KEY_TAG = "llave_celda";

    // acciones del cliente
    public static final int A_TALK = 0, A_BUY = 1, A_HINT = 2, A_MOVE = 3, A_KIT = 4, A_FORCE = 5, A_CLOSE = 6;
    // respuestas
    public static final int R_OPEN = 0, R_RESCUED = 1, R_BOUGHT = 2, R_NO_ESSENCE = 3, R_LOCKED = 4, R_KIT_GIVEN = 5, R_HAS_KIT = 6,
            R_MOVED = 7, R_HINT = 8, R_NEED_IRON = 9, R_NO_HOUSE = 10, R_NONE_FOUND = 11, R_FORCED = 12;

    /** Lingotes de hierro para forzar la cerradura si la llave se perdió. */
    public static final int FORCE_IRON = 4;

    // ================================================================== hablar

    public static void interact(ServerPlayer sp, Habitante h) {
        if (h.kind() == null) {
            return;
        }
        h.talkWith(sp);
        if (h.state() == Habitante.PRESO && sp.getMainHandItem().is(ModItems.LLAVE_CELDA.get())) {
            if (!sp.getAbilities().instabuild) {
                sp.getMainHandItem().shrink(1);
            }
            free(sp, h, null, R_RESCUED);
            return;
        }
        Essence.sync(sp);
        open(sp, h, R_OPEN, null, 0);
    }

    public static void open(ServerPlayer sp, Habitante h, int result, Component msg, int extra) {
        ServerLevel l = (ServerLevel) h.level();
        boolean guardGone = h.state() == Habitante.PRESO && guardGone(l, h);
        ModNetwork.sendTo(sp, new NpcOpenPacket(h.getId(), h.kindId(), h.state(), Vecinos.residents(l.getServer()), guardGone, result, msg, extra));
    }

    /** ¿Ya no está el guardián con la llave (ni la llave tirada cerca)? Entonces se puede forzar la cerradura. */
    static boolean guardGone(ServerLevel l, Habitante h) {
        AABB box = h.getBoundingBox().inflate(48);
        boolean guard = !l.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && KEY_TAG.equals(m.getPersistentData().getString(StructPlacer.TAG))).isEmpty();
        boolean keyNear = !l.getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(ModItems.LLAVE_CELDA.get())).isEmpty();
        return !guard && !keyNear;
    }

    /** Lo que pide el menú (ya revisado que el jugador está cerca). */
    public static void action(ServerPlayer sp, int entityId, int action, int arg) {
        Entity e = sp.level().getEntity(entityId);
        if (!(e instanceof Habitante h) || !h.isAlive() || h.kind() == null || sp.distanceToSqr(h) > 10 * 10) {
            return;
        }
        if (action == A_CLOSE) {
            return;
        }
        h.talkWith(sp);
        ServerLevel l = (ServerLevel) h.level();
        switch (action) {
            case A_BUY -> buy(sp, h, arg);
            case A_HINT -> hint(sp, h, arg);
            case A_MOVE -> moveOut(sp, h);
            case A_KIT -> {
                if (h.state() != Habitante.LIBRE) {
                    open(sp, h, R_OPEN, null, 0);
                } else if (hasKit(sp, h.kindId())) {
                    open(sp, h, R_HAS_KIT, null, 0);
                } else {
                    give(sp, kit(h.kindId()));
                    l.playSound(null, h.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.7F, 1.1F);
                    open(sp, h, R_KIT_GIVEN, null, 0);
                }
            }
            case A_FORCE -> {
                if (h.state() != Habitante.PRESO || !guardGone(l, h)) {
                    open(sp, h, R_OPEN, null, 0);
                    return;
                }
                if (!sp.getAbilities().instabuild && count(sp, Items.IRON_INGOT) < FORCE_IRON) {
                    open(sp, h, R_NEED_IRON, null, FORCE_IRON);
                    return;
                }
                if (!sp.getAbilities().instabuild) {
                    take(sp, Items.IRON_INGOT, FORCE_IRON);
                }
                free(sp, h, null, R_FORCED);
            }
            default -> {
            }
        }
    }

    // ================================================================== rescate

    /** Saca al habitante de la celda: abre la puerta, sale a esperar fuera y te da la semilla de su casa ({@code sp} puede ser null: comando). */
    public static void free(ServerPlayer sp, Habitante h, BlockPos door, int result) {
        ServerLevel l = (ServerLevel) h.level();
        if (door == null) {
            door = findDoor(l, h.blockPosition(), 6);
        }
        BlockPos spot = h.blockPosition();
        if (door != null) {
            BlockState ds = l.getBlockState(door);
            if (ds.getBlock() instanceof DoorBlock db) {
                db.setOpen(sp != null ? sp : h, l, ds, door, true);
            }
            BlockPos out = outside(l, door, h.blockPosition());
            spot = out != null ? out : door;
        }
        h.setState(Habitante.LIBRE, spot);
        Vecinos.set(l.getServer(), h.kindId(), Habitante.LIBRE, h.getUUID(), l.dimension(), spot, null, Rotation.NONE);
        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, h.getX(), h.getY() + 1.0, h.getZ(), 24, 0.5, 0.6, 0.5, 0.0);
        l.playSound(null, h.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2F, 1.0F);
        MundosDeAxelis.LOGGER.info("[Vecinos] {} rescatada por {}", h.kindId(), sp == null ? "(comando)" : sp.getName().getString());
        if (sp != null) {
            give(sp, kit(h.kindId()));
            l.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            open(sp, h, result, null, 0);
        }
    }

    /**
     * Un sitio libre al otro lado de la puerta (lejos del preso): se cruza la puerta en la dirección a la que mira, y se busca a
     * 3 bloques (o 4, 2, 5...) un hueco con suelo firme y dos bloques de aire.
     */
    static BlockPos outside(Level l, BlockPos door, BlockPos prisoner) {
        BlockState ds = l.getBlockState(door);
        Direction f = ds.getBlock() instanceof DoorBlock ? ds.getValue(DoorBlock.FACING) : Direction.NORTH;
        Direction.Axis ax = f.getAxis();
        int d = ax == Direction.Axis.X ? door.getX() - prisoner.getX() : door.getZ() - prisoner.getZ();
        Direction outDir = d == 0 ? f : Direction.fromAxisAndDirection(ax, d > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        Direction side = outDir.getClockWise();
        for (int dist : new int[]{3, 4, 2, 5, 1}) {
            for (int lat : new int[]{0, 1, -1, 2, -2}) {
                BlockPos p = door.relative(outDir, dist).relative(side, lat);
                if (standable(l, p)) {
                    return p;
                }
            }
        }
        return null;
    }

    private static boolean standable(Level l, BlockPos p) {
        return l.getBlockState(p).getCollisionShape(l, p).isEmpty() && l.getBlockState(p.above()).getCollisionShape(l, p.above()).isEmpty()
                && !l.getBlockState(p.below()).getCollisionShape(l, p.below()).isEmpty() && l.getFluidState(p).isEmpty();
    }

    static BlockPos findDoor(Level l, BlockPos c, int r) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) {
            BlockState s = l.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                double d = p.distSqr(c);
                if (d < bd) {
                    bd = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** El preso más cercano (para la llave usada en la puerta). */
    public static Habitante prisonerNear(ServerLevel l, BlockPos p, double r) {
        Habitante best = null;
        double bd = Double.MAX_VALUE;
        for (Habitante h : l.getEntitiesOfClass(Habitante.class, new AABB(p).inflate(r), x -> x.isAlive() && x.state() == Habitante.PRESO)) {
            double d = h.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bd) {
                bd = d;
                best = h;
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (!v.level().isClientSide && KEY_TAG.equals(v.getPersistentData().getString(StructPlacer.TAG))) {
            ItemEntity it = new ItemEntity(v.level(), v.getX(), v.getY() + 0.5, v.getZ(), new ItemStack(ModItems.LLAVE_CELDA.get()));
            it.setGlowingTag(true);
            e.getDrops().add(it);
        }
    }

    // ================================================================== casa

    public static ItemStack kit(String kind) {
        ItemStack s = new ItemStack(ModItems.SEMILLA_CASA.get());
        s.getOrCreateTag().putString("Npc", kind);
        return s;
    }

    static boolean hasKit(Player p, String kind) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(ModItems.SEMILLA_CASA.get()) && kind.equals(KitKind.of(s))) {
                return true;
            }
        }
        return false;
    }

    /** Lado al que mira el jugador -> giro del plano para que la casa crezca delante y la puerta quede mirándolo. */
    public static Rotation rotFor(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * Planta la semilla: revisa el sitio (despejado, sin cofres ni nadie en medio, terreno no muy desigual), construye la casa
     * y pone dentro al habitante como vecino. Devuelve la clave del mensaje de error, o null si salió bien.
     */
    public static String plantHouse(ServerLevel l, ServerPlayer sp, String kind, BlockPos origin, Direction facing) {
        return plantHouse(l, sp, kind, origin, facing, null);
    }

    /** Bloques que estorbaban en la última revisión del sitio (para el aviso «N bloques en medio»). Solo en el hilo del servidor. */
    static int lastBlocked;

    /**
     * Igual, pero sin contar {@code seed} (la Semilla de casa puesta en el suelo, que marca dónde va la casa): si todo está bien la
     * quita justo antes de construir.
     */
    public static String plantHouse(ServerLevel l, ServerPlayer sp, String kind, BlockPos origin, Direction facing, @Nullable BlockPos seed) {
        lastBlocked = 0;
        NpcKinds.Kind k = NpcKinds.get(kind);
        if (k == null) {
            return "message.mundos_de_axelis.casa.unknown";
        }
        if (l.dimension().location().getNamespace().equals(MundosDeAxelis.MODID)) {
            return "message.mundos_de_axelis.casa.dimension";
        }
        int st = Vecinos.state(l.getServer(), kind);
        if (st == Habitante.PRESO) {
            return "message.mundos_de_axelis.casa.preso";
        }
        if (st == Habitante.VECINO) {
            return "message.mundos_de_axelis.casa.ya_tiene";
        }
        Rotation rot = rotFor(facing);
        StructBp bp = StructDesigns.make(k.house());
        String err = checkSite(l, sp, bp, origin, rot, seed);
        if (err != null) {
            return err;
        }
        if (seed != null) {
            l.removeBlock(seed, false); // la semilla se convierte en la casa (no suelta nada)
        }
        new StructPlacer(l, bp, origin, rot, null).runAll();
        BlockPos spot = StructPlacer.world(origin, rot, 0, 0, 4);
        Habitante old = Vecinos.find(l.getServer(), kind);
        if (old != null) {
            poof((ServerLevel) old.level(), old.position(), false);
            old.discard();
        }
        Habitante h = Habitante.create(l, kind, Habitante.VECINO, spot);
        Vecinos.set(l.getServer(), kind, Habitante.VECINO, h == null ? null : h.getUUID(), l.dimension(), spot, origin, rot);
        poof(l, Vec3.atBottomCenterOf(spot), true);
        l.playSound(null, spot, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        l.playSound(null, spot, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        MundosDeAxelis.LOGGER.info("[Vecinos] casa de {} en {} {} {}", kind, origin.getX(), origin.getY(), origin.getZ());
        return null;
    }

    static String checkSite(ServerLevel l, ServerPlayer sp, StructBp bp, BlockPos origin, Rotation rot, @Nullable BlockPos seed) {
        int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
        int blocked = 0;
        lastBlocked = 0;
        for (Map.Entry<Long, String> e : bp.m.entrySet()) {
            long key = e.getKey();
            int y = StructBp.ky(key);
            BlockPos w = StructPlacer.world(origin, rot, StructBp.kx(key), y, StructBp.kz(key));
            if (w.equals(seed)) {
                continue;
            }
            if (w.getY() < l.getMinBuildHeight() || w.getY() >= l.getMaxBuildHeight() - 1) {
                return "message.mundos_de_axelis.casa.altura";
            }
            if (sp != null && (!sp.mayUseItemAt(w, Direction.UP, ItemStack.EMPTY) || !l.mayInteract(sp, w))) {
                return "message.mundos_de_axelis.casa.protegido";
            }
            BlockState cur = l.getBlockState(w);
            if (l.getBlockEntity(w) != null && !cur.isAir()) {
                blocked++;
            } else if (y >= 0 && !(cur.isAir() || (cur.canBeReplaced() && cur.getFluidState().isEmpty()))) {
                blocked++;
            } else if (y < 0 && !cur.getFluidState().isEmpty()) {
                blocked++;
            }
            minX = Math.min(minX, w.getX());
            minY = Math.min(minY, w.getY());
            minZ = Math.min(minZ, w.getZ());
            maxX = Math.max(maxX, w.getX());
            maxY = Math.max(maxY, w.getY());
            maxZ = Math.max(maxZ, w.getZ());
        }
        if (blocked > 0) {
            lastBlocked = blocked;
            return "message.mundos_de_axelis.casa.ocupado";
        }
        // terreno: bajo el suelo de la casa no puede haber un hueco de más de 3 bloques
        for (StructBp.Found f : bp.found) {
            for (int x = f.x0(); x <= f.x1(); x++) {
                for (int z = f.z0(); z <= f.z1(); z++) {
                    BlockPos c = StructPlacer.world(origin, rot, x, -2, z);
                    int gap = 0;
                    while (gap <= 3 && isSoft(l.getBlockState(c.below(gap)))) {
                        gap++;
                    }
                    if (gap > 3) {
                        return "message.mundos_de_axelis.casa.desigual";
                    }
                }
            }
        }
        AABB box = new AABB(minX, minY + 1, minZ, maxX + 1, maxY + 1, maxZ + 1);
        if (!l.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !(x instanceof Habitante)).isEmpty()) {
            return "message.mundos_de_axelis.casa.alguien";
        }
        return null;
    }

    private static boolean isSoft(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    /** Se muda: la casa se desarma (lo que pusiste tú se queda), queda libre en la puerta y te da otra semilla. */
    public static boolean moveOut(ServerPlayer sp, Habitante h) {
        ServerLevel l = (ServerLevel) h.level();
        Vecinos.Entry e = Vecinos.entry(l.getServer(), h.kindId());
        if (h.state() != Habitante.VECINO || e == null || e.house == null) {
            if (sp != null) {
                open(sp, h, R_OPEN, null, 0);
            }
            return false;
        }
        NpcKinds.Kind k = h.kind();
        BlockPos origin = e.house;
        Rotation rot = e.rot;
        ServerLevel hl = l.getServer().getLevel(e.dim);
        if (hl == null) {
            hl = l;
        }
        dismantle(hl, StructDesigns.make(k.house()), origin, rot);
        BlockPos spot = StructPlacer.world(origin, rot, 0, 0, 0);
        h.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        h.setState(Habitante.LIBRE, spot);
        Vecinos.set(l.getServer(), h.kindId(), Habitante.LIBRE, h.getUUID(), hl.dimension(), spot, null, Rotation.NONE);
        poof(hl, h.position(), false);
        l.playSound(null, spot, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        MundosDeAxelis.LOGGER.info("[Vecinos] {} se mudó; casa desarmada en {} {} {}", h.kindId(), origin.getX(), origin.getY(), origin.getZ());
        if (sp != null) {
            if (!hasKit(sp, h.kindId())) {
                give(sp, kit(h.kindId()));
            }
            open(sp, h, R_MOVED, null, 0);
        }
        return true;
    }

    /** Quita los bloques de la casa que siguen siendo los del plano; el suelo vuelve a ser pasto. */
    static void dismantle(ServerLevel l, StructBp bp, BlockPos origin, Rotation rot) {
        List<BlockPos> gone = new ArrayList<>();
        Set<BlockPos> goneSet = new HashSet<>();
        for (Map.Entry<Long, String> e : bp.m.entrySet()) {
            long key = e.getKey();
            int y = StructBp.ky(key);
            BlockState want = StructPlacer.planned(e.getValue(), rot);
            if (want == null || want.isAir()) {
                continue;
            }
            BlockPos w = StructPlacer.world(origin, rot, StructBp.kx(key), y, StructBp.kz(key));
            BlockState cur = l.getBlockState(w);
            if (!cur.is(want.getBlock())) {
                continue;
            }
            // lo que tenga cosas tuyas dentro (cofres, barriles, hornos, estanterías, un atril con libro) se queda
            net.minecraft.world.level.block.entity.BlockEntity be = l.getBlockEntity(w);
            if (be instanceof net.minecraft.world.Container c && !c.isEmpty()) {
                continue;
            }
            if (cur.hasProperty(net.minecraft.world.level.block.LecternBlock.HAS_BOOK) && cur.getValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK)) {
                continue;
            }
            BlockState to = y < 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (y < 0 && cur.is(Blocks.GRASS_BLOCK)) {
                continue;
            }
            l.setBlock(w, to, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            gone.add(w);
            goneSet.add(w);
        }
        // lo que estaba pegado a la casa (antorchas tuyas, etc.) se revisa como si se hubiera roto a mano
        for (BlockPos w : gone) {
            for (Direction d : Direction.values()) {
                BlockPos n = w.relative(d);
                if (goneSet.contains(n)) {
                    continue;
                }
                BlockState ns = l.getBlockState(n);
                BlockState upd = Block.updateFromNeighbourShapes(ns, l, n);
                if (upd != ns) {
                    Block.updateOrDestroy(ns, upd, l, n, Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Nube de hojas (y un destello si llega). */
    public static void poof(ServerLevel l, Vec3 p, boolean arrive) {
        l.sendParticles(ParticleTypes.CHERRY_LEAVES, p.x, p.y + 1.0, p.z, 30, 0.6, 0.8, 0.6, 0.02);
        l.sendParticles(arrive ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.POOF, p.x, p.y + 1.0, p.z, 14, 0.4, 0.6, 0.4, 0.01);
    }

    // ================================================================== tienda

    static void buy(ServerPlayer sp, Habitante h, int idx) {
        if (h.state() != Habitante.VECINO) {
            open(sp, h, R_NO_HOUSE, null, 0);
            return;
        }
        if (idx < 0 || idx >= NpcShop.OFFERS.size()) {
            return;
        }
        NpcShop.Offer o = NpcShop.OFFERS.get(idx);
        ServerLevel l = (ServerLevel) h.level();
        if (Vecinos.residents(l.getServer()) < o.needResidents()) {
            open(sp, h, R_LOCKED, null, idx);
            return;
        }
        ItemStack stack = o.stack();
        if (o.compass() != null) {
            BlockPos target = NaturalGen.nearest(l.getServer(), o.compass(), h.blockPosition());
            if (target == null) {
                open(sp, h, R_NONE_FOUND, place(o.compass()), idx);
                return;
            }
            compass(stack, l.getServer().overworld().dimension(), target, o.compass());
        }
        if (!Essence.spend(sp, o.price())) {
            open(sp, h, R_NO_ESSENCE, null, idx);
            return;
        }
        give(sp, stack);
        Essence.sync(sp);
        l.playSound(null, h.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.8F, 1.3F);
        open(sp, h, R_BOUGHT, null, idx);
    }

    /** Brújula normal que apunta a ese sitio (sin piedra imán) con el nombre de lo que encuentra. */
    static void compass(ItemStack s, net.minecraft.resources.ResourceKey<Level> dim, BlockPos target, String what) {
        CompoundTag t = s.getOrCreateTag();
        t.put("LodestonePos", NbtUtils.writeBlockPos(target));
        t.putString("LodestoneDimension", dim.location().toString());
        t.putBoolean("LodestoneTracked", false);
        s.setHoverName(Component.translatable("item.mundos_de_axelis.brujula_npc", place(what)).withStyle(st -> st.withItalic(false).withColor(0x9FE07A)));
    }

    static Component place(String what) {
        return Component.translatable("npc.mundos_de_axelis.place." + what);
    }

    // ================================================================== pistas

    private static final String[] HINT_KINDS = {"volcan", "bosque", "hielo", "forja", "torre"};

    static void hint(ServerPlayer sp, Habitante h, int n) {
        if (h.state() != Habitante.VECINO) {
            open(sp, h, R_NO_HOUSE, null, 0);
            return;
        }
        String what = HINT_KINDS[Math.floorMod(n, HINT_KINDS.length)];
        ServerLevel l = (ServerLevel) h.level();
        BlockPos from = h.blockPosition();
        BlockPos t = l.dimension() == Level.OVERWORLD ? NaturalGen.nearest(l.getServer(), what, from) : null;
        Component msg;
        if (t == null) {
            msg = Component.translatable("npc.mundos_de_axelis." + h.kindId() + ".hint.none", place(what));
        } else {
            int dx = t.getX() - from.getX(), dz = t.getZ() - from.getZ();
            int dist = (int) Math.round(Math.hypot(dx, dz));
            if (dist < 48) {
                msg = Component.translatable("npc.mundos_de_axelis." + h.kindId() + ".hint.here", place(what));
            } else {
                int rounded = Math.max(50, Math.round(dist / 50.0F) * 50);
                msg = Component.translatable("npc.mundos_de_axelis." + h.kindId() + ".hint." + what, dir(dx, dz), rounded);
            }
        }
        open(sp, h, R_HINT, msg, 0);
    }

    private static final String[] DIRS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};

    static Component dir(int dx, int dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        int i = Math.floorMod((int) Math.round(a / 45.0), 8);
        return Component.translatable("npc.mundos_de_axelis.dir." + DIRS[i]);
    }

    // ================================================================== utilidades

    static void give(ServerPlayer sp, ItemStack s) {
        if (!sp.getInventory().add(s)) {
            sp.drop(s, false);
        }
    }

    static int count(Player p, net.minecraft.world.item.Item it) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(it)) {
                n += s.getCount();
            }
        }
        return n;
    }

    static void take(Player p, net.minecraft.world.item.Item it, int n) {
        for (ItemStack s : p.getInventory().items) {
            if (n <= 0) {
                break;
            }
            if (s.is(it)) {
                int t = Math.min(n, s.getCount());
                s.shrink(t);
                n -= t;
            }
        }
    }

    /** Lee de una semilla de casa de quién es. */
    public static final class KitKind {
        public static String of(ItemStack s) {
            String k = s.hasTag() ? s.getTag().getString("Npc") : "";
            return k.isEmpty() ? NpcKinds.RUDA.id() : k;
        }

        private KitKind() {
        }
    }
}
