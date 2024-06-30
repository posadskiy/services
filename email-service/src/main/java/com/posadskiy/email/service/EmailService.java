package com.posadskiy.email.service;

import com.posadskiy.email.Config;
import com.posadskiy.email.model.SendEmail;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Singleton
public class EmailService {

    @Inject
    Config config;

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.getHost());
        properties.put("mail.smtp.port", config.getPort());
        properties.put("mail.smtp.auth", config.getAuth());
        properties.put("mail.smtp.starttls.enable", config.getStartTlsEnable());
        properties.put("mail.smtps.debug", config.getDebug());
        return properties;
    }

    public void sendText(SendEmail sendEmail) {
        Session session = Session.getDefaultInstance(getProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(config.getUsername()));
            msg.addRecipient(
                Message.RecipientType.TO,
                new InternetAddress(sendEmail.to()));

            msg.setSubject(sendEmail.subject());
            msg.setText(sendEmail.body());

            Transport.send(msg);
        } catch (MessagingException e) {
            log.info("Failed to send email", e);
        }
    }

    public void sendHtml(SendEmail sendEmail) {
        Session session = Session.getDefaultInstance(getProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });

        try {
            var message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getUsername()));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(sendEmail.to()));

            message.setSubject(sendEmail.subject());
            message.setContent(sendEmail.body(), "text/html; charset=utf-8");

            Transport.send(message);
        } catch (MessagingException e) {
            log.info("Failed to send email", e);
        }
    }
}
