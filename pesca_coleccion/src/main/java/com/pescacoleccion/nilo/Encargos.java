package com.pescacoleccion.nilo;

import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.Cana;
import com.pescacoleccion.pesca.Cebo;
import com.pescacoleccion.pesca.Especie;
import com.pescacoleccion.pesca.Especies;
import com.pescacoleccion.pesca.PezItem;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * (Servidor) Los encargos de Nilo: cada jugador tiene 3 a la vez. Se cumplen entregando peces o pescando de cierta forma; al cobrarlos
 * dan esmeraldas, carnadas y amistad. Los cumplidos se cambian por nuevos al día siguiente; uno al día se puede cambiar por otro.
 * <p>
 * Datos (por jugador, en {@code nilo}): conoce, amistad, nivel (último regalo dado), dia, cambio, hechos y la lista de encargos. Cada
 * encargo: t (tipo), e (especie), r (rareza), cm (tamaño mínimo), n (cuántos), p (llevas), es (esmeraldas), cb/cbn (carnada y
 * cuántas), am (amistad), ok (ya cobrado).
 */
public final class Encargos {
    private static final String MOD = PescaColeccion.MODID;
    public static final int CUANTOS = 3;
    /** Amistad que pide cada nivel (0 = recién conocidos ... 4 = leyenda del muelle). */
    public static final int[] NIVELES = {0, 6, 16, 32, 56};
    /** Lo que vale un pez para Nilo, por rareza (grande = la cuarta parte más grande de su especie). */
    public static final int[] VALOR = {1, 2, 4, 8, 16};
    public static final int[] VALOR_GRANDE = {2, 3, 6, 12, 24};

    private Encargos() {
    }

    public static void init() {
        Actions.on(MOD + ":nilo_encargo", (p, d) -> {
            if (Tienda.cerca(p)) {
                accion(p, Nbt.i(d, "slot"), Nbt.s(d, "que"));
            }
        });
    }

    /** Los datos de Nilo de este jugador. */
    public static CompoundTag datos(ServerPlayer p) {
        CompoundTag d = PlayerStore.of(p, MOD);
        if (!Nbt.has(d, "nilo")) {
            d.put("nilo", new CompoundTag());
        }
        return Nbt.c(d, "nilo");
    }

    public static int nivel(int amistad) {
        int n = 0;
        for (int i = 0; i < NIVELES.length; i++) {
            if (amistad >= NIVELES[i]) {
                n = i;
            }
        }
        return n;
    }

    private static long dia(ServerPlayer p) {
        return Plat.dayTime(p.level()) / 24000L;
    }

