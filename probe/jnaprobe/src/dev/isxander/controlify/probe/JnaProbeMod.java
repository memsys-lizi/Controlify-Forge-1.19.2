package dev.isxander.controlify.probe;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;

/**
 * Stand-in for mods such as Health Overlay. Forge names this module {@code healthoverlay}
 * (the mod id). It reads {@code com.sun.jna} and {@code com.sun.jna.platform}, which is the
 * split-package crash when a second JNA jar is also a game-layer module.
 *
 * <p>The jar also carries {@code jna-5.12.1.jar} under {@code META-INF/libraries}. That copy
 * is not Jar-in-Jar metadata, so FML does not install it as {@code com.sun.jna}.
 */
@Mod("healthoverlay")
public final class JnaProbeMod {
    private static final Logger LOGGER = LogManager.getLogger("healthoverlay");

    public JnaProbeMod() {
        ClassLoader loader = JnaProbeMod.class.getClassLoader();
        try {
            Class<?> jna = Class.forName("com.sun.jna.Native", false, loader);
            Class<?> platform = Class.forName("com.sun.jna.platform.WindowUtils", false, loader);
            Class<?> internal = Class.forName("com.sun.jna.internal.ReflectionUtils", false, loader);
            boolean bundled = false;
            try (InputStream in = JnaProbeMod.class.getResourceAsStream("/META-INF/libraries/jna-5.12.1.jar")) {
                bundled = in != null;
            }
            LOGGER.info(
                    "JNA probe module healthoverlay sees module {} class {} ; {} ; {} ; bundled jar present={}",
                    jna.getModule().getName(),
                    jna.getName(),
                    platform.getName(),
                    internal.getName(),
                    bundled
            );
        } catch (Throwable thrown) {
            LOGGER.error("JNA probe failed to read com.sun.jna and com.sun.jna.platform", thrown);
            throw new RuntimeException("JNA probe could not see Forge's JNA modules", thrown);
        }
    }
}
