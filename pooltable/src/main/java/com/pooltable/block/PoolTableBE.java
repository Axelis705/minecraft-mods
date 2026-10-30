package com.pooltable.block;

import com.pooltable.game.PoolPhysics;
import com.pooltable.game.PoolRules;
import com.pooltable.net.PoolNet;
import com.pooltable.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * El corazón de la mesa (vive en la parte 0,0): las 16 bolas con su física, quién juega (de 1 a 6 jugadores), el modo de juego y sus reglas
 * ({@link PoolRules}). El servidor manda; el cliente recibe las posiciones cada tick mientras algo se mueve y las dibuja suavizadas.
 */
public class PoolTableBE extends BlockEntity {
    /** Alto de la superficie del paño sobre el bloque (14/16). */
    public static final double FELT_Y = 14.0 / 16.0;
    /** Alto de la bandeja de abajo, donde se acumulan las bolas que caen (3/16). */
    public static final double SHELF_Y = 3.0 / 16.0;
    /** Dónde se alinean las bolas caídas en la bandeja: contra el tope del lado del triángulo (u fijo, v repartido a lo ancho). */
    public static final double TRAY_U = 5.14;
    public static final double TRAY_V0 = -0.179;
    public static final double TRAY_DV = 2.857 / 14.0;

    /** Una bola cayendo por una tronera (solo cliente, para animar): cae al hoyo y rueda por la bandeja hasta su sitio. */
    public static final class Drop {
        public final int ball;
        public final long start;
        public final double su;
        public final double sv;
        public final double pu;
        public final double pv;
        public final int slot;
        public boolean landed = false;
        public boolean arrived = false;

        Drop(int ball, long start, double su, double sv, double pu, double pv, int slot) {
            this.ball = ball;
            this.start = start;
            this.su = su;
            this.sv = sv;
            this.pu = pu;
            this.pv = pv;
            this.slot = slot;
        }
    }

    public final PoolPhysics ph = new PoolPhysics();
    private final Random rnd = new Random();

    // ---- partida
    private boolean shotActive = false;
    /** Reglas y marcador del modo elegido (modo, turno, grupos, puntos, tiros, récord). */
    public final PoolRules rules = new PoolRules();
    /** true = partida en curso; false = mesa libre (práctica) o sala de espera con jugadores apuntados. */
    public boolean playing = false;
    public final List<UUID> pids = new ArrayList<>();
    public final List<String> pnames = new ArrayList<>();
    /** Bolas fuera de juego en este modo (9 bolas: de la 10 a la 15), máscara de bits. */
    public int offMask = 0;
    /** Últimos sucesos de la partida (para el panel de pantalla); se guardan y se mandan a los clientes. */
    public final List<Component> log = new ArrayList<>();
    public static final int LOG_MAX = 4;
    /** Bolas caídas (sin la blanca), en el orden en que cayeron: ese es su lugar en la bandeja. */
    public final List<Integer> tray = new ArrayList<>();
    private int potSeen = 0;
    /** Solo cliente: bolas que están cayendo ahora mismo. */
    public final List<Drop> drops = new ArrayList<>();

    // ---- solo cliente (dibujo suave)
    public final float[] px = new float[PoolPhysics.N];
    public final float[] pz = new float[PoolPhysics.N];
    public final float[] cx = new float[PoolPhysics.N];
    public final float[] cz = new float[PoolPhysics.N];
    public final float[] rx = new float[PoolPhysics.N];
    public final float[] rz = new float[PoolPhysics.N];
    public final Quaternionf[] orient = new Quaternionf[PoolPhysics.N];
    public boolean clientInit = false;
    public long recvNanos = 0L;
    public boolean clientMoving = false;

