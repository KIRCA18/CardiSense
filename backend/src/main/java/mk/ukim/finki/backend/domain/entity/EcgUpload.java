package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ecg_uploads")
@Getter
@NoArgsConstructor
public class EcgUpload {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String archiveFormat;

    @Column(nullable = false)
    private long archiveSize;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] archiveContent;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    public EcgUpload(
            String originalFilename,
            String archiveFormat,
            long archiveSize,
            byte[] archiveContent
    ) {
        this.originalFilename = originalFilename;
        this.archiveFormat = archiveFormat;
        this.archiveSize = archiveSize;
        this.archiveContent = archiveContent;
    }
}