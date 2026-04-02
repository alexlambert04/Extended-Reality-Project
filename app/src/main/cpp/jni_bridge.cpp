#include <jni.h>

#include <string>

#include "rendering/renderer_stub.h"
#include "training/trainer_stub.h"

extern "C" JNIEXPORT jboolean JNICALL
Java_be_kuleuven_gt_extendedrealityproject_NativeBridge_nativeInit(
        JNIEnv* env,
        jobject thiz) {
    (void)env;
    (void)thiz;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_be_kuleuven_gt_extendedrealityproject_NativeBridge_nativeGetRuntimeStatus(
        JNIEnv* env,
        jobject thiz) {
    (void)thiz;
    std::string status = std::string("training=") + xr::TrainingState() + "; "
            + xr::LastPoseState();
    return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_be_kuleuven_gt_extendedrealityproject_NativeBridge_nativeSubmitCameraPose(
        JNIEnv* env,
        jobject thiz,
        jfloatArray pose_matrix4x4) {
    (void)thiz;
    if (pose_matrix4x4 == nullptr) {
        xr::SubmitCameraPose(nullptr, 0);
        return;
    }

    const jsize len = env->GetArrayLength(pose_matrix4x4);
    jfloat* raw = env->GetFloatArrayElements(pose_matrix4x4, nullptr);
    xr::SubmitCameraPose(raw, static_cast<int>(len));
    env->ReleaseFloatArrayElements(pose_matrix4x4, raw, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL
Java_be_kuleuven_gt_extendedrealityproject_NativeBridge_nativeStartTraining(
        JNIEnv* env,
        jobject thiz,
        jstring item_id) {
    (void)thiz;
    const char* item_chars = env->GetStringUTFChars(item_id, nullptr);
    xr::StartTraining(item_chars == nullptr ? "unknown" : item_chars);
    if (item_chars != nullptr) {
        env->ReleaseStringUTFChars(item_id, item_chars);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_be_kuleuven_gt_extendedrealityproject_NativeBridge_nativeStopTraining(
        JNIEnv* env,
        jobject thiz) {
    (void)env;
    (void)thiz;
    xr::StopTraining();
}

