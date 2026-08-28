package mk.ukim.finki.backend.service.wfdb;

import java.util.List;
import java.util.Map;

record WfdbHeader(
        String recordName,
        int signalCount,
        double samplingRateHz,
        int sampleCount,
        List<WfdbSignalSpec> signals,
        Map<String, String> metadata
) {
}

