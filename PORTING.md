# Controlify → Forge 1.19.2 (unofficial)

This is an **unofficial** port of Controlify 1.2.1 (Fabric, Minecraft 1.19.4, tag `1.2.1+1.19.4`) to **Forge 43.5.2 / Minecraft 1.19.2**. It is not published or maintained by isXander. Original copyright stays with isXander. The license is unchanged: **LGPL-3.0-or-later**.

The mod display name is **Controlify (Unofficial Forge 1.19.2 Port)**.

Phase 0 is a bootable jar. Phase 1 wires controller gameplay through Forge `KeyMapping`. Virtual-mouse navigation, rumble, and the config screen are still later phases.

## What phase 0 changed

- Build system is ForgeGradle 6 (not Architectury, not Loom). Minecraft 1.19.2, official Mojang mappings, Java 17.
- Fabric API is gone. Lifecycle, tick, and pack registration use Forge events. The public `Event` helper is a small local list, and its invokers also post Forge events (`ControlifyPreInitEvent`, `ControllersDiscoveredEvent`, `InputModeChangedEvent`, `ControllerStateUpdateEvent`, `IngameGuideRegistryForgeEvent`, `VirtualMouseToggledEvent`). Entrypoints are `ServiceLoader` of `dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint` (no `META-INF/services` file is bundled).
- Key mappings are registered on `RegisterKeyMappingsEvent` (`ControlifyKeys`). Imitated vanilla clicks skip a `KeyMapping` whose `IKeyConflictContext` is inactive, or whose `KeyModifier` is not `NONE` and is not active. Modded key ids are `controlify:key.<sanitized>` (they used to be `fabric-key-binding-api-v1:<sanitized>`).
- Config stays JSON at `<game dir>/config/controlify.json` (`FMLPaths.CONFIGDIR`). There is no config screen. YACL 2.x has no Forge 1.19.2 build, so it was removed. `startYACLOption()` was removed from the binding API.
- Sodium, Iris, and Mod Menu compatibility code was deleted. ImmediatelyFast is a no-op (`IMMEDIATELY_FAST` is always false); there is no Forge 1.19.2 compile dependency.
- GUI calls were lowered to the 1.19.2 API: `Button` constructors (no `Button.builder`), no `Tooltip`, no `ComponentPath` / `FocusNavigationEvent` / `ScreenDirection` / `InputType` / `TabNavigationBar` / `GridLayout`. Widgets use public `x` / `y` fields. `Renderable` is `Widget`.
- Screen navigation is a stub. `ScreenProcessor` calls `changeFocus` and clears focus. Tab navigation does nothing. Full UI navigation is phase 2.
- Sounds use a Forge `DeferredRegister` (`controlify:controlify.ui.focus`). The extra mappings pack is format 9 and is added with `AddPackFindersEvent`.
- `NameableEnum` moved to `dev.isxander.controlify.utils.NameableEnum` (`getDisplayName` only).
- Client init runs from `ControlifyForge` only when `Dist.CLIENT`. All mixins are in the client array, and the mixin plugin refuses them on a dedicated server.
- `fabric.mod.json` is excluded from the jar. This build is not a Fabric mod.
- SDL2 natives stay a runtime download (version `2.26.5-18`) into `<game dir>/controlify-natives`. The Gradle client run sets `controlify.loadSdlOnStartup=true` so startup initialises SDL without waiting for the onboarding screen.

### Libraries embedded with jarJar

| Artifact | Why |
| --- | --- |
| `com.github.llamalad7.mixinextras:mixinextras-forge:0.2.0-beta.8` | MixinExtras Forge mod. Compile against `mixinextras-common` of the same version. |
| `org.hid4java:hid4java:0.7.0` | Controller connection identification. |
| `net.java.dev.jna:jna:5.6.0` | hid4java native backend. Not requested by name, but hid4java does not work without it. |
| `dev.isxander:sdl2-jni:2.26.5-18` | Rumble / SDL. Natives are downloaded, not embedded. |
| `org.quiltmc:quilt-json5:1.0.3` | `hiddb.json5`. |
| `org.jetbrains:annotations:20.1.0` | Compile dependency of quilt-json5. |
| `org.joml:joml:1.10.5` | 1.19.2 does not bundle JOML. The 1.19.4 sources use it for layout and the virtual cursor. |

