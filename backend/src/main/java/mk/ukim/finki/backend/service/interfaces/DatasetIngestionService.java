package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.model.IngestedUpload;

public interface DatasetIngestionService {

    IngestedUpload ingest(User owner, String originalFilename, byte[] content);
}
