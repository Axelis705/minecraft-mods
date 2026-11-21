package com.mundosdeaxelis.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.npc.NpcKinds;
import com.mundosdeaxelis.npc.NpcService;
import com.mundosdeaxelis.npc.SemillaCasaBlock;
import com.mundosdeaxelis.npc.SemillaCasaBlockEntity;
import com.mundosdeaxelis.world.StructBp;
import com.mundosdeaxelis.world.StructDesigns;
import com.mundosdeaxelis.world.StructPlacer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Holograma de la Semilla de casa (etapa 260): dibuja la casa entera del habitante como bloques de luz azulada en el sitio donde
 * crecerá, con un recuadro del tamaño total y un cartel encima (de quién es, cuánto mide y si cabe).
 * <ul>
 * <li>Rojo: un bloque del mundo que estorba (árbol, muro...): con algo en rojo la casa no crece.</li>
 * <li>Naranja: debajo del suelo de la casa hay un hueco de más de 3 bloques (terreno muy desigual).</li>
 * </ul>
 * Lo que estorba se revisa en el cliente cada medio segundo con las mismas reglas que usa el servidor al construir.
 */
public class SemillaCasaRenderer implements BlockEntityRenderer<SemillaCasaBlockEntity> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final float[] ONES = {1.0F, 1.0F, 1.0F, 1.0F};
    private static final int[] LIGHTS = {FULL, FULL, FULL, FULL};
    private static final Direction[] DIRS = Direction.values();
    private static final double MAX_DIST = 64.0;

    private final Font font;
    private final RandomSource rand = RandomSource.create();

    public SemillaCasaRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    // ------------------------------------------------------------------ el plano de la casa (girado), una vez por tipo y giro

    private record Ghost(BlockPos rel, BlockState state) {
    }

    private record Check(BlockPos rel, int localY) {
    }

    private static final class Plan {
        final List<Ghost> ghosts = new ArrayList<>();
        final List<Check> checks = new ArrayList<>();
        final List<BlockPos> columns = new ArrayList<>();
        final Set<BlockPos> solid = new HashSet<>();
        AABB box;
        int width, depth, height;
    }

    private static final Map<String, Plan> PLANS = new HashMap<>();

    private static Plan plan(NpcKinds.Kind k, Rotation rot) {
        String key = k.id() + "/" + rot.name();
        Plan cached = PLANS.get(key);
        if (cached != null) {
            return cached;
        }
        Plan p = new Plan();
        StructBp bp = StructDesigns.make(k.house());
        int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
        for (Map.Entry<Long, String> e : bp.m.entrySet()) {
            long key2 = e.getKey();
            int x = StructBp.kx(key2), y = StructBp.ky(key2), z = StructBp.kz(key2);
            BlockPos rel = new BlockPos(x, y, z).rotate(rot);
            p.checks.add(new Check(rel, y));
            if (y < -1) {
                continue; // cimientos bajo tierra: no se ven
            }
            BlockState st = StructPlacer.planned(e.getValue(), rot);
            if (st != null && !st.isAir()) {
                p.ghosts.add(new Ghost(rel, st));
                if (st.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                    p.solid.add(rel);
                }
                minX = Math.min(minX, rel.getX());
                minY = Math.min(minY, rel.getY());
                minZ = Math.min(minZ, rel.getZ());
                maxX = Math.max(maxX, rel.getX());
                maxY = Math.max(maxY, rel.getY());
                maxZ = Math.max(maxZ, rel.getZ());
            }
        }
        for (StructBp.Found f : bp.found) {
            for (int x = f.x0(); x <= f.x1(); x++) {
                for (int z = f.z0(); z <= f.z1(); z++) {
                    p.columns.add(new BlockPos(x, -2, z).rotate(rot));
                }
            }
        }
        if (minX == Integer.MAX_VALUE) {
            minX = minY = minZ = maxX = maxY = maxZ = 0;
        }
        p.box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        p.width = maxX - minX + 1;
        p.depth = maxZ - minZ + 1;
        p.height = maxY - Math.max(minY, 0) + 1;
        PLANS.put(key, p);
        return p;
    }

    // ------------------------------------------------------------------ qué estorba (mismas reglas que NpcService.checkSite)

    private static boolean soft(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    private static void scan(Level level, BlockPos seed, Plan p, SemillaCasaBlockEntity be) {
        Set<BlockPos> blocked = new HashSet<>();
        for (Check c : p.checks) {
            if (c.rel().equals(BlockPos.ZERO)) {
                continue; // la propia semilla
            }
            BlockPos w = seed.offset(c.rel());
            BlockState cur = level.getBlockState(w);
            boolean bad;
            if (level.getBlockEntity(w) != null && !cur.isAir()) {
                bad = true;
            } else if (c.localY() >= 0) {
                bad = !(cur.isAir() || (cur.canBeReplaced() && cur.getFluidState().isEmpty()));
            } else {
                bad = !cur.getFluidState().isEmpty();
            }
            if (bad) {
                blocked.add(c.rel());
            }
        }
        Set<BlockPos> uneven = new HashSet<>();
        for (BlockPos col : p.columns) {
            BlockPos c = seed.offset(col);
            int gap = 0;
            while (gap <= 3 && soft(level.getBlockState(c.below(gap)))) {
                gap++;
            }
            if (gap > 3) {
                uneven.add(col);
            }
        }
        be.scanBlocked = blocked;
        be.scanUneven = uneven;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(SemillaCasaBlockEntity be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        Minecraft mc = Minecraft.getInstance();
        if (level == null || mc.player == null) {
            return;
        }
        BlockPos seed = be.getBlockPos();
        if (mc.player.distanceToSqr(Vec3.atCenterOf(seed)) > MAX_DIST * MAX_DIST) {
            return;
        }
        NpcKinds.Kind k = NpcKinds.get(be.kind());
        BlockState self = be.getBlockState();
        if (k == null || !self.hasProperty(SemillaCasaBlock.FACING)) {
            return;
        }
        Rotation rot = NpcService.rotFor(self.getValue(SemillaCasaBlock.FACING));
        Plan p = plan(k, rot);
        long now = level.getGameTime();
        if (be.scanKey != p || now - be.scanTick >= 10 || now < be.scanTick) {
            scan(level, seed, p, be);
            be.scanTick = now;
            be.scanKey = p;
        }
        Set<BlockPos> blocked = be.scanBlocked;
        Set<BlockPos> uneven = be.scanUneven;
        float t = now + partial;
        float alpha = 0.34F + 0.08F * Mth.sin(t * 0.12F);

        // 1) la casa, bloque a bloque, en luz azulada (rojiza donde algo estorba)
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        VertexConsumer glass = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        ps.pushPose();
        ps.translate(0.0, 0.02, 0.0); // un pelín arriba: el suelo de la casa no parpadea con el del mundo
        for (Ghost g : p.ghosts) {
            if (g.state().getRenderShape() != RenderShape.MODEL) {
                continue; // camas, cofres...: se dibujan con otro sistema; el recuadro ya marca su sitio
            }
            boolean bad = blocked.contains(g.rel());
            float r = bad ? 1.0F : 0.38F, gr = bad ? 0.30F : 0.84F, b = bad ? 0.28F : 1.0F;
            BakedModel model = brd.getBlockModel(g.state());
            ps.pushPose();
            ps.translate(g.rel().getX(), g.rel().getY(), g.rel().getZ());
            PoseStack.Pose pose = ps.last();
            for (Direction d : DIRS) {
                if (p.solid.contains(g.rel().relative(d))) {
                    continue; // cara tapada por otro bloque de la casa: no se dibuja (el holograma queda limpio)
                }
                rand.setSeed(42L);
                for (BakedQuad q : model.getQuads(g.state(), d, rand)) {
                    glass.putBulkData(pose, q, ONES, r, gr, b, bad ? 0.6F : alpha, LIGHTS, OverlayTexture.NO_OVERLAY, false);
                }
            }
            rand.setSeed(42L);
            for (BakedQuad q : model.getQuads(g.state(), null, rand)) {
                glass.putBulkData(pose, q, ONES, r, gr, b, bad ? 0.6F : alpha, LIGHTS, OverlayTexture.NO_OVERLAY, false);
            }
            ps.popPose();
        }
        ps.popPose();

        // 2) recuadros: el tamaño total, lo que estorba (rojo) y los huecos grandes bajo el suelo (naranja)
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(ps, lines, p.box.inflate(0.03), 0.45F, 0.9F, 1.0F, 0.95F);
        for (BlockPos rel : blocked) {
            LevelRenderer.renderLineBox(ps, lines, new AABB(rel).inflate(0.02), 1.0F, 0.2F, 0.2F, 1.0F);
        }
        for (BlockPos rel : uneven) {
            LevelRenderer.renderLineBox(ps, lines, new AABB(rel).inflate(0.02), 1.0F, 0.6F, 0.1F, 1.0F);
        }

        // 3) cartel encima de la semilla: de quién es, cuánto mide y qué falta
        List<Component> text = new ArrayList<>();
        text.add(Component.translatable("hologram.mundos_de_axelis.casa.title", Component.translatable(k.nameKey()))
                .withStyle(st -> st.withColor(k.accent()).withBold(true)));
        text.add(Component.translatable("hologram.mundos_de_axelis.casa.size", p.width, p.depth, p.height).withStyle(ChatFormatting.WHITE));
        if (!blocked.isEmpty()) {
            text.add(Component.translatable("hologram.mundos_de_axelis.casa.blocked", blocked.size()).withStyle(ChatFormatting.RED));
        } else if (!uneven.isEmpty()) {
            text.add(Component.translatable("hologram.mundos_de_axelis.casa.uneven").withStyle(ChatFormatting.GOLD));
        } else {
            text.add(Component.translatable("hologram.mundos_de_axelis.casa.ok").withStyle(ChatFormatting.GREEN));
        }
        text.add(Component.translatable("hologram.mundos_de_axelis.casa.rotate").withStyle(ChatFormatting.GRAY));
        ps.pushPose();
        ps.translate(0.5, 1.35 + 0.1 * text.size(), 0.5);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.022F, -0.022F, 0.022F);
        Matrix4f m = ps.last().pose();
        for (int i = 0; i < text.size(); i++) {
            Component c = text.get(i);
            float x = -font.width(c) / 2.0F;
            font.drawInBatch(c, x, i * 10.5F, 0xFFFFFFFF, false, m, buffers, Font.DisplayMode.NORMAL, 0x66000000, FULL);
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SemillaCasaBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return (int) MAX_DIST;
    }
}
