package dev.isxander.controlify.screenop.compat.vanilla;

import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.mixins.feature.screenop.vanilla.CreativeModeInventoryScreenAccessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CreativeModeInventoryScreenProcessor extends AbstractContainerScreenProcessor<CreativeModeInventoryScreen> {
    public CreativeModeInventoryScreenProcessor(CreativeModeInventoryScreen screen, Supplier<Slot> hoveredSlot, ClickSlotFunction clickSlotFunction) {
        super(screen, hoveredSlot, clickSlotFunction);
    }

    @Override
    protected void handleScreenVMouse(Controller<?, ?> controller, VirtualMouseHandler vmouse) {
        if (controller.bindings().GUI_NEXT_TAB.justPressed()) {
            selectRelativeTab(1);
        }
        if (controller.bindings().GUI_PREV_TAB.justPressed()) {
            selectRelativeTab(-1);
        }

        super.handleScreenVMouse(controller, vmouse);
    }

    private void selectRelativeTab(int delta) {
        List<CreativeModeTab> tabs = Arrays.stream(CreativeModeTab.TABS).filter(tab -> tab != null).toList();
        if (tabs.isEmpty()) {
            return;
        }

        int currentId = screen.getSelectedTab();
        int index = 0;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).getId() == currentId) {
                index = i;
                break;
            }
        }

        int newIndex = index + delta;
        if (newIndex < 0) newIndex = tabs.size() - 1;
        if (newIndex >= tabs.size()) newIndex = 0;
        ((CreativeModeInventoryScreenAccessor) screen).invokeSelectTab(tabs.get(newIndex));
    }
}
