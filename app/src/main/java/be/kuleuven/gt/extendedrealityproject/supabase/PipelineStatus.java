package be.kuleuven.gt.extendedrealityproject.supabase;

import java.util.Locale;

public enum PipelineStatus {
    UPLOADING,
    SENDING_TO_KIRI,
    PROCESSING_IN_CLOUD,
    DOWNLOADING_ARTIFACT,
    READY,
    FAILED,
    UNKNOWN;

    public static PipelineStatus from(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.toUpperCase(Locale.US));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }

    public boolean isTerminal() {
        return this == READY || this == FAILED;
    }
}

