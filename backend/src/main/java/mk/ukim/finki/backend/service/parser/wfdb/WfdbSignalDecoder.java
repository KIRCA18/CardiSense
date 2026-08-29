package mk.ukim.finki.backend.service.parser.wfdb;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

@Component
class WfdbSignalDecoder {

    ParsedEcg decode(WfdbHeader header, byte[] signalFile) {
        validateSupportedFormat(header);

        int channelCount = header.signalCount();
        int sampleCount = header.sampleCount();
        int byteOffset = header.signals().get(0).byteOffset();

        long requiredBytes =
                (long) byteOffset +
                        (long) sampleCount * channelCount * Short.BYTES;

        if (signalFile.length < requiredBytes) {
            throw new InvalidEcgException(
                    "Signal file is too short: expected at least %d bytes, got %d"
                            .formatted(requiredBytes, signalFile.length)
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(signalFile)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.position(byteOffset);

        double[][] waveform = new double[channelCount][sampleCount];

        for (int sample = 0; sample < sampleCount; sample++) {
            for (int channel = 0; channel < channelCount; channel++) {
                short digitalValue = buffer.getShort();
                WfdbSignalSpec spec = header.signals().get(channel);

                waveform[channel][sample] =
                        (digitalValue - spec.baseline()) / spec.gain();
            }
        }

        List<String> leadNames = header.signals().stream()
                .map(WfdbSignalSpec::leadName)
                .toList();

        List<String> units = header.signals().stream()
                .map(WfdbSignalSpec::unit)
                .toList();

        return new ParsedEcg(
                waveform,
                leadNames,
                header.samplingRateHz(),
                units,
                header.metadata()
        );
    }

    private void validateSupportedFormat(WfdbHeader header) {
        if (header.signalCount() <= 0) {
            throw new InvalidEcgException("WFDB record contains no signals");
        }

        long signalFileCount = header.signals().stream()
                .map(WfdbSignalSpec::fileName)
                .distinct()
                .count();

        if (signalFileCount != 1) {
            throw new InvalidEcgException(
                    "Prototype supports one shared WFDB signal file"
            );
        }

        boolean unsupportedFormat = header.signals().stream()
                .anyMatch(signal -> signal.format() != 16);

        if (unsupportedFormat) {
            throw new InvalidEcgException(
                    "Prototype supports only WFDB format 16"
            );
        }

        long offsets = header.signals().stream()
                .map(WfdbSignalSpec::byteOffset)
                .distinct()
                .count();

        if (offsets != 1) {
            throw new InvalidEcgException(
                    "All channels must use the same byte offset"
            );
        }

        boolean invalidGain = header.signals().stream()
                .anyMatch(signal -> signal.gain() <= 0);

        if (invalidGain) {
            throw new InvalidEcgException(
                    "Every WFDB signal must have a positive gain"
            );
        }
    }
}
