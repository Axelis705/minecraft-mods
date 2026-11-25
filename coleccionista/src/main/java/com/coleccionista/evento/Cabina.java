package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.fx.Fx;
import com.coleccionista.Coleccionista;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Las cabinas que viajan (elevador o vagón de tren): la manera de pasar de una sala a otra sin que se note el salto. La cabina de salida y
 * la de llegada son idénticas por dentro; cuando la cabina "viaja" (puerta cerrada, tiembla, suenan los cables o los rieles) cada quien
 * pasa a la otra en el mismo lugar y mirando igual, y al llegar se abre la puerta del otro lado.
 * <p>
 * Una cabina se describe con marcas: {@code cab.<id>.origen} (la esquina de referencia, con el giro de la cabina) y
 * {@code cab.<id>.puerta.a/.b} (las hojas de su puerta, bloques de puerta). El interior es una caja de {@code ancho x alto x fondo}
 * bloques a partir del origen (en la dirección de su giro).
 */
public final class Cabina {
    public enum Tipo {
        /** Elevador tranquilo (cables, motor). */
        ELEVADOR,
        /** Vagón de tren (traqueteo de las vías). */
        TREN,
        /** Ascensor viejo que baja a toda velocidad: sacudidas fuertes, chirridos, chispas y luces que parpadean. */
        CAIDA
    }

    /** A cada pasajero: el viaje en curso (para el parpadeo de las luces y las rayas de velocidad en su pantalla). */
    public static final String KEY_VIAJE = com.coleccionista.Coleccionista.MODID + ":viaje";

    /** Un viaje en curso. */
    private static final class Viaje {
        final String desde, hasta;
        final Tipo tipo;
        final int dura;
        final int ancho, alto, fondo;
        final Runnable alLlegar;
        int t;
        boolean pasaron;
        final List<Entity> van = new ArrayList<>();

        Viaje(String desde, String hasta, Tipo tipo, int dura, int ancho, int alto, int fondo, Runnable alLlegar) {
            this.desde = desde;
            this.hasta = hasta;
            this.tipo = tipo;
            this.dura = dura;
            this.ancho = ancho;
            this.alto = alto;
            this.fondo = fondo;
            this.alLlegar = alLlegar;
        }
    }

    private static final List<Viaje> VIAJES = new ArrayList<>();
    private static final Random R = new Random();
    /** Ticks de puerta cerrándose antes de arrancar y de frenado antes de abrir. */
    private static final int CIERRA = 20, FRENA = 25;

    private Cabina() {
    }

    public static void init() {
        AxelisCore.onServerTick(Cabina::tick);
    }

    public static boolean viajando() {
        return !VIAJES.isEmpty();
    }

    /** ¿Ese jugador está dentro de la cabina {@code id}? */
    public static boolean dentro(MinecraftServer s, String id, Entity e, int ancho, int alto, int fondo) {
        AABB c = caja(s, id, ancho, alto, fondo);
        return c != null && c.contains(e.position());
    }

    /** La caja del interior de la cabina (en el mundo), o null si no está construida. */
    public static AABB caja(MinecraftServer s, String id, int ancho, int alto, int fondo) {
        BlockPos o = Complejo.pos(s, "cab." + id + ".origen");
        if (o == null) {
            return null;
        }
        Vec3 a = Vec3.atLowerCornerOf(o);
        Vec3 b = a.add(local(ancho, alto, fondo, Complejo.yaw("cab." + id + ".origen")));
        return new AABB(a, b).inflate(0.05);
    }

    /** Un desplazamiento (x a lo ancho, y arriba, z al fondo) girado como la cabina. */
    private static Vec3 local(double x, double y, double z, float yaw) {
        double r = Math.toRadians(yaw);
        double c = Math.cos(r), s = Math.sin(r);
        return new Vec3(x * c - z * s, y, x * s + z * c);
    }

    /**
     * Arranca un viaje: cierra la puerta de {@code desde} con quien esté dentro, la cabina viaja {@code dura} ticks y llegan a {@code hasta}
     * (misma posición dentro de la cabina, mismo giro relativo). Al final abre la puerta de llegada y corre {@code alLlegar}.
     */
    public static boolean viajar(MinecraftServer s, String desde, String hasta, Tipo tipo, int dura, int ancho, int alto, int fondo,
            Runnable alLlegar) {
        return viajar(s, desde, hasta, tipo, dura, ancho, alto, fondo, null, alLlegar);
    }

