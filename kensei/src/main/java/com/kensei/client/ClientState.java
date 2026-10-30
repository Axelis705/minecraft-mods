package com.kensei.client;

import com.kensei.KenseiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Lo último que el servidor dijo sobre mi energía, mi combo y la espera del crítico, más valores suavizados para dibujar el panel. */
public final class ClientState {
    public static float energy = 0;
    /** La energía que se dibuja: sube suave hacia la real (así la recarga se ve fluida aunque el servidor avise cada 2 ticks). */
    public static float shownEnergy = 0;
    public static int counterTicks = 0;
    public static int combo = 0;
    /** Ticks que faltan para poder volver a usar el ataque crítico. */
    public static int heavyCooldown = 0;
    /** Estamina (para atacar y bloquear), la que se dibuja (suave), el máximo y los ticks de agotamiento que quedan. */
    public static float stamina = 100;
    public static float shownStamina = 100;
    public static float staminaMax = 100;
    public static int exhaustTicks = 0;
    /** Cuándo se intentó atacar sin estamina (para el destello de la barra). */
    public static long noStaminaMillis = 0;
    /** Para el destello de la barra al subir de nivel. */
    public static long lastGainMillis = 0;
    /** Cuándo se abrió el contraataque (parry o esquive perfecto): para el destello de pantalla. */
    public static long counterFlashMillis = 0;
    /** Vida que se dibuja como "estela" de daño: baja despacio hasta la vida real después de recibir un golpe. */
    public static float healthGhost = 20;
    private static float lastHealth = 20;
    private static int ghostHold = 0;

    private ClientState() {
    }

    public static void update(float newEnergy, int counter, int comboStep, int heavyCd, float newStamina, float newStaminaMax, int exhaust) {
        stamina = newStamina;
        staminaMax = Math.max(1.0F, newStaminaMax);
        exhaustTicks = exhaust;
        if (newEnergy > energy + 0.5F && Math.floor(newEnergy / 100.0F) > Math.floor(energy / 100.0F)) {
            lastGainMillis = System.currentTimeMillis(); // destello solo cuando se completa un dash
        }
        if (counter > counterTicks + 5) {
            counterFlashMillis = System.currentTimeMillis();
        }
        energy = newEnergy;
        counterTicks = counter;
        combo = comboStep;
        heavyCooldown = heavyCd;
    }

    public static void tick() {
        if (counterTicks > 0) {
            counterTicks--;
        }
        if (heavyCooldown > 0) {
            heavyCooldown--;
        }
        if (exhaustTicks > 0) {
            exhaustTicks--;
        }
        if (shownStamina > stamina) {
            shownStamina = Math.max(stamina, shownStamina - Math.max(1.5F, (shownStamina - stamina) * 0.4F));
        } else if (shownStamina < stamina) {
            shownStamina = Math.min(stamina, shownStamina + Math.max(1.2F, (stamina - shownStamina) * 0.3F));
        }
        if (shownEnergy > energy) {
            shownEnergy = energy;
        } else if (shownEnergy < energy) {
            shownEnergy = Math.min(energy, shownEnergy + Math.max(2.5F, (energy - shownEnergy) * 0.35F));
        }
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            float hp = p.getHealth();
            if (hp < lastHealth - 0.01F) {
                ghostHold = 18;
            }
            lastHealth = hp;
            if (ghostHold > 0) {
                ghostHold--;
            } else if (healthGhost > hp) {
                healthGhost = Math.max(hp, healthGhost - Math.max(0.15F, (healthGhost - hp) * 0.12F));
            }
            if (healthGhost < hp) {
                healthGhost = hp;
            }
        }
    }

    /** ¿Alcanza la estamina (y no se está agotado) para algo que cuesta esto? Si no, se destella la barra. */
    public static boolean canSpend(float cost) {
        if (!KenseiConfig.STAMINA_ENABLED.get()) {
            return true;
        }
        if (exhaustTicks > 0 || stamina + 0.001F < cost) {
            noStaminaMillis = System.currentTimeMillis();
            return false;
        }
        return true;
    }

    /** Se gasta ya en pantalla (el servidor lo confirma enseguida): así el clic siguiente se rechaza sin esperar al aviso. */
    public static void spendLocal(float cost) {
        if (KenseiConfig.STAMINA_ENABLED.get()) {
            stamina = Math.max(0.0F, stamina - cost);
        }
    }

    public static float max() {
        return KenseiConfig.maxEnergy();
    }
}
