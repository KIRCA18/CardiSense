package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;

public interface EcgFormatAdapter {

    boolean supports(String filename);

    PreparedEcgUpload prepare(String filename, byte[] content);
}
