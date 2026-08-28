package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.domain.dto.AnalysisDto;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisResponseMapper {

    public AnalysisDto toResponse(AnalysisResult result) {
        AnalysisDto.ModelInfo model = new AnalysisDto.ModelInfo(
                result.model().id(),
                result.model().checkpoint(),
                result.model().device()
        );

        AnalysisDto.InputSummary input = new AnalysisDto.InputSummary(
                result.input().leadCount(),
                result.input().sampleCount(),
                result.input().samplingRateHz(),
                result.input().durationSeconds(),
                result.input().resampledSampleCount(),
                result.input().segmentsProcessed()
        );

        List<AnalysisDto.Prediction> predictions = result.predictions().stream()
                .map(prediction -> new AnalysisDto.Prediction(
                        prediction.label(),
                        prediction.score(),
                        prediction.aggregation()
                ))
                .toList();

        return new AnalysisDto(
                model,
                result.recordId(),
                input,
                predictions,
                List.copyOf(result.warnings())
        );
    }
}
