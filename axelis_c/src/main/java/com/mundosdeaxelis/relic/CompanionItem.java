package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Figurita de un compañero: al usarla invoca a su criatura en 3D. Equipada solo da armadura. */
public class CompanionItem extends RelicItem {
    private final Companions.Spec spec;

    public CompanionItem(Companions.Spec spec) {
        super(spec.id(), spec.rgb(), 20 * spec.cdSecs(), spec.rarity());
        this.spec = spec;
    }

    public Companions.Spec spec() {
        return spec;
    }

    @Override
    protected int interval() {
        return 40;
    }

    @Override
    public boolean isPet() {
        return true;
    }

    /** Las mascotas equipadas te acompañan solas: no tienen tecla ni acción (solo su pasiva). */
    @Override
    public Kind kind() {
        return Kind.KEY;
    }

    /**
     * Las mascotas ya no dan efectos: solo armadura (1 punto cada una; 2 la estrella de su caja). Aquí solo queda el destello
     * de adorno.
     */
    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (time % 120 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.spark(spec.rgb(), 0.4F), h.x, h.y + 0.1, h.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /** Sin habilidad activa: la mascota equipada solo te acompaña y da armadura. */
    @Override
    protected boolean hasActive() {
        return false;
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Companions.summon(sp, spec, power(stack));
        return true;
    }
}
