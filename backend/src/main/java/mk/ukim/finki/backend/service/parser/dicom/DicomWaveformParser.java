package mk.ukim.finki.backend.service.parser.dicom;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class DicomWaveformParser {

    private static final List<String> STANDARD_LEADS = List.of(
            "I",
            "II",
            "III",
            "aVR",
            "aVL",
            "aVF",
            "V1",
            "V2",
            "V3",
            "V4",
            "V5",
            "V6"
    );

    private static final Set<String> SUPPORTED_SOP_CLASSES = Set.of(
            UID.TwelveLeadECGWaveformStorage,
            UID.GeneralECGWaveformStorage,
            UID.AmbulatoryECGWaveformStorage
    );

    public ParsedEcgRecord parse(Attributes dataset) {
        validateSopClass(dataset);

        Attributes multiplexGroup =
                selectLongestTwelveLeadGroup(dataset);

        ParsedEcg parsedEcg = decode(
                dataset,
                multiplexGroup
        );

        String recordName = dataset.getString(
                Tag.SOPInstanceUID
        );

        if (recordName == null || recordName.isBlank()) {
            throw new InvalidEcgException(
                    "DICOM ECG does not contain an SOP Instance UID"
            );
        }

        return new ParsedEcgRecord(
                recordName,
                parsedEcg
        );
    }

    private void validateSopClass(Attributes dataset) {
        String sopClassUid = dataset.getString(
                Tag.SOPClassUID
        );

        if (!SUPPORTED_SOP_CLASSES.contains(sopClassUid)) {
            throw new InvalidEcgException(
                    "Uploaded DICOM object is not a supported ECG waveform"
            );
        }
    }

    private Attributes selectLongestTwelveLeadGroup(
            Attributes dataset
    ) {
        Sequence waveformSequence = dataset.getSequence(
                Tag.WaveformSequence
        );

        if (waveformSequence == null ||
                waveformSequence.isEmpty()) {
            throw new InvalidEcgException(
                    "DICOM object does not contain waveform data"
            );
        }

        return waveformSequence.stream()
                .filter(group ->
                        group.getInt(
                                Tag.NumberOfWaveformChannels,
                                0
                        ) == STANDARD_LEADS.size()
                )
                .max(Comparator.comparingDouble(
                        this::durationSeconds
                ))
                .orElseThrow(() ->
                        new InvalidEcgException(
                                "DICOM object does not contain a 12-lead waveform group"
                        )
                );
    }

    private double durationSeconds(Attributes group) {
        int sampleCount = group.getInt(
                Tag.NumberOfWaveformSamples,
                0
        );

        double samplingRate = group.getDouble(
                Tag.SamplingFrequency,
                0
        );

        if (samplingRate <= 0) {
            return 0;
        }

        return sampleCount / samplingRate;
    }

    private ParsedEcg decode(
            Attributes dataset,
            Attributes group
    ) {
        int channelCount = group.getInt(
                Tag.NumberOfWaveformChannels,
                0
        );

        int sampleCount = group.getInt(
                Tag.NumberOfWaveformSamples,
                0
        );

        double samplingRateHz = group.getDouble(
                Tag.SamplingFrequency,
                0
        );

        int bitsAllocated = group.getInt(
                Tag.WaveformBitsAllocated,
                0
        );

        String sampleInterpretation = group.getString(
                Tag.WaveformSampleInterpretation
        );

        if (channelCount != STANDARD_LEADS.size()) {
            throw new InvalidEcgException(
                    "DICOM waveform must contain 12 channels"
            );
        }

        if (sampleCount <= 0 || samplingRateHz <= 0) {
            throw new InvalidEcgException(
                    "DICOM waveform has invalid dimensions or sampling rate"
            );
        }

        if (bitsAllocated != 16 ||
                !"SS".equals(sampleInterpretation)) {
            throw new InvalidEcgException(
                    "We currently support only signed 16-bit DICOM waveform samples"
            );
        }

        Sequence channelDefinitions = group.getSequence(
                Tag.ChannelDefinitionSequence
        );

        if (channelDefinitions == null ||
                channelDefinitions.size() != channelCount) {
            throw new InvalidEcgException(
                    "DICOM channel definitions do not match the channel count"
            );
        }

        byte[] waveformData;

        try {
            waveformData = group.getBytes(
                    Tag.WaveformData
            );
        } catch (IOException exception) {
            throw new InvalidEcgException(
                    "Could not read DICOM waveform data",
                    exception
            );
        }

        int expectedBytes =
                sampleCount * channelCount * Short.BYTES;

        if (waveformData == null ||
                waveformData.length != expectedBytes) {
            throw new InvalidEcgException(
                    "DICOM waveform data length does not match its dimensions"
            );
        }

        ChannelInformation[] channels =
                readChannelInformation(channelDefinitions);

        ByteBuffer buffer = ByteBuffer
                .wrap(waveformData)
                .order(
                        group.bigEndian()
                                ? ByteOrder.BIG_ENDIAN
                                : ByteOrder.LITTLE_ENDIAN
                );

        double[][] waveform = new double[
                STANDARD_LEADS.size()
                ][sampleCount];

        for (int sampleIndex = 0;
             sampleIndex < sampleCount;
             sampleIndex++) {

            for (int sourceChannelIndex = 0;
                 sourceChannelIndex < channelCount;
                 sourceChannelIndex++) {

                short rawSample = buffer.getShort();

                ChannelInformation channel =
                        channels[sourceChannelIndex];

                double physicalValue =
                        rawSample
                                * channel.sensitivity()
                                * channel.correctionFactor()
                                + channel.baseline();

                double milliVolts = toMilliVolts(
                        physicalValue,
                        channel.unit()
                );

                waveform[channel.targetLeadIndex()][sampleIndex] =
                        milliVolts;
            }
        }

        String multiplexLabel = group.getString(
                Tag.MultiplexGroupLabel,
                ""
        );

        Map<String, String> safeMetadata = Map.of(
                "sourceFormat",
                "DICOM",
                "sopClassUid",
                dataset.getString(Tag.SOPClassUID, ""),
                "multiplexGroup",
                multiplexLabel
        );

        return new ParsedEcg(
                waveform,
                STANDARD_LEADS,
                samplingRateHz,
                STANDARD_LEADS.stream()
                        .map(ignored -> "mV")
                        .toList(),
                safeMetadata
        );
    }

    private ChannelInformation[] readChannelInformation(
            Sequence channelDefinitions
    ) {
        ChannelInformation[] result =
                new ChannelInformation[
                        channelDefinitions.size()
                        ];

        Set<String> foundLeads = new HashSet<>();

        for (int sourceIndex = 0;
             sourceIndex < channelDefinitions.size();
             sourceIndex++) {

            Attributes channel =
                    channelDefinitions.get(sourceIndex);

            String lead = readLeadName(channel);

            if (!foundLeads.add(lead)) {
                throw new InvalidEcgException(
                        "DICOM waveform contains duplicate lead: " +
                                lead
                );
            }

            int targetIndex =
                    STANDARD_LEADS.indexOf(lead);

            if (targetIndex < 0) {
                throw new InvalidEcgException(
                        "Unsupported DICOM ECG lead: " + lead
                );
            }

            double sensitivity = channel.getDouble(
                    Tag.ChannelSensitivity,
                    Double.NaN
            );

            if (!Double.isFinite(sensitivity)) {
                throw new InvalidEcgException(
                        "DICOM lead " + lead +
                                " does not define channel sensitivity"
                );
            }

            double correctionFactor = channel.getDouble(
                    Tag.ChannelSensitivityCorrectionFactor,
                    1.0
            );

            double baseline = channel.getDouble(
                    Tag.ChannelBaseline,
                    0.0
            );

            String unit = readUnit(channel);

            result[sourceIndex] = new ChannelInformation(
                    targetIndex,
                    sensitivity,
                    correctionFactor,
                    baseline,
                    unit
            );
        }

        if (foundLeads.size() != STANDARD_LEADS.size()) {
            throw new InvalidEcgException(
                    "DICOM waveform does not contain all required ECG leads"
            );
        }

        return result;
    }

    private String readLeadName(Attributes channel) {
        Attributes source = channel.getNestedDataset(
                Tag.ChannelSourceSequence
        );

        String rawName = source == null
                ? null
                : source.getString(Tag.CodeMeaning);

        if (rawName == null || rawName.isBlank()) {
            rawName = channel.getString(
                    Tag.ChannelLabel
            );
        }

        if (rawName == null || rawName.isBlank()) {
            throw new InvalidEcgException(
                    "DICOM waveform contains an unnamed channel"
            );
        }

        String normalized = rawName
                .toUpperCase(Locale.ROOT)
                .replace("LEAD", "")
                .replace("(EINTHOVEN)", "")
                .replaceAll("[^A-Z0-9]", "");

        return switch (normalized) {
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
                    "Could not map DICOM channel to a standard ECG lead: " +
                            rawName
            );
        };
    }

    private String readUnit(Attributes channel) {
        Attributes unitItem = channel.getNestedDataset(
                Tag.ChannelSensitivityUnitsSequence
        );

        if (unitItem == null) {
            throw new InvalidEcgException(
                    "DICOM ECG channel does not define a physical unit"
            );
        }

        String codeValue = unitItem.getString(
                Tag.CodeValue
        );

        String codeMeaning = unitItem.getString(
                Tag.CodeMeaning
        );

        if (codeValue != null && !codeValue.isBlank()) {
            return codeValue;
        }

        if (codeMeaning != null &&
                !codeMeaning.isBlank()) {
            return codeMeaning;
        }

        throw new InvalidEcgException(
                "DICOM ECG channel contains an empty physical unit"
        );
    }

    private double toMilliVolts(
            double value,
            String unit
    ) {
        String normalized = unit
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("μ", "u")
                .replace("µ", "u");

        return switch (normalized) {
            case "uv", "microvolt", "microvolts" ->
                    value / 1000.0;

            case "mv", "millivolt", "millivolts" ->
                    value;

            case "v", "volt", "volts" ->
                    value * 1000.0;

            default -> throw new InvalidEcgException(
                    "Unsupported DICOM ECG unit: " + unit
            );
        };
    }

    private record ChannelInformation(
            int targetLeadIndex,
            double sensitivity,
            double correctionFactor,
            double baseline,
            String unit
    ) {
    }
}