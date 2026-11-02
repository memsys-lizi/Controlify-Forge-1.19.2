package dev.isxander.controlify.platform;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.controlify.Controlify;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Registers Controlify's own {@link KeyMapping}s on {@link RegisterKeyMappingsEvent}.
 * Controlify 1.2.1 does not create vanilla keys of its own; the helper is here so later
 * phases and other mods can register keys that honour Forge's conflict context and modifier.
 */
public final class ControlifyKeys {
    private static final List<KeyMapping> PENDING = new ArrayList<>();
    private static boolean registrationOpen = true;

    private ControlifyKeys() {
    }

    public static KeyMapping register(String name, IKeyConflictContext context, KeyModifier modifier, InputConstants.Type type, int keyCode, String category) {
        return register(new KeyMapping(name, context, modifier, type, keyCode, category));
    }

    public static KeyMapping register(String name, IKeyConflictContext context, KeyModifier modifier, InputConstants.Key key, String category) {
        return register(new KeyMapping(name, context, modifier, key, category));
    }

    public static KeyMapping register(String name, int keyCode, String category) {
        return register(name, KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, keyCode, category);
    }

    private static KeyMapping register(KeyMapping mapping) {
        if (registrationOpen) {
            PENDING.add(mapping);
        } else {
            Controlify.LOGGER.warn("Key mapping {} was created after RegisterKeyMappingsEvent. It is in KeyMapping.ALL but was not added to the controls screen.", mapping.getName());
        }
        return mapping;
    }

    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        registrationOpen = false;
        for (KeyMapping mapping : PENDING) {
            event.register(mapping);
        }
        PENDING.clear();
    }

    /**
     * True when this mapping's {@link IKeyConflictContext} is active (in-game, GUI, or universal).
     * A controller bind names one mapping, so the physical {@link KeyModifier} is not required:
     * the modifier only chooses which mapping shares a keyboard key. Context still gates it.
     */
    public static boolean isContextActive(KeyMapping mapping) {
        IKeyConflictContext context = mapping.getKeyConflictContext();
        return context == null || context.isActive();
    }

    /**
     * Forge key mappings can be inactive because of {@link IKeyConflictContext} (in-game vs GUI)
     * or because a {@link KeyModifier} such as Shift is not held. Keyboard-style checks use both.
     * Controller imitation uses {@link #isContextActive} and presses that one mapping.
     */
    public static boolean isKeyActive(KeyMapping mapping) {
        if (!isContextActive(mapping)) {
            return false;
        }
        IKeyConflictContext context = mapping.getKeyConflictContext();
        KeyModifier modifier = mapping.getKeyModifier();
        if (modifier != null && modifier != KeyModifier.NONE) {
            IKeyConflictContext modifierContext = context != null ? context : KeyConflictContext.UNIVERSAL;
            if (!modifier.isActive(modifierContext)) {
                return false;
            }
        }
        return true;
    }

    private static final Set<KeyMapping> ASSERTED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<KeyMapping> ASSERTED_TOGGLES = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Mark a mapping as held by the controller. {@code isDown()} then ignores the physical
     * modifier and still requires the conflict context, unless {@code toggle} is set.
     * Toggle mappings stay down after the button is released, matching vanilla sneak/sprint.
     */
    public static void assertDown(KeyMapping mapping, boolean toggle) {
        ASSERTED.add(mapping);
        if (toggle) {
            ASSERTED_TOGGLES.add(mapping);
        } else {
            ASSERTED_TOGGLES.remove(mapping);
        }
    }

    public static void clearAsserted(KeyMapping mapping) {
        ASSERTED.remove(mapping);
        ASSERTED_TOGGLES.remove(mapping);
    }

    public static boolean isControllerAsserted(KeyMapping mapping) {
        return ASSERTED.contains(mapping);
    }

    public static boolean isAssertedToggle(KeyMapping mapping) {
        return ASSERTED_TOGGLES.contains(mapping);
    }
}
