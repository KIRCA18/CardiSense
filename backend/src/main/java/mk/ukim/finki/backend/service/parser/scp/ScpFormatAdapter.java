package mk.ukim.finki.backend.service.parser.scp;

import com.pixelmed.dicom.BinaryInputStream;
import com.pixelmed.scpecg.SCPECG;
import com.pixelmed.scpecg.Section5Or6;
import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScpFormatAdapter implements EcgFormatAdapter {

    private static final List<String> REQUIRED_LEADS = List.of(
            "I", "II", "III", "aVR", "aVL", "aVF",
            "V1", "V2", "V3", "V4", "V5", "V6"
    );
    private static final int MAX_SAMPLES_PER_LEAD = 300_000;
    private static final double NANOVOLTS_PER_MILLIVOLT = 1_000_000.0;
    private static final Pattern STORED_SCP_FILENAME = Pattern.compile(
            "^(scp-[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})\\.scp$",
            Pattern.CASE_INSENSITIVE
    );

    private final ScpAnonymizationService anonymizationService;

    public ScpFormatAdapter(ScpAnonymizationService anonymizationService) {
        this.anonymizationService = anonymizationService;
    }

    @Override
    public boolean supports(String filename) {
        return filename != null &&
                filename.toLowerCase(Locale.ROOT).endsWith(".scp");
    }

    @Override
    public PreparedEcgUpload prepare(String filename, byte[] content) {
        byte[] anonymizedContent = anonymizationService.anonymize(content);
        String recordName = resolveRecordName(filename);

        ParsedEcgRecord record = new ParsedEcgRecord(
                recordName,
                parse(anonymizedContent)
        );

        return new PreparedEcgUpload(
                EcgFormat.SCP_ECG,
                recordName + ".scp",
                anonymizedContent,
                List.of(record)
        );
    }

    private String resolveRecordName(String filename) {
        String normalized = filename.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        Matcher matcher = STORED_SCP_FILENAME.matcher(fileName);

        if (matcher.matches()) {
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }

        return "scp-" + UUID.randomUUID();
    }

    private ParsedEcg parse(byte[] content) {
        try (
                BinaryInputStream input = new BinaryInputStream(
                        new ByteArrayInputStream(content),
                        false
                )
        ) {
            SCPECG scp = new SCPECG(input, false);
            Section5Or6 rhythmSection = scp.getSection6();
            if (rhythmSection == null) {
                throw new InvalidEcgException(
                        "SCP-ECG does not contain a supported section 6 rhythm waveform"
                );
            }

            String[] leadNameArray = scp.getLeadNames();
            long[] sampleCounts = scp.getNumbersOfSamples();
            short[][] decoded = scp.getDecompressedRhythmData();

            validateDimensions(scp, leadNameArray, sampleCounts, decoded);

            List<String> leadNames = Arrays.stream(leadNameArray)
                    .map(this::canonicalLeadName)
                    .toList();
            validateRequiredLeads(leadNames);

            int sampleIntervalMicroseconds =
                    scp.getDecompressedRhythmDataSampleTimeInterval();
            int amplitudeMultiplierNanovolts =
                    rhythmSection.getAmplitudeValueMultiplier();

            if (sampleIntervalMicroseconds <= 0) {
                throw new InvalidEcgException(
                        "SCP-ECG has an invalid rhythm sample interval"
                );
            }
            if (amplitudeMultiplierNanovolts <= 0) {
                throw new InvalidEcgException(
                        "SCP-ECG has an invalid amplitude multiplier"
                );
            }

            double[][] waveform = toMilliVolts(
                    decoded,
                    amplitudeMultiplierNanovolts
            );
            double samplingRateHz =
                    1_000_000.0 / sampleIntervalMicroseconds;

            Map<String, String> safeMetadata = Map.of(
                    "sourceFormat", "SCP_ECG",
                    "sampleIntervalMicroseconds",
                    Integer.toString(sampleIntervalMicroseconds),
                    "differenceDataUsed",
                    Integer.toString(rhythmSection.getDifferenceDataUsed()),
                    "referenceBeatUsed",
                    Boolean.toString(
                            scp.getSection3().getReferenceBeatUsedForCompression()
                    )
            );

            return new ParsedEcg(
                    waveform,
                    leadNames,
                    samplingRateHz,
                    leadNames.stream().map(ignored -> "mV").toList(),
                    safeMetadata
            );
        } catch (InvalidEcgException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new InvalidEcgException(
                    "Could not decode SCP-ECG waveform",
                    exception
            );
        }
    }

    private void validateDimensions(
            SCPECG scp,
            String[] leadNames,
            long[] sampleCounts,
            short[][] decoded
    ) {
        int leadCount = scp.getNumberOfLeads();
        if (leadCount != REQUIRED_LEADS.size() ||
                leadNames == null || leadNames.length != leadCount ||
                sampleCounts == null || sampleCounts.length != leadCount ||
                decoded == null || decoded.length != leadCount) {
            throw new InvalidEcgException(
                    "SCP-ECG must contain exactly 12 rhythm leads"
            );
        }

        long expectedSampleCount = sampleCounts[0];
        if (expectedSampleCount <= 0 ||
                expectedSampleCount > MAX_SAMPLES_PER_LEAD ||
                expectedSampleCount > Integer.MAX_VALUE) {
            throw new InvalidEcgException(
                    "SCP-ECG has an unsupported number of samples per lead"
            );
        }

        for (int leadIndex = 0; leadIndex < leadCount; leadIndex++) {
            if (sampleCounts[leadIndex] != expectedSampleCount ||
                    decoded[leadIndex] == null ||
                    decoded[leadIndex].length != (int) expectedSampleCount) {
                throw new InvalidEcgException(
                        "SCP-ECG leads must have equal sample counts"
                );
            }
        }
    }

    private void validateRequiredLeads(List<String> leadNames) {
        Set<String> uniqueLeads = new HashSet<>(leadNames);
        if (uniqueLeads.size() != leadNames.size()) {
            throw new InvalidEcgException("SCP-ECG contains duplicate lead names");
        }
        if (!uniqueLeads.containsAll(REQUIRED_LEADS)) {
            throw new InvalidEcgException(
                    "SCP-ECG does not contain the 12 standard leads required by ECG-FM"
            );
        }
    }

    private String canonicalLeadName(String sourceName) {
        if (sourceName == null) {
            throw new InvalidEcgException("SCP-ECG contains an unnamed lead");
        }

        return switch (sourceName.trim().toUpperCase(Locale.ROOT)) {
            case "I" -> "I";
            case "II" -> "II";
            case "III" -> "III";
            case "AVR" -> "aVR";
            case "AVL" -> "aVL";
            case "AVF" -> "aVF";
            case "V1" -> "V1";
            case "V2" -> "V2";
            case "V3" -> "V3";
            case "V4" -> "V4";
            case "V5" -> "V5";
            case "V6" -> "V6";
            default -> throw new InvalidEcgException(
                    "Unsupported SCP-ECG lead: " + sourceName
            );
        };
    }

    private double[][] toMilliVolts(
            short[][] decoded,
            int amplitudeMultiplierNanovolts
    ) {
        double scale = amplitudeMultiplierNanovolts /
                NANOVOLTS_PER_MILLIVOLT;
        double[][] waveform = new double[decoded.length][];

        for (int leadIndex = 0; leadIndex < decoded.length; leadIndex++) {
            waveform[leadIndex] = new double[decoded[leadIndex].length];
            for (int sampleIndex = 0;
                 sampleIndex < decoded[leadIndex].length;
                 sampleIndex++) {
                waveform[leadIndex][sampleIndex] =
                        decoded[leadIndex][sampleIndex] * scale;
            }
        }

        return waveform;
    }
}