    /** Deja listos los 3 encargos (los cobrados se cambian si ya es otro día). No guarda: quien llama guarda. */
    public static void preparar(ServerPlayer p) {
        CompoundTag nd = datos(p);
        ListTag l = Nbt.list(nd, "encargos");
        long hoy = dia(p);
        boolean nuevoDia = !Nbt.has(nd, "dia") || Nbt.l(nd, "dia") != hoy;
        List<CompoundTag> lista = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            lista.add(Nbt.at(l, i));
        }
        boolean cambio = false;
        for (int i = 0; i < CUANTOS; i++) {
            if (i >= lista.size()) {
                lista.add(nuevo(p, nd, lista, i, hoy, 0));
                cambio = true;
            } else if (nuevoDia && Nbt.b(lista.get(i), "ok")) {
                lista.set(i, nuevo(p, nd, lista, i, hoy, 0));
                cambio = true;
            }
        }
        if (cambio || nuevoDia) {
            ListTag out = new ListTag();
            out.addAll(lista);
            nd.put("encargos", out);
            nd.putLong("dia", hoy);
        }
    }

    // ------------------------------------------------------------------ encargos nuevos

    /** Un encargo nuevo para el lugar {@code slot} (sin repetir el tipo de los otros). */
    static CompoundTag nuevo(ServerPlayer p, CompoundTag nd, List<CompoundTag> otros, int slot, long hoy, int vuelta) {
        RandomSource r = RandomSource.create(p.getUUID().getLeastSignificantBits() ^ hoy * 31 + slot * 7919L + vuelta * 104729L + Nbt.i(nd, "hechos"));
        int nivel = nivel(Nbt.i(nd, "amistad"));
        CompoundTag peces = Nbt.c(PlayerStore.of(p, MOD), "peces");
        List<Especie> conocidas = new ArrayList<>();
        int faltan = 0;
        for (Especie e : Especies.all()) {
            if (Nbt.i(Nbt.c(peces, e.id()), "n") > 0) {
                conocidas.add(e);
            } else {
                faltan++;
            }
        }
        Set<String> usados = new HashSet<>();
        for (int i = 0; i < otros.size(); i++) {
            if (i != slot) {
                usados.add(Nbt.s(otros.get(i), "t"));
            }
        }
        List<String> tipos = new ArrayList<>();
        for (String t : new String[] {"entrega", "entrega", "grande", "rareza", "rareza", "perfecta", "noche", "lluvia", "nueva"}) {
            boolean puede = switch (t) {
                case "entrega", "grande" -> !conocidas.isEmpty();
                case "perfecta" -> nivel >= 1;
                case "nueva" -> faltan > 0;
                default -> true;
            };
            if (puede && !usados.contains(t)) {
                tipos.add(t);
            }
        }
        if (tipos.isEmpty()) {
            tipos.add("rareza");
        }
        String t = tipos.get(r.nextInt(tipos.size()));
        CompoundTag e = new CompoundTag();
        e.putString("t", t);
        e.putInt("cb", -1);
        switch (t) {
            case "entrega" -> {
                Especie es = elegir(conocidas, r, Math.min(3, nivel + 1));
                int rz = es.rareza();
                int n = rz == 0 ? 3 : rz == 1 ? 2 : 1;
                e.putString("e", es.id());
                e.putInt("n", n);
                e.putInt("es", VALOR[rz] * n + 2 + nivel);
                e.putInt("am", 2 + rz);
                premioCebo(e, Cebo.LOMBRIZ, 6);
            }
            case "grande" -> {
                Especie es = elegir(conocidas, r, Math.min(3, nivel + 1));
                e.putString("e", es.id());
                e.putFloat("cm", Math.round((es.cmMin() + (es.cmMax() - es.cmMin()) * 0.7F) * 10) / 10F);
                e.putInt("n", 1);
                e.putInt("es", VALOR_GRANDE[es.rareza()] * 2 + 2 + nivel);
                e.putInt("am", 3 + es.rareza());
                premioCebo(e, Cebo.DULCE, 2);
            }
            case "rareza" -> {
                int rz = 1 + r.nextInt(Math.min(3, nivel + 1));
                e.putInt("r", rz);
                e.putInt("n", rz == 1 ? 3 : rz == 2 ? 2 : 1);
                e.putInt("es", VALOR[rz] * (rz == 1 ? 3 : rz == 2 ? 2 : 1) + 3);
                e.putInt("am", 2 + rz);
                if (rz >= 2) {
                    premioCebo(e, Cebo.BRILLANTE, 1);
                }
            }
            case "perfecta" -> {
                int n = 1 + r.nextInt(2) + nivel / 2;
                e.putInt("n", n);
                e.putInt("es", 3 * n + 1);
                e.putInt("am", 2 + n);
            }
            case "noche" -> {
                e.putInt("n", 3);
                e.putInt("es", 5 + nivel);
                e.putInt("am", 3);
                premioCebo(e, Cebo.DULCE, 2);
            }
            case "lluvia" -> {
                e.putInt("n", 2);
                e.putInt("es", 6 + nivel);
                e.putInt("am", 3);
                premioCebo(e, Cebo.BRILLANTE, 1);
            }
            default -> {
                // especie nueva para la colección
                e.putInt("n", 1);
                e.putInt("es", 8 + nivel * 2);
                e.putInt("am", 5);
                premioCebo(e, nivel >= 2 ? Cebo.LUNA : Cebo.BRILLANTE, nivel >= 2 ? 1 : 2);
            }
        }
        return e;
    }

    private static void premioCebo(CompoundTag e, Cebo c, int n) {
        e.putInt("cb", c.ordinal());
        e.putInt("cbn", n);
    }

    /** Una especie que ya conoce, sin pasarse de rareza (prefiere las comunes en los primeros niveles). */
    private static Especie elegir(List<Especie> conocidas, RandomSource r, int maxRareza) {
        List<Especie> ok = new ArrayList<>();
        for (Especie es : conocidas) {
            if (es.rareza() <= maxRareza) {
                ok.add(es);
            }
        }
        List<Especie> l = ok.isEmpty() ? conocidas : ok;
        return l.get(r.nextInt(l.size()));
    }

    // ------------------------------------------------------------------ progreso al pescar

    /** Se sacó un pez: avanzan los encargos de pescar. Avisa cuando uno se cumple. No guarda: quien llama guarda. */
    public static void alPescar(ServerPlayer p, Especie e, float cm, boolean perfecto, boolean nuevo) {
        CompoundTag nd = datos(p);
        ListTag l = Nbt.list(nd, "encargos");
        boolean noche = Especies.hora(p.level(), "noche");
        boolean lluvia = p.level().isRaining();
        for (int i = 0; i < l.size(); i++) {
            CompoundTag q = Nbt.at(l, i);
            if (Nbt.b(q, "ok") || cumplido(q)) {
                continue;
            }
            boolean cuenta = switch (Nbt.s(q, "t")) {
                case "rareza" -> e.rareza() >= Nbt.i(q, "r");
                case "perfecta" -> perfecto;
                case "noche" -> noche;
                case "lluvia" -> lluvia;
                case "nueva" -> nuevo;
                default -> false;
            };
            if (cuenta) {
                q.putInt("p", Nbt.i(q, "p") + 1);
                if (cumplido(q)) {
                    Plat.msg(p, Component.translatable(MOD + ".nilo.cumplido"), true);
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
                }
            }
        }
    }

    /** ¿Ya se cumplió (falta cobrarlo)? Los de entregar se cumplen al entregar. */
    public static boolean cumplido(CompoundTag q) {
        return Nbt.i(q, "p") >= Math.max(1, Nbt.i(q, "n"));
    }

    // ------------------------------------------------------------------ entregar, cobrar, cambiar (botones de la ventana)

    public static void accion(ServerPlayer p, int slot, String que) {
        preparar(p);
        CompoundTag nd = datos(p);
        ListTag l = Nbt.list(nd, "encargos");
        if (slot < 0 || slot >= l.size()) {
            return;
        }
        CompoundTag q = Nbt.at(l, slot);
        if (Nbt.b(q, "ok")) {
            return;
        }
        switch (que) {
            case "entregar" -> {
                if (entregar(p, q)) {
                    cobrar(p, nd, q);
                }
            }
            case "cobrar" -> {
                if (cumplido(q)) {
                    cobrar(p, nd, q);
                }
            }
            case "cambiar" -> {
                long hoy = dia(p);
                if (Nbt.l(nd, "cambio") != hoy + 1) {
                    List<CompoundTag> lista = new ArrayList<>();
                    for (int i = 0; i < l.size(); i++) {
                        lista.add(Nbt.at(l, i));
                    }
                    l.set(slot, nuevo(p, nd, lista, slot, hoy, 1 + Nbt.i(nd, "hechos") + (int) (hoy % 97)));
                    nd.putLong("cambio", hoy + 1);
                }
            }
            default -> {
            }
        }
        PlayerStore.save(p, MOD);
    }

    /** ¿Lleva los peces que pide? Si sí, se los quita. */
    static boolean entregar(ServerPlayer p, CompoundTag q) {
        String t = Nbt.s(q, "t");
        if (!t.equals("entrega") && !t.equals("grande")) {
            return false;
        }
        String id = Nbt.s(q, "e");
        float min = t.equals("grande") ? Nbt.f(q, "cm") : 0;
        int n = Math.max(1, Nbt.i(q, "n"));
        List<ItemStack> sirven = new ArrayList<>();
        int tiene = 0;
        for (ItemStack st : Plat.mainAndOffhand(p.getInventory())) {
            if (st.getItem() instanceof PezItem pz && pz.especie().id().equals(id) && PezItem.cm(st) >= min) {
                sirven.add(st);
                tiene += st.getCount();
            }
        }
        if (tiene < n) {
            Plat.msg(p, Component.translatable(MOD + ".nilo.te_faltan", n - tiene), true);
            return false;
        }
        int falta = n;
        for (ItemStack st : sirven) {
            int k = Math.min(falta, st.getCount());
            st.shrink(k);
            falta -= k;
            if (falta == 0) {
                break;
            }
        }
        q.putInt("p", n);
        return true;
    }

    /** Paga el encargo: esmeraldas, carnadas y amistad (y el regalo si sube de nivel). */
    static void cobrar(ServerPlayer p, CompoundTag nd, CompoundTag q) {
        q.putBoolean("ok", true);
        int es = Nbt.i(q, "es");
        if (es > 0) {
            Plat.give(p, new ItemStack(Items.EMERALD, es));
        }
        int cb = Nbt.i(q, "cb");
        if (cb >= 0 && cb < Cebo.values().length && Nbt.i(q, "cbn") > 0) {
            Plat.give(p, new ItemStack(ModRegistry.CEBOS.get(Cebo.values()[cb]).get(), Nbt.i(q, "cbn")));
        }
        nd.putInt("hechos", Nbt.i(nd, "hechos") + 1);
        sumarAmistad(p, nd, Nbt.i(q, "am"));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.5F);
        PescaColeccion.LOGGER.info("PESCA_NILO_COBRO tipo={} esmeraldas={} amistad={}", Nbt.s(q, "t"), es, Nbt.i(nd, "amistad"));
    }

    /** Suma amistad; al llegar a un nivel nuevo, Nilo da su regalo. */
    public static void sumarAmistad(ServerPlayer p, CompoundTag nd, int am) {
        int antes = nivel(Nbt.i(nd, "amistad"));
        nd.putInt("amistad", Nbt.i(nd, "amistad") + am);
        int ahora = nivel(Nbt.i(nd, "amistad"));
        for (int n = Math.max(antes + 1, Nbt.i(nd, "nivel") + 1); n <= ahora; n++) {
            regalo(p, n);
            nd.putInt("nivel", n);
        }
    }

    /** El regalo de cada nivel de amistad. */
    static void regalo(ServerPlayer p, int nivel) {
        ItemStack st = switch (nivel) {
            case 1 -> new ItemStack(ModRegistry.CEBOS.get(Cebo.LOMBRIZ).get(), 8);
            case 2 -> new ItemStack(ModRegistry.CEBOS.get(Cebo.DULCE).get(), 4);
            case 3 -> new ItemStack(ModRegistry.CEBOS.get(Cebo.BRILLANTE).get(), 3);
            default -> new ItemStack(ModRegistry.CANAS.get(Cana.NILO).get());
        };
        if (nivel >= 4) {
            CompoundTag d = PlayerStore.of(p, MOD);
            d.putInt("canas", Nbt.i(d, "canas") | (1 << Cana.NILO.ordinal()));
            Plat.give(p, new ItemStack(ModRegistry.CEBOS.get(Cebo.LUNA).get(), 2));
        }
        Plat.give(p, st.copy());
        Fx.toast(p, st, Component.translatable(MOD + ".nilo.nivel", Component.translatable(MOD + ".nilo.nivel." + nivel)),
                Component.translatable(MOD + ".nilo.regalo." + nivel), 0xFF7AC8);
        PescaColeccion.LOGGER.info("PESCA_NILO_NIVEL {}", nivel);
    }
}
