package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the UBID member login view.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberCheckBox;

    @FXML
    private void initialize() {
        if (usernameField != null) {
            usernameField.requestFocus();
        }
    }

    @FXML
    private void onLogin(ActionEvent event) {
        String user = usernameField != null ? usernameField.getText() : "";
        String pass = passwordField != null ? passwordField.getText() : "";
        boolean remember = rememberCheckBox != null && rememberCheckBox.isSelected();

        // Replace with navigation or auth service integration.
        showInfo(String.format(
                "Login (demo)%nUsername: %s%nPassword: %s%nRemember: %s",
                user.isBlank() ? "(empty)" : user,
                pass.isBlank() ? "(empty)" : "••••",
                remember));
    }

    @FXML
    private void onSignUp(ActionEvent event) {
        showInfo("Sign up flow — wire to registration screen or route.");
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        showInfo("Forgot password — wire to recovery flow.");
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UBID");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
