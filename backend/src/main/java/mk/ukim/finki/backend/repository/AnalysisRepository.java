package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.domain.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    List<Analysis> findByEcgRecord_IdOrderByStartedAtDesc(UUID ecgRecordId);
}
