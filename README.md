# Controlify (Unofficial Forge 1.19.2 Port)

Unofficial port of [Controlify](https://github.com/isXander/Controlify) **1.2.1** from Fabric 1.19.4 (tag `1.2.1+1.19.4`) to **Minecraft 1.19.2 / Forge 43.5.2**.

This is not an official isXander release and it is not supported upstream. The original copyright is kept. The license is still **GNU LGPL-3.0-or-later** (see [LICENSE](LICENSE)).

In-game display name: **Controlify (Unofficial Forge 1.19.2 Port)**.

What changed, which mixins are disabled, and what later phases still have to do is in [PORTING.md](PORTING.md). The original feature write-up is in [.github/README.md](.github/README.md).

## Phase 0 status

This branch builds a client jar and boots the game. Phase 1 drives movement, look, attack, use, jump, sneak, sprint, hotbar, inventory, and pause from the controller through Forge `KeyMapping` (conflict context and key modifier). GUI navigation, rumble, and the config screen are still later phases.

```bash
./gradlew build
./gradlew runClient
```

`runClient` sets `controlify.loadSdlOnStartup=true`, so SDL2 is initialised during startup. A physical controller is not required for that check: the native library should download and load, and the game should reach the main menu. `./gradlew runClientJnaProbe` boots the jar-in-jar build next to a small mod that reads Forge's JNA the way Health Overlay does.

Java 17 is required. ForgeGradle uses the official Mojang mappings.
