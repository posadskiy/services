package com.posadskiy.email.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.posadskiy.email.resend.ResendProperties;
import com.svix.Webhook;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ResendWebhookControllerTest {

    private static final String SECRET = "whsec_" + Base64.getEncoder().encodeToString("test-secret".getBytes());

    @Test
    void acceptsASignedBounce() throws Exception {
        String payload = "{\"type\":\"email.bounced\"}";
        String id = "msg_123";
        long timestamp = Instant.now().getEpochSecond();
        String signature = new Webhook(SECRET).sign(id, timestamp, payload);

        HttpResponse<Void> response = controller().receive(request(payload, id, Long.toString(timestamp), signature));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
    }

    @Test
    void rejectsATamperedBody() throws Exception {
        String id = "msg_123";
        long timestamp = Instant.now().getEpochSecond();
        String signature = new Webhook(SECRET).sign(id, timestamp, "{}");

        HttpResponse<Void> response =
                controller().receive(request("{\"type\":\"email.bounced\"}", id, Long.toString(timestamp), signature));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }

    private static ResendWebhookController controller() {
        return new ResendWebhookController(
                new ResendProperties("re_test", "from@example.com", "https://api.resend.com", SECRET));
    }

    private static HttpRequest<?> request(String payload, String id, String timestamp, String signature) {
        return HttpRequest.POST("/email/webhooks/resend", payload)
                .header("svix-id", id)
                .header("svix-timestamp", timestamp)
                .header("svix-signature", signature);
    }
}
