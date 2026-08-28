package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.model.AnalysisResult;
import mk.ukim.finki.backend.domain.model.CommonEcg;

public interface ModelInferenceService {

    AnalysisResult infer(String modelId, CommonEcg ecg);
}
