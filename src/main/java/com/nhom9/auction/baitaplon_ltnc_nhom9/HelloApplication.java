package com.nhom9.auction.baitaplon_ltnc_nhom9;

import com.nhom9.auction.baitaplon_ltnc_nhom9.server.AuthServer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.BindException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        startBackendIfNeeded();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/LoginView.fxml"));
        System.out.println(fxmlLoader);
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    private void startBackendIfNeeded() {
        if (!isLocalServerConfigured()) {
            return;
        }

        Thread serverThread = new Thread(() -> {
            try {
                new AuthServer().start();
            } catch (BindException e) {
                // Port already in use -> backend is likely already running.
                System.out.println("Auth server already running on port 8080.");
            } catch (IOException e) {
                System.err.println("Cannot start auth server: " + e.getMessage());
            }
        }, "auth-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private boolean isLocalServerConfigured() {
        String baseUrl = AppConfig.SERVER_BASE_URL.toLowerCase();
        return baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1");
    }
}
