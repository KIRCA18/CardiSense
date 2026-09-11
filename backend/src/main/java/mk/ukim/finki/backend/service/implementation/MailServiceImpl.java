package mk.ukim.finki.backend.service.implementation;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.exception.MailSendException;
import mk.ukim.finki.backend.service.interfaces.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final String FROM_ADDRESS = "no-reply@cardisense.app";
    private static final String FROM_NAME = "CardiSense";

    private final JavaMailSender mailSender;

    @Value("${verification.code.ttl-minutes}")
    private long ttl;

    @Override
    public void sendVerificationCodeEmail(User user, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM_ADDRESS, FROM_NAME);
            helper.setTo(user.getEmail());
            helper.setSubject("Your CardiSense verification code");
            helper.setText(buildHtml(user, code), true);
        } catch (jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("Could not build verification email", e);
        }
        mailSender.send(message);
    }

    private String buildHtml(User user, String code) {
        return """
                <div style="font-family: -apple-system, Segoe UI, sans-serif; max-width: 420px; margin: 0 auto; padding: 32px 24px; color: #14181b;">
                  <p style="margin: 0 0 16px;">Hi %s,</p>
                  <p style="margin: 0 0 24px;">Use this code to verify your CardiSense account. It expires in %d minutes.</p>
                  <div style="font-size: 32px; font-weight: 700; letter-spacing: 8px; text-align: center; background: #e4f2f0; color: #0b645c; padding: 16px; border-radius: 12px;">
                    %s
                  </div>
                  <p style="margin: 24px 0 0; font-size: 13px; color: #8d969d;">If you didn't request this, you can ignore this email.</p>
                </div>
                """.formatted(user.getFirstName(), ttl, code);
    }
}