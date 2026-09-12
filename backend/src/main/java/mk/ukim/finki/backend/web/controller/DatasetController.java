package mk.ukim.finki.backend.web.controller;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.dto.DatasetResponse;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.exception.UploadReadException;
import mk.ukim.finki.backend.service.interfaces.DatasetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetResponse upload(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null) {
            throw new InvalidEcgException("A file is required");
        }
        try {
            return datasetService.upload(currentUser, file.getOriginalFilename(), file.getBytes());
        } catch (IOException exception) {
            throw new UploadReadException("Could not read uploaded ECG file", exception);
        }
    }

    @GetMapping
    public List<DatasetResponse> list(@AuthenticationPrincipal User currentUser) {
        return datasetService.listForOwner(currentUser.getId());
    }

    @GetMapping("/{id}")
    public DatasetResponse get(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        return datasetService.getForOwner(id, currentUser.getId());
    }
}
