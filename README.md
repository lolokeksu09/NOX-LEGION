# NOX 1.8 — Spectra

Major offline AMOLED live-wallpaper update based on NOX 1.7.1. **Nine original GPU scenes** in one shared GLES renderer, no account/root/cloud or extra permissions.

**New live wallpapers**
- **VEIL:** slow silky luminous fog in dark void.
- **GRID:** receding cyberpunk perspective wireframe with soft horizon glow.
- **SHARDS:** floating, rotating faceted crystals with delicate edges.
Each has three ready-made looks: Silk Blue/Ether Rose/Jade Haze; Cyan Matrix/Neon Sunset/Arctic Wire; Obsidian/Amethyst/Prism.

**New function — Сюрприз:** cross-scene discovery chooses another one of nine scene types and a curated appearance, without changing stored presets. Use Home's horizontal nine-scene carousel, Variant/Looks panels, editor, PNG and system wallpaper application as before. 1.8 gallery shows GPU-free Canvas thumbnails for the new effects; scene categories, filters, sorting and favorites are preserved.

**Compatibility:** first six modes are unchanged (FLUX, HALO, NEBULA, AURORA, PULSE, COMET). The existing Room layout and versioned `.nox` import/export remain compatible with new 1.8 app; older apps may reject new 1.8 IDs as unsupported. Old installations retain their private data.

**Installation:** debug `studio.nox.light.spectra`, version `1.8.0`, code `12`. Installs *beside* 1.7.1 due Android app ID isolation — **do not uninstall the older working NOX**. Previous presets can only be moved using available in-app export/import, not read directly from another app's sandbox.

**Verification:** CI Python structural tests, Kotlin model tests and `:app:assembleDebug`. CI builds the Android APK but does not compile GLSL on the Lenovo's GPU; visual quality, performance, thermals, live wallpaper and touch response need actual device QA.

[Design specification](docs/superpowers/specs/2026-10-24-nox-1-8-spectra-design.md) · [Implementation plan](docs/superpowers/plans/2026-10-24-nox-1-8-spectra.md) · [Patch history](docs/CHANGELOG.md) · [Contributor requirements](AGENTS.md)

---

# NOX 1.7.1 — Light Dynamics

A renderer-focused improvement to the six-scene offline AMOLED live-wallpaper studio.

**Updated live effects**:
- **AURORA** — multilayer curtains, spectral glow, fine inner filaments and smoothly evolving folds
- **PULSE** — outward-traveling rings with seamless fade, controllable color density and an interactive center that follows touch
- **COMET** — individually angled, depth-layered trails with variable length/velocity and smooth cycle fades

Existing **FLUX, HALO and NEBULA shaders are intentionally unchanged**. The current pinned Studio preview, editor sliders, presets, offline `.nox` import/export, favorites and Home six-scene navigation are preserved. No new permissions, accounts, network calls or Room migrations.

**Installation:** NOX 1.7.1 has Android debug package `studio.nox.light.lumina`, version `1.7.1` (code 11), and installs *beside* NOX 1.7. Data does not sync between debug builds; keep your working install and its presets. Artifact name: `NOX-1.7.1-light-dynamics-APK`.

**Verification:** Gradle/JUnit and structural tests run through GitHub Actions; the mobile GPU GLSL compilation, visual quality, frame rates and thermal performance remain device-QA tasks until exercised on Lenovo Legion Y70.

[Design and rendering constraints](docs/superpowers/specs/2026-10-23-nox-1-7-1-light-dynamics-design.md) · [Implementation plan](docs/superpowers/plans/2026-10-23-nox-1-7-1-light-dynamics.md) · [Per-patch changelog](docs/CHANGELOG.md).

---

# NOX 1.7 Living Scenes — Native AMOLED wallpapers

**Status:** feature branch release candidate. Automated build and Lenovo hardware QA must be checked independently. The existing NOX 1.6 installation and stored presets remain untouched.

NOX now includes **six procedural live scenes** using the existing OpenGL ES backend:

