package mk.ukim.finki.backend.domain.model;

import mk.ukim.finki.backend.domain.entity.EcgRecord;
import mk.ukim.finki.backend.domain.entity.EcgUpload;

import java.util.List;

public record IngestedUpload(
        EcgUpload upload,
        List<EcgRecord> records
) {
}
