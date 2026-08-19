package mk.ukim.finki.backend.service.wfdb;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class WfdbHeaderParser {

    private static final Pattern FORMAT_PATTERN = Pattern.compile(
            "^(?<format>\\d+)(?:x\\d+)?(?:\\+(?<offset>\\d+))?.*$"
    );

    private static final Pattern GAIN_PATTERN = Pattern.compile(
            "^(?<gain>\\d+(?:\\.\\d+)?)" +
                    "(?:\\((?<baseline>-?\\d+)\\))?" +
                    "/(?<unit>\\S+)$"
    );

    WfdbHeader parse(String headerText) {
        List<String> lines = headerText.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.isEmpty()) {
            throw new InvalidEcgException("WFDB header is empty");
        }

        String[] recordFields = split(lines.get(0));

        if (recordFields.length < 4) {
            throw new InvalidEcgException(
                    "WFDB record line must contain record name, signal count, " +
                            "sampling rate, and sample count"
            );
        }

        String recordName = recordFields[0];
        int signalCount = parsePositiveInt(recordFields[1], "signal count");
        double samplingRate = parseSamplingRate(recordFields[2]);
        int sampleCount = parsePositiveInt(recordFields[3], "sample count");

        List<String> signalLines = lines.stream()
                .skip(1)
                .filter(line -> !line.startsWith("#"))
                .limit(signalCount)
                .toList();

        if (signalLines.size() != signalCount) {
            throw new InvalidEcgException(
                    "Header declares %d signals but contains %d signal lines"
                            .formatted(signalCount, signalLines.size())
            );
        }

        List<WfdbSignalSpec> signals = signalLines.stream()
                .map(this::parseSignal)
                .toList();

        Map<String, String> metadata = parseMetadata(lines);

        return new WfdbHeader(
                recordName,
                signalCount,
                samplingRate,
                sampleCount,
                signals,
                metadata
        );
    }

    private WfdbSignalSpec parseSignal(String line) {
        String[] fields = split(line);

        if (fields.length < 9) {
            throw new InvalidEcgException(
                    "Unsupported WFDB signal line: " + line
            );
        }

        Matcher formatMatcher = FORMAT_PATTERN.matcher(fields[1]);
        if (!formatMatcher.matches()) {
            throw new InvalidEcgException(
                    "Invalid WFDB format field: " + fields[1]
            );
        }

        int format = Integer.parseInt(formatMatcher.group("format"));
        int byteOffset = formatMatcher.group("offset") == null
                ? 0
                : Integer.parseInt(formatMatcher.group("offset"));

        Matcher gainMatcher = GAIN_PATTERN.matcher(fields[2]);
        if (!gainMatcher.matches()) {
            throw new InvalidEcgException(
                    "Unsupported WFDB gain field: " + fields[2]
            );
        }

        double gain = Double.parseDouble(gainMatcher.group("gain"));
        int adcZero = Integer.parseInt(fields[4]);

        int baseline = gainMatcher.group("baseline") == null
                ? adcZero
                : Integer.parseInt(gainMatcher.group("baseline"));

        String unit = gainMatcher.group("unit");
        String leadName = fields[fields.length - 1];

        return new WfdbSignalSpec(
                fields[0],
                format,
                byteOffset,
                gain,
                baseline,
                unit,
                leadName
        );
    }

    private Map<String, String> parseMetadata(List<String> lines) {
        Map<String, String> metadata = new LinkedHashMap<>();

        for (String line : lines) {
            if (!line.startsWith("#")) {
                continue;
            }

            String comment = line.substring(1).trim();
            int separator = comment.indexOf(':');

            if (separator <= 0) {
                continue;
            }

            String key = comment.substring(0, separator).trim();
            String value = comment.substring(separator + 1).trim();

            metadata.put(key, value);
        }

        return Map.copyOf(metadata);
    }

    private double parseSamplingRate(String value) {
        String basicValue = value.split("[/(]", 2)[0];

        try {
            double samplingRate = Double.parseDouble(basicValue);
            if (samplingRate <= 0) {
                throw new NumberFormatException();
            }
            return samplingRate;
        } catch (NumberFormatException exception) {
            throw new InvalidEcgException(
                    "Invalid sampling rate: " + value,
                    exception
            );
        }
    }

    private int parsePositiveInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new InvalidEcgException(
                    "Invalid " + fieldName + ": " + value,
                    exception
            );
        }
    }

    private String[] split(String line) {
        return line.trim().split("\\s+");
    }
}
