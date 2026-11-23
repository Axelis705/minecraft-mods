package com.axeliscore.ui.client;

import com.axeliscore.data.Config;
import com.axeliscore.item.CoreItem;
import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.InfoCard;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * (Cliente) Cambia la ayuda de Minecraft por un {@link InfoCard} en los objetos que lo tengan: los {@link CoreItem} que respondan
 * {@link CoreItem#card} y los que registre un mod con {@link #register}. Funciona en cualquier pantalla (inventario, cofres, creativo).
 */
public final class Tooltips {
    /** Cuadro para un objeto (null = el de siempre). */
    public interface Provider {
        InfoCard card(ItemStack stack, boolean detail);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static Object lastKey;
    private static long lastFrame, since;
    private static Boolean enabled;

    private Tooltips() {
    }

    public static void register(Provider p) {
        PROVIDERS.add(p);
    }

    /** (Core) Se llama una vez al arrancar el cliente. */
    public static void init() {
        //#if MC >= 26.1
        //$$ RenderTooltipEvent.Pre.BUS.addListener((java.util.function.Predicate<RenderTooltipEvent.Pre>) Tooltips::pre);
        //#else
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Tooltips::pre);
        //#endif
    }

    //#if MC >= 26.1
    //$$ private static boolean pre(RenderTooltipEvent.Pre e) {
    //$$     return draw(e.getItemStack(), new G(e.getGraphics()), e.getX(), e.getY(), e.getScreenWidth(), e.getScreenHeight());
    //$$ }
    //#else
    private static void pre(RenderTooltipEvent.Pre e) {
        if (draw(e.getItemStack(), new G(e.getGraphics()), e.getX(), e.getY(), e.getScreenWidth(), e.getScreenHeight())) {
            e.setCanceled(true);
        }
    }
    //#endif

    /** El cuadro de un objeto (o null si usa el de Minecraft). */
    public static InfoCard cardFor(ItemStack st, boolean detail) {
        if (st == null || st.isEmpty()) {
            return null;
        }
        if (st.getItem() instanceof CoreItem ci) {
            InfoCard c = ci.card(st, detail);
            if (c != null) {
                return c;
            }
        }
        for (Provider p : PROVIDERS) {
            InfoCard c = p.card(st, detail);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private static boolean draw(ItemStack st, G g, int x, int y, int sw, int sh) {
        if (enabled == null) {
            enabled = Config.of("axelis_core").bool("cuadros_de_objetos", true,
                    "Cambiar la ayuda de los objetos de los mods de Axelis por cuadros de información");
        }
        if (!enabled) {
            return false;
        }
        InfoCard c = cardFor(st, CoreScreen.shift());
        if (c == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Object key = st.getItem();
        if (key != lastKey || now - lastFrame > 150) {
            since = now;
        }
        lastKey = key;
        lastFrame = now;
        Cards.drawAtMouse(g, c, x, y, sw, sh, (now - since) / 1000F);
        return true;
    }
}
