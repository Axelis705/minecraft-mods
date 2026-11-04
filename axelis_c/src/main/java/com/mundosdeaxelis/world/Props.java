package com.mundosdeaxelis.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Piezas 3D temporales para las escenas de los jefes: bloques "display" (los que usa el comando /summon block_display) con tamaño,
 * giro y brillo propios. Con ellas se arman modelos grandes (la vaina-meteoro del Rey Brote, las cadenas y los pinchos de la
 * Forja) sin necesidad de un modelo propio. Todas llevan una etiqueta para poder limpiarlas aunque el servidor se haya cerrado
 * a mitad de una escena.
 */
public final class Props {
    public static final String TAG = "mundos_intro_prop";
    private static final AABB ARENA = new AABB(-80, -20, -80, 80, 260, 80);
    private static final List<Piece> LIVE = new ArrayList<>();

    private Props() {
    }

    /** Un bloque display: su forma (bloque), tamaño y punto de apoyo (pivot) no cambian; la posición y el giro sí. */
    public static final class Piece {
        private final Display.BlockDisplay entity;
        private final String block;
        private final Vector3f scale;
        private final Vector3f pivot;
        private final int light;

        private Piece(Display.BlockDisplay entity, String block, Vector3f scale, Vector3f pivot, int light) {
            this.entity = entity;
            this.block = block;
            this.scale = scale;
            this.pivot = pivot;
            this.light = light;
        }

        /** Coloca la pieza con su pivot en {@code pos} y girada {@code rot}. */
        public void put(Vec3 pos, Quaternionf rot) {
            this.entity.load(tag(block, scale, pivot, light, pos, rot));
        }

        public void put(Vec3 pos) {
            put(pos, new Quaternionf());
        }

        public Display.BlockDisplay entity() {
            return entity;
        }

        public void remove() {
            this.entity.discard();
            LIVE.remove(this);
        }
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    private static CompoundTag tag(String block, Vector3f scale, Vector3f pivot, int light, Vec3 pos, Quaternionf rot) {
        CompoundTag t = new CompoundTag();
        CompoundTag bs = new CompoundTag();
        bs.putString("Name", block);
        t.put("block_state", bs);
        // translation = -R * pivot  (para que el pivot quede justo en la posición de la entidad)
        Vector3f tr = new Vector3f(pivot);
        rot.transform(tr);
        tr.negate();
        CompoundTag tf = new CompoundTag();
        tf.put("translation", floats(tr.x, tr.y, tr.z));
        tf.put("scale", floats(scale.x, scale.y, scale.z));
        tf.put("left_rotation", floats(rot.x, rot.y, rot.z, rot.w));
        tf.put("right_rotation", floats(0, 0, 0, 1));
        t.put("transformation", tf);
        if (light >= 0) {
            CompoundTag br = new CompoundTag();
            br.putInt("sky", light);
            br.putInt("block", light);
            t.put("brightness", br);
        }
        t.putFloat("view_range", 6.0F);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(TAG));
        t.put("Tags", tags);
        ListTag p = new ListTag();
        p.add(DoubleTag.valueOf(pos.x));
        p.add(DoubleTag.valueOf(pos.y));
        p.add(DoubleTag.valueOf(pos.z));
        t.put("Pos", p);
        ListTag m = new ListTag();
        m.add(DoubleTag.valueOf(0));
        m.add(DoubleTag.valueOf(0));
        m.add(DoubleTag.valueOf(0));
        t.put("Motion", m);
        t.put("Rotation", floats(0, 0));
        return t;
    }

    /**
     * Crea una pieza. {@code centered}: el pivot es el centro del bloque; si no, el centro de su cara de abajo (sirve para columnas,
     * cadenas y pinchos). {@code light} 0-15 fuerza ese brillo (se ve aunque esté oscuro); -1 usa la luz normal.
     */
    public static Piece spawn(ServerLevel level, Block block, Vec3 pos, float sx, float sy, float sz, boolean centered, int light, Quaternionf rot) {
        Display.BlockDisplay e = EntityType.BLOCK_DISPLAY.create(level);
        if (e == null) {
            return null;
        }
        String name = BuiltInRegistries.BLOCK.getKey(block).toString();
        Vector3f scale = new Vector3f(sx, sy, sz);
        Vector3f pivot = new Vector3f(sx / 2.0F, centered ? sy / 2.0F : 0.0F, sz / 2.0F);
        Piece p = new Piece(e, name, scale, pivot, light);
        p.put(pos, rot);
        level.addFreshEntity(e);
        LIVE.add(p);
        return p;
    }

    /** Quita todas las piezas de escena (las de esta sesión y las que hayan quedado guardadas en el mundo). */
    public static void clear(ServerLevel level) {
        for (Piece p : new ArrayList<>(LIVE)) {
            p.entity.discard();
        }
        LIVE.clear();
        for (Display.BlockDisplay d : level.getEntities(EntityType.BLOCK_DISPLAY, ARENA, (Entity e) -> e.getTags().contains(TAG))) {
            d.discard();
        }
    }
}
