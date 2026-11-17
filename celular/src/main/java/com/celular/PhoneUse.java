package com.celular;

import com.celular.plat.Plat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Lo que los demás ven cuando usas el celular: la pantalla se enciende con la app que tienes abierta (el número va en el
 * "custom_model_data" del celular que tienes en la mano, así cualquier versión lo dibuja con su modelo) y el brazo se pone en pose
 * de escribir. Con la linterna, el brazo apunta hacia adelante y el servidor pone una luz invisible donde apuntas.
 */
public final class PhoneUse {
    // pantallas (el mismo orden que SCREENS en herramientas/assets.py)
    public static final int OFF = 0, HOME = 1, INSTA = 2, CAMERA = 3, GALLERY = 4, CALL = 5, PATREON = 6, SETTINGS = 7, MUSIC = 8,
            WEATHER = 9, COMPASS = 10, CALC = 11, MESSAGES = 12, NOTES = 13, MAP = 14, WALLET = 15, FLASH = 16, JAM = 17, EAR = 18;
    public static final int COUNT = 19;
    /** Hasta dónde llega la luz de la linterna. */
    private static final double FLASH_RANGE = 28.0;

    /** Pantalla que pidió cada jugador (lo que esté usando ahora). */
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /** La luz de la linterna de cada jugador: dónde quedó. */
    private record Light(ResourceKey<Level> dim, BlockPos pos) {
    }

    private static final Map<UUID, Light> LIGHTS = new HashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Celular Pantallas");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> ticking;
    private static MinecraftServer server;

    private PhoneUse() {
    }

    // ------------------------------------------------------------------ el número en el celular

    /** La pantalla que muestra este celular (0 = apagada). */
    public static int get(ItemStack s) {
        //#if MC >= 1.21.4
        //$$ net.minecraft.world.item.component.CustomModelData c = s.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        //$$ return c == null || c.floats().isEmpty() ? 0 : Math.round(c.floats().get(0));
        //#elif MC >= 1.20.5
        //$$ net.minecraft.world.item.component.CustomModelData c = s.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        //$$ return c == null ? 0 : c.value();
        //#else
        net.minecraft.nbt.CompoundTag t = s.getTag();
        return t == null ? 0 : t.getInt("CustomModelData");
        //#endif
    }

    public static void put(ItemStack s, int v) {
        if (get(s) == v) {
            return;
        }
        //#if MC >= 1.21.4
        //$$ if (v == 0) {
        //$$     s.remove(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        //$$ } else {
        //$$     s.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
        //$$             new net.minecraft.world.item.component.CustomModelData(java.util.List.of((float) v), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        //$$ }
        //#elif MC >= 1.20.5
        //$$ if (v == 0) {
        //$$     s.remove(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        //$$ } else {
        //$$     s.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(v));
        //$$ }
        //#else
        if (v == 0) {
            net.minecraft.nbt.CompoundTag t = s.getTag();
            if (t != null) {
                t.remove("CustomModelData");
                if (t.isEmpty()) {
                    s.setTag(null);
                }
            }
        } else {
            s.getOrCreateTag().putInt("CustomModelData", v);
        }
        //#endif
    }

