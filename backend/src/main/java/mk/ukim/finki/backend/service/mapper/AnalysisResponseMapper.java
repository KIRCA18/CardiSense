package mk.ukim.finki.backend.service.mapper;

import mk.ukim.finki.backend.domain.dto.AnalysisResponse;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisResponseMapper {

    public AnalysisResponse toResponse(AnalysisResult result) {
        AnalysisResponse.ModelInfo model = new AnalysisResponse.ModelInfo(
                result.model().id(),
                result.model().checkpoint(),
                result.model().device()
        );

        AnalysisResponse.InputSummary input = new AnalysisResponse.InputSummary(
                result.input().leadCount(),
                result.input().sampleCount(),
                result.input().samplingRateHz(),
                result.input().durationSeconds(),
                result.input().resampledSampleCount(),
                result.input().segmentsProcessed()
        );

        List<AnalysisResponse.Prediction> predictions = result.predictions().stream()
                .map(prediction -> new AnalysisResponse.Prediction(
                        prediction.label(),
                        prediction.score(),
                        prediction.aggregation()
                ))
                .toList();

        return new AnalysisResponse(
                model,
                result.recordId(),
                input,
                predictions,
                List.copyOf(result.warnings())
        );
    }
}
