package com.showdeaxelis;

import com.showdeaxelis.net.ShowNet;
import com.showdeaxelis.registry.ModEntities;
import com.showdeaxelis.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Show de Axelis: un concurso de television para jugar con amigos (100 mexicanos dijeron, trivia, El Impostor...). */
@Mod(ShowMod.MOD_ID)
public final class ShowMod {
    public static final String MOD_ID = "show_de_axelis";

    public ShowMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUNDS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ShowNet.init();
    }
}
