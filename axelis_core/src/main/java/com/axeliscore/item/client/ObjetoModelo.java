package com.axeliscore.item.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.EstudioArchivos;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * (Cliente) Objetos con modelo de Blockbench: dibuja el geo.json de un {@link com.axeliscore.item.CoreItem} con {@code modelo3d(...)}
 * en la mano, en el suelo, en los marcos y en el inventario. El modelo se acomoda como un bloque: el piso de Blockbench (y = 0) queda abajo
 * del objeto y el centro (x = 0, z = 0) en medio. Cómo se ve en cada lugar (mano, suelo, inventario...) lo dice el modelo del objeto
 * ({@code axelis_core:item/modelo_3d}, o uno propio con su "display"). Lo que esté en la carpeta del estudio se recarga solo.
 */
public final class ObjetoModelo {
    private static final Map<Geo, Pose> POSES = new IdentityHashMap<>();
    private static final Map<Geo, GeoRenderer.Frame> FRAMES = new IdentityHashMap<>();

    private ObjetoModelo() {
    }

    /** Dibuja un modelo de objeto en el espacio de 0 a 1 del objeto (lo que entrega Minecraft después de las transformaciones). */
    public static void dibujar(ResourceLocation geoId, ResourceLocation tex, PoseStack ps, Draw d, int luz, int overlay) {
        Geo geo = GeoAssets.geo(geoId);
        if (geo == null || geo.bones.length == 0) {
            return;
        }
        if (POSES.size() > 64) {
            POSES.clear();
            FRAMES.clear();
        }
        Pose pose = POSES.computeIfAbsent(geo, g -> new Pose(g.bones.length));
        GeoRenderer.Frame f = FRAMES.computeIfAbsent(geo, GeoRenderer.Frame::new);
        pose.reset();
        ps.pushPose();
        // del espacio del objeto (0..1, y hacia arriba) al del modelo (como una entidad parada en el centro)
        ps.translate(0.5F, 0F, 0.5F);
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.compute(geo, pose, ps, f);
        ResourceLocation t = EstudioArchivos.textura(tex);
        GeoRenderer.draw(geo, pose, f, d.cutout(t), GeoRenderer.Pass.NORMAL, luz, overlay, 0xFFFFFFFF);
        for (Geo.Bone b : geo.bones) {
            if (b.glow && !b.cubes.isEmpty()) {
                GeoRenderer.draw(geo, pose, f, d.glow(t), GeoRenderer.Pass.GLOW, 0xF000F0, overlay, 0xFFFFFFFF);
                break;
            }
        }
        ps.popPose();
    }

    //#if MC >= 26.1
    //$$ /**
    //$$  * (26.x, al construir el mod) Agrega el tipo de dibujante especial {@code axelis_core:geo} a la lista de Minecraft (no hay evento
    //$$  * de Forge para esto). Un objeto lo usa en {@code assets/mod/items/nombre.json}.
    //$$  */
    //$$ @SuppressWarnings("unchecked")
    //$$ public static void registrar() {
    //$$     try {
    //$$         java.lang.reflect.Field f = net.minecraft.client.renderer.special.SpecialModelRenderers.class.getDeclaredField("ID_MAPPER");
    //$$         f.setAccessible(true);
    //$$         var mapper = (net.minecraft.util.ExtraCodecs.LateBoundIdMapper<ResourceLocation, com.mojang.serialization.MapCodec<
    //$$                 ? extends net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<?>>>) f.get(null);
    //$$         mapper.put(com.axeliscore.plat.Ids.core("geo"), Especial.CODEC);
    //$$     } catch (ReflectiveOperationException | RuntimeException e) {
    //$$         com.axeliscore.AxelisCore.LOGGER.error("Axelis Core: no se pudo registrar el dibujante de objetos 3D", e);
    //$$     }
    //$$ }
    //$$
    //$$ /** El dibujante especial de un objeto: su modelo y su textura (vienen del json del objeto). */
    //$$ public record Especial(ResourceLocation geo, ResourceLocation textura)
    //$$         implements net.minecraft.client.renderer.special.NoDataSpecialModelRenderer.Unbaked {
    //$$     public static final com.mojang.serialization.MapCodec<Especial> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(
    //$$             i -> i.group(ResourceLocation.CODEC.fieldOf("geo").forGetter(Especial::geo),
    //$$                     ResourceLocation.CODEC.fieldOf("textura").forGetter(Especial::textura)).apply(i, Especial::new));
    //$$
    //$$     @Override
    //$$     public com.mojang.serialization.MapCodec<Especial> type() {
    //$$         return CODEC;
    //$$     }
    //$$
    //$$     @Override
    //$$     public net.minecraft.client.renderer.special.NoDataSpecialModelRenderer bake(
    //$$             net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext c) {
    //$$         ResourceLocation g = geo, t = textura;
    //$$         return new net.minecraft.client.renderer.special.NoDataSpecialModelRenderer() {
    //$$             @Override
    //$$             public void submit(PoseStack ps, net.minecraft.client.renderer.SubmitNodeCollector out, int luz, int overlay, boolean foil,
    //$$                     int outline) {
    //$$                 Draw d = new Draw(out);
    //$$                 dibujar(g, t, ps, d, luz, overlay);
    //$$                 d.flush();
    //$$             }
    //$$
    //$$             @Override
    //$$             public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> out) {
    //$$                 for (int k = 0; k < 8; k++) {
    //$$                     out.accept(new org.joml.Vector3f(k & 1, (k >> 1) & 1, (k >> 2) & 1));
    //$$                 }
    //$$             }
    //$$         };
    //$$     }
    //$$ }
    //#else
    /** (1.20.1 y 1.21.1) Nada que registrar: cada objeto pide su dibujante con {@link #extension()}. */
    public static void registrar() {
    }

    private static net.minecraftforge.client.extensions.common.IClientItemExtensions ext;

    /** (1.20.1 y 1.21.1) La extensión que hace que Minecraft le pida a este dibujante los objetos con modelo 3D. */
    public static net.minecraftforge.client.extensions.common.IClientItemExtensions extension() {
        if (ext == null) {
            ext = new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                private Dibujante d;

                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (d == null) {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        d = new Dibujante(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                    }
                    return d;
                }
            };
        }
        return ext;
    }

    private static final class Dibujante extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
        Dibujante(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher bed, net.minecraft.client.model.geom.EntityModelSet ms) {
            super(bed, ms);
        }

        @Override
        public void renderByItem(net.minecraft.world.item.ItemStack st, net.minecraft.world.item.ItemDisplayContext ctx, PoseStack ps,
                net.minecraft.client.renderer.MultiBufferSource buf, int luz, int overlay) {
            if (st.getItem() instanceof com.axeliscore.item.CoreItem ci && ci.geo3d() != null) {
                Draw d = new Draw(buf);
                dibujar(ci.geo3d(), ci.textura3d(), ps, d, luz, overlay);
                d.flush();
            }
        }
    }
    //#endif
}
