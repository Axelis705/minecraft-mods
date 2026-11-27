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
import net.minecraft.client.model.ZombieVillagerModel;
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
 * (Cliente) En temporada navideña los mobs salen adornados (siempre el mismo adorno para cada uno: sale de su uuid; los bebés y los que
 * traen casco no):
 * <ul>
 * <li>los de cabeza de persona (zombis, esqueletos, piglins, aldeanos, saqueadores...): gorro de Santa o de duende;</li>
 * <li>los animales y otras criaturas de Minecraft (vacas, cerdos, borregos, gallinas, lobos, gatos, zorros, pandas, osos polares,
 * gólems de hierro y de nieve, creepers y arañas): gorrito a su medida, puesto sobre su cabeza;</li>
 * <li>los caballos, burros y mulas: disfrazados de reno, con su diadema de cuernos.</li>
 * </ul>
 */
public final class GorrosMobs {
    private static final ResourceLocation SANTA_GEO = rl("geo/gorro_santa.geo.json"), SANTA_TEX = rl("textures/prenda/gorro_santa.png");
    private static final ResourceLocation DUENDE_GEO = rl("geo/gorro_duende.geo.json"), DUENDE_TEX = rl("textures/prenda/gorro_duende.png");
    private static final ResourceLocation CUERNOS_GEO = rl("geo/cuernos_animal.geo.json"), CUERNOS_TEX = rl("textures/prenda/cuernos_animal.png");

    /** Dónde va el adorno en la cabeza de cada criatura (en píxeles del modelo: centro de arriba de la cabeza) y de qué tamaño. */
    record Lugar(float cx, float arriba, float cz, float escala, boolean cuernos) {
    }

    private static final Map<String, Lugar> CRIATURAS = new java.util.HashMap<>();

    static {
        lugar(0, -4, -3, 1F, false, "cow", "mooshroom");
        lugar(0, -4, -4, 1F, false, "pig");
        lugar(0, -4, -2, 0.75F, false, "sheep");
        lugar(0, -6, -0.5F, 0.5F, false, "chicken");
        lugar(1, -3, 0, 0.75F, false, "wolf");
        lugar(1, -3.5F, -2, 0.75F, false, "fox");
        lugar(0, -2, -0.5F, 0.6F, false, "cat", "ocelot");
        lugar(0, -5, 0.5F, 1.5F, false, "panda");
        lugar(0, -3, 0.5F, 0.85F, false, "polar_bear");
        lugar(0, -12, -1.5F, 1F, false, "iron_golem");
        lugar(0, -9.5F, 0, 1.2F, false, "snow_golem");
        lugar(0, -8, 0, 1F, false, "creeper");
        lugar(0, -4, -4, 1F, false, "spider", "cave_spider");
        lugar(0, -11, 1.5F, 0.75F, true, "horse", "donkey", "mule", "skeleton_horse", "zombie_horse");
    }

    private static void lugar(float cx, float arriba, float cz, float escala, boolean cuernos, String... ids) {
        for (String id : ids) {
            CRIATURAS.put("minecraft:" + id, new Lugar(cx, arriba, cz, escala, cuernos));
        }
    }

