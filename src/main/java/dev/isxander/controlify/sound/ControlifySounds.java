package dev.isxander.controlify.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ControlifySounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "controlify");

    public static final RegistryObject<SoundEvent> SCREEN_FOCUS_CHANGE = SOUND_EVENTS.register(
            "controlify.ui.focus",
            () -> new SoundEvent(new ResourceLocation("controlify", "controlify.ui.focus"))
    );

    private ControlifySounds() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    public static void init() {
        // Registry entries are created by DeferredRegister during mod construction.
    }
}
