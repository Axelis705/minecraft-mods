package com.navidad.bloque;

import com.axeliscore.fx.Fx;
import com.navidad.Navidad;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Lo que guarda una caja de regalo: el objeto envuelto, quién lo envolvió y para quién es (si tiene nombre). Al abrirla, la animación
 * dura {@link #DURA} ticks (tiembla, salta la tapa, sale la luz del color de su rareza y sube el regalo) y al final se entrega.
 */
public class CajaRegaloBE extends BlockEntity {
    public static final int DURA = 32, TAPA = 11;
    public ItemStack contenido = ItemStack.EMPTY;
    public String de = "", para = "";
    /** Cuándo empezó a abrirse (tiempo del mundo; -1 = cerrada), quién la abrió y qué tan raro es lo de adentro (0, 1, 2). */
    public long abriendo = -1;
    public UUID quien;
    public int rareza;
    /** Para la prueba con fotos. */
    public static int abiertas;

    public CajaRegaloBE(BlockPos p, BlockState s) {
        super(ModRegistry.CAJA_REGALO_BE.get(), p, s);
    }

    /** Qué tan raro es un regalo (para el color de la luz y el sonido): adornos por su rareza, lo demás por las listas de premios. */
    public static int rarezaDe(ItemStack st) {
        com.navidad.arbol.Adornos.Def d = com.navidad.arbol.Adornos.de(st.getItem());
        if (d != null) {
            return d.rareza() == com.navidad.arbol.Adornos.Rareza.EPICO ? 2 : d.rareza() == com.navidad.arbol.Adornos.Rareza.RARO ? 1 : 0;
        }
        Premios.Rareza r = Premios.rarezaDe(st);
        return r == Premios.Rareza.EPICO ? 2 : r == Premios.Rareza.RARO ? 1 : 0;
    }

    public void abrir(Player p) {
        abriendo = level.getGameTime();
        quien = p.getUUID();
        rareza = rarezaDe(contenido);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** (Los dos lados) Cada tick mientras se abre: sonidos y chispas en el servidor y, al final, el regalo. */
    public void tick() {
        if (abriendo < 0 || level == null) {
            return;
        }
        long t = level.getGameTime() - abriendo;
        if (level.isClientSide || !(level instanceof ServerLevel l)) {
            return;
        }
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        if (t == 4 || t == 8) {
            l.playSound(null, worldPosition, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.8F, 1.4F + t * 0.05F);
        }
        if (t == TAPA) {
            l.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6F, 1.7F);
            l.playSound(null, worldPosition, ModRegistry.REGALO_ABRE.get(), SoundSource.BLOCKS, 1F, 1F);
            l.sendParticles(ParticleTypes.END_ROD, x, y + 0.2, z, 14 + rareza * 10, 0.15, 0.1, 0.15, 0.08 + rareza * 0.03);
        }
        if (t == TAPA + 3) {
            l.playSound(null, worldPosition, rareza == 2 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : rareza == 1
                    ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.BLOCKS, rareza == 2 ? 0.7F : 1F,
                    rareza == 0 ? 1.6F : 1.2F);
        }
        if (t >= DURA) {
            entregar(l, x, y, z);
        }
    }

    private void entregar(ServerLevel l, double x, double y, double z) {
        ItemStack dentro = contenido.copy();
        String deQuien = de;
        contenido = ItemStack.EMPTY;
        abriendo = -1;
        abiertas++;
        ServerPlayer sp = quien == null ? null : l.getServer().getPlayerList().getPlayer(quien);
        l.removeBlock(worldPosition, false);
        Premios.confeti(l, x, y + 0.3, z, rareza > 0);
        l.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.3F + rareza * 0.1F);
        if (sp != null && sp.distanceToSqr(x, y, z) < 24 * 24) {
            com.navidad.Logros.dar(sp, "regalo");
            com.navidad.libro.Libro.evento(sp, "regalo_abierto", 1);
            ChatFormatting col = rareza == 2 ? ChatFormatting.LIGHT_PURPLE : rareza == 1 ? ChatFormatting.AQUA : ChatFormatting.WHITE;
            Component nombre = Component.literal(dentro.getCount() + "× ").append(dentro.getHoverName()).withStyle(col, ChatFormatting.BOLD);
            sp.displayClientMessage(Component.translatable("navidad.caja.abierta", deQuien, nombre), false);
            Fx.toast(sp, dentro, Component.translatable("navidad.caja.toast." + rareza), Component.translatable("navidad.caja.toast_de",
                    nombre, deQuien), rareza == 2 ? 0xD070FF : rareza == 1 ? 0x60E0FF : 0xFFD84A);
            if (!sp.getInventory().add(dentro)) {
                sp.drop(dentro, false);
            }
            // quien lo envolvió cuenta un regalo entregado (para la misión de Santa), si lo abre otra persona
            ServerPlayer envolvio = sp.server.getPlayerList().getPlayerByName(deQuien);
            if (envolvio != null && envolvio != sp) {
                com.axeliscore.npc.Misiones.evento(envolvio, Navidad.MODID + ":regalo_entregado", 1);
            }
        } else if (!dentro.isEmpty()) {
            ItemEntity it = new ItemEntity(l, x, y + 0.4, z, dentro);
            it.setDeltaMovement(0, 0.2, 0);
            l.addFreshEntity(it);
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        contenido = t.contains("Contenido") ? ItemStack.of(t.getCompound("Contenido")) : ItemStack.EMPTY;
        de = t.getString("De");
        para = t.getString("Para");
        abriendo = t.contains("Abriendo") ? t.getLong("Abriendo") : -1;
        rareza = t.getInt("Rareza");
        quien = t.hasUUID("Quien") ? t.getUUID("Quien") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        if (!contenido.isEmpty()) {
            t.put("Contenido", contenido.save(new CompoundTag()));
        }
        t.putString("De", de);
        t.putString("Para", para);
        if (abriendo >= 0) {
            t.putLong("Abriendo", abriendo);
            t.putInt("Rareza", rareza);
            if (quien != null) {
                t.putUUID("Quien", quien);
            }
        }
    }

    /** Al cliente solo le hace falta lo de la animación (qué sube y de qué color es la luz) mientras se abre. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        if (abriendo >= 0) {
            t.putLong("Abriendo", abriendo);
            t.putInt("Rareza", rareza);
            if (!contenido.isEmpty()) {
                t.put("Contenido", contenido.save(new CompoundTag()));
            }
        }
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
    }
}
