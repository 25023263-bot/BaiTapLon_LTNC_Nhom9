package com.nhom9.auction.baitaplon_ltnc_nhom9;

/**
 * Launcher wrapper — bắt buộc phải có khi đóng gói JavaFX vào fat JAR.
 *
 * Lý do: Khi chạy fat JAR, JVM kiểm tra Main-Class trong MANIFEST.MF
 * TRƯỚC khi load JavaFX module. Nếu Main-Class extends Application (JavaFX),
 * JVM ném lỗi "JavaFX runtime components are missing".
 *
 * Launcher KHÔNG extends Application → JVM load được → gọi HelloApplication.
 */
public class Launcher {
    public static void main(String[] args) {
        HelloApplication.main(args);
    }
}