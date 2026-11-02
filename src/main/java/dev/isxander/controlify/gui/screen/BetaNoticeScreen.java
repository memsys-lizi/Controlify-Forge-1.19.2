package dev.isxander.controlify.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class BetaNoticeScreen extends Screen {
    private MultiLineLabel text;

    public BetaNoticeScreen() {
        super(Component.translatable("controlify.beta.title"));
    }

    @Override
    protected void init() {
        text = MultiLineLabel.create(
                font,
                Component.translatable("controlify.beta.message",
                        Component.translatable("controlify.beta.message.link")
                                .withStyle(ChatFormatting.AQUA)
                ),
                this.width - 10
        );

        addRenderableWidget(new Button(
                this.width / 2 - 75,
                this.height - 8 - 20 - 20 - 4,
                150,
                20,
                Component.translatable("controlify.beta.button"),
                btn -> Util.getPlatform().openUri("https://github.com/isXander/controlify/issues")
        ));
        addRenderableWidget(new Button(
                this.width / 2 - 75,
                this.height - 8 - 20,
                150,
                20,
                CommonComponents.GUI_PROCEED,
                btn -> minecraft.setScreen(new TitleScreen())
        ));
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        if (text != null) {
            text.renderCentered(matrices, this.width / 2, 30);
        }
    }
}
