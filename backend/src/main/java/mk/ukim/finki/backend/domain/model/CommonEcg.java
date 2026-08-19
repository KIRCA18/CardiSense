package mk.ukim.finki.backend.domain.model;

import java.util.List;

public record CommonEcg(
        String recordId,
        double[][] waveform,
        List<String> leadNames,
        double samplingRateHz,
        List<String> units
) {
}
