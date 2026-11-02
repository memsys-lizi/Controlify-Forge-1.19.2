package dev.isxander.controlify.screenop;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.sound.ControlifySounds;
import dev.isxander.controlify.utils.NavigationHelper;
import dev.isxander.controlify.virtualmouse.VirtualMouseBehaviour;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ScreenProcessor<T extends Screen> {
    public final T screen;
    protected final NavigationHelper navigationHelper = new NavigationHelper(10, 3);
    protected static final Minecraft minecraft = Minecraft.getInstance();

    public ScreenProcessor(T screen) {
        this.screen = screen;
        ControlifyEvents.VIRTUAL_MOUSE_TOGGLED.register(this::onVirtualMouseToggled);
    }

    public void onControllerUpdate(Controller<?, ?> controller) {
        if (!Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()) {
            if (!handleComponentNavOverride(controller))
                handleComponentNavigation(controller);

            if (!handleComponentButtonOverride(controller))
                handleButtons(controller);
        } else {
            handleScreenVMouse(controller, Controlify.instance().virtualMouseHandler());
        }

        handleTabNavigation(controller);
    }

    public void render(Controller<?, ?> controller, PoseStack poseStack, float tickDelta) {
        var vmouse = Controlify.instance().virtualMouseHandler();
        this.render(controller, poseStack, tickDelta, vmouse.isVirtualMouseEnabled() ? Optional.of(vmouse) : Optional.empty());
    }

    public void onInputModeChanged(InputMode mode) {
        switch (mode) {
            case KEYBOARD_MOUSE -> screen.setFocused(null);
            case CONTROLLER -> {
                if (!Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()) {
                    setInitialFocus();
                }
            }
        }
    }

    /**
     * 1.19.2 has {@code changeFocus(boolean)} and no arrow-key {@code ComponentPath}.
     * Directional GUI navigation is phase 2. This walks the existing focus cycle so a
     * screen does not throw when a controller is used.
     */
    protected void handleComponentNavigation(Controller<?, ?> controller) {
        if (screen.getFocused() == null)
            setInitialFocus();

        var bindings = controller.bindings();
        boolean repeatEventAvailable = navigationHelper.canNavigate();
        Boolean forward = null;

        if (bindings.GUI_NAVI_RIGHT.held() && (repeatEventAvailable || !bindings.GUI_NAVI_RIGHT.prevHeld())) {
            forward = true;
            if (!bindings.GUI_NAVI_RIGHT.prevHeld())
                navigationHelper.reset();
        } else if (bindings.GUI_NAVI_LEFT.held() && (repeatEventAvailable || !bindings.GUI_NAVI_LEFT.prevHeld())) {
            forward = false;
            if (!bindings.GUI_NAVI_LEFT.prevHeld())
                navigationHelper.reset();
        } else if (bindings.GUI_NAVI_DOWN.held() && (repeatEventAvailable || !bindings.GUI_NAVI_DOWN.prevHeld())) {
            forward = true;
            if (!bindings.GUI_NAVI_DOWN.prevHeld())
                navigationHelper.reset();
        } else if (bindings.GUI_NAVI_UP.held() && (repeatEventAvailable || !bindings.GUI_NAVI_UP.prevHeld())) {
            forward = false;
            if (!bindings.GUI_NAVI_UP.prevHeld())
                navigationHelper.reset();
        }

        if (forward != null && screen.changeFocus(forward)) {
            navigationHelper.onNavigate();

            if (Controlify.instance().config().globalSettings().uiSounds)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ControlifySounds.SCREEN_FOCUS_CHANGE.get(), 1.0F));

            var focused = screen.getFocused();
            if (focused != null)
                ComponentProcessorProvider.provide(focused).onFocusGained(this, controller);
        }
    }

    protected void handleButtons(Controller<?, ?> controller) {
        if (controller.bindings().GUI_PRESS.justPressed()) {
            screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        }
        if (controller.bindings().GUI_BACK.justPressed()) {
            playClackSound();
            screen.onClose();
        }
    }

    protected void handleScreenVMouse(Controller<?, ?> controller, VirtualMouseHandler vmouse) {

    }

    protected boolean handleComponentButtonOverride(Controller<?, ?> controller) {
        var focusTree = getFocusTree();
        while (!focusTree.isEmpty()) {
            var focused = focusTree.poll();
            var processor = ComponentProcessorProvider.provide(focused);
            if (processor.overrideControllerButtons(this, controller)) return true;
        }

        return false;
    }

    protected boolean handleComponentNavOverride(Controller<?, ?> controller) {
        var focusTree = getFocusTree();
        while (!focusTree.isEmpty()) {
            var focused = focusTree.poll();
            var processor = ComponentProcessorProvider.provide(focused);
            if (processor.overrideControllerNavigation(this, controller)) return true;
        }
        return false;
    }

    protected void handleTabNavigation(Controller<?, ?> controller) {
        // TabNavigationBar does not exist in 1.19.2. Creative tabs are handled by their own processor.
    }

    public void onWidgetRebuild() {
        setInitialFocus();
    }

    public void onVirtualMouseToggled(boolean enabled) {
        if (enabled) {
            screen.setFocused(null);
        } else {
            setInitialFocus();
        }
    }

    protected void render(Controller<?, ?> controller, PoseStack poseStack, float tickDelta, Optional<VirtualMouseHandler> vmouse) {

    }

    protected void setInitialFocus() {
        if (screen.getFocused() == null && Controlify.instance().currentInputMode() == InputMode.CONTROLLER && !Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()) {
            screen.changeFocus(true);
            navigationHelper.clearDelay();
        }
    }

    public VirtualMouseBehaviour virtualMouseBehaviour() {
        return VirtualMouseBehaviour.DEFAULT;
    }

    protected Queue<GuiEventListener> getFocusTree() {
        if (screen.getFocused() == null) return new ArrayDeque<>();

        var tree = new ArrayDeque<GuiEventListener>();
        var focused = screen.getFocused();
        tree.add(focused);
        while (focused instanceof CustomFocus customFocus) {
            focused = customFocus.getCustomFocus();

            if (focused != null)
                tree.addFirst(focused);
        }

        return tree;
    }

    protected final Optional<AbstractWidget> getWidget(String translationKey) {
        var translatedName = Component.translatable(translationKey).getString();

        return screen.children().stream()
                .filter(child -> child instanceof AbstractWidget)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget.getMessage().getString().equals(translatedName))
                .findAny();
    }

    public static void playClackSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
