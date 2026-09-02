package mk.ukim.finki.backend.web.controller;

import mk.ukim.finki.backend.domain.dto.DatasetAnalysisDto;
import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.model.DatasetAnalysisResult;
import mk.ukim.finki.backend.service.interfaces.EcgAnalysisService;
import mk.ukim.finki.backend.web.mapper.AnalysisResponseMapper;
import mk.ukim.finki.backend.web.mapper.EcgUploadMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import mk.ukim.finki.backend.domain.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/analyses")
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
    public DatasetAnalysisDto analyze(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam(
                    name = "modelId",
                    defaultValue = "ecg-fm"
            ) String modelId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        AnalyzeEcgRequest request = uploadMapper.toRequest(
                files,
                modelId,
                authenticatedUser.getId()
        );

        DatasetAnalysisResult result = analysisService.analyze(request);

        return responseMapper.toResponse(result);
    }
}
