package mk.ukim.finki.backend.Domain;

import java.util.List;
import java.util.Map;

public record ParsedEcg(
        double[][] waveform,
        List<String> leadNames,
        double samplingRateHz,
        List<String> units,
        Map<String, String> metadata
) {
}