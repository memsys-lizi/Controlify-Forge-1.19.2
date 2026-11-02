package dev.isxander.controlify.gui.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.bindings.ControllerBindings;
import dev.isxander.controlify.compatibility.ControlifyCompat;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.gui.layout.AnchorPoint;
import dev.isxander.controlify.gui.layout.ColumnLayoutComponent;
import dev.isxander.controlify.gui.layout.PositionedComponent;
import dev.isxander.controlify.gui.layout.RowLayoutComponent;
import dev.isxander.controlify.mixins.feature.guide.screen.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class ContainerButtonGuide {
    private static PositionedComponent<ColumnLayoutComponent<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>> leftLayout;
    private static PositionedComponent<ColumnLayoutComponent<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>> rightLayout;

    public static void setup() {
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            removeLayout();
            if (isScreenCompatible(event.getScreen())) {
                setupLayout();
            }
        });

        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> {
            if (!isScreenCompatible(event.getScreen()) || leftLayout == null) {
                return;
            }
            var stack = event.getPoseStack();
            stack.pushPose();
            stack.translate(0, 0, 2000);
            var screen = event.getScreen();
            renderGuide((AbstractContainerScreen<?>) screen, stack, event.getPartialTick(), event.getMouseX(), event.getMouseY(), screen.width, screen.height);
            stack.popPose();
        });

        ControlifyEvents.INPUT_MODE_CHANGED.register(mode -> {
            if (isScreenCompatible(Minecraft.getInstance().screen)) {
                if (mode == InputMode.CONTROLLER) {
                    setupLayout();
                } else {
                    removeLayout();
                }
            }
        });
    }

    private static void setupLayout() {
        ControllerBindings<?> bindings = ControlifyApi.get().getCurrentController()
                .map(Controller::bindings)
                .orElseThrow();

        leftLayout = new PositionedComponent<>(
                ColumnLayoutComponent.<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>builder()
                        .spacing(2)
                        .colPadding(2, 2)
                        .elementPosition(ColumnLayoutComponent.ElementPosition.LEFT)
                        .element(RowLayoutComponent.<GuideActionRenderer<ContainerGuideCtx>>builder()
                                .spacing(5)
                                .rowPadding(0)
                                .elementPosition(RowLayoutComponent.ElementPosition.MIDDLE)
                                .element(new GuideActionRenderer<>(
                                        new GuideAction<>(bindings.VMOUSE_LCLICK, ctx -> {
                                            if (!ctx.holdingItem().isEmpty())
                                                if (ctx.hoveredSlot() != null && ctx.hoveredSlot().hasItem())
                                                    if (ctx.hoveredSlot().mayPlace(ctx.holdingItem()))
                                                        if (ctx.holdingItem().getCount() > 1)
                                                            return Optional.of(Component.translatable("controlify.guide.container.place_all"));
                                                        else
                                                            return Optional.of(Component.translatable("controlify.guide.container.place_one"));
                                                    else
                                                        return Optional.of(Component.translatable("controlify.guide.container.swap"));
                                                else if (ctx.cursorOutsideContainer())
                                                    return Optional.of(Component.translatable("controlify.guide.container.drop"));
                                            if (ctx.hoveredSlot() != null && ctx.hoveredSlot().hasItem())
                                                return Optional.of(Component.translatable("controlify.guide.container.take"));
                                            return Optional.empty();
                                        }),
                                        false, false
                                ))
                                .element(new GuideActionRenderer<>(
                                        new GuideAction<>(bindings.GUI_BACK, ctx -> {
                                            return Optional.of(Component.translatable("controlify.guide.container.exit"));
                                        }),
                                        false, false
                                ))
                                .build())
                        .build(),
                AnchorPoint.BOTTOM_LEFT,
                0, 0,
                AnchorPoint.BOTTOM_LEFT
        );

        rightLayout = new PositionedComponent<>(
                ColumnLayoutComponent.<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>builder()
                        .spacing(2)
                        .elementPosition(ColumnLayoutComponent.ElementPosition.RIGHT)
                        .colPadding(2, 2)
                        .element(RowLayoutComponent.<GuideActionRenderer<ContainerGuideCtx>>builder()
                                .spacing(5)
                                .rowPadding(0)
                                .elementPosition(RowLayoutComponent.ElementPosition.MIDDLE)
                                .element(new GuideActionRenderer<>(
                                        new GuideAction<>(bindings.VMOUSE_RCLICK, ctx -> {
                                            if (ctx.hoveredSlot() != null && ctx.hoveredSlot().getItem().getCount() > 1 && ctx.holdingItem().isEmpty())
                                                return Optional.of(Component.translatable("controlify.guide.container.take_half"));
                                            if (ctx.hoveredSlot() != null && !ctx.holdingItem().isEmpty() && ctx.hoveredSlot().mayPlace(ctx.holdingItem()))
                                                return Optional.of(Component.translatable("controlify.guide.container.take_one"));
                                            return Optional.empty();
                                        }),
                                        true, false
                                ))
                                .element(new GuideActionRenderer<>(
                                        new GuideAction<>(bindings.VMOUSE_SHIFT_CLICK, ctx -> {
                                            return Optional.of(Component.translatable("controlify.guide.container.quick_move"));
                                        }),
                                        true, false
                                ))
                                .build())
                        .build(),
                AnchorPoint.BOTTOM_RIGHT,
                0, 0,
                AnchorPoint.BOTTOM_RIGHT
        );
    }

    private static void removeLayout() {
        leftLayout = null;
        rightLayout = null;
    }

    private static void renderGuide(AbstractContainerScreen<?> screen, PoseStack stack, float tickDelta, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (leftLayout == null || rightLayout == null)
            return;

        if (!ControlifyApi.get().getCurrentController().map(controller -> controller.config().showScreenGuide).orElse(false)) {
            return;
        }

        var accessor = (AbstractContainerScreenAccessor) screen;

        ContainerGuideCtx ctx = new ContainerGuideCtx(accessor.getHoveredSlot(), screen.getMenu().getCarried(), accessor.invokeHasClickedOutside(mouseX, mouseY, accessor.getLeftPos(), accessor.getTopPos(), 0));

        for (var row : leftLayout.getComponent().getChildComponents()) {
            for (var element : row.getChildComponents()) {
                element.updateName(ctx);
            }
        }
        for (var row : rightLayout.getComponent().getChildComponents()) {
            for (var element : row.getChildComponents()) {
                element.updateName(ctx);
            }
        }

        leftLayout.updatePosition();
        rightLayout.updatePosition();

        ControlifyCompat.ifBeginHudBatching();
        leftLayout.renderComponent(stack, tickDelta);
        rightLayout.renderComponent(stack, tickDelta);
        ControlifyCompat.ifEndHudBatching();
    }

    private static boolean isScreenCompatible(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }
}
