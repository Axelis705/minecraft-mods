package com.mundosdeaxelis.client;

import com.mundosdeaxelis.entity.Archimago;
import com.mundosdeaxelis.entity.ArmaduraEncantada;
import com.mundosdeaxelis.entity.Ascua;
import com.mundosdeaxelis.entity.Brote;
import com.mundosdeaxelis.entity.Capataz;
import com.mundosdeaxelis.entity.Chispero;
import com.mundosdeaxelis.entity.CultistaVolcan;
import com.mundosdeaxelis.entity.Diablillo;
import com.mundosdeaxelis.entity.Esbirro;
import com.mundosdeaxelis.entity.Escoria;
import com.mundosdeaxelis.entity.Espinoso;
import com.mundosdeaxelis.entity.Fundidor;
import com.mundosdeaxelis.entity.GranHerrero;
import com.mundosdeaxelis.entity.GuardianMusgo;
import com.mundosdeaxelis.entity.LarvaMagma;
import com.mundosdeaxelis.entity.LibroEmbrujado;
import com.mundosdeaxelis.entity.LoboArtico;
import com.mundosdeaxelis.entity.LoboEscarcha;
import com.mundosdeaxelis.entity.Polinizador;
import com.mundosdeaxelis.entity.Raizal;
import com.mundosdeaxelis.entity.ReyBrote;
import com.mundosdeaxelis.entity.SalamandraLava;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.client.CreatureStyle.Death;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Cómo se mueve cada criatura del mod: su peso al andar, cuánto se inclina y se ladea, cómo muere y qué deja al pisar, al
 * recibir un golpe y al morir. La frecuencia de paso es la misma que usa su modelo (para que el polvo salga al apoyar el pie).
 */
final class CreatureStylesData {
    private CreatureStylesData() {
    }

