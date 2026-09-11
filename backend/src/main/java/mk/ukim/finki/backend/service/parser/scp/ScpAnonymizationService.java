package mk.ukim.finki.backend.service.parser.scp;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class ScpAnonymizationService {

    private static final int RECORD_HEADER_SIZE = 6;
    private static final int SECTION_HEADER_SIZE = 16;
    private static final int SECTION_POINTER_SIZE = 10;

    private static final Set<Integer> REQUIRED_SECTIONS = Set.of(0, 1, 3, 6);
    private static final Set<Integer> REDACTED_TEXT_TAGS = Set.of(
            0, 1, 2, 3,
            13,
            16, 17, 18, 19, 20, 21, 22, 23,
            30, 31, 35
    );
    private static final Set<Integer> DEVICE_ID_TAGS = Set.of(14, 15);

    public byte[] anonymize(byte[] originalContent) {
        if (originalContent == null ||
                originalContent.length < RECORD_HEADER_SIZE + SECTION_HEADER_SIZE) {
            throw new InvalidEcgException("SCP-ECG file is too short");
        }

        byte[] anonymized = originalContent.clone();
        Map<Integer, SectionLocation> sections = validateRecord(anonymized);

        anonymizeSectionOne(anonymized, sections.get(1));
        writeUnsignedShort(
                anonymized,
                sections.get(1).offset(),
                crc(anonymized, sections.get(1).offset() + 2, sections.get(1).length() - 2)
        );
        writeUnsignedShort(
                anonymized,
                0,
                crc(anonymized, 2, anonymized.length - 2)
        );

        validateRecord(anonymized);
        return anonymized;
    }

    private Map<Integer, SectionLocation> validateRecord(byte[] content) {
        long declaredLength = readUnsignedInt(content, 2);
        if (declaredLength != content.length) {
            throw new InvalidEcgException(
                    "SCP-ECG record length does not match the uploaded file"
            );
        }

        validateCrc(content, 0, 2, content.length - 2, "record");

        Map<Integer, SectionLocation> sections = new HashMap<>();
        int offset = RECORD_HEADER_SIZE;

        while (offset < content.length) {
            if (content.length - offset < SECTION_HEADER_SIZE) {
                throw new InvalidEcgException("SCP-ECG contains a truncated section header");
            }

            int sectionId = readUnsignedShort(content, offset + 2);
            long sectionLengthValue = readUnsignedInt(content, offset + 4);
            if (sectionLengthValue < SECTION_HEADER_SIZE ||
                    sectionLengthValue > Integer.MAX_VALUE) {
                throw new InvalidEcgException(
                        "SCP-ECG section %d has an invalid length".formatted(sectionId)
                );
            }

            int sectionLength = (int) sectionLengthValue;
            if ((sectionLength & 1) != 0 || sectionLength > content.length - offset) {
                throw new InvalidEcgException(
                        "SCP-ECG section %d is malformed or truncated".formatted(sectionId)
                );
            }

            validateCrc(
                    content,
                    offset,
                    offset + 2,
                    sectionLength - 2,
                    "section " + sectionId
            );

            if (sections.put(sectionId, new SectionLocation(offset, sectionLength)) != null) {
                throw new InvalidEcgException(
                        "SCP-ECG contains duplicate section " + sectionId
                );
            }

            offset += sectionLength;
        }

        if (offset != content.length) {
            throw new InvalidEcgException("SCP-ECG section lengths are inconsistent");
        }

        Set<Integer> missingSections = new HashSet<>(REQUIRED_SECTIONS);
        missingSections.removeAll(sections.keySet());
        if (!missingSections.isEmpty()) {
            throw new InvalidEcgException(
                    "SCP-ECG is missing required sections: " + missingSections
            );
        }

        if (sections.get(0).offset() != RECORD_HEADER_SIZE) {
            throw new InvalidEcgException("SCP-ECG section 0 must be the first section");
        }

        validateSectionPointers(content, sections);
        return sections;
    }

    private void validateSectionPointers(
            byte[] content,
            Map<Integer, SectionLocation> sections
    ) {
        SectionLocation sectionZero = sections.get(0);
        int pointerBytes = sectionZero.length() - SECTION_HEADER_SIZE;
        if (pointerBytes % SECTION_POINTER_SIZE != 0) {
            throw new InvalidEcgException("SCP-ECG section 0 has an invalid pointer table");
        }

        int end = sectionZero.offset() + sectionZero.length();
        for (int cursor = sectionZero.offset() + SECTION_HEADER_SIZE;
             cursor < end;
             cursor += SECTION_POINTER_SIZE) {
            int sectionId = readUnsignedShort(content, cursor);
            long sectionLength = readUnsignedInt(content, cursor + 2);
            long sectionIndex = readUnsignedInt(content, cursor + 6);

            if (sectionLength == 0 && sectionIndex == 0) {
                continue;
            }
            if (sectionLength == 0 || sectionIndex == 0 || sectionIndex > Integer.MAX_VALUE) {
                throw new InvalidEcgException("SCP-ECG section 0 contains an invalid pointer");
            }

            SectionLocation actual = sections.get(sectionId);
            if (actual == null ||
                    sectionLength != actual.length() ||
                    sectionIndex - 1 != actual.offset()) {
                throw new InvalidEcgException(
                        "SCP-ECG section 0 contains an inconsistent pointer to section " +
                                sectionId
                );
            }
        }
    }

    private void anonymizeSectionOne(
            byte[] content,
            SectionLocation sectionOne
    ) {
        int cursor = sectionOne.offset() + SECTION_HEADER_SIZE;
        int end = sectionOne.offset() + sectionOne.length();
        boolean terminatorFound = false;

        while (cursor < end) {
            int tag = Byte.toUnsignedInt(content[cursor]);
            if (tag == 255) {
                terminatorFound = true;
                break;
            }

            if (end - cursor < 3) {
                throw new InvalidEcgException("SCP-ECG section 1 has a truncated field");
            }

            int valueLength = readUnsignedShort(content, cursor + 1);
            int valueOffset = cursor + 3;
            if (valueLength > end - valueOffset) {
                throw new InvalidEcgException(
                        "SCP-ECG section 1 field %d exceeds the section boundary"
                                .formatted(tag)
                );
            }

            if (REDACTED_TEXT_TAGS.contains(tag)) {
                redactText(content, valueOffset, valueLength);
            } else if (DEVICE_ID_TAGS.contains(tag)) {
                redactDeviceIdentification(content, valueOffset, valueLength);
            } else if (tag == 4) {
                capAge(content, valueOffset, valueLength);
            } else if (tag == 5) {
                replaceDate(content, valueOffset, valueLength, 1900);
            } else if (tag == 25) {
                replaceDate(content, valueOffset, valueLength, 2000);
            } else if (tag == 26) {
                replaceTime(content, valueOffset, valueLength);
            }

            cursor = valueOffset + valueLength;
        }

        if (!terminatorFound) {
            throw new InvalidEcgException("SCP-ECG section 1 terminator is missing");
        }
    }

    private void redactText(byte[] content, int offset, int length) {
        Arrays.fill(content, offset, offset + length, (byte) 0);
        if (length > 1) {
            Arrays.fill(content, offset, offset + length - 1, (byte) 'X');
        }
    }

    private void redactDeviceIdentification(byte[] content, int offset, int length) {
        Arrays.fill(content, offset, offset + Math.min(length, 6), (byte) 0);
        if (length > 36) {
            Arrays.fill(content, offset + 36, offset + length, (byte) 0);
        }
    }

    private void capAge(byte[] content, int offset, int length) {
        if (length != 3) {
            throw new InvalidEcgException("SCP-ECG age field has an invalid length");
        }
        if (readUnsignedShort(content, offset) > 90) {
            writeUnsignedShort(content, offset, 90);
        }
    }

    private void replaceDate(byte[] content, int offset, int length, int year) {
        if (length != 4) {
            throw new InvalidEcgException("SCP-ECG date field has an invalid length");
        }
        writeUnsignedShort(content, offset, year);
        content[offset + 2] = 1;
        content[offset + 3] = 1;
    }

    private void replaceTime(byte[] content, int offset, int length) {
        if (length != 3) {
            throw new InvalidEcgException("SCP-ECG time field has an invalid length");
        }
        Arrays.fill(content, offset, offset + length, (byte) 0);
    }

    private void validateCrc(
            byte[] content,
            int storedCrcOffset,
            int dataOffset,
            int dataLength,
            String description
    ) {
        int stored = readUnsignedShort(content, storedCrcOffset);
        int calculated = crc(content, dataOffset, dataLength);
        if (stored != calculated) {
            throw new InvalidEcgException(
                    "SCP-ECG %s CRC is invalid".formatted(description)
            );
        }
    }

    private int crc(byte[] content, int offset, int length) {
        int crc = 0xffff;
        for (int index = offset; index < offset + length; index++) {
            crc ^= Byte.toUnsignedInt(content[index]) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0
                        ? (crc << 1) ^ 0x1021
                        : crc << 1;
                crc &= 0xffff;
            }
        }
        return crc;
    }

    private int readUnsignedShort(byte[] content, int offset) {
        return Byte.toUnsignedInt(content[offset]) |
                (Byte.toUnsignedInt(content[offset + 1]) << 8);
    }

    private long readUnsignedInt(byte[] content, int offset) {
        return Integer.toUnsignedLong(
                Byte.toUnsignedInt(content[offset]) |
                        (Byte.toUnsignedInt(content[offset + 1]) << 8) |
                        (Byte.toUnsignedInt(content[offset + 2]) << 16) |
                        (Byte.toUnsignedInt(content[offset + 3]) << 24)
        );
    }

    private void writeUnsignedShort(byte[] content, int offset, int value) {
        content[offset] = (byte) value;
        content[offset + 1] = (byte) (value >>> 8);
    }

    private record SectionLocation(int offset, int length) {
    }
}
