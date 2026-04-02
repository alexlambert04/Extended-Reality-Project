#ifndef XR_TRAINER_STUB_H
#define XR_TRAINER_STUB_H

#include <string>

namespace xr {

void StartTraining(const std::string& item_id);
void StopTraining();
const char* TrainingState();

}  // namespace xr

#endif

