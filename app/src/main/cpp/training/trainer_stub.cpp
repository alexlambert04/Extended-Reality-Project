#include "trainer_stub.h"

#include <string>

namespace {

std::string g_training_state = "idle";

}  // namespace

namespace xr {

void StartTraining(const std::string& item_id) {
    g_training_state = "training:" + item_id;
}

void StopTraining() {
    g_training_state = "idle";
}

const char* TrainingState() {
    return g_training_state.c_str();
}

}  // namespace xr

