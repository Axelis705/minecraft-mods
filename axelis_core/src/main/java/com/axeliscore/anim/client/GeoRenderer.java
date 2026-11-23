package com.axeliscore.anim.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dibuja un {@link Geo} en una {@link Pose}. Arma cada cubo igual que ModelPart de Minecraft (mismas caras, mismo UV de caja, mismo
 * espejo), así una textura hecha para un modelo normal se ve igual. Además:
 * <ul>
 *     <li>Doblez suave: un cubo con doblez (codo, rodilla, cintura) se parte en dos a la altura de la articulación; la mitad del lado
 *     del hijo lo sigue y la línea de la articulación gira a la mitad, así se dobla sin huecos ni cortes.</li>
 *     <li>Pasadas: los huesos con brillo se dibujan aparte (con un pincel que brilla en la oscuridad).</li>
 *     <li>Huesos ocultos y color/transparencia por llamada (para el destello al recibir daño, fantasmas, etc.).</li>
 * </ul>
 */
public final class GeoRenderer {
    private GeoRenderer() {
    }

    /** Qué huesos dibujar en esta pasada. */
    public enum Pass {
        /** Los normales (con luz del mundo). */
        NORMAL,
        /** Solo los que brillan. */
        GLOW,
        /** Todos (por ejemplo, una silueta). */
        ALL
    }

    /** Matrices de cada hueso para un cuadro (se reusan para no crear objetos cada vez). */
    public static final class Frame {
        final Matrix4f[] world;
        final Matrix3f[] normal;
        /** Matriz "a medio doblar" del hijo de un doblez (en el espacio del hijo). */
        final Matrix4f[] half;
        final Matrix3f[] halfN;
        final PoseStack.Pose[] poses, halfPoses;
        final PoseStack stack = new PoseStack();

        public Frame(Geo geo) {
            int n = geo.bones.length;
            world = new Matrix4f[n];
            normal = new Matrix3f[n];
            half = new Matrix4f[n];
            halfN = new Matrix3f[n];
            poses = new PoseStack.Pose[n];
            halfPoses = new PoseStack.Pose[n];
            for (int i = 0; i < n; i++) {
                world[i] = new Matrix4f();
                normal[i] = new Matrix3f();
                half[i] = new Matrix4f();
                halfN[i] = new Matrix3f();
            }
        }
    }

    /**
     * Calcula las matrices de todos los huesos a partir de la pose actual de {@code ps} (que ya debe estar en el espacio del modelo de
     * Minecraft: girada como la entidad y con scale(-1, -1, 1) y la subida de 1.501, igual que un LivingEntityRenderer).
     */
    public static void compute(Geo geo, Pose pose, PoseStack ps, Frame f) {
        Matrix4f base = ps.last().pose();
        Matrix3f baseN = ps.last().normal();
        for (int r : geo.roots) {
            bone(geo, pose, r, base, baseN, 0, 0, 0, f);
        }
        // cada hueso necesita su PoseStack.Pose (en 1.20 no se pueden crear sueltas): se piden a una pila propia, que se vacía en cada cuadro
        PoseStack st = f.stack;
        while (!atBase(st)) {
            st.popPose();
        }
        for (int i = 0; i < geo.bones.length; i++) {
            st.pushPose();
            st.last().pose().set(f.world[i]);
            st.last().normal().set(f.normal[i]);
            f.poses[i] = st.last();
            st.pushPose();
            st.last().pose().set(f.half[i]);
            st.last().normal().set(f.halfN[i]);
            f.halfPoses[i] = st.last();
        }
    }

    /** ¿La pila solo tiene la entrada de base? */
    private static boolean atBase(PoseStack st) {
        //#if MC >= 1.21.2
        //$$ return st.isEmpty();
        //#else
        return st.clear();
        //#endif
    }

    private static void bone(Geo geo, Pose pose, int b, Matrix4f parent, Matrix3f parentN, float ppx, float ppy, float ppz, Frame f) {
        Geo.Bone bone = geo.bones[b];
        int o = b * 3;
        float tx = (bone.px - ppx + pose.pos[o]) / 16F;
        float ty = (bone.py - ppy + pose.pos[o + 1]) / 16F;
        float tz = (bone.pz - ppz + pose.pos[o + 2]) / 16F;
        float rx = bone.rx + pose.rot[o], ry = bone.ry + pose.rot[o + 1], rz = bone.rz + pose.rot[o + 2];
        float sx = pose.scl[o], sy = pose.scl[o + 1], sz = pose.scl[o + 2];
        Quaternionf q = new Quaternionf().rotationZYX(rz, ry, rx);
        Matrix4f w = f.world[b].set(parent).translate(tx, ty, tz).rotate(q).scale(sx, sy, sz);
        Matrix3f n = f.normal[b].set(parentN).rotate(q);
        if (sx != 1 || sy != 1 || sz != 1) {
            // como PoseStack.scale: la normal se escala al revés (y luego se normaliza al dibujar)
            n.scale(1 / nz(sx), 1 / nz(sy), 1 / nz(sz));
        }
        // a medio doblar: misma posición, la mitad del giro (para la franja de la articulación del padre)
        Quaternionf h = new Quaternionf().slerp(q, 0.5F);
        f.half[b].set(parent).translate(tx, ty, tz).rotate(h);
        f.halfN[b].set(parentN).rotate(h);
        for (int c : bone.children) {
            bone(geo, pose, c, w, n, bone.px, bone.py, bone.pz, f);
        }
    }

