package com.navidad.bloque;

import com.navidad.objeto.Premios;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La caja de regalo para envolver: clic derecho con un objeto en la mano lo mete adentro (queda envuelta, "De: tú"); con una etiqueta
 * con nombre se le pone "Para: nombre" y entonces solo esa persona la puede abrir. Al abrirla sale el objeto con confeti y la caja se va.
 * Si se rompe envuelta, la caja sale con el regalo adentro (para llevarla a otro lado o ponerla bajo el árbol).
 */
public class CajaRegalo extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LLENA = BooleanProperty.create("llena");
    private static final VoxelShape FORMA = Block.box(3, 0, 3, 13, 11, 13);

    public CajaRegalo(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LLENA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LLENA);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        boolean llena = c.getItemInHand().getTagElement("BlockEntityTag") != null
                && c.getItemInHand().getTagElement("BlockEntityTag").contains("Contenido");
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()).setValue(LLENA, llena);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    /** Una caja de regalo ya envuelta, como objeto (al ponerla queda llena, con quién la manda y para quién es). */
    public static ItemStack envuelta(ItemStack contenido, String de, String para) {
        ItemStack st = new ItemStack(com.navidad.registry.ModRegistry.CAJA_REGALO_ITEM.get());
        net.minecraft.nbt.CompoundTag be = new net.minecraft.nbt.CompoundTag();
        be.put("Contenido", contenido.save(new net.minecraft.nbt.CompoundTag()));
        be.putString("De", de);
        be.putString("Para", para == null ? "" : para);
        st.addTagElement("BlockEntityTag", be);
        return st;
    }

    /** (Servidor) Pone una caja de regalo llena en {@code pos} si cabe ahí (lugar libre con piso firme). Devuelve si la pudo poner. */
    public static boolean poner(Level l, BlockPos pos, Direction mira, ItemStack contenido, String de, String para) {
        if (!l.getBlockState(pos).canBeReplaced() || !l.getFluidState(pos).isEmpty()
                || !l.getBlockState(pos.below()).isFaceSturdy(l, pos.below(), Direction.UP)) {
            return false;
        }
        l.setBlock(pos, com.navidad.registry.ModRegistry.CAJA_REGALO.get().defaultBlockState().setValue(FACING, mira).setValue(LLENA, true), 3);
        if (l.getBlockEntity(pos) instanceof CajaRegaloBE be) {
            be.contenido = contenido.copy();
            be.de = de;
            be.para = para == null ? "" : para;
            be.setChanged();
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new CajaRegaloBE(p, s);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(l.getBlockEntity(pos) instanceof CajaRegaloBE be)) {
            return InteractionResult.PASS;
        }
        ItemStack mano = p.getItemInHand(h);
        String yo = p.getGameProfile().getName();
        if (be.contenido.isEmpty()) {
            if (mano.isEmpty() || mano.is(asItem())) {
                p.displayClientMessage(Component.translatable("navidad.caja.vacia"), true);
                return InteractionResult.CONSUME;
            }
            be.contenido = mano.copy();
            be.de = yo;
            mano.setCount(0);
            be.setChanged();
            l.setBlock(pos, s.setValue(LLENA, true), 3);
            l.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1F, 0.8F);
            l.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1F, 1.2F);
            p.displayClientMessage(Component.translatable("navidad.caja.envuelta"), false);
            return InteractionResult.CONSUME;
        }
        if (mano.is(Items.NAME_TAG) && mano.hasCustomHoverName()) {
            be.para = mano.getHoverName().getString();
            be.setChanged();
            l.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1F, 1.2F);
            p.displayClientMessage(Component.translatable("navidad.caja.para", be.para), false);
            return InteractionResult.CONSUME;
        }
        if (!be.para.isEmpty() && !be.para.equalsIgnoreCase(yo) && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("navidad.caja.no_es_tuyo", be.para, be.de).withStyle(ChatFormatting.GOLD), true);
            l.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.6F, 1.4F);
            return InteractionResult.CONSUME;
        }
        // ¡a abrirlo!
        ItemStack dentro = be.contenido.copy();
        String deQuien = be.de;
        be.contenido = ItemStack.EMPTY;
        l.removeBlock(pos, false);
        if (p instanceof ServerPlayer sp) {
            com.navidad.Logros.dar(sp, "regalo");
            sp.displayClientMessage(Component.translatable("navidad.caja.abierta", deQuien,
                    Component.literal(dentro.getCount() + "× ").append(dentro.getHoverName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
            if (!sp.getInventory().add(dentro)) {
                sp.drop(dentro, false);
            }
            // quien lo envolvió cuenta un regalo entregado (para la misión de Santa), si lo abre otra persona
            ServerPlayer envolvio = sp.server.getPlayerList().getPlayerByName(deQuien);
            if (envolvio != null && envolvio != sp) {
                com.axeliscore.npc.Misiones.evento(envolvio, com.navidad.Navidad.MODID + ":regalo_entregado", 1);
            }
        }
        Premios.confeti((ServerLevel) l, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, true);
        l.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7F, 1.3F);
        l.playSound(null, pos, com.navidad.registry.ModRegistry.REGALO_ABRE.get(), SoundSource.BLOCKS, 1F, 1F);
        return InteractionResult.CONSUME;
    }
}
