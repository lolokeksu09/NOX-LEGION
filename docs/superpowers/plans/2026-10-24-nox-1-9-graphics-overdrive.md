# NOX 1.9 Graphics Overdrive — execution plan

**Spec:** `docs/superpowers/specs/2026-10-24-nox-1-9-graphics-overdrive-design.md`.
**Goal:** turn nine relatively sparse GLES backgrounds into premium layered AMOLED visuals, with an end-to-end saved quality switch.
**Base:** `feature/nox-1-8-spectra`.

## Task 1 — Quality model + DataStore
- [ ] Add RED structural tests for DataStore 0/1/2 quality, viewmodel setter, preview and wallpaper wiring, quality-aware shader.
- [ ] Create `core/model/src/main/kotlin/studio/nox/model/VisualQuality.kt` pure validation of three quality tiers and unit tests; test red before implementation.
- [ ] Implement non-migrating DataStore quality preference and ViewModel setter.
- [ ] Update docs/CHANGELOG.md, commit SHA.

## Task 2 — Renderer and service wiring
- [ ] Update EglSession with quality setter, thermal/power-safe downgrade and GlBackend draw argument.
- [ ] Update NoxSurfaceView configure quality and WallpaperService settings collection.
- [ ] Add uQuality uniform, implement quality-aware additional per-scene luminous detail for all nine modes.
- [ ] Preserve GLES100 compliance and avoid black-to-gray lift. Update CHANGELOG, commit.
- [ ] Confirm RED-to-GREEN tests, glslangValidator and Gradle CI; fix errors from actual logs.

## Task 3 — Studio/Home/Settings UX
- [ ] Quality tiles Balanced/Rich/Ultra with meaningful copy and current status; real onQuality callback.
- [ ] Wire quality to Home/Studio/LivePreview without changing pinned editor surface or scroll.
- [ ] Update README, QA and changelog. Commit.
- [ ] Version 1.9.0/13 and isolated debug `.overdrive`. Update artifact label. Commit with docs.

## Task 4 — Release verification
- [ ] Confirm CI structural, GLSL validation, unit tests, Android compile and APK upload on exact HEAD.
- [ ] Check artifact APK ZIP structure and SHA-256. Prepare PR for review. Record verified evidence in docs.
- [ ] Device QA still required to judge actual richness, fps, thermal, compositing and wallpaper persistence; do not claim it happened.
- [ ] Every code patch includes linked GitHub commit and documentation; no merge to main without explicit authorization.
