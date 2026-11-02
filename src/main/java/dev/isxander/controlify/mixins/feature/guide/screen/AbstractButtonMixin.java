package dev.isxander.controlify.mixins.feature.guide.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.bind.BindRenderer;
import dev.isxander.controlify.api.buttonguide.ButtonRenderPosition;
import dev.isxander.controlify.gui.ButtonGuideRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidgetMixin implements ButtonGuideRenderer<AbstractButton> {
    @Unique private RenderData<AbstractButton> renderData = null;

    @Override
    protected void controlify$onButtonGuideHead(PoseStack matrices) {
        matrices.pushPose();
        if (shouldRender() && Minecraft.getInstance().font.width(getMessage()) <= getWidth() && renderData.position() == ButtonRenderPosition.TEXT) {
            matrices.translate(getBind().size().width() / 2f, 0, 0);
        }
    }

    @Override
    protected void controlify$onButtonGuideReturn(PoseStack matrices) {
        matrices.popPose();
        if (!shouldRender()) {
            return;
        }
        switch (renderData.position()) {
            case LEFT -> getBind().render(matrices, x - getBind().size().width() - 1, y + getHeight() / 2);
            case RIGHT -> getBind().render(matrices, x + getWidth() + 1, y + getHeight() / 2);
            case TEXT -> {
                Font font = Minecraft.getInstance().font;
                int drawX;
                if (font.width(getMessage()) > getWidth()) {
                    drawX = x;
                } else {
                    drawX = x + getWidth() / 2 - font.width(getMessage()) / 2 - getBind().size().width();
                }

                getBind().render(matrices, drawX, y + getHeight() / 2);
            }
        }
    }

    @Override
    public void setButtonGuide(RenderData<AbstractButton> renderData) {
        this.renderData = renderData;
    }

    @Unique
    private boolean shouldRender() {
        return renderData != null
                && this.isActive()
                && Controlify.instance().currentInputMode() == InputMode.CONTROLLER
                && Controlify.instance().currentController().config().showScreenGuide
                && !renderData.binding().apply(Controlify.instance().currentController().bindings()).isUnbound()
                && renderData.renderPredicate().shouldDisplay((AbstractButton) (Object) this);
    }

    @Unique
    private BindRenderer getBind() {
        return renderData.binding().apply(Controlify.instance().currentController().bindings()).renderer();
    }
}
