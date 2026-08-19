package mk.ukim.finki.backend.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ModelServiceProperties.class)
public class ModelServiceConfig {

    @Bean
    RestClient modelServiceRestClient(
            RestClient.Builder builder,
            ModelServiceProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofMinutes(5));

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
