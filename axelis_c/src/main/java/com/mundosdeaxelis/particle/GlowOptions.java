package com.mundosdeaxelis.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/** Partícula con textura propia, con color (rgb; 0 = color al azar) y tamaño. */
public class GlowOptions implements ParticleOptions {
    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<GlowOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public GlowOptions fromCommand(ParticleType<GlowOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int rgb = reader.readInt();
            reader.expect(' ');
            float size = reader.readFloat();
            return new GlowOptions(type, rgb, size);
        }

        @Override
        public GlowOptions fromNetwork(ParticleType<GlowOptions> type, FriendlyByteBuf buf) {
            return new GlowOptions(type, buf.readInt(), buf.readFloat());
        }
    };

    private final ParticleType<GlowOptions> type;
    public final int rgb;
    public final float size;

    public GlowOptions(ParticleType<GlowOptions> type, int rgb, float size) {
        this.type = type;
        this.rgb = rgb & 0xFFFFFF;
        this.size = size;
    }

    public static Codec<GlowOptions> codec(ParticleType<GlowOptions> type) {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("rgb").forGetter(o -> o.rgb),
                Codec.FLOAT.fieldOf("size").forGetter(o -> o.size)
        ).apply(i, (c, s) -> new GlowOptions(type, c, s)));
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(rgb);
        buf.writeFloat(size);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %.2f", net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getKey(type), rgb, size);
    }
}
