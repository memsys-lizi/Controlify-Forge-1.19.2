package dev.isxander.controlify.utils;

import net.minecraft.network.chat.Component;

/**
 * Local replacement for YACL's {@code NameableEnum}. Config screens that displayed these names
 * are deferred to a later phase.
 */
public interface NameableEnum {
    Component getDisplayName();
}
