package mk.ukim.finki.backend.Service;

import mk.ukim.finki.backend.Domain.ParsedEcg;

public interface EcgArchiveParser {

    ParsedEcg parse(byte[] archive);
}
