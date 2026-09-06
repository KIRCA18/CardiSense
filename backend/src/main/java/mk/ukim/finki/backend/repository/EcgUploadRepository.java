package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.domain.entity.EcgUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EcgUploadRepository extends JpaRepository<EcgUpload, UUID> {
}
