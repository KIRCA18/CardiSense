package mk.ukim.finki.backend.Domain;

import java.util.List;

public record CommonEcg(
        String recordId,
        double[][] waveform,
        List<String> leadNames,
        double samplingRateHz,
        List<String> units
) {
}