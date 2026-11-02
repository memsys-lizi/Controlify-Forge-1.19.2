package dev.isxander.controlify.mixins.feature.guide.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.19.2 widgets have no {@code renderScrollingString}. Button guides hook {@code renderButton}
 * instead, and subclasses override the protected callbacks below.
 */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin extends GuiComponent {
    @Shadow public int x;
    @Shadow public int y;

    @Shadow public abstract int getHeight();

    @Shadow public abstract Component getMessage();

    @Shadow public abstract int getWidth();

    @Shadow public abstract boolean isActive();

    @Inject(method = "renderButton", at = @At("HEAD"))
    private void controlify$buttonGuideHead(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        controlify$onButtonGuideHead(poseStack);
    }

    @Inject(method = "renderButton", at = @At("RETURN"))
    private void controlify$buttonGuideReturn(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        controlify$onButtonGuideReturn(poseStack);
    }

    protected void controlify$onButtonGuideHead(PoseStack poseStack) {
    }

    protected void controlify$onButtonGuideReturn(PoseStack poseStack) {
    }
}
