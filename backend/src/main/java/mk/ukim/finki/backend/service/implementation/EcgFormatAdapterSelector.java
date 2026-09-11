package mk.ukim.finki.backend.service.implementation;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EcgFormatAdapterSelector {

    private final List<EcgFormatAdapter> adapters;

    public EcgFormatAdapterSelector(List<EcgFormatAdapter> adapters) {
        this.adapters = adapters;
    }

    public EcgFormatAdapter select(String filename) {
        List<EcgFormatAdapter> matchingAdapters = adapters.stream()
                .filter(adapter -> adapter.supports(filename))
                .toList();

        if (matchingAdapters.isEmpty()) {
            throw new InvalidEcgException(
                    "No ECG parser supports the uploaded file: " + filename
            );
        }

        if (matchingAdapters.size() > 1) {
            throw new IllegalStateException(
                    "More than one ECG parser supports: " + filename
            );
        }

        return matchingAdapters.get(0);
    }
}