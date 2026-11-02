package dev.isxander.controlify.mixins.feature.screenop;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = At.Shift.AFTER))
    private void onPostRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        ControlifyApi.get().getCurrentController().ifPresent(controller -> {
            if (minecraft.screen == null) return;
            ScreenProcessorProvider.provide(minecraft.screen).render(controller, poseStack, tickDelta);
        });
    }
}
