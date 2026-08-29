package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;

import java.util.List;

public interface EcgArchiveParser {

    List<ParsedEcgRecord> parse(String archiveFilename, byte[] archive);
}
