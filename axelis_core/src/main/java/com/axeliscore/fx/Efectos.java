package com.axeliscore.fx;

import com.axeliscore.net.CoreNet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * (Servidor) Efectos de partículas armados por capas (anillos, esferas, espirales, conos, chispas...), definidos en
 * {@code assets/<mod>/efectos/<nombre>.json} (o en la carpeta del estudio como {@code nombre.efecto.json}, que se recarga sola). El
 * servidor manda un solo mensaje y cada cliente arma las partículas.
 * <pre>
 *     Efectos.lanzar(level, "axelis_core:explosion_magica", pos);
 *     Efectos.enEntidad(jefe, "axelis_core:chispas", "right_hand");   // sale del hueso (si es del Core) o del centro
 * </pre>
 * Muestras del Core: chispas, explosion_magica, curacion, aparecer, fuego_circulo, hojas, nevada, subir_nivel, golpe, portal.
 * En Blockbench: un cuadro de partícula con el efecto "axelis_core:chispas" y el localizador (hueso) sale ahí al animar.
 */
public final class Efectos {
    private Efectos() {
    }

    public static void lanzar(ServerLevel l, String id, Vec3 pos) {
        lanzar(l, id, pos, Vec3.ZERO, 1F);
    }

    /** Con dirección (para conos, líneas y chorros) y tamaño (1 = normal). */
    public static void lanzar(ServerLevel l, String id, Vec3 pos, Vec3 dir, float escala) {
        CoreNet.EfectoMsg m = new CoreNet.EfectoMsg(id, pos.x, pos.y, pos.z, (float) dir.x, (float) dir.y, (float) dir.z, escala, -1, "");
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(pos) < 64 * 64) {
                CoreNet.NET.toPlayer(p, m);
            }
        }
    }

    /** Sale de un hueso de la entidad (si se dibuja con el Core) o de su centro; la dirección es hacia donde mira. */
    public static void enEntidad(Entity e, String id, String hueso) {
        if (!(e.level() instanceof ServerLevel l)) {
            return;
        }
        Vec3 dir = e.getLookAngle();
        CoreNet.EfectoMsg m = new CoreNet.EfectoMsg(id, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), (float) dir.x, (float) dir.y,
                (float) dir.z, 1F, e.getId(), hueso == null ? "" : hueso);
        CoreNet.NET.toNear(e, 64, m);
    }
}
