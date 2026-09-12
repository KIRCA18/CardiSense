package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.domain.dto.AnalysisEntryResponse;
import mk.ukim.finki.backend.domain.entity.Analysis;

import java.util.List;

public final class AnalysisEntryMapper {

    private AnalysisEntryMapper() {
    }

    public static AnalysisEntryResponse toResponse(Analysis analysis) {
        List<AnalysisEntryResponse.PredictionResponse> predictions = analysis.getPredictions().stream()
                .map(p -> new AnalysisEntryResponse.PredictionResponse(p.getLabel(), p.getScore(), p.getAggregation()))
                .toList();

        List<String> warnings = analysis.getWarnings().stream()
                .map(w -> w.getMessage())
                .toList();

        AnalysisEntryResponse.InputResponse input = new AnalysisEntryResponse.InputResponse(
                analysis.getInputLeadCount(),
                analysis.getInputSampleCount(),
                analysis.getInputSamplingRateHz(),
                analysis.getInputDurationSeconds(),
                analysis.getResampledSampleCount(),
                analysis.getSegmentsProcessed()
        );

        return new AnalysisEntryResponse(
                analysis.getId(),
                analysis.getEcgRecord().getUpload().getOriginalFilename(),
                analysis.getEcgRecord().getRecordName(),
                analysis.getModel().getModelKey(),
                analysis.getModel().getName(),
                analysis.getStatus().name().toLowerCase(),
                analysis.getStartedAt(),
                analysis.getCompletedAt(),
                input,
                predictions,
                warnings,
                analysis.getErrorMessage()
        );
    }
}
