# CONTEXT CAPSULE — Android 1.0 design

**Status:** design specification for owner review; NOT implemented.
**Date:** 2026-10-24
**Repository:** `lolokeksu09/NOX-LEGION` (repository name retained pending a separate explicit rename)
**Target:** Lenovo Legion Y70 2026, Android 16, non-root
**Base NOX main commit:** `8a10093f9e587120d878ed359aa6dd645f0716be`
**NOX backup:** `archive/nox-main-2026-10-24` (old feature branches remain untouched)

## 1. Intent and acceptance criteria

Replace the old NOX live-wallpaper application with **CONTEXT CAPSULE**, a native, private, offline-first **project memory and handoff** app.

The owner develops Android and web applications across long ChatGPT conversations. Conversation context can become difficult to transfer between chats. CONTEXT CAPSULE must preserve approved decisions and source-backed project status in a portable format, so a new ChatGPT conversation can continue work without re-explaining it. The app must work with **no paid AI API**, no root, and no proprietary server.

Success for Android 1.0:
1. Add and manage at least two independent local projects, e.g. WrCommunity and NOX.
2. Read public GitHub repository metadata, main commit, selected recent commits, PR metadata, and available Actions run statuses on explicit sync; show retrieved timestamps and commit SHAs.
3. Manually paste or import owner-approved text containing project decisions and tasks; allow edit/confirm/reject.
4. Produce a deterministic, readable Markdown + JSON snapshot retaining source references and explicit uncertainty.
5. Export/share that snapshot through Android Sharesheet and offer copy-to-clipboard fallback so the user can paste it into ChatGPT.
6. Export/import the app's own project data to/from a user-selected file; prevent accidental destructive replacement.
7. Remain useful offline with the last saved snapshot. Do not assert GitHub data is current when offline.

## 2. Product scope

### In Android 1.0
- Project dashboard with clear last sync time, source repo, local decisions, open tasks, capsule version.
- GitHub public repository connector by `owner/name`. Explicit sync action, safe pagination/limits, sensible caching, error and rate-limit messaging. Do not request GitHub token for public sources.
- Local decisions: decision title, details, timestamp, source type, optional source URL, confirmation status. No AI inference or blind auto-approval.
- Tasks: title, status (open/done/blocked), optional link to source issue or PR.
- GitHub snapshot: repo identifier, selected branch, HEAD SHA, commit subjects with URLs, PR IDs/status, CI run conclusions, sync timestamp.
- Capsule builder: stable sections (**Goal**, **Approved decisions**, **Verified GitHub state**, **Open questions**, **Next actions**, **Sources / caveats**); use verbatim quoted text where needed to retain exact constraints. Cap length by user-selected export mode while retaining authoritative source links.
- Import and export: UTF-8 Markdown; project backup in schema-versioned JSON with validation and explicit merge/replace choice. No file writes outside Android's document picker.
- No-cost ChatGPT handoff: platform text-sharing intent to ChatGPT if supported; otherwise clipboard copy; user manually reviews and sends. No guarantee that ChatGPT will open a fresh conversation or auto-send the payload.
- Dark premium AMOLED UI, large project cards, text-size accessibility, predictable navigation, and offline status indicators.

### Explicitly excluded from Android 1.0
- OpenAI API, ChatGPT credentials, subscription reuse, autonomous access to ChatGPT private conversation history, automatic sending of prompts.
- Auto-parsing all ChatGPT history archives (future version, via user-imported official archive with clear consent).
- Connecting to private GitHub repositories (future design requires explicit opt-in authentication and secure credentials).
- Unattended write access to GitHub, deployment, modification of CI, or device administration.
- An on-device language model as a core dependency, cloud database, custom backend, continuous background service.

## 3. Alternatives and decision

A. **Chosen: replace NOX app in the same repository after explicit review**, preserving history and `archive/nox-main-2026-10-24` plus existing feature branches. One familiar repository, no NOX code coupling, clean Gradle project.
B. Preserve existing NOX modules and add Context Capsule as a feature: rejected because live wallpaper renderer, data model and CI create unnecessary coupling.
C. Create separate GitHub repository: safer separation, but does not match owner's explicit request to repurpose the NOX-LEGION repository.

The repository display name may still say NOX-LEGION. GitHub repository rename is a separate operation; do not invent or perform it implicitly.

## 4. Android architecture

