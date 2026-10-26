package com.posadskiy.email.service;

import com.posadskiy.email.Config;
import com.posadskiy.email.client.UserClient;
import com.posadskiy.email.enums.ContentType;
import com.posadskiy.email.model.SendEmail;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Singleton
public class EmailService {

    private final Config config;
    private final UserClient userClient;

    public EmailService(Config config, UserClient userClient) {
        this.config = config;
        this.userClient = userClient;
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.getHost());
        properties.put("mail.smtp.port", config.getPort());
        properties.put("mail.smtp.auth", config.getAuth());
        properties.put("mail.smtp.starttls.enable", config.getStartTlsEnable());
        properties.put("mail.smtps.debug", config.getDebug());
        return properties;
    }

    public void sendText(String authorization, SendEmail sendEmail) {
        sendEmail(authorization, sendEmail, ContentType.Text);
    }

    public void sendHtml(String authorization, SendEmail sendEmail) {
        sendEmail(authorization, sendEmail, ContentType.HTML);
    }

    public void sendEmail(String authorization, SendEmail sendEmail, ContentType contentType) {
        Session session = Session.getDefaultInstance(getProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });

        var user = userClient.getUserById(authorization, sendEmail.userId());

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getUsername()));
            message.addRecipient(
                Message.RecipientType.TO,
                new InternetAddress(user.email()));

            message.setSubject(sendEmail.subject());
            if (contentType == ContentType.Text) {
                message.setText(sendEmail.body());
            }
            if (contentType == ContentType.HTML) {
                message.setContent(sendEmail.body(), "text/html; charset=utf-8");
            }

            Transport.send(message);
        } catch (MessagingException e) {
            log.info("Failed to send email", e);
        }
    }
}
