package com.axeliscore;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.List;

/** Qué mods instalados dependen del Core (los que lo piden en su mods.toml). */
public final class CoreUsers {
    private CoreUsers() {
    }

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        for (IModInfo m : ModList.get().getMods()) {
            if (m.getModId().equals(AxelisCore.MODID)) {
                continue;
            }
            for (IModInfo.ModVersion dep : m.getDependencies()) {
                if (AxelisCore.MODID.equals(dep.getModId())) {
                    out.add(m.getModId());
                    break;
                }
            }
        }
        return out;
    }
}