The same coordinates are also `implementation` so they are on the compile and dev runtime classpath. jarJar is not `fromRuntimeConfiguration` (that would embed Minecraft and Forge).

Gradle `runClient` / `runServer` also put JOML, hid4java, sdl2-jni, and quilt-json5 on the Forge userdev Minecraft classpath (`minecraftArtifacts`). BootstrapLauncher builds the game module layer from that list, not from the Gradle `-cp`, so mixins cannot see those classes otherwise. JNA 5.12 and JetBrains annotations 23 are already on that classpath; the older jarJar copies are not added again. Userdev does not load jar-in-jar mods, so the MixinExtras Forge jar is copied into `run/mods` for those runs. The production `-all` jar still embeds it.

## Mixins

About 80 mixins were checked against 1.19.2. Ones that target classes or members that do not exist on 1.19.2 were removed from `controlify.mixins.json`. Their sources are kept under `src/port-disabled/` (not compiled) or deleted with the compatibility modules they belonged to.

`@Shadow` names are remapped by the Mixin annotation processor into `build/tmp/compileJava/out.tsrg`, and `reobfJar` consumes that file. The refmap only remaps annotation strings (`@Inject`, `@At`, `@Accessor`). A mixin class missing from `controlify.refmap.json` is expected when it only has shadows.

### Disabled, source kept in `src/port-disabled`

| Mixin | Why it is off |
| --- | --- |
| `feature.guide.screen.TabNavigationBarMixin` | `TabNavigationBar` does not exist in 1.19.2. |
| `feature.screenop.vanilla.TabNavigationBarAccessor` | Same class. |
| `feature.screenop.vanilla.CreateWorldScreenMixin` | Create World init was rewritten around `GridLayout`, which 1.19.2 does not have. |
| `feature.screenop.vanilla.AbstractSelectionListMixin` | Injects `InputType.isKeyboard`, which does not exist. |
| `feature.screenop.vanilla.AbstractSliderButtonMixin` | `canChangeValue` and `getLastInputType` do not exist. |
| `feature.settingsbutton.ControlsScreenMixin` | Opened the YACL config screen and used `Button.builder`. |

`CreateWorldScreenProcessor` and `SliderComponentProcessor` still compile and are unused while those mixins are off.

### Deleted with Sodium / Iris / Mod Menu / YACL / Fabric API

- `compat.iris.BaseOptionElementWidgetMixin`
- `compat.sodium.CycleControlElementMixin`
- `compat.sodium.SliderControlElementMixin`
- `compat.sodium.TickBoxControlElementMixin`
- `compat.sodium.SodiumOptionsGUIAccessor`
- `compat.sodium.SodiumOptionsGUIMixin`
- `compat.yacl.CyclingControllerElementMixin`
- `compat.yacl.SliderControllerElementMixin`
- `compat.yacl.YACLScreenMixin`
- `compat.fapi.KeyBindingRegistryImplAccessor` (replaced by `KeyMapping.ALL` plus `RegisterKeyMappingsEvent`)

### Still applied, but behaviour is reduced

