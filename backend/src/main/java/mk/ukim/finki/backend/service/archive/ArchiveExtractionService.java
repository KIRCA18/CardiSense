package mk.ukim.finki.backend.service.archive;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ArchiveExtractionService {

    private static final int MAX_ENTRIES = 100;
    private static final long MAX_UNCOMPRESSED_BYTES = 50L * 1024 * 1024;
    private static final int SEVEN_Z_MEMORY_LIMIT_KIB = 64 * 1024;

    public Map<String, byte[]> extract(String filename, byte[] archiveBytes) {
        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        ExtractionState state = new ExtractionState();

        try {
            if (normalizedFilename.endsWith(".zip")) {
                extractZip(archiveBytes, state);
            } else if (normalizedFilename.endsWith(".7z")) {
                extractSevenZip(archiveBytes, state);
            } else if (normalizedFilename.endsWith(".rar")) {
                extractRar(archiveBytes, state);
            } else if (
                    normalizedFilename.endsWith(".tar.gz") ||
                            normalizedFilename.endsWith(".tgz")
            ) {
                extractTarGzip(archiveBytes, state);
            } else if (normalizedFilename.endsWith(".tar")) {
                extractTar(new ByteArrayInputStream(archiveBytes), state);
            } else {
                throw new InvalidEcgException(
                        "Unsupported archive format: " + filename
                );
            }
        } catch (InvalidEcgException exception) {
            throw exception;
        } catch (RarException | IOException exception) {
            throw new InvalidEcgException(
                    "Could not read ECG dataset archive: " + filename,
                    exception
            );
        }

        return state.result();
    }

    private void extractZip(byte[] archiveBytes, ExtractionState state)
            throws IOException {
        try (
                ZipArchiveInputStream archive = new ZipArchiveInputStream(
                        new BufferedInputStream(
                                new ByteArrayInputStream(archiveBytes)
                        )
                )
        ) {
            ZipArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                state.add(entry.getName(), entry.isDirectory(), archive);
            }
        }
    }

    private void extractTarGzip(byte[] archiveBytes, ExtractionState state)
            throws IOException {
        try (
                GzipCompressorInputStream gzip = new GzipCompressorInputStream(
                        new BufferedInputStream(
                                new ByteArrayInputStream(archiveBytes)
                        )
                )
        ) {
            extractTar(gzip, state);
        }
    }

    private void extractTar(InputStream input, ExtractionState state)
            throws IOException {
        try (TarArchiveInputStream archive = new TarArchiveInputStream(input)) {
            TarArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                state.add(entry.getName(), entry.isDirectory(), archive);
            }
        }
    }

    private void extractSevenZip(byte[] archiveBytes, ExtractionState state)
            throws IOException {
        try (
                SeekableInMemoryByteChannel channel =
                        new SeekableInMemoryByteChannel(archiveBytes);
                SevenZFile archive = SevenZFile.builder()
                        .setSeekableByteChannel(channel)
                        .setMaxMemoryLimitKiB(SEVEN_Z_MEMORY_LIMIT_KIB)
                        .get()
        ) {
            SevenZArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                state.add(
                        entry.getName(),
                        false,
                        archive.getInputStream(entry)
                );
            }
        }
    }

    private void extractRar(byte[] archiveBytes, ExtractionState state)
            throws IOException, RarException {
        try (
                Archive archive = new Archive(
                        new ByteArrayInputStream(archiveBytes)
                )
        ) {
            if (archive.isPasswordProtected()) {
                throw new InvalidEcgException(
                        "Password-protected RAR archives are not supported"
                );
            }

            for (FileHeader entry : archive.getFileHeaders()) {
                if (entry.isDirectory()) {
                    continue;
                }
                try (InputStream input = archive.getInputStream(entry)) {
                    state.add(entry.getFileName(), false, input);
                }
            }
        }
    }

    private static final class ExtractionState {

        private final Map<String, byte[]> files =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private int entryCount;
        private long totalBytes;

        void add(String rawName, boolean directory, InputStream input)
                throws IOException {
            if (directory) {
                return;
            }

            entryCount++;
            if (entryCount > MAX_ENTRIES) {
                throw new InvalidEcgException(
                        "Archive contains more than %d files"
                                .formatted(MAX_ENTRIES)
                );
            }

            String name = validateEntryName(rawName);
            validateExtension(name);
            byte[] content = readEntry(input);

            if (files.putIfAbsent(name, content) != null) {
                throw new InvalidEcgException(
                        "Archive contains duplicate file: " + name
                );
            }
        }

        Map<String, byte[]> result() {
            if (files.isEmpty()) {
                throw new InvalidEcgException("ECG dataset archive is empty");
            }
            return files;
        }

        private byte[] readEntry(InputStream input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                    throw new InvalidEcgException(
                            "Uncompressed archive contents exceed 50 MB"
                    );
                }
                output.write(buffer, 0, read);
            }

            return output.toByteArray();
        }

        private String validateEntryName(String rawName) {
            if (rawName == null || rawName.isBlank()) {
                throw new InvalidEcgException(
                        "Archive contains an unnamed file"
                );
            }

            String normalized = rawName.replace('\\', '/');
            if (
                    normalized.startsWith("/") ||
                            normalized.contains("/") ||
                            normalized.equals(".") ||
                            normalized.equals("..")
            ) {
                throw new InvalidEcgException(
                        "Archive files must be placed at the archive root: " +
                                rawName
                );
            }

            return normalized;
        }

        private void validateExtension(String name) {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (
                    !normalized.endsWith(".hea") &&
                            !normalized.endsWith(".mat") &&
                            !normalized.endsWith(".dcm")
            ) {
                throw new InvalidEcgException(
                        "Unsupported file in archive: " + name
                );
            }
        }
    }
}
