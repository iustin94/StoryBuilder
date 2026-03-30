package net.questforge.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.questforge.config.AssignmentConfig;
import net.questforge.engine.QuestLoader;
import net.questforge.engine.QuestManager;
import net.questforge.http.handlers.AssignHandler;
import net.questforge.http.handlers.NpcsHandler;
import net.questforge.http.handlers.QuestHandler;
import net.questforge.http.handlers.ReloadHandler;
import net.questforge.http.handlers.StatusHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class QuestForgeHttpServer {

    private final HttpServer server;

    public QuestForgeHttpServer(int port, QuestManager questManager,
                                AssignmentConfig assignmentConfig, QuestLoader questLoader) throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/questforge/status", cors(new StatusHandler(questManager)));
        server.createContext("/questforge/npcs", cors(new NpcsHandler()));
        server.createContext("/questforge/quests", cors(new QuestHandler(questManager, questLoader, assignmentConfig)));
        server.createContext("/questforge/assign", cors(new AssignHandler(assignmentConfig)));
        server.createContext("/questforge/unassign", cors(new AssignHandler(assignmentConfig)));
        server.createContext("/questforge/reload", cors(new ReloadHandler(questManager)));
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private HttpHandler cors(HttpHandler handler) {
        return (HttpExchange exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            handler.handle(exchange);
        };
    }
}
