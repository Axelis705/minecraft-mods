package com.mundosdeaxelis.block;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.CrateRoll;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.CrateOpenPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModBlockEntities;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Estado de un crate. Mientras una ruleta está en marcha guarda cuándo empezó, la semilla de las casillas de relleno y el
 * premio (ya sorteado en el servidor). Todo se sincroniza con los clientes: cualquiera que esté cerca ve el holograma.
 */
public class CrateBlockEntity extends BlockEntity {
    public long start = -1;
    public long seed;
    public String winner = "";
    public String openerName = "";
    public UUID opener;

    // solo cliente
    private Item[] stripCache;
    private long stripSeed;
    public int lastSlot = -1;
    public boolean spinSoundDone;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE.get(), pos, state);
    }

    public CrateKind kind() {
        return getBlockState().getBlock() instanceof CrateBlock b ? b.kind : CrateKind.PETS;
    }

    public boolean rolling() {
        return start >= 0 && !winner.isEmpty();
    }

    public Item winnerItem() {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(winner));
        return it == null ? net.minecraft.world.item.Items.AIR : it;
    }

    /** Las casillas de la ruleta (se arma una vez por apertura). */
    public Item[] strip() {
        if (stripCache == null || stripSeed != seed) {
            stripCache = CratePools.strip(kind(), seed, CrateRoll.LEN, CrateRoll.WIN, winnerItem());
            stripSeed = seed;
        }
        return stripCache;
    }

    // ------------------------------------------------------------------ guardado y red

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Start", start);
        tag.putLong("Seed", seed);
        tag.putString("Winner", winner);
        tag.putString("Opener", openerName);
        if (opener != null) {
            tag.putUUID("OpenerId", opener);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        start = tag.contains("Start") ? tag.getLong("Start") : -1;
        seed = tag.getLong("Seed");
        winner = tag.getString("Winner");
        openerName = tag.getString("Opener");
        opener = tag.hasUUID("OpenerId") ? tag.getUUID("OpenerId") : null;
        stripCache = null;
        lastSlot = -1;
        spinSoundDone = false;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        saveAdditional(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0, 5.0, 3.0);
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------ servidor

    public boolean startRoll(ServerLevel sl, ServerPlayer sp) {
        CrateKind k = kind();
        CratePools.Result res = CratePools.draw(k, sl.random, com.mundosdeaxelis.crate.Pity.forced(sp, k));
        Item prize = res.item();
        if (prize == net.minecraft.world.item.Items.AIR) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.crate_empty").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(prize);
        winner = id == null ? "" : id.toString();
        seed = sl.random.nextLong();
        start = sl.getGameTime();
        opener = sp.getUUID();
        openerName = sp.getGameProfile().getName();
        stripCache = null;
        com.mundosdeaxelis.crate.Pity.record(sp, k, res.tier());
        sync();
        Vec3 c = Vec3.atCenterOf(worldPosition);
        sl.playSound(null, worldPosition, ModSounds.CRATE_UNLOCK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Vfx.ring(sl, c.add(0, -0.4, 0), k.color, 2.6, 16);
        Vfx.flash(sl, c.add(0, 0.9, 0), k.color, 2.0, 10);
        ModNetwork.sendTo(sp, new CrateOpenPacket(worldPosition, k.ordinal(), seed, winner, openerName));
        return true;
    }

    private void reveal(ServerLevel sl) {
        CrateKind k = kind();
        Item prize = winnerItem();
        Tier tier = CratePools.tierOf(k, prize);
        Vec3 c = Vec3.atCenterOf(worldPosition);
        SoundEvent snd = ModSounds.CRATE_REVEAL.get(tier.ordinal()).get();
        float vol = 0.9F + tier.ordinal() * 0.25F;
        sl.playSound(null, worldPosition, snd, SoundSource.BLOCKS, vol, 1.0F);
        Vfx.column(sl, c.add(0, -0.4, 0), tier.color, 0.5 + 0.12 * tier.ordinal(), 3.0 + tier.ordinal() * 0.8, 50);
        Vfx.flash(sl, c.add(0, 1.6, 0), tier.color, 2.2 + tier.ordinal() * 0.7, 14);
        Vfx.ring(sl, c.add(0, -0.4, 0), tier.color, 3.0 + tier.ordinal() * 0.8, 20);
        if (tier.ordinal() >= Tier.EPIC.ordinal()) {
            Vfx.rune(sl, c.add(0, -0.45, 0), tier.color, 3.4 + tier.ordinal() * 0.5, 50);
        }
        ItemStack st = CratePools.stack(k, prize, seed);
        Component name = tier.paint(CratePools.label(st));
        // efectos en pantalla para quien está cerca
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(c) > 32 * 32) {
                continue;
            }
            switch (tier) {
                case UNCOMMON -> Fx.screen(p, 0, tier.color, 6);
                case RARE -> Fx.screen(p, 5, tier.color, 20);
                case EPIC -> {
                    Fx.screen(p, 5, tier.color, 30);
                    Fx.screen(p, 0, tier.color, 10);
                }
                case LEGENDARY -> {
                    Fx.screen(p, 3, tier.color, 40);
                }
                case MYTHIC -> {
                    Fx.screen(p, 3, tier.color, 60);
                    Fx.screen(p, 9, 20, 20); // sacudida suave (2 grados): ojo, el 3.er dato es fuerza, no color
                }
                case STELLAR -> {
                    Fx.screen(p, 3, tier.color, 70);
                    Fx.screen(p, 9, 30, 40);
                    Fx.screen(p, 5, 0xFFFFFF, 30);
                }
                case CELESTIAL -> {
                    Fx.screen(p, 3, tier.color, 90);
                    Fx.screen(p, 9, 40, 60);
                    Fx.screen(p, 5, 0xFFFFFF, 50);
                }
                default -> {
                }
            }
        }
        // aviso a todo el servidor cuando sale algo Épico o mejor
        if (tier.ordinal() >= Tier.EPIC.ordinal()) {
            Component msg = Component.translatable("message.mundos_de_axelis.crate_win", openerName, name, tier.label(),
                    Component.translatable(k.titleKey())).withStyle(ChatFormatting.WHITE);
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(msg);
            }
        } else {
            Component msg = Component.translatable("message.mundos_de_axelis.crate_win_near", openerName, name, tier.label()).withStyle(ChatFormatting.GRAY);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(c) < 40 * 40) {
                    p.sendSystemMessage(msg);
                }
            }
        }
    }

    private void finish(ServerLevel sl) {
        Item prize = winnerItem();
        ItemStack st = CratePools.stack(kind(), prize, seed);
        ServerPlayer sp = opener == null ? null : sl.getServer().getPlayerList().getPlayer(opener);
        if (!st.isEmpty()) {
            if (sp != null && sp.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 64 * 64 && sp.getInventory().add(st)) {
                sp.level().playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
            } else {
                ItemEntity ie = new ItemEntity(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, st);
                ie.setDeltaMovement(0, 0.25, 0);
                ie.setGlowingTag(true);
                sl.addFreshEntity(ie);
            }
        }
        // bonus: a veces la caja trae además la llave de la caja de nivel superior (la última caja no tiene siguiente)
        if (sp != null && kind().rank() >= 1 && kind().rank() < 8 && sl.random.nextFloat() < NEXT_KEY_CHANCE) {
            CrateKind next = CrateKind.byIndex(kind().rank() + 1);
            ItemStack key = new ItemStack(com.mundosdeaxelis.registry.ModItems.KEYS.get(next).get());
            if (!sp.getInventory().add(key)) {
                sp.drop(key, false);
            }
            bonusKeyFx(sl, sp, worldPosition, next);
        }
        clear();
    }

    /** Revelación de la llave bonus: columna, anillos y runas del color de la caja siguiente, destellos escalonados, sonido y cartel en pantalla. */
    public static void bonusKeyFx(ServerLevel sl, ServerPlayer sp, BlockPos pos, CrateKind next) {
        int rgb = next.color;
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        float pitch = 0.9F + 0.04F * next.rank();
        sl.playSound(null, pos, ModSounds.CRATE_BONUS_KEY.get(), SoundSource.BLOCKS, 1.0F, pitch);
        ModNetwork.sendTo(sp, new com.mundosdeaxelis.network.BonusKeyPacket(next.ordinal()));
        Fx.screen(sp, com.mundosdeaxelis.network.ScreenFxPacket.FLASH, rgb, 12);
        Fx.screen(sp, com.mundosdeaxelis.network.ScreenFxPacket.STARS, rgb, 70);
        Vfx.flash(sl, c, 0xFFFFFF, 1.6, 8);
        Vfx.column(sl, c.add(0, -0.5, 0), rgb, 0.45, 3.2, 40);
        Vfx.rune(sl, c.add(0, -0.55, 0), rgb, 1.5, 60);
        Vfx.ring(sl, c.add(0, -0.45, 0), rgb, 2.6, 24);
        net.minecraft.core.particles.ParticleOptions sparks = Fx.spark(rgb, 1.2F);
        Fx.helix(sl, sparks, c.x, c.y - 0.4, c.z, 0.55, 2.6, 22, 0.0);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, c.x, c.y + 0.3, c.z, 18, 0.25, 0.3, 0.25, 0.08);
        com.mundosdeaxelis.world.Scheduler.runLater(6, () -> {
            Vfx.ring(sl, c.add(0, -0.4, 0), rgb, 3.6, 22);
            Fx.ring(sl, sparks, c.x, c.y, c.z, 1.1, 18, 0.0);
        });
        com.mundosdeaxelis.world.Scheduler.runLater(14, () -> {
            Fx.helix(sl, sparks, c.x, c.y - 0.4, c.z, 0.8, 2.2, 18, 1.6);
            Fx.dustAt(sl, rgb, 1.3F, c.x, c.y + 0.6, c.z, 16, 0.5);
            Fx.sound(sl, c.x, c.y, c.z, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.6F);
        });
        com.mundosdeaxelis.world.Scheduler.runLater(24, () -> {
            Vfx.flash(sl, c.add(0, 0.4, 0), rgb, 1.1, 10);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, c.x, c.y + 0.5, c.z, 24, 0.5, 0.5, 0.5, 0.12);
        });
    }

    /** Probabilidad de que cada apertura traiga, además del premio, la llave de la caja siguiente. */
    public static final float NEXT_KEY_CHANCE = 0.08F;

    private void clear() {
        start = -1;
        winner = "";
        opener = null;
        openerName = "";
        stripCache = null;
        sync();
    }

    /** Si el crate desaparece a media apertura, el premio cae al suelo. */
    public void dropPending(Level level, BlockPos pos) {
        if (!level.isClientSide && rolling()) {
            ItemStack st = CratePools.stack(kind(), winnerItem(), seed);
            if (!st.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, st));
            }
            start = -1;
            winner = "";
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        if (level.isClientSide) {
            com.mundosdeaxelis.client.CrateClient.tick(level, pos, state, be);
            return;
        }
        if (!be.rolling()) {
            return;
        }
        ServerLevel sl = (ServerLevel) level;
        long el = sl.getGameTime() - be.start;
        if (el < 0) {
            be.clear();
            return;
        }
        if (el >= CrateRoll.TOTAL) {
            if (el > CrateRoll.TOTAL + 20 * 60) {
                // el servidor estuvo apagado: se entrega sin ceremonia
                be.finish(sl);
                return;
            }
            be.finish(sl);
            return;
        }
        if (el == CrateRoll.ROLL) {
            be.reveal(sl);
        }
    }
}
