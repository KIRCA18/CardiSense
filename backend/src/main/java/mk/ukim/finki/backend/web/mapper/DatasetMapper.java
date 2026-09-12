package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.domain.dto.DatasetResponse;
import mk.ukim.finki.backend.domain.entity.EcgRecord;
import mk.ukim.finki.backend.domain.entity.EcgUpload;

import java.util.List;

public final class DatasetMapper {

    private DatasetMapper() {
    }

    public static DatasetResponse toResponse(EcgUpload upload, List<EcgRecord> records) {
        List<DatasetResponse.RecordResponse> recordResponses = records.stream()
                .map(DatasetMapper::toRecordResponse)
                .toList();

        String originalFormat = records.isEmpty() ? "UNKNOWN" : records.get(0).getOriginalFormat();

        return new DatasetResponse(
                upload.getId(),
                upload.getOriginalFilename(),
                originalFormat,
                upload.getUploadedAt(),
                recordResponses.size(),
                recordResponses
        );
    }

    private static DatasetResponse.RecordResponse toRecordResponse(EcgRecord record) {
        return new DatasetResponse.RecordResponse(
                record.getId(),
                record.getRecordName(),
                record.getLeadNames() == null ? 0 : record.getLeadNames().size(),
                record.getSampleCount(),
                record.getSamplingRateHz(),
                record.getDurationSeconds()
        );
    }
}
