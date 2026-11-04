package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.cinematic.Subtitle;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Eventos del cliente en el bus del mod (renderers y capas de interfaz). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    /** Tecla para saltar las cinemáticas (Enter por defecto; se puede cambiar en Controles). */
    public static final KeyMapping SKIP = new KeyMapping("key." + MundosDeAxelis.MODID + ".skip_cinematic",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, "key.categories." + MundosDeAxelis.MODID);

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SKIP);
        event.register(EquipClient.MENU);
        event.register(EquipClient.RELIC);
        event.register(GraveHud.HIDE);
    }

    /** Sombreador propio de los efectos: mezcla suave sin cortes de transparencia. Si falla, se usa el de Minecraft. */
    @SubscribeEvent
    public static void registerShaders(net.minecraftforge.client.event.RegisterShadersEvent event) {
        try {
            event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),
                            new ResourceLocation(MundosDeAxelis.MODID, "vfx"), com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX),
                    shader -> VfxClient.shader = shader);
        } catch (Exception e) {
            MundosDeAxelis.LOGGER.error("No se pudo cargar el sombreador de efectos; se usa el de Minecraft", e);
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ReyBroteModel.BODY, ReyBroteModel::bodyLayer);
        event.registerLayerDefinition(ReyBroteModel.SHELL, ReyBroteModel::shellLayer);
        event.registerLayerDefinition(ReyBroteModel.SEED, ReyBroteModel::seedLayer);
        event.registerLayerDefinition(BroteModel.LAYER, BroteModel::createLayer);
        event.registerLayerDefinition(RaizalModel.LAYER, RaizalModel::createLayer);
        event.registerLayerDefinition(PolinizadorModel.LAYER, PolinizadorModel::createLayer);
        event.registerLayerDefinition(EspinosoModel.LAYER, EspinosoModel::createLayer);
        event.registerLayerDefinition(GuardianModel.LAYER, GuardianModel::createLayer);
        event.registerLayerDefinition(EscoriaModel.LAYER, EscoriaModel::createLayer);
        event.registerLayerDefinition(ChisperoModel.LAYER, ChisperoModel::createLayer);
        event.registerLayerDefinition(AscuaModel.LAYER, AscuaModel::createLayer);
        event.registerLayerDefinition(FundidorModel.LAYER, FundidorModel::createLayer);
        event.registerLayerDefinition(CapatazModel.LAYER, CapatazModel::createLayer);
        event.registerLayerDefinition(GranHerreroModel.LAYER, GranHerreroModel::createLayer);
        event.registerLayerDefinition(LoboArticoModel.LAYER, LoboArticoModel::createLayer);
        event.registerLayerDefinition(LoboEscarchaModel.LAYER, LoboEscarchaModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.mundosdeaxelis.registry.ModBlockEntities.CRATE.get(), CrateRenderer::new);
        event.registerBlockEntityRenderer(com.mundosdeaxelis.registry.ModBlockEntities.ARTIFICER_TABLE.get(), ArtificerTableRenderer::new);
        event.registerEntityRenderer(ModEntities.REY_BROTE.get(), ReyBroteRenderer::new);
        event.registerEntityRenderer(ModEntities.BROTE.get(), BroteRenderer::new);
        event.registerEntityRenderer(ModEntities.RAIZAL.get(), RaizalRenderer::new);
        event.registerEntityRenderer(ModEntities.POLINIZADOR.get(), PolinizadorRenderer::new);
        event.registerEntityRenderer(ModEntities.ESPINOSO.get(), EspinosoRenderer::new);
        event.registerEntityRenderer(ModEntities.GUARDIAN_MUSGO.get(), GuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.ESCORIA.get(), EscoriaRenderer::new);
        event.registerEntityRenderer(ModEntities.CHISPERO.get(), ChisperoRenderer::new);
        event.registerEntityRenderer(ModEntities.ASCUA.get(), AscuaRenderer::new);
        event.registerEntityRenderer(ModEntities.FUNDIDOR.get(), FundidorRenderer::new);
        event.registerEntityRenderer(ModEntities.CAPATAZ.get(), CapatazRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAN_HERRERO.get(), GranHerreroRenderer::new);
        event.registerEntityRenderer(ModEntities.LOBO_ARTICO.get(), LoboArticoRenderer::new);
        event.registerEntityRenderer(ModEntities.LOBO_ESCARCHA.get(), LoboEscarchaRenderer::new);
        // Los perritos de las mascotas se ven con su color.
    }

    @SubscribeEvent
    public static void registerParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.GLOW.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.GLOW));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.SPARKLE.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.SPARKLE));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.CONFETTI.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.CONFETTI));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.PETAL.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.CONFETTI));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.HEART.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.HEART));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.EMBER.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.EMBER));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.RUNE.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.RUNE));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.BOLT.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.BOLT));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.FEATHER.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.FEATHER));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.WISP.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.WISP));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.SHARD.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.SHARD));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.NOTE.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.NOTE));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.SLASH.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.SLASH));
        event.registerSpriteSet(com.mundosdeaxelis.particle.ModParticles.BUBBLE.get(),
                sprites -> (o, level, x, y, z, vx, vy, vz) -> new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites, GlowParticle.Kind.BUBBLE));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Cada mascota en brazos muestra el icono con su color (el número va de 0.005 a 0.145 según la mascota).
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(com.mundosdeaxelis.registry.ModMenus.EQUIP.get(), EquipScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(com.mundosdeaxelis.registry.ModMenus.TABLE.get(), TableScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Orden: lo último que se registra queda por encima.
        event.registerAboveAll("letterbox", (gui, graphics, partialTick, width, height) -> {
            float amount = CinematicClient.barAmount(partialTick);
            if (amount > 0.0F) {
                int barHeight = (int) (height * 0.12F * amount);
                graphics.fill(0, 0, width, barHeight, 0xFF000000);
                graphics.fill(0, height - barHeight, width, height, 0xFF000000);
            }
        });

        event.registerAboveAll("cinematic_subtitles", (gui, graphics, partialTick, width, height) -> {
            if (!CinematicClient.isActive()) {
                return;
            }
            float t = CinematicClient.time(partialTick);
            Font font = Minecraft.getInstance().font;
            // Pista para saltar, discreta, arriba a la derecha.
            String keyName = SKIP.getTranslatedKeyMessage().getString();
            if (keyName.equalsIgnoreCase("Entrar")) {
                keyName = "Enter";
            }
            Component hint = Component.literal("[" + keyName + "] Saltar");
            graphics.drawString(font, hint, width - font.width(hint) - 8, 6, 0x99FFFFFF);
            for (Subtitle s : CinematicClient.subtitles()) {
                if (t < s.start() || t > s.end()) {
                    continue;
                }
                float fade = Math.min(Math.min((t - s.start()) / 8.0F, (s.end() - t) / 8.0F), 1.0F);
                int alpha = Mth.clamp((int) (fade * 255.0F), 5, 255);
                List<FormattedCharSequence> lines = font.split(s.text(), (int) (width * 0.6F));
                int bottomBar = (int) (height * 0.12F * CinematicClient.barAmount(partialTick));
                int y = height - bottomBar - 16 - lines.size() * 12;
                int widest = 0;
                for (FormattedCharSequence line : lines) {
                    widest = Math.max(widest, font.width(line));
                }
                int bgAlpha = Mth.clamp((int) (fade * 235.0F), 0, 235);
                graphics.fill((width - widest) / 2 - 8, y - 5, (width + widest) / 2 + 8, y + lines.size() * 12 + 1, bgAlpha << 24);
                for (FormattedCharSequence line : lines) {
                    int x = (width - font.width(line)) / 2;
                    graphics.drawString(font, line, x, y, (alpha << 24) | 0xFFFFFF);
                    y += 12;
                }
            }
        });

        event.registerAboveAll("mundos_souls", (gui, graphics, partialTick, width, height) ->
                SoulsHud.render(graphics, partialTick, width, height));

        event.registerAboveAll("mundos_grave", (gui, graphics, partialTick, width, height) ->
                GraveHud.render(graphics, partialTick, width, height));
        event.registerAboveAll("mundos_bonus_key", (gui, graphics, partialTick, width, height) ->
                BonusKeyHud.render(graphics, partialTick, width, height));
        event.registerAboveAll("mundos_boss_damage", (gui, graphics, partialTick, width, height) ->
                BossDamageHud.render(graphics, partialTick, width, height));
        event.registerAboveAll("mundos_equip", (gui, graphics, partialTick, width, height) ->
                EquipClient.render(graphics, partialTick, width, height));

        event.registerAboveAll("mundos_boss_title", (gui, graphics, partialTick, width, height) ->
                BossTitleOverlay.render(graphics, partialTick, width, height));

        event.registerAboveAll("mundos_screen_fx", (gui, graphics, partialTick, width, height) ->
                ScreenFx.render(graphics, partialTick, width, height));

        event.registerAboveAll("mundos_notices", (gui, graphics, partialTick, width, height) ->
                NoticeHud.render(graphics, partialTick, width, height));
        event.registerAboveAll("mundos_loot", (gui, graphics, partialTick, width, height) ->
                LootHud.render(graphics, partialTick, width, height));
        event.registerAboveAll("mundos_cold", (gui, graphics, partialTick, width, height) ->
                ColdHud.render(graphics, partialTick, width, height));
        event.registerAboveAll("mundos_frozen", (gui, graphics, partialTick, width, height) ->
                FrozenClient.render(graphics, partialTick, width, height));

        event.registerAboveAll("fade", (gui, graphics, partialTick, width, height) -> {
            float a = FadeOverlay.alpha(partialTick);
            if (a > 0.0F) {
                int alpha = Math.min(255, Math.max(0, (int) (a * 255.0F)));
                graphics.fill(0, 0, width, height, (alpha << 24) | FadeOverlay.rgb());
            }
        });
    }
}
