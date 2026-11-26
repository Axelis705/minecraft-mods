package com.navidad.client;

import com.axeliscore.test.ClientTest;
import com.axeliscore.vestir.client.Prenda;
import com.axeliscore.vestir.client.VestuarioCliente;
import com.navidad.Navidad;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Lo del cliente: renderizadores, la ropa del vestuario y el escenario de la prueba con fotos. */
@Mod.EventBusSubscriber(modid = Navidad.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    private ModClient() {
    }

    private static ResourceLocation rl(String p) {
        return new ResourceLocation(Navidad.MODID, p);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModRegistry.PINATA_ENT.get(), PinataRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // gorros y cuernos: modelos de Blockbench pegados a la cabeza
            for (String g : Ropa.CABEZA) {
                VestuarioCliente.registrar(Ropa.id(g), Prenda.modelo(rl("geo/" + g + ".geo.json"), rl("textures/prenda/" + g + ".png")));
            }
            // suéteres: tipo piel, sin cabeza ni piernas
            for (String s : new String[]{"sueter_reno", "sueter_pinos", "sueter_copos"}) {
                VestuarioCliente.registrar(Ropa.id(s), Prenda.piel(rl("textures/prenda/" + s + ".png"))
                        .sin(Prenda.Parte.CABEZA, Prenda.Parte.PIERNA_DER, Prenda.Parte.PIERNA_IZQ));
            }
            // el traje de Santa: todo el cuerpo y la barba
            VestuarioCliente.registrar(Ropa.id("traje_santa"), Prenda.piel(rl("textures/prenda/traje_santa.png")));
        });
        Prueba.registrar();
    }
}
