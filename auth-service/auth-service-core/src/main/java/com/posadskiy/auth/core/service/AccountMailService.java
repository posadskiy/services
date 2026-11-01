package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.crypto.PasswordHasher;
import com.posadskiy.auth.core.crypto.PasswordPolicy;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.mail.EmailLinkOrigins;
import com.posadskiy.auth.core.mail.MailFrom;
import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.storage.db.EmailSuppressionRepository;
import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Singleton
public class AccountMailService {

    public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String CHANGE_EMAIL = "CHANGE_EMAIL";

    private static final Logger LOG = LoggerFactory.getLogger(AccountMailService.class);

    private final UsersRepository usersRepository;
    private final ActionTokenService actionTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final MailOutbox outbox;
    private final AuditService auditService;
    private final AuthProperties properties;
    private final EmailSuppressionRepository suppressionRepository;

    public AccountMailService(
        UsersRepository usersRepository,
        ActionTokenService actionTokenService,
        RefreshTokenRepository refreshTokenRepository,
        PasswordHasher passwordHasher,
        PasswordPolicy passwordPolicy,
        MailOutbox outbox,
        AuditService auditService,
        AuthProperties properties,
        EmailSuppressionRepository suppressionRepository) {
        this.usersRepository = usersRepository;
        this.actionTokenService = actionTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.outbox = outbox;
        this.auditService = auditService;
        this.properties = properties;
        this.suppressionRepository = suppressionRepository;
    }

    private static String link(String origin, String path, String token) {
        return origin + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    public void checkOrigin(@Nullable String linkOrigin) {
        origin(linkOrigin);
    }

    public void sendVerification(UserEntity user, @Nullable String linkOrigin) {
        if (user.emailVerified() || user.passwordHash() == null) {
            return;
        }
        String resolved = origin(linkOrigin);
        ActionTokenService.Issued issued = actionTokenService.issue(
            user.id(), VERIFY_EMAIL, properties.mail().verifyTtl());
        String url = link(resolved, "/verify-email", issued.raw());
        deliver(
            user,
            resolved,
            "verify-email",
            Map.of("VERIFY_URL", url),
            "verify-email/" + user.id() + "/" + issued.id(),
            "verify_email");
    }

    public void sendPasswordReset(String email, @Nullable String linkOrigin) {
        String resolved = origin(linkOrigin);
        String normalized = EmailNormalizer.normalize(email);
        if (normalized == null) {
            return;
        }
        usersRepository
            .findByEmailNormalized(normalized)
            .filter(UserEntity::active)
            .ifPresent(user -> {
                ActionTokenService.Issued issued = actionTokenService.issue(
                    user.id(), RESET_PASSWORD, properties.mail().resetTtl());
                String url = link(resolved, "/reset-password", issued.raw());
                deliver(
                    user,
                    resolved,
                    "password-reset",
                    Map.of("RESET_URL", url),
                    "password-reset/" + user.id() + "/" + issued.id(),
                    "password_reset");
            });
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        var token = actionTokenService.consume(rawToken, VERIFY_EMAIL);
        UserEntity user = usersRepository.findById(token.userId()).orElseThrow(this::invalidToken);
        if (!user.active()) {
            throw invalidToken();
        }
        if (!user.emailVerified()) {
            usersRepository.update(user.withEmailVerified(true));
            auditService.record(user.id(), "EMAIL_VERIFIED", null);
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        var token = actionTokenService.consume(rawToken, RESET_PASSWORD);
        UserEntity user = usersRepository.findById(token.userId()).orElseThrow(this::invalidToken);
        if (!user.active()) {
            throw invalidToken();
        }
        passwordPolicy.validate(newPassword, user.email());
        usersRepository.update(
            user
                .withPassword(passwordHasher.hash(newPassword), Instant.now())
                .withEmailVerified(true));
        refreshTokenRepository.revokeAllForUser(user.id());
        auditService.record(user.id(), "PASSWORD_RESET", null);
    }

    public void requestEmailChange(UserEntity user, String email, String linkOrigin, @Nullable String password) {
        if (user.passwordHash() != null) {
            if (password == null || !passwordHasher.verify(password, user.passwordHash())) {
                throw new com.posadskiy.auth.core.exception.InvalidCredentialsException();
            }
        }
        String normalized = EmailNormalizer.normalize(email);
        if (normalized == null || !normalized.contains("@")) {
            throw new AuthException("A valid email is required");
        }
        if (normalized.equals(user.emailNormalized())) {
            throw new AuthException("That is already your email address");
        }
        if (usersRepository.findByEmailNormalized(normalized).isPresent()) {
            throw new AuthException("An account with that email already exists", 409);
        }
        String resolved = origin(linkOrigin);
        ActionTokenService.Issued issued =
            actionTokenService.issue(user.id(), CHANGE_EMAIL, properties.mail().verifyTtl(), normalized);
        String url = link(resolved, "/confirm-email", issued.raw());
        deliverTo(
            user,
            email.trim(),
            resolved,
            "change-email",
            Map.of("VERIFY_URL", url),
            "change-email/" + user.id() + "/" + issued.id(),
            "change_email");
    }

    @Transactional
    public void confirmEmailChange(String rawToken) {
        var token = actionTokenService.consume(rawToken, CHANGE_EMAIL);
        UserEntity user = usersRepository.findById(token.userId()).orElseThrow(this::invalidToken);
        if (!user.active() || token.payload() == null) {
            throw invalidToken();
        }
        if (usersRepository.findByEmailNormalized(token.payload()).isPresent()) {
            throw new AuthException("An account with that email already exists", 409);
        }
        usersRepository.update(user.withEmail(token.payload(), token.payload()));
        auditService.record(user.id(), "EMAIL_CHANGED", null);
    }

    public void sendNewDevice(UserEntity user, ClientContext client) {
        if (client == null || client.linkOrigin() == null || client.linkOrigin().isBlank()) {
            return;
        }
        String device = client.userAgent() == null || client.userAgent().isBlank() ? "a new device" : client.userAgent();
        deliver(
            user,
            client.linkOrigin(),
            "new-login",
            Map.of("DEVICE", device, "WHEN", Instant.now().toString()),
            "new-login/" + user.id() + "/" + Instant.now().toEpochMilli(),
            "new_login");
    }

    private void deliver(
        UserEntity user,
        String origin,
        String template,
        Map<String, String> variables,
        String key,
        String category) {
        deliverTo(user, user.email(), origin, template, variables, key, category);
    }

    private void deliverTo(
        UserEntity user,
        String to,
        String origin,
        String template,
        Map<String, String> variables,
        String key,
        String category) {
        String normalized = EmailNormalizer.normalize(to);
        if (normalized != null && suppressionRepository.findById(normalized).isPresent()) {
            return;
        }
        try {
            AuthProperties.Mail mail = properties.mail();
            String from = MailFrom.resolve(origin, mail.grammarbarFrom(), mail.posadskiyFrom());
            outbox.submit(new TransactionalMail(from, to, template, variables, key, category));
        } catch (RuntimeException e) {
            LOG.warn("transactional mail was not accepted userId={} template={}", user.id(), template, e);
        }
    }

    private String origin(@Nullable String linkOrigin) {
        AuthProperties.Mail mail = properties.mail();
        return EmailLinkOrigins.resolve(linkOrigin, mail.allowedOrigins());
    }

    private AuthException invalidToken() {
        return new AuthException("Invalid or expired token");
    }
}
