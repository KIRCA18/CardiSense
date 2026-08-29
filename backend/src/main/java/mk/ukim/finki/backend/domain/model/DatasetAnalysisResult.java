package mk.ukim.finki.backend.domain.model;

import java.util.List;

public record DatasetAnalysisResult(
        List<RecordAnalysis> records
) {
    public DatasetAnalysisResult {
        records = List.copyOf(records);
    }

    public record RecordAnalysis(
            String recordName,
            AnalysisResult analysis
    ) {
    }
}
