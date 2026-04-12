# Extended Reality Project (MVP)

Android MVP for recording short object videos, sending them through the KIRI 3DGS pipeline, and viewing generated models locally in-app.

For the full, step-by-step technical pipeline, see [the full flow documentation](documentation/app_supabase_kiri_flow.md).

## What This MVP Does

- Record an object video in the app.
- Register and upload the recording to Supabase.
- Trigger KIRI generation through Supabase Edge Functions.
- Poll generation status from Android via raw HTTP (`OkHttp`).
- Download and extract generated model artifacts into Android `cacheDir`.
- Open the local model in `ModelViewerActivity` (WebView-based viewer).

## System Components (Medium-Level)

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

- Each KIRI account starts with about 20 free credits (1 credit = 1 generation), then asks for a 500-credit minimum top-up.
- For an MVP, that jump is less "nice upgrade" and more "do they think we are funded by venture capital already?"
- KIRI usage is credit-limited per API key, so a single static key quickly becomes a bottleneck and single point of failure.
- We use a backend-managed key pool (`apiproviderkeys`) to spread usage across multiple active keys.
- For each scan, Edge Functions pick one available key, submit the job, and store which key was used (`used_api_key_id`) so webhook download uses the same account context.
- API keys stay server-side only; the Android app never sees raw key values.
- The app gets only a safe aggregate count through `POST /rest/v1/rpc/get_available_credits`, used to show `Available Scans` and disable recording when exhausted.

## How to add a new API key to the pool

### first we have to set some things up in the kiri developer dashboard

go to the Kiri developer dashboard at https://www.kiriengine.app/api/signup
make a new account, ideally with a non important email, or just your personal one, it really doesn't matter that much
> pro tip, if you already have made an api key/kiri account once, and you want to make another kiri account
you can just use the old (already existing account) mail address and add a "." somewhere in the first part of the email address (before the "@"-sign)
kiri will recognise this email address as a new address and thus a new account, whilst the confirmation code will still be sent to that same old mail address;
no need to create a whole new mail address for each new api key

### once you've got yourself a new account, you do two things:
1) you make a new API key, the name doesnt matter, just choose something, BUT MAKE SURE YOU COPY THE API KEY (either in a txt file somewhere or in your clipboard, just somewhere), you wont be able to see the api key ever again
2) you go to the webhooks section on the dashboard and you create the webhook EXACTLY AS FOLLOWS:
   - in the callback URL field you insert: **beaysaooaaukrsvlprvp.supabase.co/functions/v1/kiri-webhook**
   - in the signing secret you insert our super-duper-secret password: **supabase**
   - save that shizzle and klaar is kees, at least for the kiri part

### second part, actually inserting the API key (which you hopefully copied, you had one job)
1) add a new row in the apiproviderkeys table
   - id: a random uuid that is automatically generated, LEAVE EMPTY
   - api_key: insert your copied api key in here
   - usage_count: the amount of times this api key already has been used, for a new account this is 0, obviously
   - max_limit: the maximum amount of times this api key can be used, for a new account this is 20
   - is_active: whether this api key is active and can be used for new generations, for a new account this is TRUE (in capitals)
   - created_at: automatically filled in with the current time, LEAVE EMPTY
   - acc_name: add here the email address of the account you used to create the api key (this is just for debugging purposes, we dont actually do anything with it programmatically)
2) save that row, and boom, you're ready, the system will automatically start to use that new api key in the pool

## What Users Should Expect

- Generation is asynchronous: upload and trigger are quick, but cloud processing takes time.
- Typical KIRI processing + end-to-end availability is around **20-25 minutes**.
- The app shows generation state and allows viewing once status is `READY`.
- Models are cached in Android `cacheDir` (OS-managed eviction).

## Known MVP Limitation (Supabase Free Tier)

Supabase free tier has a max object size limit (50 MB). If KIRI returns a ZIP artifact larger than that limit, the upload to `3d_models` can fail, and the app later cannot download the model.

### How It Appears

- Generation may reach `READY`-like completion flow but model retrieval fails.
- App-side error during model download can look like:

```text
HTTP 400: {"statusCode":"404","error":"not_found","message":"object not found"}
```

### What To Do (MVP Workaround)

- Retry the scan with a shorter/simpler capture.
- Reduce video duration and avoid unnecessary scene content.
- Re-record with tighter framing around the target object.

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```


