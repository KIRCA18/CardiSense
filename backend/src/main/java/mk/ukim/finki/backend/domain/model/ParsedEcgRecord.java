package mk.ukim.finki.backend.domain.model;

public record ParsedEcgRecord(
        String recordName,
        ParsedEcg ecg
) {
}
