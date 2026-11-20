package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.entity.Mascota;
import com.mundosdeaxelis.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Menú de una mascota (agachado + clic derecho sobre ella, etapa 254): su nombre, sentarse o seguirte, acariciarla, un truco,
 * su modo de combate, su acción especial y, si tiene, su mochila. Los botones usan el mecanismo de los menús del juego
 * ({@link #clickMenuButton}); el nombre llega con {@code PetNamePacket}.
 */
public class PetMenu extends AbstractContainerMenu {
    public static final int BUT_SIT = 0, BUT_PET = 1, BUT_TRICK = 2, BUT_MODE = 3, BUT_SPECIAL = 4;
    /** Datos que se mandan al cliente: cariño, modo, interruptor, segundos de espera de la acción y si está sentada. */
    public static final int D_LOVE = 0, D_MODE = 1, D_TOGGLE = 2, D_WAIT = 3, D_SIT = 4, D_N = 5;
    public static final int W = 176;
    /** Altura donde empieza la parte de abajo (mochila o inventario). */
    public static final int TOP = 120;

    public final int petId;
    public final int bagSize;
    public final SimpleContainer bag;
    private final ContainerData data;
    private final Mascota pet;
    private final ItemStack figurine;

    /** Cliente. */
    public PetMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readVarInt(), buf.readVarInt(), null, ItemStack.EMPTY, new SimpleContainerData(D_N));
    }

    /** Servidor. */
    public PetMenu(int id, Inventory inv, Mascota pet, ItemStack figurine) {
        this(id, inv, pet.getId(), pet.kind() != null && pet.kind().special == PetKinds.Special.BAG ? pet.kind().bag : 0, pet, figurine, live(pet, figurine));
    }

    private PetMenu(int id, Inventory inv, int petId, int bagSize, Mascota pet, ItemStack figurine, ContainerData data) {
        super(ModMenus.PET.get(), id);
        this.petId = petId;
        this.bagSize = bagSize;
        this.pet = pet;
        this.figurine = figurine;
        this.data = data;
        this.bag = new SimpleContainer(Math.max(1, bagSize));
        if (pet != null && bagSize > 0) {
            PetData.loadBag(figurine, bag);
            bag.addListener(c -> saveBag());
        }
        int rows = bagRows();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(bag, r * 9 + c, 8 + c * 18, bagY() + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack st) {
                        return allowedInBag(st);
                    }
                });
            }
        }
        int iy = invY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, iy + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c, 8 + c * 18, iy + 58));
        }
        this.addDataSlots(data);
    }

    /** Valores vivos para el cliente (se leen en cada sincronización). */
    private static ContainerData live(Mascota m, ItemStack fig) {
        return new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case D_LOVE -> m.love();
                    case D_MODE -> m.mode();
                    case D_TOGGLE -> PetActions.toggled(m) ? 1 : 0;
                    case D_WAIT -> PetActions.secondsLeft(m, fig);
                    case D_SIT -> m.isOrderedToSit() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return D_N;
            }
        };
    }

    public int bagRows() {
        return bagSize / 9;
    }

    public int bagY() {
        return TOP + 12;
    }

    public int invY() {
        return bagSize > 0 ? bagY() + bagRows() * 18 + 16 : TOP + 12;
    }

    public int height() {
        return invY() + 58 + 18 + 6;
    }

    public int get(int i) {
        return data.get(i);
    }

    /** En la mochila no caben cajas de shulker (guardaría cofres dentro de la figurita). */
    public static boolean allowedInBag(ItemStack st) {
        return !(st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) && !(st.getItem() instanceof CompanionItem);
    }

    private void saveBag() {
        if (pet == null || figurine.isEmpty()) {
            return;
        }
        PetData.saveBag(figurine, bag);
        if (pet.ownerPlayer() != null) {
            PetKeeper.saved(pet.ownerPlayer(), false);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (pet != null && bagSize > 0) {
            saveBag();
            if (player instanceof ServerPlayer sp) {
                PetKeeper.saved(sp, true);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (pet == null) {
            return true;
        }
        if (!pet.isAlive() || pet.isRemoved() || !pet.isOwnedBy(player) || player.distanceToSqr(pet) > 12 * 12) {
            return false;
        }
        return player instanceof ServerPlayer sp && pet.spec() != null && PetKeeper.figurine(sp, pet.spec()) == figurine;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pet == null || !(player instanceof ServerPlayer sp) || !stillValid(player)) {
            return false;
        }
        switch (id) {
            case BUT_SIT -> pet.toggleSit(sp);
            case BUT_PET -> pet.happy(sp, true);
            case BUT_TRICK -> pet.trick();
            case BUT_MODE -> pet.cycleMode();
            case BUT_SPECIAL -> PetActions.special(pet, sp, figurine);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Cambia el nombre (vacío: vuelve a llamarse como su especie). */
    public void rename(ServerPlayer sp, String raw) {
        if (pet == null || !stillValid(sp)) {
            return;
        }
        String s = raw == null ? "" : net.minecraft.SharedConstants.filterText(raw).trim();
        if (s.length() > PetData.MAX_NAME) {
            s = s.substring(0, PetData.MAX_NAME);
        }
        pet.setCustomName(s.isEmpty() ? null : Component.literal(s));
        pet.voice(0.6F, 1.3F);
        pet.level().broadcastEntityEvent(pet, Mascota.EV_HAPPY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (bagSize <= 0) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        int bagN = bagRows() * 9;
        if (index < bagN) {
            if (!this.moveItemStackTo(st, bagN, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!allowedInBag(st) || !this.moveItemStackTo(st, 0, bagN, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (st.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
