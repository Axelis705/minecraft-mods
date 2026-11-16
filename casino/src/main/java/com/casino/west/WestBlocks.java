package com.casino.west;

import com.casino.decor.NeonLetterBlock;
import com.casino.decor.Shapes;
import com.casino.decor.WallDecorBlock;
import com.casino.item.VipCardItem;
import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** Bloques del casino del Viejo Oeste: puertas de saloon, pianola, letras pintadas, carteles de "Se busca" y la puertita VIP. */
public final class WestBlocks {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private WestBlocks() {
    }

    // ================================================================== puertas de saloon

    /**
     * Puerta batiente de saloon (una hoja). No estorba: quien pasa la empuja y la hoja se abre hacia donde camina, se queda abierta
     * mientras pasa y luego regresa balanceándose como un resorte. La dibuja {@code WestRenderers.Door}; la física corre en el cliente.
     * FACING es hacia donde se abre (el lado al que camina quien entra); HINGE el lado de la bisagra visto así.
     */
    public static class SaloonDoor extends UseBlock implements EntityBlock {
        private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{0, 5, 7, 16, 16, 9});

        public SaloonDoor(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HINGE, DoorHingeSide.LEFT));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, HINGE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            Direction f = ctx.getHorizontalDirection();
            // si a la izquierda ya hay otra hoja, esta es la de la derecha (se juntan al centro)
            BlockState left = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(f.getCounterClockWise()));
            boolean right = left.getBlock() instanceof SaloonDoor && left.getValue(FACING) == f && left.getValue(HINGE) == DoorHingeSide.LEFT;
            return defaultBlockState().setValue(FACING, f).setValue(HINGE, right ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPES.get(s.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }

        @Override
        public BlockState mirror(BlockState s, net.minecraft.world.level.block.Mirror m) {
            return m == net.minecraft.world.level.block.Mirror.NONE ? s
                    : s.setValue(HINGE, s.getValue(HINGE) == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        }

        /** Clic: la empujas desde donde estás. */
        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide() && level.getBlockEntity(pos) instanceof SaloonDoorBE be) {
                Direction f = state.getValue(FACING);
                double side = (player.getX() - pos.getX() - 0.5) * f.getStepX() + (player.getZ() - pos.getZ() - 0.5) * f.getStepZ();
                be.kick(side < 0 ? 1 : -1, 9F);
            }
            return Plat.ok(level.isClientSide());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new SaloonDoorBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (!level.isClientSide() || type != WestReg.DOOR_BE.get()) {
                return null;
            }
            return (lvl, p, st, be) -> ((SaloonDoorBE) be).clientTick();
        }
    }

    /**
     * Puertita VIP del saloon (como la batiente, pero con panel de terciopelo y placa "VIP"): solo se abre para quien lleva encima
     * una tarjeta VIP del nivel pedido (o en creativo). Para los demás es sólida. El nivel se cambia con clic agachado (operador).
     */
    public static class VipGate extends SaloonDoor {
        public static final IntegerProperty ACCESS = IntegerProperty.create("access", 1, 2);
        private static final Map<Direction, VoxelShape> SOLID = Shapes.rotated(new double[]{0, 0, 6, 16, 24, 10});

        public VipGate(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HINGE, DoorHingeSide.LEFT).setValue(ACCESS, 1));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, HINGE, ACCESS);
        }

        /** ¿Lleva encima la tarjeta? (solo lo que trae en el inventario: así el cliente y el servidor dicen lo mismo) */
        public static boolean allowed(Entity e, int need) {
            if (!(e instanceof Player p)) {
                return false;
            }
            if (p.isCreative() || p.isSpectator()) {
                return true;
            }
            for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
                if (s.getItem() instanceof VipCardItem c && c.level >= need) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            if (c instanceof EntityCollisionContext ec && ec.getEntity() != null && allowed(ec.getEntity(), s.getValue(ACCESS))) {
                return net.minecraft.world.phys.shapes.Shapes.empty();
            }
            return SOLID.get(s.getValue(FACING));
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            int need = state.getValue(ACCESS);
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown() && Plat.isOp(player)) {
                    int next = need == 1 ? 2 : 1;
                    level.setBlock(pos, state.setValue(ACCESS, next), 3);
                    Plat.msg(player, Component.translatable("casino.west.gate_set", Component.translatable(next == 2 ? "item.casino.vip_card_gold"
                            : "item.casino.vip_card_silver")), true);
                } else if (allowed(player, need)) {
                    Plat.msg(player, Component.translatable("casino.west.gate_ok").withStyle(ChatFormatting.GOLD), true);
                } else {
                    Plat.msg(player, Component.translatable(need >= 2 ? "casino.west.gate_need_gold" : "casino.west.gate_need_silver")
                            .withStyle(ChatFormatting.RED), true);
                    level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.3F);
                }
            } else if (allowed(player, need)) {
                return super.onUse(state, level, pos, player, hand, hit);
            }
            return Plat.ok(level.isClientSide());
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (!level.isClientSide() || type != WestReg.GATE_BE.get()) {
                return null;
            }
            return (lvl, p, st, be) -> ((SaloonDoorBE) be).clientTick();
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new SaloonDoorBE(WestReg.GATE_BE.get(), pos, state);
        }
    }

    // ================================================================== pianola

    /**
     * Pianola: piano vertical que toca solo (música del Oeste para todo el saloon, se oye más fuerte cerca). Las teclas se hunden al
     * ritmo y el rollo de papel corre en la ventanita. Clic derecho la prende o la apaga.
     */
    public static class Piano extends UseBlock implements EntityBlock {
        private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{0, 0, 3, 16, 16, 16});

        public Piano(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, LIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPES.get(s.getValue(FACING));
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                boolean on = !state.getValue(LIT);
                level.setBlock(pos, state.setValue(LIT, on), 3);
                level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, on ? 1.3F : 0.6F);
            }
            return Plat.ok(level.isClientSide());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PianoBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != WestReg.PIANO_BE.get()) {
                return null;
            }
            if (!level.isClientSide()) {
                return (lvl, p, st, be) -> ((PianoBE) be).serverTick();
            }
            return (lvl, p, st, be) -> {
                if (st.getValue(LIT)) {
                    com.casino.plat.Net.onClient(() -> com.casino.client.SpeakerMusic.seen(p, com.casino.client.SpeakerMusic.PIANO));
                }
            };
        }
    }

    // ================================================================== letras pintadas y carteles

    /** Letra pintada sobre madera ("SALOON", "HOTEL", "BANCO"...): igual que la de neón (misma pantalla), pero sin luz. */
    public static class PaintedLetter extends NeonLetterBlock {
        public PaintedLetter(Properties p) {
            super(p);
        }
    }

    /** Cartel de "Se busca" pegado a la pared (4 forajidos distintos; clic para cambiarlo). */
    public static class Wanted extends WallDecorBlock {
        public static final IntegerProperty DESIGN = IntegerProperty.create("design", 0, 3);

        public Wanted(Properties p) {
            super(p, 1);
        }

        @Override
        protected IntegerProperty design() {
            return DESIGN;
        }
    }

    // ================================================================== toro mecánico y galería de tiro

    /**
     * Toro mecánico: un toro de fierro y cuero sobre un pistón. Clic para apostar y montarlo (pantalla de apuesta en el cliente). Lo
     * dibuja {@code WestRenderers.Bull}; la montada es {@link BullRideEntity}. FACING = hacia donde mira la cabeza.
     */
    public static class Bull extends UseBlock implements EntityBlock {
        private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(1, 0, 1, 15, 24, 15);

        public Bull(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPE;
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            boolean busy = !level.getEntitiesOfClass(BullRideEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5)).isEmpty();
            if (level.isClientSide()) {
                if (!busy && !player.isPassenger()) {
                    com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.openBet(pos, com.casino.client.WestClient.BULL));
                }
            } else if (busy) {
                Plat.msg(player, Component.translatable("casino.bull.busy").withStyle(ChatFormatting.YELLOW), true);
            }
            return Plat.ok(level.isClientSide());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BullBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != WestReg.BULL_BE.get() || level.isClientSide()) {
                return null;
            }
            return (lvl, p, st, be) -> ((BullBE) be).serverTick();
        }
    }

    /**
     * Caja del mostrador de la galería de tiro (campana y alcancía). Clic para apostar: te prestan un revólver y salen los blancos al
     * fondo. FACING = hacia el tirador. Los blancos y el marcador los dibuja {@code WestRenderers.Gallery}; las reglas están en
     * {@link com.casino.game.GalleryPlan}.
     */
    public static class Gallery extends UseBlock implements EntityBlock {
        private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{2, 0, 3, 14, 9, 13});

        public Gallery(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPES.get(s.getValue(FACING));
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(level.getBlockEntity(pos) instanceof GalleryBE be)) {
                return InteractionResult.PASS;
            }
            if (player.getMainHandItem().getItem() instanceof RevolverItem) {
                return InteractionResult.PASS;   // con el revólver en la mano, el clic es un disparo
            }
            if (level.isClientSide()) {
                if (!be.playing()) {
                    com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.openBet(pos, com.casino.client.WestClient.GALLERY));
                }
            } else if (be.playing()) {
                Plat.msg(player, Component.translatable("casino.gallery.busy", be.shooterName()).withStyle(ChatFormatting.YELLOW), true);
            }
            return Plat.ok(level.isClientSide());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new GalleryBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != WestReg.GALLERY_BE.get()) {
                return null;
            }
            return level.isClientSide() ? (lvl, p, st, be) -> ((GalleryBE) be).clientTick() : (lvl, p, st, be) -> ((GalleryBE) be).serverTick();
        }
    }

    /**
     * Campana del duelo al mediodía: un poste con campana a la orilla de la calle. Clic → eliges pistolero y apuesta. FACING = hacia donde
     * aparece el pistolero; las dos X (la tuya y la suya) las dibuja {@code WestRenderers.Duel}. Reglas en {@link com.casino.game.DuelRules}.
     */
    public static class DuelBell extends UseBlock implements EntityBlock {
        private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(5, 0, 5, 11, 24, 11);

        public DuelBell(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        /** Al ponerla, el pistolero queda a la derecha de quien la pone (la calle queda enfrente). */
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getClockWise());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPE;
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return useMatch(level, pos, player, hand, com.casino.client.WestClient.DUEL);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DuelBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != WestReg.DUEL_BE.get()) {
                return null;
            }
            return level.isClientSide() ? (lvl, p, st, be) -> ((DuelBE) be).clientTick() : (lvl, p, st, be) -> ((DuelBE) be).serverTick();
        }
    }

    /**
     * Charola de dados del mentiroso (dos cubiletes de cuero sobre fieltro) para poner sobre una mesa. Clic → pantalla del juego.
     * FACING = hacia el jugador; el tahúr se sienta enfrente. Reglas en {@link com.casino.game.LiarsDice}.
     */
    public static class Dice extends UseBlock implements EntityBlock {
        private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{0, 0, 0, 16, 4, 16});

        public Dice(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPES.get(s.getValue(FACING));
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof DiceBE be)) {
                return InteractionResult.PASS;
            }
            boolean mine = be.isPlayer(player.getUUID());
            if (level.isClientSide()) {
                if (be.phase() == DiceBE.IDLE || mine) {
                    com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.openDice(pos));
                }
            } else if (be.phase() != DiceBE.IDLE && !mine) {
                Plat.msg(player, Component.translatable("casino.dice.busy", be.playerName()).withStyle(ChatFormatting.YELLOW), true);
            }
            return Plat.ok(level.isClientSide());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DiceBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != WestReg.DICE_BE.get()) {
                return null;
            }
            return level.isClientSide() ? (lvl, p, st, be) -> ((DiceBE) be).clientTick() : (lvl, p, st, be) -> ((DiceBE) be).serverTick();
        }
    }

    /** Clic en la campana del duelo o en la mesa de vencidas: abre la pantalla del reto (rival NPC, reto entre jugadores o aceptar). */
    static InteractionResult useMatch(Level level, BlockPos pos, Player player, InteractionHand hand, int kind) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof MatchBE be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            if (be.phase() == MatchBE.IDLE || be.phase() == MatchBE.WAITING) {
                com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.openMatch(pos, kind));
            }
        } else if (be.phase() == MatchBE.RUNNING || be.phase() == MatchBE.RESULT) {
            Plat.msg(player, Component.translatable("casino.match.busy").withStyle(ChatFormatting.YELLOW), true);
        }
        return Plat.ok(level.isClientSide());
    }

    /**
     * Mesa de vencidas (pulso): una mesa alta y fuerte con coderas. FACING = hacia el lado del que retó (el otro, o el forzudo, va del
     * lado contrario). Clic → reto contra un forzudo o contra otro jugador. Reglas en {@link com.casino.game.ArmRules}.
     */
    public static class ArmTable extends UseBlock implements EntityBlock {
        private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(1, 0, 1, 15, 14, 15);

        public ArmTable(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return SHAPE;
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return useMatch(level, pos, player, hand, com.casino.client.WestClient.ARM);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ArmBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != WestReg.ARM_BE.get()) {
                return null;
            }
            return level.isClientSide() ? (lvl, p, st, be) -> ((ArmBE) be).clientTick() : (lvl, p, st, be) -> ((ArmBE) be).serverTick();
        }
    }
}
