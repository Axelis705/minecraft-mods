package com.mundosdeaxelis.world.decor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Decoración nueva para las estructuras (candelabros, arañas de luces, tapices, globo terráqueo, telescopio...).
 * Todo sale de {@code data/mundos_de_axelis/est_decor.json}, que fabrica {@code estructuras/herramientas/decor} junto con los modelos y las
 * texturas; aquí solo se registran los bloques, sus objetos y una pestaña creativa. Se registra sola (no hace falta tocar la clase principal).
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EstDecor {
    public static final String NS = "mundos_de_axelis";
    private static final String MANIFEST = "/data/mundos_de_axelis/est_decor.json";

    /** Ficha de una decoración tal como la escribe el generador. */
    public record Def(String id, String kind, boolean tall, int light, boolean collide, String sound, List<double[]> shape, List<double[]> shapeUpper,
                      List<Particle> particles) {
    }

    public record Particle(String type, double x, double y, double z, double rate) {
    }

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    private EstDecor() {
    }

    public static Block block(String id) {
        return BLOCKS.get(id);
    }

    static List<Def> defs() {
        List<Def> out = new ArrayList<>();
        try (InputStream in = EstDecor.class.getResourceAsStream(MANIFEST)) {
            if (in == null) {
                return out;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("blocks")) {
                JsonObject o = el.getAsJsonObject();
                List<Particle> ps = new ArrayList<>();
                for (JsonElement pe : o.getAsJsonArray("particles")) {
                    JsonObject po = pe.getAsJsonObject();
                    JsonArray pos = po.getAsJsonArray("pos");
                    ps.add(new Particle(po.get("type").getAsString(), pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble(),
                            po.get("rate").getAsDouble()));
                }
                out.add(new Def(o.get("id").getAsString(), o.get("kind").getAsString(), o.get("tall").getAsBoolean(), o.get("light").getAsInt(),
                        o.get("collide").getAsBoolean(), o.has("sound") ? o.get("sound").getAsString() : "wood", boxes(o.getAsJsonArray("shape")),
                        o.has("shape_upper") ? boxes(o.getAsJsonArray("shape_upper")) : List.of(), ps));
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer " + MANIFEST, e);
        }
        return out;
    }

    private static List<double[]> boxes(JsonArray a) {
        List<double[]> r = new ArrayList<>();
        for (JsonElement e : a) {
            JsonArray b = e.getAsJsonArray();
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) {
                v[i] = b.get(i).getAsDouble();
            }
            r.add(v);
        }
        return r;
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent e) {
        e.register(Registries.BLOCK, h -> {
            for (Def d : defs()) {
                Block b = EstBlock.create(d);
                BLOCKS.put(d.id(), b);
                h.register(new ResourceLocation(NS, d.id()), b);
            }
        });
        e.register(Registries.ITEM, h -> {
            for (Map.Entry<String, Block> en : BLOCKS.entrySet()) {
                Block b = en.getValue();
                Item it = b instanceof EstBlock eb && eb.def.tall() ? new DoubleHighBlockItem(b, new Item.Properties()) : new BlockItem(b, new Item.Properties());
                ITEMS.put(en.getKey(), it);
                h.register(new ResourceLocation(NS, en.getKey()), it);
            }
        });
        e.register(Registries.CREATIVE_MODE_TAB, h -> h.register(new ResourceLocation(NS, "estructuras_decor"),
                CreativeModeTab.builder().title(Component.translatable("itemGroup.mundos_estructuras"))
                        .icon(() -> new ItemStack(ITEMS.values().stream().findFirst().orElse(net.minecraft.world.item.Items.STONE)))
                        .displayItems((params, out) -> ITEMS.values().forEach(out::accept)).build()));
    }
}
