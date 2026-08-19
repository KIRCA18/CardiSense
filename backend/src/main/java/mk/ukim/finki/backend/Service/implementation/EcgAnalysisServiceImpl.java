package mk.ukim.finki.backend.service.implementation;

import mk.ukim.finki.backend.domain.dto.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import mk.ukim.finki.backend.domain.model.CommonEcg;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import mk.ukim.finki.backend.service.interfaces.EcgAnalysisService;
import mk.ukim.finki.backend.service.interfaces.EcgArchiveParser;
import mk.ukim.finki.backend.service.interfaces.ModelInferenceService;
import mk.ukim.finki.backend.service.validation.EcgUploadValidator;
import org.springframework.stereotype.Service;

@Service
public class EcgAnalysisServiceImpl implements EcgAnalysisService {

    private final EcgUploadValidator uploadValidator;
    private final EcgArchiveParser archiveParser;
    private final EcgAnonymizationService anonymizationService;
    private final ModelInferenceService modelInferenceService;

    public EcgAnalysisServiceImpl(
            EcgUploadValidator uploadValidator,
            EcgArchiveParser archiveParser,
            EcgAnonymizationService anonymizationService,
            ModelInferenceService modelInferenceService
    ) {
        this.uploadValidator = uploadValidator;
        this.archiveParser = archiveParser;
        this.anonymizationService = anonymizationService;
        this.modelInferenceService = modelInferenceService;
    }

    @Override
    public AnalysisResult analyze(AnalyzeEcgRequest request) {
        uploadValidator.validate(request);

        ParsedEcg parsed = archiveParser.parse(request.content());
        CommonEcg anonymized = anonymizationService.anonymize(parsed);

        return modelInferenceService.infer(request.modelId(), anonymized);
    }
}
