package net.questforge.http.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.questforge.config.AssignmentConfig;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class AssignHandler implements HttpHandler {

    private final AssignmentConfig assignmentConfig;
    private final Gson gson = new Gson();

    public AssignHandler(AssignmentConfig assignmentConfig) {
        this.assignmentConfig = assignmentConfig;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            Map<String, String> body;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = gson.fromJson(reader, Map.class);
                body = parsed;
            }

            if (body == null || body.get("questId") == null || body.get("npcRole") == null) {
                sendError(exchange, 400, "Missing questId or npcRole");
                return;
            }

            String questId = body.get("questId");
            String npcRole = body.get("npcRole");
            String path = exchange.getRequestURI().getPath();

            if (path.contains("/unassign")) {
                assignmentConfig.unassign(npcRole, questId);
            } else {
                assignmentConfig.assign(npcRole, questId);
            }

            Map<String, String> response = new LinkedHashMap<>();
            response.put("status", "ok");
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("message", message);
        sendJson(exchange, statusCode, error);
    }
}
