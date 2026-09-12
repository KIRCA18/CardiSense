package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.domain.entity.EcgUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EcgUploadRepository extends JpaRepository<EcgUpload, UUID> {

    @Query("""
            select distinct u from EcgUpload u
            join EcgRecord r on r.upload = u
            join EcgRecordOwnership o on o.ecgRecord = r
            where o.user.id = :userId
            order by u.uploadedAt desc
            """)
    List<EcgUpload> findAllOwnedBy(@Param("userId") UUID userId);

    @Query("""
            select u from EcgUpload u
            where u.id = :uploadId
            and exists (
                select 1 from EcgRecordOwnership o
                where o.ecgRecord.upload = u and o.user.id = :userId
            )
            """)
    Optional<EcgUpload> findByIdOwnedBy(@Param("uploadId") UUID uploadId, @Param("userId") UUID userId);
}
