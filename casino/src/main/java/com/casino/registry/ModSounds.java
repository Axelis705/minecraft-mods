package com.casino.registry;

import com.casino.plat.Ids;
import com.casino.Casino;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Casino.MODID);

    /** Música de jazz de las bocinas (pista larga, se transmite). */
    public static final RegistryObject<SoundEvent> MUSIC_LOUNGE = reg("music_lounge");

    /** El "tic" del puntero al pasar por cada casilla. */
    public static final RegistryObject<SoundEvent> WHEEL_TICK = reg("wheel_tick");
    /** Arranque del giro. */
    public static final RegistryObject<SoundEvent> WHEEL_START = reg("wheel_start");
    /** Fanfarria de premio normal. */
    public static final RegistryObject<SoundEvent> WHEEL_WIN = reg("wheel_win");
    /** Fanfarria larga de premio grande. */
    public static final RegistryObject<SoundEvent> WHEEL_JACKPOT = reg("wheel_jackpot");
    /** Monedas / fichas al comprar o canjear. */
    public static final RegistryObject<SoundEvent> COINS = reg("coins");
    /** Clic de ficha al apoyarla. */
    public static final RegistryObject<SoundEvent> CHIP = reg("chip");
    /** Algo no se pudo hacer. */
    public static final RegistryObject<SoundEvent> DENIED = reg("denied");
    /** Premio gordo cambiado en el podio. */
    public static final RegistryObject<SoundEvent> PRIZE_SET = reg("prize_set");

    /** La bola golpea una clavija del Plinko. */
    public static final RegistryObject<SoundEvent> PLINKO_PEG = reg("plinko_peg");
    /** Se suelta una bola. */
    public static final RegistryObject<SoundEvent> PLINKO_DROP = reg("plinko_drop");
    /** La bola cae en su casillero. */
    public static final RegistryObject<SoundEvent> PLINKO_LAND = reg("plinko_land");

    /** Se tira de la palanca de la tragamonedas. */
    public static final RegistryObject<SoundEvent> SLOT_LEVER = reg("slot_lever");
    /** "Tic" de los carretes girando. */
    public static final RegistryObject<SoundEvent> SLOT_TICK = reg("slot_tick");
    /** Un carrete se detiene. */
    public static final RegistryObject<SoundEvent> SLOT_STOP = reg("slot_stop");

    /** Una carta se reparte sobre el fieltro. */
    public static final RegistryObject<SoundEvent> CARD_DEAL = reg("card_deal");
    /** Una carta se da vuelta. */
    public static final RegistryObject<SoundEvent> CARD_FLIP = reg("card_flip");

    /** La bola corre por la pista de la ruleta. */
    public static final RegistryObject<SoundEvent> ROULETTE_ROLL = reg("roulette_roll");
    /** La bola salta entre las casillas de la ruleta. */
    public static final RegistryObject<SoundEvent> ROULETTE_TICK = reg("roulette_tick");
    /** La bola se queda quieta en su casilla. */
    public static final RegistryObject<SoundEvent> ROULETTE_LAND = reg("roulette_land");
    /** Campanilla de "no va más". */
    public static final RegistryObject<SoundEvent> ROULETTE_BELL = reg("roulette_bell");

    /** Se rasca un boleto de rasca y gana. */
    public static final RegistryObject<SoundEvent> SCRATCH = reg("scratch");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Ids.rl(Casino.MODID, name)));
    }
}
