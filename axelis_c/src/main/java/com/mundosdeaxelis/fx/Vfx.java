package com.mundosdeaxelis.fx;

import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.VfxPacket;
import com.mundosdeaxelis.vfx.VfxTex;
import com.mundosdeaxelis.vfx.VfxType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Efectos visuales grandes dentro del mundo (anillos, círculos rúnicos, columnas de luz, rayos, relámpagos, cortes,
 * cúpulas, tornados, sprites...). El servidor solo manda "crea / mueve / quita"; el cliente los anima y los dibuja
 * (ver client/VfxClient). Todos los métodos devuelven el número del efecto para poder moverlo o quitarlo después.
 */
public final class Vfx {
    private Vfx() {
    }

    private static final AtomicInteger IDS = new AtomicInteger(1);
    private static final double RANGE = 96.0;

    /** Constructor de un efecto: se llenan los datos y al final send(). */
    public static final class B {
        private final ServerLevel level;
        private final int type;
        private int rgb = 0xFFFFFF, flags, life = 20, follow;
        private float size = 1F, a, b;
        private double x, y, z, dx, dy, dz;

        private B(ServerLevel level, int type) {
            this.level = level;
            this.type = type;
        }

        public B at(Vec3 p) {
            x = p.x;
            y = p.y;
            z = p.z;
            return this;
        }

        public B at(double px, double py, double pz) {
            x = px;
            y = py;
            z = pz;
            return this;
        }

        public B rgb(int c) {
            rgb = c;
            return this;
        }

        public B size(double s) {
            size = (float) s;
            return this;
        }

        public B a(double v) {
            a = (float) v;
            return this;
        }

        public B b(double v) {
            b = (float) v;
            return this;
        }

        public B life(int ticks) {
            life = ticks;
            return this;
        }

        public B flags(int f) {
            flags |= f;
            return this;
        }

        public B style(int s) {
            flags = VfxType.withStyle(flags, s);
            return this;
        }

        public B dir(Vec3 v) {
            dx = v.x;
            dy = v.y;
            dz = v.z;
            return this;
        }

        public B dir(double ddx, double ddy, double ddz) {
            dx = ddx;
            dy = ddy;
            dz = ddz;
            return this;
        }

        /** El efecto acompaña a una entidad; la posición pasa a ser el desplazamiento respecto a ella. */
        public B follow(Entity e, double ox, double oy, double oz) {
            follow = e.getId();
            x = ox;
            y = oy;
            z = oz;
            return this;
        }

        private VfxPacket packet(int action, int id) {
            return new VfxPacket(action, id, type, rgb, flags, life, follow, size, a, b, x, y, z, dx, dy, dz);
        }

