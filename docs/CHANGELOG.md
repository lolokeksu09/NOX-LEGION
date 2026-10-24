# NOX — журнал изменений

## [1.9.3 / Vivid Light] — black-safe luminous contrast (2026-10-24)

**Branch:** `feature/nox-1-9-3-vivid-light`, based on NOX 1.9.2 Volumetric Light; pending review in a separate PR. No automatic merge into `main`.

- **Observed issue:** User reports the nine live wallpapers still look washed out/dim despite added depth. Code review shows small scene radiance coefficients and only `sum*=1.0+qualityMix*0.22` before hard `min(sum,vec3(1.0))` clipping. This cannot increase localized weak highlights enough and loses contrast in hotter peaks. This is a source-level hypothesis, not a measured device colorimetry finding.
- **Patch 1, red test:** `d260b8bf` adds numeric spectral-grade expectations (black input stays exactly black; a 0.12 neutral highlight rises above 0.22; a 0.8 highlight stays below clipping) and isolated 1.9.3 preview packaging contracts. Validation must fail against the unchanged 1.9.2 production code.
- **Targeted remedy:** a quality-gated, masked, bounded spectral tone curve on existing scene light, saturation recovery, no blanket black lift, no additional geometry loops or textures. Balanced and energy/thermal fallbacks remain unchanged, as do all nine IDs and existing `.nox` presets.
- **Verified RED evidence:** [CI #40086128072](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40086128072) failed only the two intended new Vivid contrast and 1.9.3 packaging tests; 33 existing structural contracts remained green.
- **Patch 2 — shader:** `590f7521` adds black-safe Vivid spectral grading after existing quality-gated radiance. The bounded rational curve `(sum*2.6)/(1+sum*1.8)`, chroma lift and `smoothstep(0.022,0.11,...)` mask visibly target midtones without a blanket gray overlay. Balanced stays unchanged, no new scene or data schema.
- **Patch 3 — standalone packaging:** Android 1.9.3/code16, separate preview `studio.nox.light.vivid`, launcher `NOX 1.9.3 Vivid`, CI artifact `NOX-1.9.3-vivid-APK`. README and QA ledger updated.
- **CI/result:** [Run #40086316092](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40086316092) passed all structural, GLSL, Kotlin and Android build steps plus artifact upload for commit `bbca1f4a`. Verified artifact ID `12557521130`, ZIP CRC and APK CRC; APK SHA-256 `c4d3156b7c061897ab022a1bff5178e1da9e8f911ad1cbc0f730b563b3b15a2b` (19,494,687 bytes). **Device visual improvement, power/FPS and thermals still require Lenovo Legion Y70 testing.**

## [1.9.2 / Volumetric Light] — deeper shaped materials (2026-10-24)

**Base:** 1.9.1 Cinematic Depth `3e5f592`; separate branch `feature/nox-1-9-2-volumetric-light`, not merged to `main`.

- **Patch 1 — RED contract:** `4ba8b0a` added a regression for nine independently sculpted materials, Rich/Ultra-only application and suppression of low-level haze. The base 1.9.1 shader does not yet implement this; verification and implementation follow.
- **Intended effect:** volumetric folds, toroidal profiles, dense gas lobes, depth-weighted curtains and comet wakes, energy-core shading, lit geometry and translucent crystal faces. Preserve existing shapes and nine mode IDs, `.nox` preset compatibility, pure-black AMOLED regions, pinned Studio preview and energy/thermal quality fallback.
- **Patch 2 — source:** `677bcd13` adds nine distinct low-cost geometric lighting fields to the existing quality-gated cinematic branch: FLUX lit sheet; HALO tube; NEBULA scattering; AURORA fold; PULSE spherical core; COMET plasma wake; VEIL translucent fabric; GRID illuminated pillars; SHARDS angled crystal faces. A per-fragment low-energy gate suppresses diffuse haze. This preserves the prior nine scene IDs, shader inputs, Room and exports.
- **Verification:** original missing-layer regression failed as expected in [CI #40083148531](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40083148531). The new shader requires the full GLSL/JUnit/Android CI gate before considering this implementation buildable; phone appearance/FPS/thermal testing remains outstanding.
- **Patch 3 — independent preview packaging:** structural contract `571528e9`; Android version `1.9.2` / code `15` in `7407ccd3`, independent debug suffix `.volumetric` and label `NOX 1.9.2 Volumetric` in `1ca40031`, workflow artifact `NOX-1.9.2-volumetric-APK` in `70c2f568`. Existing 1.9.1 installation/data unchanged.
- **Docs:** README and QA ledger updated with scene details and explicit lack of device testing.
- **Automated verification:** [CI #40083615236](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40083615236) on branch head `9218b433` passed Python regressions, GLES100 glslangValidator, Kotlin JUnit, Android build and artifact upload. GitHub [CI #40083507707](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40083507707) provided checked distributable `NOX-1.9.2-volumetric-APK` (artifact `12555887751`). Validated APK SHA-256 `ed281947ffd58af4e30570051141a366575c67c4e861358eb25d668a11e0a738`; full ZIP/APK CRC verified and archive contains manifest/DEX/resources. See [QA_STATUS](QA_STATUS.md).
- **Status:** Android source/build and ZIP integrity verified; target Lenovo Legion Y70 GPU visuals, FPS, thermal and battery usage **not yet measured**.

## [1.9.1 / Cinematic Depth] — denser real-time composition (2026-10-24)

**Branch:** `feature/nox-1-9-1-cinematic-depth` from 1.9 Graphics Overdrive. Unmerged; existing Android installs remain untouched.

- **Patch 1 — design/tests:** added [design](superpowers/specs/2026-10-24-nox-1-9-1-cinematic-depth-design.md), [implementation plan](superpowers/plans/2026-10-24-nox-1-9-1-cinematic-depth.md), and RED structural test for all nine compositions, continuous gas noise, and separate Ultra details. Code contract SHA `ea437b33`.
- **Patch 2 — GPU content:** added two smooth interpolated cloud-noise functions and a **distinct cinematic foreground/background pass to each of the existing 9 scenes** — sculpted FLUX ribbons, HALO arcs, NEBULA continuous clouds, AURORA curtains, PULSE reactor, layered COMET trails, VEIL volumetric folds, GRID skyline, SHARDS large facets. Ultra creates tiny highlights *only* over illuminated regions. No network, extra EGL contexts, database migration or new scene IDs. GPU implementation SHA `41d0e4c8`.
- **Render semantics:** Balanced retains original 1.9 drawing; Rich adds noticeable scene geometry; Ultra adds intensity and microdetail. Existing `SceneConfig`, .nox export, FPS limits, thermal downgrade and pinned preview remain unchanged.
- **Patch 3 — preview package:** `1.9.1` / code `14`; debug package `studio.nox.light.cinematic`, launcher `NOX 1.9.1 Cinematic`, artifact `NOX-1.9.1-cinematic-APK`. Commits: `e070bad5`, `630023bf`, `3267f88c`, `9a0bfc42`; README in `94faa8e2`.
- **Verification:** [GitHub Actions #40080423955](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40080423955) at `9a0bfc4242c39dcdc0e9b7dd5d65ae76f674a6f2` passed structural tests, glslangValidator, Kotlin JUnit, Android debug build and APK upload. Artifact ID `12555061380` (`NOX-1.9.1-cinematic-APK`), extracted APK 19,494,691 bytes, SHA-256 `398724e17bfeab8e18c766fa7eff2246e4f9de468f4ad3f88ffa5de0aad81c93`. User ZIP also checked for integrity, SHA-256 `8aa462268734a6a7d1a97d1a81ccfbb7ab6eca3a66b2fe916fe587592609a5bf`. Full details in [QA_STATUS](QA_STATUS.md). Actual Lenovo Legion Y70 visual quality, frame rate and thermals remain **unverified**.


## [1.9.0 / Graphics Overdrive] — implementation and release QA (2026-10-24)

**Status:** implementation published in feature branch; CI evidence to be recorded below. Lenovo hardware QA still required.

### Patch 1: Visual-quality domain and persistence
- Added test-first `VisualQuality` 0=Balanced / 1=Rich / 2=Ultra, verified bounds and fallback under Android battery/thermal pressure.
- Added DataStore `visual_quality` (default Rich) and ViewModel setter. No Room schema or `.nox` archive changes.
- Test commits: `8a97011`, `02e20d6`; implementation: `10e8340`, `438669e`, `4622c56`.

### Patch 2: Shared renderer and live wallpaper quality
- Quality travels through `EglSession`, `NoxSurfaceView` and system WallpaperService to `GlBackend`. The session automatically caps effective quality to Balanced under power saving/thermal stress, without changing user preference.
- Added `uQuality` uniform and per-mode **local hero lighting / depth layers for all nine scenes**: FLUX, HALO, NEBULA, AURORA, PULSE, COMET, VEIL, GRID, SHARDS. Pixel-zero regions remain black. Balanced retains original base lighting, Rich is default.
- Implementation commits: `98dc26a`, `cf29390`, `a475176`, `92f5560`.
- IMPORTANT: Independent glslangValidator pass is needed; GLSL syntax correctness is distinct from actual GPU visual quality.

### Patch 5: Release verification
- [CI #40078358750](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40078358750): 32 structural tests, glslangValidator vertex/fragment shader compilation, Kotlin tests, Android build and APK upload all **SUCCESS** at code SHA `4b04e44c073f874a7073affe69c5a317ac8f801f`.
- [CI #40078376689](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40078376689): post-fix docs SHA `8d61f1ef7bfb316de9673c5b2587f950283518b8`, all five stages **SUCCESS**.
- Artifact `NOX-1.9-overdrive-APK` (ID `12554345934`) — extracted APK 19,494,687 bytes, SHA-256 `f5c58568636059dac2c1a6d797b145e4ce50a808d7287ab799089080769b2aa1`. Both ZIP and APK CRC verified; AndroidManifest.xml, classes.dex, resources.arsc present.
- `docs/QA_STATUS.md` distinguishes confirmed CI from unverified GPU visual quality and thermal behaviour on Lenovo Legion Y70. No main merge.

### Patch 4: Preview regression fixes
- Updated pinned Studio preview structural assertion for the extra quality argument (`8d193271`). Fresh structural and GLES validation passed in GitHub CI run #40078110208; Android build revealed a missing quality parameter in FullscreenPreview.
- Fixed immersive FullscreenPreview quality propagation (`f7c3bb6e`), added regression (`4b04e44c`). Android build must be rerun; successful release status **not yet claimed**.

### Patch 3: Premium UI and packaging
- Real three-tile settings `Balanced / Rich / Ultra` with descriptions, immediate preview updates, and thermal caveat; fixed pinned Studio preview preserved.
- Android version 1.9.0/code13, separate debug application ID `studio.nox.light.overdrive`, launcher `NOX 1.9 Overdrive`; CI artifact `NOX-1.9-overdrive-APK`.
- Commits: `2f45a20`, `811d6c1`, `5d047cb`, `2cb9963`, `d5543b1`, `16042c4`.
- No permission, account, cloud, network, UI navigation or data-schema changes. Old installations remain intact.


## [1.8.0 / Patch 6] — Fix GLSL reserved identifier caught by compiler (2026-10-24)

- Independent `glslangValidator` check detected previously undetected GLSL ES 100 error: `active` is a reserved word.
- Renamed `active` variables to `visibilityMask` and updated all references in COMET and SHARDS. No changes to visual formulas.
- Updated structural regressions to reject the reserved identifier, preserving COMET/SHARDS culling.
- Source/test commits: `99755c6`, `b8d039a`, `433d68a`. Confirmed [GitHub Actions #40070162219](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40070162219) successful for shader validation, JUnit, Android build and artifact, with source SHA `433d68a4`. Verified APK SHA-256 `4a7d87a617dbed603cdabfe163b0b0fd034988257bd300aeb76aefb91e31df64`.
- Hardware/runtime behavior remains unverified.

## [1.8.0 / Patch 5] — Build-time GLES shader validation (2026-10-24)

- Added `tests/verify_glsl.py` which extracts the real VERTEX/FRAGMENT shader strings from GlBackend.kt, prepends `#version 100`, and compiles both with `glslangValidator`.
- GitHub Actions installs `glslang-tools` and runs GPU shader syntax/semantic checks *before* model unit tests and APK packaging.
- This catches GLSL compile problems beyond Android/Kotlin build validation. It does **not** verify phone-driver compatibility or thermal performance.
- Commit series: `b3a7055` (validator), `2c24cec` (CI). Results require a successful exact-head GitHub run.

## [1.8.0 / Patch 4] — Side-by-side Android preview and CI packaging (2026-10-24)

- Updated versionName `1.8.0`, versionCode `12`, independent debug package `studio.nox.light.spectra`, launcher `NOX 1.8 Spectra`.
- Workflow artifact renamed `NOX-1.8-spectra-APK`; updated structural version contracts.
- README updated with nine scenes, Surprise, .nox limitations and Android build/runtime QA distinction.
- Commits: `1492ce3`, `0cc18b8`, `8f80382`, `252b6d6`, `b782267`.
- **Verification:** confirmed successful CI #40070162219, artifact ID 12550619243, `NOX-1.8-spectra-APK`; ZIP/APK integrity verified. Existing 1.7.1 working app untouched.

## [1.8.0 / Patch 3] — Nine-scene gallery and Surprise discovery (2026-10-24)

- Home: added natural-language descriptions for VEIL, GRID, SHARDS while preserving immersive animated hero and nine-scene horizontal selector.
- Gallery: added GPU-free Canvas miniatures for VEIL, GRID, SHARDS and retained existing filters, favorites, saved looks, pinned preview and background wallpaper.
- Added **Сюрприз**: one-tap jump to a different scene and a curated look (or deterministic remix for older modes), only changing the unsaved Home draft. Existing presets remain untouched.
- Added SceneSurprise deterministic JUnit tests, including Int.MIN_VALUE and user-facing UI contract.
- Source commits: `a6d73b0`, `d1866ea`, `653bc42`; tests `8414dc1`, `6b281ef`.
- **Verification:** CI for this code was still running when written. No device shader/GPU QA yet.

## [1.8.0 / Patch 2] — Three new procedural GPU wallpapers (2026-10-24)

- Added new shared-GLES shader modes **VEIL=6**, **GRID=7**, **SHARDS=8**, using the existing time, colors, glow, density, motion, seed, and touch uniforms. Bounded loops; no extra EGL contexts.
- VEIL: layered drifting mist; GRID: bounded perspective grid; SHARDS: rotating luminous diamonds with facets and softened edges.
- Preserved original FLUX/HALO/NEBULA/AURORA/PULSE/COMET shader computations, mode IDs and global tone handling. COMET now uses explicit `uMode<5.5` dispatch to allow additional branches.
- Structural shader regression tests written in `tests/verify_structure.py` before implementation.
- Commit series: `2eaa26a` (red tests), `4a13bcd` (GPU modes). **Hardware shader compilation/thermals remain untested** until installation on Lenovo Legion Y70.

## [1.8.0 / Patch 1] — Nine-scene catalog and nine new looks (2026-10-24)

- Added VEIL / GRID / SHARDS to the immutable SceneRegistry, preserving the original six scene IDs and defaults.
- Added scene-specific 1.8 defaults and 3 named colorways for each (9 new variants, all using existing SceneConfig fields); no Room migration or network.
- Updated the `NEW` flags to identify new 1.8 entries, rather than labeling prior release scenes as new.
- Added `SpectraCatalogTest`; revised `SceneRegistryTest` for nine scenes and archive compatibility.
- Source/test commit sequence: `16fc515`, `eda1551`, `830798b`, `9ec9818`, `123c5ba`, `6d8b7ce`.
- **Verification:** tests/CI pending at time of this entry; GPU renderer still requires mode implementation.


> Каждый следующий патч обязан обновлять этот файл и публиковаться коммитом в GitHub. См. [AGENTS.md](../AGENTS.md) и [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md).

## [1.7.1 / Patch 2] — Preview packaging and regression hardening (2026-10-23)

- Updated versionName to `1.7.1`, versionCode to `11`, independent debug application ID `studio.nox.light.lumina`, launcher `NOX 1.7.1 Light Dynamics`; CI uploads `NOX-1.7.1-light-dynamics-APK`.
- Updated structural regression tests for package, version and newly described COMET branch. Build commits: `081f2678`, `f86c0923`, `8ffcdc35`, `59ec7a87`; regression correction: `52d29d96`.
- Source shader changes from Patch 1 persist; FLUX/HALO/NEBULA remain unchanged. README now documents user-visible improvements and preserving previous APKs.
- **Verification:** [GitHub Actions #40008408749](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40008408749) passed structural tests, Kotlin unit tests, Android assembleDebug and artifact upload for commit `574a4d32d907f1567bc8ccefc77b8c3047d48ca8`. Artifact ID `12525875176`, `NOX-1.7.1-light-dynamics-APK`. Extracted APK `19478319` bytes, SHA-256 `ff3ece7f5326f14656f84f16753bba00f768dcd7afea4fc6d8725ecb6e90147f`; ZIP/APK integrity and AndroidManifest/classes.dex/resources.arsc verified. GLES/runtime/thermals on Lenovo remain untested.

## [1.7.1 / Patch 1] — Light Dynamics shader refinement (2026-10-23)

- **AURORA:** five multi-scale drifting light curtains with broad color veils, luminous cores and delicate filaments, preserving pure-black regions.
- **PULSE:** six distinct outward-traveling waves; life-cycle edge fade avoids hard ring restarts, and active touch shifts the wave center.
- **COMET:** seven deterministic particles with depth-based brightness, per-comet direction/speed and longer variable tails; density controls visibility, phase fade prevents hard reset.
- Preserve existing FLUX, HALO, NEBULA shader branches, six scene IDs, Room schema and pinned Studio preview.
- Code commit: `d93c8b8225e99792014b971de4e20f5a916f81ba`. Regression contract: `4e38fb65732875b8f79c67870730021826c37988` (expected red before implementation).
- CI/build results: **not yet confirmed** for updated shader at this documentation commit. New shaders still require on-device GLSL, performance and visual validation.

## [1.7.0 / Patch 5] — Release candidate APK (2026-10-23)

- Version `1.7.0` (`versionCode=10`), separate debug app ID `studio.nox.light.livingscenes`, launcher label `NOX 1.7 Living Scenes`.
- CI output: [GitHub Actions run #40004992732](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40004992732), **success** for structural tests, Kotlin unit tests, `:app:assembleDebug`, and upload-artifact, on code commit `0446ab267d63d664d7091f6007d121acadcb498d`.
- Artifact: `NOX-1.7-living-scenes-APK`, GitHub artifact ID `12524570195`. Extracted `app-debug.apk`: 19,478,319 bytes (19.5 MB decimal); SHA-256 `bd69e94cbe6185baf5bfe2fcb31474a40b2866f767f2e382a56c4c645cfe5072`. ZIP integrity checked, `AndroidManifest.xml`, DEX and resources present.
- Code fixes during CI: corrected SceneLook/SceneLooks/NamedScenePreset Kotlin imports; added import regression test, prevented Home control overflow; avoided shadowing built-in `cross` in COMET shader.
- **Limits:** Kotlin/Android compilation does not execute GLES shader compilation on the target device. Actual visuals, stable performance, thermal behavior, touchscreen gestures, lock/unlock, live wallpaper apply and PNG export for new scenes remain **NOT TESTED ON DEVICE**.
- Docs: `README.md`, `docs/CHANGELOG.md`, `docs/QA_STATUS.md`, spec, plan. No automatic merge to `main`; preview install is isolated from earlier NOX packages.

## [1.7.0 / Patch 4] — Gallery, category filters and Home variants (2026-10-23)

- Home now has an accessible, horizontally scrollable selection of six scenes with `NEW` labels for AURORA, PULSE and COMET, plus a modal listing three curated variants for the selected new scene. Selecting a variant edits only an unsaved draft.
- Added lightweight Canvas representations of AURORA, PULSE and COMET to avoid six GL rendering surfaces.
- The gallery retains scene, name, favorites, sort and two-column art grid while adding Signature/Ambient/Tech/Cosmic category filtering.
- Added structural regression tests for all three new thumbnails, category filtering, current-configuration export and pinned live preview.
- Commit series: `722ccb8` tests, `00ba447` Home, `f22a0ce` Canvas/gallery; baseline renderer unchanged since Patch 2.
- **Verification:** passed structural tests, Kotlin tests and Android compilation in CI run #40004992732; hardware appearance and touch handling remain unverified.

## [1.7.0 / Patch 3] — Nine scene variants (2026-10-23)

- Created `ScenePresets.kt` with 3 named variations each for AURORA (Polar, Emerald, Violet Mist), PULSE (Cyan Beat, Crimson Core, Neon Echo), COMET (Deep Space, White Tail, Blue Drift).
- Reuses the existing `SceneConfig` fields with deterministic seed values; no Room migration. Added JUnit tests for names, exact colors, ranges and `.nox` round-trip.
- Commit series: `0b270e1` (red contracts), `3c047d3` (implementation).
- **Verification:** passed in CI run #40004992732; no on-device visual QA yet.

## [1.7.0 / Patch 2] — Procedural renderer (2026-10-23)

- Added GPU modes 3=AURORA, 4=PULSE, 5=COMET in the shared `GlBackend.kt`, maintaining existing modes 0=FLUX, 1=HALO, 2=NEBULA.
- New scenes use bounded iteration and existing colors, glow, motion and seed; old three shader branches retain their math.
- Added structural tests checking distinct mode dispatch and legacy NEBULA branch.
- Commit series: `34ab1c1` (red contracts), `6eb6297` (shader implementation).
- **Verification:** Kotlin/Android CI passed in run #40004992732; GLES shader compilation on Lenovo and actual appearance still need device checks.

## [1.7.0 / Patch 1] — Catalog (2026-10-23)

- Added `SceneRegistry` / `SceneCategory` and registered AURORA, PULSE, COMET without changing the defaults of FLUX, HALO or NEBULA.
- Added `SceneRegistryTest` covering ordered IDs, categories, defaults and normalization.
- Changes: `SceneRegistry.kt`, `SceneConfig.kt`, `SceneRegistryTest.kt`; commit series `91ef2e0`, `8b663b9`, `42e3871`.
- **Verification:** catalogue/legacy model tests and full CI passed (run #40004992732); no device tests.

## [1.7.0] — IMPLEMENTED PREVIEW / аппаратная QA ожидается (2026-10-23)

- **Запланировано:** AURORA, PULSE, COMET; всего 6 сцен.
- **Запланировано:** 9 стартовых вариаций, категории сцен, горизонтальный переключатель с миниатюрами и отметками NEW.
- **Архитектура:** текущий SceneConfig без изменения Room; общий OpenGL рендерер с режимами 0–5; обратная совместимость FLUX/HALO/NEBULA.
- **Документы:** [спецификация](superpowers/specs/2026-10-23-nox-1-7-living-scenes-design.md) и [план из 5 патчей](superpowers/plans/2026-10-23-nox-1-7-living-scenes.md).
- **Ветка:** `feature/nox-1-7-living-scenes` от `feature/nox-1-6-curated`.
- **Текущий статус:** исходники реализованы, Android debug APK собран и проверен в CI; аппаратная проверка GLES и автономности ещё не выполнялась.

## [1.6.0] — исходная основа новой ветки (2026-10-23)

- **Содержится в базе:** три сцены FLUX, HALO, NEBULA; избранное в коллекции и быстрые цветовые стили на главном экране.
- **Ветка:** `feature/nox-1-6-curated`; существующий PR #7 остаётся отдельным.
- **Статус:** известна успешная предыдущая CI-сборка; подтверждение работы 1.6 непосредственно на телефоне в эту запись не вносилось.

## Правила ведения

Для каждого реализованного этапа 1.7 добавлять новую запись с конкретной датой, SHA коммита, изменёнными файлами/функциями, результатами тестов, CI, ограничениями и статусом аппаратной проверки. Не подменять «запланировано» утверждением «сделано».
