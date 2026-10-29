package com.posadskiy.email.resend;

import static org.junit.jupiter.api.Assertions.*;

import com.posadskiy.email.api.TransactionalEmailRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResendTransactionalSenderTest {

    @Test
    void postsATemplatePayloadWithTheIdempotencyKey() {
        Captured captured = new Captured();
        ResendTransactionalSender sender = new ResendTransactionalSender(
                new ResendProperties(
                        "re_test",
                        "alpha@example.com,beta@example.com",
                        "https://api.resend.com",
                        null),
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
                new ResendProperties("re_test", "  ", "https://api.resend.com", null), (json, key) -> 200);
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
                new ResendProperties("re_test", "beta@example.com", "https://api.resend.com", null),
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
                new ResendProperties("", "from@example.com", "https://api.resend.com", null), (json, key) -> 200);
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
