package dev.isxander.controlify.mixins.feature.bind;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> controlify$getAll() {
        throw new AssertionError();
    }

    @Accessor("isDown")
    void setIsDown(boolean down);

    @Accessor("isDown")
    boolean getIsDown();
}
