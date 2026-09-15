package mk.ukim.finki.backend.web.controller;

import jakarta.validation.Valid;
import mk.ukim.finki.backend.domain.dto.AnalysisEntryResponse;
import mk.ukim.finki.backend.domain.dto.DatasetAnalysisDto;
import mk.ukim.finki.backend.domain.dto.RunAnalysisRequest;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.model.DatasetAnalysisResult;
import mk.ukim.finki.backend.service.interfaces.EcgAnalysisService;
import mk.ukim.finki.backend.web.mapper.AnalysisResponseMapper;
import mk.ukim.finki.backend.web.mapper.EcgUploadMapper;
import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

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
    public DatasetAnalysisDto analyzeQuick(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam(name = "modelId", defaultValue = "ecg-fm") String modelId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        AnalyzeEcgRequest request = uploadMapper.toRequest(files, modelId, authenticatedUser.getId());
        DatasetAnalysisResult result = analysisService.analyze(request);
        return responseMapper.toResponse(result);
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<AnalysisEntryResponse> run(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody RunAnalysisRequest request
    ) {
        return analysisService.run(authenticatedUser, request.recordIds(), request.modelKeys());
    }

    @GetMapping
    public List<AnalysisEntryResponse> history(@AuthenticationPrincipal User authenticatedUser) {
        return analysisService.history(authenticatedUser.getId());
    }

    @GetMapping("/{id}")
    public AnalysisEntryResponse get(
            @AuthenticationPrincipal User authenticatedUser,
            @PathVariable UUID id
    ) {
        return analysisService.getForUser(id, authenticatedUser.getId());
    }
}
