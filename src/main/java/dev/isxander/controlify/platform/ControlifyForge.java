package dev.isxander.controlify.platform;

import dev.isxander.controlify.sound.ControlifySounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ControlifyForge.MOD_ID)
public class ControlifyForge {
    public static final String MOD_ID = "controlify";

    public ControlifyForge() {
        var modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        ControlifySounds.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ControlifyClient.init(modBus);
        }
    }
}