    private static float nz(float v) {
        return Math.abs(v) < 1e-4F ? 1e-4F : v;
    }

    /**
     * Dibuja los cubos (después de {@link #compute}). {@code argb}: color que multiplica la textura (0xFFFFFFFF = normal).
     */
    public static void draw(Geo geo, Pose pose, Frame f, VB vb, Pass pass, int light, int overlay, int argb) {
        int a = (argb >>> 24) & 255, r = (argb >> 16) & 255, g = (argb >> 8) & 255, bl = argb & 255;
        for (int b = 0; b < geo.bones.length; b++) {
            Geo.Bone bone = geo.bones[b];
            if (hiddenUp(geo, pose, b) || bone.cubes.isEmpty()) {
                continue;
            }
            if (pass == Pass.NORMAL && bone.glow || pass == Pass.GLOW && !bone.glow) {
                continue;
            }
            int tb = pose.tinte[b];
            int ra = r, ga = g, ba = bl, aa = a;
            if (tb != 0xFFFFFFFF) {
                aa = a * ((tb >>> 24) & 255) / 255;
                ra = r * ((tb >> 16) & 255) / 255;
                ga = g * ((tb >> 8) & 255) / 255;
                ba = bl * (tb & 255) / 255;
            }
            for (Geo.Cube c : bone.cubes) {
                cube(geo, bone, c, f, b, vb, light, overlay, ra, ga, ba, aa, pose.du[b], pose.dv[b]);
            }
        }
    }

    private static boolean hiddenUp(Geo geo, Pose pose, int b) {
        while (b >= 0) {
            if (pose.hidden[b]) {
                return true;
            }
            b = geo.bones[b].parent;
        }
        return false;
    }

    // ------------------------------------------------------------------ un cubo, como ModelPart.Cube

