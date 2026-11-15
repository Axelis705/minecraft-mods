package com.casino.client;

import com.casino.block.BlackjackBE;
import com.casino.block.LuckyWheelBE;
import com.casino.block.PokerBE;
import com.casino.block.RouletteBE;
import com.casino.entity.DealerAnim;
import com.casino.entity.DealerEntity;
import com.casino.game.BlackjackLayout;
import com.casino.game.RouletteLayout;
import com.casino.game.RouletteTable;
import com.casino.game.RouletteWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Lo que el crupier hace con las manos según lo que pasa de verdad en su mesa (todo en el cliente, con la hora del mundo):
 * <ul>
 *   <li>blackjack y póker: toma cada carta del zapato justo cuando sale y la empuja hacia el asiento al que va; voltea su carta tapada;</li>
 *   <li>ruleta: saca la bola de la rueda, la levanta y la lanza (la bola vuela de su mano a la pista);</li>
 *   <li>anfitrión: pone la mano en la orilla de la rueda y la jala hacia abajo; mira y señala la rueda mientras gira;</li>
 *   <li>bartender: saca botella y vaso, sirve y te da el vaso; si no, pule un vaso con un trapo.</li>
 * </ul>
 * El resultado son ángulos para brazos y cabeza (con cuánto pesan sobre la animación normal), un giro del torso y lo que tiene en
 * cada mano ({@link DealerProps}).
 */
public final class DealerMotion {
    /** Brazo derecho: ángulos x/y/z y cuánto pesan (0 = nada, 1 = todo). */
    public float rx, ry, rz, rw;
    public float lx, ly, lz, lw;
    /** Cabeza: ángulos y peso. */
    public float hx, hy, hw;
    /** Giro del torso (radianes, positivo = hacia su derecha). */
    public float twist;
    /** Lo que tiene en la mano derecha / izquierda ({@link DealerProps}) y cuánto se inclina (servir). */
    public int propR, propL;
    public float tiltR;

    private static final double SHOULDER_Y = 22.0 / 16.0;
    private static final double SHOULDER_X = 5.0 / 16.0;
    private static final double EYE_Y = 1.62;
    /** Largo del brazo hasta la palma. */
    static final double ARM = 10.0 / 16.0;

    // geometría de las mesas (igual que en sus dibujantes)
    private static final double BJ_SHOE_X = 0.8, PK_SHOE_X = 0.85, SHOE_Z = -0.27;
    private static final double CARD_R = 1.6;
    private static final double TABLE_Y = 11.0 / 16.0;

    // ------------------------------------------------------------------ marco del crupier

    private final double ex, ey, ez;
    private final double fx, fz, rxv, rzv;

    private DealerMotion(DealerEntity e, float partial) {
        ex = Mth.lerp(partial, e.xo, e.getX());
        ey = Mth.lerp(partial, e.yo, e.getY());
        ez = Mth.lerp(partial, e.zo, e.getZ());
        double yaw = Math.toRadians(e.yBodyRot);
        fx = -Math.sin(yaw);
        fz = Math.cos(yaw);
        rxv = -Math.cos(yaw);
        rzv = -Math.sin(yaw);
    }

    /** Ángulos del brazo para que apunte (con la palma) hacia un punto del mundo. */
    private float[] aimArm(boolean right, Vec3 t) {
        double side = right ? 1 : -1;
        double sx = ex + rxv * SHOULDER_X * side, sy = ey + SHOULDER_Y, sz = ez + rzv * SHOULDER_X * side;
        double dx = t.x - sx, dy = t.y - sy, dz = t.z - sz;
        double fw = dx * fx + dz * fz;
        double rt = dx * rxv + dz * rzv;
        // ángulo desde "hacia abajo" y giro: hacia adelante el brazo sube por el frente; hacia atrás, por la espalda
        double alpha = Math.atan2(Math.sqrt(fw * fw + rt * rt), -dy);
        float x;
        float y;
        if (fw >= -0.05) {
            x = (float) -alpha;
            y = (float) Math.atan2(rt, Math.max(0.05, fw));
        } else {
            x = (float) alpha;
            y = (float) Math.atan2(-rt, -fw);
        }
        return new float[]{x, Mth.clamp(y, -1.4F, 1.4F)};
    }

    private void armR(Vec3 t, float w) {
        float[] a = aimArm(true, t);
        rx = a[0];
        ry = a[1];
        rw = w;
    }

