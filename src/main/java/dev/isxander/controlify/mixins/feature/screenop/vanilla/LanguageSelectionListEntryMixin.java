package dev.isxander.controlify.mixins.feature.screenop.vanilla;

import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ComponentProcessorProvider;
import dev.isxander.controlify.screenop.compat.vanilla.LanguageSelectionListComponentProcessor;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LanguageSelectScreen.LanguageSelectionList.Entry.class)
public class LanguageSelectionListEntryMixin implements ComponentProcessorProvider {
    @Shadow @Final LanguageInfo language;

    @Unique private LanguageSelectionListComponentProcessor controlify$componentProcessor = null;

    @Override
    public ComponentProcessor componentProcessor() {
        // lazily create the component processor so `language` is defined
        if (controlify$componentProcessor == null)
            controlify$componentProcessor = new LanguageSelectionListComponentProcessor(language);

        return controlify$componentProcessor;
    }
}
