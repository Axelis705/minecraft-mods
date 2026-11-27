package com.navidad.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.EstudioArchivos;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.navidad.Navidad;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * (Cliente) En temporada navideña los mobs con cabeza de persona (zombis, esqueletos, piglins, aldeanos, saqueadores...) salen con gorro:
 * a la mayoría le toca el gorro de Santa y a algunos el de duende (el mismo modelo que la ropa de los jugadores). Siempre el mismo para
 * cada mob (sale de su uuid); los bebés y los que traen casco no.
 */
public final class GorrosMobs {
    private static final ResourceLocation SANTA_GEO = rl("geo/gorro_santa.geo.json"), SANTA_TEX = rl("textures/prenda/gorro_santa.png");
    private static final ResourceLocation DUENDE_GEO = rl("geo/gorro_duende.geo.json"), DUENDE_TEX = rl("textures/prenda/gorro_duende.png");
    /** Los que ya traen sombrero propio o no tienen sentido con gorro. */
    private static final Set<String> SIN = Set.of("minecraft:witch", "minecraft:armor_stand", "minecraft:giant");
    private static final Map<Geo, Pose> POSES = new IdentityHashMap<>();
    private static final Map<Geo, GeoRenderer.Frame> FRAMES = new IdentityHashMap<>();
    /** De cada 10 mobs, cuántos traen gorro. */
    public static int deCada10 = 8;

    private GorrosMobs() {
    }

    private static ResourceLocation rl(String p) {
        return new ResourceLocation(Navidad.MODID, p);
    }

    /** Le agrega la capa del gorro a cada renderizador de mob con cabeza de persona (de vainilla y de otros mods). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void agregar(EntityRenderersEvent.AddLayers e) {
        int n = 0;
        for (EntityType<?> t : ForgeRegistries.ENTITY_TYPES.getValues()) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(t);
            if (id == null || SIN.contains(id.toString()) || Navidad.MODID.equals(id.getNamespace())) {
                continue;
            }
            EntityRenderer<?> r;
            try {
                r = e.getEntityRenderer((EntityType) t);
            } catch (RuntimeException ex) {
                continue;
            }
            if (r instanceof LivingEntityRenderer lr && lr.getModel() instanceof HeadedModel) {
                lr.addLayer(new Capa(lr));
                n++;
            }
        }
        Navidad.LOGGER.info("NAVIDAD_GORROS capas={}", n);
    }

    /** La capa: el gorro pegado a la cabeza del modelo, tal como la movió Minecraft en ese cuadro. */
    static final class Capa<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        Capa(RenderLayerParent<T, M> p) {
            super(p);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int luz, T e, float limb, float limbAmt, float pt, float age, float yaw,
                float pitch) {
            if (!Montar.temporada() || e.isInvisible() || e.isBaby() || !e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return;
            }
            int h = e.getUUID().hashCode();
            if (Math.floorMod(h, 10) >= deCada10 || !(getParentModel() instanceof HeadedModel hm)) {
                return;
            }
            ModelPart cabeza = hm.getHead();
            if (!cabeza.visible) {
                return;
            }
            boolean duende = Math.floorMod(h >> 5, 4) == 0;
            Geo geo = GeoAssets.geo(duende ? DUENDE_GEO : SANTA_GEO);
            if (geo == null || geo.bones.length == 0) {
                return;
            }
            // la matriz de la cabeza (como ModelPart.translateAndRotate); las cabezas de aldeano y saqueador son 2 más altas
            Quaternionf q = new Quaternionf().rotationZYX(cabeza.zRot, cabeza.yRot, cabeza.xRot);
            Matrix4f m = new Matrix4f(ps.last().pose()).translate(cabeza.x / 16F, cabeza.y / 16F, cabeza.z / 16F).rotate(q)
                    .scale(cabeza.xScale, cabeza.yScale, cabeza.zScale);
            if (getParentModel() instanceof VillagerModel || getParentModel() instanceof IllagerModel) {
                m.translate(0, -2 / 16F, 0);
            }
            Matrix3f n = new Matrix3f(ps.last().normal()).rotate(q);
            Pose pose = POSES.computeIfAbsent(geo, g -> new Pose(g.bones.length));
            GeoRenderer.Frame f = FRAMES.computeIfAbsent(geo, GeoRenderer.Frame::new);
            pose.reset();
            Matrix4f[] base = new Matrix4f[geo.roots.length];
            Matrix3f[] baseN = new Matrix3f[geo.roots.length];
            for (int i = 0; i < base.length; i++) {
                base[i] = m;
                baseN[i] = n;
            }
            GeoRenderer.computeRaices(geo, pose, base, baseN, f);
            Draw d = new Draw(buf);
            GeoRenderer.draw(geo, pose, f, d.cutout(EstudioArchivos.textura(duende ? DUENDE_TEX : SANTA_TEX)), GeoRenderer.Pass.NORMAL, luz,
                    LivingEntityRenderer.getOverlayCoords(e, 0F), 0xFFFFFFFF);
            d.flush();
        }
    }
}
