package com.mundosdeaxelis.encargo;

import com.mundosdeaxelis.registry.ModBlocks;
import com.mundosdeaxelis.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * El menú del Tablón de Encargos (etapa 280): no tiene huecos; manda al cliente los 3 encargos del día (tipo, cuántos, avance,
 * recogido), si ya se recogió la gran recompensa y el tier de las recompensas. Los botones 0, 1 y 2 recogen cada encargo y el 3
 * la gran recompensa.
 */
public class TablonMenu extends AbstractContainerMenu {
    public static final int DATA = Encargos.COUNT * 4 + 2;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public TablonMenu(int id, Inventory inv, ContainerLevelAccess access, ContainerData data) {
        super(ModMenus.TABLON.get(), id);
        this.access = access;
        this.data = data;
        addDataSlots(data);
    }

    public static TablonMenu client(int id, Inventory inv) {
        return new TablonMenu(id, inv, ContainerLevelAccess.NULL, new SimpleContainerData(DATA));
    }

    /** Los datos del servidor, leídos en vivo de los encargos del jugador. */
    public static ContainerData serverData(ServerPlayer sp) {
        return new ContainerData() {
            private long tick = -1;
            private List<Encargos.Encargo> cache;
            private boolean bonus;

            private void refresh() {
                long now = sp.level().getGameTime();
                if (now != tick || cache == null) {
                    tick = now;
                    cache = Encargos.today(sp);
                    bonus = Encargos.bonusClaimed(sp);
                }
            }

            @Override
            public int get(int i) {
                refresh();
                if (i < Encargos.COUNT * 4) {
                    int k = i / 4;
                    if (k >= cache.size()) {
                        return 0;
                    }
                    Encargos.Encargo e = cache.get(k);
                    return switch (i % 4) {
                        case 0 -> e.tipo().ordinal();
                        case 1 -> e.need();
                        case 2 -> e.progress();
                        default -> e.claimed() ? 1 : 0;
                    };
                }
                return i == Encargos.COUNT * 4 ? (bonus ? 1 : 0) : Encargos.rewardTier(sp);
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return DATA;
            }
        };
    }

    public Encargos.Tipo tipo(int k) {
        return Encargos.Tipo.of(data.get(k * 4));
    }

    public int need(int k) {
        return data.get(k * 4 + 1);
    }

    public int progress(int k) {
        return data.get(k * 4 + 2);
    }

    public boolean claimed(int k) {
        return data.get(k * 4 + 3) != 0;
    }

    public boolean bonusClaimed() {
        return data.get(Encargos.COUNT * 4) != 0;
    }

    public int tier() {
        return Math.max(1, data.get(Encargos.COUNT * 4 + 1));
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (p instanceof ServerPlayer sp && id >= 0 && id <= Encargos.COUNT) {
            boolean ok = Encargos.claim(sp, id);
            broadcastChanges();
            return ok;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.TABLON_ENCARGOS.get());
    }
}