- Kotlin, Jetpack Compose + Material 3, coroutines/Flow; compile/target Android SDK 36; minSdk at least 26, subject to build verification.
- Application ID: `dev.lolokeksu.contextcapsule` (distinct from installed NOX `studio.nox.light`); never silently replace NOX on device.
- Clean modules: `:app`, `:core:model`, `:core:data`, `:core:github`, `:feature:projects`, `:feature:capsule`. Module boundaries and naming are design targets, to confirm against dependency setup in implementation plan.
- Room for projects, decisions, tasks, source provenance and immutable sync snapshots. Use migrations for schema evolution.
- GitHub read-only API client, JSON parser, explicit timeouts, backoff/rate-limit handling. Repository identity and remote URLs validated and canonicalized.
- `CapsuleComposer` pure Kotlin deterministic reducer from selected Room records to Markdown and JSON. No generative text presented as confirmed facts.
- Android Storage Access Framework for export/import; standard Android Sharesheet for transfer.
- State/UI: repository/store interfaces consumed by ViewModels; distinct loading/success/partial/stale/error states.

## 5. Data credibility model

Every assertion in a capsule is tagged as one of:
- `verified_remote`: retrieved from GitHub with repo, URL, SHA (when applicable), and `checked_at`.
- `owner_confirmed`: entered or approved by owner, source and confirmation time if known.
- `unverified`: imported text lacking independent verification.
- `unknown`: missing or uncheckable data; never fill with invented facts.

Conflicts must be surfaced for owner review. Old GitHub snapshots remain historical and are never labeled 'current' without successful sync. Source quotes must not be silently rewritten; tasks cannot be marked complete solely because ChatGPT claimed completion. Imported text is user data, never executable instructions for the app.

## 6. Privacy and safety

- App-private files and Android sandbox; no analytics or advertising SDK; only INTERNET permission for GitHub access in first version.
- Store no GitHub API tokens or OpenAI keys in 1.0. Do not request accessibility or screen capture permissions.
- Disable automated app-data backup until a privacy-reviewed backup/export scheme exists. Locally exported unencrypted backups must display an explicit privacy warning; require user-chosen destination.
- Never upload the user's chat text, local notes, or capsule to GitHub; the read-only connector never receives them.
- Explicit confirm screen before backup import replacement, project deletion and export sharing.
- New app has a different application ID: old NOX remains installed, and its presets/data remain untouched. The new repository code does not preserve or migrate NOX-specific runtime data.

## 7. Primary screens and flows

1. **Projects**: list with prominent cards, last sync badge, pinned next task, Add project.
2. **Project detail**: Overview / Decisions / Tasks / Sources tabs; sync controls and source provenance.
3. **Import**: paste text or choose file; preview and classify; owner confirms inclusion.
4. **Capsule review**: source-backed sections, status caveats, length mode (full/compact), review before export.
5. **Share/export**: Copy; Share as text; save Markdown / JSON; save project backup.
6. **Settings**: AMOLED appearance, privacy, backups, app info.

Visual direction: calm editorial typography, true-black surfaces, cobalt-blue accent, crisp hierarchy, large tactile cards; prioritize readability and Android native interactions over decorative animations.

## 8. Error handling

- Network unavailable: preserve last snapshot with **stale** status; local capsule creation still works.
- GitHub 403/429: show rate-limit state and retry guidance; no aggressive polling.
- Invalid/missing repositories: no phantom project status; allow local-only project.
- Partial GitHub response: label incomplete and do not promote to verified-complete snapshot.
- Invalid JSON backup: reject safely with actionable validation error; preserve existing data.
- Share target missing: fall back to clipboard or document picker, never claim message was sent to ChatGPT.

## 9. Validation and delivery criteria

- Unit tests: deterministic capsule output, provenance labels, stale-state handling, import validation, schema migration.
- Repository tests: mocked GitHub responses, rate-limit/error branches, no writes.
- UI smoke: create project, add decision, sync mocked repo, preview capsule, export/import.
- GitHub Actions builds a **CONTEXT CAPSULE** debug APK and runs unit tests; artifacts renamed accordingly.
- Build and CI evidence required before claiming APK exists; device-side tests for Android 16 sharing and SAF are separate.
- No deleting `main` content before owner has reviewed this spec and an implementation plan.

## 10. Migration plan boundary

Before migration:
1. Backup NOX `main` with immutable source SHA in a named archive branch (**done**).
2. Keep legacy feature branches and historical commits (**done**).
3. Review this design and a written implementation plan (**pending**).

After review and approval:
4. Work on a dedicated feature branch. Remove NOX-specific renderer, wallpaper feature, old assets and tests from new app tree; replace old AGENTS, README, CI and other NOX-facing docs with CONTEXT CAPSULE equivalents. Do not rewrite published Git history.
5. Verify with CI and APK artifact; submit PR. Integrate `main` only through approved review.
6. Keep old Android package `studio.nox.light` distinct from `dev.lolokeksu.contextcapsule`.

**State:** architecture agreed at a concept level; this written specification is now pending owner review. No CONTEXT CAPSULE source code, compiled APK, or integration has been performed.
