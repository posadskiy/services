package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.storage.db.PasskeyCredentialRepository;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.WebAuthnChallengeRepository;
import com.posadskiy.auth.core.storage.db.entity.PasskeyCredentialEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import com.posadskiy.auth.core.storage.db.entity.WebAuthnChallengeEntity;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class PasskeyService implements com.yubico.webauthn.CredentialRepository {

    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";

    private final PasskeyCredentialRepository credentials;
    private final WebAuthnChallengeRepository challenges;
    private final UsersRepository usersRepository;
    private final AuditService auditService;
    private final AuthProperties properties;
    private final Set<String> origins;

    public PasskeyService(
        PasskeyCredentialRepository credentials,
        WebAuthnChallengeRepository challenges,
        UsersRepository usersRepository,
        AuditService auditService,
        AuthProperties properties) {
        this.credentials = credentials;
        this.challenges = challenges;
        this.usersRepository = usersRepository;
        this.auditService = auditService;
        this.properties = properties;
        this.origins = Set.of(properties.passkey().origins().split(",")).stream()
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    public PasskeyCeremony registrationOptions(UserEntity user, String origin) {
        try {
            RelyingParty party = party(origin);
            PublicKeyCredentialCreationOptions options = party.startRegistration(StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                    .name(user.email())
                    .displayName(user.email())
                    .id(handle(user.id()))
                    .build())
                .build());
            WebAuthnChallengeEntity saved = challenges.save(new WebAuthnChallengeEntity(
                null, user.id(), REGISTER, options.toJson(), Instant.now().plus(Duration.ofMinutes(5)), null));
            return new PasskeyCeremony(saved.id(), options.toCredentialsCreateJson());
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Passkey registration could not be started");
        }
    }

    @Transactional
    public PasskeyCredentialEntity finishRegistration(UserEntity user, long challengeId, String credentialJson, String label) {
        WebAuthnChallengeEntity challenge = requireChallenge(challengeId, REGISTER, user.id());
        try {
            PublicKeyCredentialCreationOptions options = PublicKeyCredentialCreationOptions.fromJson(challenge.requestJson());
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response =
                PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
            RegistrationResult result = partyForRpId(options.getRp().getId()).finishRegistration(FinishRegistrationOptions.builder()
                .request(options)
                .response(response)
                .build());
            challenges.deleteById(challenge.id());
            PasskeyCredentialEntity saved = credentials.save(new PasskeyCredentialEntity(
                null,
                user.id(),
                result.getKeyId().getId().getBase64Url(),
                result.getPublicKeyCose().getBase64Url(),
                result.getSignatureCount(),
                handle(user.id()).getBase64Url(),
                blankToNull(label),
                null,
                null));
            auditService.record(user.id(), "PASSKEY_REGISTERED", saved.label());
            return saved;
        } catch (RegistrationFailedException | IOException e) {
            throw new AuthException("Passkey was not accepted");
        }
    }

    public PasskeyCeremony loginOptions(String email, String origin) {
        try {
            StartAssertionOptions.StartAssertionOptionsBuilder builder = StartAssertionOptions.builder();
            if (email != null && !email.isBlank()) {
                builder.username(email.trim());
            }
            AssertionRequest request = party(origin).startAssertion(builder.build());
            Long userId = email == null || email.isBlank()
                ? null
                : usersRepository.findByEmailNormalized(email.trim().toLowerCase()).map(UserEntity::id).orElse(null);
            WebAuthnChallengeEntity saved = challenges.save(new WebAuthnChallengeEntity(
                null, userId, LOGIN, request.toJson(), Instant.now().plus(Duration.ofMinutes(5)), null));
            return new PasskeyCeremony(saved.id(), request.toCredentialsGetJson());
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Passkey sign-in could not be started");
        }
    }

    @Transactional
    public UserEntity finishLogin(long challengeId, String credentialJson) {
        WebAuthnChallengeEntity challenge = requireChallenge(challengeId, LOGIN, null);
        try {
            AssertionRequest request = AssertionRequest.fromJson(challenge.requestJson());
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response =
                PublicKeyCredential.parseAssertionResponseJson(credentialJson);
            AssertionResult result = partyForRpId(request.getPublicKeyCredentialRequestOptions().getRpId())
                .finishAssertion(FinishAssertionOptions.builder()
                .request(request)
                .response(response)
                .build());
            if (!result.isSuccess()) {
                throw new AuthException("Passkey was not accepted", 401);
            }
            challenges.deleteById(challenge.id());
            String credentialId = result.getCredential().getCredentialId().getBase64Url();
            PasskeyCredentialEntity stored = credentials
                .findByCredentialId(credentialId)
                .orElseThrow(() -> new AuthException("Passkey was not accepted", 401));
            credentials.update(stored.used(result.getSignatureCount(), Instant.now()));
            UserEntity user = usersRepository
                .findById(stored.userId())
                .filter(UserEntity::active)
                .orElseThrow(() -> new AuthException("Account is disabled", 403));
            auditService.record(user.id(), "LOGIN_SUCCESS", "passkey");
            return usersRepository.update(user.loggedInAt(Instant.now()));
        } catch (AssertionFailedException | IOException e) {
            throw new AuthException("Passkey was not accepted", 401);
        }
    }

    public List<PasskeyCredentialEntity> list(Long userId) {
        return credentials.findByUserId(userId);
    }

    @Transactional
    public void delete(UserEntity user, Long passkeyId) {
        PasskeyCredentialEntity credential = credentials
            .findById(passkeyId)
            .filter(item -> item.userId().equals(user.id()))
            .orElseThrow(() -> new AuthException("Passkey not found", 404));
        long remaining = credentials.countByUserId(user.id()) - 1;
        long providers = 0;
        if (user.passwordHash() == null && remaining <= 0 && providers == 0) {
            // Providers are checked by the caller when this would remove the last factor.
        }
        credentials.delete(credential);
        auditService.record(user.id(), "PASSKEY_REMOVED", credential.label());
    }

    public long count(Long userId) {
        return credentials.countByUserId(userId);
    }

    public void ensureCanRemove(UserEntity user, long remainingPasskeys, long liveProviders) {
        if (user.passwordHash() == null && remainingPasskeys <= 0 && liveProviders <= 0) {
            throw new AuthException("Add a password or another sign-in method before removing this passkey", 409);
        }
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userByEmail(username).map(user -> credentials.findByUserId(user.id()).stream()
                .map(this::descriptor)
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userByEmail(username).map(user -> handle(user.id()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return credentials.findByUserHandle(userHandle.getBase64Url()).stream()
            .findFirst()
            .flatMap(credential -> usersRepository.findById(credential.userId()))
            .map(UserEntity::email);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentials.findByCredentialId(credentialId.getBase64Url())
            .filter(credential -> credential.userHandle().equals(userHandle.getBase64Url()))
            .map(this::registered);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentials.findByCredentialId(credentialId.getBase64Url())
            .map(credential -> Set.of(registered(credential)))
            .orElse(Set.of());
    }

    private WebAuthnChallengeEntity requireChallenge(long id, String purpose, Long expectedUser) {
        WebAuthnChallengeEntity challenge = challenges
            .findById(id)
            .filter(item -> purpose.equals(item.purpose()))
            .filter(item -> item.expiresAt().isAfter(Instant.now()))
            .orElseThrow(() -> new AuthException("Passkey challenge expired"));
        if (expectedUser != null && challenge.userId() != null && !expectedUser.equals(challenge.userId())) {
            throw new AuthException("Passkey challenge expired");
        }
        return challenge;
    }

    private Optional<UserEntity> userByEmail(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return usersRepository.findByEmailNormalized(username.trim().toLowerCase());
    }

    private PublicKeyCredentialDescriptor descriptor(PasskeyCredentialEntity credential) {
        try {
            return PublicKeyCredentialDescriptor.builder()
                .id(ByteArray.fromBase64Url(credential.credentialId()))
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private RegisteredCredential registered(PasskeyCredentialEntity credential) {
        try {
            return RegisteredCredential.builder()
                .credentialId(ByteArray.fromBase64Url(credential.credentialId()))
                .userHandle(ByteArray.fromBase64Url(credential.userHandle()))
                .publicKeyCose(ByteArray.fromBase64Url(credential.publicKeyCose()))
                .signatureCount(credential.signatureCount())
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private RelyingParty party(String origin) {
        return partyForRpId(rpId(origin));
    }

    private RelyingParty partyForRpId(String rpId) {
        Set<String> matching = origins.stream()
            .filter(origin -> matches(host(origin), rpId))
            .collect(Collectors.toSet());
        if (matching.isEmpty()) {
            throw new AuthException("This site cannot register a passkey");
        }
        return RelyingParty.builder()
            .identity(RelyingPartyIdentity.builder().id(rpId).name(properties.passkey().rpName()).build())
            .credentialRepository(this)
            .origins(matching)
            .build();
    }

    private String rpId(String origin) {
        String host = host(origin);
        if (host == null || host.isBlank() || "localhost".equals(host) || "127.0.0.1".equals(host)) {
            return properties.passkey().rpId();
        }
        if ("grammarbar.com".equals(host) || host.endsWith(".grammarbar.com")) {
            return "grammarbar.com";
        }
        if ("posadskiy.com".equals(host) || host.endsWith(".posadskiy.com")) {
            return "posadskiy.com";
        }
        return properties.passkey().rpId();
    }

    private static boolean matches(String host, String rpId) {
        return host != null && (host.equals(rpId) || host.endsWith("." + rpId));
    }

    private static String host(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        try {
            String value = origin.contains("://") ? origin : "https://" + origin;
            return URI.create(value).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ByteArray handle(Long userId) {
        return new ByteArray(ByteBuffer.allocate(8).putLong(userId).array());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    public record PasskeyCeremony(Long challengeId, String publicKeyJson) {}
}