- **FLUX / HALO / NEBULA** — original engines preserved.
- **AURORA** — flowing polar curtains over true OLED black. Polar, Emerald, Violet Mist.
- **PULSE** — concentric continuously moving energy rings. Cyan Beat, Crimson Core, Neon Echo.
- **COMET** — slim comet trails and smooth star motion. Deep Space, White Tail, Blue Drift.

### What's new

- Home: horizontal six-scene catalog, `NEW` labels for new scenes, 3 named looks in a bottom-sheet selector for each new mode. All edits go into an **unsaved draft** unless explicitly saved/applied.
- Gallery: categories Signature / Ambient / Tech / Cosmic, while retaining search, type filters, favorites, sorting, and duplicate/delete. Artwork thumbnails use **Canvas**, not extra EGL contexts.
- Studio: pinned live GL preview stays visible above the scrolling controls, with undo/redo and fixed Save/PNG/Apply actions.
- Settings: existing 30/60/120 FPS limits, sensor control and offline `.nox` import/export unchanged.
- Safety: no network, root, database migrations, extra Android permissions or tracking. GPU shader work uses bounded loops.

### Installation compatibility

Version name `1.7.0`, code `10`, debug applicationId `studio.nox.light.livingscenes` (separate preview package from 1.6). Never uninstall older NOX instances to try this release: their private presets are not automatically migrated. Older NOX builds may explicitly reject `.nox` files containing unknown scene IDs AURORA/PULSE/COMET. Export from an older build with export support, import into 1.7.

### Development documents

- [Design and architecture](docs/superpowers/specs/2026-10-23-nox-1-7-living-scenes-design.md)
- [Five-patch TDD implementation plan](docs/superpowers/plans/2026-10-23-nox-1-7-living-scenes.md)
- [Per-patch changelog](docs/CHANGELOG.md)
- [Mandatory patch commits/documentation](AGENTS.md) and [workflow](docs/DEVELOPMENT_WORKFLOW.md)

GitHub Actions artifact after a successful build: `NOX-1.7-living-scenes-APK`. Test all six live shaders on Lenovo Legion Y70 (wallpaper setup, lock/unlock, screenshot artifacts, thermals) before treating the feature as hardware-verified.

---

# NOX 1.6 Curated — Premium Live Light Studio

Native AMOLED scene editor for Lenovo Legion Y70 2026 and Android 16. Adds **two new offline editing features** without touching OpenGL shaders, Room presets, WallpaperService or pinned studio preview.

- **Favorites:** Tap the star on any preset gallery artwork. Your favorites are stored as local ID references in DataStore (no Room schema migration) and can be filtered together with scene type, name search and sorting. Deleting a preset also clears its favorite ID. Favorite status survives restarts in this NOX installation.
- **Quick Looks:** From Home, select **Стили** to browse five curated appearance sets (Abyss, Ion Violet, Solar, Arctic, Chroma) in a native bottom sheet. Choose one to preview it immediately. This modifies only an unsaved scene draft until you explicitly edit, export or apply it, so the original saved preset remains unchanged.
- **Premium finish:** Restrained graphite and cyan surfaces, compact palette swatches and precise stage numbering. The dynamic version footer remains accurate across builds.
- **Safety:** no internet, analytics, root or new permissions; no Room migration or EGL engine changes.

## Installation

Preview version 1.6.0 uses debug package `studio.nox.light.curated`, separate from previous debug builds because GitHub Actions debug signing isn't stable between runners. **Do not uninstall your working 1.5**; private presets stay in their original app. If desired, transfer presets with 1.5's local `.nox` export/import feature. Favorited IDs are local to this app and intentionally not included in the `.nox` scene archive.

CI artifact: `NOX-1.6-curated-APK` after Python structural checks, Kotlin JUnit and Android debug assembly. On-device animation, interactions and battery behavior require user verification.

---

# NOX 1.5 Portable Studio

Native offline AMOLED live-wallpaper studio for Android 16. Preserves NOX's GPU effects, pinned preview, editor/history, curated palettes, local Room presets and system wallpaper integration.

