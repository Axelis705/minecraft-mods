package com.axeliscore.forma.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Una forma hecha con un modelo de Blockbench (.geo.json) y sus animaciones (.animation.json): camina, corre o se queda quieta según
 * qué tan rápido se mueve el jugador, y toca los gestos que pide el servidor (el nombre del gesto es el de la animación). Gira con el
 * cuerpo del jugador y la cabeza ("head" o el hueso que se diga) sigue a donde mira.
 */
public class FormaGeo implements FormasCliente.Dibujo {
    protected final ResourceLocation geo, textura;
    protected final ResourceLocation[] anims;
    protected float escala = 1F;
    protected String quieto = "idle", camina = "walk", corre = "run", cabeza = "head";
    /** Rapidez (bloques por segundo) desde la que camina y desde la que corre. */
    protected float vCamina = 0.6F, vCorre = 4.6F;
    private final Map<Geo, GeoRenderer.Frame> frames = new IdentityHashMap<>();

    public FormaGeo(ResourceLocation geo, ResourceLocation textura, ResourceLocation... anims) {
        this.geo = geo;
        this.textura = textura;
        this.anims = anims;
    }

    public FormaGeo escala(float e) {
        this.escala = e;
        return this;
    }

    public FormaGeo animaciones(String quieto, String camina, String corre) {
        this.quieto = quieto;
        this.camina = camina;
        this.corre = corre;
        return this;
    }

    public FormaGeo rapidez(float camina, float corre) {
        this.vCamina = camina;
        this.vCorre = corre;
        return this;
    }

    public FormaGeo cabeza(String hueso) {
        this.cabeza = hueso;
        return this;
    }

    /** Recién conectado el modelo: las capas (la base y los gestos encima). */
    protected void capas(AnimHost h) {
        h.animator().layer("base");
        h.animator().layer("gesto");
    }

    /** La textura para este jugador (por si cambia, por ejemplo al recibir daño). */
    protected ResourceLocation textura(net.minecraft.world.entity.player.Player p) {
        return textura;
    }

    //#if MC < 1.20.5
    @Override
    public void dibujar(net.minecraft.client.player.AbstractClientPlayer p, FormasCliente.Estado e, float pt, com.mojang.blaze3d.vertex.PoseStack ps,
            net.minecraft.client.renderer.MultiBufferSource buf, int luz) {
        AnimHost h = e.host;
        if (h.bind(GeoAssets.geo(geo), GeoAssets.clips(anims))) {
            capas(h);
        }
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        double v = Math.sqrt(dx * dx + dz * dz) * 20;
        com.axeliscore.anim.Animator a = h.animator();
        a.query("ground_speed", v);
        a.query("is_on_ground", p.onGround() ? 1 : 0);
        a.query("is_sneaking", p.isShiftKeyDown() ? 1 : 0);
        String base = v >= vCorre ? corre : v >= vCamina ? camina : quieto;
        Clip c = h.clip(base);
        if (c == null) {
            c = h.clip(quieto);
        }
        if (c != null) {
            a.play("base", c, 0.25F, base.equals(camina) ? (float) Math.max(0.6, Math.min(1.6, v / 2.2)) : 1F, false);
        }
        // un gesto nuevo que pidió el servidor
        if (e.accionNueva) {
            e.accionNueva = false;
            Clip g = h.clip(e.accion);
            if (g != null) {
                a.play("gesto", g, 0.12F, 1F, true);
            }
        }
        if (a.finished("gesto")) {
            a.stop("gesto", 0.25F);
        }
        h.advance((p.tickCount + pt) / 20F);
        // la cabeza sigue a donde mira (relativo al cuerpo)
        float cuerpo = net.minecraft.util.Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
        float cab = net.minecraft.util.Mth.wrapDegrees(net.minecraft.util.Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot) - cuerpo);
        float arriba = net.minecraft.util.Mth.lerp(pt, p.xRotO, p.getXRot());
        Geo g = h.geo();
        int hb = g.bone(cabeza);
        if (hb >= 0) {
            h.pose().rot[hb * 3 + 1] += (float) Math.toRadians(net.minecraft.util.Mth.clamp(cab, -50F, 50F)) * 0.8F;
            h.pose().rot[hb * 3] += (float) Math.toRadians(net.minecraft.util.Mth.clamp(arriba, -40F, 40F)) * 0.6F;
        }
        ps.pushPose();
        ps.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(180F - cuerpo)));
        ps.scale(-1F, -1F, 1F);
        if (escala != 1F) {
            ps.scale(escala, escala, escala);
        }
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.Frame f = frames.computeIfAbsent(g, GeoRenderer.Frame::new);
        GeoRenderer.compute(g, h.pose(), ps, f);
        com.axeliscore.plat.client.Draw d = new com.axeliscore.plat.client.Draw(buf);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.pack(0, p.hurtTime > 0);
        if (!p.isInvisible()) {
            GeoRenderer.draw(g, h.pose(), f, d.cutout(textura(p)), GeoRenderer.Pass.NORMAL, luz, overlay, 0xFFFFFFFF);
            GeoRenderer.draw(g, h.pose(), f, d.glow(textura(p)), GeoRenderer.Pass.GLOW, 0xF000F0, overlay, 0xFFFFFFFF);
        }
        extra(p, e, g, f, ps, d, luz, pt);
        d.flush();
        ps.popPose();
    }

    /** Para dibujar algo más (pegado a un hueso: {@code f.mundo(hueso)}). */
    protected void extra(net.minecraft.client.player.AbstractClientPlayer p, FormasCliente.Estado e, Geo g, GeoRenderer.Frame f,
            com.mojang.blaze3d.vertex.PoseStack ps, com.axeliscore.plat.client.Draw d, int luz, float pt) {
    }
    //#endif
}