    private static final int DOWN = 0, UP = 1, WEST = 2, NORTH = 3, EAST = 4, SOUTH = 5;
    private static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};

    private static void cube(Geo geo, Geo.Bone bone, Geo.Cube c, Frame f, int b, VB vb, int light, int ov, int r, int g, int bl, int a, float du,
            float dv) {
        float i = c.inflate;
        float x0 = c.x0 - i, y0 = c.y0 - i, z0 = c.z0 - i;
        float x1 = c.x0 + c.sx + i, y1 = c.y0 + c.sy + i, z1 = c.z0 + c.sz + i;
        if (c.mirror) {
            float t = x1;
            x1 = x0;
            x0 = t;
        }
        // las 8 esquinas, con los mismos nombres que en ModelPart.Cube
        float[][] v = {
                {x1, y0, z0}, // 0 (v)
                {x1, y1, z0}, // 1
                {x0, y1, z0}, // 2
                {x0, y0, z1}, // 3
                {x1, y0, z1}, // 4
                {x1, y1, z1}, // 5
                {x0, y1, z1}, // 6
                {x0, y0, z0}, // 7
        };
        int[][] quads = {{4, 3, 7, 0}, {1, 2, 6, 5}, {7, 3, 6, 2}, {0, 7, 2, 1}, {4, 0, 1, 5}, {3, 4, 5, 6}};
        float[][] uv = new float[6][];
        if (c.faces != null) {
            for (int k = 0; k < 6; k++) {
                uv[k] = c.faces[k];
            }
        } else {
            float dx = c.sx, dy = c.sy, dz = c.sz, u = c.u, w = c.v;
            float f4 = u, f5 = u + dz, f6 = u + dz + dx, f7 = u + dz + dx + dx, f8 = u + dz + dx + dz, f9 = u + dz + dx + dz + dx;
            float f10 = w, f11 = w + dz, f12 = w + dz + dy;
            uv[DOWN] = new float[] {f5, f10, f6, f11};
            uv[UP] = new float[] {f6, f11, f7, f10};
            uv[WEST] = new float[] {f4, f11, f5, f12};
            uv[NORTH] = new float[] {f5, f11, f6, f12};
            uv[EAST] = new float[] {f6, f11, f8, f12};
            uv[SOUTH] = new float[] {f8, f11, f9, f12};
        }
        float tw = geo.texW, th = geo.texH;
        boolean bend = c.bendBone >= 0;
        Geo.Bone child = bend ? geo.bones[c.bendBone] : null;
        for (int k = 0; k < 6; k++) {
            float[] fu = uv[k];
            if (fu == null) {
                continue;
            }
            // Polygon: vértice 0 = (u2, v1), 1 = (u1, v1), 2 = (u1, v2), 3 = (u2, v2)
            float[][] q = new float[4][];
            float[][] t = {{fu[2] + du, fu[1] + dv}, {fu[0] + du, fu[1] + dv}, {fu[0] + du, fu[3] + dv}, {fu[2] + du, fu[3] + dv}};
            for (int m = 0; m < 4; m++) {
                q[m] = v[quads[k][m]];
            }
            int[] order = c.mirror ? new int[] {3, 2, 1, 0} : new int[] {0, 1, 2, 3};
            float nx = NORMALS[k][0] * (c.mirror ? -1 : 1), ny = NORMALS[k][1], nzz = NORMALS[k][2];
            if (!bend) {
                PoseStack.Pose p = f.poses[b];
                for (int m : order) {
                    put(vb, p, bone, c, q[m], t[m][0] / tw, t[m][1] / th, nx, ny, nzz, light, ov, r, g, bl, a, null);
                }
                continue;
            }
            // con doblez: la parte del cubo del lado del punto de giro del hueso se queda con él; la del otro lado sigue al hijo (el
            // antebrazo bajo el codo, o el pecho sobre la cintura). Las caras de los extremos van enteras; los lados se parten.
            float jy = c.bendY;
            boolean childBelow = bone.py < jy; // y crece hacia abajo: el hombro (arriba) deja al hijo abajo; la cintura, arriba
            PoseStack.Pose own = f.poses[b], kid = f.poses[c.bendBone], mid = f.halfPoses[c.bendBone];
            if (k == DOWN || k == UP) {
                // DOWN es la cara de arriba del cubo (y menor); UP la de abajo
                boolean toKid = (k == UP) == childBelow;
                for (int m : order) {
                    put(vb, toKid ? kid : own, toKid ? child : bone, c, q[m], t[m][0] / tw, t[m][1] / th, nx, ny, nzz, light, ov, r, g, bl, a,
                            null);
                }
                continue;
            }
            float vTop = fu[1] + dv, vBot = fu[3] + dv;
            float span = Math.max(1e-4F, y1 - y0);
            float vJ = vTop + (vBot - vTop) * ((jy - y0) / span);
            // dos mitades: la de arriba (de y0 a la articulación) y la de abajo (de la articulación a y1)
            for (int half = 0; half < 2; half++) {
                boolean upper = half == 0;
                boolean halfIsKid = upper != childBelow;
                for (int m : order) {
                    boolean atTop = q[m][1] < jy;
                    // el vértice que queda del otro lado de esta mitad se mueve a la articulación (a medio doblar)
                    boolean moved = upper != atTop;
                    float[] pos = moved ? new float[] {q[m][0], jy, q[m][2]} : q[m];
                    float vv = moved ? vJ : t[m][1];
                    if (moved) {
                        put(vb, mid, child, c, pos, t[m][0] / tw, vv / th, nx, ny, nzz, light, ov, r, g, bl, a, null);
                    } else {
                        put(vb, halfIsKid ? kid : own, halfIsKid ? child : bone, c, pos, t[m][0] / tw, vv / th, nx, ny, nzz, light, ov, r, g,
                                bl, a, null);
                    }
                }
            }
        }
    }

    private static final Vector3f TMP = new Vector3f();

    /** Un vértice: posición del modelo (píxeles) relativa al punto de giro del hueso {@code rel}. */
    private static void put(VB vb, PoseStack.Pose p, Geo.Bone rel, Geo.Cube c, float[] mp, float u, float w, float nx, float ny, float nz,
            int light, int ov, int r, int g, int bl, int a, Object unused) {
        float x = mp[0], y = mp[1], z = mp[2];
        if (c.rotated()) {
            Vector3f d = TMP.set(x - c.cpx, y - c.cpy, z - c.cpz);
            Quaternionf q = new Quaternionf().rotationZYX(c.crz, c.cry, c.crx);
            q.transform(d);
            x = d.x + c.cpx;
            y = d.y + c.cpy;
            z = d.z + c.cpz;
            Vector3f nn = q.transform(new Vector3f(nx, ny, nz));
            nx = nn.x;
            ny = nn.y;
            nz = nn.z;
        }
        vb.v(p, (x - rel.px) / 16F, (y - rel.py) / 16F, (z - rel.pz) / 16F, r, g, bl, a, u, w, ov, light, nx, ny, nz);
    }
}
