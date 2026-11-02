package dev.isxander.controlify.mixins.feature.bind;

import dev.isxander.controlify.platform.ControlifyKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Controller binds name one {@link KeyMapping}. While that bind is held, {@code isDown()}
 * follows the controller instead of the physical {@code KeyModifier}. Conflict context still applies,
 * except for toggle mappings (vanilla sprint/sneak stay latched).
 */
@Mixin({KeyMapping.class, ToggleKeyMapping.class})
public class KeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void controlify$controllerAsserted(CallbackInfoReturnable<Boolean> ci) {
        KeyMapping mapping = (KeyMapping) (Object) this;
        if (!ControlifyKeys.isControllerAsserted(mapping)) {
            return;
        }
        if (ControlifyKeys.isAssertedToggle(mapping)) {
            ci.setReturnValue(true);
            return;
        }
        ci.setReturnValue(ControlifyKeys.isContextActive(mapping));
    }
}
