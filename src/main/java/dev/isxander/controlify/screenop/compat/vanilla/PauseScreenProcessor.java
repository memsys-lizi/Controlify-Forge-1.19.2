package dev.isxander.controlify.screenop.compat.vanilla;

import dev.isxander.controlify.api.buttonguide.ButtonGuideApi;
import dev.isxander.controlify.api.buttonguide.ButtonGuidePredicate;
import dev.isxander.controlify.api.buttonguide.ButtonRenderPosition;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

public class PauseScreenProcessor extends ScreenProcessor<PauseScreen> {
    public PauseScreenProcessor(PauseScreen screen) {
        super(screen);
    }

    @Override
    protected void handleButtons(Controller<?, ?> controller) {
        super.handleButtons(controller);

        if (controller.bindings().GUI_ABSTRACT_ACTION_1.justPressed()) {
            minecraft.setScreen(new OptionsScreen(screen, minecraft.options));
        }
        if (controller.bindings().GUI_ABSTRACT_ACTION_2.justPressed()) {
            Button disconnect = findDisconnectButton();
            if (disconnect != null) {
                disconnect.onPress();
            }
        }
    }

    @Override
    public void onWidgetRebuild() {
        super.onWidgetRebuild();

        ButtonGuideApi.addGuideToButton(
                (AbstractButton) getWidget("menu.returnToGame").orElseThrow(),
                bindings -> bindings.GUI_BACK,
                ButtonRenderPosition.TEXT,
                ButtonGuidePredicate.ALWAYS
        );
        ButtonGuideApi.addGuideToButton(
                (AbstractButton) getWidget("menu.options").orElseThrow(),
                bindings -> bindings.GUI_ABSTRACT_ACTION_1,
                ButtonRenderPosition.TEXT,
                ButtonGuidePredicate.ALWAYS
        );
        Button disconnect = findDisconnectButton();
        if (disconnect != null) {
            ButtonGuideApi.addGuideToButton(
                    disconnect,
                    bindings -> bindings.GUI_ABSTRACT_ACTION_2,
                    ButtonRenderPosition.TEXT,
                    ButtonGuidePredicate.ALWAYS
            );
        }
    }

    /**
     * 1.19.2 pause screens do not keep the disconnect button in a field.
     * Singleplayer uses menu.returnToMenu and multiplayer uses menu.disconnect.
     */
    private Button findDisconnectButton() {
        String returnToMenu = Component.translatable("menu.returnToMenu").getString();
        String disconnect = Component.translatable("menu.disconnect").getString();
        return screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> {
                    String message = button.getMessage().getString();
                    return message.equals(returnToMenu) || message.equals(disconnect);
                })
                .findFirst()
                .orElse(null);
    }
}
