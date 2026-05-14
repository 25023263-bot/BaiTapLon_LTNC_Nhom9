package com.nhom9.auction.baitaplon_ltnc_nhom9;

import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseConnection.getInstance();

        // ✅ THÊM DÒNG NÀY — start scheduler chạy background
        ServiceLocator.getInstance().getAuctionScheduler().start();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/HomeView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1120.0, 740.0);
        stage.setTitle("UBid");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // ✅ THÊM METHOD NÀY — dọn dẹp khi app tắt
        ServiceLocator.getInstance().shutdown();
    }
}