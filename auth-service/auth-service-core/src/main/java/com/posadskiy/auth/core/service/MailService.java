package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import jakarta.inject.Singleton;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private final AuthProperties.Mail mail;

    public MailService(AuthProperties properties) {
        this.mail = properties.getMail();
    }

    public void send(String to, String subject, String body) {
        if (!mail.isEnabled() || mail.getHost() == null) {
            LOG.info("mail disabled; would send to={} subject={} body={}", to, subject, body);
            return;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", mail.getHost());
            props.put("mail.smtp.port", String.valueOf(mail.getPort()));
            props.put("mail.smtp.auth", mail.getUsername() != null ? "true" : "false");
            props.put("mail.smtp.starttls.enable", String.valueOf(mail.isStartTls()));
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mail.getUsername(), mail.getPassword());
                }
            });
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mail.getFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (Exception e) {
            LOG.warn("Failed to send mail to {}", to, e);
        }
    }
}
