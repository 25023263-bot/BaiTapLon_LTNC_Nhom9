package com.nhom9.auction.baitaplon_ltnc_nhom9;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/HomeView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1120.0, 740.0);
        stage.setTitle("UBid");
        stage.setScene(scene);
        stage.show();
    }
}
