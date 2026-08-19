package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.model.ParsedEcg;

public interface EcgArchiveParser {

    ParsedEcg parse(byte[] archive);
}
