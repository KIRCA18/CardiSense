package mk.ukim.finki.backend.web.controller;

import mk.ukim.finki.backend.domain.dto.AnalysisResponse;
import mk.ukim.finki.backend.domain.dto.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import mk.ukim.finki.backend.service.interfaces.EcgAnalysisService;
import mk.ukim.finki.backend.service.mapper.AnalysisResponseMapper;
import mk.ukim.finki.backend.service.mapper.EcgUploadMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final EcgUploadMapper uploadMapper;
    private final EcgAnalysisService analysisService;
    private final AnalysisResponseMapper responseMapper;

    public AnalysisController(
            EcgUploadMapper uploadMapper,
            EcgAnalysisService analysisService,
            AnalysisResponseMapper responseMapper
    ) {
        this.uploadMapper = uploadMapper;
        this.analysisService = analysisService;
        this.responseMapper = responseMapper;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AnalysisResponse analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    name = "modelId",
                    defaultValue = "ecg-fm"
            ) String modelId
    ) {
        AnalyzeEcgRequest request = uploadMapper.toRequest(file, modelId);
        AnalysisResult result = analysisService.analyze(request);
        return responseMapper.toResponse(result);
    }
}
