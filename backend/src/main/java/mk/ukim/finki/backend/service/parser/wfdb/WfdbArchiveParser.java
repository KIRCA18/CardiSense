package mk.ukim.finki.backend.service.parser.wfdb;

import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.service.archive.ArchiveExtractionService;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class WfdbArchiveParser implements EcgFormatAdapter {

    private static final int MAX_RECORDS = 50;

    private final ArchiveExtractionService archiveExtractionService;
    private final WfdbHeaderParser headerParser;
    private final WfdbSignalDecoder signalDecoder;


    private static final List<String> SUPPORTED_SUFFIXES = List.of(
            ".zip",
            ".rar",
            ".7z",
            ".tar",
            ".tar.gz",
            ".tgz"
    );

    public WfdbArchiveParser(
            ArchiveExtractionService archiveExtractionService,
            WfdbHeaderParser headerParser,
            WfdbSignalDecoder signalDecoder
    ) {
        this.archiveExtractionService = archiveExtractionService;
        this.headerParser = headerParser;
        this.signalDecoder = signalDecoder;
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }

        String normalized = filename.toLowerCase(Locale.ROOT);

        return SUPPORTED_SUFFIXES.stream()
                .anyMatch(normalized::endsWith);
    }

    @Override
    public EcgFormat format() {
        return EcgFormat.WFDB;
    }

    @Override
    public PreparedEcgUpload prepare(
            String filename,
            byte[] content
    ) {
        Map<String, byte[]> files = archiveExtractionService.extract(
                filename,
                content
        );

        List<ParsedEcgRecord> records = parseFiles(files);

        return new PreparedEcgUpload(
                EcgFormat.WFDB,
                filename,
                content,
                records
        );
    }


    private List<ParsedEcgRecord> parseFiles(
            Map<String, byte[]> files
    ) {
        List<String> headerFiles = files.keySet().stream()
                .filter(name ->
                        name.toLowerCase(Locale.ROOT).endsWith(".hea")
                )
                .toList();

        if (headerFiles.isEmpty()) {
            throw new InvalidEcgException(
                    "Dataset archive must contain at least one .hea file"
            );
        }

        if (headerFiles.size() > MAX_RECORDS) {
            throw new InvalidEcgException(
                    "Dataset archive contains more than %d ECG records"
                            .formatted(MAX_RECORDS)
            );
        }

        Set<String> recordNames = new HashSet<>();

        return headerFiles.stream()
                .map(headerFileName -> parseRecord(
                        headerFileName,
                        files,
                        recordNames
                ))
                .toList();
    }

    private ParsedEcgRecord parseRecord(
            String headerFileName,
            Map<String, byte[]> files,
            Set<String> recordNames
    ) {
        String headerText = new String(
                files.get(headerFileName),
                StandardCharsets.US_ASCII
        );

        WfdbHeader header = headerParser.parse(headerText);

        if (!recordNames.add(header.recordName().toLowerCase(Locale.ROOT))) {
            throw new InvalidEcgException(
                    "Dataset archive contains duplicate WFDB record name: " +
                            header.recordName()
            );
        }

        List<String> referencedFiles = header.signals().stream()
                .map(WfdbSignalSpec::fileName)
                .distinct()
                .toList();

        if (referencedFiles.size() != 1) {
            throw new InvalidEcgException(
                    "Prototype supports one shared WFDB signal file"
            );
        }

        String signalFileName = referencedFiles.get(0);
        byte[] signalFile = files.get(signalFileName);

        if (signalFile == null) {
            throw new InvalidEcgException(
                    "Header references missing signal file: " + signalFileName
            );
        }

        ParsedEcg ecg = signalDecoder.decode(header, signalFile);
        return new ParsedEcgRecord(header.recordName(), ecg);
    }

}
