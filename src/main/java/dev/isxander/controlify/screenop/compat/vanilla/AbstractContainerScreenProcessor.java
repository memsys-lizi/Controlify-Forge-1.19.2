package dev.isxander.controlify.screenop.compat.vanilla;

import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.bindings.ControllerBindings;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.gui.guide.ContainerGuideCtx;
import dev.isxander.controlify.gui.guide.GuideAction;
import dev.isxander.controlify.gui.guide.GuideActionRenderer;
import dev.isxander.controlify.gui.layout.AnchorPoint;
import dev.isxander.controlify.gui.layout.ColumnLayoutComponent;
import dev.isxander.controlify.gui.layout.PositionedComponent;
import dev.isxander.controlify.gui.layout.RowLayoutComponent;
import dev.isxander.controlify.mixins.feature.guide.screen.AbstractContainerScreenAccessor;
import dev.isxander.controlify.mixins.feature.screenop.ScreenAccessor;
import dev.isxander.controlify.screenop.FocusNavigator;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseBehaviour;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AbstractContainerScreenProcessor<T extends AbstractContainerScreen<?>> extends ScreenProcessor<T> {
    private PositionedComponent<ColumnLayoutComponent<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>> leftLayout;
    private PositionedComponent<ColumnLayoutComponent<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>> rightLayout;

    private final Supplier<Slot> hoveredSlot;
    private final ClickSlotFunction clickSlotFunction;
    private boolean soughtInitialSlot;

    public AbstractContainerScreenProcessor(T screen, Supplier<Slot> hoveredSlot, ClickSlotFunction clickSlotFunction) {
        super(screen);
        this.hoveredSlot = hoveredSlot;
        this.clickSlotFunction = clickSlotFunction;
    }

    @Override
    protected void handleScreenVMouse(Controller<?, ?> controller, VirtualMouseHandler vmouse) {
        if (virtualMouseBehaviour() == VirtualMouseBehaviour.CURSOR_ONLY) {
            navigateCursor(controller, vmouse);
        }

        var accessor = (AbstractContainerScreenAccessor) screen;
        ContainerGuideCtx ctx = new ContainerGuideCtx(hoveredSlot.get(), screen.getMenu().getCarried(), accessor.invokeHasClickedOutside(vmouse.getCurrentX(1f), vmouse.getCurrentY(1f), accessor.getLeftPos(), accessor.getTopPos(), 0));

        Slot hoveredSlot = this.hoveredSlot.get();
        if (hoveredSlot != null) {
            if (controller.bindings().DROP.justPressed() && hoveredSlot.hasItem()) {
                clickSlotFunction.clickSlot(hoveredSlot, hoveredSlot.index, 0, ClickType.THROW);
            }

            if (controller.bindings().INV_SELECT.justPressed()) {
                clickSlotFunction.clickSlot(hoveredSlot, hoveredSlot.index, 0, ClickType.PICKUP);
            }

            if (controller.bindings().INV_QUICK_MOVE.justPressed()) {
                clickSlotFunction.clickSlot(hoveredSlot, hoveredSlot.index, 0, ClickType.QUICK_MOVE);
            }

            if (controller.bindings().INV_TAKE_HALF.justPressed()) {
                clickSlotFunction.clickSlot(hoveredSlot, hoveredSlot.index, 1, ClickType.PICKUP);
            }

            if (controller.bindings().SWAP_HANDS.justPressed()) {
                clickSlotFunction.clickSlot(hoveredSlot, hoveredSlot.index, 40, ClickType.SWAP);
            }
        } else {
            vmouse.handleCompatibilityBinds(controller);
        }

        if (leftLayout != null && rightLayout != null) {
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
        }
    }

    /**
     * Left stick steps between slot centers and visible widgets. Hover updates on the next render.
     */
    private void navigateCursor(Controller<?, ?> controller, VirtualMouseHandler vmouse) {
        var accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.getLeftPos();
        int top = accessor.getTopPos();
        List<FocusNavigator.Bounds> targets = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()) {
                targets.add(new FocusNavigator.Bounds(left + slot.x, top + slot.y, 16, 16));
            }
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible && widget.active) {
                targets.add(new FocusNavigator.Bounds(widget.x, widget.y, widget.getWidth(), Math.max(1, widget.getHeight())));
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        FocusNavigator.Direction direction = FocusNavigator.readDirection(controller, navigationHelper);
        Slot hovered = hoveredSlot.get();
        FocusNavigator.Bounds from = hovered != null
                ? new FocusNavigator.Bounds(left + hovered.x, top + hovered.y, 16, 16)
                : new FocusNavigator.Bounds(vmouse.getCurrentX(1f), vmouse.getCurrentY(1f), 1, 1);

        if (direction == null) {
            if (hovered == null && !soughtInitialSlot) {
                soughtInitialSlot = true;
                FocusNavigator.Bounds nearest = null;
                double best = Double.MAX_VALUE;
                for (FocusNavigator.Bounds target : targets) {
                    double dx = target.centerX() - from.centerX();
                    double dy = target.centerY() - from.centerY();
                    double distance = dx * dx + dy * dy;
                    if (distance < best) {
                        best = distance;
                        nearest = target;
                    }
                }
                if (nearest != null) {
                    vmouse.moveToGui(nearest.centerX(), nearest.centerY());
                }
            }
            return;
        }

        FocusNavigator.Bounds bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        for (FocusNavigator.Bounds target : targets) {
            Double score = FocusNavigator.score(from, target, direction);
            if (score != null && score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        if (bestTarget != null) {
            vmouse.moveToGui(bestTarget.centerX(), bestTarget.centerY());
            navigationHelper.onNavigate();
            soughtInitialSlot = true;
        }
    }

    @Override
    public void onWidgetRebuild() {
        ControllerBindings<?> bindings = ControlifyApi.get().getCurrentController()
                .map(Controller::bindings)
                .orElse(null);

        if (bindings == null) {
            return;
        }

        leftLayout = new PositionedComponent<>(
                ColumnLayoutComponent.<RowLayoutComponent<GuideActionRenderer<ContainerGuideCtx>>>builder()
                        .spacing(2)
                        .elementPosition(ColumnLayoutComponent.ElementPosition.LEFT)
                        .colPadding(2)
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
                        .colPadding(2)
                        .element(RowLayoutComponent.<GuideActionRenderer<ContainerGuideCtx>>builder()
                                .spacing(5)
                                .rowPadding(0)
                                .elementPosition(RowLayoutComponent.ElementPosition.MIDDLE)
                                .element(new GuideActionRenderer<>(
                                        new GuideAction<>(bindings.DROP, ctx -> {
                                            if (ctx.hoveredSlot() != null && ctx.hoveredSlot().hasItem())
                                                return Optional.of(Component.translatable("controlify.guide.container.drop"));
                                            return Optional.empty();
                                        }),
                                        true, false
                                ))
                                .build())
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
                                            if (ctx.hoveredSlot() != null && ctx.hoveredSlot().hasItem())
                                                return Optional.of(Component.translatable("controlify.guide.container.quick_move"));
                                            return Optional.empty();
                                        }),
                                        true, false
                                ))
                                .build())
                        .build(),
                AnchorPoint.BOTTOM_RIGHT,
                0, 0,
                AnchorPoint.BOTTOM_RIGHT
        );

        if (ControlifyApi.get().currentInputMode() == InputMode.CONTROLLER) {
            setRenderGuide(true);
        }
    }

    @Override
    public void onInputModeChanged(InputMode mode) {
        setRenderGuide(mode == InputMode.CONTROLLER);
    }

    private void setRenderGuide(boolean render) {
        List<Widget> renderables = ((ScreenAccessor) screen).getRenderables();

        if (leftLayout == null || rightLayout == null)
            return;

        if (render) {
            if (!renderables.contains(leftLayout))
                renderables.add(leftLayout);
            if (!renderables.contains(rightLayout))
                renderables.add(rightLayout);
        } else {
            renderables.remove(leftLayout);
            renderables.remove(rightLayout);
        }
    }

    @Override
    public VirtualMouseBehaviour virtualMouseBehaviour() {
        return VirtualMouseBehaviour.CURSOR_ONLY;
    }

    @FunctionalInterface
    public interface ClickSlotFunction {
        void clickSlot(Slot slot, int slotId, int button, ClickType clickType);
    }
}
