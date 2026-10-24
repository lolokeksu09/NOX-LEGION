# NOX 1.9 Graphics Overdrive — verified CI / hardware QA pending (2026-10-24)

## Confirmed, not inferred
- Live nine-mode GLES renderer has persisted `uQuality` 0/1/2 (Balanced/Rich/Ultra), additional localized glowing accents per each original mode, and keeps pure-black background in empty pixels. `GlBackend.draw`, `EglSession`, `NoxSurfaceView`, and `NoxWallpaperService` accept the saved quality setting; Android thermal/power saver downgrades rendering quality without overwriting preferences.
- [Run #40078358750](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40078358750), code and tests SHA `4b04e44c073f874a7073affe69c5a317ac8f801f`: **all success** — 32 structural tests, glslangValidator GLSL ES 100, Kotlin/JUnit, Android `:app:assembleDebug` and artifact upload.
- [Run #40078376689](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40078376689), documentation SHA `8d61f1ef7bfb316de9673c5b2587f950283518b8`: the same five steps also reported success.
- Artifact from run 40078358750: ID `12554345934`, name `NOX-1.9-overdrive-APK`. Extracted APK is **19,494,687 bytes**, SHA-256 `f5c58568636059dac2c1a6d797b145e4ce50a808d7287ab799089080769b2aa1`. Container CRC checks passed for artifact ZIP and APK, manifest, DEX and resources present.
- Earlier red tests caught missing quality features, previous CI found an omitted fullscreen-preview quality parameter. Fixed with regression tests. Changelog captures exact commits and evidence.

## Hardware verification still required
- Lenovo Legion Y70 2026: **true GPU frame rate, driver GLSL shader operation**, effect brightness, actual Rich/Ultra difference, power/thermal fallback, flicker, touch/gyro, screen lock/unlock, wallpaper installation, export PNG and system wake/sleep.
- At 120 FPS, measure device temperature, power consumption and any thermal throttling. Quality is a choice, not a measured FPS promise.
- PNG export uses Rich by default; quality setting currently applies to live preview and system live wallpaper.
- NOX 1.9 uses separate debug package `studio.nox.light.overdrive`, version 1.9.0/code13. It does not delete existing 1.8 or migrate its private presets. Use in-app .nox export/import where available and keep the old installation.

## Sources
[Design](superpowers/specs/2026-10-24-nox-1-9-graphics-overdrive-design.md) · [Plan](superpowers/plans/2026-10-24-nox-1-9-graphics-overdrive.md) · [Changelog](CHANGELOG.md) · [Contributor rules](../AGENTS.md)

---

# NOX 1.8 Spectra — CI verification and device QA (2026-10-24)

## Verified in GitHub Actions

- New shared GLES shader modes 6=VEIL, 7=GRID, 8=SHARDS. Existing modes 0–5 unchanged except the COMET branch now has an explicit uMode bound and its invalid GLSL reserved identifier `active` was renamed.
- Run [#40070162219](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40070162219) at source SHA `433d68a4be18eefdd2ffc1550da2e23f1b1070a3`: structural tests SUCCESS, independent `glslangValidator` GLES 100 vertex/fragment compile SUCCESS, Kotlin model JUnit SUCCESS, Android `:app:assembleDebug` SUCCESS, APK upload SUCCESS.
- Artifact ID `12550619243`: `NOX-1.8-spectra-APK`. Extracted `app-debug.apk`: 19,494,691 bytes; SHA-256 `4a7d87a617dbed603cdabfe163b0b0fd034988257bd300aeb76aefb91e31df64`. Container ZIP CRC verification passed, `AndroidManifest.xml`, `classes.dex`, and `resources.arsc` present.
- Run [#40070180123](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40070180123) at docs-only SHA `b4929389ca796c68afacdf65327cd51084dca978` also reported SUCCESS for the same five workflow steps when this QA note was prepared.

## Not yet verified on Lenovo Legion Y70

- Actual GPU-driver shader initialization, sustained FPS, thermal throttling and battery impact for VEIL/GRID/SHARDS (desktop GLES validator is not hardware QA).
- Scene selection gestures, Surprise previews, pinned Studio preview, Android wallpaper service lifecycle, PNG export and file import of new IDs.
- No visual artifacts or excessive flicker. The GLSL source was tested, but live appearance requires real device observation.

## Safe installation

- NOX 1.8.0/code12 debug package `studio.nox.light.spectra` runs side-by-side with 1.7.1. Previous preset databases are **not** automatically transferred; keep old installs.
- The format of .nox archives is unchanged; old apps may explicitly reject new scene IDs. Import using NOX 1.8.
- No Room migration, Internet, account, root or additional Android permissions.

---

# NOX 1.7.1 — Light Dynamics QA, 2026-10-23

## Verified in GitHub Actions

- Renderer changes to AURORA/PULSE/COMET committed in `d93c8b8225e99792014b971de4e20f5a916f81ba`. No changes to legacy FLUX/HALO/NEBULA shader branches; their code was compared directly and found byte-identical.
- Run [#40008408749](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40008408749) — `success` at SHA `574a4d32d907f1567bc8ccefc77b8c3047d48ca8`.
- Structural tests: pass; Kotlin model unit tests: pass; `:app:assembleDebug`: pass; APK upload: pass.
- Artifact `NOX-1.7.1-light-dynamics-APK` ID `12525875176`. APK 19,478,319 bytes, SHA-256 `ff3ece7f5326f14656f84f16753bba00f768dcd7afea4fc6d8725ecb6e90147f`.
- Tested ZIP/embedded APK CRC integrity; AndroidManifest.xml, classes.dex and resources.arsc present.

## Hardware tests still required on Lenovo Legion Y70

- Actual GPU shader compilation and appearance of AURORA filaments, PULSE outward wave fade and touch-centered waves, COMET tail lifecycles; Gradle compilation does not execute GLES shader.
- 60/120 FPS under normal and power-save conditions, screen lock/unlock and orientation transitions, battery/thermals, PNG export/live wallpaper service and preset continuity.
- The previous NOX versions install side by side under different package names, so their private databases are not migrated. **Do not uninstall the working 1.7 build.**

---

# NOX 1.7 Living Scenes — release-candidate verification, 2026-10-23

## Verified
- Code commit: `0446ab267d63d664d7091f6007d121acadcb498d`.
- [GitHub Actions run #40004992732](https://github.com/lolokeksu09/NOX-LEGION/actions/runs/40004992732): structural contracts **success**; `:core:model:test` **success**; `:app:assembleDebug` **success**; APK artifact upload **success**.
- GitHub artifact ID `12524570195`, name `NOX-1.7-living-scenes-APK`. Extracted APK size `19478319` bytes, SHA-256 `bd69e94cbe6185baf5bfe2fcb31474a40b2866f767f2e382a56c4c645cfe5072`. APK ZIP structure/integrity verified (manifest, DEX, Android resources).
- Scene registry tests cover six unique IDs, preserved FLUX/HALO/NEBULA defaults, new categories, normalization.
- Preset tests cover nine curated appearances, exact palettes and versioned `.nox` round-trip.
- UI structural checks cover pinned Studio preview, gallery filters, six-scene Home carousel, new Canvas thumbnails, draft handling, no new permissions, GL dispatch and correct Kotlin imports.

## Not verified on Lenovo Legion Y70 2026
- GLSL fragment shader compilation at runtime for AURORA, PULSE, COMET (Gradle does not compile GLSL on the phone).
- 10–15-minute sustained performance per new scene, actual FPS, battery/thermal observations; 60 vs 120 FPS under real system limits.
- Tap, tilt/parallax, orientation, lock/unlock, wallpaper service lifecycle, PWM-related visual appearance, PNG export and application to system wallpaper.
- Actual visual quality and absence of rectangular compositing artifacts in NEBULA or newly added scenes.

## Required device QA
1. Install NOX 1.7 beside older versions; do not uninstall older private data.
2. Open each of six scenes, use Home horizontal gallery, choose all nine named variants and inspect smoothness.
3. Enter Studio with each new scene: confirm live preview is pinned while scrolling; Undo/Redo, Save/PNG/Apply use displayed values.
4. Save, duplicate, favorite, filter by category, export/import `.nox`; force-stop and relaunch to confirm persistence.
5. Apply as live wallpaper; test home/lock screen, hidden/visible engine and thermal or energy saving.
6. Screenshot and visually inspect normal vs exported image; capture reproducible artifacts and logcat messages if any.

**Conclusions:** CI-built installable preview, **not hardware-certified**. Do not claim new shaders or 120 FPS validated on device until the user's measurements are available.

---

# NOX 1.0 — verification ledger, 2026-10-23

## Verified in this execution environment
- Kotlin 1.9 command-line compiler executed `tests/ModelContract.kt` against the pure model source: 12/12 assertions passed, including explicit RED (unresolved implementation) before GREEN.
- `python -m unittest discover -s tests -p verify_structure.py -v`: 6/6 structural assertions passed (XML well-formed, no requested permissions, modules declared, wallpaper declarations, export entrypoint, CI APK artifact path).
- Android source and Gradle module files written to disk and packaged separately.

## Blocked
- `:app:assembleDebug`, Android instrumentation, installation and GPU checks: Android SDK and Gradle are absent; DNS resolution for external SDK/Gradle endpoints fails in the container.
- Therefore NO APK binary is included or claimed to work. The configured GitHub Actions runner must execute the first actual Android build; failures must be fixed before distributing an APK.
- No validation has occurred on the Lenovo Legion Y70 2026 / Motorola Android 16 firmware.

## Device test checklist (not executed)
- Install debug APK; launch Home, Studio, Library, Settings.
- Verify three shader scenes visually and touch + gravity parallax.
- Save, reopen, duplicate, delete preset, then force-stop/relaunch.
- Export PNG to Downloads via picker and inspect pixels/orientation.
- Apply live wallpaper using OS confirmation; decline once, accept once.
- With wallpaper visible vs hidden, monitor GPU activity, FPS and sensors.
- Turn on battery saving and verify profile limitation.
- Screen lock/unlock, portrait/landscape, launcher and lockscreen behavior.
- 30-minute burn-in and thermal check.

## Threat / failure review
- No INTERNET or storage read permissions requested.
- User-controlled preset names trimmed/validated (1..48 chars).
- No automatic wallpaper changes; Android confirmation is required.
- Native GLES3 failure shows error in editor, but hardware compatibility still unverified.
- Debug signing is unsuitable for production distribution; release key must remain under owner control.
