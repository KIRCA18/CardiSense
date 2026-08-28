package mk.ukim.finki.backend.domain.dto;

import java.util.List;

public record AnalysisDto(
        ModelInfo model,
        String recordId,
        InputSummary input,
        List<Prediction> predictions,
        List<String> warnings
) {
    public record ModelInfo(
            String id,
            String checkpoint,
            String device
    ) {
    }

    public record InputSummary(
            int leadCount,
            int sampleCount,
            double samplingRateHz,
            double durationSeconds,
            int resampledSampleCount,
            int segmentsProcessed
    ) {
    }

    public record Prediction(
            String label,
            double score,
            String aggregation
    ) {
    }
}
