package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.dto.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.model.AnalysisResult;

public interface EcgAnalysisService {

    AnalysisResult analyze(AnalyzeEcgRequest request);
}