    static void register() {
        // ---------------------------------------------------------------- bosque
        CreatureStyles.put(Brote.class, CreatureStyle.of().steps(0.8F, 0.25F).bob(0.035F).death(Death.FLOP)
                .hurtFx(() -> Fx.petal(0x7BC86A, 0.5F)).deathFx(() -> Fx.petal(0x8FD86E, 0.6F), 18));
        CreatureStyles.put(Espinoso.class, CreatureStyle.of().lean(0.0F).bank(0.0F).bob(0.0F).noDust().squash(0.6F).recoil(4.0F)
                .death(Death.CRUMBLE).hurtFx(() -> Fx.petal(0x7BC86A, 0.5F)).deathFx(() -> Fx.petal(0xE070A0, 0.65F), 24)
                .ambient(() -> Fx.dust(0xFFE88A, 0.18F), 24));
        CreatureStyles.put(Polinizador.class, CreatureStyle.of().flier().lean(1.2F).bank(1.4F).death(Death.POOF)
                .hurtFx(() -> Fx.dust(0xFFE070, 0.3F)).deathFx(() -> Fx.petal(0xFFB0D0, 0.6F), 22)
                .trail(() -> Fx.dust(0xFFE070, 0.2F), 4, true));
        CreatureStyles.put(Raizal.class, CreatureStyle.of().steps(0.6F, 0.3F).heavy().bob(0.05F).lean(0.8F).squash(0.6F).recoil(6.0F)
                .death(Death.TOPPLE).hurtFx(() -> Fx.petal(0x6DB85A, 0.55F)).deathFx(() -> Fx.petal(0x7BC86A, 0.65F), 28));
        CreatureStyles.put(GuardianMusgo.class, CreatureStyle.of().steps(0.5F, 0.3F).heavy().bob(0.06F).lean(0.6F).squash(0.5F)
                .recoil(4.0F).death(Death.CRUMBLE).hurtFx(() -> Fx.petal(0x5A9A4A, 0.6F)).deathFx(() -> Fx.petal(0x6DB85A, 0.7F), 34));
        CreatureStyles.put(ReyBrote.class, CreatureStyle.of().heavy().bob(0.0F).lean(0.5F).bank(0.5F).squash(0.5F).recoil(3.0F)
                .death(Death.MELT).hurtFx(() -> Fx.dust(0x9CE08A, 0.5F)).deathFx(() -> Fx.petal(0x8FD86E, 0.8F), 44));
        // ---------------------------------------------------------------- forja
        CreatureStyles.put(Escoria.class, CreatureStyle.of().steps(0.7F, 0.25F).bob(0.04F).death(Death.MELT)
                .hurtFx(() -> Fx.ember(0xFF7A2A, 0.4F)).deathFx(() -> ParticleTypes.LAVA, 14).ambient(() -> ParticleTypes.SMOKE, 20));
        CreatureStyles.put(Chispero.class, CreatureStyle.of().lean(0.0F).bank(0.0F).bob(0.0F).noDust().squash(0.4F).recoil(3.0F)
                .death(Death.CRUMBLE).hurtFx(() -> Fx.ember(0xFFA040, 0.4F)).deathFx(() -> Fx.ember(0xFF8A30, 0.5F), 26));
        CreatureStyles.put(Ascua.class, CreatureStyle.of().flier().lean(1.2F).bank(1.5F).death(Death.POOF)
                .hurtFx(() -> Fx.ember(0xFFB040, 0.4F)).deathFx(() -> Fx.ember(0xFF8A30, 0.5F), 28)
                .trail(() -> Fx.ember(0xFF9A3A, 0.28F), 2, true));
        CreatureStyles.put(Fundidor.class, CreatureStyle.of().steps(0.55F, 0.3F).heavy().bob(0.05F).lean(0.7F).squash(0.5F)
                .recoil(5.0F).death(Death.TOPPLE).hurtFx(() -> Fx.ember(0xFF7A2A, 0.45F)).deathFx(() -> Fx.ember(0xFF8A30, 0.55F), 30)
                .ambient(() -> ParticleTypes.SMOKE, 18));
        CreatureStyles.put(Capataz.class, CreatureStyle.of().steps(0.5F, 0.3F).heavy().bob(0.05F).lean(0.7F).squash(0.45F)
                .recoil(5.0F).death(Death.TOPPLE).hurtFx(() -> Fx.spark(0xFFC060, 0.4F)).deathFx(() -> Fx.ember(0xFF8A30, 0.55F), 30));
        CreatureStyles.put(GranHerrero.class, CreatureStyle.of().steps(0.45F, 0.3F).heavy().bob(0.0F).lean(0.45F).bank(0.4F)
                .squash(0.35F).recoil(4.0F).death(Death.TOPPLE).hurtFx(() -> Fx.spark(0xFFC060, 0.5F))
                .deathFx(() -> Fx.ember(0xFF8A30, 0.7F), 48));
        // ---------------------------------------------------------------- hielo
        CreatureStyles.put(LoboEscarcha.class, CreatureStyle.of().steps(0.7F, 0.25F).bob(0.03F).lean(1.0F).bank(1.3F).death(Death.FLOP)
                .hurtFx(() -> ParticleTypes.SNOWFLAKE).deathFx(() -> ParticleTypes.SNOWFLAKE, 30)
                .trail(() -> ParticleTypes.SNOWFLAKE, 4, false));
        CreatureStyles.put(LoboArtico.class, CreatureStyle.of().steps(0.6F, 0.3F).heavy().bob(0.0F).lean(0.5F).bank(0.6F).squash(0.3F)
                .recoil(4.0F).death(Death.NONE).hurtFx(() -> ParticleTypes.SNOWFLAKE).deathFx(() -> ParticleTypes.SNOWFLAKE, 44));
        // ---------------------------------------------------------------- torre
        CreatureStyles.put(Esbirro.class, CreatureStyle.of().steps(0.6662F, 0.2F).bob(0.02F).lean(0.9F).bank(1.0F).death(Death.FLOP)
                .hurtFx(() -> Fx.wisp(0x9A6AFF, 0.4F)).deathFx(() -> Fx.wisp(0x9A6AFF, 0.6F), 24));
        CreatureStyles.put(ArmaduraEncantada.class, CreatureStyle.of().steps(0.6662F, 0.2F).bob(0.03F).lean(0.6F).squash(0.3F)
                .recoil(5.0F).death(Death.CRUMBLE).hurtFx(() -> Fx.spark(0xDDE4FF, 0.4F)).deathFx(() -> Fx.wisp(0x8AA0FF, 0.6F), 22));
        CreatureStyles.put(LibroEmbrujado.class, CreatureStyle.of().flier().death(Death.NONE).hurtFx(() -> Fx.paper(0xE8DCC0, 0.5F))
                .deathFx(() -> Fx.paper(0xE8DCC0, 0.55F), 30).trail(() -> Fx.rune(0xB070FF, 0.25F), 6, true));
        CreatureStyles.put(Archimago.class, CreatureStyle.of().noDust().bob(0.0F).lean(0.4F).bank(0.5F).squash(0.3F).recoil(3.0F)
                .death(Death.NONE).hurtFx(() -> Fx.rune(0xB070FF, 0.4F)));
        // ---------------------------------------------------------------- volcán (mallas)
        CreatureStyles.put(SalamandraLava.class, CreatureStyle.of().steps(0.6662F, 0.3F).bob(0.02F).lean(0.8F).bank(1.2F).death(Death.FLOP)
                .hurtFx(() -> Fx.ember(0xFF7A2A, 0.4F)).deathFx(() -> ParticleTypes.LAVA, 12));
        CreatureStyles.put(Diablillo.class, CreatureStyle.of().flier().lean(1.2F).bank(1.4F).death(Death.POOF)
                .hurtFx(() -> Fx.ember(0xFFA040, 0.4F)).deathFx(() -> Fx.ember(0xFF8A30, 0.5F), 22)
                .trail(() -> Fx.ember(0xFF9A3A, 0.22F), 3, false));
        CreatureStyles.put(CultistaVolcan.class, CreatureStyle.of().steps(0.6662F, 0.2F).bob(0.02F).lean(0.8F).death(Death.TOPPLE)
                .hurtFx(() -> Fx.ember(0xFF7A2A, 0.4F)).deathFx(() -> Fx.ember(0xFF6A20, 0.55F), 26));
        CreatureStyles.put(LarvaMagma.class, CreatureStyle.of().steps(0.6662F, 0.2F).bob(0.02F).lean(0.6F).squash(1.2F).death(Death.MELT)
                .hurtFx(() -> Fx.ember(0xFF7A2A, 0.35F)).deathFx(() -> ParticleTypes.LAVA, 10));
    }
}
