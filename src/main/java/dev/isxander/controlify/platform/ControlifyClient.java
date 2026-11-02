package dev.isxander.controlify.platform;

import dev.isxander.controlify.Controlify;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-only Forge hooks. Loaded from {@link ControlifyForge} only when the dist is the client,
 * so a dedicated server does not resolve Minecraft client classes while linking this class's caller
 * until the client branch actually runs.
 */
public final class ControlifyClient {
    private ControlifyClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ControlifyKeys::onRegisterKeys);
        modBus.addListener(ControlifyClient::onAddPackFinders);
        MinecraftForge.EVENT_BUS.addListener(ControlifyClient::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Controlify.isReadyToTick()) {
            return;
        }
        Controlify.instance().tick(Minecraft.getInstance());
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        var modFile = ModList.get().getModFileById("controlify");
        if (modFile == null) {
            Controlify.LOGGER.warn("Could not locate the Controlify mod file; extra joystick mappings were not registered.");
            return;
        }

        Path path = modFile.getFile().findResource("resourcepacks", "extra_mappings");
        if (!Files.isDirectory(path)) {
            Controlify.LOGGER.warn("Extra mappings resource pack is missing at {}", path);
            return;
        }

        event.addRepositorySource((consumer, constructor) -> {
            Pack pack = constructor.create(
                    "controlify:extra_mappings",
                    Component.translatable("controlify.resource_pack.extra_mappings"),
                    true,
                    () -> new PathPackResources("controlify_extra_mappings", path),
                    new PackMetadataSection(Component.translatable("controlify.resource_pack.extra_mappings"), 9),
                    Pack.Position.TOP,
                    PackSource.BUILT_IN,
                    false
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
