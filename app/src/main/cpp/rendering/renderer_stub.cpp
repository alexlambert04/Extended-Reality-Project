#include "renderer_stub.h"

#include <string>

namespace {

std::string g_pose_state = "pose:none";

}  // namespace

namespace xr {

void SubmitCameraPose(const float* pose, int len) {
    if (pose != nullptr && len >= 16) {
        g_pose_state = "pose:received_4x4";
    } else {
        g_pose_state = "pose:invalid";
    }
}

const char* LastPoseState() {
    return g_pose_state.c_str();
}

}  // namespace xr

