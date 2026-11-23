package com.pescacoleccion.nilo;

import com.axeliscore.character.Persona;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

/**
 * Nilo, el viejo pescador: llega la primera vez que alguien saca un pez, arma su muelle en esa orilla y se queda ahí pescando. Al
 * hablarle abre su ventana: encargos, tienda y su amistad (ver {@link NiloMundo}, {@link Encargos} y {@link Tienda}).
 */
public class Nilo extends Persona {
    /** Lo que está haciendo. */
    private static final int LLEGANDO = 0, OBRA = 1, A_CASA = 2, PESCANDO = 3;
    private int estado = A_CASA;
    private int t;
    /** Ciclo de pesca: 0 descanso, 1 lanzar, 2 esperar, 3 recoger. */
    private int pesca, pescaHasta = 40;
    private final ArrayDeque<Muelle.Pieza> obra = new ArrayDeque<>();
    /** Quien lo vio llegar (para saludarlo con un diálogo). */
    private UUID recibe;
    private BlockPos llegaA;
    private int charlaHasta;

    public Nilo(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        Plat.invulnerable(this);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.CANAS.get(com.pescacoleccion.pesca.Cana.ROBLE).get()));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ------------------------------------------------------------------ llegada y muelle (los pide NiloMundo)

    /** Llega caminando hasta {@code orilla}; ahí saluda a {@code p} y arma el muelle (si hay sitio). */
    void llegar(ServerPlayer p, BlockPos orilla, List<Muelle.Pieza> piezas) {
        recibe = p.getUUID();
        llegaA = orilla;
        obra.clear();
        obra.addAll(piezas);
        estado = LLEGANDO;
        t = 0;
    }

    /** Arma el muelle desde donde está (por comando). */
    void construir(List<Muelle.Pieza> piezas) {
        obra.clear();
        obra.addAll(piezas);
        estado = OBRA;
        t = 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        t++;
        if (tickCount % 100 == 0 && !NiloMundo.esElNilo(sl, this)) {
            // ya hay otro Nilo en este mundo
            discard();
            return;
        }
        switch (estado) {
            case LLEGANDO -> llegando(sl);
            case OBRA -> obra(sl);
            case A_CASA -> aCasa(sl);
            default -> pescar(sl);
        }
    }

    private void llegando(ServerLevel sl) {
        if (llegaA == null) {
            estado = A_CASA;
            return;
        }
        Vec3 dst = Vec3.atBottomCenterOf(llegaA);
        if (position().distanceToSqr(dst) > 2.5) {
            if (t % 20 == 1) {
                Plat.walkTo(this, dst.x, dst.y, dst.z, 0.7);
            }
            if (t < 240) {
                return;
            }
            moveTo(dst.x, dst.y, dst.z, getYRot(), getXRot());
        }
        getNavigation().stop();
        ServerPlayer p = recibe == null ? null : sl.getServer().getPlayerList().getPlayer(recibe);
        if (p != null && com.pescacoleccion.pelea.Peleas.peleando(p) && t < 2400) {
            // está peleando con un pez: espera a que termine para saludar (el diálogo cerraría la pelea)
            if (t % 20 == 0) {
                getLookControl().setLookAt(p, 30F, 30F);
            }
            return;
        }
        if (p != null) {
            getLookControl().setLookAt(p, 60F, 60F);
            playAction("wave");
            NiloMundo.saludo(p, this, !obra.isEmpty());
        }
        estado = OBRA;
        t = 0;
    }

    private void obra(ServerLevel sl) {
        if (obra.isEmpty()) {
            estado = A_CASA;
            t = 0;
            NiloMundo.muelleListo(sl);
            return;
        }
        if (t % 2 != 0) {
            return;
        }
        Muelle.Pieza pz = obra.poll();
        if (Muelle.cabe(sl, pz)) {
            sl.setBlock(pz.pos(), pz.state(), 3);
            sl.playSound(null, pz.pos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.2F);
            sl.sendParticles(ParticleTypes.POOF, pz.pos().getX() + 0.5, pz.pos().getY() + 0.6, pz.pos().getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.01);
            getLookControl().setLookAt(pz.pos().getX() + 0.5, pz.pos().getY() + 0.5, pz.pos().getZ() + 0.5);
            if (t % 6 == 0) {
                Plat.swing(this);
            }
        }
    }

    private void aCasa(ServerLevel sl) {
        NiloMundo.Casa c = NiloMundo.casa(sl);
        if (c == null) {
            estado = PESCANDO;
            return;
        }
        Vec3 dst = Vec3.atBottomCenterOf(c.parado());
        double d2 = position().distanceToSqr(dst);
        if (d2 < 1.0) {
            getNavigation().stop();
            estado = PESCANDO;
            pesca = 0;
            pescaHasta = t + 30;
            return;
        }
        if (d2 > 48 * 48 || t > 400) {
            // muy lejos o atorado: vuelve de un salto
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            moveTo(dst.x, dst.y, dst.z, c.yaw(), 0);
            t = 0;
            return;
        }
        if (t % 20 == 1) {
            Plat.walkTo(this, dst.x, dst.y, dst.z, 0.6);
        }
    }

    /** Pesca en su muelle: lanza, espera, recoge (a veces saca algo y festeja). */
    private void pescar(ServerLevel sl) {
        NiloMundo.Casa c = NiloMundo.casa(sl);
        if (c != null && position().distanceToSqr(Vec3.atBottomCenterOf(c.parado())) > 4) {
            estado = A_CASA;
            t = 0;
            return;
        }
        if (t < charlaHasta) {
            return;
        }
        Vec3 agua = c != null ? Vec3.atCenterOf(c.agua()) : position().add(getLookAngle().scale(4));
        if (pesca != 0) {
            getLookControl().setLookAt(agua.x, agua.y, agua.z);
            if (c != null) {
                setYRot(c.yaw());
                yBodyRot = c.yaw();
            }
        }
        if (t < pescaHasta) {
            if (pesca == 2 && t % 30 == 0) {
                sl.sendParticles(ParticleTypes.BUBBLE, agua.x, agua.y + 0.4, agua.z, 2, 0.15, 0.05, 0.15, 0.02);
            }
            return;
        }
        switch (pesca) {
            case 0 -> {
                pesca = 1;
                playAction("fish_cast");
                pescaHasta = t + 22;
            }
            case 1 -> {
                pesca = 2;
                sl.sendParticles(ParticleTypes.SPLASH, agua.x, agua.y + 0.5, agua.z, 10, 0.2, 0.05, 0.2, 0.1);
                sl.playSound(null, agua.x, agua.y, agua.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.35F, 1.1F);
                playAction("fish_hold", 180);
                pescaHasta = t + 120 + random.nextInt(60);
            }
            case 2 -> {
                pesca = 3;
                playAction("fish_reel", 30);
                pescaHasta = t + 30;
            }
            default -> {
                pesca = 0;
                if (random.nextFloat() < 0.45F) {
                    sl.sendParticles(ParticleTypes.SPLASH, agua.x, agua.y + 0.5, agua.z, 16, 0.3, 0.1, 0.3, 0.2);
                    sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 0.6F, 1F);
                    playAction("cheer");
                } else {
                    playAction(random.nextBoolean() ? "shake_head" : "think", 40);
                }
                pescaHasta = t + 60 + random.nextInt(80);
            }
        }
    }

    // ------------------------------------------------------------------ hablar

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return Plat.ok(level().isClientSide());
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player, 60F, 60F);
            pesca = 0;
            charlaHasta = t + 160;
            pescaHasta = charlaHasta;
            playAction("talk", 50);
            NiloMundo.hablar(sp, this);
        }
        return Plat.ok(level().isClientSide());
    }

    @Override
    protected Component axInfoHint() {
        return Component.translatable("pesca_coleccion.nilo.pista");
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            NiloMundo.murio(sl, this);
        }
    }
}
