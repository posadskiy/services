package com.posadskiy.email.resend;

import com.posadskiy.email.api.TransactionalEmailRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResendTransactionalSenderTest {

    @Test
    void postsATemplatePayloadWithTheIdempotencyKey() {
        Captured captured = new Captured();
        ResendTransactionalSender sender = new ResendTransactionalSender(
            new ResendProperties("re_test", "alpha@example.com,beta@example.com", "https://api.resend.com"),
            captured);

        sender.send(new TransactionalEmailRequest(
            "Alpha <alpha@example.com>",
            "person@example.com",
            "verify-email",
            Map.of("VERIFY_URL", "https://grammarbar.com/verify-email?token=abc"),
            "verify-email/1/2",
            "verify_email"));

        assertEquals("verify-email/1/2", captured.key);
        assertTrue(captured.json.contains("\"from\":\"Alpha <alpha@example.com>\""));
        assertTrue(captured.json.contains("\"subject\":\"Confirm your email\""));
        assertTrue(captured.json.contains("max-width:560px"));
        assertTrue(captured.json.contains("https://grammarbar.com/verify-email?token=abc"));
        assertFalse(captured.json.contains("\"template\""));
        assertTrue(captured.json.contains("\"value\":\"verify_email\""));
    }

    @Test
    void rejectsAMissingAllowlistBeforeCallingResend() {
        ResendTransactionalSender sender = new ResendTransactionalSender(
            new ResendProperties("re_test", "  ", "https://api.resend.com"), (json, key) -> 200);
        EmailDeliveryException ex = assertThrows(
            EmailDeliveryException.class,
            () -> sender.send(new TransactionalEmailRequest(
                "Alpha <alpha@example.com>",
                "person@example.com",
                "verify-email",
                Map.of("VERIFY_URL", "https://example.com"),
                "k",
                "verify_email")));
        assertEquals("RESEND_ALLOWED_FROM is not set", ex.getMessage());
    }

    @Test
    void rejectsAFromAddressThatIsNotOnTheAllowlist() {
        ResendTransactionalSender sender = new ResendTransactionalSender(
            new ResendProperties("re_test", "beta@example.com", "https://api.resend.com"),
            (json, key) -> 200);
        EmailDeliveryException ex = assertThrows(
            EmailDeliveryException.class,
            () -> sender.send(new TransactionalEmailRequest(
                "Alpha <alpha@example.com>",
                "person@example.com",
                "verify-email",
                Map.of("VERIFY_URL", "https://example.com"),
                "k",
                "verify_email")));
        assertEquals("from address is not allowed", ex.getMessage());
    }

    @Test
    void rejectsAMissingApiKeyBeforeCallingResend() {
        ResendTransactionalSender sender = new ResendTransactionalSender(
            new ResendProperties("", "from@example.com", "https://api.resend.com"), (json, key) -> 200);
        assertThrows(
            EmailDeliveryException.class,
            () -> sender.send(new TransactionalEmailRequest(
                "Beta <beta@example.com>",
                "person@example.com",
                "verify-email",
                Map.of("VERIFY_URL", "https://example.com"),
                "k",
                "verify_email")));
    }

    private static ResendTransactionalSender restricted(Captured captured) {
        return new ResendTransactionalSender(
            new ResendProperties("re_test", "alpha@example.com", "https://api.resend.com"),
            captured,
            "https://grammarbar.com, https://costy.posadskiy.com/");
    }

    private static TransactionalEmailRequest linkTo(String url) {
        return new TransactionalEmailRequest(
            "Alpha <alpha@example.com>",
            "person@example.com",
            "verify-email",
            Map.of("VERIFY_URL", url),
            "k",
            "verify_email");
    }

    @Test
    void sendsALinkOnAnAllowedOrigin() {
        Captured captured = new Captured();
        restricted(captured).send(linkTo("https://costy.posadskiy.com/verify-email?token=abc"));
        assertNotNull(captured.json);
    }

    @Test
    void refusesALinkOnAnotherOriginBeforeCallingResend() {
        Captured captured = new Captured();
        ResendTransactionalSender sender = restricted(captured);
        for (String url : new String[] {
            "https://evil.example/verify-email?token=abc",
            "https://grammarbar.com.evil.example/verify-email",
            "http://grammarbar.com/verify-email",
            "https://grammarbar.com:8443/verify-email",
            "//grammarbar.com/verify-email",
            "not a url"
        }) {
            assertThrows(LinkNotAllowedException.class, () -> sender.send(linkTo(url)), url);
        }
        assertNull(captured.json);
    }

    private static final class Captured implements ResendHttp {
        String json;
        String key;

        @Override
        public int postEmails(String json, String idempotencyKey) {
            this.json = json;
            this.key = idempotencyKey;
            return 200;
        }
    }
}
