package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.dto.ModelResponse;
import mk.ukim.finki.backend.domain.entity.MlModel;
import mk.ukim.finki.backend.repository.MlModelRepository;
import mk.ukim.finki.backend.service.interfaces.ModelCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelCatalogServiceImpl implements ModelCatalogService {

    private final MlModelRepository mlModelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModelResponse> listActive() {
        return mlModelRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    private ModelResponse toResponse(MlModel model) {
        return new ModelResponse(
                model.getModelKey(),
                model.getName(),
                model.getVersion(),
                model.getDescription(),
                model.getDevice(),
                model.getCheckpoint()
        );
    }
}