- `feature.chatkbheight.ChatComponentMixin` still shifts chat while the chat screen is open. The 1.19.4 `@ModifyConstant` of the 40px render inset is `require = 0` because 1.19.2 `ChatComponent.render` has no such constant. `render` here is `(PoseStack, int)`.
- `feature.screenop.ScreenMixin` injects at the return of `rebuildWidgets`. On 1.19.2, `Screen.init(Minecraft, int, int)` calls `rebuildWidgets()` (which calls `init()`), so the 1.19.4 inject after `init()` had nothing to hit.
- `core.ClientPacketListenerMixin` and `feature.guide.ingame.ClientPacketListenerMixin` inject after the `PUTFIELD` that stores `LocalPlayer.input`. 1.19.2 writes the new `KeyboardInput` to that field.
- `feature.guide.screen.AbstractWidgetMixin` / `AbstractButtonMixin` draw button guides from `renderButton`. 1.19.2 has no `renderScrollingString`, so the scrolling-label shift is gone.
- `feature.screenop.ScreenProcessor` does not do 1.19.4 `ComponentPath` navigation.
- `feature.screenop.GameRendererMixin` and `feature.virtualmouse.GameRendererMixin` inject after `ForgeHooksClient.drawScreen` and allocate a new `PoseStack` for the overlay.
- `feature.rumble.blockbreak.MultiPlayerGameModeMixin` injects Forge's patched `lambda$startDestroyBlock$1` (the lambda takes `PlayerInteractEvent.LeftClickBlock`). That name is the same in the official and SRG Forge jars, so it is not in the refmap. If a future Forge build renames it, block-break rumble will fail to apply and the game will not boot until the inject is updated.
- `ContainerButtonGuide.setup()` is still never called (same as upstream 1.2.1). The class was ported to Forge screen events so it compiles.

## Phase 1: in-game controls

Controller gameplay uses the 1.2.1 binds (left stick move, right stick look, triggers attack/use, A jump, stick click sneak, stick click sprint, bumpers change hotbar slot, Y inventory, Start pause). There is no config screen yet, so the defaults are what you get.

- Movement is `ControllerPlayerMovement`, installed when a controller is current and again on login/respawn. Jump and sneak are mirrored onto `keyJump` and `keyShift`, including toggle sneak and the rule that a jump button held across a GUI does not jump when the GUI closes.
- Look runs from the right stick during `MouseHandler.turnPlayer`. Controller mode does not require the cursor to be grabbed.
- Attack, use/place, drop, swap hands, inventory, perspective, chat, pick block, sprint, and the player list press that one `KeyMapping`. `isDown()` and `consumeClick()` see it. `KeyMapping.set` / `KeyMapping.click` are not used: on Forge those hit every mapping bound to the same key, and `click` follows whichever modifier is physically held.
- A controller bind names one mapping. Its `IKeyConflictContext` must be active (in-game versus GUI). Its `KeyModifier` selects the mapping and is not required on the keyboard, so a Shift-modified mod bind still fires from the controller without holding Shift, and a plain bind does not also fire the Shift variant.
- Toggle mappings (`ToggleKeyMapping`, or Controlify's toggle sprint) latch on press. Holding them does not flip every tick.
- Hotbar bumpers call `Inventory.swapPaint`. Spectator mode also notifies the spectator hotbar. Slots 1–9 stay keyboard keys.
- Pause calls `pauseGame`. It is not a `KeyMapping`.

## Later phases (not in this change)

1. Movement, look, attack, place, and hotbar are in phase 1.
2. Virtual mouse and GUI navigation (the 1.19.4 focus API has to be rebuilt; `TabNavigationBar` and Create World stay disabled until then).
3. Button hints, rumble (this is the part players care about most), reach-around assist.
4. Config screen (no YACL on this Forge version; a Forge or handmade screen).
5. Integration pass.

Input conflicts to watch while doing that work:

- Epic Fight
- MrCrayfish's Gun Mod (`cgm`)
- Simple Voice Chat
- WebDisplays / MCEF
- KeybindsGalore-Forge

## Building and checking SDL

```bash
./gradlew build
./gradlew runClient
```

`runClient` is headless-unfriendly. On a machine without a display, run it under `xvfb-run`. The client property `controlify.loadSdlOnStartup=true` is only on the Gradle run, not inside the jar.

A successful SDL check in `run/logs/latest.log` looks like:

- `Downloading SDL2 native library` or a load from `controlify-natives/`
- `Initialised SDL2` (debug) and no `Failed to load SDL2` / `Failed to initialise SDL2`
- The main menu, with no controller attached

Install `build/libs/controlify-1.2.1+1.19.2-forge-all.jar`. That is the reobfuscated jar with jarJar libraries (MixinExtras, hid4java, JNA, sdl2-jni, quilt-json5, annotations, JOML). The thinner `controlify-1.2.1+1.19.2-forge.jar` is the same code without those libraries and will not load. Both are production (SRG) jars. Do not put them on the Gradle dev classpath.
