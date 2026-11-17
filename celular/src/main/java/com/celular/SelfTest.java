package com.celular;

import com.celular.net.NetTest;
import com.celular.plat.Plat;
import com.celular.wallet.Wallet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * {@code /celular prueba}: pruebas que corren dentro de un servidor de verdad (el CI las hace en las 4 versiones al arrancar):
 * la pantalla del celular, la luz de la linterna, los mensajes, la billetera y los mensajes de red.
 */
public final class SelfTest {
    private SelfTest() {
    }

    private static int check(Consumer<String> log, String name, boolean ok) {
        log.accept(name + (ok ? " OK" : " FALLO"));
        return ok ? 0 : 1;
    }

    public static int run(MinecraftServer s, Consumer<String> out) {
        Consumer<String> log = t -> {
            Celular.LOGGER.info("PRUEBA_CELULAR {}", t);
            out.accept(t);
        };
        int bad = 0;
        // 1. la pantalla que ven los demás (custom_model_data) se pone y se quita sin dejar nada
        try {
            ItemStack st = new ItemStack(CelularReg.PHONES.get(0).get());
            boolean all = true;
            for (int i = 1; i < PhoneUse.COUNT; i++) {
                PhoneUse.put(st, i);
                all &= PhoneUse.get(st) == i;
            }
            PhoneUse.put(st, PhoneUse.OFF);
            bad += check(log, "pantalla", all && PhoneUse.get(st) == 0 && ItemStack.matches(st, new ItemStack(CelularReg.PHONES.get(0).get())));
        } catch (Throwable e) {
            bad += check(log, "pantalla " + e, false);
        }
        // 2. la luz de la linterna: alumbra 15, y si nadie la usa se borra sola con un tick aleatorio
        try {
            ServerLevel lvl = s.overworld();
            BlockPos pos = new BlockPos(0, Plat.maxY(lvl) - 4, 0);
            lvl.getChunk(0, 0);
            if (!lvl.getBlockState(pos).isAir()) {
                log.accept("luz OMITIDA (no hay aire)");
            } else {
                lvl.setBlock(pos, CelularReg.FLASH_LIGHT.get().defaultBlockState(), 2);
                BlockState st = lvl.getBlockState(pos);
                boolean placed = st.getBlock() == CelularReg.FLASH_LIGHT.get() && st.getLightEmission() == 15 && !PhoneUse.lightInUse(lvl, pos);
                st.randomTick(lvl, pos, lvl.getRandom());
                boolean gone = lvl.getBlockState(pos).isAir();
                if (!gone) {
                    lvl.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                bad += check(log, "luz", placed && gone);
            }
        } catch (Throwable e) {
            bad += check(log, "luz " + e, false);
        }
        // 3. billetera: lo que no es ficha vale 0 (y dice si el Casino está)
        try {
            bad += check(log, "billetera", Wallet.value(new ItemStack(Items.DIAMOND)) == 0 && Wallet.value(ItemStack.EMPTY) == 0);
        } catch (Throwable e) {
            bad += check(log, "billetera " + e, false);
        }
        // 4. mensajes (claves, sin leer, guardar y leer el archivo)
        try {
            bad += com.celular.msg.Messages.selfTest(s, log);
        } catch (Throwable e) {
            bad += check(log, "mensajes " + e, false);
        }
        // 5. mensajes de red (escribir y leer de vuelta)
        bad += NetTest.run(log);
        log.accept("TOTAL fallos=" + bad + " fin");
        return bad;
    }
}
