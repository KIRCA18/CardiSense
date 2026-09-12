package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.dto.DatasetResponse;
import mk.ukim.finki.backend.domain.entity.User;

import java.util.List;
import java.util.UUID;

public interface DatasetService {

    DatasetResponse upload(User owner, String originalFilename, byte[] content);

    List<DatasetResponse> listForOwner(UUID ownerId);

    DatasetResponse getForOwner(UUID datasetId, UUID ownerId);
}
