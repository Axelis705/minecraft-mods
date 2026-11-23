package com.coreprueba;

import com.axeliscore.AxelisCore;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Mod mínimo que usa Axelis Core: solo para las pruebas automáticas (comprueba que el Core va dentro del jar y carga). */
@Mod(CorePrueba.MODID)
public class CorePrueba {
    public static final String MODID = "core_prueba";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CorePrueba(FMLJavaModLoadingContext context) {
        AxelisCore.use(MODID);
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void commands(RegisterCommandsEvent e) {
            e.getDispatcher().register(Commands.literal("coreprueba").executes(c -> {
                c.getSource().sendSuccess(() -> Component.literal("core_prueba: " + AxelisCore.version()), false);
                LOGGER.info("COREPRUEBA_OK {}", AxelisCore.version());
                return 1;
            }));
        }
    }
}
