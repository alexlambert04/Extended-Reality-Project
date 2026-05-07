# Extended Reality Project - 3D Gaussian Splatting Marketplace MVP

An Android application for capturing object videos, processing them via KIRI 3D Gaussian Splatting cloud pipeline, and viewing generated 3D models locally in-app. This is a second-hand marketplace where sellers capture items as videos and buyers can view them as interactive 3D models.

**Status:** MVP - Core pipeline implemented and functional  
**For detailed technical flow:** See [the full flow documentation](documentation/app_supabase_kiri_flow.md)

## Current Implementation State

### ✅ Core Features Implemented

- **Video Capture:** Record object videos using CameraX with configurable constraints (max 3-minute duration, HD quality).
- **Marketplace Listing:** Browse available items as a marketplace gallery with search and category filters.
- **Supabase Integration:** Full REST API integration for data persistence and file uploads.
- **KIRI Pipeline:** Submit videos to KIRI 3D Gaussian Splatting engine through Supabase Edge Functions.
- **Asynchronous Polling:** Real-time status polling via HTTP with exponential backoff.
- **Model Download & Extraction:** Automatic ZIP extraction of generated artifacts into app cache.
- **3D Viewer:** WebView-based gsplat.js viewer for local `.ply` model rendering.
- **Local Model Library:** Browse cached models directly from device storage.
- **Generation Progress UI:** Live status tracking with debug information.
- **Multi-API-Key Rotation:** Server-side key management for KIRI credit distribution.

### What This MVP Does

1. **Record:** Capture object video (30-180 seconds) using the device camera.
2. **Upload:** Register item in Supabase and upload video to cloud storage.
3. **Generate:** Trigger KIRI 3D generation through backend Edge Functions.
4. **Monitor:** Poll generation status with live progress updates.
5. **Download:** Fetch completed model artifact from Supabase and cache locally.
6. **View:** Open and render 3D model in WebView-based gsplat viewer.
7. **Browse:** View marketplace items created by other users with local model caching.

## Architecture

### Tech Stack

- **Language:** Java (Android 27+)
- **Build:** Gradle with Kotlin DSL (AGP 8.13.2)
- **UI Framework:** AndroidX with View Binding
- **Networking:** OkHttp 4.12.0 for REST API calls
- **Camera:** CameraX 1.3.4 for video recording
- **3D Rendering:** Sceneform + gsplat.js (WebView-based)
- **AR Capabilities:** Google ARCore 1.31.0 (integrated)
- **Image Loading:** Glide 4.16.0
- **Backend:** Supabase (PostgreSQL + Storage + Edge Functions)
- **3D Generation:** KIRI Engine API (external cloud service)

### Project Structure

```
app/src/main/java/be/kuleuven/gt/extendedrealityproject/
├── MainActivity.java                 # Bottom nav hub (Browse / Sell)
├── camera/
│   ├── CameraCaptureActivity.java    # Video recording UI
│   ├── RegistrationActivity.java     # Post-recording form (title, metadata)
│   ├── GenerationProgressActivity.java # Real-time status tracker
│   ├── ModelViewerActivity.java      # WebView-based 3D viewer
│   ├── RecordingFlowContract.java    # Intent extras contract
│   └── GenerationSessionStore.java   # Local persistence for recovery
├── ar/
│   └── [ARCore integration for future AR viewing]
├── ui/
│   ├── browse/
│   │   ├── BrowseFragment.java       # Marketplace listing & search
│   │   ├── ItemCardAdapter.java      # RecyclerView adapter
│   │   └── [related views and models]
│   └── sell/
│       ├── SellFragment.java         # Sell/upload hub
│       └── [related views and models]
├── supabase/
│   ├── SupabaseRepository.java       # Core REST API client
│   ├── MarketplaceItemRecord.java    # Data model (DB row)
│   └── [related data classes]
└── [other utilities]
```

### System Components (Medium-Level)

- **Android App (Java):**
  - Capture flow and generation progress UI.
  - Marketplace/gallery list and local cache checks.
  - Repository layer handling Supabase REST + function calls.
- **Supabase Database (PostgreSQL):**
  - `MarketplaceItems` tracks item lifecycle and model URL.
  - `apiproviderkeys` stores rotated KIRI API keys (Edge Functions only).
  - RPC `get_available_credits()` returns safe remaining scan count.
- **Supabase Storage:**
  - `raw_scans` for uploaded source videos.
  - `3d_models` for generated model ZIP artifacts.
- **Supabase Edge Functions:**
  - `start-kiri-job` uploads to KIRI with key rotation.
  - `kiri-webhook` receives completion callback and stores model artifact.
