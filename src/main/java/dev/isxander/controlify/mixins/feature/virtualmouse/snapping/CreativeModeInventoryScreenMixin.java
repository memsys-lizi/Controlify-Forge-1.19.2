package dev.isxander.controlify.mixins.feature.virtualmouse.snapping;

import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreenMixin<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private float scrollOffs;
    @Shadow private EditBox searchBox;
    @Shadow protected abstract boolean canScroll();

    protected CreativeModeInventoryScreenMixin(Component title) {
        super(title);
    }

    @Override
    public Set<SnapPoint> getSnapPoints() {
        Set<SnapPoint> points = super.getSnapPoints();
        for (var tab : CreativeModeTab.TABS) {
            if (tab == null) continue;
            int column = tab.getColumn();
            int tabX = 28 * column;
            if (tab.isAlignedRight()) {
                tabX = imageWidth - 28 * (6 - column) + 2;
            } else if (column > 0) {
                tabX += column;
            }
            int tabY = tab.isTopRow() ? -32 : imageHeight;

            points.add(new SnapPoint(new Vector2i(leftPos + tabX + 14, topPos + tabY + 16), 18));
        }

        if (canScroll()) {
            int scrollTop = topPos + 18;
            int scrollBottom = scrollTop + 112;
            points.add(new SnapPoint(new Vector2i(leftPos + 175 + 6, scrollTop + (int)((float)(scrollBottom - scrollTop - 17) * scrollOffs) + 7), 15));
        }

        if (searchBox.isVisible())
            points.add(new SnapPoint(new Vector2i(searchBox.x + searchBox.getWidth() / 2, searchBox.y + searchBox.getHeight() / 2), searchBox.getHeight() + 2));

        return points;
    }
}
