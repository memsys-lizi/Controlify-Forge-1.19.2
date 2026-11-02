package dev.isxander.controlify.mixins.feature.screenop.vanilla;

import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ComponentProcessorProvider;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.utils.NavigationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 1.19.2 sliders have no {@code canChangeValue} flag. Press enters adjust mode so
 * left and right change the value instead of leaving the widget.
 */
@Mixin(AbstractSliderButton.class)
public class AbstractSliderButtonMixin implements ComponentProcessorProvider {
    @Unique
    private boolean controlify$adjusting;

    @Unique
    private final NavigationHelper controlify$sliderNav = new NavigationHelper(8, 2);

    @Unique
    private final ComponentProcessor controlify$processor = new ComponentProcessor() {
        @Override
        public boolean overrideControllerNavigation(ScreenProcessor<?> screen, Controller<?, ?> controller) {
            if (!controlify$adjusting) {
                return false;
            }
            var slider = (AbstractSliderButton) (Object) AbstractSliderButtonMixin.this;
            boolean repeat = controlify$sliderNav.canNavigate();
            if (controller.bindings().GUI_NAVI_LEFT.held() && (repeat || !controller.bindings().GUI_NAVI_LEFT.prevHeld())) {
                slider.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0);
                if (!controller.bindings().GUI_NAVI_LEFT.prevHeld()) {
                    controlify$sliderNav.reset();
                }
                controlify$sliderNav.onNavigate();
                return true;
            }
            if (controller.bindings().GUI_NAVI_RIGHT.held() && (repeat || !controller.bindings().GUI_NAVI_RIGHT.prevHeld())) {
                slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
                if (!controller.bindings().GUI_NAVI_RIGHT.prevHeld()) {
                    controlify$sliderNav.reset();
                }
                controlify$sliderNav.onNavigate();
                return true;
            }
            return true;
        }

        @Override
        public boolean overrideControllerButtons(ScreenProcessor<?> screen, Controller<?, ?> controller) {
            if (controller.bindings().GUI_PRESS.justPressed()) {
                controlify$adjusting = !controlify$adjusting;
                ((AbstractSliderButton) (Object) AbstractSliderButtonMixin.this).playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
            if (controlify$adjusting && controller.bindings().GUI_BACK.justPressed()) {
                controlify$adjusting = false;
                return true;
            }
            return false;
        }

        @Override
        public void onFocusGained(ScreenProcessor<?> screen, Controller<?, ?> controller) {
            controlify$adjusting = false;
        }
    };

    @Override
    public ComponentProcessor componentProcessor() {
        return controlify$processor;
    }
}
