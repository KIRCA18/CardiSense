package mk.ukim.finki.backend.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisEntryResponse(
        UUID id,
        String datasetName,
        String recordFilename,
        String modelKey,
        String modelName,
        String status,
        Instant startedAt,
        Instant completedAt,
        InputResponse input,
        List<PredictionResponse> predictions,
        List<String> warnings,
        String errorMessage
) {
    public record InputResponse(
            int leadCount,
            int sampleCount,
            double samplingRateHz,
            double durationSeconds,
            int resampledSampleCount,
            int segmentsProcessed
    ) {
    }

    public record PredictionResponse(
            String label,
            double score,
            String aggregation
    ) {
    }
}
