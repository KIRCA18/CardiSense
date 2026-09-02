package mk.ukim.finki.backend.service.parser;

import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.service.archive.ArchiveExtractionService;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import mk.ukim.finki.backend.service.parser.dicom.DicomFormatAdapter;
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

    private static final int MAX_DICOM_RECORDS = 50;

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

    public ArchiveFormatAdapter(
            ArchiveExtractionService archiveExtractionService,
            WfdbDatasetParser wfdbDatasetParser,
            DicomFormatAdapter dicomFormatAdapter
    ) {
        this.archiveExtractionService = archiveExtractionService;
        this.wfdbDatasetParser = wfdbDatasetParser;
        this.dicomFormatAdapter = dicomFormatAdapter;
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
        boolean containsWfdb = files.keySet().stream()
                .anyMatch(this::isWfdb);

        if (containsDicom && containsWfdb) {
            throw new InvalidEcgException(
                    "Dataset archive cannot mix DICOM and WFDB files"
            );
        }

        if (containsDicom) {
            return prepareDicomDataset(files);
        }

        return new PreparedEcgUpload(
                EcgFormat.WFDB,
                filename,
                content,
                wfdbDatasetParser.parse(files)
        );
    }

    private PreparedEcgUpload prepareDicomDataset(
            Map<String, byte[]> files
    ) {
        if (files.size() > MAX_DICOM_RECORDS) {
            throw new InvalidEcgException(
                    "Dataset archive contains more than %d DICOM ECG records"
                            .formatted(MAX_DICOM_RECORDS)
            );
        }

        List<ParsedEcgRecord> records = new ArrayList<>();
        Map<String, byte[]> anonymizedFiles = new LinkedHashMap<>();
        Set<String> recordNames = new HashSet<>();

        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            if (file.getValue().length == 0) {
                throw new InvalidEcgException(
                        "DICOM file in dataset archive is empty: " + file.getKey()
                );
            }

            PreparedEcgUpload preparedDicom = dicomFormatAdapter.prepare(
                    file.getKey(),
                    file.getValue()
            );

            ParsedEcgRecord record = preparedDicom.records().get(0);
            String normalizedRecordName = record.recordName()
                    .toLowerCase(Locale.ROOT);

            if (!recordNames.add(normalizedRecordName)) {
                throw new InvalidEcgException(
                        "Dataset archive contains duplicate DICOM record ID: " +
                                record.recordName()
                );
            }

            if (anonymizedFiles.putIfAbsent(
                    preparedDicom.filenameForStorage(),
                    preparedDicom.contentForStorage()
            ) != null) {
                throw new InvalidEcgException(
                        "Dataset archive produces duplicate anonymized DICOM filenames"
                );
            }

            records.add(record);
        }

        String safeFilename = "dicom-dataset-" + UUID.randomUUID() + ".zip";

        return new PreparedEcgUpload(
                EcgFormat.DICOM,
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
                    "Could not create anonymized DICOM dataset archive",
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
}
