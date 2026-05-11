package com.nhom9.auction.baitaplon_ltnc_nhom9.client;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AuthApiClient {
    private final HttpClient httpClient;
    private String lastError = "";

    public AuthApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public User login(String username, String password) {
        String body = "username=" + encode(username) + "&password=" + encode(password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.SERVER_BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String payload = response.body();
            if (response.statusCode() != 200 || payload == null || !payload.startsWith("OK|")) {
                lastError = extractError(payload, "Login failed (HTTP " + response.statusCode() + ").");
                return null;
            }
            String[] parts = payload.split("\\|", -1);
            if (parts.length < 5) {
                lastError = "Invalid server response.";
                return null;
            }
            lastError = "";
            return new User(
                    Integer.parseInt(parts[1]),
                    parts[2],
                    parts[3],
                    Double.parseDouble(parts[4])
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastError = "Cannot connect to server. Please start backend first.";
            return null;
        } catch (NumberFormatException e) {
            lastError = "Invalid data from server.";
            return null;
        }
    }

    public boolean register(String fullName, String username, String password) {
        String body = "fullName=" + encode(fullName)
                + "&username=" + encode(username)
                + "&password=" + encode(password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.SERVER_BASE_URL + "/api/auth/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String payload = response.body();
            if (response.statusCode() != 200 || payload == null || !payload.startsWith("OK|")) {
                lastError = extractError(payload, "Register failed (HTTP " + response.statusCode() + ").");
                return false;
            }
            lastError = "";
            return true;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastError = "Cannot connect to server. Please start backend first.";
            return false;
        }
    }

    public String getLastError() {
        return lastError;
    }

    private String extractError(String payload, String defaultError) {
        if (payload == null || payload.isBlank()) {
            return defaultError;
        }
        if (payload.startsWith("ERR|")) {
            return payload.substring(4).trim();
        }
        return defaultError;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
