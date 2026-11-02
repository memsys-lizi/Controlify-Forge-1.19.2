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
import net.minecraft.client.gui.components.AbstractSelectionList;
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

            handleScroll(controller);
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
     * Move focus to the nearest widget in the stick direction. Lists move row by row.
     */
    protected void handleComponentNavigation(Controller<?, ?> controller) {
        if (screen.getFocused() == null)
            setInitialFocus();

        FocusNavigator.Direction direction = FocusNavigator.readDirection(controller, navigationHelper);
        if (direction != null && FocusNavigator.move(screen, direction)) {
            navigationHelper.onNavigate();

            if (Controlify.instance().config().globalSettings().uiSounds)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ControlifySounds.SCREEN_FOCUS_CHANGE.get(), 1.0F));

            var focused = screen.getFocused();
            if (focused != null)
                ComponentProcessorProvider.provide(focused).onFocusGained(this, controller);
        }
    }

    /**
     * Right stick scrolls the focused list, or the screen if it has no list.
     */
    protected void handleScroll(Controller<?, ?> controller) {
        double delta = controller.bindings().VMOUSE_SCROLL_DOWN.state() - controller.bindings().VMOUSE_SCROLL_UP.state();
        if (Math.abs(delta) < 0.2) {
            return;
        }

        if (screen.getFocused() instanceof AbstractSelectionList<?> focusedList) {
            focusedList.setScrollAmount(focusedList.getScrollAmount() + delta * 10);
            return;
        }

        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                list.setScrollAmount(list.getScrollAmount() + delta * 10);
                return;
            }
        }

        screen.mouseScrolled(minecraft.getWindow().getGuiScaledWidth() / 2.0, minecraft.getWindow().getGuiScaledHeight() / 2.0, -delta);
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
            for (GuiEventListener child : screen.children()) {
                if (FocusNavigator.focusable(child)) {
                    screen.setFocused(child);
                    break;
                }
            }
            if (screen.getFocused() == null) {
                screen.changeFocus(true);
            }
            navigationHelper.clearDelay();
        }
    }

    public VirtualMouseBehaviour virtualMouseBehaviour() {
        return VirtualMouseBehaviour.DEFAULT;
    }

    protected Queue<GuiEventListener> getFocusTree() {
        if (screen.getFocused() == null) return new ArrayDeque<>();

        var tree = new ArrayDeque<GuiEventListener>();
        GuiEventListener focused = screen.getFocused();
        if (focused instanceof AbstractSelectionList<?> list) {
            GuiEventListener entry = list.getFocused();
            if (entry == null) {
                entry = list.getSelected();
            }
            if (entry != null) {
                focused = entry;
            }
        }
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
