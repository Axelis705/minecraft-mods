package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.navidad.entidad.Pinata;
import com.navidad.entidad.TrineoSanta;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lo que pasa solo en la temporada: la cuenta regresiva al entrar, el regalo de Santa el 25 de diciembre, la rosca el Día de Reyes,
 * el trineo de Santa que cruza el cielo de noche (y en Nochebuena tres veces), más las posadas y el intercambio de regalos.
 */
public final class Fiestas {
    /** Si Santa, los regalos del árbol y el duende ladrón llegan solos (la prueba con fotos lo apaga para que no estorben). */
    public static boolean automatico = true;

    private Fiestas() {
    }

    public static void init() {
        AxelisCore.onLogin(Fiestas::alEntrar);
        AxelisCore.onServerTick(Fiestas::cadaTick);
    }

    // ------------------------------------------------------------------ al entrar

    /** Lo que sabe el cliente de la temporada (para la nieve que cae y los renos). */
    public static final String KEY = Navidad.MODID + ":temporada";

    static CompoundTag estado(MinecraftServer s) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("si", Temporada.activa(s));
        t.putInt("dia", Temporada.diaDiciembre());
        return t;
    }

    /** Avisa a todos si cambió la temporada (por ejemplo con el comando). */
    public static void avisar(MinecraftServer s) {
        com.axeliscore.data.PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    static void alEntrar(ServerPlayer p) {
        MinecraftServer s = p.server;
        com.axeliscore.data.PlayerStore.Sync.toPlayer(p, KEY, estado(s));
        LocalDate h = Temporada.hoy();
        LocalDate navidad = LocalDate.of(h.getYear(), 12, 25);
        long faltan = ChronoUnit.DAYS.between(h, navidad);
        if (h.getMonth() == Month.DECEMBER && h.getDayOfMonth() == 24) {
            p.sendSystemMessage(Component.translatable("navidad.fiesta.nochebuena").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (faltan > 0 && faltan <= 30) {
            p.sendSystemMessage(Component.translatable("navidad.fiesta.faltan", faltan).withStyle(ChatFormatting.GREEN));
        }
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        if (h.getMonth() == Month.DECEMBER && h.getDayOfMonth() == 25 && yo.getInt("regaloSanta") != h.getYear()) {
            yo.putInt("regaloSanta", h.getYear());
            w.dirty();
            p.sendSystemMessage(Component.translatable("navidad.fiesta.navidad").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            darCaja(p, Premios.sacar(Premios.Rareza.EPICO, p.getRandom()), "Santa");
        }
        if (h.getMonth() == Month.JANUARY && h.getDayOfMonth() == 6 && yo.getInt("rosca") != h.getYear()) {
            yo.putInt("rosca", h.getYear());
            w.dirty();
            p.sendSystemMessage(Component.translatable("navidad.fiesta.reyes").withStyle(ChatFormatting.GOLD));
            ItemStack r = new ItemStack(ModRegistry.ROSCA.get());
            if (!p.getInventory().add(r)) {
                p.drop(r, false);
            }
        }
    }

    /** Una caja de regalo envuelta, de parte de alguien, con algo adentro, directo al inventario. */
    public static void darCaja(ServerPlayer p, ItemStack dentro, String de) {
        ItemStack caja = new ItemStack(ModRegistry.CAJA_REGALO_ITEM.get());
        CompoundTag be = new CompoundTag();
        be.put("Contenido", dentro.save(new CompoundTag()));
        be.putString("De", de);
        be.putString("Para", p.getGameProfile().getName());
        caja.getOrCreateTag().put("BlockEntityTag", be);
        if (!p.getInventory().add(caja)) {
            p.drop(caja, false);
        }
        // el confeti, un poco enfrente (si sale en la cara no se ve nada)
        Vec3 f = Vec3.directionFromRotation(0, p.getYRot()).scale(2.2);
        Premios.confeti(p.serverLevel(), p.getX() + f.x, p.getY() + 1.6, p.getZ() + f.z, true);
    }

    // ------------------------------------------------------------------ el trineo de noche

    /**
     * Santa pasa <b>una vez al día del juego</b> por cada quien (en Nochebuena, tres): a una hora al azar entre el mediodía y la madrugada,
     * pero solo cuando estás afuera (si estás en una cueva, espera a que salgas). Así lo ves aunque duermas en cuanto anochece.
     */
    static void cadaTick(MinecraftServer s) {
        ServerLevel l = s.overworld();
        if (l.getGameTime() % 1200 == 0) {
            avisar(s);
        }
        if (l.getGameTime() % 20 != 7 || !automatico || !Temporada.activa(s) || l.players().isEmpty()) {
            return;
        }
        long dia = l.getDayTime() / 24000L, hora = l.getDayTime() % 24000L;
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        int veces = Temporada.diaDiciembre() == 24 ? 3 : 1;
        for (ServerPlayer p : l.players()) {
            CompoundTag yo = w.player(p.getUUID());
            if (yo.getLong("santaDia") != dia || !yo.contains("santaHora")) {
                yo.putLong("santaDia", dia);
                yo.putLong("santaHora", 6000 + l.random.nextInt(15000));
                yo.putInt("santaVino", 0);
                w.dirty();
            }
            if (yo.getInt("santaVino") >= veces || hora < yo.getLong("santaHora") || hora > 23200 || p.isSpectator()
                    || !l.canSeeSky(p.blockPosition().above())) {
                continue;
            }
            trineo(l, p);
            // a los que están cerca también les cuenta como su visita de hoy (pasa encima de todos)
            for (ServerPlayer q : l.players()) {
                if (q.distanceTo(p) < 96) {
                    CompoundTag c = w.player(q.getUUID());
                    c.putLong("santaDia", dia);
                    c.putInt("santaVino", c.getInt("santaVino") + 1);
                    c.putLong("santaHora", hora + 3000);
                }
            }
            w.dirty();
            return;
        }
    }

    /** Manda el trineo para que pase encima de alguien. */
    public static void trineo(ServerLevel l, ServerPlayer sobre) {
        double a = l.random.nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        Vec3 desde = sobre.position().add(dir.scale(-140)).add(0, 22, 0);
        TrineoSanta tr = ModRegistry.TRINEO.get().create(l);
        if (tr == null) {
            return;
        }
        tr.moveTo(desde.x, Math.min(desde.y, l.getMaxBuildHeight() - 4), desde.z, 0, 0);
        tr.setDeltaMovement(dir.scale(0.5));
        l.addFreshEntity(tr);
        for (ServerPlayer p : l.players()) {
            if (p.distanceTo(sobre) < 96) {
                p.sendSystemMessage(Component.translatable("navidad.fiesta.trineo").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    // ------------------------------------------------------------------ posada e intercambio

    /** Una posada: piñata colgando enfrente, palo y venda para todos los de cerca, la canción y papel picado de confeti. */
    public static void posada(ServerLevel l, ServerPlayer quien) {
        Vec3 f = Vec3.directionFromRotation(0, quien.getYRot()).scale(3);
        Pinata p = ModRegistry.PINATA_ENT.get().create(l);
        if (p != null) {
            p.moveTo(quien.getX() + f.x, quien.getY() + 0.3, quien.getZ() + f.z, quien.getYRot(), 0);
            l.addFreshEntity(p);
        }
        l.playSound(null, quien.blockPosition(), ModRegistry.CANCION_PINATA.get(), SoundSource.RECORDS, 1.5F, 1F);
        for (ServerPlayer o : l.players()) {
            if (o.distanceTo(quien) < 24) {
                o.sendSystemMessage(Component.translatable("navidad.fiesta.posada", quien.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE,
                        ChatFormatting.BOLD));
                if (!o.getInventory().contains(new ItemStack(ModRegistry.PALO_PINATA.get()))) {
                    o.getInventory().add(new ItemStack(ModRegistry.PALO_PINATA.get()));
                }
                if (!o.getInventory().contains(new ItemStack(ModRegistry.VENDA.get()))) {
                    o.getInventory().add(new ItemStack(ModRegistry.VENDA.get()));
                }
                Premios.confeti(l, o.getX(), o.getY() + 2, o.getZ(), false);
            }
        }
    }

    /** Intercambio de regalos (amigo secreto): a cada quien le toca alguien distinto y solo él lo sabe. */
    public static int intercambio(MinecraftServer s) {
        List<ServerPlayer> ps = new ArrayList<>(s.getPlayerList().getPlayers());
        if (ps.size() < 2) {
            return 0;
        }
        Collections.shuffle(ps);
        for (int i = 0; i < ps.size(); i++) {
            ServerPlayer da = ps.get(i), recibe = ps.get((i + 1) % ps.size());
            da.sendSystemMessage(Component.translatable("navidad.fiesta.intercambio", recibe.getDisplayName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            da.playNotifySound(ModRegistry.CASCABELES.get(), SoundSource.PLAYERS, 1F, 1F);
        }
        return ps.size();
    }
}
