package br.com.naheroback.providers.deepseek;

import br.com.naheroback.modules.practiceExams.services.StudyPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekClientTest {

    private static final String VALID_PLAN = """
            {"summary": "Close to passing. Focus on security.",
             "priorities": [
               {"domain": "Security and Compliance", "why": "Missed IAM.", "whatToStudy": "IAM policies."},
               {"domain": "Cloud Concepts", "why": "Missed elasticity.", "whatToStudy": "Elasticity."},
               {"domain": "Billing, Pricing, and Support", "why": "Missed support plans.", "whatToStudy": "Support plans."}
             ],
             "plan": ["IAM users", "IAM policies", "Elasticity", "Support plans", "Pricing models"]}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<String> replies = new ArrayDeque<>();
    private final AtomicInteger calls = new AtomicInteger();

    private HttpServer server;
    private DeepSeekClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            String content = replies.size() > 1 ? replies.poll() : replies.peek();
            byte[] body = objectMapper.writeValueAsBytes(
                    Map.of("choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        client = new DeepSeekClient("http://localhost:" + server.getAddress().getPort(), "test-key", "deepseek-flash",
                Duration.ofSeconds(5), objectMapper, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Should map a reply in the requested shape to the DTO")
    void shouldMapAValidReply() {
        replies.add(VALID_PLAN);

        StudyPlan plan = client.completeJson("system", "user", StudyPlan.class);

        assertEquals(3, plan.priorities().size());
        assertEquals("Security and Compliance", plan.priorities().getFirst().domain());
        assertEquals(5, plan.plan().size());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("Should fail after the retry when a required field is missing")
    void shouldFailWhenAFieldIsMissing() {
        replies.add("{\"summary\": \"Close to passing. Focus on security.\", \"priorities\": []}");

        assertThrows(IllegalStateException.class, () -> client.completeJson("system", "user", StudyPlan.class));
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("Should fail when the reply has a key outside the requested shape")
    void shouldFailOnAnUnknownKey() throws Exception {
        Map<String, Object> plan = objectMapper.readValue(VALID_PLAN, Map.class);
        plan.put("score", "62%");
        replies.add(objectMapper.writeValueAsString(plan));

        assertThrows(IllegalStateException.class, () -> client.completeJson("system", "user", StudyPlan.class));
    }

    @Test
    @DisplayName("Should fail when the plan has more topics than allowed")
    void shouldFailOnTooManyTopics() throws Exception {
        Map<String, Object> plan = objectMapper.readValue(VALID_PLAN, Map.class);
        plan.put("plan", List.of("1", "2", "3", "4", "5", "6", "7", "8"));
        replies.add(objectMapper.writeValueAsString(plan));

        assertThrows(IllegalStateException.class, () -> client.completeJson("system", "user", StudyPlan.class));
    }

    @Test
    @DisplayName("Should fail when a priority has a blank field")
    void shouldFailOnABlankPriorityField() {
        replies.add(VALID_PLAN.replace("\"why\": \"Missed IAM.\"", "\"why\": \"  \""));

        assertThrows(IllegalStateException.class, () -> client.completeJson("system", "user", StudyPlan.class));
    }

    @Test
    @DisplayName("Should fail when the reply is not JSON")
    void shouldFailOnNonJson() {
        replies.add("Here is your plan: study IAM.");

        assertThrows(IllegalStateException.class, () -> client.completeJson("system", "user", StudyPlan.class));
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("Should return the retried reply when the first one deviates from the shape")
    void shouldRecoverOnTheRetry() {
        replies.add("{\"summary\": \"Too short.\"}");
        replies.add(VALID_PLAN);

        StudyPlan plan = client.completeJson("system", "user", StudyPlan.class);

        assertEquals(5, plan.plan().size());
        assertEquals(2, calls.get());
    }
}
