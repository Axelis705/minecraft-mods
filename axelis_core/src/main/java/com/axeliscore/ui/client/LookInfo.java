package com.axeliscore.ui.client;

import com.axeliscore.data.Config;
import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import com.axeliscore.plat.client.Screens;
import com.axeliscore.ui.InfoBlock;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.InfoSource;
import com.axeliscore.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * (Cliente) La tarjeta que aparece al mirar un personaje, una criatura o un bloque de los mods de Axelis ({@link InfoSource},
 * {@link InfoBlock}): nombre, vida y qué se puede hacer. Aparece suave después de mirar un momento y se va suave.
 */
public final class LookInfo {
    private static Object key;
    private static InfoCard card;
    private static long lookSince, lastSeen;
    private static float shown;
    private static long lastFrame;
    private static Boolean enabled;
    private static boolean nearCrosshair = true;

    private LookInfo() {
    }

    /** La tarjeta de lo que se mira ahora (o null). */
    public static InfoCard current(boolean detail) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hr = mc.hitResult;
        if (mc.level == null || hr == null || hr.getType() == HitResult.Type.MISS) {
            return null;
        }
        if (hr instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof InfoSource src && !e.isInvisible()) {
                InfoCard c = new InfoCard(e.getDisplayName()).width(160);
                src.axInfo(c, detail);
                return c.isEmpty() ? null : c;
            }
        } else if (hr instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            BlockState st = mc.level.getBlockState(pos);
            if (st.getBlock() instanceof InfoBlock ib) {
                InfoCard c = new InfoCard(st.getBlock().getName()).width(160);
                ib.axInfo(mc.level, pos, st, c, detail);
                return c.isEmpty() ? null : c;
            }
        }
        return null;
    }

    private static Object keyOf() {
        HitResult hr = Minecraft.getInstance().hitResult;
        if (hr instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        if (hr instanceof BlockHitResult bhr) {
            return bhr.getBlockPos().immutable();
        }
        return null;
    }

    /** (Core) Se dibuja en la capa del HUD del Core. */
    public static void render(G g, int sw, int sh) {
        if (enabled == null) {
            Config cfg = Config.of("axelis_core");
            enabled = cfg.bool("tarjeta_al_mirar", true, "Mostrar una tarjeta al mirar personajes, criaturas y bloques de los mods de Axelis");
            nearCrosshair = !"arriba".equals(cfg.str("tarjeta_posicion", "mira",
                    "Dónde sale la tarjeta al mirar algo: \"mira\" (junto a la mira) o \"arriba\" (arriba al centro)"));
        }
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1000F);
        lastFrame = now;
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || Screens.current() != null) {
            shown = 0;
            return;
        }
        InfoCard c = current(CoreScreen.shift());
        Object k = c == null ? null : keyOf();
        if (k != null && !k.equals(key)) {
            key = k;
            lookSince = now;
        }
        if (c != null) {
            card = c;
            lastSeen = now;
        }
        boolean show = c != null && now - lookSince > 180;
        shown += ((show ? 1 : 0) - shown) * Math.min(1, dt * (show ? 10 : 7));
        if (card == null || shown < 0.02F || now - lastSeen > 1000) {
            if (!show && shown < 0.02F) {
                key = null;
            }
            return;
        }
        Cards.Layout l = Cards.measure(card);
        int x, y;
        if (nearCrosshair) {
            x = sw / 2 + 14;
            y = sh / 2 + 6;
            if (x + l.w > sw - 4) {
                x = sw / 2 - 14 - l.w;
            }
            y = Math.min(y, sh - 50 - l.h);
        } else {
            x = (sw - l.w) / 2;
            y = 6;
        }
        float a = Ui.ease(shown);
        g.push();
        g.translate(0, (1 - a) * 4, 0);
        Cards.draw(g, card, l, x, y, a * 0.97F);
        g.pop();
    }
}
