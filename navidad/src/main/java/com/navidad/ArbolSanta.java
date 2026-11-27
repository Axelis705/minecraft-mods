package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.deco.Deco;
import com.navidad.entidad.DuendeLadron;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tu árbol de Navidad. El último árbol que pones (de Navidad, nevado, dorado o pino en maceta) queda como <b>tu árbol</b>, y cada mañana
 * de la temporada Santa te deja regalos debajo (cajas de regalo "De: Santa, Para: ti"). Entre más adornos navideños tenga alrededor
 * (luces, esferas, guirnaldas, regalos de adorno, coronas... a 5 bloques), más y mejores regalos; el 25 de diciembre, un regalo extra y
 * uno épico. Si mientras tanto lastimaste aldeanos, gólems, gatos o renos, solo carbón. Si ya hay 4 regalos sin abrir bajo el árbol,
 * Santa espera a que los abras. Con clic derecho (mano vacía) en tu árbol ves cuántos adornos tiene. Algunas noches, si hay regalos sin
 * abrir, llega el {@link DuendeLadron} a robarse uno.
 */
public final class ArbolSanta {
    private static final String[] ARBOLES = {"arbol_navidad", "arbol_nevado", "arbol_dorado", "pino_maceta"};
    /** Cuántas veces Santa dejó regalos (para la prueba con fotos). */
    public static int entregas;