- **KIRI Engine:**
   - Cloud-side 3D Gaussian Splatting generation.

## Multi-API-Key System (Why We Needed It)

- Each KIRI account starts with ~20 free credits (1 credit = 1 generation), then requires a 500-credit minimum purchase.
- For an MVP, that jump is a significant barrier to entry.
- KIRI usage is credit-limited per API key, making a single static key a bottleneck.
- **Solution:** Backend-managed key pool stored in `apiproviderkeys` table, spread across multiple KIRI accounts.
- **How it works:**
  1. For each new scan, Edge Functions select an available (non-exhausted) API key.
  2. Job is submitted to KIRI under that key; the key ID is stored in `MarketplaceItems.used_api_key_id`.
  3. Webhook handler uses the same key context for model download.
  4. App queries `/rest/v1/rpc/get_available_credits` RPC to get safe aggregate count (no raw keys leaked).
  5. "Available Scans" is disabled when credits are exhausted.

### How to Add a New KIRI API Key

#### Step 1: Set up in KIRI Developer Dashboard

1. Go to https://www.kiriengine.app/api/signup
2. Create a new account (use a non-critical email, or add a dot prefix trick to reuse old address):
   - Example: `user@gmail.com` → `user.1@gmail.com` (confirmation still goes to first address)
3. Generate a new API key and **copy it immediately** (you won't see it again).
4. Go to **Webhooks** section and create a webhook:
   - **Callback URL:** `beaysaooaaukrsvlprvp.supabase.co/functions/v1/kiri-webhook`
   - **Signing Secret:** `supabase`
   - Save

#### Step 2: Insert into Supabase `apiproviderkeys` Table

Add a new row with:
- **id:** (auto-generated UUID, leave empty)
- **api_key:** (paste your copied key)
- **usage_count:** `0` (new account)
- **max_limit:** `20` (KIRI free tier default)
- **is_active:** `TRUE` (enabled for new jobs)
- **created_at:** (auto-filled, leave empty)
- **acc_name:** (email used for the KIRI account, for debugging only)

The system automatically picks this key for new generations once saved.

## User Experience & Flow

### Selling (Recording & Uploading)

1. **Tap "Sell" tab** → Select "Record Object"
2. **Record video** (30-180 seconds) with live preview and constraints
3. **Enter metadata** (title, category, description, price, location, seller name)
4. **Tap "Generate Model"** → Async upload and KIRI job trigger
5. **Live progress** updates (polling status every 5-10 seconds with exponential backoff)
6. **Status stages:**
   - `UPLOADING` → Video file transfer to cloud
   - `SENDING_TO_KIRI` → Invoking Edge Function with KIRI API
   - `PROCESSING_IN_CLOUD` → KIRI 3DGS training pipeline
   - `DOWNLOADING_ARTIFACT` → Fetching generated model
   - `READY` → Model available for viewing
   - `FAILED` → Detailed error with retry option
7. **Tap "View Model"** → Opens WebView 3D gsplat viewer

### Browsing (Marketplace)

1. **Tap "Browse" tab** → See marketplace gallery (items in `READY` status)
2. **Search/filter** by category or text
3. **Tap item** → View item details (title, price, description, seller)
4. **Tap "View Model"** → Download if needed and open in 3D viewer
5. **Local caching** → Models stay on device after first download

### Timing Expectations

- **Upload:** ~1-5 minutes (depends on video size and network)
- **KIRI Processing:** ~15-20 minutes (cloud-side 3DGS training)
- **Total Latency:** ~20-25 minutes from record to view
- Models are polled every 5-10 seconds; app remains responsive during wait

## Known MVP Limitations

### Supabase Free Tier Upload Limit (50 MB)

KIRI may generate ZIP artifacts larger than Supabase's 50 MB object size limit. If this occurs:

**Symptoms:**
- Generation reaches `READY`-like completion but model download fails
- HTTP 404: `"object not found"` or `"file not found"`

**Workaround:**
- Re-record with shorter duration (30-60 seconds ideal)
- Reduce video quality or frame rate if possible
- Tighter framing on target object (avoid background)
- Check generated file size in server logs to confirm size as root cause

### Client-Side File Size Check

App validates video before upload:
- Refuses uploads > 50 MB with clear message
- Encourages shorter or lower-quality re-record

## Testing & Debugging

- **Available Scans Widget:** Shows remaining KIRI credits (updated every 30 seconds)
- **Progress Debug Panel:** Detailed status, timestamps, serialize ID, backend messages
- **Local Models List:** Browse cached models in device storage
- **Session Recovery:** App persists generation state; progress survives app restart
- **Retry Paths:** Failed jobs can be retried from progress screen

## Architecture Decision: Why Cloud-Based KIRI Instead of On-Device

### Original Vision
The project began with a goal of **fully on-device 3D Gaussian Splatting** (3DGS training + rendering using ARCore poses and edge compute).

### Reality Check
1. **On-device 3DGS training** requires:
   - Full CUDA/GPU-level optimization code in C++ (NDK)
   - Significant low-level rendering pipeline work
   - Would essentially be writing a thesis project, not an MVP
   - Average smartphones lack GPU memory for realistic-scale training

2. **Alternative options evaluated:**
   - Host own CUDA server (expensive, operational burden)
   - Use spare gaming laptop (availability issues)
   - Use external cloud service (scaling, cost-effectiveness)

### Final Decision: KIRI Engine API

**Why KIRI:**
- ✅ Simple REST API integration
- ✅ 20 free generatations per account (MVP budget-friendly)
- ✅ Good documentation
- ✅ Webhook support for async callbacks
- ✅ Fast processing (~15-20 min per model)
- ✅ Reliable `.ply` output format

**Implementation:**
- Supabase Edge Functions handle API calls server-side (no raw KIRI keys in app)
- Multi-account key rotation distributes free credits
- Webhook callbacks trigger model download
- App polls status via Supabase RPC and REST endpoints

**Flow:**
1. App records video
2. Video → Supabase Storage (`raw_scans/`)
3. Supabase Edge Function → KIRI Engine API
4. KIRI processes 3DGS model (15-20 min)
5. KIRI webhook → Supabase download and store in (`3d_models/`)
6. App polls status, downloads model when ready
7. App renders via gsplat.js viewer

This is a **pragmatic MVP trade-off**: we gain a working end-to-end system now, with clear upgrade path to on-device training later.

## Build & Setup

### Prerequisites

1. **Android Studio** 2024.1+
2. **Android SDK 34**, NDK (if using C++ components)
3. **Java 17+**
4. **Supabase Project** with configured URL and anon key
5. **KIRI Engine API** account(s) and keys in `apiproviderkeys` table

### Configuration

Set environment variables or add to `local.properties`:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_anon_key_here
```

The build system reads these and injects them as `BuildConfig` fields for secure access.

### Build Command

```powershell
.\gradlew.bat :app:assembleDebug
```

Or use Android Studio's build menu.

### Dependencies

- **androidx.appcompat:appcompat** 1.7.0
- **androidx.camera:*** (CameraX suite) 1.3.4
- **com.google.android.material:material** 1.12.0
- **com.squareup.okhttp3:okhttp** 4.12.0
- **com.github.bumptech.glide:glide** 4.16.0
- **com.google.ar:core** (ARCore) 1.31.0
- **androidx.webkit:webkit** 1.15.0 (WebView for gsplat viewer)
- Plus testing libs (JUnit, Espresso)

## Implementation Checklist

### Phase 1: Capture & Recording ✅ Complete
- [x] CameraX video recording with constraints
- [x] Registration form with metadata input
- [x] Local file size validation

### Phase 2: Supabase & Pipeline ✅ Complete
- [x] SupabaseRepository REST client
- [x] Video upload to `raw_scans` storage
- [x] Supabase Edge Function calls (`start-kiri-job`)
- [x] Async status polling with exponential backoff
- [x] Multi-API-key rotation system
- [x] RPC call for available credits
- [x] Error handling and retry paths
- [x] Session persistence for recovery

### Phase 3: Model Retrieval & Visualization ✅ Complete
- [x] Model download from `3d_models` storage
- [x] ZIP extraction to cache
- [x] WebView-based gsplat.js viewer
- [x] Local `.ply` model support

### Phase 4: Marketplace & Browsing ✅ Complete
- [x] BrowseFragment marketplace list
- [x] Category filtering and search
- [x] ItemCardAdapter with pagination
- [x] Remote model caching
- [x] LocalModelsActivity for cached models

### Future Enhancements (Post-MVP)
- [ ] Persistent storage migration (`filesDir` instead of `cacheDir`)
- [ ] Storage quota management and LRU eviction with pinning
- [ ] User authentication and RLS policies
- [ ] Shared/Public model discovery
- [ ] AR viewing with ARCore plane detection
- [ ] Local on-device 3DGS training (requires significant C++ work)
- [ ] Automatic retry-kiri-job Edge Function for failed jobs
