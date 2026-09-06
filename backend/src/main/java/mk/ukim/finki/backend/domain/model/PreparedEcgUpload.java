package mk.ukim.finki.backend.domain.model;

import mk.ukim.finki.backend.domain.enums.EcgFormat;

import java.util.List;

public record PreparedEcgUpload(
        EcgFormat format,
        String filenameForStorage,
        byte[] contentForStorage,
        List<ParsedEcgRecord> records
) {
}