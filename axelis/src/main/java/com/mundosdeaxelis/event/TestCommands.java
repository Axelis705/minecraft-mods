package com.mundosdeaxelis.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.gear.ArmorDefs;
import com.mundosdeaxelis.gear.GearDef;
import com.mundosdeaxelis.gear.GearDefs;
import com.mundosdeaxelis.gear.GearSet;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comandos de prueba para no tener que jugar horas: {@code /corazon prueba ...} (hace falta permiso de operador).
 * <pre>
 *   /corazon prueba capas                    -> te da las 8 capas
 *   /corazon prueba equipo &lt;set|todos&gt;      -> te da la armadura (4 piezas) y todas las herramientas de ese set
 *   /corazon prueba llaves [cantidad]       -> te da esa cantidad de llaves de cada caja (por defecto 5)
 *   /corazon prueba cajas                   -> te da las ocho cajas de colección
 *   /corazon prueba esencia &lt;cantidad&gt;      -> te da Esencia
 *   /corazon prueba niveles                 -> muestra qué niveles de calidad da cada caja
 *   /corazon prueba simular &lt;caja&gt; [n]      -> simula n aperturas (1000 por defecto) y cuenta qué sale
 * </pre>
 */
public final class TestCommands {
    private TestCommands() {
    }

    private static List<String> setIds() {
        List<String> l = new ArrayList<>();
        for (GearSet s : GearSet.values()) {
            l.add(s.id);
        }
        l.add("todos");
        return l;
    }

