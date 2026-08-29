package mk.ukim.finki.backend.service.implementation;

import mk.ukim.finki.backend.domain.exception.ModelInferenceException;
import mk.ukim.finki.backend.service.externalmodel.FastApiContracts;
import mk.ukim.finki.backend.domain.model.AnalysisResult;
import mk.ukim.finki.backend.domain.model.CommonEcg;
import mk.ukim.finki.backend.service.interfaces.ModelInferenceService;
import mk.ukim.finki.backend.web.mapper.FastApiModelMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FastApiModelServiceImpl implements ModelInferenceService {

    private final RestClient restClient;
    private final FastApiModelMapper mapper;

    public FastApiModelServiceImpl(
            RestClient modelServiceRestClient,
            FastApiModelMapper mapper
    ) {
        this.restClient = modelServiceRestClient;
        this.mapper = mapper;
    }

    @Override
    public AnalysisResult infer(String modelId, CommonEcg ecg) {
        FastApiContracts.InferenceRequest request = mapper.toRequest(modelId, ecg);

        try {
            FastApiContracts.InferenceResponse response = restClient.post()
                    .uri("/v1/infer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FastApiContracts.InferenceResponse.class);

            if (response == null) {
                throw new ModelInferenceException(
                        "Model service returned an empty response"
                );
            }

            return mapper.toDomain(response);

        } catch (RestClientResponseException exception) {
            throw new ModelInferenceException(
                    "Model service returned HTTP %s: %s"
                            .formatted(
                                    exception.getStatusCode(),
                                    exception.getResponseBodyAsString()
                            ),
                    exception
            );
        } catch (RestClientException exception) {
            throw new ModelInferenceException(
                    "Could not communicate with the model service",
                    exception
            );
        }
    }
}
