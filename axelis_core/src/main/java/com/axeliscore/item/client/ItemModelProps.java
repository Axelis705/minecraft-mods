package com.axeliscore.item.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Ids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * (Cliente) Condiciones propias para los modelos de los objetos, iguales en todas las versiones. Por ejemplo, "la caña está lanzada"
 * para dibujar la caña sin el hilo que cuelga mientras el corcho está en el agua (como la caña de Minecraft).
 * <p>
 * Uso (en el constructor del mod, antes de que se carguen los modelos):
 * <pre>
 * Net.onClient(() -&gt; ItemModelProps.bool(MODID, "lanzada", (stack, quien) -&gt; ...));
 * </pre>
 * Y en los recursos:
 * <ul>
 * <li>1.20.1 y 1.21.1, en el modelo del objeto: {@code "overrides": [{"predicate": {"mod:lanzada": 1}, "model": "mod:item/cana_lanzada"}]}</li>
 * <li>1.21.4 en adelante, en {@code assets/mod/items/cana.json}: {@code {"model": {"type": "minecraft:condition", "property":
 * "mod:lanzada", "on_true": {...}, "on_false": {...}}}}</li>
 * </ul>
 */
public final class ItemModelProps {
    private ItemModelProps() {
    }

    /** La condición: el objeto y quién lo lleva (puede ser null, por ejemplo en un marco o en el inventario sin dueño). */
    @FunctionalInterface
    public interface Test {
        boolean test(ItemStack stack, LivingEntity holder);
    }

    /** Registra la condición {@code modid:name} (verdadero = 1 en las versiones viejas). */
    public static void bool(String modid, String name, Test test) {
        ResourceLocation id = Ids.rl(modid, name);
        //#if MC >= 1.21.4
        //$$ Cond.register(id, test);
        //#else
        net.minecraft.client.renderer.item.ItemProperties.registerGeneric(id, (stack, level, entity, seed) -> test.test(stack, entity) ? 1F : 0F);
        //#endif
        AxelisCore.LOGGER.debug("Axelis Core: condición de modelo {}", id);
    }

    //#if MC >= 1.21.4
    //$$ /** La condición dentro del sistema de modelos de objetos de 1.21.4+ (se agrega a la lista de Minecraft por su nombre). */
    //$$ private static final class Cond implements net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty {
    //$$     private final Test test;
    //$$     private final com.mojang.serialization.MapCodec<Cond> codec;
    //$$
    //$$     private Cond(Test test) {
    //$$         this.test = test;
    //$$         this.codec = com.mojang.serialization.MapCodec.unit(this);
    //$$     }
    //$$
    //$$     @Override
    //$$     public boolean get(ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, LivingEntity entity, int seed,
    //$$             net.minecraft.world.item.ItemDisplayContext ctx) {
    //$$         return test.test(stack, entity);
    //$$     }
    //$$
    //$$     @Override
    //$$     public com.mojang.serialization.MapCodec<Cond> type() {
    //$$         return codec;
    //$$     }
    //$$
    //$$     @SuppressWarnings("unchecked")
    //$$     static void register(ResourceLocation id, Test test) {
    //$$         try {
    //$$             java.lang.reflect.Field f = net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.class
    //$$                     .getDeclaredField("ID_MAPPER");
    //$$             f.setAccessible(true);
    //$$             var mapper = (net.minecraft.util.ExtraCodecs.LateBoundIdMapper<ResourceLocation, com.mojang.serialization.MapCodec<
    //$$                     ? extends net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty>>) f.get(null);
    //$$             mapper.put(id, new Cond(test).codec);
    //$$         } catch (ReflectiveOperationException | RuntimeException e) {
    //$$             AxelisCore.LOGGER.error("Axelis Core: no se pudo registrar la condición de modelo {}", id, e);
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
