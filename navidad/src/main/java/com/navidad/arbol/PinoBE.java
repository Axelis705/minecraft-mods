package com.navidad.arbol;

import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Lo que tiene colgado el Pino de Navidad: un adorno (o nada) en cada lugar de {@link PuntosArbol#LUGARES}, cuántas tiras de luces y de
 * guirnalda lleva (de 0 a 3) y qué hay en la punta. Se guarda con el mundo, se manda a los jugadores para dibujarlo y se queda en el
 * objeto al romper el árbol (así lo cambias de lugar con todo y adornos).
 */
public class PinoBE extends BlockEntity {
    public static final int MAX_TIRAS = 3;
    public final String[] lugares = new String[PuntosArbol.LUGARES.length];
    public int luces, guirnaldas;
    public String punta = "";
    /** El último nivel que se le festejó al árbol (para avisar solo cuando sube). */
    public int nivelVisto;
    /** (Cliente) cuándo apareció cada adorno, para que salte al colgarlo (el último lugar es la punta). */
    public final long[] aparecio = new long[PuntosArbol.LUGARES.length + 1];
    private boolean cargado;

    public PinoBE(BlockPos p, BlockState s) {
        super(ModRegistry.PINO_NAVIDAD_BE.get(), p, s);
        java.util.Arrays.fill(lugares, "");
    }

    // ------------------------------------------------------------------ colgar y quitar

    private static double dist2(float[] l, Vec3 v) {
        double dx = l[0] - v.x, dy = l[1] - v.y, dz = l[2] - v.z;
        return dx * dx + dy * dy * 0.7 + dz * dz;
    }

    /** El lugar libre más cerca de donde hiciste clic (en bloques desde el centro del piso del árbol), o -1 si ya no hay. */
    public int libre(Vec3 donde) {
        int mejor = -1;
        double md = Double.MAX_VALUE;
        for (int i = 0; i < lugares.length; i++) {
            if (!lugares[i].isEmpty()) {
                continue;
            }
            double d = dist2(PuntosArbol.LUGARES[i], donde);
            if (d < md) {
                md = d;
                mejor = i;
            }
        }
        return mejor;
    }

    /** Qué pasó al intentar colgar algo. */
    public enum Resultado {
        COLGADO, SIN_LUGAR, LUCES_LLENAS, GUIRNALDAS_LLENAS, PUNTA_CAMBIADA
    }

    /** Cuelga el adorno; si era una punta y ya había otra, la de antes queda en {@code devolver[0]}. */
    public Resultado colgar(Adornos.Def d, Vec3 donde, ItemStack[] devolver) {
        switch (d.tipo()) {
            case LUCES -> {
                if (luces >= MAX_TIRAS) {
                    return Resultado.LUCES_LLENAS;
                }
                luces++;
            }
            case GUIRNALDA -> {
                if (guirnaldas >= MAX_TIRAS) {
                    return Resultado.GUIRNALDAS_LLENAS;
                }
                guirnaldas++;
            }
            case PUNTA -> {
                boolean habia = !punta.isEmpty();
                if (habia) {
                    devolver[0] = new ItemStack(Adornos.item(punta));
                }
                punta = d.id();
                cambio();
                return habia ? Resultado.PUNTA_CAMBIADA : Resultado.COLGADO;
            }
            default -> {
                int i = libre(donde);
                if (i < 0) {
                    return Resultado.SIN_LUGAR;
                }
                lugares[i] = d.id();
            }
        }
        cambio();
        return Resultado.COLGADO;
    }

    /** Quita el adorno más cerca de donde hiciste clic (o la punta, o una tira) y lo devuelve; vacío si no tenía nada. */
    public ItemStack quitar(Vec3 donde) {
        int mejor = -1;
        double md = Double.MAX_VALUE;
        for (int i = 0; i < lugares.length; i++) {
            if (lugares[i].isEmpty()) {
                continue;
            }
            double d = dist2(PuntosArbol.LUGARES[i], donde);
            if (d < md) {
                md = d;
                mejor = i;
            }
        }
        ItemStack out = ItemStack.EMPTY;
        if (!punta.isEmpty() && donde.y > 1.9) {
            out = new ItemStack(Adornos.item(punta));
            punta = "";
        } else if (mejor >= 0 && md < 0.5) {
            out = new ItemStack(Adornos.item(lugares[mejor]));
            lugares[mejor] = "";
        } else if (luces > 0) {
            luces--;
            out = new ItemStack(Adornos.item("adorno_luces"));
        } else if (guirnaldas > 0) {
            guirnaldas--;
            out = new ItemStack(Adornos.item("adorno_guirnalda"));
        } else if (mejor >= 0) {
            out = new ItemStack(Adornos.item(lugares[mejor]));
            lugares[mejor] = "";
        } else if (!punta.isEmpty()) {
            out = new ItemStack(Adornos.item(punta));
            punta = "";
        }
        if (!out.isEmpty()) {
            cambio();
        }
        return out;
    }

    /** Todo lo que tiene colgado, como objetos (para soltarlo si el árbol se rompe sin poder guardarse). */
    public java.util.List<ItemStack> todo() {
        java.util.List<ItemStack> l = new java.util.ArrayList<>();
        for (String s : lugares) {
            if (!s.isEmpty()) {
                l.add(new ItemStack(Adornos.item(s)));
            }
        }
        if (luces > 0) {
            l.add(new ItemStack(Adornos.item("adorno_luces"), luces));
        }
        if (guirnaldas > 0) {
            l.add(new ItemStack(Adornos.item("adorno_guirnalda"), guirnaldas));
        }
        if (!punta.isEmpty()) {
            l.add(new ItemStack(Adornos.item(punta)));
        }
        return l;
    }

    // ------------------------------------------------------------------ puntos

    /** Los puntos de lo que tiene colgado (sin contar la variedad ni la casa). */
    public int puntosAdornos() {
        int n = 0;
        for (String s : lugares) {
            Adornos.Def d = Adornos.de(s);
            if (d != null) {
                n += d.puntos();
            }
        }
        n += luces * 3 + guirnaldas * 2;
        Adornos.Def p = Adornos.de(punta);
        if (p != null) {
            n += p.puntos();
        }
        return n;
    }

    /** Cuántos adornos distintos tiene (cada uno distinto da un punto más). */
    public int variedad() {
        Set<String> s = new HashSet<>();
        for (String x : lugares) {
            if (!x.isEmpty()) {
                s.add(x);
            }
        }
        if (luces > 0) {
            s.add("adorno_luces");
        }
        if (guirnaldas > 0) {
            s.add("adorno_guirnalda");
        }
        if (!punta.isEmpty()) {
            s.add(punta);
        }
        return s.size();
    }

    public int colgados() {
        int n = 0;
        for (String s : lugares) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ guardar y mandar

    private void cambio() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        String[] antes = lugares.clone();
        String puntaAntes = punta;
        java.util.Arrays.fill(lugares, "");
        ListTag l = t.getList("Adornos", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(l.size(), lugares.length); i++) {
            lugares[i] = l.getString(i);
        }
        luces = Math.max(0, Math.min(MAX_TIRAS, t.getInt("Luces")));
        guirnaldas = Math.max(0, Math.min(MAX_TIRAS, t.getInt("Guirnaldas")));
        punta = t.getString("Punta");
        nivelVisto = t.getInt("NivelVisto");
        boolean primera = !cargado;
        cargado = true;
        if (level != null && level.isClientSide && !primera) {
            long ahora = level.getGameTime();
            for (int i = 0; i < lugares.length; i++) {
                if (!lugares[i].isEmpty() && !lugares[i].equals(antes[i])) {
                    aparecio[i] = ahora;
                }
            }
            if (!punta.isEmpty() && !punta.equals(puntaAntes)) {
                aparecio[lugares.length] = ahora;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        escribir(t);
        t.putInt("NivelVisto", nivelVisto);
    }

    /** Lo que se dibuja (adornos, tiras y punta), igual en el bloque y en el objeto. */
    public void escribir(CompoundTag t) {
        ListTag l = new ListTag();
        for (String s : lugares) {
            l.add(StringTag.valueOf(s));
        }
        t.put("Adornos", l);
        t.putInt("Luces", luces);
        t.putInt("Guirnaldas", guirnaldas);
        t.putString("Punta", punta);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        escribir(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
    }
}