    private void armL(Vec3 t, float w) {
        float[] a = aimArm(false, t);
        lx = a[0];
        ly = a[1];
        lw = w;
    }

    private void look(Vec3 t, float w) {
        double dx = t.x - ex, dy = t.y - (ey + EYE_Y), dz = t.z - ez;
        double fw = dx * fx + dz * fz;
        double rt = dx * rxv + dz * rzv;
        hy = (float) Mth.clamp(Math.atan2(rt, Math.max(0.05, fw)), -1.1, 1.1);
        hx = (float) Mth.clamp(Math.atan2(-dy, Math.sqrt(fw * fw + rt * rt)), -0.8, 0.9);
        hw = w;
    }

    /** Un punto en el marco de una mesa (origen en el centro de su bloque principal, +z hacia los jugadores, +x a la derecha de ellos). */
    static Vec3 local(BlockPos o, Direction f, double lx, double ly, double lz) {
        Direction r = f.getCounterClockWise();
        return new Vec3(o.getX() + 0.5 + r.getStepX() * lx + f.getStepX() * lz, o.getY() + ly, o.getZ() + 0.5 + r.getStepZ() * lx + f.getStepZ() * lz);
    }

    private static float smooth(double x) {
        x = Mth.clamp(x, 0, 1);
        return (float) (x * x * (3 - 2 * x));
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z));
    }

    // ------------------------------------------------------------------ cálculo

    public static DealerMotion compute(DealerEntity e, float partial) {
        DealerMotion m = new DealerMotion(e, partial);
        Level level = e.level();
        if (level == null) {
            return m;
        }
        double time = level.getGameTime() + partial;
        BlockPos tp = e.clientTable();
        BlockEntity be = tp == null ? null : level.getBlockEntity(tp);
        try {
            switch (e.getGame()) {
                case DealerEntity.GAME_BLACKJACK -> {
                    if (be instanceof BlackjackBE bj) {
                        m.cards(bj.facing(), tp, time, BJ_SHOE_X, blackjackDeals(bj), bj.revealAt, new double[]{0.075, -0.2});
                    }
                }
                case DealerEntity.GAME_POKER -> {
                    if (be instanceof PokerBE pk) {
                        m.cards(pk.facing(), tp, time, PK_SHOE_X, pokerDeals(pk), pk.revealAt, new double[]{0.0, -0.2});
                    }
                }
                case DealerEntity.GAME_ROULETTE -> {
                    if (be instanceof RouletteBE ro) {
                        m.roulette(ro, tp, time);
                    }
                }
                case DealerEntity.GAME_WHEEL -> {
                    if (be instanceof LuckyWheelBE w) {
                        m.host(e, w, time, partial);
                    }
                }
                case DealerEntity.GAME_BARTENDER -> m.bartender(e, time, partial);
                case DealerEntity.GAME_PATRON -> m.patron(e, partial);
                case DealerEntity.GAME_SECURITY -> m.security(e, partial);
                default -> {
                }
            }
        } catch (RuntimeException ignored) {
            // si la mesa cambió a medio cuadro, este cuadro va sin manos
        }
        return m;
    }

    // ------------------------------------------------------------------ cartas

    /** Cartas que reparte: {hora en que sale del zapato, x, z} en el marco de la mesa. */
    private static double[][] blackjackDeals(BlackjackBE be) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        for (int j = 0; j < be.dBorn.length; j++) {
            if (be.dBorn[j] > -500) {
                out.add(new double[]{be.dBorn[j], (j - 0.5) * 0.15, -0.2});
            }
        }
        for (int i = 0; i < BlackjackLayout.SEATS; i++) {
            double[] cp = BlackjackLayout.polar(CARD_R, BlackjackLayout.seatAngle(i));
            for (long b : be.born[i]) {
                if (b > -500) {
                    out.add(new double[]{b, cp[0], cp[1]});
                }
            }
            for (long b : be.born2[i]) {
                if (b > -500) {
                    out.add(new double[]{b, cp[0], cp[1]});
                }
            }
        }
        return out.toArray(new double[0][]);
    }

    private static double[][] pokerDeals(PokerBE be) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        for (int j = 0; j < be.bornDealer.length; j++) {
            if (be.bornDealer[j] > -500) {
                out.add(new double[]{be.bornDealer[j], (j - 0.5) * 0.2, -0.2});
            }
        }
        for (int j = 0; j < be.bornBoard.length; j++) {
            if (be.bornBoard[j] > -500) {
                out.add(new double[]{be.bornBoard[j], (j - 2) * 0.2, 0.25});
            }
        }
        for (int i = 0; i < BlackjackLayout.SEATS; i++) {
            double[] cp = BlackjackLayout.polar(CARD_R, BlackjackLayout.seatAngle(i));
            for (long b : be.bornHole[i]) {
                if (b > -500) {
                    out.add(new double[]{b, cp[0], cp[1]});
                }
            }
        }
        return out.toArray(new double[0][]);
    }

    /**
     * Reparto: la mano izquierda (el zapato está a su izquierda) llega al zapato un poco antes de que salga cada carta, la toma y la
     * empuja un trecho hacia donde va; si viene otra, regresa al zapato. Voltear la carta tapada: la derecha va a su carta y gira la muñeca.
     */
    private void cards(Direction f, BlockPos o, double time, double shoeX, double[][] deals, long revealAt, double[] hole) {
        Vec3 shoe = local(o, f, shoeX, TABLE_Y + 0.12, SHOE_Z);
        double[] cur = null;
        double[] prev = null;
        // cada carta: la toma del zapato (PICK ticks antes), la desliza sobre la mesa con la mano hasta HOLD del camino (la carta de la
        // mesa va justo bajo la palma: mismo camino y misma curva que en el dibujo de la mesa) y la suelta; luego vuelve por la siguiente
        double fly = com.casino.game.CardTiming.FLY, pick = com.casino.game.CardTiming.PICK, hold = com.casino.game.CardTiming.HOLD * fly;
        for (double[] d : deals) {
            double p = time - d[0];
            if (p >= -pick && p <= hold && (cur == null || d[0] > cur[0])) {
                cur = d;
            }
        }
        if (cur == null) {
            for (double[] d : deals) {
                double p = time - d[0];
                if (p > hold && p <= hold + 9 && (cur == null || d[0] > cur[0])) {
                    cur = d;
                }
            }
        }
        if (cur != null) {
            for (double[] d : deals) {
                if (d[0] < cur[0] && cur[0] - d[0] <= com.casino.game.CardTiming.GAP + 2 && (prev == null || d[0] > prev[0])) {
                    prev = d;
                }
            }
            double p = time - cur[0];
            Vec3 dest = local(o, f, cur[1], TABLE_Y + 0.08, cur[2]);
            Vec3 hand;
            float w;
            if (p < 0) {
                Vec3 from = prev != null ? cardAt(shoe, local(o, f, prev[1], TABLE_Y + 0.08, prev[2]), com.casino.game.CardTiming.HOLD) : shoe;
                hand = lerp(from, shoe, smooth((p + pick) / (pick - 1)));
                w = prev != null ? 1 : smooth((p + pick) / 4);
                if (p > -3) {
                    propL = DealerProps.CARD; // la carta en la mano, saliendo del zapato
                }
            } else if (p <= hold) {
                hand = cardAt(shoe, dest, p / fly).add(0, 0.03, 0);
                w = 1;
            } else {
                hand = cardAt(shoe, dest, com.casino.game.CardTiming.HOLD).add(0, 0.03 + 0.08 * smooth((p - hold) / 5), 0);
                w = 1 - smooth((p - hold) / 9);
            }
            armL(hand, w);
            look(hand, 0.55F * w);
            Vec3 aim = local(o, f, cur[1], 0, cur[2]);
            double rt = (aim.x - ex) * rxv + (aim.z - ez) * rzv;
            twist = (float) Mth.clamp(rt * 0.18, -0.3, 0.3) * w;
        }
        // voltear la carta tapada
        double q = time - revealAt;
        if (q >= -8 && q <= 14) {
            float w = smooth((q + 8) / 6) * (1 - smooth((q - 8) / 6));
            Vec3 card = local(o, f, hole[0], TABLE_Y + 0.06 + (q > 0 && q < 8 ? 0.1 * Math.sin(Math.PI * q / 8) : 0), hole[1]);
            armR(card, w);
            rz = (float) (q > 0 && q < 8 ? -1.2 * Math.sin(Math.PI * q / 8) : 0);
            look(card, 0.7F * w);
        }
    }

    /** Dónde va la carta que se desliza (t de 0 a 1), con la misma curva que la mesa: frena al llegar y se eleva un poco a la mitad. */
    private static Vec3 cardAt(Vec3 shoe, Vec3 dest, double t) {
        t = Math.max(0, Math.min(1, t));
        double e = 1 - (1 - t) * (1 - t);
        return lerp(shoe, dest, e).add(0, Math.sin(Math.PI * t) * 0.12, 0);
    }

    // ------------------------------------------------------------------ ruleta

    /** Dónde suelta la bola el crupier de ruleta (marco de la mesa): arriba de la rueda, un poco a su derecha. */
    static final double[] TOSS = {-2.2, 1.55, -1.25};
    /** Ticks que pasan desde que toma la bola hasta que cae en la pista; los últimos 12 va volando. */
    public static final int TOSS_TICKS = 24;
    public static final int FLIGHT_TICKS = 12;

    private void roulette(RouletteBE be, BlockPos o, double time) {
        Direction f = be.facing();
        RouletteTable t = be.table;
        double toss = time - (be.spinStart - TOSS_TICKS);
        Vec3 wheel = local(o, f, RouletteLayout.WHEEL_X, RouletteLayout.Y_TOP + 0.05, RouletteLayout.WHEEL_Z);
        if (t.phase == RouletteTable.Phase.SPINNING && be.spinSeq > 0 && toss >= 0 && toss <= TOSS_TICKS + 10) {
            Vec3 pocket = local(o, f, RouletteLayout.WHEEL_X - 0.15, RouletteLayout.Y_TOP + 0.06, RouletteLayout.WHEEL_Z - 0.75);
            Vec3 lift = local(o, f, TOSS[0] + 0.1, TOSS[1] - 0.2, TOSS[2] - 0.25);
            Vec3 rel = local(o, f, TOSS[0], TOSS[1], TOSS[2]);
            RouletteWheel.Ball b = RouletteWheel.ball(be.spinStart, be.spinStart, be.spinSeq, be.spinNumber, be.spinSeed);
            double rad = RouletteLayout.R_TRACK_MID;
            Vec3 track = local(o, f, RouletteLayout.WHEEL_X + rad * Math.cos(b.angle), RouletteLayout.Y_TOP + 0.08, RouletteLayout.WHEEL_Z + rad * Math.sin(b.angle));
            Vec3 hand;
            float w = 1;
            if (toss < 6) {                      // va por la bola a la rueda
                hand = pocket;
                w = smooth(toss / 5);
            } else if (toss < 10) {              // la saca y la levanta
                hand = lerp(pocket, lift, smooth((toss - 6) / 4));
                propR = DealerProps.BALL;
            } else if (toss < FLIGHT_TICKS) {    // impulso hacia atrás... y la suelta
                hand = lerp(lift, rel, smooth((toss - 10) / 2));
                propR = DealerProps.BALL;
            } else {                             // acompaña con la mano y la sigue con la mirada
                hand = lerp(rel, track, 0.35 * smooth((toss - FLIGHT_TICKS) / 4));
                w = 1 - smooth((toss - FLIGHT_TICKS - 4) / 8);
            }
            armR(hand, w);
            look(toss < FLIGHT_TICKS ? hand : tossBall(be, o, time), 0.85F);
            return;
        }
        // mientras gira, la sigue con la mirada
        if (t.phase == RouletteTable.Phase.SPINNING && time >= be.spinStart) {
            look(wheel, 0.6F);
        }
    }

    /** La bola en el aire (entre la mano y la pista), en coordenadas del mundo. */
    private static Vec3 tossBall(RouletteBE be, BlockPos o, double time) {
        double[] p = tossLocal(be, time);
        return local(o, be.facing(), p[0], p[1], p[2]);
    }

    /** Posición de la bola volando, en el marco de la mesa (null si no está volando). */
    public static double[] tossLocal(RouletteBE be, double time) {
        double u = (time - (be.spinStart - FLIGHT_TICKS)) / FLIGHT_TICKS;
        RouletteWheel.Ball b = RouletteWheel.ball(be.spinStart, be.spinStart, be.spinSeq, be.spinNumber, be.spinSeed);
        double rad = RouletteLayout.R_TRACK_MID;
        double tx = RouletteLayout.WHEEL_X + rad * Math.cos(b.angle), tz = RouletteLayout.WHEEL_Z + rad * Math.sin(b.angle);
        double ty = RouletteLayout.Y_TOP + 0.09;
        u = Mth.clamp(u, 0, 1);
        double x = Mth.lerp(u, TOSS[0], tx), z = Mth.lerp(u, TOSS[2], tz);
        double y = Mth.lerp(u, TOSS[1], ty) + Math.sin(Math.PI * u) * 0.35;
        return new double[]{x, y, z};
    }

    // ------------------------------------------------------------------ anfitrión

    private void host(DealerEntity e, LuckyWheelBE w, double time, float partial) {
        Vec3 hub = w.hub();
        Direction f = w.facing();
        Direction r = f.getCounterClockWise();
        double side = (ex - hub.x) * r.getStepX() + (ez - hub.z) * r.getStepZ() >= 0 ? 1 : -1;
        boolean rightNear = (hub.x - ex) * rxv + (hub.z - ez) * rzv > 0; // el brazo del lado de la rueda
        double t = e.animId == DealerAnim.PULL ? e.tickCount - e.animStart + partial : -1;
        if (t >= 0 && t <= DealerAnim.duration(DealerAnim.PULL)) {
            // la mano a la orilla (un poco arriba de la mitad) y luego la arrastra hacia abajo, acompañando la rueda
            double a0 = Math.toRadians(12), a1 = Math.toRadians(-40);
            double a = t < 11 ? a0 : Mth.lerp(smooth((t - 11) / 6), a0, a1);
            Vec3 rim = hub.add(r.getStepX() * side * 1.48 * Math.cos(a), 1.48 * Math.sin(a), r.getStepZ() * side * 1.48 * Math.cos(a))
                    .add(f.getStepX() * 0.12, 0, f.getStepZ() * 0.12);
            float wgt = smooth(t / 8) * (1 - smooth((t - 22) / 10));
            if (rightNear) {
                armR(rim, wgt);
            } else {
                armL(rim, wgt);
            }
            look(rim, 0.8F * wgt);
            return;
        }
        if (w.spinning) {
            // mientras gira: la señala con la mano y no le quita los ojos de encima
            Vec3 center = hub.add(f.getStepX() * 0.3, 0.2, f.getStepZ() * 0.3);
            float wgt = smooth((time - w.spinStart - 8) / 10) * 0.75F;
            if (rightNear) {
                armR(center, wgt);
            } else {
                armL(center, wgt);
            }
            look(hub, 0.75F);
        }
    }

    // ------------------------------------------------------------------ clientes

    /** Los clientes: unos traen su copa (y le dan traguitos), otros juegan con unas fichas en la mano. */
    private void patron(DealerEntity e, float partial) {
        if (e.holdsDrink()) {
            propL = DealerProps.GLASS_FULL;
            double t = e.animId == DealerAnim.SIP ? e.tickCount - e.animStart + partial : -1;
            Vec3 chest = new Vec3(ex + fx * 0.32 - rxv * 0.1, ey + 1.1, ez + fz * 0.32 - rzv * 0.1);
            if (t >= 0) {
                // la copa sube a la boca, un traguito, y baja
                Vec3 mouth = new Vec3(ex + fx * 0.28, ey + 1.62, ez + fz * 0.28);
                float k = smooth(t / 7) * (1 - smooth((t - 20) / 8));
                armL(lerp(chest, mouth, k), 1);
                if (k > 0.8) {
                    hx = -0.35F;
                    hy = 0;
                    hw = k;
                }
            } else {
                armL(chest, 0.85F);
            }
        } else if (e.getVariant() % 3 == 1) {
            propR = DealerProps.CHIPS;
        }
    }

    // ------------------------------------------------------------------ guardias del robo

    /** Guardias: tranquilos, manos libres; en alerta sacan la porra (o la pistola); apuntando, el brazo al frente (con pistola, a dos manos). */
    private void security(DealerEntity e, float partial) {
        int c = e.combat();
        if (c == 0) {
            return;
        }
        boolean armed = e.guardKind() == 1;
        propR = armed ? DealerProps.PISTOL : c == 2 ? DealerProps.TASER : DealerProps.BATON;
        if (c == 2) {
            double yaw = Math.toRadians(Mth.lerp(partial, e.yHeadRotO, e.yHeadRot));
            double pitch = Math.toRadians(e.getXRot());
            Vec3 fwd = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            Vec3 aim = new Vec3(ex, ey + 1.38, ez).add(fwd.scale(1.6));
            armR(aim, 1);
            if (armed) {
                armL(aim.add(-rxv * 0.12, 0, -rzv * 0.12), 0.9F);
            }
        }
    }

    // ------------------------------------------------------------------ bartender

    private void bartender(DealerEntity e, double time, float partial) {
        Vec3 front = new Vec3(ex + fx * 0.45, ey, ez + fz * 0.45);
        Vec3 glass = front.add(rxv * -0.12, 1.08, rzv * -0.12);   // vaso al frente, un poco a su izquierda
        if (e.animId == DealerAnim.SERVE) {
            double t = e.tickCount - e.animStart + partial;
            boolean back = (e.animSeq() & 1) == 1;         // una vez de abajo de la barra, otra de la repisa de atrás
            Vec3 shelf = back ? new Vec3(ex - fx * 0.9 + rxv * 0.35, ey + 1.45, ez - fz * 0.9 + rzv * 0.35)
                    : new Vec3(ex + fx * 0.35 + rxv * 0.2, ey + 0.55, ez + fz * 0.35 + rzv * 0.2);
            Vec3 under = new Vec3(ex + fx * 0.35 - rxv * 0.25, ey + 0.6, ez + fz * 0.35 - rzv * 0.25);
            Vec3 pour = glass.add(rxv * 0.28, 0.36, rzv * 0.28);   // así la boca de la botella inclinada queda justo sobre el vaso
            Player p = Minecraft.getInstance().player;
            Vec3 give = p != null && p.distanceToSqr(ex, ey, ez) < 49 ? p.position().add(0, 1.15, 0) : front.add(fx * 0.8, 1.2, fz * 0.8);
            // mano derecha: botella
            if (t < 12) {
                armR(shelf, smooth(t / 6));
                if (back) {
                    twist = 0.9F * smooth(t / 6);
                }
                look(shelf, 0.8F);
                if (t > 7) {
                    propR = DealerProps.BOTTLE;
                }
            } else if (t < 40) {
                propR = DealerProps.BOTTLE;
                Vec3 h = t < 18 ? lerp(shelf, pour, smooth((t - 12) / 6)) : pour;
                armR(h, 1);
                if (back) {
                    twist = 0.9F * (1 - smooth((t - 12) / 6));
                }
                tiltR = t >= 20 ? smooth((t - 20) / 4) * (1 - smooth((t - 34) / 5)) : 0;
                look(glass, 0.8F);
            } else if (t < 48) {
                armR(lerp(pour, shelf, smooth((t - 40) / 5)), 1 - smooth((t - 44) / 4));
                if (t < 45) {
                    propR = DealerProps.BOTTLE;
                }
            }
            // mano izquierda: vaso
            if (t >= 10 && t < 18) {
                armL(lerp(under, glass, smooth((t - 13) / 5)), smooth((t - 10) / 3));
                if (t > 13) {
                    propL = DealerProps.GLASS_EMPTY;
                }
            } else if (t >= 18 && t < 46) {
                armL(glass, 1);
                propL = t >= 28 ? DealerProps.GLASS_FULL : DealerProps.GLASS_EMPTY;
            } else if (t >= 46 && t < DealerAnim.SERVE_HANDOVER + 2) {
                Vec3 h = lerp(glass, lerp(glass, give, 0.55), smooth((t - 46) / 8));
                armL(h, 1);
                propL = DealerProps.GLASS_FULL;
                look(give, 0.7F);
            } else if (t >= DealerAnim.SERVE_HANDOVER + 2) {
                armL(lerp(glass, give, 0.55), 1 - smooth((t - DealerAnim.SERVE_HANDOVER - 2) / 8));
            }
            return;
        }
        if (e.animId == DealerAnim.SHAKER) {
            propR = DealerProps.SHAKER;
            return;
        }
        // sin pedido: pule un vaso con el trapo (si no está hablando ni haciendo otra cosa)
        if (e.animId == DealerAnim.NONE && e.getTip() < 0) {
            double a = time * 0.35;
            Vec3 g = glass.add(0, -0.05, 0);
            armL(g, 1);
            armR(g.add(rxv * 0.06 + Math.cos(a) * 0.05 * fx, 0.08 + Math.sin(a) * 0.06, rzv * 0.06 + Math.cos(a) * 0.05 * fz), 1);
            propL = DealerProps.GLASS_EMPTY;
            propR = DealerProps.RAG;
            look(g, 0.35F);
        }
    }
}
