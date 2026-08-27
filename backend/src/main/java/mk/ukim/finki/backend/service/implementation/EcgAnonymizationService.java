package mk.ukim.finki.backend.service.implementation;

import mk.ukim.finki.backend.domain.model.CommonEcg;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EcgAnonymizationService {

    public CommonEcg anonymize(ParsedEcg parsed) {
        return new CommonEcg(
                UUID.randomUUID().toString(),
                parsed.waveform(),
                List.copyOf(parsed.leadNames()),
                parsed.samplingRateHz(),
                List.copyOf(parsed.units())
        );
    }
}
