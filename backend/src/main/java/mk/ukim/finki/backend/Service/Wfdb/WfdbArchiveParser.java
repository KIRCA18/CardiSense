package mk.ukim.finki.backend.service.wfdb;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import mk.ukim.finki.backend.service.interfaces.EcgArchiveParser;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

@Component
public class WfdbArchiveParser implements EcgArchiveParser {

    private static final int MAX_ENTRIES = 10;
    private static final int MAX_UNCOMPRESSED_BYTES = 10 * 1024 * 1024;

    private final WfdbHeaderParser headerParser;
    private final WfdbSignalDecoder signalDecoder;

    public WfdbArchiveParser(
            WfdbHeaderParser headerParser,
            WfdbSignalDecoder signalDecoder
    ) {
        this.headerParser = headerParser;
        this.signalDecoder = signalDecoder;
    }

    @Override
    public ParsedEcg parse(byte[] archive) {
        Map<String, byte[]> files = readZip(archive);

        List<String> headerFiles = files.keySet().stream()
                .filter(name -> name.toLowerCase().endsWith(".hea"))
                .toList();

        if (headerFiles.size() != 1) {
            throw new InvalidEcgException(
                    "ZIP must contain exactly one .hea file"
            );
        }

        String headerFileName = headerFiles.get(0);
        String headerText = new String(
                files.get(headerFileName),
                StandardCharsets.US_ASCII
        );

        WfdbHeader header = headerParser.parse(headerText);

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

        return signalDecoder.decode(header, signalFile);
    }

    private Map<String, byte[]> readZip(byte[] archive) {
        Map<String, byte[]> files =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        int entryCount = 0;
        int totalBytes = 0;

        try (
                ZipInputStream zip = new ZipInputStream(
                        new ByteArrayInputStream(archive)
                )
        ) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                entryCount++;
                if (entryCount > MAX_ENTRIES) {
                    throw new InvalidEcgException(
                            "ZIP contains too many files"
                    );
                }

                String name = validateEntryName(entry.getName());
                String extension = extension(name);

                if (!extension.equals("hea") && !extension.equals("mat")) {
                    throw new InvalidEcgException(
                            "Unsupported file in ZIP: " + name
                    );
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;

                while ((read = zip.read(buffer)) != -1) {
                    totalBytes += read;

                    if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                        throw new InvalidEcgException(
                                "Uncompressed ZIP contents exceed 10 MB"
                        );
                    }

                    output.write(buffer, 0, read);
                }

                if (files.putIfAbsent(name, output.toByteArray()) != null) {
                    throw new InvalidEcgException(
                            "ZIP contains duplicate file: " + name
                    );
                }
            }
        } catch (ZipException exception) {
            throw new InvalidEcgException(
                    "Uploaded file is not a valid ZIP archive",
                    exception
            );
        } catch (IOException exception) {
            throw new InvalidEcgException(
                    "Could not read ZIP archive",
                    exception
            );
        }

        if (files.isEmpty()) {
            throw new InvalidEcgException("ZIP archive is empty");
        }

        return files;
    }

    private String validateEntryName(String name) {
        String normalized = name.replace('\\', '/');

        if (
                normalized.startsWith("/") ||
                        normalized.contains("/") ||
                        normalized.equals(".") ||
                        normalized.equals("..")
        ) {
            throw new InvalidEcgException(
                    "ZIP files must be placed at the archive root: " + name
            );
        }

        return normalized;
    }

    private String extension(String name) {
        int separator = name.lastIndexOf('.');

        if (separator < 0 || separator == name.length() - 1) {
            return "";
        }

        return name.substring(separator + 1).toLowerCase();
    }
}
