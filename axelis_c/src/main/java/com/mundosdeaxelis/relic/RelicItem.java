package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiered;
import com.mundosdeaxelis.crate.Tiers;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base de las reliquias: objetos únicos con poder pasivo (mientras están equipadas en el menú de equipo) y, si lo
 * tienen, una habilidad activa con enfriamiento: se lanza con la tecla R (reliquias de tecla) o sosteniéndolas en la
 * mano con clic derecho (reliquias de mano, que no ocupan ranura).
 */
public abstract class RelicItem extends Item implements Tiered {
    private final String id;
    private final Tier tier;
    private final int color;
    private final int cooldown;

    protected RelicItem(String id, int color, int cooldownTicks, Rarity rarity) {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Tiers.of(id).rarity));
        this.id = id;
        this.tier = Tiers.of(id);
        this.color = color;
        this.cooldown = cooldownTicks;
    }

    private static final Map<String, Supplier<SoundEvent>> SFX = Map.ofEntries(
            Map.entry("sun_crown", ModSounds.RELIC_FIRE::get), Map.entry("dragon_scale", ModSounds.RELIC_FIRE::get),
            Map.entry("frost_bracelet", ModSounds.RELIC_ICE::get),
            Map.entry("crystal_heart", ModSounds.RELIC_HEAL::get), Map.entry("oak_heart", ModSounds.RELIC_HEAL::get),
            Map.entry("eternal_rose", ModSounds.RELIC_HEAL::get),
            Map.entry("vampire_chalice", ModSounds.RELIC_DARK::get), Map.entry("specter_mask", ModSounds.RELIC_DARK::get),
            Map.entry("ender_orb", ModSounds.RELIC_DARK::get),
            Map.entry("wind_ring", ModSounds.RELIC_WHOOSH::get), Map.entry("fox_charm", ModSounds.RELIC_WHOOSH::get),
            Map.entry("wolf_fang", ModSounds.RELIC_WHOOSH::get), Map.entry("tide_shell", ModSounds.RELIC_WHOOSH::get),
            Map.entry("titan_belt", ModSounds.RELIC_BOOM::get), Map.entry("warrior_medal", ModSounds.RELIC_BOOM::get),
            Map.entry("storm_eye", ModSounds.RELIC_BOOM::get), Map.entry("smith_ring", ModSounds.RELIC_BOOM::get));

    /** Sonido al activar la habilidad (cada reliquia tiene su familia y un tono propio según su color). */
    private SoundEvent sfx() {
        Supplier<SoundEvent> s = SFX.get(id);
        return s != null ? s.get() : ModSounds.RELIC_MAGIC.get();
    }

    /** Efecto común al activar: sonido propio, círculo rúnico en el suelo, anillo de luz y destello suave en pantalla. */
    protected final void activationFx(ServerPlayer sp, ServerLevel sl) {
        activationFx(sp, sl, ItemStack.EMPTY);
    }

    protected final void activationFx(ServerPlayer sp, ServerLevel sl, ItemStack used) {
        float hue = ((color >> 16 & 255) * 3 + (color >> 8 & 255) * 5 + (color & 255)) / (9f * 255f);
        float pitch = 0.85F + hue * 0.35F;
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_ACTIVATE.get(), SoundSource.PLAYERS, 0.35F, pitch);
        SoundEvent own = RelicSounds.get(id, "cast");
        if (own != null) {
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), own, SoundSource.PLAYERS, 0.9F, 1.0F);
        } else {
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), sfx(), SoundSource.PLAYERS, 0.8F, 0.9F + hue * 0.2F);
        }
        ItemStack ref = !used.isEmpty() ? used : (sp.getMainHandItem().getItem() == this ? sp.getMainHandItem() : sp.getOffhandItem());
        double lv = level(ref);
        switch (Tiers.circle(id)) {
            case RUNE -> {
                // Círculo de invocación: solo para las reliquias de magia, áreas e invocaciones.
                // En primera persona el suelo solo se ve a partir de ~2,3 bloques: por eso es grande y,
                // además, aparece un sello vertical delante del jugador que sí se ve de frente.
                Vfx.rune(sl, sp.position(), color, 3.0 + 0.2 * lv, 36);
                Vfx.ring(sl, sp.position().add(0, 0.05, 0), color, 4.6, 20);
                Vec3 fl = new Vec3(sp.getLookAngle().x, 0, sp.getLookAngle().z);
                fl = fl.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : fl.normalize();
                Vfx.runeFacing(sl, sp.position().add(fl.scale(2.3)).add(0, 1.15, 0), fl.scale(-1), color, 1.25 + 0.05 * lv, 22);
            }
            case PULSE -> Vfx.ring(sl, sp.position().add(0, 0.05, 0), color, 3.4, 14);
            default -> Vfx.flash(sl, RelicFx.hand(sp), color, 0.9, 7);
        }
        Fx.ring(sl, Fx.spark(color, 0.6F), sp.getX(), sp.getY() + 1.0, sp.getZ(), 0.9, 14, 0.4);
        Fx.screen(sp, 1, color, Tiers.circle(id) == Tiers.Circle.RUNE ? 14 : 8);
    }

    // ---- Niveles: cada uso suma experiencia; a más nivel, menos enfriamiento y más potencia ----

    private static final int[] LEVEL_USES = {0, 10, 25, 50, 90};

    /** Nivel (1-5) de la figurita o reliquia que está haciendo su pasiva en este momento. */
    protected int equippedLevel = 1;

    /** Pone el objeto en ese nivel (de 1 a 5) sin bajarlo si ya tenía más experiencia. */
    public static void setLevel(ItemStack stack, int level) {
        int lv = Math.max(1, Math.min(LEVEL_USES.length, level));
        stack.getOrCreateTag().putInt("mdc_uses", Math.max(uses(stack), LEVEL_USES[lv - 1]));
    }

    public static int uses(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("mdc_uses") : 0;
    }

    /** Nivel de 1 a 5. */
    public static int level(ItemStack stack) {
        int u = uses(stack);
        int lv = 1;
        for (int i = 1; i < LEVEL_USES.length; i++) {
            if (u >= LEVEL_USES[i]) {
                lv = i + 1;
            }
        }
        return lv;
    }

    /** Multiplicador de daño / duración según el nivel (1.0 a 1.4) y la rareza (0,9 a 1,5). */
    public static float power(ItemStack stack) {
        return (1.0F + 0.1F * (level(stack) - 1)) * Tiers.of(stack).power;
    }

    /** Enfriamiento ya reducido por el nivel (5 % menos por nivel sobre el 1). */
    protected static int scaledCooldown(ItemStack stack, int ticks) {
        return Math.max(1, Math.round(ticks * (1.0F - 0.05F * (level(stack) - 1)) * Tiers.of(stack).cooldown));
    }

    protected final void bump(ServerPlayer sp, ItemStack stack) {
        int before = level(stack);
        stack.getOrCreateTag().putInt("mdc_uses", uses(stack) + 1);
        int after = level(stack);
        if (after > before) {
            ServerLevel sl = sp.serverLevel();
            Vec3 p = sp.position();
            Fx.sound(sl, p.x, p.y, p.z, ModSounds.RELIC_LEVELUP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            Vfx.column(sl, p, color, 0.7, 6.5, 36);
            Vfx.rune(sl, p, 0xFFFFFF, 2.6, 40);
            Vfx.flash(sl, p.add(0, 1.2, 0), color, 2.6, 14);
            Fx.screen(sp, 0, color, 12);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.relic_levelup", stack.getHoverName(), after)
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    public int color() {
        return color;
    }

    @Override
    public Tier tier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return com.mundosdeaxelis.crate.Tiers.shown(this).paint(super.getName(stack));
    }

    /** Nombre interno ("star_bow"...), usado para los textos y los sonidos propios. */
    public String relicId() {
        return id;
    }

    /** Cómo se usa una reliquia. */
    public enum Kind {
        /** Solo poder pasivo: basta con llevarla equipada. */
        PASSIVE,
        /** Pasivo mientras está equipada y habilidad con la tecla R. */
        KEY,
        /** Hay que sostenerla en la mano y mantener el clic derecho; no ocupa ranura de equipo. */
        HAND
    }

    public Kind kind() {
        if (channelTicks() > 0) {
            return Kind.HAND;
        }
        return hasActive() ? Kind.KEY : Kind.PASSIVE;
    }

    /** ¿Es la figurita de una mascota? (van en las ranuras de mascotas y se llaman con la tecla V). */
    public boolean isPet() {
        return false;
    }

    /**
     * Devuelve el objeto de esta reliquia si el jugador la tiene activa: equipada en el menú o, si es de mano, sostenida
     * en una mano. Llevarla suelta en la barra rápida ya no la activa.
     */
    public static ItemStack carried(Player player, Supplier<? extends Item> item) {
        Item it = item.get();
        if (player instanceof ServerPlayer sp) {
            ItemStack eq = Equipment.find(sp, item);
            if (!eq.isEmpty()) {
                return eq;
            }
        }
        if (it instanceof RelicItem r && r.kind() == Kind.HAND) {
            if (player.getMainHandItem().is(it)) {
                return player.getMainHandItem();
            }
            if (player.getOffhandItem().is(it)) {
                return player.getOffhandItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /** Cada cuántos ticks se llama a {@link #passive}. */
    protected int interval() {
        return 10;
    }

    protected abstract void passive(ServerPlayer sp, ServerLevel sl, long time);

    protected boolean hasActive() {
        return cooldown > 0;
    }

    /** Habilidad activa; devuelve false si no pudo usarse (no gasta enfriamiento). */
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        return false;
    }

    // ---- Uso mantenido (el jugador levanta la reliquia mientras mantiene el clic derecho) ----

    /** Si es mayor que 0, la habilidad se usa manteniendo el clic derecho hasta ese número de ticks. */
    protected int channelTicks() {
        return 0;
    }

    /** Postura del jugador mientras la usa (arco, catalejo, cuerno, cepillo...). */
    protected UseAnim anim() {
        return UseAnim.BOW;
    }

    /** Al empezar a mantener; devuelve false para rechazar. */
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        return true;
    }

    /** Cada tick mientras se mantiene (used = ticks que lleva). */
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
    }

    /** Al terminar: soltó antes (completed = false) o llegó al máximo (true). Aquí se llama a {@link #spend}. */
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
    }

    /** Gasta el enfriamiento y lanza el efecto común de activación. */
    protected final void spend(ServerPlayer sp, ItemStack stack, int cooldownTicks, boolean fx) {
        sp.getCooldowns().addCooldown(this, scaledCooldown(stack, cooldownTicks));
        stack.getOrCreateTag().putBoolean("mdc_cd", true);
        bump(sp, stack);
        if (fx) {
            activationFx(sp, sp.serverLevel(), stack);
        }
    }

    protected final int cooldownTicks() {
        return cooldown;
    }

    /**
     * Mientras se mantiene el clic derecho, la reliquia se baja a la esquina de la pantalla
     * (antes quedaba delante de la cara y tapaba la vista).
     */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        if (channelTicks() <= 0) {
            return;
        }
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
                if (!player.isUsingItem() || player.getUseItem().getItem() != stack.getItem()) {
                    return false;
                }
                float side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
                float t = Math.min(1.0F, (player.getTicksUsingItem() + partial) / 6.0F);
                float k = t * t * (3.0F - 2.0F * t);
                ps.translate(side * (0.56F + 0.16F * k), -0.52F - 0.16F * k - 0.6F * equip, -0.72F - 0.10F * k);
                ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(side * -10.0F * k));
                ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-14.0F * k));
                float sc = 1.0F - 0.25F * k;
                ps.scale(sc, sc, sc);
                return true;
            }
        });
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return channelTicks() > 0 ? channelTicks() : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return channelTicks() > 0 ? anim() : UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (!level.isClientSide && living instanceof ServerPlayer sp && channelTicks() > 0) {
            channelTick(sp, sp.serverLevel(), stack, channelTicks() - remaining);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide && living instanceof ServerPlayer sp && channelTicks() > 0) {
            channelEnd(sp, sp.serverLevel(), stack, channelTicks() - timeLeft, false);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide && living instanceof ServerPlayer sp && channelTicks() > 0) {
            channelEnd(sp, sp.serverLevel(), stack, channelTicks(), true);
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** Que cambiar los datos internos (enfriamiento, usos, combustible) no haga "saltar" el objeto en la mano. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasActive()) {
            return InteractionResultHolder.pass(stack);
        }
        if (kind() != Kind.HAND) {
            // las reliquias de tecla y las mascotas se equipan en el menú (G); ya no se usan con clic derecho
            if (!level.isClientSide && player instanceof ServerPlayer hp) {
                hp.displayClientMessage(Component.translatable("message.mundos_de_axelis.equip_hint").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.pass(stack);
        }
        if (channelTicks() > 0) {
            if (!level.isClientSide && player instanceof ServerPlayer csp && !channelStart(csp, csp.serverLevel(), stack)) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (active(sp, sp.serverLevel(), stack)) {
            sp.getCooldowns().addCooldown(this, scaledCooldown(stack, cooldown));
            stack.getOrCreateTag().putBoolean("mdc_cd", true);
            bump(sp, stack);
            activationFx(sp, sp.serverLevel());
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        return InteractionResultHolder.fail(stack);
    }

    /** Las reliquias de mano solo funcionan mientras están en la mano principal o en la secundaria. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp) || kind() != Kind.HAND) {
            return;
        }
        boolean off = sp.getOffhandItem() == stack;
        boolean main = slot == sp.getInventory().selected && sp.getInventory().items.get(slot) == stack;
        if (!off && !main) {
            return;
        }
        tickEquipped(sp, stack, -1, level.getGameTime());
    }

    /**
     * Cada tick de una reliquia o mascota que está funcionando (equipada, o de mano sostenida): aviso de que ya se
     * recargó, poder pasivo y destellos. {@code idx} reparte los destellos entre las ranuras para no llenar la pantalla.
     */
    public final void tickEquipped(ServerPlayer sp, ItemStack stack, int idx, long t) {
        ServerLevel sl = sp.serverLevel();
        if (t % 5 == 0 && stack.hasTag() && stack.getTag().getBoolean("mdc_cd") && !sp.getCooldowns().isOnCooldown(this)) {
            stack.getTag().remove("mdc_cd");
            if (stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_READY.get(), SoundSource.PLAYERS, 0.45F, 1.0F);
            Fx.ring(sl, Fx.spark(color, 0.5F), sp.getX(), sp.getY() + 1.0, sp.getZ(), 0.7, 12, 0);
        }
        if (t % interval() == 0) {
            equippedLevel = level(stack);
            passive(sp, sl, t);
        }
        boolean emit = idx < 0 ? t % 12 == 0 : t % 12 == 0 && ((t / 12) + idx) % 3 == 0;
        if (emit) {
            double a = t * 0.07;
            for (int i = 0; i < 2; i++) {
                double ang = a + i * Math.PI;
                sl.sendParticles(Fx.dust(color, 0.45F), sp.getX() + Math.cos(ang) * 0.75, sp.getY() + 0.9 + Math.sin(t * 0.05 + i) * 0.5,
                        sp.getZ() + Math.sin(ang) * 0.75, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Lanza la habilidad de una reliquia (o llama a una mascota) que está equipada. Devuelve false si está en
     * enfriamiento o no pudo usarse (en ese caso no gasta nada).
     */
    public final boolean useEquipped(ServerPlayer sp, ItemStack stack) {
        if (!hasActive() || channelTicks() > 0) {
            return false;
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        ServerLevel sl = sp.serverLevel();
        if (!active(sp, sl, stack)) {
            return false;
        }
        sp.getCooldowns().addCooldown(this, scaledCooldown(stack, cooldown));
        stack.getOrCreateTag().putBoolean("mdc_cd", true);
        bump(sp, stack);
        activationFx(sp, sl, stack);
        return true;
    }

    private static String fmtArmor(float a) {
        return a == Math.floor(a) ? Integer.toString((int) a) : Float.toString(a);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.tier", com.mundosdeaxelis.crate.Tiers.shown(this).label()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.relic").withStyle(ChatFormatting.GOLD));
        String kindKey = isPet() ? "pet" : kind().name().toLowerCase(java.util.Locale.ROOT);
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.kind." + kindKey).withStyle(ChatFormatting.DARK_AQUA));
        if (!isPet()) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + id + ".1").withStyle(ChatFormatting.GRAY));
        }
        if (isPet()) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.pet_armor", fmtArmor(com.mundosdeaxelis.crate.CratePools.petArmor(id))).withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.pet_only").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (hasActive() && !isPet()) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + id + ".2").withStyle(ChatFormatting.AQUA));
            if (cooldown > 0) {
                tooltip.add(Component.translatable("tooltip.mundos_de_axelis.relic.cooldown", Math.round(cooldown * tier.cooldown / 20F)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        int lv = level(stack);
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= lv ? "\u2605" : "\u2606");
        }
        int u = uses(stack);
        if (lv < 5) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.relic.level", stars.toString(), lv, u, LEVEL_USES[lv])
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.relic.level_max", stars.toString(), lv).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + id + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
