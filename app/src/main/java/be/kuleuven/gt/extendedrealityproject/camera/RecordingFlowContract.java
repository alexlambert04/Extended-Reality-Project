package be.kuleuven.gt.extendedrealityproject.camera;

public final class RecordingFlowContract {

    public static final long MAX_RECORDING_MS = 150_000L;
    public static final long RECOMMENDATION_MS = 60_000L;
    public static final long MAX_UPLOAD_BYTES = 250L * 1024L * 1024L;
    public static final long RECOMMENDED_UPLOAD_BYTES = 240L * 1024L * 1024L;

    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_RECORDING_TITLE = "extra_recording_title";
    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_HINT_SHORTER_RECORDING = "extra_hint_shorter_recording";

    private RecordingFlowContract() {
        // Utility class
    }
}