    /** El celular que tiene en la mano (primero la derecha), o null. */
    private static ItemStack inHand(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof PhoneItem) {
            return p.getMainHandItem();
        }
        if (p.getOffhandItem().getItem() instanceof PhoneItem) {
            return p.getOffhandItem();
        }
        return null;
    }

    // ------------------------------------------------------------------ lo que pide el celular

    /** El celular de {@code p} dice qué está usando (0 = lo guardó). */
    public static void set(ServerPlayer p, int screen) {
        if (screen < 0 || screen >= COUNT) {
            return;
        }
        MinecraftServer s = Plat.server(p);
        if (screen == OFF) {
            ACTIVE.remove(p.getUUID());
            lightOff(s, p.getUUID());
        } else {
            ACTIVE.put(p.getUUID(), screen);
            if (screen != FLASH) {
                lightOff(s, p.getUUID());
            }
            startTimer(s);
        }
        sync(p);
    }

    /** Pone la pantalla correcta en el celular de la mano y apaga los demás celulares del inventario. */
    private static void sync(ServerPlayer p) {
        Integer want = ACTIVE.get(p.getUUID());
        ItemStack hand = inHand(p);
        for (ItemStack st : Plat.mainAndOffhand(p.getInventory())) {
            if (st.getItem() instanceof PhoneItem && st != hand && get(st) != 0) {
                put(st, 0);
            }
        }
        if (hand != null) {
            put(hand, want == null ? 0 : want);
        }
    }

    // ------------------------------------------------------------------ reloj (10 veces por segundo)

    private static void startTimer(MinecraftServer s) {
        server = s;
        if (ticking == null) {
            ticking = TIMER.scheduleAtFixedRate(() -> {
                MinecraftServer sv = server;
                if (sv != null) {
                    sv.execute(PhoneUse::tick);
                }
            }, 100, 100, TimeUnit.MILLISECONDS);
        }
    }

    private static void tick() {
        MinecraftServer s = server;
        if (s == null) {
            return;
        }
        for (UUID id : new ArrayList<>(ACTIVE.keySet())) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) {
                ACTIVE.remove(id);
                lightOff(s, id);
                continue;
            }
            sync(p);
            if (ACTIVE.get(id) == FLASH) {
                if (inHand(p) == null || p.isSpectator()) {
                    lightOff(s, id);                       // sin el celular en la mano no hay linterna
                } else {
                    flash(s, p);
                }
            }
        }
        if (ACTIVE.isEmpty() && LIGHTS.isEmpty() && ticking != null) {
            ticking.cancel(false);
            ticking = null;
        }
    }

    // ------------------------------------------------------------------ linterna

    /** ¿Esta luz la está usando alguna linterna? (las que no, se borran solas). */
    public static boolean lightInUse(Level level, BlockPos pos) {
        for (Light l : LIGHTS.values()) {
            if (l.pos.equals(pos) && l.dim == level.dimension()) {
                return true;
            }
        }
        return false;
    }

    private static void flash(MinecraftServer s, ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(FLASH_RANGE));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        BlockPos target = null;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bh = (BlockHitResult) hit;
            // el bloque de aire justo delante de lo que alumbras
            target = bh.getBlockPos().relative(bh.getDirection());
        } else {
            target = BlockPos.containing(end);
        }
        // si ahí no cabe (agua, plantas, otro bloque), se busca un poco más cerca
        Vec3 back = p.getLookAngle().scale(-1);
        Vec3 at = Vec3.atCenterOf(target);
        BlockPos chosen = null;
        for (int k = 0; k < 4; k++) {
            BlockPos b = BlockPos.containing(at.add(back.scale(k)));
            BlockState st = level.getBlockState(b);
            if (st.isAir() || st.getBlock() == CelularReg.FLASH_LIGHT.get()) {
                chosen = b;
                break;
            }
        }
        Light old = LIGHTS.get(p.getUUID());
        if (chosen == null) {
            lightOff(s, p.getUUID());
            return;
        }
        if (old != null && old.pos.equals(chosen) && old.dim == level.dimension()
                && level.getBlockState(chosen).getBlock() == CelularReg.FLASH_LIGHT.get()) {
            return;                                        // sigue alumbrando lo mismo
        }
        LIGHTS.put(p.getUUID(), new Light(level.dimension(), chosen));
        if (old != null) {
            remove(s, old);
        }
        if (level.getBlockState(chosen).isAir()) {
            level.setBlock(chosen, CelularReg.FLASH_LIGHT.get().defaultBlockState(), 2);
        }
    }

    private static void lightOff(MinecraftServer s, UUID id) {
        Light l = LIGHTS.remove(id);
        if (l != null) {
            remove(s, l);
        }
    }

    private static void remove(MinecraftServer s, Light l) {
        ServerLevel level = s.getLevel(l.dim);
        if (level == null || !level.isLoaded(l.pos)) {
            return;                                        // si no está cargado, se borrará sola cuando vuelva a cargarse
        }
        for (Light o : LIGHTS.values()) {
            if (o.pos.equals(l.pos) && o.dim == l.dim) {
                return;                                    // otra linterna alumbra en el mismo sitio
            }
        }
        if (level.getBlockState(l.pos).getBlock() == CelularReg.FLASH_LIGHT.get()) {
            level.setBlock(l.pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ entrar, salir, apagar

    public static void logout(ServerPlayer p) {
        ACTIVE.remove(p.getUUID());
        lightOff(Plat.server(p), p.getUUID());
        sync(p);
    }

    /** Al entrar: ningún celular del inventario se queda encendido de antes. */
    public static void login(ServerPlayer p) {
        ACTIVE.remove(p.getUUID());
        sync(p);
    }

    public static void clear(MinecraftServer s) {
        for (Light l : new ArrayList<>(LIGHTS.values())) {
            remove(s, l);
        }
        LIGHTS.clear();
        ACTIVE.clear();
        if (ticking != null) {
            ticking.cancel(false);
            ticking = null;
        }
        server = null;
    }
}
