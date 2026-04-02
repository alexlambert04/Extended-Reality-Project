package be.kuleuven.gt.extendedrealityproject;

public class NativeBridge {

    static {
        System.loadLibrary("xr_native");
    }

    public native boolean nativeInit();

    public native String nativeGetRuntimeStatus();

    public native void nativeSubmitCameraPose(float[] poseMatrix4x4);

    public native void nativeStartTraining(String itemId);

    public native void nativeStopTraining();

    public String getRuntimeStatus() {
        return nativeGetRuntimeStatus();
    }

    public void submitCameraPose(float[] poseMatrix4x4) {
        nativeSubmitCameraPose(poseMatrix4x4);
    }

    public void startTraining(String itemId) {
        nativeStartTraining(itemId);
    }

    public void stopTraining() {
        nativeStopTraining();
    }
}