    public PoolTableBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POOL_TABLE.get(), pos, state);
    }

    // ------------------------------------------------------------------ geometría

    public Direction facing() {
        return getBlockState().getValue(PoolTableBlock.FACING);
    }

    public Vec3 dirU() {
        Direction f = facing();
        return new Vec3(f.getStepX(), 0, f.getStepZ());
    }

    public Vec3 dirV() {
        Direction f = facing().getClockWise();
        return new Vec3(f.getStepX(), 0, f.getStepZ());
    }

    /** Centro de la mesa (a la altura del bloque). */
    public Vec3 center() {
        Vec3 u = dirU();
        Vec3 v = dirV();
        double hu = (PoolTableBlock.NU - 1) / 2.0;
        double hv = (PoolTableBlock.NV - 1) / 2.0;
        return new Vec3(worldPosition.getX() + 0.5 + u.x * hu + v.x * hv, worldPosition.getY(), worldPosition.getZ() + 0.5 + u.z * hu + v.z * hv);
    }

    /** Punto del mundo sobre el paño para unas coordenadas (u, v) del tablero. */
    public Vec3 world(double u, double v) {
        Vec3 c = center();
        Vec3 du = dirU();
        Vec3 dv = dirV();
        double a = u - PoolPhysics.L / 2;
        double b = v - PoolPhysics.W / 2;
        return new Vec3(c.x + du.x * a + dv.x * b, worldPosition.getY() + FELT_Y, c.z + du.z * a + dv.z * b);
    }

    /** Vector horizontal del mundo -> componentes (u, v) del tablero. */
    public double[] toTableDir(double wx, double wz) {
        Vec3 du = dirU();
        Vec3 dv = dirV();
        return new double[] {wx * du.x + wz * du.z, wx * dv.x + wz * dv.z};
    }

    /** Distancia horizontal de un punto al borde de la mesa (0 si está encima) y su diferencia de altura. */
    public double distanceTo(Vec3 p) {
        Vec3 c = center();
        double[] d = toTableDir(p.x - c.x, p.z - c.z);
        double du = Math.max(0, Math.abs(d[0]) - PoolTableBlock.NU / 2.0);
        double dv = Math.max(0, Math.abs(d[1]) - PoolTableBlock.NV / 2.0);
        double dy = Math.abs(p.y - (worldPosition.getY() + 1.0));
        return dy > 2.6 ? 99.0 : Math.hypot(du, dv);
    }

    /** La mesa más cercana al punto (dentro del alcance), o null. */
    public static PoolTableBE findNear(Level level, Vec3 p, double reach) {
        PoolTableBE best = null;
        double bd = reach;
        BlockPos o = BlockPos.containing(p);
        // la bola-entidad vive en una esquina de la mesa: hasta 5 bloques del centro + el alcance
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -3; dy <= 2; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockEntity be = level.getBlockEntity(o.offset(dx, dy, dz));
                    if (be instanceof PoolTableBE t) {
                        double d = t.distanceTo(p);
                        if (d <= bd) {
                            bd = d;
                            best = t;
                        }
                    }
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ partida (servidor)

    /** Acciones del menú de la mesa (las manda el cliente). */
    public static final int A_MODE = 0;
    public static final int A_JOIN = 1;
    public static final int A_LEAVE = 2;
    public static final int A_START = 3;
    public static final int A_RESET = 4;

    public PoolRules.Mode mode() {
        return rules.mode;
    }

    /** ¿El número de jugadores apuntados sirve para el modo elegido? */
    public boolean validCount() {
        return rules.mode.accepts(pids.size());
    }

    /** Jugador al que le toca (solo si hay partida). */
    public UUID currentPlayer() {
        return playing && rules.turn < pids.size() ? pids.get(rules.turn) : null;
    }

    private void broadcast(Component c, boolean actionBar) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 ctr = center();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) < 24 * 24) {
                if (actionBar) {
                    p.displayClientMessage(c, true);
                } else {
                    p.sendSystemMessage(c);
                }
            }
        }
    }

    private Component name(int i) {
        return Component.literal(i >= 0 && i < pnames.size() ? pnames.get(i) : "?").withStyle(ChatFormatting.YELLOW);
    }

    /** Nombre de un equipo: sus jugadores unidos con " + " (con 2 jugadores es el nombre del jugador). */
    private Component teamName(int t) {
        net.minecraft.network.chat.MutableComponent c = Component.empty();
        boolean first = true;
        for (int i = 0; i < pnames.size(); i++) {
            if (rules.teamOf(i) == t) {
                if (!first) {
                    c.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
                }
                c.append(name(i));
                first = false;
            }
        }
        return c;
    }

    private Component groupName(int g) {
        return Component.translatable(g == 0 ? "pooltable.group.solids" : "pooltable.group.stripes").withStyle(ChatFormatting.AQUA);
    }

    private Component arg(Object a) {
        if (a instanceof PoolRules.P p) {
            return name(p.i());
        } else if (a instanceof PoolRules.T t) {
            return teamName(t.i());
        } else if (a instanceof PoolRules.G g) {
            return groupName(g.g());
        } else if (a instanceof PoolRules.N n) {
            return Component.literal(Integer.toString(n.n())).withStyle(ChatFormatting.WHITE);
        } else if (a instanceof PoolRules.Key k) {
            return Component.translatable(k.k());
        }
        return Component.literal(String.valueOf(a));
    }

    private void sendEvents(List<PoolRules.Ev> events) {
        for (PoolRules.Ev e : events) {
            Object[] args = new Object[e.args.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = arg(e.args[i]);
            }
            ChatFormatting col = switch (e.style) {
                case PoolRules.GOOD -> ChatFormatting.GREEN;
                case PoolRules.GOLD -> ChatFormatting.GOLD;
                case PoolRules.BAD -> ChatFormatting.RED;
                default -> ChatFormatting.GRAY;
            };
            Component msg = Component.translatable(e.key, args).withStyle(col);
            if (!e.bar) {
                broadcast(msg, false); // lo importante queda también en el chat
            }
            if (!e.key.equals("pooltable.msg.turn") && !e.key.equals("pooltable.msg.again") && !e.key.equals("pooltable.msg.break")) {
                log.add(msg);
                while (log.size() > LOG_MAX) {
                    log.remove(0);
                }
            }
        }
    }

    private static void tell(ServerPlayer p, String key, ChatFormatting col) {
        p.displayClientMessage(Component.translatable(key).withStyle(col), true);
    }

    /** ¿Puede este jugador reiniciar la mesa? Si hay partida, solo los que juegan (o cualquiera si alguno de ellos se fue del servidor). */
    private boolean canManage(ServerPlayer p) {
        if (!playing || pids.contains(p.getUUID())) {
            return true;
        }
        if (level instanceof ServerLevel sl) {
            for (UUID id : pids) {
                if (sl.getServer().getPlayerList().getPlayer(id) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Lo que se hace desde el menú: elegir modo, apuntarse, salir, empezar o reiniciar. Todo se comprueba aquí, en el servidor. */
    public void menuAction(ServerPlayer p, int action, int arg) {
        if (shotActive || ph.moving()) {
            tell(p, "pooltable.msg.busy", ChatFormatting.RED);
            return;
        }
        int idx = pids.indexOf(p.getUUID());
        switch (action) {
            case A_MODE -> {
                if (playing) {
                    tell(p, "pooltable.msg.in_progress", ChatFormatting.RED);
                } else {
                    rules.mode = PoolRules.Mode.byOrdinal(arg);
                    sync();
                }
            }
            case A_JOIN -> {
                if (playing) {
                    tell(p, "pooltable.msg.in_progress", ChatFormatting.RED);
                } else if (idx < 0) {
                    if (pids.size() >= PoolRules.MAX_PLAYERS) {
                        tell(p, "pooltable.msg.full", ChatFormatting.RED);
                    } else {
                        pids.add(p.getUUID());
                        pnames.add(p.getName().getString());
                        broadcast(Component.translatable("pooltable.msg.joined", name(pids.size() - 1), pids.size()).withStyle(ChatFormatting.GREEN), false);
                        sync();
                    }
                }
            }
            case A_LEAVE -> {
                if (idx < 0) {
                    return;
                }
                if (playing) {
                    broadcast(Component.translatable("pooltable.msg.cancelled", name(idx)).withStyle(ChatFormatting.GRAY), false);
                    playing = false;
                    pids.clear();
                    pnames.clear();
                    rules.turn = 0;
                } else {
                    pids.remove(idx);
                    pnames.remove(idx);
                }
                sync();
            }
            case A_START -> {
                if (playing) {
                    return;
                }
                if (idx < 0) {
                    tell(p, "pooltable.msg.join_first", ChatFormatting.RED);
                } else if (!validCount()) {
                    tell(p, "pooltable.msg.bad_count", ChatFormatting.RED);
                } else {
                    startMatch();
                }
            }
            case A_RESET -> {
                if (canManage(p)) {
                    resetTable();
                    broadcast(Component.translatable("pooltable.msg.reset", p.getName()).withStyle(ChatFormatting.GRAY), false);
                } else {
                    tell(p, "pooltable.msg.in_progress", ChatFormatting.RED);
                }
            }
            default -> {
            }
        }
    }

    /** Arma el triángulo de nuevo: la bandeja queda vacía. */
    private void rackBalls() {
        ph.rack(rnd);
        tray.clear();
        potSeen = 0;
    }

    private void resetShot() {
        ph.resetShot();
        potSeen = 0;
    }

    public void resetTable() {
        rackBalls();
        offMask = 0;
        pids.clear();
        pnames.clear();
        playing = false;
        rules.turn = 0;
        rules.group[0] = -1;
        rules.group[1] = -1;
        rules.shots = 0;
        log.clear();
        shotActive = false;
        sync();
    }

    private void startMatch() {
        PoolRules.Outcome o = rules.start(ph, rnd, pids.size());
        log.clear();
        tray.clear();
        potSeen = 0;
        offMask = PoolRules.offMask(rules.mode);
        playing = true;
        shotActive = false;
        sendEvents(o.events);
        sync();
    }

    /** El jugador soltó el taco: golpea la blanca en la dirección en que mira, con la potencia (0..1) cargada. */
    public boolean tryShoot(ServerPlayer p, double power) {
        if (shotActive || ph.moving()) {
            return false;
        }
        if (playing) {
            int idx = pids.indexOf(p.getUUID());
            if (idx < 0) {
                tell(p, "pooltable.msg.in_progress", ChatFormatting.RED);
                return false;
            }
            if (idx != rules.turn) {
                tell(p, "pooltable.msg.not_your_turn", ChatFormatting.RED);
                return false;
            }
        }
        if (ph.out[0]) {
            ph.respotCue();
        }
        Vec3 look = p.getLookAngle();
        double[] d = toTableDir(look.x, look.z);
        if (Math.hypot(d[0], d[1]) < 1e-4) {
            return false;
        }
        double angle = Math.atan2(d[1], d[0]);
        double speed = 1.2 + 10.8 * Math.pow(Mth01(power), 1.25);
        ph.shoot(angle, speed);
        potSeen = 0;
        shotActive = true;
        if (level != null) {
            Vec3 w = world(ph.x[0], ph.z[0]);
            level.playSound(null, w.x, w.y, w.z, SoundEvents.BAMBOO_WOOD_HIT, SoundSource.BLOCKS, 0.4F + 0.6F * (float) power, 0.7F + 0.4F * (float) power);
        }
        return true;
    }

    private static double Mth01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    public void serverTick() {
        if (ph.moving()) {
            ph.tick();
            while (potSeen < ph.potted.size()) {
                int b = ph.potted.get(potSeen++);
                if (b != 0 && !tray.contains(b)) {
                    tray.add(b);
                }
            }
            playImpacts();
            sendMoveSync();
            return;
        }
        if (shotActive) {
            shotActive = false;
            resolveShot();
            sendMoveSync();
            sync();
        }
    }

    private void playImpacts() {
        if (level == null || ph.impacts.isEmpty()) {
            ph.impacts.clear();
            return;
        }
        List<double[]> list = new ArrayList<>(ph.impacts);
        ph.impacts.clear();
        list.sort((a, b) -> Double.compare(b[3] + (b[0] == 2 ? 100 : 0), a[3] + (a[0] == 2 ? 100 : 0)));
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            double[] im = list.get(i);
            Vec3 w = world(im[1], im[2]);
            float vol = (float) Math.max(0.15, Math.min(1.0, 0.2 + im[3] / 8.0));
            SoundEvent s;
            float pitch;
            if (im[0] == 0) {
                s = SoundEvents.AMETHYST_BLOCK_HIT;
                pitch = 1.7F + rnd.nextFloat() * 0.3F;
            } else if (im[0] == 1) {
                s = SoundEvents.WOOD_HIT;
                pitch = 1.1F + rnd.nextFloat() * 0.2F;
            } else {
                s = SoundEvents.CHEST_CLOSE;
                pitch = 1.5F;
                vol = 0.7F;
            }
            level.playSound(null, w.x, w.y, w.z, s, SoundSource.BLOCKS, vol, pitch);
        }
    }

    // ------------------------------------------------------------------ reglas

    private void resolveShot() {
        if (!playing) {
            // práctica libre: sin reglas, solo se recoloca la blanca si cae
            if (ph.potted.contains(0)) {
                ph.respotCue();
                broadcast(Component.translatable("pooltable.msg.scratch_free").withStyle(ChatFormatting.GRAY), true);
            }
            boolean any = false;
            for (int b = 1; b < PoolPhysics.N; b++) {
                any |= !ph.out[b] && (offMask & (1 << b)) == 0;
            }
            if (!any) {
                broadcast(Component.translatable("pooltable.msg.clean").withStyle(ChatFormatting.GOLD), false);
            }
            resetShot();
            return;
        }
        PoolRules.Outcome o = rules.resolve(ph);
        for (int b : o.respot) {
            ph.respotBall(b);
            tray.remove(Integer.valueOf(b));
        }
        if (o.cueScratch) {
            ph.respotCue();
        }
        if (o.rerack) {
            rackBalls();
        }
        sendEvents(o.events);
        if (o.ended) {
            // la partida termina: los jugadores siguen apuntados por si quieren la revancha (Empezar de nuevo)
            playing = false;
            rules.turn = 0;
        }
        resetShot();
    }

    // ------------------------------------------------------------------ red y guardado

    /** Avisa a los que ven la mesa de los cambios de partida (nombres, turno, bolas caídas). */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void sendMoveSync() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        float[] x = new float[PoolPhysics.N];
        float[] z = new float[PoolPhysics.N];
        int mask = 0;
        for (int i = 0; i < PoolPhysics.N; i++) {
            x[i] = (float) ph.x[i];
            z[i] = (float) ph.z[i];
            if (ph.out[i]) {
                mask |= 1 << i;
            }
        }
        PoolNet.sendToTracking(sl, worldPosition, new com.pooltable.net.PoolSyncPacket(worldPosition, x, z, mask, ph.moving()));
    }

    /** Lo que recibe el cliente en cada tick de movimiento. */
    public void clientReceive(float[] x, float[] z, int mask, boolean moving) {
        boolean hadInit = clientInit;
        if (!clientInit) {
            clientLoadFrom(x, z);
            clientInit = true;
        } else {
            System.arraycopy(cx, 0, px, 0, PoolPhysics.N);
            System.arraycopy(cz, 0, pz, 0, PoolPhysics.N);
        }
        System.arraycopy(x, 0, cx, 0, PoolPhysics.N);
        System.arraycopy(z, 0, cz, 0, PoolPhysics.N);
        for (int i = 0; i < PoolPhysics.N; i++) {
            boolean now = (mask & (1 << i)) != 0;
            if (hadInit && !ph.out[i] && now) {
                startDrop(i, x[i], z[i]);
            }
            if (ph.out[i] && !now) {
                // vuelve a la mesa (la blanca recolocada): aparece directamente, sin deslizarse desde la tronera
                px[i] = x[i];
                pz[i] = z[i];
                rx[i] = x[i];
                rz[i] = z[i];
                final int ball = i;
                drops.removeIf(d -> d.ball == ball);
            }
            ph.out[i] = now;
            ph.x[i] = x[i];
            ph.z[i] = z[i];
        }
        recvNanos = System.nanoTime();
        clientMoving = moving;
    }

    /** Una bola acaba de caer en una tronera: se anota su sitio en la bandeja y se empieza la animación. */
    private void startDrop(int ball, double u, double v) {
        double[] best = PoolPhysics.POCKETS[0];
        double bd = Double.MAX_VALUE;
        for (double[] p : PoolPhysics.POCKETS) {
            double d = Math.hypot(p[0] - u, p[1] - v);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        int slot = -1;
        if (ball != 0) {
            if (!tray.contains(ball)) {
                tray.add(ball);
            }
            slot = tray.indexOf(ball);
        }
        drops.removeIf(d -> d.ball == ball);
        drops.add(new Drop(ball, System.nanoTime(), u, v, best[0], best[1], slot));
    }

    private void clientLoadFrom(float[] x, float[] z) {
        for (int i = 0; i < PoolPhysics.N; i++) {
            px[i] = cx[i] = rx[i] = x[i];
            pz[i] = cz[i] = rz[i] = z[i];
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag xs = new ListTag();
        ListTag zs = new ListTag();
        int mask = 0;
        for (int i = 0; i < PoolPhysics.N; i++) {
            xs.add(DoubleTag.valueOf(ph.x[i]));
            zs.add(DoubleTag.valueOf(ph.z[i]));
            if (ph.out[i]) {
                mask |= 1 << i;
            }
        }
        tag.put("bx", xs);
        tag.put("bz", zs);
        tag.putInt("out", mask);
        ListTag ids = new ListTag();
        ListTag names = new ListTag();
        for (int i = 0; i < pids.size(); i++) {
            ids.add(StringTag.valueOf(pids.get(i).toString()));
            names.add(StringTag.valueOf(pnames.get(i)));
        }
        tag.put("pids", ids);
        tag.put("pnames", names);
        tag.putInt("turn", rules.turn);
        tag.putInt("g0", rules.group[0]);
        tag.putInt("g1", rules.group[1]);
        tag.putInt("shots", rules.shots);
        tag.putInt("mode", rules.mode.ordinal());
        tag.putBoolean("playing", playing);
        tag.putInt("np", rules.players);
        tag.putIntArray("scores", rules.score.clone());
        tag.putInt("best", rules.best);
        tag.putInt("off", offMask);
        ListTag lg = new ListTag();
        for (Component c : log) {
            lg.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        tag.put("log", lg);
        int[] tr = new int[tray.size()];
        for (int i = 0; i < tr.length; i++) {
            tr[i] = tray.get(i);
        }
        tag.putIntArray("tray", tr);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("bx", Tag.TAG_LIST)) {
            ListTag xs = tag.getList("bx", Tag.TAG_DOUBLE);
            ListTag zs = tag.getList("bz", Tag.TAG_DOUBLE);
            int mask = tag.getInt("out");
            for (int i = 0; i < PoolPhysics.N && i < xs.size(); i++) {
                ph.x[i] = xs.getDouble(i);
                ph.z[i] = zs.getDouble(i);
                ph.vx[i] = 0;
                ph.vz[i] = 0;
                ph.out[i] = (mask & (1 << i)) != 0;
            }
        }
        pids.clear();
        pnames.clear();
        ListTag ids = tag.getList("pids", Tag.TAG_STRING);
        ListTag names = tag.getList("pnames", Tag.TAG_STRING);
        for (int i = 0; i < ids.size() && i < names.size(); i++) {
            try {
                pids.add(UUID.fromString(ids.getString(i)));
                pnames.add(names.getString(i));
            } catch (IllegalArgumentException ignored) {
                // identificador dañado: se ignora
            }
        }
        rules.mode = PoolRules.Mode.byOrdinal(tag.contains("mode") ? tag.getInt("mode") : PoolRules.Mode.EIGHT.ordinal());
        playing = tag.getBoolean("playing");
        rules.players = tag.contains("np") ? tag.getInt("np") : pids.size();
        rules.turn = Math.max(0, Math.min(PoolRules.MAX_PLAYERS - 1, tag.getInt("turn")));
        rules.group[0] = tag.contains("g0") ? tag.getInt("g0") : -1;
        rules.group[1] = tag.contains("g1") ? tag.getInt("g1") : -1;
        rules.shots = tag.getInt("shots");
        rules.best = tag.getInt("best");
        offMask = tag.getInt("off");
        log.clear();
        ListTag lg = tag.getList("log", Tag.TAG_STRING);
        for (int i = 0; i < lg.size() && i < LOG_MAX; i++) {
            try {
                Component c = Component.Serializer.fromJson(lg.getString(i));
                if (c != null) {
                    log.add(c);
                }
            } catch (RuntimeException ignored) {
                // texto dañado: se ignora
            }
        }
        int[] sc = tag.getIntArray("scores");
        java.util.Arrays.fill(rules.score, 0);
        for (int i = 0; i < sc.length && i < rules.score.length; i++) {
            rules.score[i] = sc[i];
        }
        if (playing && (pids.isEmpty() || rules.turn >= pids.size())) {
            playing = false; // datos incoherentes (versión anterior): mesa libre
            rules.turn = 0;
        }
        tray.clear();
        for (int b : tag.getIntArray("tray")) {
            if (b > 0 && b < PoolPhysics.N && !tray.contains(b)) {
                tray.add(b);
            }
        }
        // una bola caída que no esté en la bandeja (mesa guardada con otra versión) se añade al final
        for (int b = 1; b < PoolPhysics.N; b++) {
            if (ph.out[b] && !tray.contains(b) && (offMask & (1 << b)) == 0) {
                tray.add(b);
            }
        }
        potSeen = 0;
        if (level != null && level.isClientSide || level == null) {
            // en el cliente: partir de lo que dice el servidor (sin animación)
            float[] x = new float[PoolPhysics.N];
            float[] z = new float[PoolPhysics.N];
            for (int i = 0; i < PoolPhysics.N; i++) {
                x[i] = (float) ph.x[i];
                z[i] = (float) ph.z[i];
            }
            clientLoadFrom(x, z);
            clientInit = true;
            recvNanos = System.nanoTime();
            clientMoving = false;
        }
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
        return new AABB(worldPosition).inflate(7.5, 2.0, 7.5);
    }
}
