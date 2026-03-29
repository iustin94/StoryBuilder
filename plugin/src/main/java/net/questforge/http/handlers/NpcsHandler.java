package net.questforge.http.handlers;

import com.google.gson.Gson;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.NPCRole;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NpcsHandler implements HttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Collection<NPCRole> roles = NPCPlugin.get().getBuilderManager().getRegisteredRoles();
            List<Map<String, String>> result = new ArrayList<>();
            for (NPCRole role : roles) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("role", role.getName());
                result.add(entry);
            }

            sendJson(exchange, 200, result);
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
