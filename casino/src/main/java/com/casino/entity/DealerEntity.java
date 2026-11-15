package com.casino.entity;

import com.casino.plat.Plat;
import com.casino.plat.Nbt;
import com.casino.net.CasinoNet;
import com.casino.net.OpenBlackjackPacket;
import com.casino.net.OpenPokerPacket;
import com.casino.net.OpenRoulettePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * El crupier: un personaje fijo y invulnerable que se queda detrás de su mesa de blackjack, mira al jugador más cercano y reparte
 * (mueve el brazo). Al hacerle clic derecho se abre la mesa. Lo coloca la mesa sola y lo quita cuando se rompe la mesa.
 */
public class DealerEntity extends Mob {
    /** Qué mesa atiende: 0 = blackjack, 1 = ruleta (cambia la ropa y qué panel se abre). */
    public static final int GAME_BLACKJACK = 0;
    public static final int GAME_ROULETTE = 1;
    public static final int GAME_POKER = 2;
    /** No atiende mesa: es el guardia de la zona VIP (junto al elevador). */
    public static final int GAME_GUARD = 3;
    /** No atiende mesa: es el bartender de la barra. Con clic derecho te sirve un cóctel de la casa (da Suerte un rato). */
    public static final int GAME_BARTENDER = 4;
    /** No atiende mesa: es el anfitrión de la ruleta de la suerte. Al hablarle te pregunta si quieres girar y la gira por ti. */
    public static final int GAME_WHEEL = 5;
    /** Cliente del casino: pasea, se sienta, ve las mesas y platica. Tiene una de {@link #PATRON_SKINS} pieles (y su nombre). */
    public static final int GAME_PATRON = 6;
    public static final int PATRON_SKINS = 12;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GAME = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.INT);
    /** Animación que está haciendo: (número de vez × 100) + cuál (ver {@link DealerAnim}); así el cliente nota cuando empieza otra igual. */
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.INT);
    /** La mesa (o ruleta) que atiende, también en el cliente: las animaciones siguen las cartas, la bola y la rueda de verdad. */
    private static final EntityDataAccessor<BlockPos> TABLE = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.BLOCK_POS);

    /** Consejo que está diciendo ahora (-1 = ninguno): el crupier levanta la mano y sobre su cabeza sale un globo con el texto. */
    private static final EntityDataAccessor<Integer> TIP = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.INT);
    public static final int TIPS_ROULETTE = 12;
    public static final int TIPS_BLACKJACK = 10;
    public static final int TIPS_POKER = 10;
    public static final int TIPS_GUARD = 6;
    public static final int TIPS_BARTENDER = 10;
    public static final int TIPS_WHEEL = 8;
    public static final int TIPS_PATRON = 12;
    /** Lo que dice al hablarle (no es un consejo al azar): el número es 100 + la frase ("say_N" en los textos). */
    public static final int SAY = 100;
    /** Cuánto dura el globo en el aire (el cliente lo usa para desvanecerlo al final). */
    public static final int TIP_TICKS = 180;
    /** Cóctel de la casa: Suerte por 3 minutos, uno por jugador cada 60 segundos. */
    private static final int DRINK_TICKS = 3600;
    private static final int DRINK_COOLDOWN = 1200;
    private final java.util.Map<java.util.UUID, Long> served = new java.util.HashMap<>();

    /** Solo en el cliente: ticks desde que empezó el globo actual (para escribir el texto letra por letra y animarlo). */
    public int tipAge;
    private int lastTip = -1;
    /** Solo en el cliente: la animación actual y cuándo empezó (tickCount). */
    public int animId;
    public int animStart;
    /** Solo en el cliente (1.20/1.21.1): lo último que calculó el dibujante para las manos (DealerMotion). */
    public Object clientMotion;
    private int lastAnim;
    // servidor: contador de animaciones, hasta cuándo dura la actual y a quién ya saludó (para no saludar a cada rato)
    private int animSeq;
    private int animUntil;
    private int nextFidget = 300 + (int) (Math.random() * 600);
    private final java.util.Map<java.util.UUID, Integer> greeted = new java.util.HashMap<>();
    /** Anfitrión: a quién le va a girar la ruleta y en qué tick (cuando baja el brazo). */
    @Nullable
    private java.util.UUID spinFor;
    private int spinAt;
    /** Bartender: a quién le está sirviendo y en qué tick le llega el vaso. */
    @Nullable
    private java.util.UUID serveFor;
    private int serveAt;

    @Nullable
    private BlockPos table;
    private int tipLeft;
    private int nextTip = 400 + (int) (Math.random() * 400);

    public DealerEntity(EntityType<? extends DealerEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        // los clientes no se meten a la alberca
        //#if MC >= 1.20.5
        //$$ setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
        //#else
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, -1.0F);
        //#endif
        Plat.invulnerable(this);
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
    //$$     super.defineSynchedData(b);
    //$$     b.define(GAME, GAME_BLACKJACK);
    //$$     b.define(TIP, -1);
    //$$     b.define(ANIM, 0);
    //$$     b.define(TABLE, BlockPos.ZERO);
    //$$     b.define(VARIANT, 0);
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GAME, GAME_BLACKJACK);
        this.entityData.define(TIP, -1);
        this.entityData.define(ANIM, 0);
        this.entityData.define(TABLE, BlockPos.ZERO);
        this.entityData.define(VARIANT, 0);
    }
    //#endif

    /** Número de vez de la animación actual (cliente): sirve para variar (por ejemplo, de dónde saca la botella el bartender). */
    public int animSeq() {
        return lastAnim / 100;
    }

    /** Empieza una animación (servidor). */
    public void anim(int a) {
        animSeq = (animSeq + 1) % 20000;
        this.entityData.set(ANIM, animSeq * 100 + a);
        animUntil = tickCount + DealerAnim.duration(a);
    }

    /** ¿Está a la mitad de una animación? (servidor) */
    public boolean animBusy() {
        return tickCount < animUntil;
    }

    public int getTip() {
        return this.entityData.get(TIP);
    }

    /** Dice algo ahora mismo (consejo o respuesta), con el sonidito y el globo. */
    public void say(int tip) {
        this.entityData.set(TIP, tip);
        tipLeft = TIP_TICKS;
        nextTip = Math.max(nextTip, 600);
        level().playSound(null, getX(), getY() + 1.6, getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.3F);
    }

    /** Cada tanto, si hay alguien cerca, dice un consejo al azar de su juego. */
    private void tickTips() {
        if (tipLeft > 0) {
            if (--tipLeft == 0) {
                this.entityData.set(TIP, -1);
            }
            return;
        }
        if (--nextTip > 0) {
            return;
        }
        nextTip = 900 + random.nextInt(900); // entre 45 y 90 segundos
        if (level().getNearestPlayer(this, 8.0) == null) {
            nextTip = 200;
            return;
        }
        int g = getGame();
        int n = g == GAME_ROULETTE ? TIPS_ROULETTE : g == GAME_POKER ? TIPS_POKER : g == GAME_GUARD ? TIPS_GUARD
                : g == GAME_BARTENDER ? TIPS_BARTENDER : g == GAME_WHEEL ? TIPS_WHEEL : g == GAME_PATRON ? TIPS_PATRON : TIPS_BLACKJACK;
        int t = random.nextInt(n);
        say(t);
        nextTip = 900 + random.nextInt(900);
    }

    public int getGame() {
        return this.entityData.get(GAME);
    }

    public void setGame(int game) {
        this.entityData.set(GAME, game);
        // los clientes caminan; los demás se quedan en su lugar
        setNoAi(game != GAME_PATRON);
    }

    public boolean isPatron() {
        return getGame() == GAME_PATRON;
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, Math.floorMod(v, PATRON_SKINS));
    }

    /** Cliente: de dónde no se aleja. */
    @Nullable
    private BlockPos home;

    @Nullable
    public BlockPos home() {
        return home;
    }

    public void setHome(BlockPos p) {
        home = p;
    }

    /** Cliente con copa en la mano (uno de cada tres). */
    public boolean holdsDrink() {
        return isPatron() && getVariant() % 3 == 0;
    }

    @Override
    protected void registerGoals() {
        PatronAI.register(this, this.goalSelector);
    }

    public void setTable(BlockPos pos) {
        this.table = pos;
        this.entityData.set(TABLE, pos == null ? BlockPos.ZERO : pos);
    }

    /** La mesa que atiende, vista desde el cliente (null si no tiene). */
    @Nullable
    public BlockPos clientTable() {
        BlockPos p = this.entityData.get(TABLE);
        return BlockPos.ZERO.equals(p) ? null : p;
    }

    @Nullable
    public BlockPos getTable() {
        return table;
    }

    @Override
    public boolean isPushable() {
        return isPatron();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // El cuerpo siempre mira hacia donde mira la mesa; solo la cabeza sigue al jugador más cercano.
        float base = getYRot();
        if (!isPatron()) {
            yBodyRot = base;
            yBodyRotO = base;
        }
        if (level().isClientSide()) {
            int t = getTip();
            if (t != lastTip) {
                lastTip = t;
                tipAge = 0;
            } else {
                tipAge++;
            }
            int a = this.entityData.get(ANIM);
            if (a != lastAnim) {
                lastAnim = a;
                animId = a % 100;
                animStart = tickCount;
            }
            if (animId != 0 && tickCount - animStart > DealerAnim.duration(animId)) {
                animId = 0;
            }
            // el chorrito al servir: cae de la botella al vaso
            int st = tickCount - animStart;
            if (animId == DealerAnim.SERVE && st >= 23 && st <= 35 && st % 2 == 0) {
                double yaw = Math.toRadians(yBodyRot);
                double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
                double px = getX() + fx * 0.45 - rx * 0.06, pz = getZ() + fz * 0.45 - rz * 0.06;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_HONEY, px, getY() + 1.36, pz, 0, -0.05, 0);
            }
        } else {
            tickTips();
            if (tickCount % 10 == 0) {
                greet();
            }
            fidget();
            if (getGame() == GAME_WHEEL) {
                tickHost();
            }
            if (serveFor != null && tickCount >= serveAt) {
                handOver();
            }
            Player p = isPatron() ? null : level().getNearestPlayer(this, 7.0);
            if (isPatron()) {
                // el cuerpo y la cabeza los manejan sus ganas de pasear y de mirar
            } else if (p != null) {
                double dx = p.getX() - getX();
                double dz = p.getZ() - getZ();
                double dy = p.getEyeY() - getEyeY();
                float want = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                float rel = Mth.clamp(Mth.wrapDegrees(want - base), -70.0F, 70.0F);
                yHeadRot = base + rel;
                setXRot(Mth.clamp((float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)), -30.0F, 30.0F));
            } else {
                yHeadRot = base;
                setXRot(0.0F);
            }
        }
    }

    /** De vez en cuando, sin que nadie le pida nada, hace algo de su oficio (barajar, acomodar fichas, secar un vaso...). */
    private void fidget() {
        if (--nextFidget > 0) {
            return;
        }
        nextFidget = 400 + random.nextInt(800);
        if (animBusy() || getTip() >= 0 || level().getNearestPlayer(this, 16.0) == null) {
            return;
        }
        int a = switch (getGame()) {
            case GAME_BLACKJACK, GAME_POKER -> random.nextInt(3) == 0 ? DealerAnim.CHIP : DealerAnim.SHUFFLE;
            case GAME_ROULETTE -> DealerAnim.CHIP;
            case GAME_BARTENDER -> DealerAnim.SHAKER;
            case GAME_WHEEL -> DealerAnim.PRESENT;
            case GAME_GUARD -> DealerAnim.EARPIECE;
            case GAME_PATRON -> holdsDrink() ? DealerAnim.SIP : random.nextInt(3) == 0 ? DealerAnim.NOD : DealerAnim.NONE;
            default -> DealerAnim.NONE;
        };
        if (a != DealerAnim.NONE) {
            anim(a);
        }
    }

    /** Saluda con la mano a quien llega (si no lo había visto en los últimos 2 minutos). */
    private void greet() {
        Player p = level().getNearestPlayer(this, 4.5);
        if (p == null) {
            return;
        }
        if (greeted.size() > 64) {
            greeted.clear();
        }
        Integer last = greeted.put(p.getUUID(), tickCount);
        if (last != null && tickCount - last < 2400 || animBusy()) {
            return;
        }
        if (isPatron() && random.nextInt(3) != 0) {
            return; // los clientes no saludan a todo el que pasa
        }
        anim(getGame() == GAME_GUARD ? DealerAnim.NOD : DealerAnim.WAVE);
        if (getGame() == GAME_WHEEL && getTip() < 0) {
            say(SAY + 5);
        }
    }

    // ------------------------------------------------------------------ anfitrión de la ruleta de la suerte

    @Nullable
    private com.casino.block.LuckyWheelBE wheel() {
        if (table != null && level().getBlockEntity(table) instanceof com.casino.block.LuckyWheelBE w) {
            return w;
        }
        return null;
    }

    private void tickHost() {
        if (table == null && tickCount % 40 == 5) {
            // la plantilla lo pone junto a la ruleta: la busca alrededor y se queda con su bloque principal
            BlockPos c = blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 4, 6))) {
                if (level().getBlockEntity(q) instanceof com.casino.block.LuckyWheelBE w) {
                    setTable(q.immutable());
                    standByWheel(w);
                    break;
                }
            }
        }
        if (spinFor != null && tickCount >= spinAt) {
            com.casino.block.LuckyWheelBE w = wheel();
            ServerPlayer p = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(spinFor);
            spinFor = null;
            if (w != null && p != null) {
                w.hostSpin(p);
            }
        }
    }

    /** Se para pegado a la orilla de la rueda (del lado donde está), para alcanzarla con la mano al jalarla. */
    private void standByWheel(com.casino.block.LuckyWheelBE w) {
        net.minecraft.world.phys.Vec3 hub = w.hub();
        Direction f = w.facing();
        Direction r = f.getCounterClockWise();
        double side = (getX() - hub.x) * r.getStepX() + (getZ() - hub.z) * r.getStepZ() >= 0 ? 1 : -1;
        double x = hub.x + r.getStepX() * 1.95 * side;
        double z = hub.z + r.getStepZ() * 1.95 * side;
        if (Math.abs(x - getX()) + Math.abs(z - getZ()) < 1.5) {
            float yaw = f.toYRot();
            this.moveTo(x, getY(), z, yaw, 0.0F);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        }
    }

    /** Lo que el anfitrión contesta al hablarle: 0 = ¿quieres girar?, 1 = ya giraste hoy, 2 = la ruleta está girando. */
    public int hostMode(ServerPlayer p) {
        com.casino.block.LuckyWheelBE w = wheel();
        if (w == null || w.spinning || spinFor != null) {
            return 2;
        }
        return com.casino.data.CasinoData.get(Plat.server(p)).canSpin(p.getUUID()) ? 0 : 1;
    }

    private void talkHost(ServerPlayer p) {
        int mode = hostMode(p);
        java.time.Duration d = com.casino.data.CasinoData.untilReset();
        com.casino.block.LuckyWheelBE w = wheel();
        String prize = w == null ? "" : w.podium.getCount() + " x " + w.podium.getHoverName().getString();
        CasinoNet.sendTo(p, new com.casino.net.WheelDialogPacket(getId(), mode, (int) d.toHours(), d.toMinutesPart(), prize));
        if (mode == 0) {
            anim(DealerAnim.PRESENT);
            say(SAY);
        } else if (mode == 1) {
            anim(DealerAnim.SHAKE);
            say(SAY + 2);
        } else {
            anim(DealerAnim.STOP);
            say(SAY + 6);
        }
    }

    /** El jugador dijo que sí: el anfitrión levanta el brazo y al bajarlo (medio segundo después) la ruleta empieza a girar. */
    public void hostAccept(ServerPlayer p) {
        if (hostMode(p) != 0 || p.distanceToSqr(this) > 10 * 10) {
            if (hostMode(p) == 1) {
                anim(DealerAnim.SHAKE);
                say(SAY + 2);
            }
            return;
        }
        com.casino.data.CasinoData.get(Plat.server(p)).markSpin(p.getUUID());
        spinFor = p.getUUID();
        spinAt = tickCount + 14;
        anim(DealerAnim.PULL);
        say(SAY + 1);
    }

    /** La ruleta se detuvo: festeja con el jugador. */
    public void hostCelebrate(boolean big) {
        anim(big ? DealerAnim.CHEER : DealerAnim.CLAP);
        say(SAY + (big ? 3 : 4));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (getGame() == GAME_WHEEL) {
            if (!level().isClientSide() && player instanceof ServerPlayer sp) {
                talkHost(sp);
            }
            return Plat.ok(level().isClientSide());
        }
        if (isPatron()) {
            if (!level().isClientSide()) {
                // voltea a verte y te dice algo
                getNavigation().stop();
                getLookControl().setLookAt(player, 30.0F, 30.0F);
                say(SAY + random.nextInt(4));
                anim(random.nextBoolean() ? DealerAnim.WAVE : DealerAnim.NOD);
            }
            return Plat.ok(level().isClientSide());
        }
        if (!level().isClientSide() && getGame() == GAME_GUARD && player instanceof ServerPlayer sp) {
            int tier = com.casino.item.VipCardItem.tier(sp);
            Plat.msg(sp, net.minecraft.network.chat.Component.translatable("casino.guard.say_" + Math.min(2, tier)).withStyle(net.minecraft.ChatFormatting.GOLD), false);
            say(SAY + Math.min(2, tier));
            anim(tier > 0 ? DealerAnim.PRESENT : DealerAnim.STOP);
            return Plat.ok(false);
        }
        if (!level().isClientSide() && getGame() == GAME_BARTENDER && player instanceof ServerPlayer sp) {
            serve(sp);
            return Plat.ok(false);
        }
        if (getGame() == GAME_BARTENDER || getGame() == GAME_GUARD) {
            return Plat.ok(true);
        }
        if (!level().isClientSide() && table != null && player instanceof ServerPlayer sp) {
            if (!animBusy()) {
                anim(DealerAnim.NOD);
            }
            if (getGame() == GAME_POKER) {
                CasinoNet.sendTo(sp, new OpenPokerPacket(table));
            } else if (getGame() == GAME_ROULETTE) {
                CasinoNet.sendTo(sp, new OpenRoulettePacket(table));
            } else {
                CasinoNet.sendTo(sp, new OpenBlackjackPacket(table));
            }
        }
        return Plat.ok(level().isClientSide());
    }

    /** El bartender te sirve el cóctel de la casa: Suerte por 3 minutos (uno cada minuto por jugador). */
    private void serve(ServerPlayer sp) {
        long now = level().getGameTime();
        Long last = served.get(sp.getUUID());
        if (last != null && now - last < DRINK_COOLDOWN) {
            say(SAY + 3);
            anim(DealerAnim.SHAKE);
            return;
        }

        if (serveFor != null) {
            say(SAY + 3);
            return;
        }
        served.put(sp.getUUID(), now);
        // primero lo prepara (saca botella y vaso, sirve) y el efecto llega cuando te da el vaso
        anim(DealerAnim.SERVE);
        serveFor = sp.getUUID();
        serveAt = tickCount + DealerAnim.SERVE_HANDOVER;
        level().playSound(null, getX(), getY() + 1, getZ(), net.minecraft.sounds.SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, 1.2F);
        say(SAY + random.nextInt(3));
    }

    /** Termina el servicio: el vaso llega a la mano del jugador. */
    private void handOver() {
        java.util.UUID id = serveFor;
        serveFor = null;
        ServerPlayer sp = level().getServer() == null || id == null ? null : level().getServer().getPlayerList().getPlayer(id);
        if (sp == null || sp.distanceToSqr(this) > 8 * 8) {
            return;
        }
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK, DRINK_TICKS, 0));
        com.casino.data.CasinoStats.drank(sp);
        //#if MC >= 1.21.2
        //$$ level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        //#else
        level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        //#endif
        Plat.msg(sp, net.minecraft.network.chat.Component.translatable("casino.bartender.served").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
    }

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$     super.addAdditionalSaveData(out);
    //$$     out.putInt("Game", getGame());
    //$$     out.putInt("Variant", getVariant());
    //$$     if (home != null) {
    //$$         out.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
    //$$     }
    //$$     if (table != null) {
    //$$         out.putIntArray("Table", new int[]{table.getX(), table.getY(), table.getZ()});
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$     super.readAdditionalSaveData(in);
    //$$     setGame(in.getIntOr("Game", 0));
    //$$     setVariant(in.getIntOr("Variant", 0));
    //$$     int[] hm = in.getIntArray("Home").orElse(new int[0]);
    //$$     home = hm.length == 3 ? new BlockPos(hm[0], hm[1], hm[2]) : null;
    //$$     int[] t = in.getIntArray("Table").orElse(new int[0]);
    //$$     setTable(t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null);
    //$$ }
    //#else
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Game", getGame());
        tag.putInt("Variant", getVariant());
        if (home != null) {
            tag.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
        }
        if (table != null) {
            tag.putIntArray("Table", new int[]{table.getX(), table.getY(), table.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGame(Nbt.i(tag, "Game"));
        setVariant(Nbt.i(tag, "Variant"));
        int[] hm = Nbt.ia(tag, "Home");
        home = hm.length == 3 ? new BlockPos(hm[0], hm[1], hm[2]) : null;
        int[] t = Nbt.ia(tag, "Table");
        setTable(t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null);
    }
    //#endif
}
