package mk.ukim.finki.backend.web.controller;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.dto.ModelResponse;
import mk.ukim.finki.backend.service.interfaces.ModelCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelCatalogService modelCatalogService;

    @GetMapping
    public List<ModelResponse> list() {
        return modelCatalogService.listActive();
    }
}
