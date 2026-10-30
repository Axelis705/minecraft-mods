package com.scardax.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Puente servidor -> cliente (solo se carga en el cliente). */
public final class ClientBridge {
    private ClientBridge() {
    }

    public static void rules(boolean open, boolean strict, Map<ResourceLocation, Integer> rules) {
        ClientRules.replace(rules);
        ClientRules.strict = strict;
        Minecraft mc = Minecraft.getInstance();
        if (open) {
            mc.setScreen(new MobRulesScreen());
        } else if (mc.screen instanceof MobRulesScreen s) {
            s.refresh();
        }
    }
}
