package com.coleccionista.bloque.deco;

import com.coleccionista.Coleccionista;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Los bloques nuevos del complejo (paredes, pisos, rejas, puertas, catres, lámparas, cámaras...). Todo sale de
 * {@code data/coleccionista/deco.json}, que fabrica {@code herramientas/deco/build.py} junto con los modelos y las texturas; aquí se
 * registran los bloques, sus objetos, sus entidades de bloque (baúl y textos) y la pestaña creativa.
 */
@Mod.EventBusSubscriber(modid = Coleccionista.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Deco {
    private static final String MANIFEST = "/data/coleccionista/deco.json";

    /** Ficha de un bloque tal como la escribe el generador. */
    public record Def(String id, String kind, String sub, boolean tall, int light, boolean collide, String sound, float strength,
                      List<double[]> shape, List<double[]> shapeUpper, List<Particle> particles, JsonObject raw) {
        public boolean flag(String k) {
            return raw.has(k) && raw.get(k).isJsonPrimitive() && (raw.get(k).getAsJsonPrimitive().isBoolean() ? raw.get(k).getAsBoolean()
                    : !raw.get(k).getAsString().isEmpty());
        }

        public String str(String k) {
            return raw.has(k) && raw.get(k).isJsonPrimitive() ? raw.get(k).getAsString() : "";
        }
    }

    public record Particle(String type, double x, double y, double z, double rate) {
    }

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static BlockEntityType<CajonBE> CAJON_BE;
    public static BlockEntityType<TextoBE> TEXTO_BE;
    public static BlockEntityType<RejaBE> REJA_BE;

    private Deco() {
    }

    /** Los barrotes oxidados (las rejas que cierran y abren las pruebas). */
    public static net.minecraft.world.level.block.state.BlockState barrotes() {
        return block("barrotes").defaultBlockState();
    }

    public static Block block(String id) {
        return BLOCKS.get(id);
    }

    static List<Def> defs() {
        List<Def> out = new ArrayList<>();
        try (InputStream in = Deco.class.getResourceAsStream(MANIFEST)) {
            if (in == null) {
                return out;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("blocks")) {
                JsonObject o = el.getAsJsonObject();
                List<Particle> ps = new ArrayList<>();
                if (o.has("particles")) {
                    for (JsonElement pe : o.getAsJsonArray("particles")) {
                        JsonObject po = pe.getAsJsonObject();
                        JsonArray pos = po.getAsJsonArray("pos");
                        ps.add(new Particle(po.get("type").getAsString(), pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble(),
                                po.get("rate").getAsDouble()));
                    }
                }
                out.add(new Def(o.get("id").getAsString(), o.get("kind").getAsString(), o.has("sub") ? o.get("sub").getAsString() : "",
                        o.has("tall") && o.get("tall").getAsBoolean(), o.has("light") ? o.get("light").getAsInt() : 0,
                        !o.has("collide") || o.get("collide").getAsBoolean(), o.has("sound") ? o.get("sound").getAsString() : "stone",
                        o.has("strength") ? o.get("strength").getAsFloat() : 1.5F, boxes(o, "shape"), boxes(o, "shape_upper"), ps, o));
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer " + MANIFEST, e);
        }
        return out;
    }

    private static List<double[]> boxes(JsonObject o, String key) {
        List<double[]> r = new ArrayList<>();
        if (!o.has(key)) {
            return r;
        }
        for (JsonElement e : o.getAsJsonArray(key)) {
            JsonArray b = e.getAsJsonArray();
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) {
                v[i] = b.get(i).getAsDouble();
            }
            r.add(v);
        }
        return r;
    }

    static SoundType sonido(String s) {
        return switch (s) {
            case "metal" -> SoundType.METAL;
            case "wood" -> SoundType.WOOD;
            case "wool", "cloth" -> SoundType.WOOL;
            case "glass" -> SoundType.GLASS;
            case "lantern" -> SoundType.LANTERN;
            case "chain" -> SoundType.CHAIN;
            default -> SoundType.STONE;
        };
    }

    private static Block crear(Def d) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().strength(d.strength(), 6F).sound(sonido(d.sound()));
        return switch (d.kind()) {
            case "cubo" -> new Block(d.light() > 0 ? p.lightLevel(st -> d.light()) : p);
            case "vidrio" -> new GlassBlock(p.noOcclusion().isViewBlocking((s, g, q) -> false).isSuffocating((s, g, q) -> false));
            case "pilar" -> new RotatedPillarBlock(p);
            case "tubo" -> new DecoTubo(p.noOcclusion());
            case "palanca" -> new net.minecraft.world.level.block.LeverBlock(p.noCollission());
            case "losa" -> new SlabBlock(d.flag("transparente") ? p.noOcclusion() : p);
            case "escalera" -> new StairBlock(() -> BLOCKS.get(d.str("base")).defaultBlockState(), p);
            case "barrotes" -> d.flag("alto") ? new DecoBarandal(p.noOcclusion()) : new IronBarsBlock(p.noOcclusion());
            case "puerta" -> new DoorBlock(p.noOcclusion(), BlockSetType.IRON);
            case "reja" -> new DecoReja(p.noOcclusion(), d.flag("ascensor"));
            case "capas" -> new DecoCapas(p.noOcclusion());
            case "cadena" -> new net.minecraft.world.level.block.ChainBlock(p.noOcclusion());
            case "trampilla" -> new net.minecraft.world.level.block.TrapDoorBlock(p.noOcclusion(), BlockSetType.IRON);
            case "alfombra" -> new CarpetBlock(p);
            case "cama" -> new DecoCama(p.noOcclusion());
            default -> DecoBlock.crear(p, d);
        };
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent e) {
        e.register(Registries.BLOCK, h -> {
            for (Def d : defs()) {
                Block b = crear(d);
                BLOCKS.put(d.id(), b);
                h.register(new ResourceLocation(Coleccionista.MODID, d.id()), b);
            }
        });
        e.register(Registries.ITEM, h -> {
            for (Map.Entry<String, Block> en : BLOCKS.entrySet()) {
                Block b = en.getValue();
                Item it;
                if (b instanceof DecoCama) {
                    it = new BedItem(b, new Item.Properties().stacksTo(1));
                } else if (b instanceof DoorBlock || b instanceof DecoBlock db && db.def.tall()) {
                    it = new DoubleHighBlockItem(b, new Item.Properties());
                } else {
                    it = new BlockItem(b, new Item.Properties());
                }
                ITEMS.put(en.getKey(), it);
                h.register(new ResourceLocation(Coleccionista.MODID, en.getKey()), it);
            }
        });
        e.register(Registries.BLOCK_ENTITY_TYPE, h -> {
            List<Block> cajones = new ArrayList<>(), textos = new ArrayList<>();
            for (Block b : BLOCKS.values()) {
                if (b instanceof DecoBlock db && db.def.flag("cajon")) {
                    cajones.add(b);
                }
                if (b instanceof DecoBlock db && db.def.flag("texto")) {
                    textos.add(b);
                }
            }
            CAJON_BE = BlockEntityType.Builder.of(CajonBE::new, cajones.toArray(new Block[0])).build(null);
            TEXTO_BE = BlockEntityType.Builder.of(TextoBE::new, textos.toArray(new Block[0])).build(null);
            h.register(new ResourceLocation(Coleccionista.MODID, "cajon"), CAJON_BE);
            h.register(new ResourceLocation(Coleccionista.MODID, "texto"), TEXTO_BE);
            REJA_BE = BlockEntityType.Builder.of(RejaBE::new, BLOCKS.values().stream().filter(b -> b instanceof DecoReja).toArray(Block[]::new)).build(null);
            h.register(new ResourceLocation(Coleccionista.MODID, "reja"), REJA_BE);
        });
        e.register(Registries.CREATIVE_MODE_TAB, h -> h.register(new ResourceLocation(Coleccionista.MODID, "deco"),
                CreativeModeTab.builder().title(Component.translatable("itemGroup.coleccionista.deco"))
                        .icon(() -> new ItemStack(ITEMS.getOrDefault("barrotes", net.minecraft.world.item.Items.IRON_BARS)))
                        .displayItems((params, out) -> ITEMS.values().forEach(out::accept)).build()));
    }
}
