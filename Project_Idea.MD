# Project Context: 3D Gaussian Splatting (3DGS) AR Marketplace

## 1. Project Overview
This project is a second-hand marketplace application. It allows sellers to capture multiple photos of physical items, which are processed entirely **on-device** into high-fidelity 3D models using 3D Gaussian Splatting (3DGS). Buyers can view these items in a 3D viewer or project them into their physical space using Augmented Reality (AR) to assess scale and condition.

The core engineering challenge is bridging high-level Android UI components with complex, low-level C++ rendering and hardware-aware edge-compute training pipelines.

## 2. Architecture & Tech Stack
This is a fully native, on-device (edge compute) architecture. There is no cloud processing for the 3D models.

* **Application & UI Layer:** Kotlin + Jetpack Compose. Handles marketplace grids, user flows, and state management.
* **Spatial & AR Layer:** Google ARCore.
    * *Capture Phase:* Extracts raw tracking matrices/camera poses to bypass traditional Structure-from-Motion (SfM).
    * *Viewing Phase:* Handles plane detection and anchors the 3D model in physical space.
* **Training Engine (Edge Compute):** C++ via Android NDK.
    * Implements hardware-aware 3DGS training loops (e.g., PocketGS concepts) optimizing for mobile GPU memory constraints.
* **Rendering Layer:** Vulkan compute shaders (via C++) OR WebGPU (`gsplat.js` via WebView). Handles sorting and rendering of millions of semi-transparent splats at 60fps.
* **The Bridge:** Java Native Interface (JNI) and CMake. Connects the Kotlin UI layer to the C++ processing layer.

## 3. Directory Structure & Domain Separation
The project uses a unified Android Studio/Gradle build system. Dependencies are separated by architectural layer.

```text
app/
├── src/
│   ├── main/
│   │   ├── java/com/team/app/     # Kotlin/UI Domain
│   │   │   ├── ui/                # Jetpack Compose UI screens
│   │   │   ├── ar/                # ARCore session and pose extraction
│   │   │   └── NativeBridge.kt    # Kotlin declarations of external C++ functions
│   │   │
│   │   ├── cpp/                   # C++/NDK Domain
│   │   │   ├── training/          # C++ code for 3DGS math and optimization
│   │   │   ├── rendering/         # Vulkan setup and compute shaders
│   │   │   └── jni_bridge.cpp     # C++ implementation linking to NativeBridge.kt
│   │   │
│   │   ├── res/                   # Standard Android resources
│   │   └── AndroidManifest.xml    
│   │
├── CMakeLists.txt                 # NDK compiler instructions for the cpp/ directory
└── build.gradle.kts               # Handles Kotlin compilation and CMake invocation
```

## DISCLAIMER
This is only a project idea is not a fixed implementation. All proposed software does not have to be that exact one. This file only outlines the general idea, not yet the exact implementation. The project is open to changes and improvements.