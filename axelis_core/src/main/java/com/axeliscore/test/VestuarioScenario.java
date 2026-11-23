package com.axeliscore.test;

import com.axeliscore.vestir.client.VestuarioCliente;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Prueba con cliente del Vestuario: un overol (tipo piel, con número) y un sombrero (modelo de Blockbench) puestos al jugador y a una persona. */
final class VestuarioScenario {
    private VestuarioScenario() {
    }

    static void vestuario(ClientTest.Script t) {
        t.add("vestuario: poner", 1500, () -> {
            t.cmd("time set noon");
            t.cmd("gamemode creative @a");
            t.cmd("tp @a 0 -60 40 180 0");
            t.cmd("kill @e[type=axelis_core:persona]");
            t.cmd("summon axelis_core:persona 1.5 -60 37.5");
            t.cmd("axeliscore vestir @a axelis_core:overol numero=12");
            t.cmd("axeliscore vestir @a axelis_core:sombrero");
        });
        t.add("vestuario: persona", 800, () -> {
            t.cmd("axeliscore vestir @e[type=axelis_core:persona] axelis_core:overol numero=07");
            t.cmd("axeliscore vestir @e[type=axelis_core:persona] axelis_core:sombrero");
        });
        t.add("vestuario puesto", 100, () -> {
            var p = Minecraft.getInstance().player;
            t.check(VestuarioCliente.de(p.getUUID()).size() == 2, "llevo el overol y el sombrero (" + VestuarioCliente.de(p.getUUID()).size() + ")");
        });
        t.photo("30_vestuario_frente", 1200, () -> {
            t.hud(false);
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        t.photo("30_vestuario_espalda", 1000, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        t.photo("30_vestuario_persona", 1000, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.lookFrom(1.5, -60, 40.5, 1.5, -59, 37.5);
        });
        t.add("vestuario: quitar", 800, () -> {
            t.hud(true);
            t.cmd("axeliscore desvestir @a");
            t.cmd("kill @e[type=axelis_core:persona]");
        });
        t.add("vestuario quitado", 100, () -> t.check(VestuarioCliente.de(Minecraft.getInstance().player.getUUID()).isEmpty(), "ya no llevo nada"));
    }
}
