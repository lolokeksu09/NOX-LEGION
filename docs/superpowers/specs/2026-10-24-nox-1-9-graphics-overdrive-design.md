# NOX 1.9 Graphics Overdrive — approved design

**Date:** 2026-10-24. **Base:** `feature/nox-1-8-spectra`. **Status:** implementation in progress, not released.
**Intent:** make existing nine live wallpapers visually fuller, richer and more impactful, rather than adding more sparse scene modes.

## Design decisions
- Keep all nine IDs, SceneConfig structure, Room schema, .nox V1 interchange, fixed Studio preview, 4 bottom navigation tabs and true-black OLED presentation.
- Add **persistent scene quality** mode: Balanced (0, restrained), Rich (1, default), Ultra (2, maximum). This is a **global local DataStore preference**, not a SceneConfig field, so older archives/presets stay compatible.
- Quality must affect both render brightness *and actual visual structure*: distinct depth layer / bloom / detail contribution, not simply a whole-screen multiplier. Shader color must stay near zero for empty pixels, with controlled RGB clamp.
- Thread quality through preview -> NoxSurfaceView -> EglSession -> GlBackend; and wallpaper -> EglSession. PNG export renders Rich by default, independent of FPS setting (static frame).
- During system power saver or >= moderate thermal throttling, session automatically caps quality to Balanced; retain existing FrameBudget FPS limits. The user's stored setting is not overwritten.
- Avoid 9 GL contexts, screen-space convolution, noise across the entire OLED screen, new permissions/root/network.

## Art direction and shader strategy
- **FLUX:** broaden flowing forms with bright cores, soft halos, variation in thickness and faint flow sparks.
- **HALO:** central luminous ring plus satellite arcs and bounded radial corona.
- **NEBULA:** large overlapping gaseous clouds with clear blue-violet depth, stars and central glowing nebula; avoid chunky grid artefacts by using continuous smooth field.
- **AURORA:** wider scenic curtains, better layered ribbon bloom and fine vertical filaments.
- **PULSE:** central energy core and luminous inner halo, additional concentric shockwave detail (non-flashing).
- **COMET:** low-intensity background star haze + hero comet corona/trails.
- **VEIL:** multi-band mist with fine luminous veins and diffuse envelope.
- **GRID:** horizon layers, light nodes, lattice contrast and focal bloom.
- **SHARDS:** larger luminous facets + edge reflections and small orbiting debris.
Existing primary/secondary colors, glow/density/motion/speed control character; quality switches scale and detail.
No strobe events, screen-wide flashes or flickering brightness discontinuities.

## Implementation surface
- `core/data/.../SettingsRepository.kt`: DataStore quality key, validated 0..2, default 1. `NoxSettings` additional trailing field; caller defaults continue working.
- `app/.../NoxViewModel.kt`: `setQuality` setter.
- `app/.../NoxApp.kt`: 3 real quality tiles in Performance; pass quality to Home/Studio/LivePreview.
- `core/renderer/.../NoxSurfaceView.kt`: configure quality; session setter.
- `core/renderer/.../EglSession.kt`: quality transport, power/thermal cap, unchanged frame gating.
- `feature/wallpaper/.../NoxWallpaperService.kt`: observe saved quality and relay to live service.
- `core/renderer/.../GlBackend.kt`: `uQuality` uniform 0/1/2 and shader post-branch selective detail.
- `tests/verify_structure.py`: end-to-end wiring plus quality-aware shader detail contracts; Android CI GLSL validator and Kotlin/JUnit.
- Version `1.9.0`, code 13, separate `.overdrive` debug package. Preserve earlier installs.

## QA gates
1. RED structural tests verify persistent quality wiring and distinct contributions for all nine modes.
2. GLES100 `glslangValidator` compiles vertex/fragment (existing CI), JUnit model tests pass; Android assembleDebug passes.
3. Inspect exact-head GitHub workflow artifact APK, ZIP CRC, manifest and DEX; keep notes in docs/QA_STATUS.md and docs/CHANGELOG.md.
4. Do not claim visual quality, FPS/temperature or driver/runtime verified until Lenovo Legion Y70 hardware comparison at 60/120FPS and Balanced/Rich/Ultra.
5. After **each patch** commit to GitHub and update CHANGELOG/docs per AGENTS.md; PR targets 1.8 Spectra and never merges main unrequested.
