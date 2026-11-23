package com.coleccionista.evento;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** La pausa del organizador: los vivos se quedan quietos donde estaban (los fantasmas siguen libres). */
final class Pausa {
    private static final UUID MOD_ID = UUID.fromString("8c1d6a3e-2f5b-4c77-9a40-c0ec0c10b5a1");
    private static final Map<UUID, Vec3> DONDE = new HashMap<>();

    private Pausa() {
    }

    static void congelar(MinecraftServer s, Evento ev, boolean si) {
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (e instanceof ServerPlayer sp) {
                AttributeInstance a = sp.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null) {
                    a.removeModifier(MOD_ID);
                    if (si) {
                        a.addTransientModifier(new AttributeModifier(MOD_ID, "pausa del evento", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                }
            } else if (e instanceof Mob m) {
                m.setNoAi(si);
            }
            if (si) {
                DONDE.put(p.id, e.position());
            }
        }
        if (!si) {
            DONDE.clear();
        }
    }

    /** Si alguien se movió (saltando o empujado), vuelve a su lugar. */
    static void mantener(MinecraftServer s, Evento ev) {
        for (Map.Entry<UUID, Vec3> d : DONDE.entrySet()) {
            ServerPlayer p = s.getPlayerList().getPlayer(d.getKey());
            if (p != null && !p.isSpectator() && p.position().distanceToSqr(d.getValue()) > 0.25) {
                p.teleportTo(p.serverLevel(), d.getValue().x, d.getValue().y, d.getValue().z, p.getYRot(), p.getXRot());
            }
        }
    }
}
