package com.navidad.bloque;

import com.navidad.Temporada;
import com.navidad.objeto.Premios;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.axeliscore.data.WorldStore;
import com.navidad.Navidad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.time.LocalDate;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

/**
 * El calendario de Adviento: una casita con 24 puertas. Del 1 al 24 de diciembre (fecha de verdad, hora del centro de México) cada
 * persona abre la puerta del día una vez y le sale su premio; el 24 es el grande. Cada quien lleva su cuenta con el mundo (no con el
 * bloque: aunque haya varios calendarios, cada puerta se abre una sola vez por año).
 */
public class Calendario extends HorizontalDirectionalBlock {
    private static final VoxelShape FORMA = Block.box(1, 0, 4, 15, 15, 12);
    /** El premio de cada día: id, cantidad (el 24, además, un premio épico). */
    private static final String[][] DIAS = {
            {"navidad:galleta_jengibre", "6"}, {"navidad:esfera_roja", "4"}, {"navidad:baston_caramelo", "5"}, {"minecraft:emerald", "4"},
            {"navidad:chocolate_caliente", "3"}, {"navidad:luces_colgantes", "4"}, {"navidad:adorno_campanita", "2"}, {"navidad:tamal", "4"},
            {"navidad:corona_navidad", "1"}, {"minecraft:gold_ingot", "6"}, {"navidad:ponche", "3"}, {"navidad:pinata", "1"},
            {"navidad:calcetin_rojo", "2"}, {"minecraft:diamond", "2"}, {"navidad:bunuelo", "4"}, {"navidad:adorno_copo", "3"},
            {"navidad:nochebuena", "2"}, {"navidad:champurrado", "3"}, {"navidad:gorro_santa", "1"}, {"minecraft:golden_apple", "2"},
            {"navidad:muneco_nieve", "1"}, {"navidad:adorno_renito", "1"}, {"navidad:pavo_navideno", "2"}, {"minecraft:diamond", "5"}};

    public Calendario(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    private static String clave() {
        return "adviento_" + Temporada.hoy().getYear();
    }

    public static boolean abierta(MinecraftServer s, UUID id, int dia) {
        return (WorldStore.get(s, Navidad.MODID).player(id).getInt(clave()) & (1 << dia)) != 0;
    }

    public static void abrir(MinecraftServer s, UUID id, int dia) {
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        CompoundTag t = w.player(id);
        t.putInt(clave(), t.getInt(clave()) | (1 << dia));
        w.dirty();
    }

    public static int cuantas(MinecraftServer s, UUID id) {
        return Integer.bitCount(WorldStore.get(s, Navidad.MODID).player(id).getInt(clave()));
    }

    /** Para las pruebas: cierra todas las puertas de esa persona. */
    public static void reiniciar(MinecraftServer s, UUID id) {
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        w.player(id).remove(clave());
        w.dirty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(p instanceof ServerPlayer sp) || l.getServer() == null) {
            return InteractionResult.PASS;
        }
        int dia = Temporada.diaDiciembre();
        if (dia == 0) {
            LocalDate h0 = Temporada.hoy();
            LocalDate dic = LocalDate.of(h0.getMonthValue() == 12 ? h0.getYear() : h0.getYear(), 12, 1);
            if (dic.isBefore(h0)) {
                dic = dic.plusYears(1);
            }
            long faltan = ChronoUnit.DAYS.between(h0, dic);
            sp.displayClientMessage(Component.translatable("navidad.calendario.espera", faltan).withStyle(ChatFormatting.GOLD), true);
            l.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.5F, 1.5F);
            return InteractionResult.CONSUME;
        }
        if (dia > 24) {
            sp.displayClientMessage(Component.translatable("navidad.calendario.feliz", cuantas(l.getServer(), sp.getUUID())).withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (abierta(l.getServer(), sp.getUUID(), dia)) {
            sp.displayClientMessage(Component.translatable("navidad.calendario.ya", dia).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        abrir(l.getServer(), sp.getUUID(), dia);
        String[] d = DIAS[dia - 1];
        ItemStack premio = Premios.pila(d[0], Integer.parseInt(d[1]));
        Premios.Rareza rz = dia == 24 ? Premios.Rareza.EPICO : dia % 6 == 0 ? Premios.Rareza.RARO : Premios.Rareza.COMUN;
        Premios.dar(sp, premio, rz, dia == 24 ? "navidad.calendario.nochebuena" : "navidad.calendario.puerta");
        if (dia == 24) {
            Premios.dar(sp, Premios.sacar(Premios.Rareza.EPICO, l.random), Premios.Rareza.EPICO, "navidad.regalo.te_toco");
        }
        l.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.6F);
        l.playSound(null, pos, com.navidad.registry.ModRegistry.CALENDARIO_ABRE.get(), SoundSource.BLOCKS, 1F, 1F);
        return InteractionResult.CONSUME;
    }
}
