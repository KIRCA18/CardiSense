package mk.ukim.finki.backend.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatasetResponse(
        UUID id,
        String name,
        String originalFormat,
        Instant uploadedAt,
        int recordCount,
        List<RecordResponse> records
) {
    public record RecordResponse(
            UUID id,
            String filename,
            int leadCount,
            int sampleCount,
            double samplingRateHz,
            double durationSeconds
    ) {
    }
}