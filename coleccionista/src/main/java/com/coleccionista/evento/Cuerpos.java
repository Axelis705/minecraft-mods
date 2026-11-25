package com.coleccionista.evento;

import com.coleccionista.Coleccionista;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Los cuerpos de los eliminados: se dejan donde cayeron y se quitan al reiniciar el evento o con {@code /evento cuerpos limpiar}. */
public final class Cuerpos {
    private Cuerpos() {
    }

    /** Quiénes, al ser eliminados, quedan como estatua de cera sentada (la vitrina) en lugar de tirados en el suelo. */
    public static final java.util.Set<java.util.UUID> ESTATUAS = new java.util.HashSet<>();
    /** Quiénes, al ser eliminados, se van de espaldas con su silla (la caja de música: el balazo). */
    public static final java.util.Set<java.util.UUID> EN_SILLA = new java.util.HashSet<>();
    /** Quiénes, al ser eliminados, se envenenaron en el salón de té: verdes, y se van de espaldas con su silla. */
    public static final java.util.Set<java.util.UUID> VENENO = new java.util.HashSet<>();
    /** Quiénes, al ser eliminados, no dejan cuerpo (se los comió Cascabel). */
    public static final java.util.Set<java.util.UUID> SIN_CUERPO = new java.util.HashSet<>();

    /** Deja el cuerpo de la pieza donde está su entidad (antes de volverse fantasma). */
    static void dejar(Entity e, Pieza p) {
        if (!(e.level() instanceof ServerLevel l)) {
            return;
        }
        if (SIN_CUERPO.remove(p.id)) {
            Coleccionista.LOGGER.info("COLECCIONISTA_CUERPO sin_cuerpo {} {}", p.etiqueta(), p.nombre);
            return;
        }
        Cadaver c = ModRegistry.CADAVER.get().create(l);
        if (c == null) {
            return;
        }
        if (VENENO.remove(p.id) && e.getVehicle() instanceof Silla sv) {
            // el veneno: el cuerpo verde se va de espaldas con la silla (la dibuja el cliente); la silla de verdad se quita
            c.moveTo(sv.getX(), sv.getY(), sv.getZ(), sv.getYRot(), 0);
            c.de(p.id, p.nombre, Cadaver.VENENO + Math.abs(p.id.hashCode()) % 3);
            c.setYRot(sv.getYRot());
            c.setNoGravity(true);
            c.caer((int) l.getGameTime());
            sv.soltar();
            sv.discard();
            l.addFreshEntity(c);
            Coleccionista.LOGGER.info("COLECCIONISTA_CUERPO veneno {} {}", p.etiqueta(), p.nombre);
            return;
        }
        if (EN_SILLA.remove(p.id) && e.getVehicle() instanceof Silla si) {
            // la silla se va de espaldas con la pieza (la dibuja el cliente); la silla de verdad se quita
            c.moveTo(si.getX(), si.getY(), si.getZ(), si.getYRot(), 0);
            c.de(p.id, p.nombre, Cadaver.EN_SILLA + Math.abs(p.id.hashCode()) % 3);
            c.setYRot(si.getYRot());
            c.setNoGravity(true);
            c.caer((int) l.getGameTime());
            si.soltar();
            si.discard();
            l.addFreshEntity(c);
            Coleccionista.LOGGER.info("COLECCIONISTA_CUERPO silla {} {}", p.etiqueta(), p.nombre);
            return;
        }
        boolean estatua = ESTATUAS.remove(p.id);
        // la estatua queda de pie en el piso de la vitrina (aunque la pieza iba sentada)
        double y = estatua && e.getVehicle() != null ? e.getVehicle().getY() : e.getY();
        c.moveTo(e.getX(), y, e.getZ(), e.getYRot(), 0);
        c.de(p.id, p.nombre, estatua ? Cadaver.ESTATUA + Math.abs(p.id.hashCode()) % 3 : Math.abs(p.id.hashCode()) % 4);
        if (estatua) {
            c.setYRot(e.getVehicle() != null ? e.getVehicle().getYRot() : e.getYRot());
            c.setNoGravity(true);
        }
        l.addFreshEntity(c);
        l.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 0.4, e.getZ(), 14, 0.3, 0.2, 0.3, 0.03);
        Coleccionista.LOGGER.info("COLECCIONISTA_CUERPO {} {}", p.etiqueta(), p.nombre);
    }

    /** Quita todos los cuerpos de todos los mundos. Devuelve cuántos quitó. */
    public static int limpiar(MinecraftServer s) {
        int n = 0;
        for (ServerLevel l : s.getAllLevels()) {
            List<Entity> quitar = new ArrayList<>();
            for (Entity e : l.getAllEntities()) {
                if (e instanceof Cadaver) {
                    quitar.add(e);
                }
            }
            for (Entity e : quitar) {
                e.discard();
                n++;
            }
        }
        return n;
    }
}
