package com.navidad.client;

import com.axeliscore.client.SimpleGeoRenderer;
import com.axeliscore.combat.client.JefeRenderer;
import com.axeliscore.vestir.client.Prenda;
import com.axeliscore.vestir.client.VestuarioCliente;
import com.navidad.Navidad;
import com.navidad.entidad.TrineoMadera;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
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
        e.registerEntityRenderer(ModRegistry.MUNECO_MALVADO.get(), c -> new SimpleGeoRenderer<>(c, Navidad.MODID, "muneco_malvado", 0.5F, 1F));
        e.registerEntityRenderer(ModRegistry.RENO.get(), c -> new SimpleGeoRenderer<>(c, Navidad.MODID, "reno", 0.6F, 1.05F));
        e.registerEntityRenderer(ModRegistry.HOMBRE_JENGIBRE.get(), JengibreRenderer::new);
        e.registerEntityRenderer(ModRegistry.TRINEO.get(), TrineoRenderer::new);
        e.registerEntityRenderer(ModRegistry.TRINEO_MADERA.get(), TrineoMaderaRenderer::new);
        e.registerEntityRenderer(ModRegistry.REGALO_CAYENDO.get(), RegaloCayendoRenderer::new);
        e.registerEntityRenderer(ModRegistry.DUENDE_LADRON.get(), c -> new com.axeliscore.client.PersonaRenderer<com.navidad.entidad.DuendeLadron>(c,
                d -> new net.minecraft.resources.ResourceLocation(Navidad.MODID, "textures/entity/duende_ladron.png")) {
            @Override
            protected float modelScale(com.navidad.entidad.DuendeLadron d) {
                return 0.6F;
            }
        });
        e.registerEntityRenderer(ModRegistry.RENITO.get(), c -> new SimpleGeoRenderer<>(c, Navidad.MODID, "renito", 0.4F, 0.55F));
        e.registerEntityRenderer(ModRegistry.PINGUINO.get(), c -> new SimpleGeoRenderer<>(c, Navidad.MODID, "pinguino", 0.35F, 1F) {
            @Override
            protected float modelScale(com.navidad.entidad.Pinguino p) {
                return p.isBaby() ? 0.55F : 1F;
            }
        });
        e.registerEntityRenderer(ModRegistry.KRAMPUS.get(), c -> new JefeRenderer<>(c, Navidad.MODID, "krampus", 1.2F, 1.1F, 2));
        e.registerBlockEntityRenderer(ModRegistry.PINO_NAVIDAD_BE.get(), PinoRenderer::new);
        e.registerBlockEntityRenderer(ModRegistry.CAJA_REGALO_BE.get(), CajaRenderer::new);
    }

    /** En temporada, los mobs con cabeza de persona salen con gorro de Santa o de duende. */
    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.AddLayers e) {
        GorrosMobs.agregar(e);
    }

    /** Modelos sueltos (no son de ningún bloque): el trineo de madera. */
    @SubscribeEvent
    public static void onModelos(ModelEvent.RegisterAdditional e) {
        e.register(TrineoMaderaRenderer.MODELO);
        e.register(RegaloCayendoRenderer.PARACAIDAS);
        for (int c = 0; c < 4; c++) {
            e.register(CajaRenderer.CUERPO[c]);
            e.register(CajaRenderer.TAPA[c]);
            e.register(RegaloCayendoRenderer.CAJA[c]);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        TrineoMadera.alAparecer = TrineoMaderaRenderer::sonar;
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
            // los patines: una bota con cuchilla en cada pierna
            VestuarioCliente.registrar(Ropa.id("patines"), Prenda.modelo(rl("geo/patines.geo.json"), rl("textures/prenda/patines.png")));
            // la bufanda: en el cuello, con las puntas al frente
            VestuarioCliente.registrar(Ropa.id("bufanda"), Prenda.modelo(rl("geo/bufanda.geo.json"), rl("textures/prenda/bufanda.png")));
            // el traje de Santa: todo el cuerpo y la barba
            VestuarioCliente.registrar(Ropa.id("traje_santa"), Prenda.piel(rl("textures/prenda/traje_santa.png")));
        });
        SantaAviso.init();
        Prueba.registrar();
    }
}
