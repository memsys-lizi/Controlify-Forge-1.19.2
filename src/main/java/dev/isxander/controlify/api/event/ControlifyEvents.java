package dev.isxander.controlify.api.event;

import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.ingameguide.IngameGuideRegistry;
import dev.isxander.controlify.api.ingameinput.LookInputModifier;
import dev.isxander.controlify.bindings.ControllerBindings;
import dev.isxander.controlify.controller.Controller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

public final class ControlifyEvents {
    /**
     * Triggers when the input mode is changed from keyboard to controller or vice versa.
     */
    public static final dev.isxander.controlify.api.event.Event<InputModeChanged> INPUT_MODE_CHANGED = dev.isxander.controlify.api.event.Event.of(callbacks -> mode -> {
        for (InputModeChanged callback : callbacks) {
            callback.onInputModeChanged(mode);
        }
        MinecraftForge.EVENT_BUS.post(new InputModeChangedEvent(mode));
    });

    /**
     * Triggers every tick when the current controller state has been updated.
     */
    public static final dev.isxander.controlify.api.event.Event<ControllerStateUpdate> ACTIVE_CONTROLLER_TICKED = dev.isxander.controlify.api.event.Event.of(callbacks -> controller -> {
        for (ControllerStateUpdate callback : callbacks) {
            callback.onControllerStateUpdate(controller);
        }
        MinecraftForge.EVENT_BUS.post(new ControllerStateUpdateEvent(controller, true));
    });

    /**
     * @deprecated Use {@link #ACTIVE_CONTROLLER_TICKED} instead.
     */
    @Deprecated
    public static final dev.isxander.controlify.api.event.Event<ControllerStateUpdate> CONTROLLER_STATE_UPDATED = ACTIVE_CONTROLLER_TICKED;

    /**
     * Triggers every tick when any connected controller's state has been updated before the active controller is ticked.
     */
    public static final dev.isxander.controlify.api.event.Event<ControllerStateUpdate> CONTROLLER_STATE_UPDATE = dev.isxander.controlify.api.event.Event.of(callbacks -> controller -> {
        for (ControllerStateUpdate callback : callbacks) {
            callback.onControllerStateUpdate(controller);
        }
        MinecraftForge.EVENT_BUS.post(new ControllerStateUpdateEvent(controller, false));
    });

    /**
     * Triggers when the button guide entries are being populated, so you can add more of your own.
     */
    public static final dev.isxander.controlify.api.event.Event<IngameGuideRegistryEvent> INGAME_GUIDE_REGISTRY = dev.isxander.controlify.api.event.Event.of(callbacks -> (bindings, registry) -> {
        for (IngameGuideRegistryEvent callback : callbacks) {
            callback.onRegisterIngameGuide(bindings, registry);
        }
        MinecraftForge.EVENT_BUS.post(new IngameGuideRegistryForgeEvent(bindings, registry));
    });

    /**
     * Triggers in a GUI when the virtual mouse is toggled on or off.
     */
    public static final dev.isxander.controlify.api.event.Event<VirtualMouseToggled> VIRTUAL_MOUSE_TOGGLED = dev.isxander.controlify.api.event.Event.of(callbacks -> enabled -> {
        for (VirtualMouseToggled callback : callbacks) {
            callback.onVirtualMouseToggled(enabled);
        }
        MinecraftForge.EVENT_BUS.post(new VirtualMouseToggledEvent(enabled));
    });

    /**
     * Allows you to modify the look input before it is applied to the player.
     * These modifiers are called before the look input is multiplied by the sensitivity.
     */
    public static final dev.isxander.controlify.api.event.Event<LookInputModifier> LOOK_INPUT_MODIFIER = dev.isxander.controlify.api.event.Event.of(callbacks -> new LookInputModifier() {
        @Override
        public float modifyX(float x, Controller<?, ?> controller) {
            for (LookInputModifier callback : callbacks) {
                x = callback.modifyX(x, controller);
            }
            return x;
        }

        @Override
        public float modifyY(float y, Controller<?, ?> controller) {
            for (LookInputModifier callback : callbacks) {
                y = callback.modifyY(y, controller);
            }
            return y;
        }
    });

    private ControlifyEvents() {
    }

    @FunctionalInterface
    public interface InputModeChanged {
        void onInputModeChanged(InputMode mode);
    }

    @FunctionalInterface
    public interface ControllerStateUpdate {
        void onControllerStateUpdate(Controller<?, ?> controller);
    }

    @FunctionalInterface
    public interface IngameGuideRegistryEvent {
        void onRegisterIngameGuide(ControllerBindings<?> bindings, IngameGuideRegistry registry);
    }

    @FunctionalInterface
    public interface VirtualMouseToggled {
        void onVirtualMouseToggled(boolean enabled);
    }

    public static final class InputModeChangedEvent extends Event {
        private final InputMode mode;

        public InputModeChangedEvent(InputMode mode) {
            this.mode = mode;
        }

        public InputMode getMode() {
            return mode;
        }
    }

    public static final class ControllerStateUpdateEvent extends Event {
        private final Controller<?, ?> controller;
        private final boolean activeController;

        public ControllerStateUpdateEvent(Controller<?, ?> controller, boolean activeController) {
            this.controller = controller;
            this.activeController = activeController;
        }

        public Controller<?, ?> getController() {
            return controller;
        }

        public boolean isActiveController() {
            return activeController;
        }
    }

    public static final class IngameGuideRegistryForgeEvent extends Event {
        private final ControllerBindings<?> bindings;
        private final IngameGuideRegistry registry;

        public IngameGuideRegistryForgeEvent(ControllerBindings<?> bindings, IngameGuideRegistry registry) {
            this.bindings = bindings;
            this.registry = registry;
        }

        public ControllerBindings<?> getBindings() {
            return bindings;
        }

        public IngameGuideRegistry getRegistry() {
            return registry;
        }
    }

    public static final class VirtualMouseToggledEvent extends Event {
        private final boolean enabled;

        public VirtualMouseToggledEvent(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public static final class ControlifyPreInitEvent extends Event {
        private final ControlifyApi controlify;

        public ControlifyPreInitEvent(ControlifyApi controlify) {
            this.controlify = controlify;
        }

        public ControlifyApi getControlify() {
            return controlify;
        }
    }

    public static final class ControllersDiscoveredEvent extends Event {
        private final ControlifyApi controlify;

        public ControllersDiscoveredEvent(ControlifyApi controlify) {
            this.controlify = controlify;
        }

        public ControlifyApi getControlify() {
            return controlify;
        }
    }
}
