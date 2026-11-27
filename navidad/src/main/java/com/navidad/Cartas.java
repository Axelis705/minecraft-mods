package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * La carta a Santa: escribes en un libro y pluma lo que quieres y lo metes en el buzón de Santa (en temporada, una carta al día). La carta
 * sale volando al Polo Norte y a la mañana siguiente (o a la media hora, si el tiempo está detenido) llegan cajas de regalo "De: Santa"
 * con lo que pediste (hasta 3 cosas) y su respuesta escrita. Si mientras tanto le hiciste daño a aldeanos, gólems, gatos o renos...
 * te toca carbón.
 */
public final class Cartas {
    /** Si el tiempo del mundo no avanza, la respuesta llega a esta hora de verdad (milisegundos). */
    public static final long ESPERA_MAX = 30L * 60L * 1000L;

    private Cartas() {
    }

    public static void init() {
        AxelisCore.onServerTick(s -> {
            if (s.getTickCount() % 100 == 37) {
                for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                    entregar(p, false);
                }
            }
        });
        AxelisCore.onLogin(p -> entregar(p, false));
    }

    /** El texto de un libro y pluma (o de un libro firmado). */
    public static String leer(ItemStack libro) {
        CompoundTag t = libro.getTag();
        if (t == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (t.contains("title", Tag.TAG_STRING)) {
            sb.append(t.getString("title")).append('\n');
        }
        ListTag pags = t.getList("pages", Tag.TAG_STRING);
        boolean firmado = libro.is(Items.WRITTEN_BOOK);
        for (int i = 0; i < pags.size(); i++) {
            String p = pags.getString(i);
            if (firmado) {
                try {
                    Component c = Component.Serializer.fromJson(p);
                    p = c == null ? p : c.getString();
                } catch (RuntimeException ex) {
                    // se queda el texto crudo
                }
            }
            sb.append(p).append('\n');
        }
        return sb.toString();
    }

    /** Cuántas veces ha lastimado a los que no se debe (aldeanos, gólems, gatos, renos...). */
    static int malos(ServerPlayer p) {
        int n = 0;
        for (EntityType<?> t : new EntityType<?>[]{EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM,
                EntityType.CAT, ModRegistry.RENO.get()}) {
            n += p.getStats().getValue(Stats.ENTITY_KILLED.get(t));
        }
        return n;
    }

    /** Mete la carta en el buzón. Devuelve true si salió volando (y se gasta el libro). */
    public static boolean mandar(ServerPlayer p, ItemStack libro, BlockPos buzon) {
        MinecraftServer s = p.server;
        if (!Temporada.activa(s)) {
            p.displayClientMessage(Component.translatable("navidad.carta.vacaciones").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        if (yo.contains("carta")) {
            p.displayClientMessage(Component.translatable("navidad.carta.pendiente").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        long hoy = Temporada.hoy().toEpochDay();
        if (yo.getLong("cartaDia") == hoy) {
            p.displayClientMessage(Component.translatable("navidad.carta.hoy").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        String texto = leer(libro);
        if (texto.isBlank()) {
            p.displayClientMessage(Component.translatable("navidad.carta.vacia").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        List<Deseos.Deseo> ds = Deseos.buscar(texto, p.getRandom());
        CompoundTag c = new CompoundTag();
        c.putLong("dia", p.serverLevel().getDayTime() / 24000L);
        c.putLong("cuando", System.currentTimeMillis());
        c.putInt("malos", malos(p));
        ListTag lista = new ListTag();
        List<String> claves = new ArrayList<>();
        for (Deseos.Deseo d : ds) {
            CompoundTag dt = new CompoundTag();
            dt.putString("clave", d.clave());
            dt.putString("tipo", d.tipo().name());
            ListTag cosas = new ListTag();
            for (ItemStack st : d.cosas()) {
                cosas.add(st.save(new CompoundTag()));
            }
            dt.put("cosas", cosas);
            lista.add(dt);
            claves.add(d.clave());
        }
        c.put("deseos", lista);
        yo.put("carta", c);
        yo.putLong("cartaDia", hoy);
        w.dirty();
        libro.shrink(1);
        ServerLevel l = p.serverLevel();
        l.playSound(null, buzon, ModRegistry.CARTA_VUELA.get(), SoundSource.BLOCKS, 1F, 1F);
        l.sendParticles(ParticleTypes.END_ROD, buzon.getX() + 0.5, buzon.getY() + 1.1, buzon.getZ() + 0.5, 14, 0.2, 0.4, 0.2, 0.06);
        l.sendParticles(ParticleTypes.SNOWFLAKE, buzon.getX() + 0.5, buzon.getY() + 1.3, buzon.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.04);
        p.sendSystemMessage(Component.translatable("navidad.carta.enviada").withStyle(ChatFormatting.AQUA));
        Navidad.LOGGER.info("NAVIDAD_CARTA enviada de={} deseos={}", p.getGameProfile().getName(), claves);
        return true;
    }

    /** Si ya es la mañana siguiente (o pasó media hora), llega la respuesta de Santa. {@code ya}: sin esperar (comando de prueba). */
    public static boolean entregar(ServerPlayer p, boolean ya) {
        WorldStore w = WorldStore.get(p.server, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        if (!yo.contains("carta", Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag c = yo.getCompound("carta");
        if (!ya) {
            long t = p.server.overworld().getDayTime();
            boolean manana = t / 24000L > c.getLong("dia") && t % 24000L < 12000L;
            if (!manana && System.currentTimeMillis() - c.getLong("cuando") < ESPERA_MAX) {
                return false;
            }
        }
        yo.remove("carta");
        w.dirty();
        boolean malo = malos(p) > c.getInt("malos");
        List<ItemStack> regalos = new ArrayList<>();
        List<String> tipos = new ArrayList<>();
        if (malo) {
            regalos.add(new ItemStack(Items.COAL, 3));
        } else {
            ListTag ds = c.getList("deseos", Tag.TAG_COMPOUND);
            for (int i = 0; i < ds.size(); i++) {
                CompoundTag d = ds.getCompound(i);
                tipos.add(d.getString("tipo"));
                ListTag cosas = d.getList("cosas", Tag.TAG_COMPOUND);
                for (int k = 0; k < cosas.size(); k++) {
                    ItemStack st = ItemStack.of(cosas.getCompound(k));
                    if (!st.isEmpty()) {
                        regalos.add(st);
                    }
                }
            }
            if (regalos.isEmpty()) {
                regalos.add(new ItemStack(ModRegistry.REGALO_MISTERIOSO.get()));
            }
        }
        for (ItemStack st : regalos) {
            Fiestas.darCaja(p, st, "Santa");
        }
        ItemStack carta = respuesta(p.getGameProfile().getName(), regalos, malo, tipos, c.getList("deseos", Tag.TAG_COMPOUND).isEmpty());
        if (!p.getInventory().add(carta)) {
            p.drop(carta, false);
        }
        p.playNotifySound(ModRegistry.RISA_SANTA.get(), SoundSource.PLAYERS, 1F, 1F);
        p.playNotifySound(ModRegistry.CASCABELES.get(), SoundSource.PLAYERS, 0.8F, 1F);
        p.sendSystemMessage(Component.translatable(malo ? "navidad.carta.llego_malo" : "navidad.carta.llego")
                .withStyle(malo ? ChatFormatting.DARK_GRAY : ChatFormatting.RED, ChatFormatting.BOLD));
        Navidad.LOGGER.info("NAVIDAD_CARTA entregada a={} regalos={} malo={}", p.getGameProfile().getName(), regalos.size(), malo);
        return true;
    }

    /** La carta de respuesta: un libro firmado por Santa (con textos traducibles, para que se lea en el idioma de cada quien). */
    static ItemStack respuesta(String nombre, List<ItemStack> regalos, boolean malo, List<String> tipos, boolean nada) {
        ItemStack b = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag t = b.getOrCreateTag();
        t.putString("title", "Santa");
        t.putString("author", "Santa");
        t.putBoolean("resolved", true);
        ListTag pags = new ListTag();
        MutableComponent p1 = Component.translatable("navidad.carta.resp.hola", nombre).append("\n\n");
        if (malo) {
            p1.append(Component.translatable("navidad.carta.resp.malo"));
        } else if (nada) {
            p1.append(Component.translatable("navidad.carta.resp.nada"));
        } else {
            p1.append(Component.translatable("navidad.carta.resp.leida"));
            if (tipos.contains("DIFICIL")) {
                p1.append("\n\n").append(Component.translatable("navidad.carta.resp.dificil"));
            }
            if (tipos.contains("BONITO")) {
                p1.append("\n\n").append(Component.translatable("navidad.carta.resp.bonito"));
            }
        }
        pags.add(StringTag.valueOf(Component.Serializer.toJson(p1)));
        MutableComponent p2 = Component.translatable(malo ? "navidad.carta.resp.carbon" : "navidad.carta.resp.trae").append("\n");
        for (ItemStack st : regalos) {
            p2.append("\n- ").append(st.getHoverName()).append(st.getCount() > 1 ? " x" + st.getCount() : "");
        }
        p2.append("\n\n").append(Component.translatable("navidad.carta.resp.firma"));
        pags.add(StringTag.valueOf(Component.Serializer.toJson(p2)));
        t.put("pages", pags);
        return b;
    }
}
