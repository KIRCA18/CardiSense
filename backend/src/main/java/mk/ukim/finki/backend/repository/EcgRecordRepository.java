package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.domain.entity.EcgRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EcgRecordRepository extends JpaRepository<EcgRecord, UUID> {

    List<EcgRecord> findByUpload_IdOrderByRecordNameAsc(UUID uploadId);
}
