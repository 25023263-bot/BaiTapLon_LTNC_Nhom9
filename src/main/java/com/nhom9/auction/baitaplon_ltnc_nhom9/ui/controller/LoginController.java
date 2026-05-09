package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.client.AuthApiClient;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {
    private final AuthApiClient authApiClient = new AuthApiClient();

    @FXML private VBox loginCard;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private CheckBox rememberMe;
    @FXML private Label loginError;

    @FXML private VBox registerCard;
    @FXML private TextField regFullName;
    @FXML private TextField regUsername;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label regError;

    public void initialize() {
        loginUsername.setOnAction(e -> loginPassword.requestFocus());
        loginPassword.setOnAction(e -> onLogin());
    }

    @FXML
    private void handleRegister() {
        loginCard.setVisible(false);
        loginCard.setManaged(false);
        registerCard.setVisible(true);
        registerCard.setManaged(true);
    }

    @FXML
    private void handleBackToLogin() {
        registerCard.setVisible(false);
        registerCard.setManaged(false);
        loginCard.setVisible(true);
        loginCard.setManaged(true);
    }

    @FXML
    private void onLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please input username and password.");
            return;
        }

        User user = authApiClient.login(username, password);
        if (user == null) {
            showError(authApiClient.getLastError());
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginUsername.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Cannot open main screen.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegister() {
        String fullName = regFullName.getText().trim();
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        String confirm = regConfirm.getText();

        if (fullName.isEmpty() || username.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            showRegError("Please input all register fields.");
            return;
        }
        if (!password.equals(confirm)) {
            showRegError("Confirm password does not match.");
            return;
        }

        boolean success = authApiClient.register(fullName, username, password);
        if (!success) {
            showRegError(authApiClient.getLastError());
            return;
        }

        regError.setVisible(false);
        regError.setManaged(false);
        handleBackToLogin();
    }

    @FXML
    private void forgotPassword() {
        showError("Feature is not implemented.");
    }

    private void showRegError(String msg) {
        regError.setText(msg);
        regError.setVisible(true);
        regError.setManaged(true);
    }

    private void showError(String message) {
        loginError.setText(message);
        loginError.setVisible(true);
        loginError.setManaged(true);
    }
}