    private static List<String> crateIds() {
        List<String> l = new ArrayList<>();
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS) {
                l.add(k.id);
            }
        }
        return l;
    }

    /**
     * Etapa 268: tira el botín de los cofres muchas veces (cada tipo, las etapas 1 a 8 y las cerraduras 1 a 5) y comprueba que nunca
     * sale vacío, ni con pilas vacías o demasiado grandes, ni con más de 27 cosas; que los fragmentos de llave son siempre los de la
     * caja de la etapa y que el gran tesoro siempre trae alguno. Dice de media cuántas pilas, fragmentos y libros salen de cada tipo.
     */
    private static int botin(CommandSourceStack src) {
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(268L);
        String fail = null;
        StringBuilder stats = new StringBuilder();
        for (com.mundosdeaxelis.block.VaultLoot.Kind k : com.mundosdeaxelis.block.VaultLoot.Kind.values()) {
            int rolls = 0, stacks = 0, frags = 0, books = 0, hearts = 0, runes = 0, gems = 0, essences = 0;
            for (int st = 1; st <= 8; st++) {
                for (int lv = 1; lv <= 5; lv++) {
                    for (int i = 0; i < 20; i++) {
                        java.util.List<net.minecraft.world.item.ItemStack> out = com.mundosdeaxelis.block.VaultLoot.roll(k, st, r, lv);
                        rolls++;
                        stacks += out.size();
                        if (out.isEmpty() || out.size() > 27) {
                            fail = k + " etapa " + st + ": " + out.size() + " pilas";
                        }
                        boolean frag = false, heart = false, essence = false;
                        for (net.minecraft.world.item.ItemStack it : out) {
                            if (it.isEmpty() || it.getCount() > it.getMaxStackSize()) {
                                fail = k + " etapa " + st + ": pila rara " + it;
                            }
                            if (it.getItem() instanceof com.mundosdeaxelis.item.KeyFragmentItem kf) {
                                frag = true;
                                frags += it.getCount();
                                if (kf.kind != com.mundosdeaxelis.crate.CrateKind.byIndex(st)) {
                                    fail = k + " etapa " + st + ": fragmento de otra caja (" + kf.kind.id + ")";
                                }
                            }
                            if (it.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
                                books++;
                            }
                            // etapa 269: sin pociones; runas y reparadores siempre del tier del cofre
                            if (it.is(net.minecraft.world.item.Items.POTION)) {
                                fail = k + " etapa " + st + ": trae una poción";
                            }
                            if (it.getItem() instanceof com.mundosdeaxelis.item.EssenceRuneItem er && er.tier != st) {
                                fail = k + " etapa " + st + ": runa de otro tier (" + er.tier + ")";
                            }
                            com.mundosdeaxelis.crate.CrateKind ck = com.mundosdeaxelis.crate.CrateKind.byIndex(st);
                            if (it.getItem() instanceof com.mundosdeaxelis.item.AcornItem
                                    && it.getItem() != com.mundosdeaxelis.registry.ModItems.mender(ck, false)
                                    && it.getItem() != com.mundosdeaxelis.registry.ModItems.mender(ck, true)) {
                                fail = k + " etapa " + st + ": reparador de otro tier (" + it + ")";
                            }
                            if (it.is(com.mundosdeaxelis.registry.ModItems.HEART_FRAGMENT.get())) {
                                hearts++;
                                heart = true;
                            }
                            if (it.getItem() instanceof com.mundosdeaxelis.item.EssenceRuneItem) {
                                runes += it.getCount();
                            }
                            // etapa 271: gemas vacías con la pureza del tier y esencias del elemento del tier
                            if (it.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Vacia) {
                                gems += it.getCount();
                                if (com.mundosdeaxelis.gema.Gemas.pureza(it) != com.mundosdeaxelis.gema.Gemas.purezaDeTier(st)) {
                                    fail = k + " etapa " + st + ": gema vacía con otra pureza";
                                }
                                if (!com.mundosdeaxelis.block.VaultLoot.isStar(it)) {
                                    fail = "la gema vacía no es pieza estrella";
                                }
                            }
                            if (it.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Esencia es) {
                                essences += it.getCount();
                                essence = true;
                                if (es.el != com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(st)) {
                                    fail = k + " etapa " + st + ": esencia de otro tier (" + es.el.id + ")";
                                }
                            }
                        }
                        if (k == com.mundosdeaxelis.block.VaultLoot.Kind.TREASURE && !frag) {
                            fail = "TREASURE etapa " + st + ": sin fragmentos";
                        }
                        if (k == com.mundosdeaxelis.block.VaultLoot.Kind.TREASURE && !heart) {
                            fail = "TREASURE etapa " + st + ": sin fragmento de corazón";
                        }
                        if ((k == com.mundosdeaxelis.block.VaultLoot.Kind.TREASURE || k == com.mundosdeaxelis.block.VaultLoot.Kind.CRYPT) && !essence) {
                            fail = k + " etapa " + st + ": sin esencia de infusión";
                        }
                    }
                }
            }
            stats.append(String.format(java.util.Locale.ROOT, " %s: %.1f pilas, %.2f fragmentos, %.2f libros, %.2f runas, %.2f corazones, %.2f gemas, %.2f esencias;",
                    k.name(), stacks / (double) rolls, frags / (double) rolls, books / (double) rolls, runes / (double) rolls, hearts / (double) rolls,
                    gems / (double) rolls, essences / (double) rolls));
        }
        String msg = (fail == null ? "PRUEBA_BOTIN_OK" : "PRUEBA_BOTIN_FALLO " + fail) + stats;
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /**
     * Etapa 271: las gemas sin jugador (con un jugador falso de Forge). Engarza y quita gemas, comprueba el efecto de cada elemento
     * golpeando a un cerdo y hace el camino completo de la Mesa de Gemas: infundir (gasta Esencia), engarzar y quitar.
     */
    private static int gemas(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        String fail = null;
        StringBuilder info = new StringBuilder();
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        fp.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        for (com.mundosdeaxelis.gema.Gemas.Elemento el : com.mundosdeaxelis.gema.Gemas.Elemento.values()) {
            net.minecraft.world.item.ItemStack sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            com.mundosdeaxelis.gema.Gemas.engarzar(sword, el, 4);
            if (com.mundosdeaxelis.gema.Gemas.gemaDe(sword) != el || com.mundosdeaxelis.gema.Gemas.purezaDe(sword) != 4) {
                fail = "engarce " + el.id;
            }
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
            fp.setHealth(10.0F);
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(sl);
            if (pig == null) {
                fail = "sin cerdo";
                break;
            }
            pig.moveTo(base.getX() + 2.5, base.getY(), base.getZ() + 0.5);
            pig.setNoAi(true);
            sl.addFreshEntity(pig);
            float before = pig.getHealth();
            pig.hurt(sl.damageSources().playerAttack(fp), 2.0F);
            float lost = before - pig.getHealth();
            boolean ok = switch (el) {
                case VENENO -> pig.hasEffect(net.minecraft.world.effect.MobEffects.POISON);
                case FILO -> lost > 2.2F;
                case ESCARCHA -> pig.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                case FUEGO -> pig.getRemainingFireTicks() > 0;
                case ARCANO -> lost > 4.0F || pig.isDeadOrDying();
                case VAMPIRO -> fp.getHealth() > 10.0F;
                case RAYO -> lost >= 2.0F;
                case VACIO -> pig.hasEffect(net.minecraft.world.effect.MobEffects.WITHER);
            };
            info.append(String.format(java.util.Locale.ROOT, " %s:%.1f", el.id, lost));
            if (!ok) {
                fail = "efecto de " + el.id + " (daño " + lost + ", vida del jugador " + fp.getHealth() + ")";
            }
            pig.discard();
            com.mundosdeaxelis.gema.Gemas.quitar(sword);
            if (com.mundosdeaxelis.gema.Gemas.gemaDe(sword) != null) {
                fail = "quitar " + el.id;
            }
        }
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        // la Mesa de Gemas, paso a paso
        com.mundosdeaxelis.gema.GemTableMenu m = new com.mundosdeaxelis.gema.GemTableMenu(0, fp.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
        int e0 = com.mundosdeaxelis.table.Essence.get(fp);
        if (e0 < 200) {
            com.mundosdeaxelis.table.Essence.add(fp, 200);
            e0 = com.mundosdeaxelis.table.Essence.get(fp);
        }
        m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA).set(com.mundosdeaxelis.block.VaultLoot.emptyGem(3));
        m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_ESENCIA).set(new net.minecraft.world.item.ItemStack(
                com.mundosdeaxelis.registry.ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO).get(), 2));
        m.clickMenuButton(fp, com.mundosdeaxelis.gema.GemTableMenu.B_INFUNDIR);
        net.minecraft.world.item.ItemStack made = m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA).getItem();
        int cost = com.mundosdeaxelis.gema.GemTableMenu.infuseCost(2);
        if (!(made.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Gema g) || g.el != com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO
                || com.mundosdeaxelis.gema.Gemas.pureza(made) != 2) {
            fail = "infundir dio " + made;
        } else if (com.mundosdeaxelis.table.Essence.get(fp) != e0 - cost) {
            fail = "infundir costó " + (e0 - com.mundosdeaxelis.table.Essence.get(fp)) + " en vez de " + cost;
        } else if (m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_ESENCIA).getItem().getCount() != 1) {
            fail = "infundir no gastó una esencia";
        }
        m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_GEMA).set(made.copy());
        m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA).set(net.minecraft.world.item.ItemStack.EMPTY);
        m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_ARMA).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE));
        m.clickMenuButton(fp, com.mundosdeaxelis.gema.GemTableMenu.B_ENGARZAR);
        net.minecraft.world.item.ItemStack axe = m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_ARMA).getItem();
        if (com.mundosdeaxelis.gema.Gemas.gemaDe(axe) != com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO
                || !m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_GEMA).getItem().isEmpty()) {
            fail = "engarzar en la mesa";
        }
        int e1 = com.mundosdeaxelis.table.Essence.get(fp);
        m.clickMenuButton(fp, com.mundosdeaxelis.gema.GemTableMenu.B_QUITAR);
        net.minecraft.world.item.ItemStack back = m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_GEMA).getItem();
        if (com.mundosdeaxelis.gema.Gemas.gemaDe(axe) != null || !(back.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Gema)
                || com.mundosdeaxelis.gema.Gemas.pureza(back) != 2 || com.mundosdeaxelis.table.Essence.get(fp) != e1 - com.mundosdeaxelis.gema.GemTableMenu.REMOVE_COST) {
            fail = "quitar en la mesa (" + back + ")";
        }
        // combinar: 3 Gemas de Fuego Talladas + Esencia = 1 Pulida (y quedan las que sobran)
        for (int i = 0; i < 4; i++) {
            m.slots.get(i).set(net.minecraft.world.item.ItemStack.EMPTY);
        }
        m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA).set(com.mundosdeaxelis.gema.Gemas.conPureza(new net.minecraft.world.item.ItemStack(
                com.mundosdeaxelis.registry.ModItems.GEMAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO).get(), 4), 2));
        int e2 = com.mundosdeaxelis.table.Essence.get(fp);
        if (e2 < 200) {
            com.mundosdeaxelis.table.Essence.add(fp, 200);
            e2 = com.mundosdeaxelis.table.Essence.get(fp);
        }
        m.clickMenuButton(fp, com.mundosdeaxelis.gema.GemTableMenu.B_COMBINAR);
        net.minecraft.world.item.ItemStack left = m.slots.get(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA).getItem();
        boolean gotPolished = false;
        for (net.minecraft.world.item.ItemStack it : fp.getInventory().items) {
            if (it.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Gema && com.mundosdeaxelis.gema.Gemas.pureza(it) == 3) {
                gotPolished = true;
                it.setCount(0);
            }
        }
        if (left.getCount() != 1 || com.mundosdeaxelis.gema.Gemas.pureza(left) != 2 || !gotPolished
                || com.mundosdeaxelis.table.Essence.get(fp) != e2 - com.mundosdeaxelis.gema.GemTableMenu.combineCost(2)) {
            fail = "combinar 3 gemas (quedan " + left + ", pulida " + gotPolished + ")";
        }
        if (com.mundosdeaxelis.gema.GemTableMenu.combinable(com.mundosdeaxelis.gema.Gemas.conPureza(new net.minecraft.world.item.ItemStack(
                com.mundosdeaxelis.registry.ModItems.GEMA_VACIA.get(), 3), 4))) {
            fail = "se pueden combinar gemas Perfectas";
        }
        // etapa 277: gemas en la armadura
        net.minecraft.world.item.ItemStack chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
        net.minecraft.world.item.ItemStack boots = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS);
        net.minecraft.world.item.ItemStack helm = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
        net.minecraft.world.item.ItemStack legs = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS);
        if (!com.mundosdeaxelis.gema.Gemas.admite(chest) || com.mundosdeaxelis.gema.Gemas.esArma(chest)) {
            fail = "la armadura no admite gemas";
        }
        com.mundosdeaxelis.gema.Gemas.engarzar(chest, com.mundosdeaxelis.gema.Gemas.Elemento.VACIO, 4);
        com.mundosdeaxelis.gema.Gemas.engarzar(boots, com.mundosdeaxelis.gema.Gemas.Elemento.RAYO, 2);
        com.mundosdeaxelis.gema.Gemas.engarzar(helm, com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO, 3);
        com.mundosdeaxelis.gema.Gemas.engarzar(legs, com.mundosdeaxelis.gema.Gemas.Elemento.ESCARCHA, 2);
        double hpMod = chest.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
        boolean speed = !boots.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.FEET)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).isEmpty();
        if (hpMod < 4.0D || !speed) {
            fail = "atributos de las gemas en la armadura (vida " + hpMod + ", velocidad " + speed + ")";
        }
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helm);
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, legs);
        net.minecraftforge.event.entity.living.LivingHurtEvent fireEv = new net.minecraftforge.event.entity.living.LivingHurtEvent(fp, sl.damageSources().inFire(), 10.0F);
        com.mundosdeaxelis.gema.GemasArmadura.onHurt(fireEv);
        net.minecraft.world.entity.animal.Pig biter = net.minecraft.world.entity.EntityType.PIG.create(sl);
        biter.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 2.5);
        sl.addFreshEntity(biter);
        net.minecraftforge.event.entity.living.LivingHurtEvent meleeEv = new net.minecraftforge.event.entity.living.LivingHurtEvent(fp, sl.damageSources().mobAttack(biter), 4.0F);
        com.mundosdeaxelis.gema.GemasArmadura.onHurt(meleeEv);
        if (Math.abs(fireEv.getAmount() - 10.0F * (1.0F - 0.24F)) > 0.01F) {
            fail = "la gema de fuego en el casco no baja el daño de fuego (" + fireEv.getAmount() + ")";
        } else if (!biter.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
            fail = "la gema de escarcha en las grebas no ralentiza a quien te golpea";
        }
        biter.discard();
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
        // un pico no admite gemas
        if (com.mundosdeaxelis.gema.Gemas.admite(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE))) {
            fail = "un pico admite gemas";
        }
        for (int i = 0; i < 4; i++) {
            m.slots.get(i).set(net.minecraft.world.item.ItemStack.EMPTY);
        }
        String msg = (fail == null ? "PRUEBA_GEMAS_OK" : "PRUEBA_GEMAS_FALLO " + fail) + info;
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /**
     * Etapa 272: el Mímico sin jugador (con un jugador falso de Forge). Duerme quieto como un cofre; al intentar abrirlo despierta;
     * su botín trae una esencia del tier y una pieza buena; matándolo suelta el botín solo si el tier ya se puede saquear; y cada
     * estructura de tier 1 a 5 tiene el suyo en el plano.
     */
    private static int mimico(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        com.mundosdeaxelis.table.Essence.forgetSeals(fp);
        String fail = null;
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        fp.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 6.5);
        // 1) dormido y despertar
        com.mundosdeaxelis.entity.Mimico m = com.mundosdeaxelis.registry.ModEntities.MIMICO.get().create(sl);
        m.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
        m.setTier(1);
        sl.addFreshEntity(m);
        if (m.isAwake() || m.isPushable() || !m.canBeCollidedWith()) {
            fail = "dormido no parece un cofre";
        }
        m.interact(fp, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!m.isAwake() || m.getTarget() != fp) {
            fail = "no despierta al intentar abrirlo";
        }
        // 2) su botín
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(272L);
        int stars = 0;
        for (int t = 1; t <= 5 && fail == null; t++) {
            for (int i = 0; i < 60; i++) {
                java.util.List<net.minecraft.world.item.ItemStack> l = com.mundosdeaxelis.entity.Mimico.loot(t, r, null);
                boolean ess = false, star = false;
                for (net.minecraft.world.item.ItemStack it : l) {
                    ess |= it.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Esencia;
                    star |= com.mundosdeaxelis.block.VaultLoot.isStar(it) || it.getItem() instanceof com.mundosdeaxelis.item.KeyFragmentItem;
                    if (it.getItem() instanceof net.minecraft.world.item.PotionItem) {
                        fail = "botín del mímico con poción";
                    }
                }
                if (!ess || !star) {
                    fail = "botín del mímico (tier " + t + ") sin esencia o sin pieza buena";
                }
                stars += star ? 1 : 0;
            }
        }
        // 3) matarlo: tier 1 suelta botín; tier 3 (sellado para un jugador sin sellos) no
        int[] counts = new int[2];
        int[] tiers = {1, 3};
        for (int k = 0; k < 2 && fail == null; k++) {
            sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
            com.mundosdeaxelis.entity.Mimico v = com.mundosdeaxelis.registry.ModEntities.MIMICO.get().create(sl);
            v.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
            v.setTier(tiers[k]);
            sl.addFreshEntity(v);
            v.hurt(sl.damageSources().playerAttack(fp), 100000.0F);
            counts[k] = sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(6)).size();
            v.discard();
        }
        if (fail == null && (counts[0] < 2 || counts[1] != 0)) {
            fail = "botín al morir: tier 1 soltó " + counts[0] + ", tier 3 sellado soltó " + counts[1];
        }
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
        m.discard();
        // 4) en los planos
        for (String d : new String[]{"santuario", "bosque", "forja", "fragua", "hielo", "fortin", "volcan", "torre_arcana", "torre"}) {
            boolean has = false;
            for (com.mundosdeaxelis.world.StructBp.Mob mb : com.mundosdeaxelis.world.StructDesigns.make(d).mobs) {
                has |= mb.kind().equals("mimico") && mb.tag() != null && mb.tag().startsWith("mimico_");
            }
            if (!has) {
                fail = "sin mímico en " + d;
            }
        }
        String msg = (fail == null ? "PRUEBA_MIMICO_OK" : "PRUEBA_MIMICO_FALLO " + fail) + " botin_estrella=" + stars + " soltados=" + counts[0] + "/" + counts[1];
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /**
     * Etapa 273: los arrojadizos con un jugador falso. Cada uno revienta junto a un husk (enemigo) y una oveja (no enemigo): el
     * husk sufre su efecto y la oveja y el jugador no; lanzarlo con el objeto crea el proyectil; el gran tesoro trae los del tier.
     * Como la nube y el remolino tardan, el resultado se escribe 2 segundos después.
     */
    private static int arrojadizos(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        fp.moveTo(base.getX() + 0.5, base.getY(), base.getZ() - 6.5, 180.0F, 0.0F); // mira al norte: lo lanza lejos de las pruebas
        fp.setHealth(fp.getMaxHealth());
        java.util.List<String> fails = new java.util.concurrent.CopyOnWriteArrayList<>();
        java.util.List<Runnable> checks = new java.util.ArrayList<>();
        int i = 0;
        for (com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo t : com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo.values()) {
            double x = base.getX() + 0.5 + 12 * i++, z = base.getZ() + 0.5;
            net.minecraft.world.entity.monster.Husk husk = net.minecraft.world.entity.EntityType.HUSK.create(sl);
            net.minecraft.world.entity.animal.Sheep pig = net.minecraft.world.entity.EntityType.SHEEP.create(sl);
            husk.moveTo(x + 1.0, base.getY(), z, 0, 0);
            husk.setNoAi(true);
            pig.moveTo(x - 1.0, base.getY(), z, 0, 0);
            pig.setNoAi(true);
            sl.addFreshEntity(husk);
            sl.addFreshEntity(pig);
            net.minecraft.world.phys.Vec3 c = new net.minecraft.world.phys.Vec3(x, base.getY(), z);
            com.mundosdeaxelis.arrojadizo.Arrojadizos.burst(sl, c, t, fp, null);
            checks.add(() -> {
                boolean hit = husk.isDeadOrDying() || husk.isRemoved() || husk.getHealth() < husk.getMaxHealth();
                boolean extra = switch (t) {
                    case ESCARCHA -> husk.isDeadOrDying() || husk.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                    default -> true;
                };
                if (!hit || !extra) {
                    fails.add(t.id + " no afectó al enemigo (vida " + husk.getHealth() + ")");
                }
                if (pig.getHealth() < pig.getMaxHealth()) {
                    fails.add(t.id + " dañó a la oveja");
                }
                husk.discard();
                pig.discard();
            });
        }
        // lanzar con el objeto
        net.minecraft.world.item.ItemStack orb = new net.minecraft.world.item.ItemStack(
                com.mundosdeaxelis.registry.ModItems.ARROJADIZOS.get(com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo.ESCARCHA).get(), 3);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, orb);
        orb.getItem().use(sl, fp, net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean flying = !sl.getEntitiesOfClass(com.mundosdeaxelis.arrojadizo.ArrojadizoEntity.class, fp.getBoundingBox().inflate(4)).isEmpty();
        if (!flying || fp.getMainHandItem().getCount() != 2) {
            fails.add("lanzar con el objeto (proyectil " + flying + ", quedan " + fp.getMainHandItem().getCount() + ")");
        }
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        // el gran tesoro trae los del tier
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(273L);
        for (int st = 1; st <= 5; st++) {
            boolean has = false;
            for (net.minecraft.world.item.ItemStack it : com.mundosdeaxelis.block.VaultLoot.roll(com.mundosdeaxelis.block.VaultLoot.Kind.TREASURE, st, r)) {
                has |= it.getItem() instanceof com.mundosdeaxelis.arrojadizo.ArrojadizoItem ai && ai.tipo.tier == st;
            }
            if (!has) {
                fails.add("gran tesoro de tier " + st + " sin arrojadizos");
            }
        }
        com.mundosdeaxelis.world.Scheduler.runLater(45, () -> {
            checks.forEach(Runnable::run);
            if (fp.getHealth() < fp.getMaxHealth()) {
                fails.add("dañó a quien lo lanzó");
            }
            sl.getEntitiesOfClass(com.mundosdeaxelis.arrojadizo.ArrojadizoEntity.class, fp.getBoundingBox().inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
            String msg = fails.isEmpty() ? "PRUEBA_ARROJADIZOS_OK" : "PRUEBA_ARROJADIZOS_FALLO " + String.join("; ", fails);
            com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        });
        src.sendSuccess(() -> Component.literal("arrojadizos: resultado en 2 s"), true);
        return 1;
    }

    /** Etapa 275: los enemigos de élite (con un Brote de tier 1 y un jugador falso). */
    private static int elites(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        com.mundosdeaxelis.table.Essence.forgetSeals(fp);
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        net.minecraft.world.entity.EntityType<?> broteType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(new net.minecraft.resources.ResourceLocation(com.mundosdeaxelis.MundosDeAxelis.MODID, "brote"));
        String fail = null;
        // quién puede ser élite
        net.minecraft.world.entity.Entity sample = broteType.create(sl);
        if (com.mundosdeaxelis.world.Elites.eligibleTier(sample) != 1) {
            fail = "el Brote no puede ser élite";
        }
        net.minecraft.world.entity.Entity boss = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(new net.minecraft.resources.ResourceLocation(com.mundosdeaxelis.MundosDeAxelis.MODID, "rey_brote")).create(sl);
        if (boss != null && com.mundosdeaxelis.world.Elites.eligibleTier(boss) != 0) {
            fail = "un jefe puede ser élite";
        }
        for (com.mundosdeaxelis.world.Elites.Afijo a : com.mundosdeaxelis.world.Elites.Afijo.values()) {
            net.minecraft.world.entity.Mob m = (net.minecraft.world.entity.Mob) broteType.create(sl);
            m.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
            m.setNoAi(true);
            float hp0 = m.getMaxHealth();
            com.mundosdeaxelis.world.Elites.make(m, 1, a);
            sl.addFreshEntity(m);
            if (Math.abs(m.getMaxHealth() - hp0 * 2.5F) > 0.05F || m.getCustomName() == null || com.mundosdeaxelis.world.Elites.afijo(m) != a) {
                fail = "élite " + a.id + " (vida " + hp0 + " -> " + m.getMaxHealth() + ")";
            }
            net.minecraft.world.entity.animal.Sheep v = net.minecraft.world.entity.EntityType.SHEEP.create(sl);
            v.moveTo(base.getX() + 1.5, base.getY(), base.getZ() + 0.5, 0, 0);
            v.setNoAi(true);
            sl.addFreshEntity(v);
            m.setHealth(m.getMaxHealth() * 0.5F);
            float h1 = m.getHealth();
            v.hurt(sl.damageSources().mobAttack(m), 2.0F);
            boolean ok = switch (a) {
                case ARDIENTE -> v.isOnFire();
                case GELIDO -> v.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                case VAMPIRICO -> m.getHealth() > h1;
                case BLINDADO -> m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) >= 8.0D
                        && m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) >= 0.7D;
                case VELOZ -> m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        > m.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.3D;
                case EXPLOSIVO -> true;
            };
            if (!ok) {
                fail = "poder " + a.id;
            }
            v.discard();
            if (a == com.mundosdeaxelis.world.Elites.Afijo.ARDIENTE) {
                // matarlo: suelta su botín de tier 1
                sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(5))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                m.hurt(sl.damageSources().playerAttack(fp), 100000.0F);
                int n = sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(5),
                        ie -> ie.getItem().getItem() instanceof com.mundosdeaxelis.item.EssenceRuneItem).size();
                if (n < 1) {
                    fail = "el élite no suelta su runa";
                }
                sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(5))
                        .forEach(net.minecraft.world.entity.Entity::discard);
            }
            m.discard();
        }
        // cuántos salen
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(275L);
        int elites = 0;
        for (int i = 0; i < 400; i++) {
            net.minecraft.world.entity.Mob m = (net.minecraft.world.entity.Mob) broteType.create(sl);
            com.mundosdeaxelis.world.Elites.maybe(m, r);
            elites += com.mundosdeaxelis.world.Elites.isElite(m) ? 1 : 0;
        }
        if (elites < 20 || elites > 75) {
            fail = "salen " + elites + " élites de 400";
        }
        String msg = (fail == null ? "PRUEBA_ELITES_OK" : "PRUEBA_ELITES_FALLO " + fail) + " elites=" + elites + "/400";
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /** Etapa 276: el Saqueador del Tesoro con un jugador falso: al golpearlo huye con su cuenta atrás, escapa, y muerto suelta su saco. */
    private static int saqueador(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        com.mundosdeaxelis.table.Essence.forgetSeals(fp);
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        String fail = null;
        com.mundosdeaxelis.entity.Saqueador s = com.mundosdeaxelis.registry.ModEntities.SAQUEADOR.get().create(sl);
        s.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        s.setTier(2);
        sl.addFreshEntity(s);
        if (Math.abs(s.getMaxHealth() - com.mundosdeaxelis.world.DungeonBalance.hp(2, 8.0F)) > 0.01F || s.spotted()) {
            fail = "vida o estado inicial";
        }
        s.hurt(sl.damageSources().playerAttack(fp), 1.0F);
        if (!s.spotted() || s.escapeIn() != com.mundosdeaxelis.entity.Saqueador.ESCAPE_TICKS) {
            fail = "al golpearlo no huye con su cuenta atrás";
        }
        s.escape();
        if (!s.isRemoved()) {
            fail = "no escapa";
        }
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(276L);
        for (int t = 1; t <= 5; t++) {
            int stacks = com.mundosdeaxelis.entity.Saqueador.loot(t, r).size();
            if (stacks < 6) {
                fail = "saco de tier " + t + " con " + stacks + " cosas";
            }
        }
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
        com.mundosdeaxelis.entity.Saqueador k = com.mundosdeaxelis.registry.ModEntities.SAQUEADOR.get().create(sl);
        k.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        k.setTier(1);
        sl.addFreshEntity(k);
        k.hurt(sl.damageSources().playerAttack(fp), 100000.0F);
        int n = sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(8)).size();
        if (n < 6) {
            fail = "muerto suelta " + n + " cosas";
        }
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
        k.discard();
        String msg = (fail == null ? "PRUEBA_SAQUEADOR_OK" : "PRUEBA_SAQUEADOR_FALLO " + fail) + " soltados=" + n;
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /** Etapa 278: el Altar Maldito con un jugador falso: despierta, 3 oleadas (la última con un élite), botín, descanso y sellos. */
    private static int altar(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        com.mundosdeaxelis.table.Essence.forgetSeals(fp);
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(src.getPosition());
        fp.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 8.5);
        String fail = null;
        sl.setBlock(pos, com.mundosdeaxelis.registry.ModBlocks.ALTAR_MALDITO.get().defaultBlockState(), 3);
        com.mundosdeaxelis.block.AltarMalditoBlockEntity be = (com.mundosdeaxelis.block.AltarMalditoBlockEntity) sl.getBlockEntity(pos);
        be.setTier(1);
        java.util.List<net.minecraft.server.level.ServerPlayer> near = java.util.List.of(fp);
        int e0 = com.mundosdeaxelis.table.Essence.get(fp);
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
        be.activate(fp);
        if (be.state() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.RUNNING) {
            fail = "no despierta";
        }
        boolean elite = false;
        int[] sizes = new int[3];
        for (int w = 1; w <= 3 && fail == null; w++) {
            for (int t = 0; t < 120 && be.wave() < w; t++) {
                be.tick(sl, near);
            }
            sizes[w - 1] = be.mobs().size();
            if (be.wave() != w || be.mobs().size() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.waveCount(w, 1)) {
                fail = "oleada " + w + " (ola " + be.wave() + ", " + be.mobs().size() + " enemigos)";
                break;
            }
            for (java.util.UUID id : new java.util.ArrayList<>(be.mobs())) {
                net.minecraft.world.entity.Entity e = sl.getEntity(id);
                if (w == 3 && e != null && com.mundosdeaxelis.world.Elites.isElite(e)) {
                    elite = true;
                }
                if (e != null) {
                    e.discard();
                }
            }
            be.tick(sl, near);
        }
        if (fail == null && !elite) {
            fail = "la última oleada no trae élite";
        }
        be.tick(sl, near);
        int items = sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(6)).size();
        if (fail == null && (be.state() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.RESTING
                || sl.getBlockState(pos).getValue(com.mundosdeaxelis.block.AltarMalditoBlock.CARGADO) || items < 3
                || com.mundosdeaxelis.table.Essence.get(fp) <= e0)) {
            fail = "al vencer (estado " + be.state() + ", " + items + " objetos, esencia " + e0 + " -> " + com.mundosdeaxelis.table.Essence.get(fp) + ")";
        }
        be.activate(fp);
        if (fail == null && be.state() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.RESTING) {
            fail = "despierta mientras descansa";
        }
        be.recharge();
        if (fail == null && (be.state() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.IDLE || !sl.getBlockState(pos).getValue(com.mundosdeaxelis.block.AltarMalditoBlock.CARGADO))) {
            fail = "no se recarga";
        }
        be.setTier(3);
        be.activate(fp);
        if (fail == null && be.state() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.IDLE) {
            fail = "un altar sellado despierta";
        }
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
        sl.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        for (String d : new String[]{"santuario", "bosque", "forja", "fragua", "hielo", "fortin", "volcan", "torre_arcana", "torre"}) {
            com.mundosdeaxelis.world.StructBp b = com.mundosdeaxelis.world.StructDesigns.make(d);
            int[] a = com.mundosdeaxelis.world.StructKit.altars(d)[0];
            String st = b.m.get(com.mundosdeaxelis.world.StructBp.key(a[0], a[1], a[2]));
            if (st == null || !st.contains("altar_maldito")) {
                fail = "sin altar en " + d + " (" + st + ")";
            }
        }
        // barras pegadas: una barra de jefe se quita sola a quien está lejos aunque el jefe no se mueva
        net.minecraft.world.entity.animal.Pig owner = net.minecraft.world.entity.EntityType.PIG.create(sl);
        owner.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sl.addFreshEntity(owner);
        net.minecraft.server.level.ServerBossEvent bar = new net.minecraft.server.level.ServerBossEvent(Component.literal("prueba"),
                net.minecraft.world.BossEvent.BossBarColor.RED, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        com.mundosdeaxelis.world.BarrasLejanas.track(bar, owner);
        fp.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 10.5);
        bar.addPlayer(fp);
        com.mundosdeaxelis.world.BarrasLejanas.sweep();
        boolean keptNear = bar.getPlayers().contains(fp);
        fp.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 80.5);
        com.mundosdeaxelis.world.BarrasLejanas.sweep();
        boolean goneFar = !bar.getPlayers().contains(fp);
        owner.discard();
        com.mundosdeaxelis.world.BarrasLejanas.sweep();
        if (fail == null && (!keptNear || !goneFar)) {
            fail = "barra de jefe (cerca " + keptNear + ", lejos quitada " + goneFar + ")";
        }
        String msg = (fail == null ? "PRUEBA_ALTAR_OK" : "PRUEBA_ALTAR_FALLO " + fail) + " oleadas=" + java.util.Arrays.toString(sizes) + " botin=" + items;
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /** Etapa 279: la protección contra la mala suerte y el brillo de las piezas estrella en el suelo. */
    private static int suerte(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        String fail = null;
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(279L);
        int added = 0, at = -1;
        for (int i = 1; i <= com.mundosdeaxelis.block.VaultLoot.PITY; i++) {
            java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
            items.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD));
            if (com.mundosdeaxelis.block.VaultLoot.pity(data, items, 2, r)) {
                added++;
                at = i;
                boolean star = false;
                for (net.minecraft.world.item.ItemStack it : items) {
                    star |= com.mundosdeaxelis.block.VaultLoot.isStar(it) || it.getItem() instanceof com.mundosdeaxelis.item.KeyFragmentItem;
                }
                if (!star) {
                    fail = "la protección no añadió una pieza buena";
                }
            }
        }
        if (fail == null && (added != 1 || at != com.mundosdeaxelis.block.VaultLoot.PITY)) {
            fail = "protección contra la mala suerte (añadió " + added + " en el cofre " + at + ")";
        }
        java.util.List<net.minecraft.world.item.ItemStack> lucky = new java.util.ArrayList<>();
        lucky.add(new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.HEART_FRAGMENT.get()));
        data.putInt(com.mundosdeaxelis.block.VaultLoot.PITY_KEY, 5);
        com.mundosdeaxelis.block.VaultLoot.pity(data, lucky, 2, r);
        if (fail == null && data.getInt(com.mundosdeaxelis.block.VaultLoot.PITY_KEY) != 0) {
            fail = "una pieza estrella no reinicia la cuenta";
        }
        net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(src.getPosition());
        net.minecraft.world.entity.item.ItemEntity heart = new net.minecraft.world.entity.item.ItemEntity(sl, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.HEART_FRAGMENT.get()));
        net.minecraft.world.entity.item.ItemEntity dirt = new net.minecraft.world.entity.item.ItemEntity(sl, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT));
        sl.addFreshEntity(heart);
        sl.addFreshEntity(dirt);
        if (fail == null && (!heart.hasGlowingTag() || dirt.hasGlowingTag())) {
            fail = "brillo en el suelo (corazón " + heart.hasGlowingTag() + ", tierra " + dirt.hasGlowingTag() + ")";
        }
        heart.discard();
        dirt.discard();
        String msg = fail == null ? "PRUEBA_SUERTE_OK" : "PRUEBA_SUERTE_FALLO " + fail;
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    /** Etapa 280: los encargos del Tablón con un jugador falso. */
    // ------------------------------------------------------------------ etapa 281: armas nuevas

    /** Muñeco de prueba de las armas: un vindicador quieto (enemigo, sin armadura y al que le afecta el veneno). */
    private static net.minecraft.world.entity.monster.Vindicator muñeco(net.minecraft.server.level.ServerLevel sl, double x, double y, double z, float yaw) {
        net.minecraft.world.entity.monster.Vindicator h = net.minecraft.world.entity.EntityType.VINDICATOR.create(sl);
        h.moveTo(x, y, z, yaw, 0.0F);
        h.setYBodyRot(yaw);
        h.setYHeadRot(yaw);
        h.setNoAi(true);
        h.setPersistenceRequired();
        h.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(150.0);
        h.setHealth(150.0F);
        h.addTag("prueba_armas");
        sl.addFreshEntity(h);
        h.setYBodyRot(yaw);
        return h;
    }

    private static int armas(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        java.util.List<String> fails = new java.util.concurrent.CopyOnWriteArrayList<>();
        sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Vindicator.class, new net.minecraft.world.phys.AABB(base).inflate(80),
                e -> e.getTags().contains("prueba_armas")).forEach(net.minecraft.world.entity.Entity::discard);
        // 1) las 40 armas: daño, velocidad, alcance, barrido y gemas
        for (com.mundosdeaxelis.arma.TierArma t : com.mundosdeaxelis.arma.TierArma.values()) {
            for (com.mundosdeaxelis.arma.TipoArma tp : com.mundosdeaxelis.arma.TipoArma.values()) {
                net.minecraft.world.item.Item it = com.mundosdeaxelis.registry.ModItems.arma(tp, t);
                if (!(it instanceof com.mundosdeaxelis.arma.ArmaItem a)) {
                    fails.add("no existe " + com.mundosdeaxelis.arma.ArmaItem.id(tp, t));
                    continue;
                }
                float want = Math.round(t.danoBase() * tp.mult * 2.0F) / 2.0F;
                net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(a);
                double dmg = 1.0, spd = 4.0, reach = 0.0;
                for (java.util.Map.Entry<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> m
                        : st.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).entries()) {
                    if (m.getKey() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) {
                        dmg += m.getValue().getAmount();
                    } else if (m.getKey() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED) {
                        spd += m.getValue().getAmount();
                    } else if (m.getKey() == net.minecraftforge.common.ForgeMod.ENTITY_REACH.get()) {
                        reach += m.getValue().getAmount();
                    }
                }
                if (Math.abs(dmg - want) > 0.01 || Math.abs(a.dano() - want) > 0.01 || Math.abs(spd - tp.velocidad) > 0.01 || Math.abs(reach - tp.alcance) > 0.01) {
                    fails.add(a + " daño " + dmg + "/" + want + " velocidad " + spd + " alcance " + reach);
                }
                if (tp == com.mundosdeaxelis.arma.TipoArma.ESPADA && Math.abs(a.dano() - com.mundosdeaxelis.gear.GearStats.sword(2 * t.nivel())) > 0.01) {
                    fails.add("la espada " + t.id + " no es la de referencia (LoadoutRef)");
                }
                if (a.canPerformAction(st, net.minecraftforge.common.ToolActions.SWORD_SWEEP) != (tp == com.mundosdeaxelis.arma.TipoArma.ESPADA)) {
                    fails.add("barrido de " + a);
                }
                if (!com.mundosdeaxelis.gema.Gemas.admite(st)) {
                    fails.add("no admite gemas " + a);
                }
            }
        }
        // 2) las cajas dan las armas de su tier y ya no las espadas viejas
        for (com.mundosdeaxelis.arma.TierArma t : com.mundosdeaxelis.arma.TierArma.values()) {
            java.util.Set<net.minecraft.world.item.Item> en = new java.util.HashSet<>();
            com.mundosdeaxelis.crate.CratePools.pool(t.caja).values().forEach(l -> l.forEach(pz -> en.add(pz.item())));
            for (com.mundosdeaxelis.arma.TipoArma tp : com.mundosdeaxelis.arma.TipoArma.values()) {
                if (!en.contains(com.mundosdeaxelis.registry.ModItems.arma(tp, t))) {
                    fails.add("la caja " + t.caja.id + " no da " + tp.id);
                }
            }
            if (en.stream().anyMatch(x -> x instanceof com.mundosdeaxelis.gear.LadderSword)) {
                fails.add("la caja " + t.caja.id + " aún da espadas viejas");
            }
            if (!com.mundosdeaxelis.crate.CratePools.verify(t.caja).isEmpty()) {
                fails.add("orden de la caja " + t.caja.id + ": " + com.mundosdeaxelis.crate.CratePools.verify(t.caja));
            }
        }
        // 3) el estilo de cada tipo (tier Naturaleza: su veneno no cambia el daño)
        com.mundosdeaxelis.arma.TierArma B = com.mundosdeaxelis.arma.TierArma.BOSQUE;
        java.util.function.Function<com.mundosdeaxelis.arma.TipoArma, com.mundosdeaxelis.arma.ArmaItem> A =
                tp -> (com.mundosdeaxelis.arma.ArmaItem) com.mundosdeaxelis.registry.ModItems.arma(tp, B);
        double x0 = base.getX() + 0.5, y0 = base.getY(), z0 = base.getZ() + 0.5;
        fp.moveTo(x0, y0, z0 - 3.0, 0.0F, 0.0F); // mira al sur (+Z)
        fp.setHealth(fp.getMaxHealth());
        net.minecraft.world.entity.monster.Vindicator h1 = muñeco(sl, x0, y0, z0, 180.0F); // mira al jugador
        net.minecraft.world.entity.monster.Vindicator h2 = muñeco(sl, x0 + 1.2, y0, z0, 180.0F); // al lado
        net.minecraft.world.entity.monster.Vindicator h3 = muñeco(sl, x0, y0, z0 + 1.6, 180.0F); // detrás (en la línea del golpe)
        java.util.function.BiConsumer<String, Boolean> ok = (what, cond) -> {
            if (!cond) {
                fails.add(what);
            }
        };
        com.mundosdeaxelis.arma.Armas.cargarContra(fp);
        float r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.ESPADA), fp, sl, h1, 10.0F, false, false);
        ok.accept("contraataque " + r, Math.abs(r - 13.5F) < 0.01F);
        float hp2 = h2.getHealth();
        com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.ESPADA), fp, sl, h1, 10.0F, true, false);
        ok.accept("barrido de la espada (" + hp2 + " -> " + h2.getHealth() + ")", h2.getHealth() < hp2 - 2.0F);
        ok.accept("veneno de Naturaleza", h1.hasEffect(net.minecraft.world.effect.MobEffects.POISON));
        net.minecraft.world.entity.monster.Husk zombi = net.minecraft.world.entity.EntityType.HUSK.create(sl);
        zombi.moveTo(x0 - 6, y0, z0, 180.0F, 0.0F); // mira al jugador: sin puñalada
        zombi.setNoAi(true);
        zombi.addTag("prueba_armas");
        sl.addFreshEntity(zombi);
        zombi.setYBodyRot(180.0F);
        r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.DAGAS), fp, sl, zombi, 10.0F, false, false);
        ok.accept("el veneno sobre un no-muerto pega +10 % (" + r + ")", Math.abs(r - 11.0F) < 0.01F && !zombi.hasEffect(net.minecraft.world.effect.MobEffects.POISON));
        zombi.discard();
        h1.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(10.0);
        r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.HACHA), fp, sl, h1, 10.0F, false, false);
        ok.accept("quiebra armaduras " + r + " (armadura " + h1.getArmorValue() + ")", Math.abs(r - 10.0F * (1.0F + h1.getArmorValue() * 0.025F)) < 0.01F && r > 12.0F);
        h1.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0.0);
        hp2 = h2.getHealth();
        r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.MARTILLO), fp, sl, h1, 10.0F, true, false);
        ok.accept("martillo aturde", h1.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN));
        ok.accept("onda del martillo", h2.getHealth() < hp2 - 1.5F);
        r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.LANZA), fp, sl, h1, 10.0F, false, true);
        ok.accept("carga de la lanza " + r, Math.abs(r - 13.0F) < 0.01F);
        float hp3 = h3.getHealth();
        com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.LANZA), fp, sl, h1, 10.0F, true, false);
        ok.accept("la lanza atraviesa (" + hp3 + " -> " + h3.getHealth() + ")", h3.getHealth() < hp3 - 2.0F);
        r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.DAGAS), fp, sl, h1, 10.0F, false, false);
        ok.accept("dagas de frente " + r, Math.abs(r - 10.0F) < 0.01F);
        h1.setYBodyRot(0.0F); // ahora da la espalda al jugador
        h1.setYRot(0.0F);
        r = com.mundosdeaxelis.arma.Armas.alGolpear(A.apply(com.mundosdeaxelis.arma.TipoArma.DAGAS), fp, sl, h1, 10.0F, false, false);
        ok.accept("puñalada por la espalda " + r, Math.abs(r - 17.5F) < 0.01F);
        // 4) elementos de otros tiers
        com.mundosdeaxelis.arma.Armas.alGolpear((com.mundosdeaxelis.arma.ArmaItem) com.mundosdeaxelis.registry.ModItems.arma(com.mundosdeaxelis.arma.TipoArma.DAGAS,
                com.mundosdeaxelis.arma.TierArma.MAGMA), fp, sl, h2, 10.0F, false, false);
        ok.accept("fuego (Fuego)", h2.isOnFire());
        com.mundosdeaxelis.arma.Armas.alGolpear((com.mundosdeaxelis.arma.ArmaItem) com.mundosdeaxelis.registry.ModItems.arma(com.mundosdeaxelis.arma.TipoArma.DAGAS,
                com.mundosdeaxelis.arma.TierArma.HIELO), fp, sl, h3, 10.0F, false, false);
        ok.accept("escarcha (Hielo)", h3.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN));
        com.mundosdeaxelis.arma.Armas.alGolpear((com.mundosdeaxelis.arma.ArmaItem) com.mundosdeaxelis.registry.ModItems.arma(com.mundosdeaxelis.arma.TipoArma.DAGAS,
                com.mundosdeaxelis.arma.TierArma.ABISMO), fp, sl, h3, 10.0F, false, false);
        ok.accept("vacío (Abismo)", h3.hasEffect(net.minecraft.world.effect.MobEffects.WITHER));
        fp.setHealth(10.0F);
        com.mundosdeaxelis.arma.Armas.alGolpear((com.mundosdeaxelis.arma.ArmaItem) com.mundosdeaxelis.registry.ModItems.arma(com.mundosdeaxelis.arma.TipoArma.DAGAS,
                com.mundosdeaxelis.arma.TierArma.DRAGON), fp, sl, h3, 30.0F, false, false);
        ok.accept("vampiro (Dragón) " + fp.getHealth(), fp.getHealth() > 10.5F);
        fp.setHealth(fp.getMaxHealth());
        net.minecraft.world.entity.monster.Vindicator hSangre = muñeco(sl, x0 - 10, y0, z0, 180.0F);
        float hpSangre = hSangre.getHealth();
        com.mundosdeaxelis.arma.Armas.alGolpear((com.mundosdeaxelis.arma.ArmaItem) com.mundosdeaxelis.registry.ModItems.arma(com.mundosdeaxelis.arma.TipoArma.DAGAS,
                com.mundosdeaxelis.arma.TierArma.FORJA), fp, sl, hSangre, 20.0F, false, false);
        // 5) almas: matar da almas; la especial las gasta y sin almas no sale
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(
                com.mundosdeaxelis.registry.ModItems.arma(com.mundosdeaxelis.arma.TipoArma.ESPADA, com.mundosdeaxelis.arma.TierArma.MAGIA)));
        com.mundosdeaxelis.gear.Souls.set(fp, 0);
        net.minecraft.world.entity.monster.Vindicator victima = muñeco(sl, x0 + 6, y0, z0, 0.0F);
        victima.hurt(sl.damageSources().playerAttack(fp), 100000.0F);
        int almas = com.mundosdeaxelis.gear.Souls.get(fp);
        ok.accept("matar con el arma da almas (" + almas + ")", almas > 0);
        com.mundosdeaxelis.arma.ArmaItem espada = A.apply(com.mundosdeaxelis.arma.TipoArma.ESPADA);
        com.mundosdeaxelis.gear.Souls.set(fp, 3);
        fp.getCooldowns().removeCooldown(espada);
        ok.accept("la especial sale sin almas", !com.mundosdeaxelis.arma.Armas.tryEspecial(espada, fp, sl, new net.minecraft.world.item.ItemStack(espada)));
        com.mundosdeaxelis.gear.Souls.set(fp, 40);
        boolean usa = com.mundosdeaxelis.arma.Armas.tryEspecial(espada, fp, sl, new net.minecraft.world.item.ItemStack(espada));
        ok.accept("la especial no gasta sus almas (" + com.mundosdeaxelis.gear.Souls.get(fp) + ")", usa && com.mundosdeaxelis.gear.Souls.get(fp) == 40 - com.mundosdeaxelis.arma.TipoArma.ESPADA.almas);
        ok.accept("la especial no se enfría", fp.getCooldowns().isOnCooldown(espada));
        ok.accept("se repite antes de tiempo", !com.mundosdeaxelis.arma.Armas.tryEspecial(espada, fp, sl, new net.minecraft.world.item.ItemStack(espada)));
        // 6) especiales: cada una en su carril, una detrás de otra (el muñeco del Tajo queda delante del jugador)
        net.minecraft.world.entity.monster.Vindicator tajo = muñeco(sl, x0, y0, z0 + 4.5, 180.0F);
        float hpTajo = tajo.getHealth();
        java.util.Map<String, Float> antes = new java.util.HashMap<>();
        java.util.List<net.minecraft.world.entity.monster.Vindicator> lanes = new java.util.ArrayList<>();
        com.mundosdeaxelis.arma.TipoArma[] orden = {com.mundosdeaxelis.arma.TipoArma.HACHA, com.mundosdeaxelis.arma.TipoArma.MARTILLO,
                com.mundosdeaxelis.arma.TipoArma.LANZA, com.mundosdeaxelis.arma.TipoArma.DAGAS};
        int[] cuando = {12, 50, 100, 115};
        for (int i = 0; i < orden.length; i++) {
            final int k = i;
            double lx = x0 + 12 * (i + 1);
            double dz = orden[i] == com.mundosdeaxelis.arma.TipoArma.LANZA ? 1.6 : orden[i] == com.mundosdeaxelis.arma.TipoArma.DAGAS ? 4.0 : 2.0;
            net.minecraft.world.entity.monster.Vindicator h = muñeco(sl, lx, y0, z0 - 3.0 + dz, 180.0F);
            lanes.add(h);
            antes.put(orden[i].id, h.getHealth());
            com.mundosdeaxelis.world.Scheduler.runLater(cuando[i], () -> {
                fp.moveTo(lx, y0, z0 - 3.0, 0.0F, 8.0F);
                if (!com.mundosdeaxelis.arma.Armas.especial(A.apply(orden[k]), fp, sl)) {
                    fails.add("no sale la especial " + orden[k].id);
                }
            });
        }
        boolean[] aturdido = {false};
        for (int tt = 54; tt <= 100; tt += 3) {
            com.mundosdeaxelis.world.Scheduler.runLater(tt, () -> {
                if (lanes.get(1).hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
                    aturdido[0] = true;
                }
            });
        }
        com.mundosdeaxelis.world.Scheduler.runLater(160, () -> {
            if (tajo.getHealth() >= hpTajo - 2.0F) {
                fails.add("el Tajo creciente no hiere (" + tajo.getHealth() + ")");
            }
            if (hSangre.getHealth() > hpSangre - 5.0F || hSangre.getHealth() < hpSangre - 7.0F) {
                fails.add("el sangrado no quita 3 × 10 % (" + hpSangre + " -> " + hSangre.getHealth() + ")");
            }
            for (int i = 0; i < orden.length; i++) {
                net.minecraft.world.entity.monster.Vindicator h = lanes.get(i);
                float lost = antes.get(orden[i].id) - h.getHealth();
                float need = orden[i] == com.mundosdeaxelis.arma.TipoArma.HACHA ? 12.0F : 4.0F;
                if (lost < need) {
                    fails.add("la especial " + orden[i].id + " quita poco (" + lost + ")");
                }
            }
            if (!aturdido[0]) {
                fails.add("el Terremoto no aturde");
            }
            if (!com.mundosdeaxelis.arma.Armas.sombra(fp)) {
                fails.add("el Paso sombrío no da la sombra");
            }
            sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Vindicator.class, new net.minecraft.world.phys.AABB(base).inflate(80),
                    e -> e.getTags().contains("prueba_armas")).forEach(net.minecraft.world.entity.Entity::discard);
            sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(80)).forEach(net.minecraft.world.entity.Entity::discard);
            String msg = fails.isEmpty() ? "PRUEBA_ARMAS_OK 40 armas" : "PRUEBA_ARMAS_FALLO " + fails;
            com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
            sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        });
        src.sendSuccess(() -> Component.literal("Prueba de armas en marcha (8 s)..."), true);
        return 1;
    }

    private static int encargos(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(sl);
        com.mundosdeaxelis.table.Essence.forgetSeals(fp);
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(src.getPosition());
        String fail = null;
        com.mundosdeaxelis.encargo.Encargos.ageOneDay(fp);
        java.util.List<com.mundosdeaxelis.encargo.Encargos.Encargo> a = com.mundosdeaxelis.encargo.Encargos.today(fp);
        java.util.List<com.mundosdeaxelis.encargo.Encargos.Encargo> b = com.mundosdeaxelis.encargo.Encargos.today(fp);
        java.util.Set<com.mundosdeaxelis.encargo.Encargos.Tipo> kinds = new java.util.HashSet<>();
        a.forEach(e -> kinds.add(e.tipo()));
        if (a.size() != 3 || kinds.size() != 3 || !a.equals(b)) {
            fail = "los 3 encargos del día (" + a + ")";
        }
        // cumplir y recoger
        int e0 = com.mundosdeaxelis.table.Essence.get(fp);
        for (com.mundosdeaxelis.encargo.Encargos.Encargo e : a) {
            com.mundosdeaxelis.encargo.Encargos.progress(fp, e.tipo(), e.need());
        }
        boolean c0 = com.mundosdeaxelis.encargo.Encargos.claim(fp, 3);
        for (int i = 0; i < 3; i++) {
            if (!com.mundosdeaxelis.encargo.Encargos.claim(fp, i)) {
                fail = "no se puede recoger el encargo " + i;
            }
        }
        boolean again = com.mundosdeaxelis.encargo.Encargos.claim(fp, 0);
        boolean bonus = com.mundosdeaxelis.encargo.Encargos.claim(fp, 3);
        boolean bonusAgain = com.mundosdeaxelis.encargo.Encargos.claim(fp, 3);
        if (fail == null && (c0 || again || !bonus || bonusAgain)) {
            fail = "recoger (gran recompensa antes " + c0 + ", repetir " + again + ", gran " + bonus + ", gran otra vez " + bonusAgain + ")";
        }
        if (fail == null && com.mundosdeaxelis.table.Essence.get(fp) <= e0) {
            fail = "recoger no da Esencia";
        }
        // un día nuevo trae encargos nuevos sin recoger
        com.mundosdeaxelis.encargo.Encargos.ageOneDay(fp);
        if (fail == null && com.mundosdeaxelis.encargo.Encargos.today(fp).stream().anyMatch(com.mundosdeaxelis.encargo.Encargos.Encargo::claimed)) {
            fail = "el día nuevo no trae encargos nuevos";
        }
        // las muertes cuentan solas
        com.mundosdeaxelis.encargo.Encargos.force(fp, new com.mundosdeaxelis.encargo.Encargos.Tipo[]{com.mundosdeaxelis.encargo.Encargos.Tipo.ENEMIGOS,
                com.mundosdeaxelis.encargo.Encargos.Tipo.ELITES, com.mundosdeaxelis.encargo.Encargos.Tipo.MIMICO}, new int[]{0, 0, 0});
        net.minecraft.world.entity.EntityType<?> broteType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(new net.minecraft.resources.ResourceLocation(com.mundosdeaxelis.MundosDeAxelis.MODID, "brote"));
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.entity.Mob m = (net.minecraft.world.entity.Mob) broteType.create(sl);
            m.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
            if (i == 1) {
                com.mundosdeaxelis.world.Elites.make(m, 1, com.mundosdeaxelis.world.Elites.Afijo.VELOZ);
            }
            sl.addFreshEntity(m);
            m.hurt(sl.damageSources().playerAttack(fp), 100000.0F);
        }
        com.mundosdeaxelis.entity.Mimico mi = com.mundosdeaxelis.registry.ModEntities.MIMICO.get().create(sl);
        mi.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        mi.setTier(1);
        sl.addFreshEntity(mi);
        mi.hurt(sl.damageSources().playerAttack(fp), 100000.0F);
        java.util.List<com.mundosdeaxelis.encargo.Encargos.Encargo> k = com.mundosdeaxelis.encargo.Encargos.today(fp);
        if (fail == null && (k.get(0).progress() != 2 || k.get(1).progress() != 1 || k.get(2).progress() != 1)) {
            fail = "las muertes no cuentan (" + k + ")";
        }
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(base).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
        String msg = (fail == null ? "PRUEBA_ENCARGOS_OK" : "PRUEBA_ENCARGOS_FALLO " + fail) + " " + a;
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return fail == null ? 1 : 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("prueba")
                .then(Commands.literal("equipo")
                        .then(Commands.argument("set", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(setIds(), b))
                                .executes(ctx -> equipo(ctx.getSource(), StringArgumentType.getString(ctx, "set")))))
                .then(Commands.literal("capas").executes(ctx -> capas(ctx.getSource())))
                .then(Commands.literal("botin").executes(ctx -> botin(ctx.getSource())))
                .then(Commands.literal("gemas").executes(ctx -> gemas(ctx.getSource())))
                .then(Commands.literal("mimico").executes(ctx -> mimico(ctx.getSource())))
                .then(Commands.literal("arrojadizos").executes(ctx -> arrojadizos(ctx.getSource())))
                .then(Commands.literal("elites").executes(ctx -> elites(ctx.getSource())))
                .then(Commands.literal("saqueador").executes(ctx -> saqueador(ctx.getSource())))
                .then(Commands.literal("altar").executes(ctx -> altar(ctx.getSource())))
                .then(Commands.literal("suerte").executes(ctx -> suerte(ctx.getSource())))
                .then(Commands.literal("encargos").executes(ctx -> encargos(ctx.getSource())))
                .then(Commands.literal("armas").executes(ctx -> armas(ctx.getSource())))
                .then(Commands.literal("encargos_listos").executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    com.mundosdeaxelis.encargo.Encargos.force(sp, new com.mundosdeaxelis.encargo.Encargos.Tipo[]{
                            com.mundosdeaxelis.encargo.Encargos.Tipo.ELITES, com.mundosdeaxelis.encargo.Encargos.Tipo.COFRES,
                            com.mundosdeaxelis.encargo.Encargos.Tipo.ALTAR}, new int[]{-1, 2, 0});
                    ctx.getSource().sendSuccess(() -> Component.literal("Encargos de prueba puestos."), false);
                    return 1;
                }))
                .then(Commands.literal("olvidar_progreso").executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    // los sellos que lleva encima se volverían a apuntar en el Archivo: se le quitan
                    for (net.minecraft.world.item.ItemStack it : sp.getInventory().items) {
                        if (it.getItem() instanceof com.mundosdeaxelis.item.SealItem) {
                            it.setCount(0);
                        }
                    }
                    int n = com.mundosdeaxelis.table.Essence.forgetSeals(sp);
                    ctx.getSource().sendSuccess(() -> Component.literal("Sellos olvidados: " + n + " (vuelves a la etapa 0)."), false);
                    return 1;
                }))
                .then(Commands.literal("llaves")
                        .executes(ctx -> llaves(ctx.getSource(), 5))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> llaves(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cantidad")))))
                .then(Commands.literal("cajas").executes(ctx -> cajas(ctx.getSource())))
                .then(Commands.literal("esencia")
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100000))
                                .executes(ctx -> esencia(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cantidad")))))
                .then(Commands.literal("mesa")
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> mesa(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel")))))
                .then(Commands.literal("materiales_jefe").executes(ctx -> materialesJefe(ctx.getSource())))
                .then(Commands.literal("niveles").executes(ctx -> niveles(ctx.getSource())))
                .then(Commands.literal("ficha")
                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("modo", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("entrada_oculto", "entrada_visible", "antesala"), b))
                                        .executes(ctx -> ficha(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa"),
                                                StringArgumentType.getString(ctx, "modo"))))))
                .then(Commands.literal("resultado")
                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 2))
                                .executes(ctx -> resultado(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa"), true))
                                .then(Commands.literal("poco")
                                        .executes(ctx -> resultado(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa"), false)))))
                .then(Commands.literal("congelar")
                        .executes(ctx -> congelar(ctx.getSource(), com.mundosdeaxelis.world.Frozen.DEFAULT_HITS))
                        .then(Commands.argument("saltos", IntegerArgumentType.integer(1, 60))
                                .executes(ctx -> congelar(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "saltos")))))
                .then(Commands.literal("frio")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    com.mundosdeaxelis.world.Cold.set(sp, IntegerArgumentType.getInteger(ctx, "valor"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Medidor de frío puesto."), false);
                                    return 1;
                                })))
                .then(Commands.literal("zona_fria")
                        .then(Commands.argument("ritmo", IntegerArgumentType.integer(1, 60))
                                .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> {
                                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                            int secs = IntegerArgumentType.getInteger(ctx, "segundos");
                                            com.mundosdeaxelis.world.Cold.zone(sp, IntegerArgumentType.getInteger(ctx, "ritmo"), secs * 20);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Estás en una zona helada durante " + secs
                                                    + " s (ponte junto a una fogata para calentarte)."), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("aviso")
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            com.mundosdeaxelis.world.Titles.hint(sp, "Puente de losas", "En cada fila hay una losa falsa: elige bien");
                            com.mundosdeaxelis.world.Titles.hint(sp, "Sala despejada", "Sala del Capataz");
                            com.mundosdeaxelis.world.Titles.hint(sp, "Prueba de runas", "Mira la secuencia y repítela");
                            com.mundosdeaxelis.world.Titles.actionBar(sp, "Encendiendo la linterna... 40%  (no se separen)");
                            com.mundosdeaxelis.world.Titles.lootEssence(sp, 15);
                            com.mundosdeaxelis.world.Titles.loot(sp, ModItems.SANCTUARY_BERRY.get(), 1);
                            com.mundosdeaxelis.world.Titles.loot(sp, ModItems.EMBER_STEEL.get(), 2);
                            com.mundosdeaxelis.world.Titles.actionBarLong(sp, "Caíste. Vuelves al inicio del tramo: inténtalo otra vez.", 4);
                            ctx.getSource().sendSuccess(() -> Component.literal("Avisos de prueba enviados (arriba a la derecha)."), false);
                            return 1;
                        }))
                .then(Commands.literal("llave_bonus")
                        .executes(ctx -> llaveBonus(ctx.getSource(), CrateKind.FORGE))
                        .then(Commands.argument("caja", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(crateIds(), b))
                                .executes(ctx -> llaveBonus(ctx.getSource(), CrateKind.byId(StringArgumentType.getString(ctx, "caja"))))))
                .then(Commands.literal("sello")
                        .then(Commands.argument("caja", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(crateIds(), b))
                                .executes(ctx -> sello(ctx.getSource(), StringArgumentType.getString(ctx, "caja"), 1))
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> sello(ctx.getSource(), StringArgumentType.getString(ctx, "caja"),
                                                IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                .then(Commands.literal("progreso")
                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> progreso(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa")))))
                .then(Commands.literal("escudo").executes(ctx -> escudo(ctx.getSource())))
                .then(Commands.literal("jefe").executes(ctx -> jefe(ctx.getSource())))
                .then(Commands.literal("arena").executes(ctx -> arena(ctx.getSource())))
                .then(Commands.literal("saltardungeon").executes(ctx -> saltarDungeon(ctx.getSource())))
                .then(Commands.literal("jefe2").executes(ctx -> jefe2(ctx.getSource())))
                .then(Commands.literal("intro").executes(ctx -> intro(ctx.getSource(), com.mundosdeaxelis.world.BossIntro.Kind.FOREST)))
                .then(Commands.literal("intro3").executes(ctx -> introHielo(ctx.getSource())))
                .then(Commands.literal("intro2").executes(ctx -> intro(ctx.getSource(), com.mundosdeaxelis.world.BossIntro.Kind.FORGE)))
                .then(Commands.literal("arena2").executes(ctx -> arena2(ctx.getSource())))
                .then(Commands.literal("jefe3").executes(ctx -> jefe3(ctx.getSource())))
                .then(Commands.literal("entrar3").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    com.mundosdeaxelis.world.BossTravel.enter(ctx.getSource().getServer(), java.util.List.of(p), 3);
                    ctx.getSource().sendSuccess(() -> Component.literal("Entrando a la sala del Lobo Ártico (dimensión de la etapa 3)."), false);
                    return 1;
                }))
                .then(Commands.literal("arena3").executes(ctx -> arena3(ctx.getSource())))
                .then(Commands.literal("entrar4").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    com.mundosdeaxelis.world.BossTravel.enter(ctx.getSource().getServer(), java.util.List.of(p), 4);
                    ctx.getSource().sendSuccess(() -> Component.literal("Entrando al corazón del volcán (dimensión de la etapa 4)."), false);
                    return 1;
                }))
                .then(Commands.literal("entrar5").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    com.mundosdeaxelis.world.BossTravel.enter(ctx.getSource().getServer(), java.util.List.of(p), 5);
                    ctx.getSource().sendSuccess(() -> Component.literal("Entrando a la cima de la Torre del Archimago (dimensión de la etapa 5)."), false);
                    return 1;
                }))
                .then(Commands.literal("construir")
                        .then(Commands.argument("etapa", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 5))
                                .executes(ctx -> construir(ctx.getSource(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "etapa")))))
                .then(Commands.literal("jefe5").executes(ctx -> jefe4(ctx.getSource())))
                .then(Commands.literal("arena5").executes(ctx -> arena4(ctx.getSource())))
                .then(Commands.literal("limpiar5").executes(ctx -> limpiar4(ctx.getSource())))
                .then(Commands.literal("limpiar3").executes(ctx -> limpiar3(ctx.getSource())))
                .then(Commands.literal("saltardungeon2").executes(ctx -> saltarDungeon2(ctx.getSource())))
                .then(Commands.literal("plano")
                        .executes(ctx -> plano(ctx.getSource(), new java.util.Random().nextLong(), 1))
                        .then(Commands.argument("jugadores", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> plano(ctx.getSource(), new java.util.Random().nextLong(), IntegerArgumentType.getInteger(ctx, "jugadores")))
                                .then(Commands.argument("semilla", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                                        .executes(ctx -> plano(ctx.getSource(), com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx, "semilla"),
                                                IntegerArgumentType.getInteger(ctx, "jugadores"))))))
                .then(Commands.literal("salir").executes(ctx -> salir(ctx.getSource())))
                .then(Commands.literal("balance").executes(ctx -> balance(ctx.getSource())))
                .then(Commands.literal("dificultad")
                        .executes(ctx -> dificultad(ctx.getSource(), -1))
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(0, 8))
                                .executes(ctx -> dificultad(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel")))))
                .then(Commands.literal("simular")
                        .then(Commands.argument("caja", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(crateIds(), b))
                                .executes(ctx -> simular(ctx.getSource(), StringArgumentType.getString(ctx, "caja"), 1000))
                                .then(Commands.argument("n", IntegerArgumentType.integer(10, 100000))
                                        .executes(ctx -> simular(ctx.getSource(), StringArgumentType.getString(ctx, "caja"),
                                                IntegerArgumentType.getInteger(ctx, "n"))))));
    }

    /** Hace aparecer al Rey Brote donde estás (para probar el combate sin ir a la arena). */
    private static int jefe(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.ReyBrote b = com.mundosdeaxelis.registry.ModEntities.REY_BROTE.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        b.moveTo(sp.getX() + 6, sp.getY(), sp.getZ(), 0.0F, 0.0F);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Rey Brote aparecido (vida " + (int) b.getMaxHealth() + ")."), false);
        return 1;
    }

    /**
     * Prueba la intro épica del jefe (hay que estar en su arena, cerca del centro; quita al jefe que haya para que salga de nuevo).
     */
    private static int intro(CommandSourceStack src, com.mundosdeaxelis.world.BossIntro.Kind kind) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        net.minecraft.world.phys.AABB all = new net.minecraft.world.phys.AABB(-60, 0, -60, 60, 200, 60);
        for (com.mundosdeaxelis.entity.ReyBrote b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.ReyBrote.class, all)) {
            b.discard();
        }
        for (com.mundosdeaxelis.entity.GranHerrero b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.GranHerrero.class, all)) {
            b.discard();
        }
        com.mundosdeaxelis.world.BossIntro.play(level, new java.util.ArrayList<>(level.players()), kind);
        src.sendSuccess(() -> Component.literal("Intro del jefe en marcha (usa la tecla de saltar cinemática para cortarla)."), false);
        return 1;
    }

    /** Prueba la intro del Lobo Ártico (hay que estar en su arena de hielo; quita al lobo que haya). */
    private static int introHielo(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        com.mundosdeaxelis.entity.LoboArtico old = com.mundosdeaxelis.world.IceIntro.find(level);
        if (old != null) {
            old.discard();
        }
        java.util.List<ServerPlayer> ps = new java.util.ArrayList<>(level.players());
        if (!com.mundosdeaxelis.world.IceIntro.canPlay(ps)) {
            src.sendFailure(Component.literal("Ya hay una cinemática en marcha."));
            return 0;
        }
        com.mundosdeaxelis.world.IceIntro.play(level, ps, () -> {
            com.mundosdeaxelis.entity.LoboArtico b = com.mundosdeaxelis.world.IceIntro.find(level);
            if (b != null) {
                b.setNoAi(false);
                b.setInvulnerable(false);
            }
        });
        src.sendSuccess(() -> Component.literal("Intro del Lobo Ártico en marcha (tecla de saltar para cortarla)."), false);
        return 1;
    }

    /** Hace aparecer al Gran Herrero de Brasas (etapa 2) donde estás. */
    private static int jefe2(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.GranHerrero b = com.mundosdeaxelis.registry.ModEntities.GRAN_HERRERO.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        b.moveTo(sp.getX() + 8, sp.getY(), sp.getZ(), 0.0F, 0.0F);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Gran Herrero aparecido (vida " + (int) b.getMaxHealth() + ")."), false);
        return 1;
    }

    // ------------------------------------------------------------------ Archimago (etapa 4)

    private static net.minecraft.world.phys.Vec3 arena4Center;

    /** Hace aparecer al Archimago (etapa 4). Dentro de la arena de prueba (/prueba arena4) aparece en su centro. */
    private static int jefe4(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.Archimago b = com.mundosdeaxelis.registry.ModEntities.ARCHIMAGO.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        boolean inArena = arena4Center != null && sp.serverLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && Math.hypot(sp.getX() - arena4Center.x, sp.getZ() - arena4Center.z) < 24;
        net.minecraft.world.phys.Vec3 at = inArena ? arena4Center : sp.position().add(8, 0, 0);
        b.moveTo(at.x, at.y, at.z, 90.0F, 0.0F);
        b.setHome(at);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Archimago aparecido (vida " + (int) b.getMaxHealth() + "). Fases al 65 % y 30 %."), false);
        return 1;
    }

    /**
     * Arena de prueba para el Archimago: un disco de 20 bloques de radio flotando en el cielo (sobre donde estás), con suelo
     * oscuro y anillos de amatista, un muro bajo y linternas en el borde. Te lleva cerca del borde. Se quita con /prueba limpiar4.
     */
    /**
     * Construye la arena (y la dungeon, en las etapas 1 y 2) en la primera copia de esa etapa, sin jugadores; sirve también desde la
     * consola del servidor (pruebas automáticas). Al terminar escribe en el registro PRUEBA_CONSTRUIR_n_OK con el tiempo que tardó.
     */
    /**
     * Prueba del escudo de lava de los cultistas: se lo pone a una salamandra viva del mundo, le pega dos veces 5 de daño y comprueba
     * que el primer golpe no le quita vida (el escudo se queda con 3), y que el segundo rompe el escudo y solo pasan 2.
     */
    private static int escudo(CommandSourceStack src) {
        net.minecraft.server.level.ServerLevel sl = src.getLevel();
        java.util.List<? extends com.mundosdeaxelis.entity.SalamandraLava> all = sl.getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(com.mundosdeaxelis.entity.SalamandraLava.class),
                net.minecraft.world.entity.Entity::isAlive);
        if (all.isEmpty()) {
            com.mundosdeaxelis.MundosDeAxelis.LOGGER.info("PRUEBA_ESCUDO_SIN_SALAMANDRA");
            src.sendFailure(Component.literal("No hay ninguna salamandra de lava"));
            return 0;
        }
        com.mundosdeaxelis.entity.SalamandraLava s = all.get(0);
        s.setHealth(s.getMaxHealth());
        com.mundosdeaxelis.entity.EscudoLava.give(sl, s);
        float h0 = s.getHealth();
        s.invulnerableTime = 0;
        s.hurt(sl.damageSources().generic(), 5.0F);
        float h1 = s.getHealth(), left1 = com.mundosdeaxelis.entity.EscudoLava.left(s);
        s.invulnerableTime = 0;
        s.hurt(sl.damageSources().generic(), 5.0F);
        float h2 = s.getHealth();
        boolean has2 = com.mundosdeaxelis.entity.EscudoLava.has(s);
        boolean ok = h1 == h0 && Math.abs(left1 - 3.0F) < 0.01F && !has2 && h2 < h0 && h2 > h0 - 2.01F;
        String msg = (ok ? "PRUEBA_ESCUDO_OK" : "PRUEBA_ESCUDO_FALLO") + String.format(java.util.Locale.ROOT,
                " vida %.1f -> %.1f -> %.1f, escudo tras el 1er golpe %.1f, sigue tras el 2.o %s", h0, h1, h2, left1, has2);
        com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
        return ok ? 1 : 0;
    }

    private static final int[] BUILDS = new int[6];

    private static int construir(CommandSourceStack src, int stage) {
        net.minecraft.server.MinecraftServer server = src.getServer();
        java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> copies = com.mundosdeaxelis.world.ModDimensions.copiesOf(stage);
        net.minecraft.server.level.ServerLevel level = copies.isEmpty() ? null : server.getLevel(copies.get(0));
        if (level == null) {
            src.sendFailure(Component.literal("No existe la dimensión de la etapa " + stage));
            return 0;
        }
        final long t0 = System.currentTimeMillis();
        final int vez = ++BUILDS[stage];
        Runnable ok = () -> {
            boolean built = switch (stage) {
                case 1 -> com.mundosdeaxelis.world.ForestArena.isBuilt(level);
                case 2 -> com.mundosdeaxelis.world.ForgeArena.isBuilt(level);
                case 3 -> com.mundosdeaxelis.world.IceArena.isBuilt(level);
                case 4 -> com.mundosdeaxelis.world.FireArena.isBuilt(level);
                default -> com.mundosdeaxelis.world.TowerArena.isBuilt(level);
            };
            String msg = (built ? "PRUEBA_CONSTRUIR_" + stage + "_OK" : "PRUEBA_CONSTRUIR_" + stage + "_SIN_MARCA")
                    + " vez " + vez + " en " + String.format(java.util.Locale.ROOT, "%.1f", (System.currentTimeMillis() - t0) / 1000.0) + " s";
            com.mundosdeaxelis.MundosDeAxelis.LOGGER.info(msg);
            src.sendSuccess(() -> Component.literal(msg), true);
        };
        long seed = 12345L + stage;
        switch (stage) {
            case 1 -> com.mundosdeaxelis.world.ForestArena.ensureBuilt(level, () -> {
                com.mundosdeaxelis.world.DungeonLayout lay = com.mundosdeaxelis.world.DungeonLayout.generate(seed, 2);
                com.mundosdeaxelis.world.ForestDungeon.build(level, lay, ok);
            });
            case 2 -> com.mundosdeaxelis.world.ForgeArena.ensureBuilt(level, () -> {
                com.mundosdeaxelis.world.DungeonLayout lay = com.mundosdeaxelis.world.DungeonLayout.generate(seed, 2, true);
                com.mundosdeaxelis.world.ForgeDungeon.build(level, lay, ok);
            });
            case 3 -> com.mundosdeaxelis.world.IceArena.ensureBuilt(level, ok);
            case 4 -> com.mundosdeaxelis.world.FireArena.ensureBuilt(level, ok);
            default -> com.mundosdeaxelis.world.TowerArena.ensureBuilt(level, ok);
        }
        src.sendSuccess(() -> Component.literal("Construyendo la etapa " + stage + "..."), true);
        return 1;
    }

    private static int arena4(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            src.sendFailure(Component.literal("La arena de prueba se construye en el Overworld."));
            return 0;
        }
        int cx = net.minecraft.util.Mth.floor(sp.getX());
        int cz = net.minecraft.util.Mth.floor(sp.getZ());
        int fy = 236;
        int radius = 20;
        net.minecraft.world.level.block.state.BlockState floor = net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState tiles = net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState amethyst = net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState wall = net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState light = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 1.5) {
                    continue;
                }
                net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(cx + dx, fy, cz + dz);
                if (d > radius) {
                    // muro bajo con una linterna cada tanto
                    double ang = Math.atan2(dz, dx);
                    boolean lamp = Math.abs(Math.sin(ang * 8.0)) > 0.985;
                    level.setBlock(p, wall, 2);
                    level.setBlock(p.above(), wall, 2);
                    level.setBlock(p.above(2), lamp ? light : wall, 2);
                    continue;
                }
                // anillos de amatista a 6, 11 y 16 bloques y una cruz de baldosas: el dibujo rúnico del suelo
                boolean ring = Math.abs(d - 6.0) < 0.5 || Math.abs(d - 11.0) < 0.5 || Math.abs(d - 16.0) < 0.5;
                boolean cross = (Math.abs(dx) <= 0 || Math.abs(dz) <= 0 || Math.abs(dx) == Math.abs(dz)) && d > 2.0;
                level.setBlock(p, ring ? amethyst : cross ? tiles : floor, 2);
                level.setBlock(p.below(), floor, 2);
                for (int dy = 1; dy <= 12; dy++) {
                    level.setBlock(p.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(new net.minecraft.core.BlockPos(cx, fy, cz), light, 2);
        arena4Center = new net.minecraft.world.phys.Vec3(cx + 0.5, fy + 1, cz + 0.5);
        sp.teleportTo(level, cx + 0.5, fy + 1, cz + 14.5, 180.0F, 0.0F);
        src.sendSuccess(() -> Component.literal("Arena del Archimago lista (disco de 20 bloques en el cielo). Ahora usa /prueba jefe4. Para quitarla: /prueba limpiar4."), false);
        return 1;
    }

    /** Quita la arena de prueba del Archimago y al jefe. */
    private static int limpiar4(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(sp.getX() - 120, level.getMinBuildHeight(), sp.getZ() - 120,
                sp.getX() + 120, level.getMaxBuildHeight(), sp.getZ() + 120);
        for (com.mundosdeaxelis.entity.Archimago b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.Archimago.class, box)) {
            b.clearFight(level);
            b.discard();
        }
        if (arena4Center != null && level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            int cx = net.minecraft.util.Mth.floor(arena4Center.x);
            int cz = net.minecraft.util.Mth.floor(arena4Center.z);
            int fy = net.minecraft.util.Mth.floor(arena4Center.y) - 1;
            net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int dx = -23; dx <= 23; dx++) {
                for (int dz = -23; dz <= 23; dz++) {
                    for (int dy = -1; dy <= 3; dy++) {
                        level.setBlock(new net.minecraft.core.BlockPos(cx + dx, fy + dy, cz + dz), air, 2);
                    }
                }
            }
            arena4Center = null;
        }
        src.sendSuccess(() -> Component.literal("Arena del Archimago quitada."), false);
        return 1;
    }

    // ------------------------------------------------------------------ Lobo Ártico (etapa 3)

    private static net.minecraft.world.phys.Vec3 arena3Center;

    /** Hace aparecer al Lobo Ártico (etapa 3) frente a ti. Mejor dentro de la arena de prueba (/prueba arena3). */
    private static int jefe3(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.LoboArtico b = com.mundosdeaxelis.registry.ModEntities.LOBO_ARTICO.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        net.minecraft.world.phys.Vec3 home = arena3Center != null && sp.serverLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && Math.hypot(sp.getX() - arena3Center.x, sp.getZ() - arena3Center.z) < 26 ? arena3Center : sp.position();
        b.moveTo(sp.getX() + 10, sp.getY(), sp.getZ(), 90.0F, 0.0F);
        b.setHome(home);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Lobo Ártico aparecido (vida " + (int) b.getMaxHealth() + "). Fases al 65 % y 30 %."), false);
        return 1;
    }

    /**
     * Arena de prueba para el Lobo Ártico: un disco de nieve de 24 bloques de radio flotando en el cielo (sobre donde estás), con un
     * muro de hielo alrededor y sin braseros (la ventisca crea sus propias zonas de calor). Te lleva al centro.
     * Se quita con /prueba limpiar3.
     */
    private static int arena3(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            src.sendFailure(Component.literal("La arena de prueba se construye en el Overworld."));
            return 0;
        }
        int cx = net.minecraft.util.Mth.floor(sp.getX());
        int cz = net.minecraft.util.Mth.floor(sp.getZ());
        int fy = 236;
        int radius = 24;
        net.minecraft.world.level.block.state.BlockState snow = net.minecraft.world.level.block.Blocks.SNOW_BLOCK.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState packed = net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState lantern = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > radius + 1.5) {
                    continue;
                }
                net.minecraft.core.BlockPos fp = new net.minecraft.core.BlockPos(cx + dx, fy, cz + dz);
                boolean ring = d > radius - 1.5 || ((int) d % 6 == 5);
                level.setBlock(fp, ring ? packed : snow, 2);
                level.setBlock(fp.below(), packed, 2);
                level.setBlock(fp.below(2), packed, 2);
                if (d > radius - 0.5) {
                    for (int h = 1; h <= 12; h++) {
                        level.setBlock(fp.above(h), h % 4 == 0 && ((dx + dz) & 3) == 0 ? lantern : packed, 2);
                    }
                }
            }
        }
        arena3Center = new net.minecraft.world.phys.Vec3(cx + 0.5, fy + 1.0, cz + 0.5);
        sp.teleportTo(level, cx + 0.5, fy + 1.0, cz - 12.5, 0.0F, 0.0F);
        src.sendSuccess(() -> Component.literal("Arena de hielo lista (disco de 24 bloques en el cielo, sin braseros: la ventisca crea zonas de calor). Ahora usa /prueba jefe3. Para quitarla: /prueba limpiar3."), false);
        return 1;
    }

    /** Quita la arena de prueba del Lobo Ártico y a sus criaturas. */
    private static int limpiar3(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(sp.getX() - 120, level.getMinBuildHeight(), sp.getZ() - 120,
                sp.getX() + 120, level.getMaxBuildHeight(), sp.getZ() + 120);
        for (com.mundosdeaxelis.entity.LoboArtico b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.LoboArtico.class, box)) {
            b.clearFight(level);
            b.discard();
        }
        for (com.mundosdeaxelis.entity.LoboEscarcha b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.LoboEscarcha.class, box)) {
            b.discard();
        }
        if (arena3Center != null && level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            int cx = net.minecraft.util.Mth.floor(arena3Center.x);
            int cz = net.minecraft.util.Mth.floor(arena3Center.z);
            int fy = net.minecraft.util.Mth.floor(arena3Center.y) - 1;
            net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int dx = -27; dx <= 27; dx++) {
                for (int dz = -27; dz <= 27; dz++) {
                    for (int dy = -3; dy <= 14; dy++) {
                        level.setBlock(new net.minecraft.core.BlockPos(cx + dx, fy + dy, cz + dz), air, 2);
                    }
                }
            }
            arena3Center = null;
        }
        src.sendSuccess(() -> Component.literal("Arena del Lobo Ártico quitada."), false);
        return 1;
    }

    private static int arena2(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 2);
        return 1;
    }

    /** Entra a la Forja y salta las salas (puertas abiertas hasta el jefe). */
    private static int saltarDungeon2(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 2);
        waitAndSkip2(src.getServer(), 0);
        return 1;
    }

    /** La copia de la dungeon donde hay jugadores ahora (o la primera, si no hay nadie). */
    private static net.minecraft.server.level.ServerLevel copyInUse(net.minecraft.server.MinecraftServer server,
                                                                   java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> keys) {
        for (var k : keys) {
            net.minecraft.server.level.ServerLevel l = server.getLevel(k);
            if (l != null && !l.players().isEmpty()) {
                return l;
            }
        }
        return server.getLevel(keys.get(0));
    }

    private static void waitAndSkip2(net.minecraft.server.MinecraftServer server, int tries) {
        com.mundosdeaxelis.world.Scheduler.runLater(40, () -> {
            net.minecraft.server.level.ServerLevel l = copyInUse(server, com.mundosdeaxelis.world.ModDimensions.FORGE_COPIES);
            if (l == null) {
                return;
            }
            if (!com.mundosdeaxelis.world.ForgeManager.of(l).hasLayout() || com.mundosdeaxelis.world.ForgeDungeon.isBuilding(l) || l.players().isEmpty()) {
                if (tries < 60) {
                    waitAndSkip2(server, tries + 1);
                }
                return;
            }
            com.mundosdeaxelis.world.ForgeManager.of(l).skipAll(l);
            int k = 0;
            for (ServerPlayer q : l.players()) {
                net.minecraft.world.phys.Vec3 v = com.mundosdeaxelis.world.ForgeManager.of(l).entryPoint(l, k++);
                q.teleportTo(l, v.x, v.y, v.z, 180.0F, 0.0F);
            }
        });
    }

    private static int arena(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 1);
        return 1;
    }

    /** Entra y salta las salas: puertas abiertas hasta el jefe (para probar solo la pelea). Espera a que se construya la dungeon. */
    private static int saltarDungeon(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 1);
        waitAndSkip(src.getServer(), 0);
        return 1;
    }

    private static void waitAndSkip(net.minecraft.server.MinecraftServer server, int tries) {
        com.mundosdeaxelis.world.Scheduler.runLater(40, () -> {
            net.minecraft.server.level.ServerLevel l = copyInUse(server, com.mundosdeaxelis.world.ModDimensions.FOREST_COPIES);
            if (l == null) {
                return;
            }
            if (!com.mundosdeaxelis.world.DungeonManager.of(l).hasLayout() || com.mundosdeaxelis.world.ForestDungeon.isBuilding(l) || l.players().isEmpty()) {
                if (tries < 60) {
                    waitAndSkip(server, tries + 1);
                }
                return;
            }
            com.mundosdeaxelis.world.DungeonManager.of(l).skipAll(l);
            int k = 0;
            for (ServerPlayer q : l.players()) {
                net.minecraft.world.phys.Vec3 v = com.mundosdeaxelis.world.DungeonManager.of(l).entryPoint(l, k++);
                q.teleportTo(l, v.x, v.y, v.z, 180.0F, 0.0F);
            }
        });
    }

    /** Muestra en el chat el plano de una dungeon (sin construirla): /… plano [jugadores] [semilla]. */
    private static int plano(CommandSourceStack src, long seed, int players) {
        com.mundosdeaxelis.world.DungeonLayout lay = com.mundosdeaxelis.world.DungeonLayout.generate(seed, players);
        src.sendSuccess(() -> Component.literal(lay.summary()), false);
        src.sendSuccess(() -> Component.literal(lay.ascii()), false);
        src.sendSuccess(() -> Component.literal("E=entrada M=mini-jefe A=antesala L=llave C=cripta P=saltos R=runas 1-5=salas de combate"), false);
        return 1;
    }

    private static int salir(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        com.mundosdeaxelis.world.BossTravel.leave(src.getPlayerOrException());
        return 1;
    }

    private static int balance(CommandSourceStack src) {
        for (int i = 1; i <= 8; i++) {
            final int st = i;
            src.sendSuccess(() -> Component.literal(com.mundosdeaxelis.world.BossBalance.describe(st)), false);
        }
        return 1;
    }

    private static int sello(CommandSourceStack src, String caja, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS && k.id.equals(caja)) {
                give(sp, ModItems.SEALS.get(k).get(), n);
                src.sendSuccess(() -> Component.literal("Sello de " + k.id + " x" + n + " entregado."), false);
                return 1;
            }
        }
        src.sendFailure(Component.literal("Caja desconocida: " + caja));
        return 0;
    }

    private static int progreso(CommandSourceStack src, int etapa) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        boolean nuevo = com.mundosdeaxelis.table.Essence.registerProgress(sp, etapa);
        src.sendSuccess(() -> Component.literal(nuevo ? "Progreso registrado hasta la etapa " + etapa + "." : "Ese progreso ya estaba registrado."), false);
        return 1;
    }

    private static int dificultad(CommandSourceStack src, int nivel) {
        com.mundosdeaxelis.world.WorldTier t = com.mundosdeaxelis.world.WorldTier.get(src.getServer());
        if (nivel >= 0) {
            t.set(nivel);
        }
        int lv = t.level();
        src.sendSuccess(() -> Component.literal("Dificultad del mundo: nivel " + lv + " (monstruos +" + Math.round(com.mundosdeaxelis.world.WorldTierEvents.HP_PER_LEVEL * lv * 100)
                + "% de vida, +" + Math.round(com.mundosdeaxelis.world.WorldTierEvents.DMG_PER_LEVEL * lv * 100) + "% de daño). Los monstruos que ya existen no cambian; los nuevos sí."), false);
        return 1;
    }

    private static void give(ServerPlayer sp, Item it, int n) {
        if (it == null || it == Items.AIR) {
            return;
        }
        ItemStack st = new ItemStack(it, n);
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
    }

    private static Item byName(String id) {
        ResourceLocation rl = new ResourceLocation(MundosDeAxelis.MODID, id);
        return ForgeRegistries.ITEMS.containsKey(rl) ? ForgeRegistries.ITEMS.getValue(rl) : null;
    }

    private static int equipo(CommandSourceStack src, String which) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        int n = 0;
        for (GearSet s : GearSet.values()) {
            if (!which.equals("todos") && !s.id.equals(which)) {
                continue;
            }
            for (ArmorDefs.Piece p : ArmorDefs.Piece.values()) {
                give(sp, byName(ArmorDefs.id(s, p)), 1);
                n++;
            }
            for (GearDef d : GearDefs.of(s)) {
                give(sp, byName(d.id()), 1);
                n++;
            }
        }
        if (n == 0) {
            src.sendFailure(Component.literal("Set desconocido. Usa: " + String.join(", ", setIds())));
            return 0;
        }
        final int total = n;
        src.sendSuccess(() -> Component.literal("Te di " + total + " piezas de equipo (" + which + ")."), false);
        return n;
    }

    private static int capas(CommandSourceStack src) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        int n = 0;
        for (com.mundosdeaxelis.cape.CapeDefs.Def d : com.mundosdeaxelis.cape.CapeDefs.ALL) {
            give(sp, byName(d.id()), 1);
            n++;
        }
        final int total = n;
        src.sendSuccess(() -> Component.literal("Te di las " + total + " capas. Póntelas en la ranura Capa del menú de equipo (G) o con clic derecho."), false);
        return n;
    }

    private static int llaves(CommandSourceStack src, int n) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS) {
                give(sp, ModItems.KEYS.get(k).get(), n);
            }
        }
        src.sendSuccess(() -> Component.literal("Te di " + n + " llaves de cada caja."), false);
        return 1;
    }

    private static int cajas(CommandSourceStack src) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS) {
                give(sp, ModItems.CRATE_ITEMS.get(k).get(), 1);
            }
        }
        src.sendSuccess(() -> Component.literal("Te di las ocho cajas de colección."), false);
        return 1;
    }

    /** (Prueba) Pone tu nivel de Mesa del Artífice (1 a 8). */
    private static int mesa(CommandSourceStack src, int lv) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        Essence.setLevel(sp, lv);
        com.mundosdeaxelis.relic.Equipment.sync(sp);
        Essence.sync(sp, Component.literal("Nivel de mesa: " + lv), true);
        src.sendSuccess(() -> Component.literal("Tu Mesa del Artífice ahora es de nivel " + lv + "."), false);
        return 1;
    }

    /** (Prueba) Abre la ficha del jefe sin jugar la dungeon: entrada con el jefe oculto o visible, o la ficha de la antesala. */
    private static int ficha(CommandSourceStack src, int stage, String modo) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        java.util.List<ServerPlayer> group = java.util.List.of(sp);
        com.mundosdeaxelis.network.BossBriefPacket pk;
        switch (modo) {
            case "entrada_oculto" -> pk = com.mundosdeaxelis.world.BossInfo.brief(stage, sp.blockPosition(), group, sp).withKnown(false);
            case "entrada_visible" -> pk = com.mundosdeaxelis.world.BossInfo.brief(stage, sp.blockPosition(), group, sp).withKnown(true);
            case "antesala" -> pk = com.mundosdeaxelis.world.BossInfo.dossier(stage, group, sp);
            default -> {
                src.sendFailure(Component.literal("Modos: entrada_oculto, entrada_visible, antesala."));
                return 0;
            }
        }
        com.mundosdeaxelis.network.ModNetwork.sendTo(sp, pk);
        return 1;
    }

    /** (Prueba) Muestra la pantalla de resultados con premios de ejemplo (sin dar nada). */
    private static int resultado(CommandSourceStack src, int stage, boolean full) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        java.util.List<com.mundosdeaxelis.network.BossResultPacket.Got> got = new ArrayList<>();
        java.util.List<com.mundosdeaxelis.network.BossResultPacket.Miss> missed = new ArrayList<>();
        if (full) {
            int k = 0;
            for (com.mundosdeaxelis.network.BossBriefPacket.Reward r : com.mundosdeaxelis.world.BossRewards.preview(stage)) {
                // de ejemplo: lo seguro «sale», y de los extras sale uno sí y otro no
                if (r.tag() == 0 || k++ % 2 == 0) {
                    got.add(new com.mundosdeaxelis.network.BossResultPacket.Got(r.item(), r.item().isEmpty() ? 150 : 1, r.tag() == 0));
                } else {
                    missed.add(new com.mundosdeaxelis.network.BossResultPacket.Miss(r.item(), r.item().isEmpty() ? 1 : 0, 10 + 7 * k));
                }
            }
        }
        String name = stage == 2 ? "Gran Herrero de Brasas" : "Rey Brote";
        com.mundosdeaxelis.network.ModNetwork.sendTo(sp, new com.mundosdeaxelis.network.BossResultPacket(stage, name, full,
                full ? 62 : 3, full ? 84 : 0, got, stage, stage * 20, true, missed));
        return 1;
    }

    /** (Prueba) Muestra la revelación de la llave bonus (cartel, sonido y efectos) frente a ti; no entrega la llave. */
    private static int llaveBonus(CommandSourceStack src, CrateKind kind) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        if (kind == null || kind == CrateKind.PETS) {
            src.sendFailure(Component.literal("Caja no válida."));
            return 0;
        }
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(sp.position().add(sp.getLookAngle().multiply(3, 0, 3)));
        com.mundosdeaxelis.block.CrateBlockEntity.bonusKeyFx(sp.serverLevel(), sp, pos, kind);
        return 1;
    }

    /** (Prueba) Te da 3 de cada material de jefe. */
    private static int materialesJefe(CommandSourceStack src) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        for (com.mundosdeaxelis.table.Mats m : com.mundosdeaxelis.table.Mats.values()) {
            if (m.isBoss()) {
                ItemStack st = new ItemStack(com.mundosdeaxelis.registry.ModItems.MATS.get(m).get(), 3);
                if (!sp.getInventory().add(st)) {
                    sp.drop(st, false);
                }
            }
        }
        src.sendSuccess(() -> Component.literal("Te di 3 de cada material de jefe."), false);
        return 1;
    }

    private static int esencia(CommandSourceStack src, int n) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        Essence.add(sp, n);
        Essence.sync(sp);
        src.sendSuccess(() -> Component.literal("Te di " + n + " de Esencia."), false);
        return 1;
    }

    private static int niveles(CommandSourceStack src) {
        for (CrateKind k : CrateKind.values()) {
            if (k == CrateKind.PETS) {
                continue;
            }
            StringBuilder sb = new StringBuilder(k.id).append(": ");
            int[] lv = k.levels();
            int[] pc = k.levelPct();
            for (int i = 0; i < lv.length; i++) {
                sb.append("nivel ").append(lv[i]).append(" ").append(pc[i]).append("%").append(i < lv.length - 1 ? " · " : "");
            }
            final String line = sb.toString();
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static String kindOf(Item it) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(it);
        if (rl == null) {
            return "otro";
        }
        String p = rl.getPath();
        if (!rl.getNamespace().equals(MundosDeAxelis.MODID)) {
            return "Minecraft";
        }
        if (p.contains("_armor_")) {
            return "armadura";
        }
        if (p.startsWith("comp_")) {
            return "mascota";
        }
        if (p.startsWith("key_")) {
            return "llave";
        }
        if (p.startsWith("decor_")) {
            return "decoración";
        }
        if (p.startsWith("cape_")) {
            return "capa";
        }
        if (p.startsWith("mat_")) {
            return "material";
        }
        if (GearDefs.get(p) != null || p.startsWith("gear_") || com.mundosdeaxelis.arma.Armas.esArma(p)) {
            return "herramienta/arma";
        }
        return "reliquia";
    }

    private static int simular(CommandSourceStack src, String crate, int n) {
        CrateKind k = CrateKind.byId(crate);
        if (k == null || k == CrateKind.PETS) {
            src.sendFailure(Component.literal("Caja desconocida. Usa: " + String.join(", ", crateIds())));
            return 0;
        }
        RandomSource rs = RandomSource.create();
        Map<Tier, Integer> byTier = new EnumMap<>(Tier.class);
        Map<String, Integer> byKind = new HashMap<>();
        Map<Item, Integer> byItem = new HashMap<>();
        for (int i = 0; i < n; i++) {
            CratePools.Result r = CratePools.draw(k, rs, null);
            byTier.merge(r.tier(), 1, Integer::sum);
            byKind.merge(kindOf(r.item()), 1, Integer::sum);
            byItem.merge(r.item(), 1, Integer::sum);
        }
        src.sendSuccess(() -> Component.literal("=== " + n + " aperturas de la caja " + k.id + " ==="), false);
        List<String> bad = CratePools.verify(k);
        src.sendSuccess(() -> Component.literal(bad.isEmpty() ? "Orden justo (lo mejor sale menos): OK"
                : "Orden justo: FALLA -> " + bad.get(0) + " (+" + (bad.size() - 1) + ")"), false);
        StringBuilder t = new StringBuilder("Rareza: ");
        for (Tier tier : Tier.values()) {
            int c = byTier.getOrDefault(tier, 0);
            if (c > 0) {
                t.append(tier.id).append(" ").append(String.format("%.1f", 100.0 * c / n)).append("% · ");
            }
        }
        final String tl = t.toString();
        src.sendSuccess(() -> Component.literal(tl), false);
        List<Map.Entry<String, Integer>> ks = new ArrayList<>(byKind.entrySet());
        ks.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        StringBuilder kb = new StringBuilder("Tipo: ");
        for (Map.Entry<String, Integer> e : ks) {
            kb.append(e.getKey()).append(" ").append(String.format("%.1f", 100.0 * e.getValue() / n)).append("% · ");
        }
        final String kl = kb.toString();
        src.sendSuccess(() -> Component.literal(kl), false);
        List<Map.Entry<Item, Integer>> items = new ArrayList<>(byItem.entrySet());
        items.sort(Comparator.comparingInt((Map.Entry<Item, Integer> e) -> e.getValue()).reversed());
        StringBuilder ib = new StringBuilder("Más frecuentes: ");
        for (int i = 0; i < Math.min(8, items.size()); i++) {
            ib.append(items.get(i).getKey().getDescription().getString()).append(" ")
                    .append(String.format("%.1f", 100.0 * items.get(i).getValue() / n)).append("% · ");
        }
        final String il = ib.toString();
        src.sendSuccess(() -> Component.literal(il), false);
        return n;
    }

    private static int congelar(CommandSourceStack src, int saltos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        boolean ok = com.mundosdeaxelis.world.Frozen.freeze(sp, saltos);
        src.sendSuccess(() -> Component.literal(ok ? "Congelado: pulsa ESPACIO para romper el hielo."
                : "No se pudo congelar (ya estás congelado o acabas de romper el hielo)."), false);
        return ok ? 1 : 0;
    }
}
