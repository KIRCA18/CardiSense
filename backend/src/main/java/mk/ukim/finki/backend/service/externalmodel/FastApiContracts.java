package mk.ukim.finki.backend.service.externalmodel;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

public final class FastApiContracts {

    private FastApiContracts() {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InferenceRequest(
            String modelId,
            EcgPayload ecg
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EcgPayload(
            String recordId,
            double[][] waveform,
            List<String> leadNames,
            double samplingRateHz,
            List<String> units
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InferenceResponse(
            ModelInfo model,
            String recordId,
            InputSummary input,
            List<Prediction> predictions,
            List<String> warnings
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ModelInfo(
            String id,
            String checkpoint,
            String device
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InputSummary(
            int leadCount,
            int sampleCount,
            double samplingRateHz,
            double durationSeconds,
            int resampledSampleCount,
            int segmentsProcessed
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Prediction(
            String label,
            double score,
            String aggregation
    ) {
    }
}
