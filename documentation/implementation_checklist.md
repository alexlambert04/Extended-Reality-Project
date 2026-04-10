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