    /** Lo mismo, pero solo con esas entidades (null: todos los que estén dentro). */
    public static boolean viajar(MinecraftServer s, String desde, String hasta, Tipo tipo, int dura, int ancho, int alto, int fondo,
            List<? extends Entity> quienes, Runnable alLlegar) {
        AABB c = caja(s, desde, ancho, alto, fondo);
        if (c == null || Complejo.pos(s, "cab." + hasta + ".origen") == null) {
            return false;
        }
        Viaje v = new Viaje(desde, hasta, tipo, dura, ancho, alto, fondo, alLlegar);
        if (quienes != null) {
            v.van.addAll(quienes);
        } else {
            v.van.addAll(s.overworld().getEntitiesOfClass(Entity.class, c, e -> e instanceof ServerPlayer || e instanceof com.coleccionista.bot.PiezaBot));
        }
        long now = s.overworld().getGameTime();
        for (Entity e : v.van) {
            if (e instanceof ServerPlayer sp) {
                net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
                t.putString("tipo", tipo.name());
                t.putLong("desde", now + CIERRA);
                t.putInt("dura", dura);
                com.axeliscore.data.PlayerStore.Sync.toPlayer(sp, KEY_VIAJE, t);
            }
        }
        puerta(s, desde, false);
        sonido(s, v, SoundEvents.IRON_DOOR_CLOSE, 1F, 0.6F);
        VIAJES.add(v);
        Coleccionista.LOGGER.info("COLECCIONISTA_CABINA sale {} -> {} van={}", desde, hasta, v.van.size());
        return true;
    }

    private static void tick(MinecraftServer s) {
        if (VIAJES.isEmpty()) {
            return;
        }
        for (Viaje v : new ArrayList<>(VIAJES)) {
            v.t++;
            int t = v.t - CIERRA;
            if (t == 0) {
                // arranca: un jalón
                sonido(s, v, v.tipo == Tipo.TREN ? SoundEvents.MINECART_RIDING : SoundEvents.ANVIL_PLACE, 0.8F, 0.5F);
                sacudir(v, v.tipo == Tipo.CAIDA ? 1.4F : 0.8F, 8);
                if (v.tipo == Tipo.CAIDA) {
                    // se suelta: chirrido de metal, el estómago al aire
                    sonido(s, v, SoundEvents.GRINDSTONE_USE, 1F, 0.5F);
                    sonido(s, v, SoundEvents.ELYTRA_FLYING, 0.7F, 1.3F);
                }
            }
            if (t > 0 && t < v.dura && v.tipo == Tipo.CAIDA) {
                // cae a toda velocidad: tiembla todo el tiempo, golpes fuertes al azar, chispas y el viento
                if (t % 8 == 0) {
                    sacudir(v, 0.45F + R.nextFloat() * 0.2F, 10);
                }
                if (t % 37 == 11 || R.nextInt(70) == 0) {
                    sacudir(v, 1.1F, 7);
                    sonido(s, v, R.nextBoolean() ? SoundEvents.ANVIL_LAND : SoundEvents.CHAIN_HIT, 0.7F, 0.5F + R.nextFloat() * 0.3F);
                }
                if (t % 23 == 5) {
                    sonido(s, v, com.coleccionista.registry.ModRegistry.CHISPA.get(), 0.8F, 0.7F + R.nextFloat() * 0.5F);
                }
                if (t % 50 == 25) {
                    sonido(s, v, SoundEvents.ELYTRA_FLYING, 0.6F, 1.2F + R.nextFloat() * 0.3F);
                }
                if (t % 31 == 17) {
                    sonido(s, v, SoundEvents.GRINDSTONE_USE, 0.6F, 0.45F + R.nextFloat() * 0.2F);
                }
                if (!v.pasaron && t >= v.dura / 2) {
                    pasar(s, v);
                    sonido(s, v, SoundEvents.ANVIL_LAND, 0.6F, 0.5F);
                    sacudir(v, 1.3F, 12);
                }
            } else if (t > 0 && t < v.dura) {
                if (v.tipo == Tipo.TREN) {
                    // el traqueteo de las vías
                    if (t % 9 == 0 || t % 9 == 3) {
                        sonido(s, v, SoundEvents.CHAIN_PLACE, 0.6F, 0.5F + R.nextFloat() * 0.15F);
                    }
                    if (t % 9 == 0) {
                        sacudir(v, 0.18F, 6);
                    }
                    if (t % 70 == 20) {
                        sonido(s, v, SoundEvents.MINECART_RIDING, 0.5F, 0.6F);
                    }
                } else {
                    // el elevador: cables, motor y algún golpe
                    if (t % 20 == 1) {
                        sacudir(v, 0.14F, 22);
                    }
                    if (t % 27 == 13) {
                        sonido(s, v, R.nextBoolean() ? SoundEvents.CHAIN_STEP : SoundEvents.CHAIN_HIT, 0.8F, 0.5F + R.nextFloat() * 0.3F);
                    }
                    if (t % 40 == 5) {
                        sonido(s, v, SoundEvents.BEACON_AMBIENT, 0.5F, 0.5F);
                    }
                }
                // a la mitad, con una sacudida fuerte que lo disimula, pasan a la otra cabina
                if (!v.pasaron && t >= v.dura / 2) {
                    pasar(s, v);
                    sonido(s, v, SoundEvents.ANVIL_LAND, 0.4F, 0.6F);
                    sacudir(v, 1.0F, 10);
                }
            }
            if (t == v.dura) {
                if (!v.pasaron) {
                    pasar(s, v);
                }
                if (v.tipo == Tipo.CAIDA) {
                    // el frenón: chirrido largo y el golpe del final
                    sonido(s, v, SoundEvents.GRINDSTONE_USE, 1F, 0.4F);
                    sacudir(v, 1.6F, 14);
                }
                sonido(s, v, SoundEvents.ANVIL_LAND, 0.7F, 0.5F);
                sonido(s, v, SoundEvents.BELL_BLOCK, 0.8F, 0.9F);
                sacudir(v, 0.9F, 8);
            }
            if (t >= v.dura + FRENA) {
                puerta(s, v.hasta, true);
                sonido(s, v, SoundEvents.IRON_DOOR_OPEN, 1F, 0.6F);
                VIAJES.remove(v);
                Coleccionista.LOGGER.info("COLECCIONISTA_CABINA llega {}", v.hasta);
                if (v.alLlegar != null) {
                    v.alLlegar.run();
                }
            }
        }
    }

