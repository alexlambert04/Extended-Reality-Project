# Android Capture Flow Implementation Checklist

This checklist tracks the Android-side implementation for the recording + registration flow from `documentation/app_supabase_kiri_flow.md`.

- [x] Add a new `Record Object` entry button on `MainActivity` (keep existing main screen behavior unchanged).
- [x] Add CameraX dependencies and register new activities in the manifest.
- [x] Build `CameraCaptureActivity` with a modern camera preview UI and recording controls.
- [x] Enforce recording constraints: max 3-minute hard stop (auto-stop) and a 30-second guidance hint.
- [x] Auto-navigate to registration after recording finalization.
- [x] Build `RegistrationActivity` with title input and actions: `Generate Model` and `Redo Recording`.
- [x] Wire `Redo Recording` to return to camera capture and `Generate Model` to continue to app flow (stub for upload pipeline).
- [x] Verify project compiles after integration.

## Next Phase: Supabase + KIRI Pipeline Wiring

- [x] Add Supabase client dependencies and network permissions (`INTERNET`) and wire secure config for URL/key via `BuildConfig` placeholders.
- [x] Create Android data models/enums that match `MarketplaceItems` and pipeline statuses (`UPLOADING`, `SENDING_TO_KIRI`, `PROCESSING_IN_CLOUD`, `DOWNLOADING_ARTIFACT`, `READY`, `FAILED`).
- [x] Build a `SupabaseRepository` service layer for: insert item row, upload video to `raw_scans`, update `file_path`, and invoke `start-kiri-job`.
- [x] Replace the `Generate Model` stub in `RegistrationActivity` with real async orchestration + clear error handling.
- [x] Add a dedicated `GenerationProgressActivity` that shows current stage, item id, serialize id (when available), timestamps, and latest backend message for visual debugging.
- [x] Implement Supabase Realtime subscription for the specific `MarketplaceItems.id` and update progress UI live until `READY` or `FAILED`.
- [x] Add retry paths in UI and backend call flow (retry `start-kiri-job`, resume by `item_id`, and prevent duplicate jobs).
- [x] Add local persistence for active generation (`item_id`, title, local video path) so progress can recover after app restart.
- [x] Add mapping from backend status/failure to user-facing text + detailed debug text section.
- [ ] Verify end-to-end with test scenarios: success path, upload failure, edge function failure, and webhook-complete transition to `READY`.

## Optional Supabase Backend Task (Recommended)

- [ ] Add a `retry-kiri-job` Edge Function for controlled retries and idempotency checks (recommended to keep retry policy server-side).

## Upload Limit Hardening (Supabase Free Tier)

- [x] Switch recording quality default from 1080p (`FHD`) to 720p (`HD`) to reduce upload payloads.
- [x] Add pre-upload client-side file size validation before calling Supabase APIs.
- [x] Block uploads above 50 MB with a clear user-facing message and redo guidance.
- [x] Show recorded file size on the registration screen for easier debugging and operator visibility.
- [x] Add direct jump-back to camera from oversize error with a one-time shorter recording hint.

## Next Phase: Model Retrieval + Visualization (MVP)

- [x] Extend `SupabaseRepository` with model artifact retrieval (`model_url` download to cache) and ZIP extraction for first `.ply` file.
- [x] Add a `View Model` action to `GenerationProgressActivity` that is enabled only when status is `READY`.
- [x] Add visual debug states for model retrieval (`download started`, `extract success`, and error paths) in progress screen output.
- [x] Build `ModelViewerActivity` with a WebView-based 3D viewer and local `.ply` loading.
- [x] Replace the old Three.js `PLYLoader` viewer with a local bundled `gsplat.js` WebView viewer implementation.

## Local Model Library (Device Storage)

- [x] Add a `View Local Models` button on `MainActivity`.
- [x] Build `LocalModelsActivity` listing cached models found under app cache (`cache/models/*/model.ply`).
- [x] Allow opening any local model directly in `ModelViewerActivity` without waiting for new KIRI processing.
- [x] Add refresh and empty-state UI for local model browsing.
- [ ] Pause further Local Model Library feature expansion until core gsplat viewer behavior is validated on target devices.

## gsplat Migration (Now Active)

- [x] Replace CDN-hosted viewer scripts with locally bundled assets for deterministic runtime behavior.
- [x] Integrate a `gsplat` renderer path in the viewer.
- [x] Add `.splat` direct-loading optimization path (fallback to `.ply` where needed).
- [ ] Define conversion/asset contract for direct local scans (before KIRI) so preview mode can visualize local intermediate data.

## Planning: Local Library Reliability + Shared Model Discovery

### Investigate Missing Local File

- [ ] Trace model lifecycle around `SupabaseRepository.downloadAndExtractModel`, `LocalModelsActivity.loadModels`, and `ModelViewerActivity` path checks to map create/read/fail points.
- [ ] Confirm current storage path behavior (`cache/models/{item_id}`) and document volatility causes (OS cache cleanup, low-storage eviction, and manual clear-cache).
- [ ] Add root-cause diagnostics for disappearances (`item_id`, absolute path, file size, extraction outcome, viewer open result).
- [ ] Write an incident note template so when a file disappears we can classify: never downloaded, extraction failed, overwritten, cache-evicted, or path mismatch.

### Local Capacity and Scale Strategy

- [ ] Define persistent storage target for long-lived models (`filesDir/models`) and treat `cacheDir` as temporary.
- [ ] Track metadata per model (`item_id`, `title`, format, bytes, created_at, last_accessed_at, source, pinned flag).
- [ ] Set storage policy thresholds (warning budget + hard cap) and implement LRU eviction with pin protection.
- [ ] Add startup migration from `cache/models/*` to persistent storage and safe cleanup for orphaned files.
- [ ] Add operator/user visibility for storage usage (total bytes, model count, and cleanup actions).

### Supabase Models Not Created by Current User

- [ ] Define browse scope: `My Models`, `Shared/Public`, and optional `Team` views with separate filters.
- [ ] Extend repository APIs with paginated list queries for `READY` items (`limit`, `offset`/cursor, sort, optional owner filter, search text).
- [ ] Align Supabase RLS/storage policies for read access to permitted non-owner items and artifacts.
- [ ] Add UI flow for remote discovery (list cards, pagination/loading states, preview metadata, download/open actions).
- [ ] Cache downloaded remote models into local library with source attribution and deduping by `item_id`.

### Decisions to Lock Before Build-Out

- [ ] Decide sharing model: anonymous public feed vs authenticated user/org feed.
- [ ] Decide storage policy strictness: automatic LRU only vs user pinning + manual cleanup controls.
- [ ] Decide remote sync behavior: on-demand fetch only vs optional background refresh.
