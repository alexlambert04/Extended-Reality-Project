package be.kuleuven.gt.extendedrealityproject;

public class NativeBridge {

    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary("xr_native");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        NATIVE_AVAILABLE = loaded;
    }

    public native boolean nativeInit();

    public native String nativeGetRuntimeStatus();

    public native void nativeSubmitCameraPose(float[] poseMatrix4x4);

    public native void nativeStartTraining(String itemId);

    public native void nativeStopTraining();

    public String getRuntimeStatus() {
        if (!NATIVE_AVAILABLE) {
            return "Native runtime status: library not loaded";
        }
        return nativeGetRuntimeStatus();
    }

    public boolean initializeRuntime() {
        if (!NATIVE_AVAILABLE) {
            return false;
        }
        return nativeInit();
    }

    public void submitCameraPose(float[] poseMatrix4x4) {
        if (NATIVE_AVAILABLE) {
            nativeSubmitCameraPose(poseMatrix4x4);
        }
    }

    public void startTraining(String itemId) {
        if (NATIVE_AVAILABLE) {
            nativeStartTraining(itemId);
        }
    }

    public void stopTraining() {
        if (NATIVE_AVAILABLE) {
            nativeStopTraining();
        }
    }
}