    /** Cada quien pasa a la cabina de llegada en el mismo lugar relativo y mirando igual. */
    private static void pasar(MinecraftServer s, Viaje v) {
        v.pasaron = true;
        ServerLevel l = s.overworld();
        BlockPos a = Complejo.pos(s, "cab." + v.desde + ".origen"), b = Complejo.pos(s, "cab." + v.hasta + ".origen");
        if (a == null || b == null) {
            return;
        }
        float ya = Complejo.yaw("cab." + v.desde + ".origen"), yb = Complejo.yaw("cab." + v.hasta + ".origen");
        float giro = yb - ya;
        for (Entity e : v.van) {
            if (!e.isAlive()) {
                continue;
            }
            Vec3 rel = e.position().subtract(Vec3.atLowerCornerOf(a));
            Vec3 r = local(rel.x, rel.y, rel.z, giro);
            Vec3 nuevo = Vec3.atLowerCornerOf(b).add(r);
            float yaw = e.getYRot() + giro;
            e.fallDistance = 0;
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(l, nuevo.x, nuevo.y, nuevo.z, yaw, sp.getXRot());
            } else {
                e.moveTo(nuevo.x, nuevo.y, nuevo.z, yaw, e.getXRot());
                e.setYHeadRot(yaw);
            }
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Abre o cierra la puerta de una cabina (sus hojas, de arriba y de abajo). */
    public static void puerta(MinecraftServer s, String id, boolean abrir) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {"a", "b"}) {
            BlockPos p = Complejo.pos(s, "cab." + id + ".puerta." + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
        }
    }

    private static void sacudir(Viaje v, float fuerza, int ticks) {
        for (Entity e : v.van) {
            if (e instanceof ServerPlayer sp) {
                Fx.shake(sp, fuerza, ticks);
            }
        }
    }

    private static void sonido(MinecraftServer s, Viaje v, SoundEvent ev, float vol, float tono) {
        for (Entity e : v.van) {
            if (e instanceof ServerPlayer sp) {
                sp.playNotifySound(ev, SoundSource.BLOCKS, vol, tono);
            }
        }
    }

    public static void reiniciar(MinecraftServer s) {
        VIAJES.clear();
    }
}
