package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.dto.AnalysisEntryResponse;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.model.DatasetAnalysisResult;
import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;

import java.util.List;
import java.util.UUID;

public interface EcgAnalysisService {

    DatasetAnalysisResult analyze(AnalyzeEcgRequest request);

    List<AnalysisEntryResponse> run(User user, List<UUID> recordIds, List<String> modelKeys);

    List<AnalysisEntryResponse> history(UUID userId);

    AnalysisEntryResponse getForUser(UUID analysisId, UUID userId);
}
