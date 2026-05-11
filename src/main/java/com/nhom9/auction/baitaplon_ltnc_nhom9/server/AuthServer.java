package com.nhom9.auction.baitaplon_ltnc_nhom9.server;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class AuthServer {
    private final UserRepository userRepository = new UserRepository();

    public static void main(String[] args) throws IOException {
        new AuthServer().start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/auth/login", this::handleLogin);
        server.createContext("/api/auth/register", this::handleRegister);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Auth server started at http://localhost:8080");
        if (!DatabaseConnection.ensureSchemaInitialized()) {
            System.out.println("Warning: database is not ready yet. Fix DB config and retry requests.");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 405, "ERR|Method not allowed");
            return;
        }

        Map<String, String> form = parseForm(exchange);
        String username = form.getOrDefault("username", "").trim();
        String password = form.getOrDefault("password", "").trim();
        if (username.isEmpty() || password.isEmpty()) {
            write(exchange, 400, "ERR|Missing username or password.");
            return;
        }

        if (!isDatabaseReady()) {
            write(exchange, 503, "ERR|Database is unavailable.");
            return;
        }

        try {
            User user = userRepository.login(username, password);
            if (user == null) {
                write(exchange, 401, "ERR|Wrong username or password.");
                return;
            }

            String response = "OK|"
                    + user.getId() + "|"
                    + safe(user.getFullName()) + "|"
                    + safe(user.getUsername()) + "|"
                    + user.getBalance();
            write(exchange, 200, response);
        } catch (RuntimeException e) {
            e.printStackTrace();
            write(exchange, 500, "ERR|Internal server error.");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 405, "ERR|Method not allowed");
            return;
        }

        Map<String, String> form = parseForm(exchange);
        String fullName = form.getOrDefault("fullName", "").trim();
        String username = form.getOrDefault("username", "").trim();
        String password = form.getOrDefault("password", "");

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            write(exchange, 400, "ERR|Missing register fields.");
            return;
        }

        if (!isDatabaseReady()) {
            write(exchange, 503, "ERR|Database is unavailable.");
            return;
        }

        try {
            boolean ok = userRepository.register(fullName, username, password);
            if (!ok) {
                write(exchange, 409, "ERR|Username already exists.");
                return;
            }
            write(exchange, 200, "OK|Registered");
        } catch (RuntimeException e) {
            e.printStackTrace();
            write(exchange, 500, "ERR|Internal server error.");
        }
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            String raw = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = new HashMap<>();
            if (raw.isBlank()) {
                return form;
            }

            String[] pairs = raw.split("&");
            for (String pair : pairs) {
                String[] parts = pair.split("=", 2);
                String key = decode(parts[0]);
                String value = parts.length > 1 ? decode(parts[1]) : "";
                form.put(key, value);
            }
            return form;
        }
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", "/");
    }

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private boolean isDatabaseReady() {
        return DatabaseConnection.ensureSchemaInitialized();
    }
}
