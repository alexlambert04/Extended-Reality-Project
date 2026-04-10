package be.kuleuven.gt.extendedrealityproject.camera;

public final class RecordingFlowContract {

    public static final long MAX_RECORDING_MS = 180_000L;
    public static final long RECOMMENDATION_MS = 30_000L;

    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_RECORDING_TITLE = "extra_recording_title";

    private RecordingFlowContract() {
        // Utility class
    }
}

