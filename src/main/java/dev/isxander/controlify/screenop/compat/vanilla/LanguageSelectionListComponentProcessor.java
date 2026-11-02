package dev.isxander.controlify.screenop.compat.vanilla;

import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.mixins.feature.screenop.vanilla.OptionsSubScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;

public class LanguageSelectionListComponentProcessor implements ComponentProcessor {
    private final LanguageInfo language;

    public LanguageSelectionListComponentProcessor(LanguageInfo language) {
        this.language = language;
    }

    @Override
    public boolean overrideControllerButtons(ScreenProcessor<?> screen, Controller<?, ?> controller) {
        if (controller.bindings().GUI_PRESS.justPressed()) {
            var minecraft = Minecraft.getInstance();
            var languageManager = minecraft.getLanguageManager();
            if (!language.equals(languageManager.getSelected())) {
                languageManager.setSelected(language);
                minecraft.options.languageCode = language.getCode();
                minecraft.reloadResourcePacks();
                minecraft.options.save();
            }

            minecraft.setScreen(((OptionsSubScreenAccessor) screen.screen).getLastScreen());

            return true;
        }

        return false;
    }
}
