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

    /** Deja el cuerpo de la pieza donde está su entidad (antes de volverse fantasma). */
    static void dejar(Entity e, Pieza p) {
        if (!(e.level() instanceof ServerLevel l)) {
            return;
        }
        Cadaver c = ModRegistry.CADAVER.get().create(l);
        if (c == null) {
            return;
        }
        c.moveTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), 0);
        boolean estatua = ESTATUAS.remove(p.id);
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
