package com.axeliscore.npc;

import com.axeliscore.ui.Theme;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Un NPC: su nombre, su piel, cómo saluda, su tienda, sus misiones, la amistad (niveles y regalos) y su rutina. Se registra al
 * arrancar, igual en el servidor y en el cliente:
 * <pre>
 *     public static final NpcDef HERRERA = NpcDef.registrar(new NpcDef("mimod", "herrera")
 *             .piel(Ids.rl("mimod", "textures/entity/herrera.png")).tema(Theme.MADERA)
 *             .saludos("mimod.herrera.hola1", "mimod.herrera.hola2")
 *             .tienda(new Tienda().vende("minecraft:iron_sword", 1, 6).compra("minecraft:iron_ingot", 1))
 *             .mision(new Mision("mimod", "hierro").objetivo(Objetivo.entregar("minecraft:iron_ingot", 8)).premio(Premio.esmeraldas(5))));
 * </pre>
 * Textos: el nombre es {@code npc.<mod>.<nombre>}; los niveles de amistad, {@code axelis_core.amistad.nivel<N>} (o los propios con
 * {@link #niveles}).
 */
public final class NpcDef {
    private static final Map<String, NpcDef> REGISTRO = new LinkedHashMap<>();

    public final String id;
    ResourceLocation piel;
    Theme tema = Theme.PERGAMINO;
    final List<String> saludos = new ArrayList<>();
    Tienda tienda;
    final List<Mision> misiones = new ArrayList<>();
    int[] umbrales = {0, 10, 30, 60, 100};
    String[] nombresNivel;
    final Map<Integer, Consumer<ServerPlayer>> regalos = new HashMap<>();
    Rutina rutina = Rutina.NORMAL;

    public NpcDef(String modid, String nombre) {
        this.id = modid + ":" + nombre;
    }

    public static synchronized NpcDef registrar(NpcDef d) {
        REGISTRO.put(d.id, d);
        for (Mision m : d.misiones) {
            Misiones.registrar(m.deNpc(d.id));
        }
        return d;
    }

    public static NpcDef porId(String id) {
        return id == null ? null : REGISTRO.get(id);
    }

    public static Collection<NpcDef> todos() {
        return REGISTRO.values();
    }

    /** Piel de 64x64 (como la de un jugador). */
    public NpcDef piel(ResourceLocation tex) {
        this.piel = tex;
        return this;
    }

    /** Colores de su ventana. */
    public NpcDef tema(Theme t) {
        this.tema = t;
        return this;
    }

    /** Claves de traducción de lo que dice al hablarle (elige una al azar; con amistad alta, las del final). */
    public NpcDef saludos(String... claves) {
        saludos.addAll(List.of(claves));
        return this;
    }

    public NpcDef tienda(Tienda t) {
        this.tienda = t;
        return this;
    }

    public NpcDef mision(Mision m) {
        misiones.add(m);
        return this;
    }

    /** Puntos para cada nivel de amistad (el primero siempre 0). */
    public NpcDef amistad(int... umbrales) {
        this.umbrales = umbrales;
        return this;
    }

    /** Claves de traducción del nombre de cada nivel de amistad. */
    public NpcDef niveles(String... claves) {
        this.nombresNivel = claves;
        return this;
    }

    /** Regalo al llegar a un nivel de amistad (1 = el segundo nivel...). */
    public NpcDef regalo(int nivel, Consumer<ServerPlayer> r) {
        regalos.put(nivel, r);
        return this;
    }

    public NpcDef rutina(Rutina r) {
        this.rutina = r;
        return this;
    }

    // ------------------------------------------------------------------ lectura

    public Component nombre() {
        return Component.translatable("npc." + id.replace(':', '.'));
    }

    public ResourceLocation piel() {
        return piel;
    }

    public Theme tema() {
        return tema;
    }

    public Tienda tienda() {
        return tienda;
    }

    public List<Mision> misiones() {
        return misiones;
    }

    public List<String> saludos() {
        return saludos;
    }

    public int niveles() {
        return umbrales.length;
    }

    public int umbral(int nivel) {
        return umbrales[Math.max(0, Math.min(umbrales.length - 1, nivel))];
    }

    /** Nivel de amistad con esos puntos (0 = recién conocidos). */
    public int nivelDe(int puntos) {
        int n = 0;
        for (int i = 0; i < umbrales.length; i++) {
            if (puntos >= umbrales[i]) {
                n = i;
            }
        }
        return n;
    }

    public Component nombreNivel(int nivel) {
        if (nombresNivel != null && nivel < nombresNivel.length) {
            return Component.translatable(nombresNivel[nivel]);
        }
        return Component.translatable("axelis_core.amistad.nivel" + Math.min(4, nivel));
    }

    /** ¿Da un regalo al llegar a ese nivel? */
    public boolean tieneRegalo(int nivel) {
        return regalos.containsKey(nivel);
    }

    public Rutina rutina() {
        return rutina;
    }

    /** Cómo pasa el día un NPC (lo usa {@link Npc} si tiene casa o lugar de trabajo). */
    public enum Rutina {
        /** Trabaja de día cerca de su trabajo, en la tarde va a su casa y de noche se sienta a descansar. */
        NORMAL,
        /** Se queda siempre cerca de su lugar (guardias, tenderos). */
        QUIETO,
        /** Pasea por ahí (sin casa ni trabajo). */
        LIBRE
    }
}
