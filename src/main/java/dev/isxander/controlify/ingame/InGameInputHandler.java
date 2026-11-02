package dev.isxander.controlify.ingame;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ingameinput.LookInputModifier;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.controller.gamepad.GamepadController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;

import java.util.function.BiFunction;

public class InGameInputHandler {
    private final Controller<?, ?> controller;
    private final Minecraft minecraft;

    private double lookInputX, lookInputY;
    private boolean shouldShowPlayerList;

    public InGameInputHandler(Controller<?, ?> controller) {
        this.controller = controller;
        this.minecraft = Minecraft.getInstance();

    }

    /**
     * Install or remove controller movement on the local player.
     * Called when this handler becomes current and whenever the input mode changes.
     * A listener is not registered here: each new handler used to stack one, and the
     * stale handler could put the previous controller back in charge.
     */
    public void applyToPlayer(InputMode mode) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.input = mode == InputMode.CONTROLLER
                ? new ControllerPlayerMovement(controller, minecraft.player)
                : new KeyboardInput(minecraft.options);
    }

    public void inputTick() {
        handlePlayerLookInput();
        handleKeybinds();
    }

    protected void handleKeybinds() {
        shouldShowPlayerList = false;

        if (minecraft.screen != null)
            return;

        if (controller.bindings().PAUSE.justPressed()) {
            minecraft.pauseGame(false);
        }
        if (minecraft.player != null) {
            // Attack, use, drop, swap hands, inventory and perspective are vanilla KeyMappings.
            // imitateVanillaClick presses that one mapping so handleKeybinds performs the action.
            if (controller.bindings().NEXT_SLOT.justPressed()) {
                cycleHotbar(-1);
            }
            if (controller.bindings().PREV_SLOT.justPressed()) {
                cycleHotbar(1);
            }
        }
        if (controller.bindings().TOGGLE_HUD_VISIBILITY.justPressed()) {
            minecraft.options.hideGui = !minecraft.options.hideGui;
        }

        shouldShowPlayerList = controller.bindings().SHOW_PLAYER_LIST.held();
    }

    protected void handlePlayerLookInput() {
        var player = this.minecraft.player;
        var gamepad = controller instanceof GamepadController ? (GamepadController) controller : null;

        boolean controllerLook = Controlify.instance().currentInputMode() == InputMode.CONTROLLER;
        boolean focusOk = minecraft.isWindowActive() || Controlify.instance().config().globalSettings().outOfFocusInput;
        if ((!minecraft.mouseHandler.isMouseGrabbed() && !controllerLook) || !focusOk || minecraft.screen != null || player == null) {
            lookInputX = 0;
            lookInputY = 0;
            return;
        }

        // flick stick - turn 90 degrees immediately upon turning
        // should be paired with gyro controls
        if (gamepad != null && gamepad.config().flickStick) {
            var turnAngle = 90 / 0.15f; // Entity#turn multiplies cursor delta by 0.15 to get rotation

            player.turn(
                    (controller.bindings().LOOK_RIGHT.justPressed() ? turnAngle : 0)
                            - (controller.bindings().LOOK_LEFT.justPressed() ? turnAngle : 0),
                    (controller.bindings().LOOK_DOWN.justPressed() ? turnAngle : 0)
                            - (controller.bindings().LOOK_UP.justPressed() ? turnAngle : 0)
            );

            return;
        }

        // normal look input
        var impulseY = controller.bindings().LOOK_DOWN.state() - controller.bindings().LOOK_UP.state();
        var impulseX = controller.bindings().LOOK_RIGHT.state() - controller.bindings().LOOK_LEFT.state();
        impulseX *= Math.abs(impulseX);
        impulseY *= Math.abs(impulseY);

        if (controller.config().reduceAimingSensitivity && player != null && player.isUsingItem()) {
            float aimMultiplier = switch (player.getUseItem().getUseAnimation()) {
                case BOW, CROSSBOW, SPEAR -> 0.6f;
                case SPYGLASS -> 0.2f;
                default -> 1f;
            };
            impulseX *= aimMultiplier;
            impulseY *= aimMultiplier;
        }

        // gyro input
        if (gamepad != null
                && gamepad.hasGyro()
                && (!gamepad.config().gyroRequiresButton || gamepad.bindings().GAMEPAD_GYRO_BUTTON.held())
        ) {
            var gyroDelta = gamepad.state().gyroDelta();

            impulseX += (gyroDelta.yaw() + gyroDelta.pitch()) * gamepad.config().gyroLookSensitivity;
            impulseY += gyroDelta.roll() * gamepad.config().gyroLookSensitivity;
        }

        LookInputModifier lookInputModifier = ControlifyEvents.LOOK_INPUT_MODIFIER.invoker();
        impulseX = lookInputModifier.modifyX(impulseX, controller);
        impulseY = lookInputModifier.modifyY(impulseY, controller);

        lookInputX = impulseX * controller.config().horizontalLookSensitivity * 65f;
        lookInputY = impulseY * controller.config().verticalLookSensitivity * 65f;
    }

    public void processPlayerLook(float deltaTime) {
        if (minecraft.player != null) {
            minecraft.player.turn(lookInputX * deltaTime, lookInputY * deltaTime);
        }
    }

    public boolean shouldShowPlayerList() {
        return this.shouldShowPlayerList;
    }

    private void cycleHotbar(double direction) {
        var inventory = minecraft.player.getInventory();
        inventory.swapPaint(direction);
        if (minecraft.player.isSpectator()) {
            minecraft.gui.getSpectatorGui().onHotbarSelected(inventory.selected);
        }
    }

    public record FunctionalLookInputModifier(BiFunction<Float, Controller<?, ?>, Float> x, BiFunction<Float, Controller<?, ?>, Float> y) implements LookInputModifier {
        @Override
        public float modifyX(float x, Controller<?, ?> controller) {
            return this.x.apply(x, controller);
        }

        @Override
        public float modifyY(float y, Controller<?, ?> controller) {
            return this.y.apply(y, controller);
        }
    }
}
