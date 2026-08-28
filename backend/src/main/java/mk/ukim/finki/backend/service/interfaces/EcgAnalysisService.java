package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.model.DatasetAnalysisResult;

public interface EcgAnalysisService {

    DatasetAnalysisResult analyze(AnalyzeEcgRequest request);
}
