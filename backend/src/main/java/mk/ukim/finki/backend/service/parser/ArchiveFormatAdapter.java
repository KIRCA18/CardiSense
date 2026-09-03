package mk.ukim.finki.backend.service.parser;

import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.service.archive.ArchiveExtractionService;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import mk.ukim.finki.backend.service.parser.dicom.DicomFormatAdapter;
import mk.ukim.finki.backend.service.parser.scp.ScpFormatAdapter;
import mk.ukim.finki.backend.service.parser.wfdb.WfdbDatasetParser;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ArchiveFormatAdapter implements EcgFormatAdapter {

    private static final int MAX_SINGLE_FILE_RECORDS = 50;

    private static final List<String> SUPPORTED_SUFFIXES = List.of(
            ".zip",
            ".rar",
            ".7z",
            ".tar",
            ".tar.gz",
            ".tgz"
    );

    private final ArchiveExtractionService archiveExtractionService;
    private final WfdbDatasetParser wfdbDatasetParser;
    private final DicomFormatAdapter dicomFormatAdapter;
    private final ScpFormatAdapter scpFormatAdapter;

    public ArchiveFormatAdapter(
            ArchiveExtractionService archiveExtractionService,
            WfdbDatasetParser wfdbDatasetParser,
            DicomFormatAdapter dicomFormatAdapter,
            ScpFormatAdapter scpFormatAdapter
    ) {
        this.archiveExtractionService = archiveExtractionService;
        this.wfdbDatasetParser = wfdbDatasetParser;
        this.dicomFormatAdapter = dicomFormatAdapter;
        this.scpFormatAdapter = scpFormatAdapter;
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }

        String normalized = filename.toLowerCase(Locale.ROOT);
        return SUPPORTED_SUFFIXES.stream().anyMatch(normalized::endsWith);
    }

    @Override
    public PreparedEcgUpload prepare(String filename, byte[] content) {
        Map<String, byte[]> files = archiveExtractionService.extract(
                filename,
                content
        );

        boolean containsDicom = files.keySet().stream()
                .anyMatch(this::isDicom);
        boolean containsScp = files.keySet().stream()
                .anyMatch(this::isScp);
        boolean containsWfdb = files.keySet().stream()
                .anyMatch(this::isWfdb);

        int formatCount = (containsDicom ? 1 : 0) +
                (containsScp ? 1 : 0) +
                (containsWfdb ? 1 : 0);
        if (formatCount > 1) {
            throw new InvalidEcgException(
                    "Dataset archive cannot mix DICOM, SCP-ECG and WFDB files"
            );
        }

        if (containsDicom) {
            return prepareSingleFileDataset(
                    files,
                    dicomFormatAdapter,
                    EcgFormat.DICOM,
                    "DICOM",
                    "dicom"
            );
        }

        if (containsScp) {
            return prepareSingleFileDataset(
                    files,
                    scpFormatAdapter,
                    EcgFormat.SCP_ECG,
                    "SCP-ECG",
                    "scp"
            );
        }

        return new PreparedEcgUpload(
                EcgFormat.WFDB,
                filename,
                content,
                wfdbDatasetParser.parse(files)
        );
    }

    private PreparedEcgUpload prepareSingleFileDataset(
            Map<String, byte[]> files,
            EcgFormatAdapter adapter,
            EcgFormat format,
            String formatLabel,
            String filenamePrefix
    ) {
        if (files.size() > MAX_SINGLE_FILE_RECORDS) {
            throw new InvalidEcgException(
                    "Dataset archive contains more than %d %s records"
                            .formatted(MAX_SINGLE_FILE_RECORDS, formatLabel)
            );
        }

        List<ParsedEcgRecord> records = new ArrayList<>();
        Map<String, byte[]> anonymizedFiles = new LinkedHashMap<>();
        Set<String> recordNames = new HashSet<>();

        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            if (file.getValue().length == 0) {
                throw new InvalidEcgException(
                        "%s file in dataset archive is empty: %s"
                                .formatted(formatLabel, file.getKey())
                );
            }

            PreparedEcgUpload preparedFile = adapter.prepare(
                    file.getKey(),
                    file.getValue()
            );

            if (preparedFile.records().size() != 1) {
                throw new IllegalStateException(
                        formatLabel + " adapter must return exactly one record"
                );
            }

            ParsedEcgRecord record = preparedFile.records().get(0);
            String normalizedRecordName = record.recordName()
                    .toLowerCase(Locale.ROOT);

            if (!recordNames.add(normalizedRecordName)) {
                throw new InvalidEcgException(
                        "Dataset archive contains duplicate %s record ID: %s"
                                .formatted(formatLabel, record.recordName())
                );
            }

            if (anonymizedFiles.putIfAbsent(
                    preparedFile.filenameForStorage(),
                    preparedFile.contentForStorage()
            ) != null) {
                throw new InvalidEcgException(
                        "Dataset archive produces duplicate anonymized %s filenames"
                                .formatted(formatLabel)
                );
            }

            records.add(record);
        }

        String safeFilename = filenamePrefix + "-dataset-" +
                UUID.randomUUID() + ".zip";

        return new PreparedEcgUpload(
                format,
                safeFilename,
                createZip(anonymizedFiles),
                List.copyOf(records)
        );
    }

    private byte[] createZip(Map<String, byte[]> files) {
        try (
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes)
        ) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(0L);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }

            zip.finish();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new InvalidEcgException(
                    "Could not create anonymized ECG dataset archive",
                    exception
            );
        }
    }

    private boolean isDicom(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".dcm");
    }

    private boolean isWfdb(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".hea") || normalized.endsWith(".mat");
    }

    private boolean isScp(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".scp");
    }
}
