package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.dto.DatasetResponse;
import mk.ukim.finki.backend.domain.entity.EcgUpload;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.exception.NotFoundException;
import mk.ukim.finki.backend.domain.model.IngestedUpload;
import mk.ukim.finki.backend.repository.EcgRecordRepository;
import mk.ukim.finki.backend.repository.EcgUploadRepository;
import mk.ukim.finki.backend.service.interfaces.DatasetIngestionService;
import mk.ukim.finki.backend.service.interfaces.DatasetService;
import mk.ukim.finki.backend.web.mapper.DatasetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final DatasetIngestionService ingestionService;
    private final EcgUploadRepository uploadRepository;
    private final EcgRecordRepository ecgRecordRepository;

    @Override
    @Transactional
    public DatasetResponse upload(User owner, String originalFilename, byte[] content) {
        IngestedUpload ingested = ingestionService.ingest(owner, originalFilename, content);
        return DatasetMapper.toResponse(ingested.upload(), ingested.records());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetResponse> listForOwner(UUID ownerId) {
        return uploadRepository.findAllOwnedBy(ownerId).stream()
                .map(upload -> DatasetMapper.toResponse(
                        upload,
                        ecgRecordRepository.findByUpload_IdOrderByRecordNameAsc(upload.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetResponse getForOwner(UUID datasetId, UUID ownerId) {
        EcgUpload upload = uploadRepository.findByIdOwnedBy(datasetId, ownerId)
                .orElseThrow(() -> new NotFoundException("Dataset not found"));
        return DatasetMapper.toResponse(upload, ecgRecordRepository.findByUpload_IdOrderByRecordNameAsc(upload.getId()));
    }
}
