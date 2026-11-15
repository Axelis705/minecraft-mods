package com.casino.heist;

import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.data.CasinoStats;
import com.casino.entity.DealerEntity;
import com.casino.game.Prize;
import com.casino.net.CasinoNet;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El robo a la bóveda. Un robo vive en la memoria del servidor mientras dura (si el servidor se apaga, se cancela y el casino se
 * arregla solo al volver). Lo mueve la puerta de la bóveda ({@link VaultDoorBE}) en cada tick; los bloques y guardias le preguntan con
 * {@link #at}.
 *
 * <p>Fases: SIGILO (puerta de servicio con ganzúa, pistas, caja fuerte, lector de la escalera, láseres y los dos lectores) → APERTURA
 * (la puerta gira) → BÓVEDA (tiempo para saquear) → ESCAPE (la puerta se cierra, gas adentro, alarma y combate; los jugadores que
 * están en el casino pueden ayudar a seguridad) → TERMINADO (se arregla todo y la bóveda queda cerrada hasta el día siguiente).
 */
public final class HeistManager {
    public enum Phase { STEALTH, OPENING, VAULT, ESCAPE, OVER }

    public static final int OPEN_TICKS = 120;
    public static final int CLOSE_TICKS = 50;
    public static final int LOOT_TICKS = 2000;
    public static final int GAS_TICKS = 400;
    public static final int CAMERAS_OFF = 1800;
    public static final int BAG_SLOTS = 8;
    public static final long BAN_TICKS = 24000;
    public static final String BAN_KEY = "casino_heist_ban";
    /** Valor de un manotazo de cada carrito (fichas) y cuánto pesa en la mochila. */
    static final int[] GRAB_VALUE = {6000, 9000, 12000, 5000};
    static final int[] GRAB_SLOTS = {1, 2, 1, 1};

    private static final Map<String, Heist> HEISTS = new LinkedHashMap<>();

    private HeistManager() {
    }

    // ================================================================== datos de un robo

    public static final class Member {
        public final UUID id;
        public final String name;
        public long bag;
        public int slots;
        public int gold;
        public int diamonds;
        public boolean done;
        public boolean escaped;

        Member(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class Heist {
        public final String dim;
        public final BlockPos vault;
        public final int[] zone;
        public final Map<UUID, Member> crew = new LinkedHashMap<>();
        public final List<UUID> cops = new ArrayList<>();
        public final Map<UUID, Long> offered = new HashMap<>();
        public Phase phase = Phase.STEALTH;
        public float suspicion;
        public boolean alarm;
        public long lastSpot;
        public final int[] combo = new int[3];
        public int clues;
        public boolean serviceOpen;
        public boolean safeOpen;
        public boolean stairsOpen;
        public boolean gateOpen;
        public long camerasOffUntil;
        public long phaseAt;
        public long vaultEnd;
        public long overAt;
        public long started;
        long readerTime = -1000;
        BlockPos readerPos;
        UUID readerBy;
        final Map<UUID, BlockPos> pendingLoot = new HashMap<>();
        final Map<UUID, BlockPos> pendingGame = new HashMap<>();
        public final List<CompoundTag> deadGuards = new ArrayList<>();
        long nextReinforce;
        int robbersCaught;

        Heist(String dim, BlockPos vault, int[] zone) {
            this.dim = dim;
            this.vault = vault;
            this.zone = zone;
        }

        public boolean isCrew(Player p) {
            Member m = crew.get(p.getUUID());
            return m != null && !m.done;
        }

        public boolean isCop(Player p) {
            return cops.contains(p.getUUID());
        }

        /** ¿Los guardias atacan? (alarma o escape) */
        public boolean hostile() {
            return alarm || phase == Phase.ESCAPE;
        }

        public boolean camerasOff(long now) {
            return now < camerasOffUntil;
        }

        public List<ServerPlayer> crewOnline(Level level) {
            List<ServerPlayer> out = new ArrayList<>();
            MinecraftServer s = level.getServer();
            if (s == null) {
                return out;
            }
            for (Member m : crew.values()) {
                ServerPlayer p = s.getPlayerList().getPlayer(m.id);
                if (p != null && !m.done && p.isAlive() && p.level() == level) {
                    out.add(p);
                }
            }
            return out;
        }

        public boolean contains(BlockPos p, int grow) {
            return p.getX() >= zone[0] - grow && p.getX() <= zone[3] + grow && p.getY() >= zone[1] - grow && p.getY() <= zone[4] + grow
                    && p.getZ() >= zone[2] - grow && p.getZ() <= zone[5] + grow;
        }

        int aliveCount() {
            int n = 0;
            for (Member m : crew.values()) {
                if (!m.done) {
                    n++;
                }
            }
            return n;
        }
    }

    private static String key(Level level, BlockPos vault) {
        return level.dimension().toString() + "|" + vault.asLong();
    }

    @javax.annotation.Nullable
    public static Heist byVault(Level level, BlockPos vault) {
        return HEISTS.get(key(level, vault));
    }

    /** El robo en curso en el casino donde está esta posición (o null). */
    @javax.annotation.Nullable
    public static Heist at(Level level, BlockPos pos) {
        if (HEISTS.isEmpty() || level.isClientSide()) {
            return null;
        }
        String dim = level.dimension().toString();
        for (Heist h : HEISTS.values()) {
            if (h.dim.equals(dim) && h.contains(pos, 8)) {
                return h;
            }
        }
        return null;
    }

    @javax.annotation.Nullable
    public static Heist ofPlayer(ServerPlayer p) {
        for (Heist h : HEISTS.values()) {
            if (h.isCrew(p) || h.isCop(p)) {
                return h;
            }
        }
        return null;
    }

    // ================================================================== utilidades

    /** ¿Se ve de un punto al otro sin bloques de por medio? (who: una entidad cualquiera para el contexto de choque) */
    public static boolean clear(Level level, Vec3 from, Vec3 to, net.minecraft.world.entity.Entity who) {
        HitResult r = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who));
        return r.getType() == HitResult.Type.MISS || r.getLocation().distanceToSqr(to) < 0.04;
    }

    static int[] zoneOf(Level level, BlockPos pos) {
        MinecraftServer s = level.getServer();
        if (s != null) {
            CasinoData d = CasinoData.get(s);
            String dim = level.dimension().toString();
            List<int[]> boxes = d.zoneBoxes();
            for (int i = 0; i < boxes.size(); i++) {
                int[] b = boxes.get(i);
                if (d.zoneDims().get(i).equals(dim) && pos.getX() >= b[0] - 2 && pos.getX() <= b[3] + 2 && pos.getY() >= b[1] - 2
                        && pos.getY() <= b[4] + 2 && pos.getZ() >= b[2] - 2 && pos.getZ() <= b[5] + 2) {
                    return b.clone();
                }
            }
        }
        return new int[]{pos.getX() - 40, pos.getY() - 8, pos.getZ() - 40, pos.getX() + 40, pos.getY() + 60, pos.getZ() + 40};
    }

    static void msg(ServerPlayer p, Component c) {
        Plat.msg(p, c, false);
    }

    static void bar(ServerPlayer p, Component c) {
        Plat.msg(p, c, true);
    }

    static net.minecraft.network.chat.MutableComponent t(String k, Object... a) {
        return Component.translatable("casino.heist." + k, a);
    }

    static void sound(Level level, BlockPos pos, SoundEvent s, float vol, float pitch) {
        level.playSound(null, pos, s, SoundSource.BLOCKS, vol, pitch);
    }

    static void sound(Level level, Vec3 pos, SoundEvent s, float vol, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    public static MobEffectInstance slowness(int ticks, int amp) {
        //#if MC >= 26.1
        //$$ return new MobEffectInstance(MobEffects.SLOWNESS, ticks, amp);
        //#else
        return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp);
        //#endif
    }

    public static MobEffectInstance nausea(int ticks) {
        //#if MC >= 26.1
        //$$ return new MobEffectInstance(MobEffects.NAUSEA, ticks, 0);
        //#else
        return new MobEffectInstance(MobEffects.CONFUSION, ticks, 0);
        //#endif
    }

    /** Daño como porcentaje de la vida máxima (así quita lo mismo con más corazones de un modpack). */
    public static boolean hurtPct(LivingEntity target, net.minecraft.world.damagesource.DamageSource src, float pct) {
        float amount = Math.max(1F, target.getMaxHealth() * pct);
        //#if MC >= 1.21.2
        //$$ return target.level() instanceof ServerLevel sl && target.hurtServer(sl, src, amount);
        //#else
        return target.hurt(src, amount);
        //#endif
    }

    // ================================================================== veto (1 día de Minecraft)

    public static boolean banned(Player p) {
        if (Plat.isOp(p)) {
            return false; // los operadores pueden probar el robo las veces que quieran
        }
        long until = Nbt.l(p.getPersistentData(), BAN_KEY);
        return until > 0 && p.level().getGameTime() < until;
    }

    static void ban(ServerPlayer p) {
        p.getPersistentData().putLong(BAN_KEY, p.level().getGameTime() + BAN_TICKS);
    }

    public static Component banText(Player p) {
        long left = Nbt.l(p.getPersistentData(), BAN_KEY) - p.level().getGameTime();
        return t("banned", Math.max(1, left / 1200)).withStyle(ChatFormatting.RED);
    }

    // ================================================================== el contacto (salón VIP)

    /** Clic derecho al contacto: le cuenta el plan y, si se puede, le abre la pantalla para empezar. */
    public static void contact(ServerPlayer p, DealerEntity npc) {
        Level level = p.level();
        VaultDoorBE vault = VaultDoorBE.find(level, npc.blockPosition());
        int reason = 0;
        CasinoStats.Entry e = CasinoStats.get(Plat.server(p)).entry(p.getUUID());
        int lvl = e == null ? 0 : e.level();
        if (vault == null) {
            reason = 5;
        } else if (banned(p)) {
            reason = 2;
        } else if (lvl < 4 && !Plat.isOp(p)) {
            reason = 1;
        } else if (byVault(level, vault.getBlockPos()) != null) {
            reason = 3;
        } else if (!vault.ready(level) && !Plat.isOp(p)) {
            reason = 4;
        }
        List<String> crew = new ArrayList<>();
        crew.add(p.getGameProfile().getName());
        for (Player o : level.getEntitiesOfClass(Player.class, new AABB(npc.blockPosition()).inflate(8))) {
            if (o != p && !o.isSpectator() && !banned(o)) {
                crew.add(o.getGameProfile().getName());
            }
        }
        npc.say(DealerEntity.SAY + (reason == 0 ? 0 : 1));
        CasinoNet.sendTo(p, new HeistPlanPacket(reason, lvl, crew, npc.blockPosition()));
    }

    /** Empieza el robo (desde la pantalla del contacto). Entra al equipo quien esté a menos de 8 bloques del contacto. */
    public static void start(ServerPlayer leader, BlockPos npcPos) {
        Level level = leader.level();
        if (leader.distanceToSqr(Vec3.atCenterOf(npcPos)) > 100) {
            return;
        }
        VaultDoorBE vault = VaultDoorBE.find(level, npcPos);
        CasinoStats.Entry e = CasinoStats.get(Plat.server(leader)).entry(leader.getUUID());
        if (vault == null || banned(leader) || (e == null || e.level() < 4) && !Plat.isOp(leader) || byVault(level, vault.getBlockPos()) != null
                || !vault.ready(level) && !Plat.isOp(leader)) {
            return;
        }
        Heist h = new Heist(level.dimension().toString(), vault.getBlockPos(), zoneOf(level, vault.getBlockPos()));
        h.crew.put(leader.getUUID(), new Member(leader.getUUID(), leader.getGameProfile().getName()));
        for (Player o : level.getEntitiesOfClass(Player.class, new AABB(npcPos).inflate(8))) {
            if (o instanceof ServerPlayer sp && o != leader && !o.isSpectator() && !banned(o) && ofPlayer(sp) == null) {
                h.crew.put(o.getUUID(), new Member(o.getUUID(), o.getGameProfile().getName()));
            }
        }
        for (int i = 0; i < 3; i++) {
            h.combo[i] = 10 + level.getRandom().nextInt(90);
        }
        h.started = level.getGameTime();
        h.phaseAt = h.started;
        HEISTS.put(key(level, vault.getBlockPos()), h);
        vault.markDirty();
        for (ServerPlayer p : h.crewOnline(level)) {
            msg(p, t("start", h.crew.size()).withStyle(ChatFormatting.GOLD));
            msg(p, t("obj.door_hint").withStyle(ChatFormatting.GRAY));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 0.6F);
        }
        sync(level, h);
    }

    // ================================================================== sigilo: te ven, alarma, láseres

    public static void spot(Heist h, ServerPlayer p, float amount, Vec3 from) {
        if (h.phase != Phase.STEALTH || h.alarm) {
            return;
        }
        h.suspicion = Math.min(100F, h.suspicion + amount);
        h.lastSpot = p.level().getGameTime();
        if (h.suspicion >= 100F) {
            alarm(p.level(), h, "alarm.seen");
        }
    }

    /** Tocar un láser es la muerte (y si nadie había dado la alarma, suena). */
    public static void laser(Heist h, ServerPlayer p, BlockPos pos) {
        Level level = p.level();
        if (p.isCreative() || p.isSpectator()) {
            return;
        }
        sound(level, pos, SoundEvents.NOTE_BLOCK_PLING.value(), 1F, 2F);
        level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1F, 1.6F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
        bar(p, t("laser_kill").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        hurtPct(p, level.damageSources().magic(), 100F);
        if (h.phase == Phase.STEALTH && !h.alarm) {
            alarm(level, h, "alarm.laser");
        }
    }

    /** Suena la alarma: los guardias sacan las armas. */
    public static void alarm(Level level, Heist h, String why) {
        if (h.alarm) {
            return;
        }
        h.alarm = true;
        h.suspicion = 100F;
        h.nextReinforce = level.getGameTime() + 400;
        for (ServerPlayer p : h.crewOnline(level)) {
            msg(p, t(why).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        sound(level, h.vault, SoundEvents.BELL_BLOCK, 2F, 0.7F);
        offerCops(level, h);
        sync(level, h);
    }

    // ================================================================== bloques

    /** Puertas de hierro del casino: la de servicio se abre con ganzúa; la de la escalera con la tarjeta del gerente. */
    public static boolean useIronDoor(ServerPlayer p, BlockPos pos) {
        Level level = p.level();
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof DoorBlock) || st.getBlock() != Blocks.IRON_DOOR) {
            return false;
        }
        BlockPos lower = st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (!CasinoData.get(Plat.server(p)).inZone(level.dimension().toString(), pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        Heist h = at(level, pos);
        if (h == null || !h.isCrew(p)) {
            bar(p, t("staff_only").withStyle(ChatFormatting.RED));
            return true;
        }
        if (level.getBlockState(lower).getValue(DoorBlock.OPEN)) {
            return true;
        }
        if (readerNear(level, lower) != null) {
            bar(p, t("need_card").withStyle(ChatFormatting.YELLOW));
            return true;
        }
        h.pendingGame.put(p.getUUID(), lower);
        CasinoNet.sendTo(p, new HeistGamePacket(HeistGamePacket.LOCKPICK, lower, new int[]{h.alarm ? 1 : 0}));
        return true;
    }

    static BlockPos readerNear(Level level, BlockPos door) {
        for (BlockPos q : BlockPos.betweenClosed(door.offset(-2, -1, -2), door.offset(2, 2, 2))) {
            if (level.getBlockState(q).getBlock() instanceof HeistBlocks.Reader) {
                return q.immutable();
            }
        }
        return null;
    }

    static BlockPos doorNear(Level level, BlockPos reader) {
        for (BlockPos q : BlockPos.betweenClosed(reader.offset(-2, -2, -2), reader.offset(2, 1, 2))) {
            BlockState s = level.getBlockState(q);
            if (s.getBlock() == Blocks.IRON_DOOR && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                return q.immutable();
            }
        }
        return null;
    }

    static void setDoor(Level level, BlockPos lower, boolean open) {
        BlockState s = level.getBlockState(lower);
        if (s.getBlock() == Blocks.IRON_DOOR) {
            level.setBlock(lower, s.setValue(DoorBlock.OPEN, open), 10);
            BlockState u = level.getBlockState(lower.above());
            if (u.getBlock() == Blocks.IRON_DOOR) {
                level.setBlock(lower.above(), u.setValue(DoorBlock.OPEN, open), 10);
            }
            sound(level, lower, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 1F, 1F);
        }
    }

    public static void useClue(ServerPlayer p, BlockPos pos) {
        Heist h = at(p.level(), pos);
        if (h == null || !h.isCrew(p)) {
            bar(p, t("note_idle").withStyle(ChatFormatting.GRAY));
            return;
        }
        VaultDoorBE v = VaultDoorBE.at(p.level(), h.vault);
        int i = v == null ? 0 : v.noteIndex(pos);
        boolean isNew = (h.clues & (1 << i)) == 0;
        h.clues |= 1 << i;
        sound(p.level(), pos, SoundEvents.BOOK_PAGE_TURN, 1F, 1F);
        for (ServerPlayer o : h.crewOnline(p.level())) {
            msg(o, t("clue", i + 1, String.format("%02d", h.combo[i])).withStyle(isNew ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        sync(p.level(), h);
    }

    public static void useSafe(ServerPlayer p, BlockPos pos) {
        Level level = p.level();
        Heist h = at(level, pos);
        if (level.getBlockState(pos).getValue(HeistBlocks.OPEN)) {
            bar(p, t("safe_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (h == null || !h.isCrew(p)) {
            bar(p, t("safe_locked").withStyle(ChatFormatting.GRAY));
            return;
        }
        int[] known = new int[3];
        for (int i = 0; i < 3; i++) {
            known[i] = (h.clues & (1 << i)) != 0 ? h.combo[i] : -1;
        }
        h.pendingGame.put(p.getUUID(), pos);
        CasinoNet.sendTo(p, new HeistGamePacket(HeistGamePacket.SAFE, pos, known));
    }

    public static void useConsole(ServerPlayer p, BlockPos pos) {
        Heist h = at(p.level(), pos);
        BlockPos gate = doorNear(p.level(), pos);
        if (h == null || !h.isCrew(p)) {
            bar(p, t(gate != null ? "gate_idle" : "console_idle").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (gate != null) {
            // consola de la compuerta: se hackea para abrir la puerta de hierro de al lado
            if (p.level().getBlockState(gate).getValue(DoorBlock.OPEN)) {
                bar(p, t("gate_already").withStyle(ChatFormatting.GREEN));
                return;
            }
            h.pendingGame.put(p.getUUID(), pos);
            CasinoNet.sendTo(p, new HeistGamePacket(HeistGamePacket.HACK, pos, new int[]{1}));
            return;
        }
        if (h.camerasOff(p.level().getGameTime())) {
            bar(p, t("cams_already").withStyle(ChatFormatting.GREEN));
            return;
        }
        h.pendingGame.put(p.getUUID(), pos);
        CasinoNet.sendTo(p, new HeistGamePacket(HeistGamePacket.HACK, pos, new int[0]));
    }

    public static boolean hasCard(Player p) {
        for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
            if (s.is(HeistReg.KEYCARD.get())) {
                return true;
            }
        }
        return false;
    }

    public static void useReader(ServerPlayer p, BlockPos pos) {
        Level level = p.level();
        Heist h = at(level, pos);
        if (h == null || !h.isCrew(p)) {
            bar(p, t("staff_only").withStyle(ChatFormatting.RED));
            sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.6F);
            return;
        }
        if (!hasCard(p)) {
            bar(p, t("need_card").withStyle(ChatFormatting.YELLOW));
            sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.6F);
            return;
        }
        BlockState rs = level.getBlockState(pos);
        BlockPos door = doorNear(level, pos);
        if (door != null) {
            // lector de la escalera
            level.setBlock(pos, rs.setValue(HeistBlocks.LIT, true), 3);
            setDoor(level, door, true);
            h.stairsOpen = true;
            sound(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 1F, 1.6F);
            sync(level, h);
            return;
        }
        // lectores de la bóveda: con 2 o más en el equipo hay que pasar las dos tarjetas a la vez
        if (h.phase != Phase.STEALTH) {
            return;
        }
        long now = level.getGameTime();
        level.setBlock(pos, rs.setValue(HeistBlocks.LIT, true), 3);
        sound(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 1F, 1.6F);
        int alive = 0;
        for (ServerPlayer o : h.crewOnline(level)) {
            alive++;
        }
        boolean ok = alive <= 1;
        if (!ok && h.readerPos != null && !h.readerPos.equals(pos) && !p.getUUID().equals(h.readerBy) && now - h.readerTime <= 30) {
            ok = true;
        }
        if (!ok) {
            h.readerPos = pos.immutable();
            h.readerBy = p.getUUID();
            h.readerTime = now;
            bar(p, t("reader_wait").withStyle(ChatFormatting.YELLOW));
            VaultDoorBE v = VaultDoorBE.at(level, h.vault);
            if (v != null) {
                v.unlightLater(pos, now + 30);
            }
            return;
        }
        openVault(level, h);
    }

    public static void useVaultDoor(ServerPlayer p, BlockPos pos) {
        Heist h = at(p.level(), pos);
        bar(p, (h != null && h.isCrew(p) ? t("vault_hint") : t("vault_idle")).withStyle(ChatFormatting.GRAY));
    }

    public static void useCart(ServerPlayer p, BlockPos pos) {
        Level level = p.level();
        Heist h = at(level, pos);
        BlockState st = level.getBlockState(pos);
        if (h == null || !h.isCrew(p) || h.phase != Phase.VAULT) {
            bar(p, t("cart_idle").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (st.getValue(HeistBlocks.Cart.LEFT) <= 0) {
            bar(p, t("cart_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Member m = h.crew.get(p.getUUID());
        int kind = st.getValue(HeistBlocks.Cart.KIND).ordinal();
        if (m.slots + GRAB_SLOTS[kind] > BAG_SLOTS) {
            bar(p, t("bag_full").withStyle(ChatFormatting.RED));
            return;
        }
        h.pendingLoot.put(p.getUUID(), pos.immutable());
        CasinoNet.sendTo(p, new HeistGamePacket(HeistGamePacket.LOOT, pos, new int[]{kind, (int) Math.max(0, h.vaultEnd - level.getGameTime())}));
    }

    // ================================================================== resultados de los minijuegos

    /** Lo que el jugador hizo en un minijuego (o en la pantalla del contacto). */
    public static void action(ServerPlayer p, int action, BlockPos pos, int[] v) {
        Level level = p.level();
        if (action == HeistActionPacket.START) {
            start(p, pos);
            return;
        }
        if (action == HeistActionPacket.HELP) {
            joinCops(p);
            return;
        }
        Heist h = ofPlayer(p);
        if (h == null || !h.isCrew(p) || p.distanceToSqr(Vec3.atCenterOf(pos)) > 49) {
            return;
        }
        long now = level.getGameTime();
        switch (action) {
            case HeistActionPacket.LOCKPICK -> {
                if (!pos.equals(h.pendingGame.remove(p.getUUID()))) {
                    return;
                }
                if (v.length > 0 && v[0] == 1) {
                    setDoor(level, pos, true);
                    h.serviceOpen = true;
                    bar(p, t("door_open").withStyle(ChatFormatting.GREEN));
                    for (ServerPlayer o : h.crewOnline(level)) {
                        msg(o, t("obj.clues_hint").withStyle(ChatFormatting.GRAY));
                    }
                } else {
                    spot(h, p, 15F, p.position());
                    sound(level, pos, SoundEvents.CHAIN_BREAK, 1F, 1.4F);
                    bar(p, t("lockpick_fail").withStyle(ChatFormatting.RED));
                }
            }
            case HeistActionPacket.SAFE -> {
                if (!pos.equals(h.pendingGame.remove(p.getUUID()))) {
                    return;
                }
                if (v.length >= 3 && v[0] == h.combo[0] && v[1] == h.combo[1] && v[2] == h.combo[2]) {
                    BlockState st = level.getBlockState(pos);
                    if (st.getBlock() instanceof HeistBlocks.Safe) {
                        level.setBlock(pos, st.setValue(HeistBlocks.OPEN, true), 3);
                    }
                    h.safeOpen = true;
                    sound(level, pos, SoundEvents.IRON_TRAPDOOR_OPEN, 1F, 0.7F);
                    ItemStack cards = new ItemStack(HeistReg.KEYCARD.get(), Math.max(1, h.aliveCount()));
                    net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, cards);
                    for (ServerPlayer o : h.crewOnline(level)) {
                        msg(o, t("safe_open", p.getGameProfile().getName()).withStyle(ChatFormatting.GREEN));
                    }
                } else {
                    spot(h, p, 12F, p.position());
                    sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 1F, 0.5F);
                    bar(p, t("safe_wrong").withStyle(ChatFormatting.RED));
                }
            }
            case HeistActionPacket.HACK -> {
                if (!pos.equals(h.pendingGame.remove(p.getUUID()))) {
                    return;
                }
                BlockPos gate = doorNear(level, pos);
                if (v.length > 0 && v[0] == 1 && gate != null) {
                    setDoor(level, gate, true);
                    h.gateOpen = true;
                    for (ServerPlayer o : h.crewOnline(level)) {
                        msg(o, t("gate_open").withStyle(ChatFormatting.GREEN));
                    }
                    sound(level, pos, SoundEvents.BEACON_ACTIVATE, 1F, 1.4F);
                } else if (v.length > 0 && v[0] == 1) {
                    h.camerasOffUntil = now + CAMERAS_OFF;
                    for (ServerPlayer o : h.crewOnline(level)) {
                        msg(o, t("cams_off", CAMERAS_OFF / 20).withStyle(ChatFormatting.GREEN));
                    }
                    sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 1F, 1.5F);
                } else {
                    spot(h, p, 20F, p.position());
                    bar(p, t("hack_fail").withStyle(ChatFormatting.RED));
                }
            }
            case HeistActionPacket.LOOT -> {
                BlockPos want = h.pendingLoot.remove(p.getUUID());
                if (!pos.equals(want) || h.phase != Phase.VAULT) {
                    return;
                }
                BlockState st = level.getBlockState(pos);
                if (!(st.getBlock() instanceof HeistBlocks.Cart) || st.getValue(HeistBlocks.Cart.LEFT) <= 0) {
                    return;
                }
                int kind = st.getValue(HeistBlocks.Cart.KIND).ordinal();
                Member m = h.crew.get(p.getUUID());
                if (m.slots + GRAB_SLOTS[kind] > BAG_SLOTS) {
                    return;
                }
                int q = v.length > 0 ? Math.max(0, Math.min(2, v[0])) : 0;
                float mult = q == 2 ? 1.0F : q == 1 ? 0.75F : 0.4F;
                long value = (long) (GRAB_VALUE[kind] * mult);
                m.bag += value;
                m.slots += GRAB_SLOTS[kind];
                if (kind == HeistBlocks.Kind.GOLD.ordinal()) {
                    m.gold += 3 + q * 2;
                } else if (kind == HeistBlocks.Kind.DIAMOND.ordinal()) {
                    m.diamonds += 1 + q;
                }
                level.setBlock(pos, st.setValue(HeistBlocks.Cart.LEFT, st.getValue(HeistBlocks.Cart.LEFT) - 1), 3);
                Plat.swing(p);
                SoundEvent s = kind == 0 ? SoundEvents.BOOK_PAGE_TURN : kind == 1 ? SoundEvents.CHAIN_PLACE : kind == 2 ? SoundEvents.AMETHYST_BLOCK_CHIME
                        : SoundEvents.AMETHYST_BLOCK_HIT;
                sound(level, pos, s, 1F, 0.9F + q * 0.15F);
                if (level instanceof ServerLevel sl) {
                    ItemStack shown = new ItemStack(kind == 0 ? Items.PAPER : kind == 1 ? Items.GOLD_INGOT : kind == 2 ? Items.DIAMOND : Chips.itemFor(100).get());
                    //#if MC >= 26.1
                    //$$ sl.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.25, 0.15, 0.25, 0.06);
                    //#else
                    sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.25, 0.15, 0.25,
                            0.06);
                    //#endif
                }
                bar(p, t("grabbed", String.format("%,d", value), m.slots, BAG_SLOTS).withStyle(q == 2 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                sync(level, h);
            }
            default -> {
            }
        }
    }

    /** Cierra un minijuego sin terminarlo (se olvida lo pendiente). */
    public static void cancel(ServerPlayer p) {
        Heist h = ofPlayer(p);
        if (h != null) {
            h.pendingGame.remove(p.getUUID());
            h.pendingLoot.remove(p.getUUID());
        }
    }

    // ================================================================== la bóveda

    static void openVault(Level level, Heist h) {
        if (h.phase != Phase.STEALTH) {
            return;
        }
        h.phase = Phase.OPENING;
        h.phaseAt = level.getGameTime();
        VaultDoorBE v = VaultDoorBE.at(level, h.vault);
        if (v != null) {
            v.startOpen(level.getGameTime());
        }
        for (ServerPlayer p : h.crewOnline(level)) {
            msg(p, t("vault_opening").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        sync(level, h);
    }

    /** Lo mueve la puerta de la bóveda en cada tick. */
    static void tick(Level level, VaultDoorBE v, Heist h) {
        long now = level.getGameTime();
        // jugadores: muertos, desconectados, escapados
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        for (Member m : h.crew.values()) {
            if (m.done) {
                continue;
            }
            ServerPlayer p = server.getPlayerList().getPlayer(m.id);
            if (p == null || !p.isAlive() || p.level() != level) {
                fail(level, h, m, p);
                continue;
            }
            if (h.phase == Phase.ESCAPE && !h.contains(p.blockPosition(), 6)) {
                escaped(level, h, m, p);
            }
            if (h.phase == Phase.ESCAPE && now % 40 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        }
        switch (h.phase) {
            case STEALTH -> {
                if (!h.alarm && now - h.lastSpot > 40 && h.suspicion > 0) {
                    h.suspicion = Math.max(0F, h.suspicion - 0.35F);
                }
                if (now - h.started > 20 * 60 * 20) {
                    // 20 minutos sin llegar a la bóveda: se cancela
                    for (Member m : h.crew.values()) {
                        if (!m.done) {
                            m.done = true;
                            ServerPlayer p = server.getPlayerList().getPlayer(m.id);
                            if (p != null) {
                                msg(p, t("timeout").withStyle(ChatFormatting.RED));
                            }
                        }
                    }
                }
            }
            case OPENING -> {
                if (now - h.phaseAt >= OPEN_TICKS) {
                    h.phase = Phase.VAULT;
                    h.phaseAt = now;
                    h.vaultEnd = now + LOOT_TICKS;
                    for (ServerPlayer p : h.crewOnline(level)) {
                        msg(p, t("vault_open", LOOT_TICKS / 20).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                    }
                }
            }
            case VAULT -> {
                long left = h.vaultEnd - now;
                if (left == 600 || left == 200 || left == 100) {
                    for (ServerPlayer p : h.crewOnline(level)) {
                        bar(p, t("vault_closing", left / 20).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1F, 0.8F);
                    }
                }
                boolean allOut = now - h.phaseAt > 200;
                boolean anyLoot = false;
                for (ServerPlayer p : h.crewOnline(level)) {
                    if (v.inside(p.position())) {
                        allOut = false;
                    }
                    if (h.crew.get(p.getUUID()).bag > 0) {
                        anyLoot = true;
                    }
                }
                if (left <= 0 || allOut && anyLoot) {
                    closeVault(level, v, h);
                }
            }
            case ESCAPE -> {
                // gas dentro de la bóveda mientras está cerrada
                if (now - h.phaseAt < GAS_TICKS + CLOSE_TICKS && now - h.phaseAt > CLOSE_TICKS && now % 20 == 0) {
                    for (Player p : level.getEntitiesOfClass(Player.class, v.interior())) {
                        if (!p.isCreative() && !p.isSpectator()) {
                            hurtPct(p, level.damageSources().magic(), 0.14F);
                            p.addEffect(nausea(100));
                            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        }
                    }
                }
                if (now - h.phaseAt == GAS_TICKS + CLOSE_TICKS) {
                    v.setGas(false);
                }
                // refuerzos cada 20 segundos: vuelven los guardias caídos
                if (now >= h.nextReinforce) {
                    h.nextReinforce = now + 400;
                    GuardAI.reinforce(level, h);
                }
                if (now % 200 == 0) {
                    offerCops(level, h);
                }
            }
            default -> {
            }
        }
        if (h.phase != Phase.OVER && h.aliveCount() == 0) {
            h.phase = Phase.OVER;
            h.overAt = now;
            v.setCooldown(level);
            for (UUID c : h.cops) {
                ServerPlayer p = server.getPlayerList().getPlayer(c);
                if (p != null) {
                    msg(p, t("cops_over").withStyle(ChatFormatting.AQUA));
                }
            }
        }
        if (h.phase == Phase.OVER && now - h.overAt >= 200) {
            HEISTS.remove(key(level, h.vault));
            v.reset(level, h);
            for (Member m : h.crew.values()) {
                ServerPlayer p = server.getPlayerList().getPlayer(m.id);
                if (p != null) {
                    removeCards(p);
                    CasinoNet.sendTo(p, HeistSyncPacket.none());
                }
            }
            for (UUID c : h.cops) {
                ServerPlayer p = server.getPlayerList().getPlayer(c);
                if (p != null) {
                    CasinoNet.sendTo(p, HeistSyncPacket.none());
                }
            }
            return;
        }
        if (now % 10 == 0) {
            sync(level, h);
        }
    }

    static void closeVault(Level level, VaultDoorBE v, Heist h) {
        long now = level.getGameTime();
        h.phase = Phase.ESCAPE;
        h.phaseAt = now;
        v.startClose(now);
        v.setGas(true);
        h.alarm = true;
        h.nextReinforce = now + 200;
        for (ServerPlayer p : h.crewOnline(level)) {
            msg(p, t("escape").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            if (v.inside(p.position())) {
                msg(p, t("gas").withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        sound(level, h.vault, SoundEvents.BELL_BLOCK, 2F, 0.6F);
        offerCops(level, h);
        sync(level, h);
    }

    static void escaped(Level level, Heist h, Member m, ServerPlayer p) {
        m.done = true;
        m.escaped = true;
        p.removeEffect(MobEffects.GLOWING);
        List<ItemStack> items = new ArrayList<>(Chips.stacksFor((int) Math.min(Integer.MAX_VALUE, m.bag)));
        if (m.gold > 0) {
            items.add(new ItemStack(Items.GOLD_INGOT, m.gold));
        }
        if (m.diamonds > 0) {
            items.add(new ItemStack(Items.DIAMOND, m.diamonds));
        }
        Prize prize = new Prize(t("prize"), items, m.bag > 0 ? 10 : 0, -1, true);
        prize.deliver(p);
        if (m.bag > 0) {
            CasinoNet.sendTo(p, new com.casino.net.WheelWinPacket(prize));
        }
        msg(p, t("escaped", String.format("%,d", m.bag)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
        removeCards(p);
        CasinoNet.sendTo(p, HeistSyncPacket.none());
        for (ServerPlayer o : h.crewOnline(level)) {
            msg(o, t("crew_escaped", m.name).withStyle(ChatFormatting.GREEN));
        }
    }

    static void fail(Level level, Heist h, Member m, ServerPlayer p) {
        m.done = true;
        m.bag = 0;
        m.gold = 0;
        m.diamonds = 0;
        MinecraftServer server = level.getServer();
        if (p != null) {
            ban(p);
            removeCards(p);
            msg(p, t("caught").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            CasinoNet.sendTo(p, HeistSyncPacket.none());
            if (h.phase == Phase.ESCAPE || h.alarm) {
                rewardCops(level, h, p);
            }
        } else if (server != null) {
            // desconectado: el veto se le pone al volver
            h.offered.put(m.id, -1L);
        }
        for (ServerPlayer o : h.crewOnline(level)) {
            msg(o, t("crew_down", m.name).withStyle(ChatFormatting.RED));
        }
    }

    static void removeCards(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(HeistReg.KEYCARD.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    // ================================================================== ayudar a seguridad

    static void offerCops(Level level, Heist h) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() != level || h.crew.containsKey(p.getUUID()) || h.isCop(p) || banned(p) || !h.contains(p.blockPosition(), 0)) {
                continue;
            }
            Long last = h.offered.get(p.getUUID());
            if (last != null && now - last < 1200) {
                continue;
            }
            h.offered.put(p.getUUID(), now);
            CasinoNet.sendTo(p, new HeistOfferPacket(h.aliveCount()));
        }
    }

    static void joinCops(ServerPlayer p) {
        Heist h = at(p.level(), p.blockPosition());
        if (h == null || !h.hostile() || h.crew.containsKey(p.getUUID()) || h.isCop(p) || banned(p)) {
            return;
        }
        h.cops.add(p.getUUID());
        msg(p, t("cop_joined").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1F, 1F);
        sync(p.level(), h);
    }

    /** Cayó un ladrón: premio para quien lo atrapó y algo para los demás de seguridad. */
    static void rewardCops(Level level, Heist h, ServerPlayer robber) {
        MinecraftServer server = level.getServer();
        if (server == null || h.cops.isEmpty()) {
            return;
        }
        h.robbersCaught++;
        LivingEntity killer = robber.getKillCredit();
        for (UUID c : h.cops) {
            ServerPlayer cop = server.getPlayerList().getPlayer(c);
            if (cop == null) {
                continue;
            }
            boolean main = killer == cop;
            int amount = main ? 4000 : 1000;
            Chips.give(cop, amount);
            msg(cop, t(main ? "cop_reward" : "cop_assist", robber.getGameProfile().getName(), String.format("%,d", amount))
                    .withStyle(ChatFormatting.AQUA));
            level.playSound(null, cop.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }

    // ================================================================== lo que ve cada jugador

    static void sync(Level level, Heist h) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        long now = level.getGameTime();
        int[] known = new int[3];
        for (int i = 0; i < 3; i++) {
            known[i] = (h.clues & (1 << i)) != 0 ? h.combo[i] : -1;
        }
        int obj;
        if (h.phase == Phase.STEALTH) {
            obj = !h.serviceOpen ? 0 : !h.safeOpen ? (Integer.bitCount(h.clues) < 3 ? 1 : 2) : !h.stairsOpen ? 3 : !h.gateOpen ? 4 : 5;
        } else {
            obj = h.phase == Phase.OPENING ? 6 : h.phase == Phase.VAULT ? 7 : 8;
        }
        int timer = h.phase == Phase.VAULT ? (int) Math.max(0, h.vaultEnd - now)
                : h.camerasOff(now) ? -(int) (h.camerasOffUntil - now) : 0;
        for (Member m : h.crew.values()) {
            ServerPlayer p = server.getPlayerList().getPlayer(m.id);
            if (p != null && !m.done) {
                CasinoNet.sendTo(p, new HeistSyncPacket(1, h.phase.ordinal(), obj, h.suspicion, h.alarm, timer, known, m.bag, m.slots, BAG_SLOTS,
                        h.aliveCount()));
            }
        }
        for (UUID c : h.cops) {
            ServerPlayer p = server.getPlayerList().getPlayer(c);
            if (p != null) {
                CasinoNet.sendTo(p, new HeistSyncPacket(2, h.phase.ordinal(), 9, 0, true, 0, new int[]{-1, -1, -1}, 0, 0, 0, h.aliveCount()));
            }
        }
    }

    /** Al entrar al mundo: si se desconectó a mitad de un robo, le toca el veto. */
    public static void onLogin(ServerPlayer p) {
        for (Heist h : HEISTS.values()) {
            Long v = h.offered.get(p.getUUID());
            if (v != null && v == -1L) {
                h.offered.remove(p.getUUID());
                ban(p);
                msg(p, t("caught").withStyle(ChatFormatting.RED));
            }
        }
    }

    /** Guardia de bóveda que cayó: se anota para que vuelva al reiniciar (o como refuerzo). */
    public static void guardDied(DealerEntity g) {
        Heist h = at(g.level(), g.blockPosition());
        if (h != null) {
            h.deadGuards.add(GuardAI.spawnData(g));
        }
    }

    /** ¿Hay un robo en esta bóveda? (para la puerta, al cargar). */
    public static boolean active(Level level, BlockPos vault) {
        return byVault(level, vault) != null;
    }

    /** Si se rompe o descarga la puerta: se olvida el robo. */
    static void drop(Level level, BlockPos vault) {
        HEISTS.remove(key(level, vault));
    }

}