        /** Lo crea para los jugadores cercanos y devuelve su número. */
        public int send() {
            int id = IDS.getAndIncrement();
            Vec3 where = new Vec3(x, y, z);
            if (follow > 0) {
                Entity e = level.getEntity(follow);
                if (e != null) {
                    where = e.position();
                }
            }
            final Vec3 w = where;
            ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                    new PacketDistributor.TargetPoint(w.x, w.y, w.z, RANGE, level.dimension())), packet(VfxType.ACT_ADD, id));
            return id;
        }

        /** Vuelve a mandar la creación de un efecto que ya existe (mismo número): sirve para refrescarlo y para quien llegó tarde. */
        public void resend(int id) {
            if (id <= 0) {
                return;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                    new PacketDistributor.TargetPoint(x, y, z, RANGE, level.dimension())), packet(VfxType.ACT_ADD, id));
        }

        /** Cambia posición, tamaño, color... de un efecto que ya existe. life > 0 = ticks que le quedan. */
        public void update(int id) {
            if (id <= 0) {
                return;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet(VfxType.ACT_UPDATE, id));
        }
    }

    public static B make(ServerLevel level, int type) {
        return new B(level, type);
    }

    public static void remove(ServerLevel level, int id) {
        if (id <= 0) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), VfxPacket.remove(id));
    }

    // ------------------------------------------------------------------ atajos

    /** Anillo plano que se expande por el suelo (onda de choque). */
    public static int ring(ServerLevel l, Vec3 pos, int rgb, double radius, int life) {
        return make(l, VfxType.RING).at(pos).rgb(rgb).size(radius).life(life).send();
    }

    /** Anillo en cualquier plano (normal = hacia dónde mira). */
    public static int ringFacing(ServerLevel l, Vec3 pos, Vec3 normal, int rgb, double radius, int life) {
        return make(l, VfxType.RING).at(pos).dir(normal).flags(VfxType.F_NORMAL).rgb(rgb).size(radius).life(life).send();
    }

    /** Círculo rúnico que gira en el suelo. */
    public static int rune(ServerLevel l, Vec3 pos, int rgb, double radius, int life) {
        return make(l, VfxType.RUNE).at(pos).rgb(rgb).size(radius).life(life).send();
    }

    /** Círculo rúnico que sigue a una entidad (en sus pies). */
    public static int runeFollow(ServerLevel l, Entity e, int rgb, double radius, int life) {
        return make(l, VfxType.RUNE).follow(e, 0, 0.04, 0).rgb(rgb).size(radius).life(life).send();
    }

    /** Círculo rúnico vertical (como un sello delante del jugador). */
    public static int runeFacing(ServerLevel l, Vec3 pos, Vec3 normal, int rgb, double radius, int life) {
        return make(l, VfxType.RUNE).at(pos).dir(normal).flags(VfxType.F_NORMAL).rgb(rgb).size(radius).life(life).send();
    }

    public static int column(ServerLevel l, Vec3 pos, int rgb, double radius, double height, int life) {
        return make(l, VfxType.COLUMN).at(pos).rgb(rgb).size(radius).a(height).life(life).send();
    }

    public static int beam(ServerLevel l, Vec3 from, Vec3 to, int rgb, double width, int life, int flags) {
        return make(l, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(rgb).size(width).life(life).flags(flags).send();
    }

    /** Hilo que cuelga entre dos puntos, con brillo en los extremos. */
    public static int thread(ServerLevel l, Vec3 from, Vec3 to, int rgb, double width, int life) {
        return make(l, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(rgb).size(width).life(life)
                .flags(VfxType.F_CURVE | VfxType.F_CAPS).a(0.04).b(4).send();
    }

    public static int bolt(ServerLevel l, Vec3 from, Vec3 to, int rgb, double width, int life) {
        return make(l, VfxType.BOLT).at(from).dir(to.subtract(from)).rgb(rgb).size(width).life(life).send();
    }

    /** Corte de energía que avanza (travel = hacia dónde y cuánto), roll = giro en radianes. */
    public static int slash(ServerLevel l, Vec3 pos, Vec3 travel, int rgb, double size, double roll, int life) {
        return make(l, VfxType.SLASH).at(pos).dir(travel).rgb(rgb).size(size).a(roll).life(life).send();
    }

    public static int flash(ServerLevel l, Vec3 pos, int rgb, double size, int life) {
        return make(l, VfxType.FLASH).at(pos).rgb(rgb).size(size).life(life).send();
    }

    public static int orb(ServerLevel l, Vec3 pos, int rgb, double size, int life, boolean trail) {
        B b = make(l, VfxType.ORB).at(pos).rgb(rgb).size(size).life(life);
        if (trail) {
            b.flags(VfxType.F_TRAIL);
        }
        return b.send();
    }

    public static int dome(ServerLevel l, Vec3 pos, int rgb, double radius, int life, boolean hemi) {
        B b = make(l, VfxType.DOME).at(pos).rgb(rgb).size(radius).life(life);
        if (hemi) {
            b.flags(VfxType.F_HEMI);
        }
        return b.send();
    }

    /** Esfera de luz alrededor de una entidad (su centro). */
    public static int domeFollow(ServerLevel l, Entity e, int rgb, double radius, int life) {
        return make(l, VfxType.DOME).follow(e, 0, e.getBbHeight() * 0.5, 0).rgb(rgb).size(radius).life(life).send();
    }

    public static int funnel(ServerLevel l, Vec3 pos, int rgb, double radius, double height, int life) {
        return make(l, VfxType.FUNNEL).at(pos).rgb(rgb).size(radius).a(height).life(life).send();
    }

    /** Sprite animado que mira al jugador (dragón, hada, zzz...). dir sirve para voltearlo según hacia dónde va. */
    public static int sprite(ServerLevel l, Vec3 pos, int tex, double size, Vec3 dir, int rgb, boolean additive, int life) {
        B b = make(l, VfxType.SPRITE).at(pos).a(tex).size(size).rgb(rgb).life(life);
        if (dir != null) {
            b.dir(dir);
        }
        if (additive) {
            b.flags(VfxType.F_ADD);
        }
        return b.send();
    }

    public static int spriteFollow(ServerLevel l, Entity e, double ox, double oy, double oz, int tex, double size, int rgb, boolean additive, int life) {
        B b = make(l, VfxType.SPRITE).follow(e, ox, oy, oz).a(tex).size(size).rgb(rgb).life(life);
        if (additive) {
            b.flags(VfxType.F_ADD);
        }
        return b.send();
    }

    /** Dibujo en planos cruzados que crece (árbol del mundo) o estandarte (style 1). */
    public static int cross(ServerLevel l, Vec3 pos, int tex, double size, int growTicks, int style, Vec3 facing, int rgb, int life) {
        B b = make(l, VfxType.CROSS).at(pos).b(tex).a(growTicks).size(size).rgb(rgb).life(life).style(style);
        if (facing != null) {
            b.dir(facing);
        }
        return b.send();
    }

    /** Colmillos que salen del suelo en círculo. */
    public static int maw(ServerLevel l, Vec3 pos, int rgb, double radius, double height, int life) {
        return make(l, VfxType.MAW).at(pos).rgb(rgb).size(radius).a(height).life(life).send();
    }

    /** Látigo / cinta ondulante: voidStyle = true para el estilo oscuro. */
    public static int whip(ServerLevel l, Vec3 from, Vec3 to, int rgb, double width, int life, boolean voidStyle, double amp, double waves) {
        B b = make(l, VfxType.RIBBON).at(from).dir(to.subtract(from)).rgb(rgb).size(width).life(life)
                .flags(VfxType.F_GROW).a(amp).b(waves);
        if (voidStyle) {
            b.style(1);
        }
        return b.send();
    }

    /** Moneda que sube, gira y cae mostrando una cara (0 = cara, 1 = cruz). */
    public static int coin(ServerLevel l, Vec3 pos, int face, double height, int life) {
        return make(l, VfxType.COIN).at(pos).size(0.34).a(height).b(face).life(life).send();
    }

    public static int tex(String name) {
        for (int i = 0; i < VfxTex.FILE.length; i++) {
            if (VfxTex.FILE[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }
}
