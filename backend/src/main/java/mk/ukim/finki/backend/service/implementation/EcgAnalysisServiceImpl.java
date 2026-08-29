package mk.ukim.finki.backend.service.implementation;

import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.model.CommonEcg;
import mk.ukim.finki.backend.domain.model.DatasetAnalysisResult;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.service.interfaces.EcgAnalysisService;
import mk.ukim.finki.backend.service.interfaces.EcgArchiveParser;
import mk.ukim.finki.backend.service.interfaces.ModelInferenceService;
import mk.ukim.finki.backend.service.validation.EcgUploadValidator;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public DatasetAnalysisResult analyze(AnalyzeEcgRequest request) {
        uploadValidator.validate(request);

        List<ParsedEcgRecord> parsedRecords =
                archiveParser.parse(
                        request.originalFilename(),
                        request.content()
                );

        List<DatasetAnalysisResult.RecordAnalysis> analyses = parsedRecords.stream()
                .map(record -> analyzeRecord(record, request.modelId()))
                .toList();

        return new DatasetAnalysisResult(analyses);
    }

    private DatasetAnalysisResult.RecordAnalysis analyzeRecord(
            ParsedEcgRecord record,
            String modelId
    ) {
        CommonEcg anonymized = anonymizationService.anonymize(record.ecg());

        return new DatasetAnalysisResult.RecordAnalysis(
                record.recordName(),
                modelInferenceService.infer(modelId, anonymized)
        );
    }
}
