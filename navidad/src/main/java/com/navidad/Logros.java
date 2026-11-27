package com.navidad;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Los logros de la pestaña navideña (los JSON los escribe {@code herramientas/logros.py}); casi todos se dan desde aquí. */
public final class Logros {
    private Logros() {
    }

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Logros::alPoner);
    }

    static void alPoner(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getPlacedBlock().getBlock() == com.navidad.bloque.deco.Deco.block("pesebre")) {
            dar(p, "nacimiento");
        }
    }

    /** Le da a alguien el logro {@code navidad:navidad/<id>} (si ya lo tenía, no pasa nada). */
    public static void dar(ServerPlayer p, String id) {
        if (p == null) {
            return;
        }
        Advancement a = p.server.getAdvancements().getAdvancement(new ResourceLocation(Navidad.MODID, "navidad/" + id));
        if (a != null && p.getAdvancements().award(a, "hecho")) {
            Navidad.LOGGER.info("NAVIDAD_LOGRO {} {}", p.getGameProfile().getName(), id);
        }
        // el Libro de Navidad cuenta los logros (aunque ya lo tuviera de antes: así una tarea nueva se puede completar)
        com.navidad.libro.Libro.evento(p, "logro:" + id, 1);
    }
}
