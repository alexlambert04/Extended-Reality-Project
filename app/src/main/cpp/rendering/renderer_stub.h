#ifndef XR_RENDERER_STUB_H
#define XR_RENDERER_STUB_H

namespace xr {

void SubmitCameraPose(const float* pose, int len);
const char* LastPoseState();

}  // namespace xr

#endif

