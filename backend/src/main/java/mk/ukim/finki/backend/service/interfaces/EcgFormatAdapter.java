package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;

public interface EcgFormatAdapter {

    boolean supports(String filename);

    EcgFormat format();

    PreparedEcgUpload prepare(String filename, byte[] content);
}
