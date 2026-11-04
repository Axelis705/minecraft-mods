package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Capa de tela. Se apoya en la misma física que la capa de Minecraft (el vaivén por el movimiento del cuerpo: adelante, de lado
 * y arriba/abajo, más el rebote al caminar y la inclinación al agacharse), pero en vez de una tabla rígida dibuja una malla de
 * {@value #NX} x {@value #NY} cuadros: la tela se curva hacia atrás al correr, se ensancha hacia el borde y le corren ondas
 * (más grandes cuanto más rápido vas, y una brisa suave aunque estés quieto). Lleva cara de fuera y forro por dentro, con las
 * normales de la superficie ondulada para que la luz marque los pliegues, y una segunda pasada brillante (textura *_glow).
 */
public class CapeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Cuadros de la malla (a lo ancho y a lo largo). */
    /** Piezas 3D pegadas a la tela (etapas 118-129). Desactivadas a petición: las capas vuelven a ser solo tela pintada. Poner true para recuperarlas. */
    static final boolean ACCESSORIES = false;
    static final int NX = 12, NY = 18;
    /** Medidas en bloques: ancho arriba y abajo, largo. */
    static final float TOP_W = 0.60F, BOT_W = 0.80F, LEN = 1.12F;
    /** Separación entre la tela de fuera y el forro. */
    static final float HALF_THICK = 0.0035F;
    /** Distancia mínima a la espalda: la capa vanilla va a 0,125 (la mitad del grosor del cuerpo); la capa del mod pega a eso + un poco. */
    static final float BACK_MIN = 0.14F;
    /** Regiones de la textura (128 x 128): fuera a la izquierda, forro a la derecha, 64 x 96 cada una. */
    static final float TEX = 128.0F, RW = 64.0F, RH = 96.0F;

    /** Estado de los muelles del vaivén lateral de cada jugador: borde (pos, vel), parte media (pos, vel) y el último instante. */
    private static final java.util.Map<java.util.UUID, float[]> SWING = new java.util.HashMap<>();

    public CapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        String id = ClientCapes.of(p.getUUID());
        if (id == null || p.isInvisible() || p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        // --- distancia a la espalda según la armadura: sin peto casi pegada; con peto, justo por fuera de él (y del faldón más abajo)
        float backTop = BACK_MIN, backLow = BACK_MIN;
        net.minecraft.world.item.ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        net.minecraft.world.item.ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        if (chest.getItem() instanceof com.mundosdeaxelis.gear.SetArmor sa) {
            backTop = Math.max(backTop, 0.137F + SetArmorModels.backExtent(sa.set(), false, true));
            backLow = Math.max(backLow, 0.137F + SetArmorModels.backExtent(sa.set(), false, false));
        } else if (!chest.isEmpty()) {
            backTop = Math.max(backTop, 0.20F);
            backLow = Math.max(backLow, 0.20F);
        }
        if (legs.getItem() instanceof com.mundosdeaxelis.gear.SetArmor sl) {
            backLow = Math.max(backLow, 0.137F + SetArmorModels.backExtent(sl.set(), true, false));
        } else if (!legs.isEmpty()) {
            backLow = Math.max(backLow, 0.17F);
        }
        // piezas pegadas al cuerpo (hombreras, collar...): en el sistema de coordenadas del torso, así siguen al agacharse
        float[][] acc = ACCESSORIES ? CapeAccessories.DATA.get(id) : null;
        if (acc != null) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            drawAccessories(buffers, pose, acc, id, light, null, null, 1);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0.0F, 0.0F, backTop);
        // --- misma física que la capa de Minecraft
        double d0 = Mth.lerp(partialTick, p.xCloakO, p.xCloak) - Mth.lerp(partialTick, p.xo, p.getX());
        double d1 = Mth.lerp(partialTick, p.yCloakO, p.yCloak) - Mth.lerp(partialTick, p.yo, p.getY());
        double d2 = Mth.lerp(partialTick, p.zCloakO, p.zCloak) - Mth.lerp(partialTick, p.zo, p.getZ());
        float yaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
        double sin = Mth.sin(yaw * ((float) Math.PI / 180F));
        double cos = -Mth.cos(yaw * ((float) Math.PI / 180F));
        float up = Mth.clamp((float) d1 * 10.0F, -6.0F, 32.0F);
        float fwd = Mth.clamp((float) (d0 * sin + d2 * cos) * 100.0F, 0.0F, 150.0F);
        float side = Mth.clamp((float) (d0 * cos - d2 * sin) * 100.0F, -20.0F, 20.0F);
        float bob = Mth.lerp(partialTick, p.oBob, p.bob);
        float walk = Mth.lerp(partialTick, p.walkDistO, p.walkDist);
        up += Mth.sin(walk * 6.0F) * 32.0F * bob;
        if (p.isCrouching()) {
            up += 25.0F;
        }
        // --- vaivén lateral con inercia: dos muelles (borde y parte media) que siguen el movimiento de lado y los giros del cuerpo
        float turn = Mth.wrapDegrees(p.yBodyRot - p.yBodyRotO);
        float target = Mth.clamp(side * 0.0105F - turn * 0.007F, -0.32F, 0.32F);
        float[] st = SWING.computeIfAbsent(p.getUUID(), k -> new float[7]);
        if (SWING.size() > 96) {
            SWING.clear();
        }
        float dtTot = ageInTicks - st[4];
        if (dtTot < 0.0F || dtTot > 8.0F) {
            st[0] = st[1] = st[2] = st[3] = st[5] = st[6] = 0.0F;
            dtTot = 0.0F;
        }
        st[4] = ageInTicks;
        // elevación por estar en el aire: en el suelo 0; saltando o cayendo la capa flota hacia arriba (más al caer). Va con un muelle
        // blando (se levanta con retraso, rebota un poco y baja despacio) para que no se mueva como una tabla.
        float vy = (float) p.getDeltaMovement().y;
        boolean airborne = !(p.onGround() || p.isInWater() || p.onClimbable());
        float airTarget = airborne ? 12.0F + Mth.clamp(-vy * 50.0F, 0.0F, 26.0F) + Mth.clamp(vy * 25.0F, 0.0F, 10.0F) : 0.0F;
        int sub = Math.max(1, (int) Math.ceil(dtTot / 0.5F));
        float dt = dtTot / sub;
        for (int k = 0; k < sub; k++) {
            st[1] += (-0.26F * (st[0] - target) - 0.30F * st[1]) * dt;   // borde: lento y con rebote
            st[0] += st[1] * dt;
            st[3] += (-0.55F * (st[2] - target) - 0.45F * st[3]) * dt;   // parte media: más rápida
            st[2] += st[3] * dt;
            st[6] += (-0.22F * (st[5] - airTarget) - 0.42F * st[6]) * dt;  // elevación en el aire: blanda y con un poco de rebote
            st[5] += st[6] * dt;
        }
        // Elevación de la tela. En Minecraft normal sería 6 + fwd/2 + up; al volar o caer rápido pasa de 100 grados y la capa se voltea sobre la
        // cabeza, así que pasado cierto punto sube mucho menos. Solo una parte del giro es rígido: el resto curva la tela (la punta se va atrás).
        float lift = fwd / 2.0F + up + Math.max(0.0F, st[5]);
        float soft = Math.max(-6.0F, lift > 42.0F ? 42.0F + (lift - 42.0F) * 0.22F : lift);
        float liftBend = Mth.DEG_TO_RAD * 0.62F * Math.max(soft, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(5.0F + 0.32F * soft));
        pose.mulPose(Axis.ZP.rotationDegrees(side / 2.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - side / 2.0F));
        float swingEdge = st[0], swingMid = st[2] - st[0];

        // --- forma de la tela
        float speed = Mth.clamp(fwd / 50.0F, 0.0F, 2.2F);
        float t = ageInTicks;
        float phase = t * 0.09F + walk * 1.9F;
        float[][] px = new float[NY + 1][NX + 1], py = new float[NY + 1][NX + 1], pz = new float[NY + 1][NX + 1];
        float cy = 0.0F, cz = 0.0F;
        float step = LEN / NY;
        float breath = 0.04F * Mth.sin(t * 0.07F);
        for (int j = 0; j <= NY; j++) {
            float v = j / (float) NY;
            if (j > 0) {
                float vm = (j - 0.5F) / NY;
                // la tela se curva hacia atrás con la velocidad y respira un poco
                float bend = (0.03F + 0.06F * Math.min(speed, 1.5F)) * (float) Math.pow(vm, 1.5) + liftBend * (float) Math.pow(vm, 1.2) + breath * vm;
                cy += step * Mth.cos(bend);
                cz -= step * Mth.sin(bend);
            }
            float width = Mth.lerp((float) Math.pow(v, 0.8), TOP_W, BOT_W);
            float amp = (0.014F + 0.050F * speed) * (float) Math.pow(v, 1.2);
            // curva en S: el borde se queda atrás y la parte media se adelanta, y se acorta un poco al columpiarse
            float sway = swingEdge * (float) Math.pow(v, 1.8) + swingMid * 0.7F * Mth.sin((float) Math.PI * v)
                    + Mth.sin(phase * 0.5F + v * 2.0F) * 0.010F * (0.3F + speed) * v * v;
            float arc = (swingEdge * swingEdge) * 0.5F * v * v / LEN;
            for (int i = 0; i <= NX; i++) {
                float u = i / (float) NX - 0.5F;
                float wave = Mth.sin(u * 10.0F + phase + side * 0.03F * v - v * 7.0F) + 0.45F * Mth.sin(u * 19.0F - phase * 1.35F + v * 9.0F);
                // pliegues fijos (la tela se junta en el cuello y se abre hacia abajo) + ondas que corren
                float out = amp * (1.4F + wave) + 0.008F * v * (1.0F + Mth.sin(u * 13.0F + 1.0F));
                // más pegada arriba (sobre el peto) y, si hay faldón o botas, un poco más separada abajo
                float lowShift = (backLow - backTop) * v * v * (3.0F - 2.0F * v);
                px[j][i] = u * width + sway;
                py[j][i] = cy - wave * amp * 0.12F - arc;
                pz[j][i] = cz - out - lowShift;
            }
        }
        // normales de la superficie (hacia fuera = -z)
        float[][][] nrm = new float[NY + 1][NX + 1][3];
        for (int j = 0; j <= NY; j++) {
            for (int i = 0; i <= NX; i++) {
                int i0 = Math.max(0, i - 1), i1 = Math.min(NX, i + 1), j0 = Math.max(0, j - 1), j1 = Math.min(NY, j + 1);
                float ax = px[j][i1] - px[j][i0], ay = py[j][i1] - py[j][i0], az = pz[j][i1] - pz[j][i0];
                float bx = px[j1][i] - px[j0][i], by = py[j1][i] - py[j0][i], bz = pz[j1][i] - pz[j0][i];
                // a x b apunta hacia +z (adentro) con x a la derecha e y hacia abajo: se invierte
                float nx = -(ay * bz - az * by), ny = -(az * bx - ax * bz), nzz = -(ax * by - ay * bx);
                float len = Mth.sqrt(nx * nx + ny * ny + nzz * nzz);
                if (len < 1.0E-5F) {
                    nx = 0;
                    ny = 0;
                    nzz = -1;
                    len = 1;
                }
                nrm[j][i][0] = nx / len;
                nrm[j][i][1] = ny / len;
                nrm[j][i][2] = nzz / len;
            }
        }
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ClientCapes.texture(id)));
        surface(vc, m, nm, px, py, pz, nrm, light, HALF_THICK, 1.0F, 0.0F, false);
        surface(vc, m, nm, px, py, pz, nrm, light, -HALF_THICK, -1.0F, RW, true);
        VertexConsumer gc = buffers.getBuffer(RenderType.eyes(ClientCapes.glow(id)));
        surface(gc, m, nm, px, py, pz, nrm, LightTexture.FULL_BRIGHT, HALF_THICK * 2.0F, 1.0F, 0.0F, false);
        if (acc != null) {
            float[][][][] fr = frames(px, py, pz, nrm);
            drawAccessories(buffers, pose, acc, id, light, new float[][][]{px, py, pz}, fr, 0);
        }
        pose.popPose();
    }

    /** Ejes (derecha, abajo, fuera) de cada punto de la malla, para pegarle piezas a la tela. [j][i][0..2][x,y,z]. */
    private static float[][][][] frames(float[][] px, float[][] py, float[][] pz, float[][][] nrm) {
        float[][][][] f = new float[NY + 1][NX + 1][3][3];
        for (int j = 0; j <= NY; j++) {
            for (int i = 0; i <= NX; i++) {
                int i0 = Math.max(0, i - 1), i1 = Math.min(NX, i + 1), j0 = Math.max(0, j - 1), j1 = Math.min(NY, j + 1);
                float[] n = nrm[j][i];
                float[] b = {px[j1][i] - px[j0][i], py[j1][i] - py[j0][i], pz[j1][i] - pz[j0][i]};
                float[] a = {px[j][i1] - px[j][i0], py[j][i1] - py[j][i0], pz[j][i1] - pz[j][i0]};
                float[] d = ortho(b, n, null);
                float[] r = ortho(a, n, d);
                f[j][i][0] = r;
                f[j][i][1] = d;
                f[j][i][2] = n;
            }
        }
        return f;
    }

    /** v menos sus componentes sobre n (y sobre m si no es null), normalizado. */
    private static float[] ortho(float[] v, float[] n, float[] m) {
        float dn = v[0] * n[0] + v[1] * n[1] + v[2] * n[2];
        float[] o = {v[0] - dn * n[0], v[1] - dn * n[1], v[2] - dn * n[2]};
        if (m != null) {
            float dm = o[0] * m[0] + o[1] * m[1] + o[2] * m[2];
            o[0] -= dm * m[0];
            o[1] -= dm * m[1];
            o[2] -= dm * m[2];
        }
        float len = Mth.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]);
        if (len < 1.0E-6F) {
            return new float[]{m == null ? 0 : 1, m == null ? 1 : 0, 0};
        }
        return new float[]{o[0] / len, o[1] / len, o[2] / len};
    }

    private static final float K = 1.0F / 16.0F;
    private static final int[][] FACE_N = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final int[][][] FACE_C = {
            {{1, -1, -1}, {1, 1, -1}, {1, 1, 1}, {1, -1, 1}}, {{-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}, {-1, -1, -1}},
            {{-1, 1, -1}, {-1, 1, 1}, {1, 1, 1}, {1, 1, -1}}, {{-1, -1, 1}, {-1, -1, -1}, {1, -1, -1}, {1, -1, 1}},
            {{-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}}, {{1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}}};

    /** Gira un punto: primero z, luego x, luego y (grados). */
    private static float[] rotE(float x, float y, float z, float rx, float ry, float rz) {
        float c = Mth.cos(rz * Mth.DEG_TO_RAD), s = Mth.sin(rz * Mth.DEG_TO_RAD);
        float x1 = x * c - y * s, y1 = x * s + y * c;
        c = Mth.cos(rx * Mth.DEG_TO_RAD);
        s = Mth.sin(rx * Mth.DEG_TO_RAD);
        float y2 = y1 * c - z * s, z2 = y1 * s + z * c;
        c = Mth.cos(ry * Mth.DEG_TO_RAD);
        s = Mth.sin(ry * Mth.DEG_TO_RAD);
        return new float[]{x1 * c + z2 * s, y2, -x1 * s + z2 * c};
    }

    /**
     * Piezas 3D de la capa (cubos). {@code kind} 0 = las pegadas a la tela (con sus ejes en {@code fr}), 1 = las pegadas al cuerpo.
     * La textura es la propia de la capa: cada color es una celda de 4x4 píxeles bajo la tela.
     */
    private static void drawAccessories(MultiBufferSource buffers, PoseStack pose, float[][] rows, String id, int light, float[][][] pos, float[][][][] fr, int kind) {
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ClientCapes.texture(id)));
        VertexConsumer gc = null;
        for (float[] r : rows) {
            if ((int) r[0] != kind || (kind == 0 && fr == null)) {
                continue;
            }
            boolean glow = r[13] > 0.5F;
            if (glow && gc == null) {
                gc = buffers.getBuffer(RenderType.eyes(ClientCapes.glow(id)));
            }
            float[] A = null, R = null, D = null, N = null;
            if (kind == 0) {
                float jf = Mth.clamp(r[1], 0.0F, 1.0F) * NY, iff = (Mth.clamp(r[2], -0.5F, 0.5F) + 0.5F) * NX;
                int j0 = Math.min((int) jf, NY - 1), i0 = Math.min((int) iff, NX - 1);
                float fj = jf - j0, fi = iff - i0;
                A = new float[3];
                R = new float[3];
                D = new float[3];
                N = new float[3];
                for (int c = 0; c < 3; c++) {
                    float[] ps = {fr[j0][i0][0][c], fr[j0][i0 + 1][0][c], fr[j0 + 1][i0][0][c], fr[j0 + 1][i0 + 1][0][c]};
                    float[] ds = {fr[j0][i0][1][c], fr[j0][i0 + 1][1][c], fr[j0 + 1][i0][1][c], fr[j0 + 1][i0 + 1][1][c]};
                    float[] ns = {fr[j0][i0][2][c], fr[j0][i0 + 1][2][c], fr[j0 + 1][i0][2][c], fr[j0 + 1][i0 + 1][2][c]};
                    R[c] = bil(ps, fi, fj);
                    D[c] = bil(ds, fi, fj);
                    N[c] = bil(ns, fi, fj);
                }
                A[0] = bil(new float[]{pos[0][j0][i0], pos[0][j0][i0 + 1], pos[0][j0 + 1][i0], pos[0][j0 + 1][i0 + 1]}, fi, fj);
                A[1] = bil(new float[]{pos[1][j0][i0], pos[1][j0][i0 + 1], pos[1][j0 + 1][i0], pos[1][j0 + 1][i0 + 1]}, fi, fj);
                A[2] = bil(new float[]{pos[2][j0][i0], pos[2][j0][i0 + 1], pos[2][j0 + 1][i0], pos[2][j0 + 1][i0 + 1]}, fi, fj);
                N = norm(N);
                D = ortho(D, N, null);
                R = ortho(R, N, D);
            }
            float cx = (r[3] + r[6] / 2.0F) * K, cy = (r[4] + r[7] / 2.0F) * K, cz = (r[5] + r[8] / 2.0F) * K;
            float hx = r[6] * K / 2.0F, hy = r[7] * K / 2.0F, hz = r[8] * K / 2.0F;
            int sw = (int) r[12];
            float u0 = ((sw % 32) * 4 + 0.5F) / TEX, u1 = ((sw % 32) * 4 + 3.5F) / TEX;
            float v0 = (96 + (sw / 32) * 4 + 0.25F) / TEX, v1 = (96 + (sw / 32) * 4 + 3.75F) / TEX;
            for (int pass = 0; pass < (glow ? 2 : 1); pass++) {
                VertexConsumer o2 = pass == 0 ? vc : gc;
                int l2 = pass == 0 ? light : LightTexture.FULL_BRIGHT;
                for (int f = 0; f < 6; f++) {
                    float[] nl = rotE(FACE_N[f][0], FACE_N[f][1], FACE_N[f][2], r[9], r[10], r[11]);
                    float[] nw = kind == 0
                            ? new float[]{R[0] * nl[0] + D[0] * nl[1] + N[0] * nl[2], R[1] * nl[0] + D[1] * nl[1] + N[1] * nl[2], R[2] * nl[0] + D[2] * nl[1] + N[2] * nl[2]}
                            : nl;
                    for (int c = 0; c < 4; c++) {
                        int[] s = FACE_C[f][c];
                        float[] o = rotE(s[0] * hx, s[1] * hy, s[2] * hz, r[9], r[10], r[11]);
                        float lx = cx + o[0], ly = cy + o[1], lz = cz + o[2];
                        float wx, wy, wz;
                        if (kind == 0) {
                            wx = A[0] + R[0] * lx + D[0] * ly + N[0] * lz;
                            wy = A[1] + R[1] * lx + D[1] * ly + N[1] * lz;
                            wz = A[2] + R[2] * lx + D[2] * ly + N[2] * lz;
                        } else {
                            wx = lx;
                            wy = ly;
                            wz = lz;
                        }
                        float cs = glow ? (pass == 0 ? 0.55F : 0.70F) : 1.0F;
                        o2.vertex(m, wx, wy, wz).color(cs, cs, cs, 1.0F).uv(c == 0 || c == 3 ? u0 : u1, c < 2 ? v0 : v1)
                                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l2).normal(nm, nw[0], nw[1], nw[2]).endVertex();
                    }
                }
            }
        }
    }

    private static float bil(float[] c, float fi, float fj) {
        return (c[0] * (1 - fi) + c[1] * fi) * (1 - fj) + (c[2] * (1 - fi) + c[3] * fi) * fj;
    }

    private static float[] norm(float[] v) {
        float len = Mth.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return len < 1.0E-6F ? new float[]{0, 0, -1} : new float[]{v[0] / len, v[1] / len, v[2] / len};
    }

    /**
     * Una cara de la tela. {@code offset} la separa de la superficie a lo largo de su normal (fuera + / forro -),
     * {@code ns} da vuelta la normal para el forro y {@code u0} elige la región de la textura (0 fuera, 64 forro).
     */
    private static void surface(VertexConsumer vc, Matrix4f m, Matrix3f nm, float[][] px, float[][] py, float[][] pz, float[][][] nrm,
                                int light, float offset, float ns, float u0, boolean mirror) {
        for (int j = 0; j < NY; j++) {
            for (int i = 0; i < NX; i++) {
                int[][] c = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] k : c) {
                    int ci = k[0], cj = k[1];
                    float[] n = nrm[cj][ci];
                    float fu = ci / (float) NX;
                    if (mirror) {
                        fu = 1.0F - fu;
                    }
                    float uu = (u0 + fu * RW) / TEX;
                    float vv = cj / (float) NY * RH / TEX;
                    vc.vertex(m, px[cj][ci] + n[0] * offset, py[cj][ci] + n[1] * offset, pz[cj][ci] + n[2] * offset)
                            .color(1.0F, 1.0F, 1.0F, 1.0F).uv(uu, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                            .normal(nm, n[0] * ns, n[1] * ns, n[2] * ns).endVertex();
                }
            }
        }
    }

    /** Añade la capa a los dos modelos de jugador (normal y delgado). */
    @Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        /** Con una capa del mod puesta, la capa normal de Minecraft (la de la skin) no se dibuja: se envuelve su capa de dibujo. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void hideVanillaCape(PlayerRenderer pr) {
            try {
                java.util.List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers =
                        net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                                net.minecraft.client.renderer.entity.LivingEntityRenderer.class, pr, "layers");
                for (int i = 0; i < layers.size(); i++) {
                    RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> l = layers.get(i);
                    if (l instanceof net.minecraft.client.renderer.entity.layers.CapeLayer vanilla) {
                        layers.set(i, new HideWhenModCape(pr, vanilla));
                    }
                }
            } catch (RuntimeException e) {
                MundosDeAxelis.LOGGER.warn("No se pudo ocultar la capa normal al llevar una capa del mod", e);
            }
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                if (event.getPlayerSkin(skin) instanceof PlayerRenderer pr) {
                    pr.addLayer(new CapeLayer(pr));
                    hideVanillaCape(pr);
                }
            }
        }
    }

    /** Envuelve la capa normal de Minecraft y solo la dibuja si el jugador no lleva una capa del mod. */
    static final class HideWhenModCape extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> inner;

        HideWhenModCape(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                        RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> inner) {
            super(parent);
            this.inner = inner;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (ClientCapes.of(p.getUUID()) == null) {
                inner.render(pose, buffers, light, p, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        }
    }
}
