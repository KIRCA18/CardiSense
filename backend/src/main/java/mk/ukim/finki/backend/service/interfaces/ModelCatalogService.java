package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.dto.ModelResponse;

import java.util.List;

public interface ModelCatalogService {

    List<ModelResponse> listActive();
}
