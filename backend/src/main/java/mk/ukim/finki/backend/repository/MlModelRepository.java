package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.domain.entity.MlModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MlModelRepository extends JpaRepository<MlModel, UUID> {

    Optional<MlModel> findByModelKey(String modelKey);

    List<MlModel> findByActiveTrue();
}
