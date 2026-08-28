package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.service.externalmodel.FastApiContracts;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import mk.ukim.finki.backend.domain.model.CommonEcg;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FastApiModelMapper {

    public FastApiContracts.InferenceRequest toRequest(
            String modelId,
            CommonEcg ecg
    ) {
        return new FastApiContracts.InferenceRequest(
                modelId,
                new FastApiContracts.EcgPayload(
                        ecg.recordId(),
                        ecg.waveform(),
                        ecg.leadNames(),
                        ecg.samplingRateHz(),
                        ecg.units()
                )
        );
    }

    public AnalysisResult toDomain(FastApiContracts.InferenceResponse response) {
        AnalysisResult.ModelInfo model = new AnalysisResult.ModelInfo(
                response.model().id(),
                response.model().checkpoint(),
                response.model().device()
        );

        AnalysisResult.InputSummary input = new AnalysisResult.InputSummary(
                response.input().leadCount(),
                response.input().sampleCount(),
                response.input().samplingRateHz(),
                response.input().durationSeconds(),
                response.input().resampledSampleCount(),
                response.input().segmentsProcessed()
        );

        List<AnalysisResult.Prediction> predictions = response.predictions().stream()
                .map(prediction -> new AnalysisResult.Prediction(
                        prediction.label(),
                        prediction.score(),
                        prediction.aggregation()
                ))
                .toList();

        return new AnalysisResult(
                model,
                response.recordId(),
                input,
                predictions,
                List.copyOf(response.warnings())
        );
    }
}
