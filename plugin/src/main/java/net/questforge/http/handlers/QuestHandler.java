package net.questforge.http.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.questforge.config.AssignmentConfig;
import net.questforge.engine.QuestLoader;
import net.questforge.engine.QuestManager;
import net.questforge.model.Quest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestHandler implements HttpHandler {

    private final QuestManager questManager;
    private final QuestLoader questLoader;
    private final AssignmentConfig assignmentConfig;
    private final Gson gson = new Gson();

    public QuestHandler(QuestManager questManager, QuestLoader questLoader, AssignmentConfig assignmentConfig) {
        this.questManager = questManager;
        this.questLoader = questLoader;
        this.assignmentConfig = assignmentConfig;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            switch (method) {
                case "GET":
                    handleGet(exchange);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    sendError(exchange, 405, "Method not allowed");
                    break;
            }
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        Map<String, Quest> quests = questManager.getQuests();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, Quest> entry : quests.entrySet()) {
            Quest quest = entry.getValue();
            Map<String, Object> questData = new LinkedHashMap<>();
            questData.put("id", quest.getQuestId());
            questData.put("name", quest.getQuestName());
            questData.put("npcRoles", assignmentConfig.getNpcsForQuest(quest.getQuestId()));
            result.add(questData);
        }

        sendJson(exchange, 200, result);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        Quest quest;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            quest = gson.fromJson(reader, Quest.class);
        }

        if (quest == null || quest.getQuestId() == null) {
            sendError(exchange, 400, "Invalid quest JSON");
            return;
        }

        questLoader.save(quest);
        questManager.reload();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("questId", quest.getQuestId());
        sendJson(exchange, 200, response);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String prefix = "/questforge/quests/";
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            sendError(exchange, 400, "Missing quest id in path");
            return;
        }

        String questId = path.substring(prefix.length());
        questLoader.delete(questId);
        assignmentConfig.removeQuest(questId);
        questManager.reload();

        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "ok");
        sendJson(exchange, 200, response);
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
