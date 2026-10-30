package com.posadskiy.email.resend;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkResendHttpTest {

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int[] statuses = {200};

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            int call = calls.getAndIncrement();
            int status = statuses[Math.min(call, statuses.length - 1)];
            byte[] body = "{\"message\":\"bad address person@example.com\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private JdkResendHttp http() {
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        return new JdkResendHttp(new ResendProperties("re_key", "from@example.com", url, null));
    }

    @Test
    void retriesTransientFailuresWithTheSameIdempotencyKey() {
        statuses = new int[] {503, 429, 200};
        assertEquals(200, http().postEmails("{}", "key-1"));
        assertEquals(3, calls.get());
    }

    @Test
    void doesNotRetryRejections() {
        statuses = new int[] {422};
        assertThrows(EmailDeliveryException.class, () -> http().postEmails("{}", "key-1"));
        assertEquals(1, calls.get());
    }

    @Test
    void givesUpAfterTheLastRetry() {
        statuses = new int[] {500};
        assertThrows(EmailDeliveryException.class, () -> http().postEmails("{}", "key-1"));
        assertEquals(3, calls.get());
    }

    @Test
    void errorExcerptsDoNotCarryRecipientAddresses() {
        statuses = new int[] {422};
        EmailDeliveryException e = assertThrows(EmailDeliveryException.class, () -> http().postEmails("{}", "key-1"));
        assertFalse(e.getMessage().contains("person@example.com"));
        assertTrue(e.getMessage().contains("<address>"));
    }

    @Test
    void rendersPlaceholdersInOnePass() {
        String html = ResendTransactionalSender.render(
                "<a href=\"{{{A}}}\">{{{B}}}</a>{{{MISSING}}}", Map.of("A", "x{{{B}}}&", "B", "<b>"));
        assertEquals("<a href=\"x{{{B}}}&amp;\">&lt;b&gt;</a>{{{MISSING}}}", html);
    }
}
