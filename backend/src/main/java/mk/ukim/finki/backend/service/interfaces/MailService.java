package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.entity.User;

public interface MailService {
    void sendVerificationCodeEmail(User user, String code);
}