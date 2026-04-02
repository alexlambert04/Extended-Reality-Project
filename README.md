# Extended Reality Project

Baseline Android architecture for a 3DGS AR marketplace with a Java UI layer and native C++ pipeline scaffolding.

## Architecture Snapshot

- Java app layer: `app/src/main/java/be/kuleuven/gt/extendedrealityproject`
- AR package: `app/src/main/java/be/kuleuven/gt/extendedrealityproject/ar`
- UI state package: `app/src/main/java/be/kuleuven/gt/extendedrealityproject/ui`
- JNI bridge: `app/src/main/java/be/kuleuven/gt/extendedrealityproject/NativeBridge.java`
- Native C++ layer: `app/src/main/cpp`
- CMake entrypoint: `app/CMakeLists.txt`

## Native Entry Points

`NativeBridge` currently exposes placeholder JNI methods:

- `nativeInit()`
- `nativeGetRuntimeStatus()`
- `nativeSubmitCameraPose(float[])`
- `nativeStartTraining(String)`
- `nativeStopTraining()`

These map to stubs in `jni_bridge.cpp`, `training/trainer_stub.cpp`, and `rendering/renderer_stub.cpp`.

## Quick Try

```powershell
.\gradlew.bat :app:assembleDebug
```

If ARCore support is unavailable on a test device, the manifest is configured as optional and the app remains installable as a scaffold.

