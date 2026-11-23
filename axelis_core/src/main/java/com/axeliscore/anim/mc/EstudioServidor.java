package com.axeliscore.anim.mc;

import com.axeliscore.anim.Animated;
import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.CoreScreens;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * (Servidor) {@code /axeliscore estudio}: abre el editor de animaciones con la criatura animada que se esté mirando (operadores). Mientras
 * se edita, la criatura se queda quieta (sin IA) y al cerrar vuelve a la normalidad.
 */
public final class EstudioServidor {
    public static final String DATOS = AxelisCore.MODID + ":estudio", PANTALLA = AxelisCore.MODID + ":estudio",
            FIN = AxelisCore.MODID + ":estudio_fin";
    private static final Set<UUID> QUIETOS = new HashSet<>();
    private static boolean iniciado;

    private EstudioServidor() {
    }

    public static synchronized void init() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        Actions.on(FIN, (p, d) -> {
            Entity e = p.level().getEntity(Nbt.i(d, "ent"));
            if (e instanceof Mob m && QUIETOS.remove(m.getUUID())) {
                m.setNoAi(false);
            }
        });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nodo() {
        return Commands.literal("estudio").requires(Plat::isOp).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Entity e = mirando(p);
            if (e == null) {
                c.getSource().sendFailure(Component.literal("Mira a una criatura animada (a menos de 12 bloques) y vuelve a intentarlo"));
                return 0;
            }
            if (e instanceof Mob m && !m.isNoAi()) {
                m.setNoAi(true);
                QUIETOS.add(m.getUUID());
            }
            CompoundTag t = new CompoundTag();
            t.putInt("ent", e.getId());
            PlayerStore.Sync.toPlayer(p, DATOS, t);
            CoreScreens.open(p, PANTALLA);
            AxelisCore.LOGGER.info("AXCORE_ESTUDIO abierto {}", e.getType().getDescriptionId());
            return 1;
        });
    }

    /** La criatura animada que el jugador tiene más enfrente (o la más cercana). */
    static Entity mirando(ServerPlayer p) {
        Vec3 ojo = p.getEyePosition(), mira = p.getLookAngle();
        Entity mejor = null;
        double mejorPunto = 0.8;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(12), x -> x instanceof Animated)) {
            Vec3 d = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(ojo);
            double punto = d.normalize().dot(mira);
            if (punto > mejorPunto) {
                mejorPunto = punto;
                mejor = e;
            }
        }
        if (mejor == null) {
            double cerca = 36;
            for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(6), x -> x instanceof Animated)) {
                double d = e.distanceToSqr(p);
                if (d < cerca) {
                    cerca = d;
                    mejor = e;
                }
            }
        }
        return mejor;
    }
}