## What's new in 1.5

- **Portable library** in Settings: Export all presets to a user-chosen `.nox` file; import one from any previous NOX installation. Both use Android's system file chooser, no network or storage permissions. Imports **add** scenes; existing presets remain. Max 100 presets per file, 256k text-size bound; corrupted and incompatible formats are rejected.
- **Gallery sorting**: newest, by preset name or scene, in addition to existing filters and search.
- **Version header correctness**: derives from the Android BuildConfig, not a stale manually typed number.
- **NEBULA numerical stability**: star positions now use bounded trig-free hash. Rectangular artifacts visible in captured screenshots may also be caused by SurfaceView capture/composition or GPU driver differences; only actual device tests can confirm whether they disappear.

### Installing

`NOX 1.5 Portable` is a separate debug package `studio.nox.light.portable`. Previous NOX versions remain installed with their own data. To transfer scenes: open old NOX's Settings > Export if that version supports it, then import in 1.5. Existing versions without export must be upgraded with a **consistently signed build** to expose their data; sandboxed presets cannot be automatically read or copied by NOX 1.5. **Do not uninstall an old NOX to try 1.5, as that deletes its private presets.**

Build artifact: `NOX-1.5-portable-APK`. Requires on-device QA for Live Wallpapers, SurfaceView screenshot artifacts, scroll, file picker and performance. Debug APK is not an owner-signed stable release.

---

# NOX 1.4 — Atelier

A native, private, offline AMOLED live wallpaper studio for Android. Target tested phone family: Lenovo Legion Y70 2026 with Android 16 and seller-installed Motorola firmware.

## Atelier, release candidate

The four Android screens have a more cohesive, artwork-led layout:

- **Overview:** the animated scene fills available screen space, not a fixed-height card. Three lightweight interactive scene thumbnails switch FLUX / HALO / NEBULA. **New variant** runs the deterministic existing SceneLooks.shuffle on the actual preview, without silently overwriting the saved preset. Edit, PNG export and applying wallpaper use that displayed variant. Applying explicitly saves the chosen variant.
- **Studio:** the OpenGL preview stays pinned while controls scroll. The **Save / Export PNG / Apply wallpaper dock** is always visible at the bottom. Undo/redo, precise parameter steppers, color looks and harmonies remain functional.
- **Collection:** searchable two-column artwork gallery with FLUX/HALO/NEBULA filter, active-wallpaper mark, tap to edit and item menu for Edit / Use / Duplicate / Delete (delete requires confirmation). Thumbnails use simple Canvas graphics, avoiding extra GL contexts.
- **Settings:** compact, genuinely wired 30/60/120 FPS tiles and motion-sensor switch. Thermal/energy constraints remain enforced; no fake live telemetry.

All effects still use original GLSL renderer and WallpaperService. Existing Room schema, permissions, import/export behavior, and stored models are untouched.

### Safe installation

Debug APK label **NOX 1.4 Atelier**, separate Android package `studio.nox.light.atelier`. Because GitHub Actions produces a new debug signature, this preview installs **beside** previous NOX installations, never over them. Your presets in other versions are retained, but are **not migrated** between separate preview apps.

Do not uninstall NOX 1.3 until testing is complete.

### Build checks

CI checks Python structural contracts, Kotlin model unit tests and `:app:assembleDebug`; it uploads `NOX-1.4-atelier-APK`. Successful compilation is not proof of smooth operation or of battery performance on actual hardware; test these with the installed APK.

Built with Kotlin, Compose BOM 2025.10.01, OpenGL ES 3.0, AGP 8.13.2, Gradle 8.13, Android SDK 36. Offline and no root.

## Development history

Earlier releases and design plans remain in `docs/superpowers/`. Version 1.3 Signature includes bounded undo/redo and precision controls; 1.2 Palette Lab adds harmonic colors and pinned preview resizing; 1.1 added palette looks, search, full-screen preview and remix.

