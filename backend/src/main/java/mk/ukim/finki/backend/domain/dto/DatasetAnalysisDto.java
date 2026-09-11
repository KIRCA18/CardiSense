package mk.ukim.finki.backend.domain.dto;

import java.util.List;

public record DatasetAnalysisDto(
        int recordCount,
        List<RecordAnalysisDto> records
) {
    public record RecordAnalysisDto(
            String recordName,
            AnalysisDto analysis
    ) {
    }
}
