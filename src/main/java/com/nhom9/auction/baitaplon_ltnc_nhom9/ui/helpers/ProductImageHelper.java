package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/** Tạo node ảnh sản phẩm (file thật hoặc emoji fallback). */
public final class ProductImageHelper {

    private ProductImageHelper() {}

    public static Node buildNode(String imageUrl, String emoji, double width, double height) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                String uri = new File(imageUrl).toURI().toString();
                Image img = new Image(uri, width, height, true, true, false);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    return iv;
                }
            } catch (Exception ignored) {
            }
        }
        Label fallback = new Label(emoji);
        fallback.getStyleClass().add("card-image-icon");
        return fallback;
    }
}
