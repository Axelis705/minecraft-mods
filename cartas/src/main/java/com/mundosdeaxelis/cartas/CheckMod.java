package com.mundosdeaxelis.cartas;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Mod de comprobacion: carga el sistema de cartas igual que lo hara Mundos de Axelis, para probarlo en un servidor real. */
@Mod(Cartas.MOD_ID)
public final class CheckMod {
    public CheckMod() {
        Cartas.init(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
