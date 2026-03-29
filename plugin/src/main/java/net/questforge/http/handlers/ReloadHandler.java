package net.questforge.http.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.questforge.engine.QuestManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReloadHandler implements HttpHandler {

    private final QuestManager questManager;
    private final Gson gson = new Gson();

    public ReloadHandler(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            questManager.reload();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ok");
            response.put("questsLoaded", questManager.getQuests().size());
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
