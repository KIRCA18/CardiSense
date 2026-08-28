package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.domain.dto.AnalysisDto;
import mk.ukim.finki.backend.domain.dto.DatasetAnalysisDto;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import mk.ukim.finki.backend.domain.model.DatasetAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisResponseMapper {

    public DatasetAnalysisDto toResponse(DatasetAnalysisResult result) {
        List<DatasetAnalysisDto.RecordAnalysisDto> records = result.records().stream()
                .map(record -> new DatasetAnalysisDto.RecordAnalysisDto(
                        record.recordName(),
                        toAnalysisDto(record.analysis())
                ))
                .toList();

        return new DatasetAnalysisDto(records.size(), records);
    }

    private AnalysisDto toAnalysisDto(AnalysisResult result) {
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
