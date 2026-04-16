package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class GUIController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    private Parent loginRoot;


    public void initialize() {
        categoryCombo.getItems().addAll(
                "All Categories",
                "Electronics",
                "Fashion",
                "Motors",
                "Collectibles & Art",
                "Sports",
                "Health & Beauty"
        );
        categoryCombo.setValue("All Categories");


    }

    @FXML
    protected void onSearch() {
        String keyword = searchField.getText().trim();
        if (!keyword.isEmpty()) {
            System.out.println("Tìm kiếm: " + keyword
                    + " | Category: " + categoryCombo.getValue());
        }
    }



    @FXML
    protected void onLogout(ActionEvent event) {
        try {
            // ✅ ĐÚNG
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/hellofx/Login.fxml"));
            Parent root = loader.load();

            // Lấy Stage từ MenuItem → ContextMenu → ownerWindow
            MenuItem menuItem = (MenuItem) event.getSource();
            Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

            root.setOpacity(0);
            stage.getScene().setRoot(root);

            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

            stage.setTitle("Đăng nhập");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    protected void onSignIn(ActionEvent event) {
        try {
            // ✅ ĐÚNG
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/hellofx/Login.fxml"));
            Parent root = loader.load(); // ✅ load lại Login.fxml

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            root.setOpacity(0);
            stage.getScene().setRoot(root);

            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

            stage.setTitle("Đăng nhập");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    protected void onRegister(ActionEvent event) {
        try {
            // ✅ ĐÚNG
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/hellofx/Register.fxml"));
            Parent root = loader.load(); //

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            root.setOpacity(0);
            stage.getScene().setRoot(root);

            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

            stage.setTitle("Đăng kí");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
