package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.domain.entity.EcgRecordOwnership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EcgRecordOwnershipRepository extends JpaRepository<EcgRecordOwnership, UUID> {

    List<EcgRecordOwnership> findByUser_IdOrderByLinkedAtDesc(UUID userId);

    List<EcgRecordOwnership> findByEcgRecord_IdInAndUser_Id(List<UUID> ecgRecordIds, UUID userId);

    boolean existsByEcgRecord_Upload_IdAndUser_Id(UUID uploadId, UUID userId);
}