    /** headParts() de los modelos por partes (vacas, lobos, caballos...): se busca una vez por clase, sin depender de nombres. */
    private static final Map<Class<?>, java.lang.reflect.Method> CABEZAS = new java.util.HashMap<>();
    private static boolean sinReflexion;
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
            if (r instanceof LivingEntityRenderer lr && (lr.getModel() instanceof HeadedModel || CRIATURAS.containsKey(id.toString()))) {
                lr.addLayer(new Capa(lr, CRIATURAS.get(id.toString())));
                n++;
            }
        }
        Navidad.LOGGER.info("NAVIDAD_GORROS capas={}", n);
    }

    /** La parte de la cabeza de un modelo: la de los de cabeza de persona, la "head" de los modelos con raíz y, en los modelos por
     *  partes (vacas, lobos, caballos...), la primera de sus partes de cabeza. */
    static ModelPart cabeza(EntityModel<?> m) {
        if (m instanceof HeadedModel hm) {
            return hm.getHead();
        }
        if (m instanceof net.minecraft.client.model.HierarchicalModel<?> hm) {
            ModelPart r = hm.root();
            return r.hasChild("head") ? r.getChild("head") : null;
        }
        if (m instanceof net.minecraft.client.model.AgeableListModel<?> am && !sinReflexion) {
            try {
                java.lang.reflect.Method mt = CABEZAS.get(m.getClass());
                if (mt == null) {
                    mt = buscarCabezas(am);
                    CABEZAS.put(m.getClass(), mt);
                }
                for (Object o : (Iterable<?>) mt.invoke(am)) {
                    return (ModelPart) o;
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                sinReflexion = true;
                Navidad.LOGGER.warn("NAVIDAD_GORROS no pude leer la cabeza de {}: {}", m.getClass().getSimpleName(), ex.toString());
            }
        }
        return null;
    }

    /** De los dos métodos de partes (cabeza y cuerpo, sin argumentos), el de la cabeza es el que da menos partes. */
    private static java.lang.reflect.Method buscarCabezas(Object modelo) throws ReflectiveOperationException {
        java.lang.reflect.Method mejor = null;
        int menos = Integer.MAX_VALUE;
        for (java.lang.reflect.Method mt : net.minecraft.client.model.AgeableListModel.class.getDeclaredMethods()) {
            if (mt.getParameterCount() != 0 || !Iterable.class.isAssignableFrom(mt.getReturnType())) {
                continue;
            }
            mt.setAccessible(true);
            int n = 0;
            for (Object ignored : (Iterable<?>) mt.invoke(modelo)) {
                n++;
            }
            if (n > 0 && n < menos) {
                menos = n;
                mejor = mt;
            }
        }
        if (mejor == null) {
            throw new NoSuchMethodException("headParts");
        }
        return mejor;
    }

    /** La capa: el gorro pegado a la cabeza del modelo, tal como la movió Minecraft en ese cuadro. */
    static final class Capa<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private final Lugar lugar;

        Capa(RenderLayerParent<T, M> p, Lugar lugar) {
            super(p);
            this.lugar = lugar;
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int luz, T e, float limb, float limbAmt, float pt, float age, float yaw,
                float pitch) {
            if (!Montar.temporada() || e.isInvisible() || e.isBaby() || !e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return;
            }
            int h = e.getUUID().hashCode();
            if (Math.floorMod(h, 10) >= (lugar == null ? deCada10 : deCada10 - 2)) {
                return;
            }
            ModelPart cabeza = cabeza(getParentModel());
            if (cabeza == null || !cabeza.visible) {
                return;
            }
            boolean duende = Math.floorMod(h >> 5, 4) == 0;
            boolean cuernos = lugar != null && lugar.cuernos();
            ResourceLocation geoId = cuernos ? CUERNOS_GEO : duende ? DUENDE_GEO : SANTA_GEO;
            ResourceLocation texId = cuernos ? CUERNOS_TEX : duende ? DUENDE_TEX : SANTA_TEX;
            Geo geo = GeoAssets.geo(geoId);
            if (geo == null || geo.bones.length == 0) {
                return;
            }
            // la matriz de la cabeza (como ModelPart.translateAndRotate); las cabezas de aldeano, zombi aldeano y saqueador son 2 más altas
            Quaternionf q = new Quaternionf().rotationZYX(cabeza.zRot, cabeza.yRot, cabeza.xRot);
            Matrix4f m = new Matrix4f(ps.last().pose()).translate(cabeza.x / 16F, cabeza.y / 16F, cabeza.z / 16F).rotate(q)
                    .scale(cabeza.xScale, cabeza.yScale, cabeza.zScale);
            if (lugar != null) {
                // el punto de arriba de la cabeza de un jugador (0, -8, 0) va al centro de arriba de la cabeza de esta criatura
                m.translate(lugar.cx() / 16F, lugar.arriba() / 16F, lugar.cz() / 16F).scale(lugar.escala()).translate(0, 8 / 16F, 0);
            } else if (getParentModel() instanceof VillagerModel || getParentModel() instanceof IllagerModel
                    || getParentModel() instanceof ZombieVillagerModel) {
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
            GeoRenderer.draw(geo, pose, f, d.cutout(EstudioArchivos.textura(texId)), GeoRenderer.Pass.NORMAL, luz,
                    LivingEntityRenderer.getOverlayCoords(e, 0F), 0xFFFFFFFF);
            d.flush();
        }
    }
}
