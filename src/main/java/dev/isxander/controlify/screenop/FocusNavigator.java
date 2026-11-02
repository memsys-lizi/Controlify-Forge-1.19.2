package dev.isxander.controlify.screenop;

import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.mixins.feature.screenop.vanilla.AbstractSelectionListAccessor;
import dev.isxander.controlify.utils.NavigationHelper;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/**
 * Directional focus for 1.19.2 screens. That version has {@code changeFocus(boolean)}
 * and no arrow-key path, so each direction picks the nearest widget in that direction.
 * Selection lists move one row at a time and scroll the row into view.
 */
public final class FocusNavigator {
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public record Bounds(int x, int y, int width, int height) {
        public double centerX() {
            return x + width / 2.0;
        }

        public double centerY() {
            return y + height / 2.0;
        }
    }

    private FocusNavigator() {
    }

    /**
     * Lower is closer. Empty when {@code to} is not in {@code direction} from {@code from}.
     */
    public static Double score(Bounds from, Bounds to, Direction direction) {
        double dx = to.centerX() - from.centerX();
        double dy = to.centerY() - from.centerY();
        double primary;
        double secondary;
        switch (direction) {
            case RIGHT -> {
                primary = dx;
                secondary = Math.abs(dy);
            }
            case LEFT -> {
                primary = -dx;
                secondary = Math.abs(dy);
            }
            case DOWN -> {
                primary = dy;
                secondary = Math.abs(dx);
            }
            case UP -> {
                primary = -dy;
                secondary = Math.abs(dx);
            }
            default -> {
                return null;
            }
        }
        if (primary <= 1) {
            return null;
        }
        if (secondary > primary * 2.5 + 24) {
            return null;
        }
        return primary + secondary * 0.35;
    }

    public static Direction readDirection(Controller<?, ?> controller, NavigationHelper helper) {
        var bindings = controller.bindings();
        boolean repeat = helper.canNavigate();
        Direction direction = null;
        boolean fresh = false;

        if (bindings.GUI_NAVI_RIGHT.held() && (repeat || !bindings.GUI_NAVI_RIGHT.prevHeld())) {
            direction = Direction.RIGHT;
            fresh = !bindings.GUI_NAVI_RIGHT.prevHeld();
        } else if (bindings.GUI_NAVI_LEFT.held() && (repeat || !bindings.GUI_NAVI_LEFT.prevHeld())) {
            direction = Direction.LEFT;
            fresh = !bindings.GUI_NAVI_LEFT.prevHeld();
        } else if (bindings.GUI_NAVI_DOWN.held() && (repeat || !bindings.GUI_NAVI_DOWN.prevHeld())) {
            direction = Direction.DOWN;
            fresh = !bindings.GUI_NAVI_DOWN.prevHeld();
        } else if (bindings.GUI_NAVI_UP.held() && (repeat || !bindings.GUI_NAVI_UP.prevHeld())) {
            direction = Direction.UP;
            fresh = !bindings.GUI_NAVI_UP.prevHeld();
        }

        if (direction != null && fresh) {
            helper.reset();
        }
        return direction;
    }

    public static boolean move(Screen screen, Direction direction) {
        GuiEventListener current = screen.getFocused();
        if (current instanceof AbstractSelectionList<?> list && moveWithinList(list, direction)) {
            return true;
        }

        Bounds origin = boundsOf(current);
        GuiEventListener best = null;
        double bestScore = Double.MAX_VALUE;
        for (GuiEventListener child : screen.children()) {
            if (child == current || !focusable(child)) {
                continue;
            }
            Bounds bounds = boundsOf(child);
            if (bounds == null) {
                continue;
            }
            if (origin == null) {
                best = child;
                break;
            }
            Double candidate = score(origin, bounds, direction);
            if (candidate != null && candidate < bestScore) {
                bestScore = candidate;
                best = child;
            }
        }

        if (best == null) {
            return false;
        }
        focus(screen, best, direction);
        return true;
    }

    public static boolean focusable(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            return widget.visible && widget.active;
        }
        if (listener instanceof AbstractSelectionList<?> list) {
            return !list.children().isEmpty();
        }
        return false;
    }

    public static Bounds boundsOf(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            return new Bounds(widget.x, widget.y, widget.getWidth(), Math.max(1, widget.getHeight()));
        }
        if (listener instanceof AbstractSelectionList<?> list) {
            return new Bounds(list.getLeft(), list.getTop(), Math.max(1, list.getWidth()), Math.max(1, list.getBottom() - list.getTop()));
        }
        return null;
    }

    private static boolean moveWithinList(AbstractSelectionList<?> list, Direction direction) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return false;
        }
        var children = list.children();
        if (children.isEmpty()) {
            return false;
        }
        int index = children.indexOf(list.getSelected());
        if (index < 0) {
            index = 0;
        }
        int next = index + (direction == Direction.DOWN ? 1 : -1);
        if (next < 0 || next >= children.size()) {
            return false;
        }
        selectEntry(list, children.get(next));
        return true;
    }

    private static void focus(Screen screen, GuiEventListener next, Direction direction) {
        screen.setFocused(next);
        if (next instanceof AbstractSelectionList<?> list && !list.children().isEmpty()) {
            var selected = list.getSelected();
            if (selected == null || !list.children().contains(selected)) {
                int index = direction == Direction.UP ? list.children().size() - 1 : 0;
                selected = list.children().get(index);
            }
            selectEntry(list, selected);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void selectEntry(AbstractSelectionList list, AbstractSelectionList.Entry entry) {
        list.setSelected(entry);
        list.setFocused(entry);
        if (entry instanceof ContainerObjectSelectionList.Entry row && row.getFocused() == null && !row.children().isEmpty()) {
            row.setFocused((GuiEventListener) row.children().get(0));
        }
        scrollEntryIntoView(list, list.children().indexOf(entry));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void scrollEntryIntoView(AbstractSelectionList list, int index) {
        if (index < 0) {
            return;
        }
        int itemHeight = ((AbstractSelectionListAccessor) list).controlify$getItemHeight();
        double rowTop = (double) index * itemHeight;
        double rowBottom = rowTop + itemHeight;
        double scroll = list.getScrollAmount();
        int view = Math.max(1, list.getBottom() - list.getTop());
        if (rowTop < scroll) {
            list.setScrollAmount(rowTop);
        } else if (rowBottom > scroll + view) {
            list.setScrollAmount(rowBottom - view);
        }
    }
}
