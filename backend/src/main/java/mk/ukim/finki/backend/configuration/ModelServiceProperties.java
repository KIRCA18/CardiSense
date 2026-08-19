package mk.ukim.finki.backend.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cardisense.model-service")
public record ModelServiceProperties(
        @NotBlank String baseUrl
) {
}
