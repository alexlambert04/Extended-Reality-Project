cmake_minimum_required(VERSION 3.22.1)
project(xr_native)

add_library(
        xr_native
        SHARED
        src/main/cpp/jni_bridge.cpp
        src/main/cpp/training/trainer_stub.cpp
        src/main/cpp/rendering/renderer_stub.cpp)

find_library(log-lib log)

target_link_libraries(
        xr_native
        ${log-lib})

