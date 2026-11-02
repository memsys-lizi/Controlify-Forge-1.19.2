package dev.isxander.controlify.compatibility;

import net.minecraftforge.fml.ModList;

public class ControlifyCompat {
    /**
     * ImmediatelyFast has no Forge 1.19.2 build that this port compiles against.
     * HUD batching is a no-op until a compatible build exists.
     */
    public static final boolean IMMEDIATELY_FAST = false;

    public static void ifBeginHudBatching() {
    }

    public static void ifEndHudBatching() {
    }

    public static boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