    private ArbolSanta() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ArbolSanta::alPoner);
        MinecraftForge.EVENT_BUS.addListener(ArbolSanta::alRomper);
        MinecraftForge.EVENT_BUS.addListener(ArbolSanta::alTocar);
        AxelisCore.onServerTick(s -> {
            if (s.getTickCount() % 40 == 13 && Fiestas.automatico) {
                manana(s, false);
            }
            if (s.getTickCount() % 200 == 77 && Fiestas.automatico) {
                ladron(s);
            }
        });
    }

    public static boolean esArbol(Block b) {
        for (String id : ARBOLES) {
            if (Deco.block(id) == b) {
                return true;
            }
        }
        return false;
    }

    private static ListTag lista(MinecraftServer s) {
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        if (!w.global().contains("arboles")) {
            w.global().put("arboles", new ListTag());
        }
        return w.global().getList("arboles", Tag.TAG_COMPOUND);
    }

    private static BlockPos pos(CompoundTag c) {
        return new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
    }

    private static ServerLevel nivel(MinecraftServer s, CompoundTag c) {
        return s.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("mundo"))));
    }

    // ------------------------------------------------------------------ poner, quitar y revisar tu árbol

    static void alPoner(BlockEvent.EntityPlaceEvent e) {
        if (!esArbol(e.getPlacedBlock().getBlock()) || !(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof Level l)) {
            return;
        }
        ListTag ls = lista(p.server);
        for (int i = ls.size() - 1; i >= 0; i--) {
            if (ls.getCompound(i).getUUID("quien").equals(p.getUUID())) {
                ls.remove(i);
            }
        }
        CompoundTag c = new CompoundTag();
        c.putUUID("quien", p.getUUID());
        c.putString("nombre", p.getGameProfile().getName());
        c.putString("mundo", l.dimension().location().toString());
        c.putInt("x", e.getPos().getX());
        c.putInt("y", e.getPos().getY());
        c.putInt("z", e.getPos().getZ());
        // Santa pasa la próxima mañana (si lo pones de madrugada, esta misma)
        long hora = l.getDayTime() % 24000L, dia = l.getDayTime() / 24000L;
        c.putLong("visto", hora < 4000 ? dia - 1 : dia);
        c.putInt("malos", Cartas.malos(p));
        ls.add(c);
        WorldStore.get(p.server, Navidad.MODID).dirty();
        p.sendSystemMessage(Component.translatable("navidad.arbol.tuyo").withStyle(ChatFormatting.GOLD));
        Navidad.LOGGER.info("NAVIDAD_ARBOL {} puso su árbol en {}", p.getGameProfile().getName(), e.getPos().toShortString());
    }

    static void alRomper(BlockEvent.BreakEvent e) {
        if (!esArbol(e.getState().getBlock()) || !(e.getLevel() instanceof Level l) || l.isClientSide()) {
            return;
        }
        MinecraftServer s = l.getServer();
        ListTag ls = lista(s);
        for (int i = ls.size() - 1; i >= 0; i--) {
            CompoundTag c = ls.getCompound(i);
            BlockPos p = pos(c);
            if (c.getString("mundo").equals(l.dimension().location().toString()) && Math.abs(p.getX() - e.getPos().getX()) <= 0
                    && Math.abs(p.getZ() - e.getPos().getZ()) <= 0 && Math.abs(p.getY() - e.getPos().getY()) <= 1) {
                ls.remove(i);
                WorldStore.get(s, Navidad.MODID).dirty();
            }
        }
    }

    static void alTocar(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !e.getEntity().getMainHandItem().isEmpty() || e.getEntity().isSecondaryUseActive()
                || !(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        BlockState st = e.getLevel().getBlockState(e.getPos());
        if (!esArbol(st.getBlock())) {
            return;
        }
        int n = adornos(p.serverLevel(), e.getPos());
        int nivel = nivel(n);
        String estrellas = "★".repeat(nivel + 1) + "☆".repeat(3 - nivel);
        p.displayClientMessage(Component.translatable("navidad.arbol.adornos", n, estrellas).withStyle(ChatFormatting.GOLD), true);
        e.setCanceled(true);
    }

    /** Cuántos adornos navideños hay alrededor del árbol (a 5 bloques a los lados, de 1 abajo a 5 arriba). */
    public static int adornos(ServerLevel l, BlockPos arbol) {
        int n = 0;
        Block caja = ModRegistry.CAJA_REGALO.get();
        for (BlockPos q : BlockPos.betweenClosed(arbol.offset(-5, -1, -5), arbol.offset(5, 5, 5))) {
            BlockState b = l.getBlockState(q);
            if (b.isAir() || b.is(caja) || esArbol(b.getBlock())) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b.getBlock());
            if (id != null && Navidad.MODID.equals(id.getNamespace())) {
                n++;
            }
        }
        return n;
    }

    /** 0 = sencillo, 1 = adornado, 2 = muy adornado, 3 = espectacular. */
    public static int nivel(int adornos) {
        return adornos >= 15 ? 3 : adornos >= 8 ? 2 : adornos >= 3 ? 1 : 0;
    }

    // ------------------------------------------------------------------ cada mañana: los regalos de Santa

    /** Revisa los árboles y deja los regalos de hoy. Con {@code ya}, a todos de una vez (comando de prueba). Devuelve cuántos árboles. */
    public static int manana(MinecraftServer s, boolean ya) {
        if (!Temporada.activa(s) && !ya) {
            return 0;
        }
        ListTag ls = lista(s);
        int hechos = 0;
        for (int i = 0; i < ls.size(); i++) {
            CompoundTag c = ls.getCompound(i);
            ServerLevel l = nivel(s, c);
            if (l == null) {
                continue;
            }
            BlockPos arbol = pos(c);
            long dia = l.getDayTime() / 24000L, hora = l.getDayTime() % 24000L;
            boolean toca = ya || hora < 4000 && c.getLong("visto") < dia;
            if (!toca || !l.isLoaded(arbol)) {
                continue;
            }
            if (!esArbol(l.getBlockState(arbol).getBlock())) {
                continue;
            }
            c.putLong("visto", dia);
            entregar(s, l, arbol, c);
            hechos++;
        }
        if (hechos > 0) {
            WorldStore.get(s, Navidad.MODID).dirty();
        }
        return hechos;
    }

    private static void entregar(MinecraftServer s, ServerLevel l, BlockPos arbol, CompoundTag c) {
        RandomSource r = l.random;
        List<BlockPos> sinAbrir = cajasCerca(l, arbol);
        String nombre = c.getString("nombre");
        UUID quien = c.getUUID("quien");
        ServerPlayer p = s.getPlayerList().getPlayer(quien);
        if (sinAbrir.size() >= 4) {
            if (p != null) {
                p.sendSystemMessage(Component.translatable("navidad.arbol.lleno").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        // ¿se portó mal desde la última visita?
        boolean malo = false;
        if (p != null) {
            int m = Cartas.malos(p);
            malo = m > c.getInt("malos");
            c.putInt("malos", m);
        }
        int n = adornos(l, arbol), nv = nivel(n);
        boolean navidad = Temporada.diaDiciembre() == 25;
        List<ItemStack> regalos = new ArrayList<>();
        if (malo) {
            regalos.add(new ItemStack(Items.COAL, 2 + r.nextInt(3)));
        } else {
            int cuantos = (nv == 0 ? 1 : nv == 1 ? 1 + r.nextInt(2) : nv == 2 ? 2 : 3) + (navidad ? 1 : 0);
            float raro = new float[]{0.08F, 0.2F, 0.35F, 0.45F}[nv], epico = new float[]{0F, 0.01F, 0.03F, 0.06F}[nv];
            for (int k = 0; k < cuantos; k++) {
                float x = r.nextFloat();
                Premios.Rareza rz = navidad && k == 0 ? Premios.Rareza.EPICO : x < epico ? Premios.Rareza.EPICO : x < epico + raro
                        ? Premios.Rareza.RARO : Premios.Rareza.COMUN;
                regalos.add(Premios.sacar(rz, r));
            }
        }
        // los lugares alrededor del árbol (primero los más cercanos)
        List<BlockPos> lugares = new ArrayList<>();
        for (int rad = 1; rad <= 3; rad++) {
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) {
                        continue;
                    }
                    for (int dy = -1; dy <= 1; dy++) {
                        lugares.add(arbol.offset(dx, dy, dz));
                    }
                }
            }
        }
        int puestos = 0;
        for (ItemStack st : regalos) {
            for (BlockPos q : lugares) {
                Direction mira = Direction.Plane.HORIZONTAL.getRandomDirection(r);
                if (CajaRegalo.poner(l, q, mira, st, "Santa", nombre)) {
                    l.sendParticles(ParticleTypes.SNOWFLAKE, q.getX() + 0.5, q.getY() + 0.6, q.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                    l.sendParticles(ParticleTypes.END_ROD, q.getX() + 0.5, q.getY() + 0.8, q.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
                    puestos++;
                    break;
                }
            }
        }
        l.playSound(null, arbol, ModRegistry.CASCABELES.get(), SoundSource.BLOCKS, 1F, 1F);
        entregas++;
        if (p != null) {
            p.sendSystemMessage(Component.translatable(malo ? "navidad.arbol.carbon" : "navidad.arbol.regalos", puestos)
                    .withStyle(malo ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD, ChatFormatting.BOLD));
            if (!malo && puestos > 0) {
                Logros.dar(p, "arbol");
            }
        }
        Navidad.LOGGER.info("NAVIDAD_ARBOL regalos para {}: {} (adornos={}, nivel={}, malo={})", nombre, puestos, n, nv, malo);
    }

    /** Las cajas de regalo llenas alrededor de un árbol. */
    public static List<BlockPos> cajasCerca(ServerLevel l, BlockPos arbol) {
        List<BlockPos> out = new ArrayList<>();
        Block caja = ModRegistry.CAJA_REGALO.get();
        for (BlockPos q : BlockPos.betweenClosed(arbol.offset(-4, -2, -4), arbol.offset(4, 2, 4))) {
            BlockState b = l.getBlockState(q);
            if (b.is(caja) && b.getValue(CajaRegalo.LLENA)) {
                out.add(q.immutable());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ el duende ladrón

    /** Algunas noches, si hay regalos sin abrir bajo tu árbol y estás cerca (para que lo veas), llega el duende ladrón. */
    static void ladron(MinecraftServer s) {
        if (!Temporada.activa(s)) {
            return;
        }
        ListTag ls = lista(s);
        for (int i = 0; i < ls.size(); i++) {
            CompoundTag c = ls.getCompound(i);
            ServerLevel l = nivel(s, c);
            BlockPos arbol = pos(c);
            if (l == null || !l.isLoaded(arbol)) {
                continue;
            }
            long dia = l.getDayTime() / 24000L, hora = l.getDayTime() % 24000L;
            if (hora < 13500 || hora > 22000 || c.getLong("ladronDia") == dia) {
                continue;
            }
            ServerPlayer p = s.getPlayerList().getPlayer(c.getUUID("quien"));
            if (p == null || p.level() != l || p.distanceToSqr(arbol.getX(), arbol.getY(), arbol.getZ()) > 40 * 40) {
                continue;
            }
            c.putLong("ladronDia", dia);
            WorldStore.get(s, Navidad.MODID).dirty();
            if (l.random.nextInt(3) != 0) {
                continue;
            }
            List<BlockPos> cajas = cajasCerca(l, arbol);
            if (cajas.isEmpty()) {
                continue;
            }
            DuendeLadron.llegar(l, arbol, cajas.get(l.random.nextInt(cajas.size())), p);
        }
    }

    /** (Comando de prueba) Manda al duende ladrón ya, si hay regalos bajo el árbol de alguien. */
    public static boolean ladronYa(ServerPlayer p) {
        ListTag ls = lista(p.server);
        for (int i = 0; i < ls.size(); i++) {
            CompoundTag c = ls.getCompound(i);
            ServerLevel l = nivel(p.server, c);
            if (l == null || !c.getUUID("quien").equals(p.getUUID())) {
                continue;
            }
            List<BlockPos> cajas = cajasCerca(l, pos(c));
            if (!cajas.isEmpty()) {
                DuendeLadron.llegar(l, pos(c), cajas.get(0), p);
                return true;
            }
        }
        return false;
    }
}
